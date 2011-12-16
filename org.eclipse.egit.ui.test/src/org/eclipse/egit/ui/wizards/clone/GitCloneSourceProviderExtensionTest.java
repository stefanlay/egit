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
package org.eclipse.egit.ui.wizards.clone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.junit.Test;

public class GitCloneSourceProviderExtensionTest {

	@SuppressWarnings("boxing")
	@Test
	public void testGetRepositoryImports() throws Exception {
		List<CloneSourceProvider> repositoryImports = GitCloneSourceProviderExtension
				.getCloneSourceProvider();
		assertThat(repositoryImports, is(notNullValue()));
		assertThat(repositoryImports.size(), is(4));
		assertThat(repositoryImports.get(1).getLabel(),
				is("ServerWithoutPage1"));
		assertThat(repositoryImports.get(1).hasFixLocation(), is(true));
		assertThat(repositoryImports.get(1).getRepositoryServerProvider(),
				is(TestRepositoryServerProvider.class));
		assertThat(repositoryImports.get(2).getLabel(), is("TestServer"));
		assertThat(repositoryImports.get(2).hasFixLocation(), is(false));
		assertThat(repositoryImports.get(2).getRepositoryServerProvider(),
				is(TestRepositoryServerProvider.class));
		assertThat(repositoryImports.get(2).getRepositoryImportPage(),
				is(TestRepositorySearchPage.class));
		assertThat(repositoryImports.get(3).getLabel(),
				is("ServerWithoutPage2"));
		assertThat(repositoryImports.get(3).hasFixLocation(), is(false));
	}
}
