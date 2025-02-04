/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;

/**
 * Implements "Open" (double-click).
 * <p>
 * On a file, this delegates "Open in Text Editor", while on a branch or tag, it
 * is doing "Checkout".
 */
public class OpenCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final List<RepositoryTreeNode> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;

		final RepositoryTreeNode node = nodes.get(0);

		if (node instanceof RefNode || node instanceof TagNode)
			return new CheckoutCommand().execute(event);
		if (node instanceof FileNode)
			return new OpenInEditorCommand().execute(event);

		return null;
	}
}
