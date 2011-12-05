package org.eclipse.egit.ui;

import java.net.URI;
import java.util.Map;

/**
 * @author d044495
 *
 */
public interface IRepositoryServerProvider {

	/**
	 * @return List of servers
	 */
	public Map<String, URI> getRepositoryServerURIs();
}
