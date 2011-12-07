package org.eclipse.egit.ui.internal.provisional.wizards;

import org.eclipse.jgit.transport.URIish;

/**
 * @author d044495
 *
 */
public class GitRepositoryInfo {

	private final URIish uri;

	/**
	 * @param uri
	 */
	public GitRepositoryInfo(URIish uri) {
		this.uri = uri;
	}

	/**
	 * @return uri
	 */
	public URIish getUri() {
		return uri;
	}
}
