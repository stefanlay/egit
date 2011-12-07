package org.eclipse.egit.ui.internal.clone;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.clone.GitRepositoryImportExtension.RepositoryImport;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 *
 */
public class GitImportWizard2 extends AbstractGitCloneWizard implements IImportWizard {

	private List<RepositoryImport> repositoryImports;

	private GitSelectRepositoryPage selectRepoPage = new GitSelectRepositoryPage();

	private GitSelectWizardPage importWithDirectoriesPage = new GitSelectWizardPage();

	private GitProjectsImportPage projectsImportPage = new GitProjectsImportPage();

	private GitCreateGeneralProjectPage createGeneralProjectPage = new GitCreateGeneralProjectPage();


	/**
	 *
	 */
	public GitImportWizard2() {
		setWindowTitle(UIText.GitImportWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		validSource = new SourceBranchPage();
		cloneDestination = new CloneDestinationPage();
	}

	@Override
	public void addPages() {
		try {
			repositoryImports = GitRepositoryImportExtension.getRepositoryImports();
		} catch (CoreException e) {
			Activator
				.handleError(e.getCause().getMessage(), e.getCause(), true);
		}
		addPage(new RepositoryLocationPage(repositoryImports));
		addPage(selectRepoPage);
		addPage(validSource);
		addPage(cloneDestination);
		addPage(importWithDirectoriesPage);
		addPage(projectsImportPage);
		addPage(createGeneralProjectPage);
	}



	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing o do

	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(selectRepoPage) )
			return importWithDirectoriesPage;
		if (page instanceof IRepositorySearchResult) {
			IRepositorySearchResult result = (IRepositorySearchResult)page;
			GitRepositoryInfo gitRepositoryInfo = result.getGitRepositoryInfo();

			validSource.setSelection(new RepositorySelection(gitRepositoryInfo.getUri(), null));
			return validSource;
		}

		return super.getNextPage(page);
	}

}
