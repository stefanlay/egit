/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - implementation of getWorkbenchPage
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.action;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;
import static org.eclipse.ui.PlatformUI.getWorkbench;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCacheFile;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Action to open a compare editor from a SyncInfo object.
 */
public class GitOpenInCompareAction extends Action {

	private final Action oldAction;

	private final ISynchronizePageConfiguration conf;

	/**
	 * @param configuration
	 * @param oldAction
	 */
	public GitOpenInCompareAction(ISynchronizePageConfiguration configuration,
			Action oldAction) {
		this.conf = configuration;
		this.oldAction = oldAction;
	}

	public void run() {
		ISelection selection = conf.getSite().getSelectionProvider()
				.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = ((IStructuredSelection) selection);
			boolean reuseEditor = sel.size() == 1;
			for (Iterator iterator = sel.iterator(); iterator.hasNext();) {
				Object obj = iterator.next();
				if (obj instanceof GitModelBlob)
					handleGitObjectComparison((GitModelBlob) obj, reuseEditor);
				else {
					oldAction.run();
					// we assume here that oldAction is instance of
					// org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction
					// with in it's run method will also iterate over selected
					// resources therefore we need to brake this loop when we
					// meet not know object
					break;
				}
			}
		}
	}

	private void handleGitObjectComparison(GitModelBlob obj, boolean reuseEditor) {
		ITypedElement left;
		ITypedElement right;
		IFile file = (IFile) obj.getResource();
		if (obj instanceof GitModelWorkingFile) {
			if (file.getLocation() == null)
				left = new LocalNonWorkspaceTypedElement(file.getFullPath().toString());
			else
				left= SaveableCompareEditorInput.createFileElement(file);
			right = getCachedFileElement(file);
		} else if (obj instanceof GitModelCacheFile) {
			left = getCachedFileElement(file);
			right = getHeadFileElement(obj);
		} else {
			oldAction.run();
			return;
		}

		GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
				left, right, null);

		IWorkbenchPage page = getWorkbenchPage(conf.getSite());
		OpenInCompareAction.openCompareEditor(in, page, reuseEditor);
	}

	private static IWorkbenchPage getWorkbenchPage(ISynchronizePageSite site) {
		IWorkbenchPage page = null;
		if (site == null || site.getWorkbenchSite() == null) {
			IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
			if (window != null)
				page = window.getActivePage();
		} else
			page = site.getWorkbenchSite().getPage();

		return page;
	}

	private ITypedElement getCachedFileElement(IFile file) {
		try {
			return CompareUtils.getHeadTypedElement(file);
		} catch (IOException e) {
			return null;
		}
	}

	private ITypedElement getHeadFileElement(GitModelBlob blob) {
		Repository repo = blob.getRepository();
		String gitPath = stripWorkDir(repo.getWorkTree(), blob.getLocation().toFile());

		return CompareUtils.getFileRevisionTypedElement(gitPath, blob.getBaseCommit(), repo);
	}

}
