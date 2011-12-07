package org.eclipse.egit.ui.wizards.clone;

import java.net.URISyntaxException;

import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class TestRepositorySearchPage extends WizardPage implements IRepositorySearchResult {

	public TestRepositorySearchPage() {
		super(TestRepositorySearchPage.class.getName());
		setTitle("Find Repository");
		setMessage("Do the needful");
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		new Label(main, SWT.NULL).setText("Search for Repos");
		setControl(main);
	}

	public GitRepositoryInfo getGitRepositoryInfo() {
		try {
			return new GitRepositoryInfo(new URIish("http://egit.eclipse.org/egit.git"));
		} catch (URISyntaxException e) {
			return null;
		}
	}

}
