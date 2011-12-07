package org.eclipse.egit.ui.internal.provisional.wizards;

import java.net.URI;

/**
 * @author d044495
 *
 */
public class RepositoryServerInfo {

	private String label;

	private URI uri;

	/**
	 * @param label
	 * @param uri
	 */
	public RepositoryServerInfo(String label, URI uri) {
		this.label = label;
		this.uri = uri;
	}

	/**
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return uri
	 */
	public URI getUri() {
		return uri;
	}
}
