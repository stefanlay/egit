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

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as part
 * of a work in progress. There is no guarantee that this API will work or that
 * it will remain the same. Please do not use this API without consulting with
 * the <Your Team Name> team.
 * </p>
 *
 * Encapsulates info of a git repository
 */
public class GitRepositoryInfo {

	private final String cloneUri;
	private final UserPasswordCredentials credentials;

	/**
	 * @param cloneUri
	 *            the URI where the repository can be cloned from
	 * @param credentials
	 *            the credentials needed for log in, may be null
	 */
	public GitRepositoryInfo(String cloneUri, UserPasswordCredentials credentials) {
		this.cloneUri = cloneUri;
		this.credentials = credentials;
	}

	/**
	 * @return the URI where the repository can be cloned from
	 */
	public String getCloneUri() {
		return cloneUri;
	}

	/**
	 * @return the credentials needed for log in
	 */
	public UserPasswordCredentials getCredentials() {
		return credentials;
	}
}
