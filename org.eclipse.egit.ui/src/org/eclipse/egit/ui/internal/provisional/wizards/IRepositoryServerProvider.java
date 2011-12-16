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

import java.util.Collection;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as part
 * of a work in progress. There is no guarantee that this API will work or that
 * it will remain the same. Please do not use this API without consulting with
 * the <Your Team Name> team.
 * </p>
 *
 * Provides info about servers which host git repositories.
 */
public interface IRepositoryServerProvider {

	/**
	 * @return List of server infos
	 */
	public Collection<RepositoryServerInfo> getRepositoryServerInfos();
}
