package org.eclipse.egit.ui.internal.clone;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.IRepositorySearchResult;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.clone.GitRepositoryImportExtension.RepositoryImport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 *
 */
public class GitImportWizard2 extends Wizard implements IImportWizard {

	private List<RepositoryImport> repositoryImports;

	private GitSelectRepositoryPage selectRepoPage = new GitSelectRepositoryPage();

	private SourceBranchPage sourceBranchPage = new SourceBranchPage();

	private CloneDestinationPage cloneDestinationPage = new CloneDestinationPage();

	private GitSelectWizardPage importWithDirectoriesPage = new GitSelectWizardPage();

	private GitProjectsImportPage projectsImportPage = new GitProjectsImportPage();

	private GitCreateGeneralProjectPage createGeneralProjectPage = new GitCreateGeneralProjectPage();


	/**
	 *
	 */
	public GitImportWizard2() {
		setWindowTitle(UIText.GitImportWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		// TODO Auto-generated constructor stub
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
		addPage(sourceBranchPage);
		addPage(cloneDestinationPage);
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
		if (page instanceof IRepositorySearchResult)
			return sourceBranchPage;
		return super.getNextPage(page);
	}

}
