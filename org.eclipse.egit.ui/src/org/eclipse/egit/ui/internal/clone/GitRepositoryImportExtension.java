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
 * @author d044495
 *
 */
public class GitRepositoryImportExtension {

	private static final String REPOSITORY_IMPORT_PROVIDER_ID = "org.eclipse.egit.ui.repositoryImportSource"; //$NON-NLS-1$
//	private static final String REPOSITORY_IMPORT_PROVIDER_ID = "org.eclipse.egit.ui.repositoryImportSource"; //$NON-NLS-1$

	/**
	 * @return repositoryImports
	 * @throws CoreException
	 */
	public static List<RepositoryImport> getRepositoryImports() throws CoreException {
		List<RepositoryImport> repositoryImports = new ArrayList<RepositoryImport>();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry
				.getConfigurationElementsFor(REPOSITORY_IMPORT_PROVIDER_ID);
		if (config.length > 0) {
			addImport(repositoryImports, config, 0);
		}

		return repositoryImports;
	}

	private static void addImport(List<RepositoryImport> repositoryImports,
			IConfigurationElement[] config, int index) {
		int myIndex = index;
		String label = config[myIndex].getAttribute("label"); //$NON-NLS-1$
		boolean hasFixLocation = Boolean.valueOf(config[myIndex].getAttribute("hasFixLocation")).booleanValue(); //$NON-NLS-1$
		myIndex++;
		IConfigurationElement serverProviderElement = null;
		if (myIndex < config.length && config[myIndex].getName().equals("repositoryServerProvider")) { //$NON-NLS-1$
			serverProviderElement = config[myIndex];
			myIndex++;
		}
		IConfigurationElement pageElement = null;
		if (myIndex < config.length && config[myIndex].getName().equals("repositoryImportPage")) { //$NON-NLS-1$
			pageElement = config[myIndex];
			myIndex++;
		}
		repositoryImports.add(new RepositoryImport(label, serverProviderElement, pageElement, hasFixLocation));
		if (myIndex == config.length)
			return;
		addImport(repositoryImports, config, myIndex);
	}

	/**
	 * @author d044495
	 *
	 */
	public static class RepositoryImport {

		/**
		 *
		 */
		public static RepositoryImport LOCAL = new RepositoryImport("Local", null, null, true); //$NON-NLS-1$

		private String label;
		private IConfigurationElement repositoryServerProviderElement;
		private IConfigurationElement repositoryImportPageELement;
		private boolean hasFixLocation = false;


		/**
		 * @param label
		 * @param repositoryServerProviderElement
		 * @param repositoryImportPageElement
		 * @param hasFixLocation
		 */
		public RepositoryImport(String label, IConfigurationElement repositoryServerProviderElement,
				IConfigurationElement repositoryImportPageElement, boolean hasFixLocation) {
			this.label = label;
			this.repositoryServerProviderElement = repositoryServerProviderElement;
			this.repositoryImportPageELement = repositoryImportPageElement;
			this.hasFixLocation = hasFixLocation;
		}

		/**
		 * @return label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * @return repositoryServerProvider
		 * @throws CoreException
		 */
		public IRepositoryServerProvider getRepositoryServerProvider() throws CoreException {
			if (repositoryServerProviderElement == null)
				return null;
			Object object = repositoryServerProviderElement.createExecutableExtension("class"); //$NON-NLS-1$
			IRepositoryServerProvider provider = null;
			if (object instanceof IRepositoryServerProvider) {
				provider = (IRepositoryServerProvider)object;
			}
			return provider;
		}

		/**
		 * @return repositoryImportPage
		 * @throws CoreException
		 */
		public WizardPage getRepositoryImportPage() throws CoreException {
			if (repositoryImportPageELement == null)
				return null;
			Object object = repositoryImportPageELement.createExecutableExtension("class"); //$NON-NLS-1$
			WizardPage page = null;
			if (object instanceof WizardPage && object instanceof IRepositorySearchResult) {
				page = (WizardPage)object;
			}
			return page;
		}

		/**
		 * @return multipleServers
		 */
		public boolean hasFixLocation() {
			return hasFixLocation;
		}
	}

}
