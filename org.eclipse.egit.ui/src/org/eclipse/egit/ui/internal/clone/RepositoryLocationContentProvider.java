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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositoryServerProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class RepositoryLocationContentProvider implements ITreeContentProvider {

	private Map<RepositoryServerInfo, CloneSourceProvider> parents = new HashMap<RepositoryServerInfo, CloneSourceProvider>();

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public boolean hasChildren(Object element) {
		Object[] children = calculateChildren(element);
		return children != null && children.length > 0;
	}

	public Object getParent(Object element) {
		if (element instanceof RepositoryServerInfo)
			return parents.get(element);
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		List<CloneSourceProvider> repositoryImports = (List<CloneSourceProvider>) inputElement;
		return repositoryImports.toArray(new CloneSourceProvider[repositoryImports
				.size()]);
	}

	public Object[] getChildren(Object parentElement) {
		return calculateChildren(parentElement);
	}

	private Object[] calculateChildren(Object parentElement) {
		if (parentElement instanceof CloneSourceProvider) {
			CloneSourceProvider repositoryImport = (CloneSourceProvider) parentElement;
			if (repositoryImport.hasFixLocation())
				return null;
			Collection<RepositoryServerInfo> repositoryServerInfos = getRepositoryServerInfos(repositoryImport);
			if (repositoryServerInfos == null)
				return null;
			cacheParents(repositoryImport, repositoryServerInfos);
			return repositoryServerInfos
					.toArray(new RepositoryServerInfo[repositoryServerInfos
							.size()]);
		}
		return null;
	}

	private Collection<RepositoryServerInfo> getRepositoryServerInfos(
			CloneSourceProvider repositoryImport) {
		Collection<RepositoryServerInfo> repositoryServerInfos = null;
		IRepositoryServerProvider repositoryServerProvider;
		try {
			repositoryServerProvider = repositoryImport
					.getRepositoryServerProvider();
		} catch (CoreException e) {
			Activator.error(e.getLocalizedMessage(), e);
			return null;
		}
		if (repositoryServerProvider == null)
			return null;
		try {
			repositoryServerInfos = repositoryServerProvider
					.getRepositoryServerInfos();
		} catch (RuntimeException e) {
			Activator.error("Error on providing repository server infos", e); //$NON-NLS-1$
		}
		return repositoryServerInfos;
	}

	private void cacheParents(CloneSourceProvider repositoryImport,
			Collection<RepositoryServerInfo> repositoryServerInfos) {
		for (RepositoryServerInfo info : repositoryServerInfos) {
			parents.put(info, repositoryImport);
		}
	}

}
