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
package org.eclipse.egit.ui.internal.provisional.wizards;

import java.net.URI;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as part
 * of a work in progress. There is no guarantee that this API will work or that
 * it will remain the same. Please do not use this API without consulting with
 * the <Your Team Name> team.
 * </p>
 *
 * Contains info of a server which hosts git repositories.
 */
public class RepositoryServerInfo {

	private String label;

	private URI uri;

	private UserPasswordCredentials credentials;

	/**
	 * @param label
	 *            the human readable label of the repository server to be shown
	 *            in the UI
	 * @param uri
	 *            the URI of the repository server
	 * @param credentials
	 *            the credentials needed for log in, may be null
	 */
	public RepositoryServerInfo(String label, URI uri, UserPasswordCredentials credentials) {
		this.label = label;
		this.uri = uri;
		this.credentials = credentials;
	}

	/**
	 * @return label the human readable label of the repository server to be
	 *         shown in the UI
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the URI of the repository server which can be used for queries
	 *         for repositories
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @return the credentials needed for log in
	 */
	public UserPasswordCredentials getCredentials() {
		return credentials;
	}

}
