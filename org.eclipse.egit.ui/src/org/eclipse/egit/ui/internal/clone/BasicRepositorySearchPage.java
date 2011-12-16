package org.eclipse.egit.ui.internal.clone;

import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Allows to search for a git repository with the repository name
 */
public class BasicRepositorySearchPage extends WizardPage implements
		IRepositorySearchResult {

	private Text searchText;

	private GitRepositoryInfo repositoryInfo;
	/**
	 *
	 */
	public BasicRepositorySearchPage() {
		super(BasicRepositorySearchPage.class.getName());
		setTitle("Search for repositories"); //$NON-NLS-1$
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		final Composite root = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(root);

		Composite rowOne = new Composite(root, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(rowOne);

		Composite rowTwo = new Composite(root, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(rowTwo);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(rowTwo);

		searchText = new Text(rowTwo, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(searchText);

		final Button searchButton = new Button(rowTwo, SWT.NONE);
		searchButton.setText("Search"); //$NON-NLS-1$
		searchButton.setEnabled(false);

		final TableViewer repoListViewer = new TableViewer(root);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(repoListViewer.getControl());
		repoListViewer.setContentProvider(new ArrayContentProvider());

		repoListViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				new IStyledLabelProvider() {

					private Image repoImage = UIIcons.REPOSITORY.createImage();

					public void removeListener(ILabelProviderListener listener) {
//
					}

					public boolean isLabelProperty(Object element,
							String property) {
						return false;
					}

					public void dispose() {
						repoImage.dispose();
					}

					public void addListener(ILabelProviderListener listener) {
//
					}

					public StyledString getStyledText(Object element) {
						StyledString styled = new StyledString();
						GitRepositoryInfo repo = (GitRepositoryInfo) element;
						styled.append(repo.getName());
						return styled;
					}

					public Image getImage(Object element) {
						return repoImage;
					}
				}));

		repoListViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					public void selectionChanged(SelectionChangedEvent event) {
						validate(repoListViewer);
					}
				});

		searchText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				searchButton
						.setEnabled(searchText.getText().trim().length() != 0);
			}
		});

		searchButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent selectionEvent) {
				search(searchText.getText().trim(), repoListViewer);
			}
		});

		setControl(root);
	}

	private void search(String trim, TableViewer repoListViewer) {
		//

	}

	private void validate(TableViewer viewer) {
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
			repositoryInfo = (GitRepositoryInfo) ((IStructuredSelection) selection).getFirstElement();
		}
		setPageComplete(!selection.isEmpty());
	}

	public GitRepositoryInfo getGitRepositoryInfo() {
		return repositoryInfo;
	}

}
