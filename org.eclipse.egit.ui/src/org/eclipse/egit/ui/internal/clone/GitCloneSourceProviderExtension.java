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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositoryServerProvider;
import org.eclipse.jface.wizard.WizardPage;

/**
 * Provides access to the extensions of the cloneSourceProvider extension point
 */
public class GitCloneSourceProviderExtension {

	private static final String CLONE_SOURCE_PROVIDER_ID = "org.eclipse.egit.ui.cloneSourceProvider"; //$NON-NLS-1$

	/**
	 * @return the list of {@code CloneSourceProvider} read from the extension
	 *         point registry
	 */
	public static List<CloneSourceProvider> getCloneSourceProvider() {
		List<CloneSourceProvider> cloneSourceProvider = new ArrayList<CloneSourceProvider>();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry
				.getConfigurationElementsFor(CLONE_SOURCE_PROVIDER_ID);
		if (config.length > 0) {
			addCloneSourceProvider(cloneSourceProvider, config, 0);
		}

		return cloneSourceProvider;
	}

	private static void addCloneSourceProvider(
			List<CloneSourceProvider> cloneSourceProvider,
			IConfigurationElement[] config, int index) {
		int myIndex = index;
		String label = config[myIndex].getAttribute("label"); //$NON-NLS-1$
		boolean hasFixLocation = Boolean.valueOf(
				config[myIndex].getAttribute("hasFixLocation")).booleanValue(); //$NON-NLS-1$
		myIndex++;
		IConfigurationElement serverProviderElement = null;
		if (myIndex < config.length
				&& config[myIndex].getName().equals("repositoryServerProvider")) { //$NON-NLS-1$
			serverProviderElement = config[myIndex];
			myIndex++;
		}
		IConfigurationElement pageElement = null;
		if (myIndex < config.length
				&& config[myIndex].getName().equals("repositoryImportPage")) { //$NON-NLS-1$
			pageElement = config[myIndex];
			myIndex++;
		}
		cloneSourceProvider.add(new CloneSourceProvider(label,
				serverProviderElement, pageElement, hasFixLocation));
		if (myIndex == config.length)
			return;
		addCloneSourceProvider(cloneSourceProvider, config, myIndex);
	}

	/**
	 * Encapsulates a clone source provided by an extension of the extension
	 * point "org.eclipse.egit.ui.cloneSourceProvider"
	 */
	public static class CloneSourceProvider {

		/**
		 * The constant provider for used for local repositories
		 */
		public static final CloneSourceProvider LOCAL = new CloneSourceProvider(
				"Local", null, null, true); //$NON-NLS-1$

		private String label;

		private IConfigurationElement repositoryServerProviderElement;

		private IConfigurationElement repositoryImportPageELement;

		private boolean hasFixLocation = false;

		private CloneSourceProvider(String label,
				IConfigurationElement repositoryServerProviderElement,
				IConfigurationElement repositoryImportPageElement,
				boolean hasFixLocation) {
			this.label = label;
			this.repositoryServerProviderElement = repositoryServerProviderElement;
			this.repositoryImportPageELement = repositoryImportPageElement;
			this.hasFixLocation = hasFixLocation;
		}

		/**
		 * @return label the human readable name of a type of servers which
		 *         contains repositories
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * @return a class which Provides a list of servers which host git
		 *         repositories. This class is newly created on each invocation
		 *         of this method. A client is responsible of caching this
		 *         class.
		 * @throws CoreException
		 */
		public IRepositoryServerProvider getRepositoryServerProvider()
				throws CoreException {
			if (repositoryServerProviderElement == null)
				return null;
			Object object = repositoryServerProviderElement
					.createExecutableExtension("class"); //$NON-NLS-1$
			IRepositoryServerProvider provider = null;
			if (object instanceof IRepositoryServerProvider) {
				provider = (IRepositoryServerProvider) object;
			}
			return provider;
		}

		/**
		 * @return A wizard page which can can return information of a git
		 *         repository. This class is newly created on each invocation of
		 *         this method. A client is responsible of caching this class.
		 * @throws CoreException
		 */
		public WizardPage getRepositoryImportPage() throws CoreException {
			if (repositoryImportPageELement == null)
				return null;
			Object object = repositoryImportPageELement
					.createExecutableExtension("class"); //$NON-NLS-1$
			WizardPage page = null;
			if (object instanceof WizardPage
					&& object instanceof IRepositorySearchResult) {
				page = (WizardPage) object;
			}
			return page;
		}

		/**
		 * @return true if there will be no ability to add different servers of
		 *         this type. The provided repositoryImportPage has to know
		 *         where to look for the repositories.
		 */
		public boolean hasFixLocation() {
			return hasFixLocation;
		}
	}

}
