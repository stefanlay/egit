package org.eclipse.egit.ui.internal.provisional.wizards;

import java.util.Collection;

/**
 * @author d044495
 *
 */
public interface IRepositoryServerProvider {

	/**
	 * @return List of servers
	 */
	public Collection<RepositoryServerInfo> getRepositoryServerInfos();
}
