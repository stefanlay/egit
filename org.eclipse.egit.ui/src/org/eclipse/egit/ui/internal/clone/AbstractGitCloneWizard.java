/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.egit.core.op.ConfigureFetchAfterCloneTask;
import org.eclipse.egit.core.op.ConfigurePushAfterCloneTask;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.op.SetChangeIdTask;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkingSet;

/**
 * Implements the basic functionality of a clone wizard
 */
public abstract class AbstractGitCloneWizard extends Wizard {

	/**
	 * a page for branch selection
	 */
	protected SourceBranchPage validSource;

	/**
	 * a page for selection of the clone destination
	 */
	protected CloneDestinationPage cloneDestination;

	/**
	 * a page for some gerrit related parameters
	 */
	protected GerritConfigurationPage gerritConfiguration;

	/**
	 * the path where a clone has been made to
	 */
	protected String alreadyClonedInto;

	/**
	 * whether the clone operation is done later on by the caller of the wizard
	 */
	protected boolean callerRunsCloneOperation;

	private CloneOperation cloneOperation;

	/**
	 * adds some basic functionality
	 */
	public AbstractGitCloneWizard() {
		setNeedsProgressMonitor(true);
		validSource = new SourceBranchPage() {

			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					setSelection(getRepositorySelection());
					setCredentials(getCredentials());
				}
				super.setVisible(visible);
			}
		};
		cloneDestination = new CloneDestinationPage() {
			@Override
			public void setVisible(boolean visible) {
				if (visible)
					setSelection(getRepositorySelection(),
							validSource.getAvailableBranches(),
							validSource.getSelectedBranches(),
							validSource.getHEAD());
				super.setVisible(visible);
			}
		};
	}

	/**
	 * subclasses may add pages to the Wizard which will be shown before the clone step
	 */
	protected abstract void addPreClonePages();

	/**
	 * subclasses may add pages to the Wizard which will be shown after the clone step
	 */
	protected abstract void addPostClonePages();

	/**
	 * @return the currently selected repository to clone
	 */
	protected abstract RepositorySelection getRepositorySelection();

	/**
	 * @return credentials
	 */
	protected abstract UserPasswordCredentials getCredentials();

	@Override
	final public void addPages() {
		addPreClonePages();
		addPage(validSource);
		addPage(cloneDestination);
		addPostClonePages();
	}

	/**
	 * @param uri
	 * @param credentials
	 * @return if clone was successful
	 */
	protected boolean performClone(URIish uri,
			UserPasswordCredentials credentials) {
		setWindowTitle(NLS.bind(UIText.GitCloneWizard_jobName, uri.toString()));
		final boolean allSelected;
		final Collection<Ref> selectedBranches;
		if (validSource.isSourceRepoEmpty()) {
			// fetch all branches of empty repo
			allSelected = true;
			selectedBranches = Collections.emptyList();
		} else {
			allSelected = validSource.isAllSelected();
			selectedBranches = validSource.getSelectedBranches();
		}
		final File workdir = cloneDestination.getDestinationFile();
		final Ref ref = cloneDestination.getInitialBranch();
		final String remoteName = cloneDestination.getRemote();

		boolean created = workdir.exists();
		if (!created)
			created = workdir.mkdirs();

		if (!created || !workdir.isDirectory()) {
			final String errorMessage = NLS.bind(
					UIText.GitCloneWizard_errorCannotCreate, workdir.getPath());
			ErrorDialog.openError(getShell(), getWindowTitle(),
					UIText.GitCloneWizard_failed, new Status(IStatus.ERROR,
							Activator.getPluginId(), 0, errorMessage, null));
			// let's give user a chance to fix this minor problem
			return false;
		}

		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		final CloneOperation op = new CloneOperation(uri, allSelected,
				selectedBranches, workdir, ref != null ? ref.getName() : null,
				remoteName, timeout);
		if (credentials != null)
			op.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					credentials.getUser(), credentials.getPassword()));

		if (gerritConfiguration != null
				&& gerritConfiguration.configureGerrit()) {
			boolean hasReviewNotes = hasReviewNotes(uri, timeout, credentials);
			if (!hasReviewNotes)
				MessageDialog.openInformation(getShell(),
						UIText.GitCloneWizard_MissingNotesTitle,
						UIText.GitCloneWizard_MissingNotesMessage);
			doGerritConfiguration(remoteName, op, hasReviewNotes);
		}

		if (cloneDestination.isImportProjects()) {
			final IWorkingSet[] sets = cloneDestination.getWorkingSets();
			op.addPostCloneTask(new PostCloneTask() {
				public void execute(Repository repository,
						IProgressMonitor monitor) throws CoreException {
					importProjects(repository, sets);
				}
			});
		}

		alreadyClonedInto = workdir.getPath();

		if (!callerRunsCloneOperation)
			runAsJob(uri, op);
		else
			cloneOperation = op;
		return true;
	}

	private void importProjects(final Repository repository,
			final IWorkingSet[] sets) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		Job importJob = new Job(MessageFormat.format(
				UIText.GitCloneWizard_jobImportProjects, repoName)) {

			protected IStatus run(IProgressMonitor monitor) {
				List<File> files = new ArrayList<File>();
				ProjectUtil.findProjectFiles(files, repository.getWorkTree(),
						null, monitor);
				if (files.isEmpty())
					return Status.OK_STATUS;

				Set<ProjectRecord> records = new LinkedHashSet<ProjectRecord>();
				for (File file : files)
					records.add(new ProjectRecord(file));
				try {
					ProjectUtils.createProjects(records, repository, sets,
							monitor);
				} catch (InvocationTargetException e) {
					Activator.logError(e.getLocalizedMessage(), e);
				} catch (InterruptedException e) {
					Activator.logError(e.getLocalizedMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		importJob.schedule();
	}

	private boolean hasReviewNotes(final URIish uri, int timeout,
			UserPasswordCredentials credentials) {
		boolean hasNotes = false;
		try {
			final Repository db = new FileRepository(new File("/tmp")); //$NON-NLS-1$
			final ListRemoteOperation listRemoteOp = new ListRemoteOperation(
					db, uri, timeout);
			if (credentials != null)
				listRemoteOp
						.setCredentialsProvider(new EGitCredentialsProvider(
								credentials.getUser(), credentials
										.getPassword()));
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					listRemoteOp.run(monitor);
				}
			});
			String notesRef = Constants.R_NOTES + "review"; //$NON-NLS-1$
			hasNotes = listRemoteOp.getRemoteRef(notesRef) != null;
		} catch (IOException e) {
			Activator.handleError(UIText.GitCloneWizard_failed, e, true);
		} catch (InvocationTargetException e) {
			Activator.handleError(UIText.GitCloneWizard_failed, e.getCause(),
					true);
		} catch (InterruptedException e) {
			// nothing to do
		}
		return hasNotes;
	}

	private void doGerritConfiguration(final String remoteName,
			final CloneOperation op, final boolean hasNotes) {
		String gerritBranch = gerritConfiguration.getBranch();
		URIish pushURI = gerritConfiguration.getURI();
		if (hasNotes) {
			String notesRef = Constants.R_NOTES + "review"; //$NON-NLS-1$
			op.addPostCloneTask(new ConfigureFetchAfterCloneTask(remoteName,
					notesRef + ":" + notesRef)); //$NON-NLS-1$
		}
		if (gerritBranch != null && gerritBranch.length() > 0) {
			ConfigurePushAfterCloneTask push = new ConfigurePushAfterCloneTask(
					remoteName, "HEAD:refs/for/" + gerritBranch, pushURI); //$NON-NLS-1$
			op.addPostCloneTask(push);
		}
		op.addPostCloneTask(new SetChangeIdTask(true));
	}

	/**
	 * @param container
	 */
	public void runCloneOperation(IWizardContainer container) {
		try {
			container.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					executeCloneOperation(cloneOperation, monitor);
				}
			});
		} catch (InvocationTargetException e) {
			Activator.handleError(UIText.GitCloneWizard_failed, e.getCause(),
					true);
		} catch (InterruptedException e) {
			// nothing to do
		}
	}

	private void runAsJob(final URIish uri, final CloneOperation op) {
		final Job job = new Job(NLS.bind(UIText.GitCloneWizard_jobName,
				uri.toString())) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					return executeCloneOperation(op, monitor);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				} catch (InvocationTargetException e) {
					Throwable thr = e.getCause();
					return new Status(IStatus.ERROR, Activator.getPluginId(),
							0, thr.getMessage(), thr);
				}
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.CLONE))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private IStatus executeCloneOperation(final CloneOperation op,
			final IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		final RepositoryUtil util = Activator.getDefault().getRepositoryUtil();

		op.run(monitor);
		util.addConfiguredRepository(op.getGitDir());
		return Status.OK_STATUS;
	}

	/**
	 * @param newValue
	 *            if true the clone wizard just creates a clone operation. The
	 *            caller has to run this operation using runCloneOperation. If
	 *            false the clone operation is performed using a job.
	 */
	public void setCallerRunsCloneOperation(boolean newValue) {
		callerRunsCloneOperation = newValue;
	}

}
