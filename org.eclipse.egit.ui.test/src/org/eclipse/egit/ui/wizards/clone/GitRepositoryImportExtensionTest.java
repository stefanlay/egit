package org.eclipse.egit.ui.wizards.clone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.eclipse.egit.ui.internal.clone.GitRepositoryImportExtension;
import org.eclipse.egit.ui.internal.clone.GitRepositoryImportExtension.RepositoryImport;
import org.junit.Test;

public class GitRepositoryImportExtensionTest {

	@SuppressWarnings("boxing")
	@Test
	public void testGetRepositoryImports() throws Exception {
		List<RepositoryImport> repositoryImports = GitRepositoryImportExtension.getRepositoryImports();
		assertThat(repositoryImports, is (notNullValue()));
		assertThat(repositoryImports.size(), is(4));
		assertThat(repositoryImports.get(1).getLabel(), is("ServerWithoutPage1"));
		assertThat(repositoryImports.get(1).hasFixLocation(), is(true));
		assertThat(repositoryImports.get(1).getRepositoryServerProvider(), is(TestRepositoryServerProvider.class));
		assertThat(repositoryImports.get(2).getLabel(), is("TestServer"));
		assertThat(repositoryImports.get(2).hasFixLocation(), is(false));
		assertThat(repositoryImports.get(2).getRepositoryServerProvider(), is(TestRepositoryServerProvider.class));
		assertThat(repositoryImports.get(2).getRepositoryImportPage(), is(TestRepositorySearchPage.class));
		assertThat(repositoryImports.get(3).getLabel(), is("ServerWithoutPage2"));
		assertThat(repositoryImports.get(3).hasFixLocation(), is(false));
	}
}
