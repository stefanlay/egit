/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.RevisionAnnotationController;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * Blame operation
 */
public class BlameOperation implements IEGitOperation {

	private Repository repository;

	private IStorage storage;

	private String path;

	private AnyObjectId startCommit;

	private Shell shell;

	private IWorkbenchPage page;

	/**
	 * Create annotate operation
	 *
	 * @param repository
	 * @param storage
	 * @param path
	 * @param startCommit
	 * @param shell
	 * @param page
	 */
	public BlameOperation(Repository repository, IStorage storage, String path,
			AnyObjectId startCommit, Shell shell, IWorkbenchPage page) {
		this.repository = repository;
		this.storage = storage;
		this.path = path;
		this.startCommit = startCommit;
		this.shell = shell;
		this.page = page;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		final RevisionInformation info = new RevisionInformation();
		info.setHoverControlCreator(new BlameInformationControlCreator(false));
		info.setInformationPresenterControlCreator(new BlameInformationControlCreator(
				true));

		final BlameCommand command = new BlameCommand(repository)
				.setFollowFileRenames(true).setFilePath(path);
		if (startCommit != null)
			command.setStartCommit(startCommit);
		if (Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.BLAME_IGNORE_WHITESPACE))
			command.setTextComparator(RawTextComparator.WS_IGNORE_ALL);

		final BlameResult result = command.call();
		if (result == null)
			return;

		Map<RevCommit, BlameRevision> revisions = new HashMap<RevCommit, BlameRevision>();
		int lineCount = result.getResultContents().size();
		monitor.beginTask("", lineCount); //$NON-NLS-1$
		BlameRevision previous = null;
		for (int i = 0; i < lineCount; i++) {
			RevCommit commit = result.getSourceCommit(i);
			if (commit == null) {
				// Unregister the current revision
				if (previous != null) {
					previous.register();
					previous = null;
				}
				continue;
			}
			BlameRevision revision = revisions.get(commit);
			if (revision == null) {
				revision = new BlameRevision();
				revision.setRepository(repository);
				revision.setCommit(commit);
				revisions.put(commit, revision);
				info.addRevision(revision);
			}
			if (previous != null)
				if (previous == revision)
					previous.addLine();
				else {
					previous.register();
					previous = revision.reset(i);
				}
			else
				previous = revision.reset(i);
			monitor.worked(1);
		}
		if (previous != null)
			previous.register();

		shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					AbstractDecoratedTextEditor editor;
					if (storage instanceof IFile)
						editor = RevisionAnnotationController.openEditor(page,
								(IFile) storage);
					else
						editor = RevisionAnnotationController.openEditor(page,
								storage, storage);
					if (editor != null)
						editor.showRevisionInformation(info,
								"org.eclipse.egit.ui.internal.decorators.GitQuickDiffProvider"); //$NON-NLS-1$
				} catch (PartInitException e) {
					Activator.handleError(
							"Error displaying blame annotations", e, //$NON-NLS-1$
							false);
				}
			}
		});
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}