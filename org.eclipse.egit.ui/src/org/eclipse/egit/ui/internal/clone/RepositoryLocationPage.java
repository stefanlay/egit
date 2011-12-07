package org.eclipse.egit.ui.internal.clone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.clone.GitRepositoryImportExtension.RepositoryImport;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author d044495
 *
 */
public class RepositoryLocationPage extends WizardPage {

	private final List<RepositoryImport> repositoryImports;

	private Map<RepositoryImport, WizardPage> resolvedWizardPages;

	private TreeViewer tv;

	/**
	 * @param repositoryImports
	 */
	public RepositoryLocationPage(List<RepositoryImport> repositoryImports) {
		super(RepositoryLocationPage.class.getName());
		repositoryImports.add(0, RepositoryImport.LOCAL);
		this.repositoryImports = repositoryImports;
		resolvedWizardPages = new HashMap<RepositoryImport, WizardPage>();
		setTitle("Select Repository Source"); //$NON-NLS-1$
		setMessage("Do the needful"); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0)
				.applyTo(main);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		// use a filtered tree
		FilteredTree tree = new FilteredTree(main, SWT.SINGLE | SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL, new PatternFilter(), true);

		tv = tree.getViewer();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		tv.setContentProvider(new RepositoryLocationContentProvider());

		tv.setLabelProvider(new RepositoryLocationLabelProvider());

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		tv.setInput(repositoryImports);
		setControl(main);
	}

	private void checkPage() {
		setErrorMessage(null);
		boolean complete = false;
		IStructuredSelection selection = (IStructuredSelection) tv
				.getSelection();
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof RepositoryImport) {
				RepositoryImport repositoryImport = (RepositoryImport) element;
				if (repositoryImport.equals(RepositoryImport.LOCAL)
						|| repositoryImport.hasFixLocation())
					complete = true;
			} else if (element instanceof RepositoryServerInfo) {
				complete = true;
			}
		}

		setPageComplete(complete);
	}

	@Override
	public IWizardPage getNextPage() {
		IStructuredSelection selection = (IStructuredSelection) tv
				.getSelection();

		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof RepositoryImport) {
				return getNextPage((RepositoryImport) element);
			} else if (element instanceof RepositoryServerInfo) {
				Object parent = ((ITreeContentProvider) tv.getContentProvider())
						.getParent(element);
				if (parent instanceof RepositoryImport)
					return getNextPage((RepositoryImport) parent);
			}
		}

		return null;

	}

	private IWizardPage getNextPage(RepositoryImport repositoryImport) {
		if (repositoryImport.equals(RepositoryImport.LOCAL))
			return getWizard().getNextPage(this);
		else
			return getWizardPage(repositoryImport);
	}

	private WizardPage getWizardPage(RepositoryImport repositoryImport) {
		WizardPage nextPage;
		nextPage = resolvedWizardPages.get(repositoryImport);
		if (nextPage == null) {
			try {
				nextPage = repositoryImport.getRepositoryImportPage();
				if (nextPage == null)
					nextPage = new RepositorySelectionPage(); // TODO default
																// page for repo
																// search
			} catch (CoreException e) {
				Activator.error(e.getLocalizedMessage(), e);
				nextPage = new RepositorySelectionPage(); // TODO default page
															// for repo search
			}
			nextPage.setWizard(getWizard());
			resolvedWizardPages.put(repositoryImport, nextPage);
		}
		return nextPage;
	}
}
