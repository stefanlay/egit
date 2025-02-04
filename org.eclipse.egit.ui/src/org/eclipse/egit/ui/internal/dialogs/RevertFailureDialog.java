/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.WorkbenchStyledLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Dialog to display when a revert fails
 */
public class RevertFailureDialog extends MessageDialog {

	/**
	 * Show dialog for failure result
	 *
	 * @param shell
	 * @param commit
	 * @param result
	 */
	public static void show(Shell shell, RevCommit commit, MergeResult result) {
		String message = MessageFormat
				.format(UIText.RevertFailureDialog_Message, commit
						.abbreviate(7).name());
		RevertFailureDialog dialog = new RevertFailureDialog(shell, message,
				result);
		dialog.setShellStyle(dialog.getShellStyle() | SWT.SHEET | SWT.RESIZE);
		dialog.open();
	}

	private static class Path extends WorkbenchAdapter {

		private final String path;

		private Path(String path) {
			this.path = path;
		}

		public String getLabel(Object object) {
			return path;
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			String name = new org.eclipse.core.runtime.Path(path).lastSegment();
			if (name != null) {
				return PlatformUI.getWorkbench().getEditorRegistry()
						.getImageDescriptor(name);
			} else
				return PlatformUI.getWorkbench().getSharedImages()
						.getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
		}

		public StyledString getStyledText(Object object) {
			int lastSlash = path.lastIndexOf('/');
			StyledString styled = new StyledString();
			if (lastSlash != -1 && lastSlash + 1 < path.length()) {
				String name = path.substring(lastSlash + 1);
				styled.append(name).append(' ');
				styled.append("- ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				styled.append(path.substring(0, lastSlash),
						StyledString.QUALIFIER_STYLER);
			} else
				styled.append(path);
			return styled;
		}
	}

	private static class RevertFailure extends WorkbenchAdapter {

		private final MergeFailureReason reason;

		private final List<Path> paths;

		private RevertFailure(MergeFailureReason reason) {
			this.reason = reason;
			this.paths = new ArrayList<Path>();
		}

		private RevertFailure add(String path) {
			paths.add(new Path(path));
			return this;
		}

		public Object[] getChildren(Object object) {
			return paths.toArray();
		}

		public String getLabel(Object object) {
			switch (reason) {
			case DIRTY_INDEX:
				return UIText.RevertFailureDialog_ReasonChangesInIndex;
			case DIRTY_WORKTREE:
				return UIText.RevertFailureDialog_ReasonChangesInWorkingDirectory;
			case COULD_NOT_DELETE:
				return UIText.RevertFailureDialog_ReasonDeleteFailure;
			default:
				return super.getLabel(object);
			}
		}

		public StyledString getStyledText(Object object) {
			StyledString styled = new StyledString(getLabel(object));
			styled.append(' ');
			styled.append(MessageFormat.format("({0})", //$NON-NLS-1$
					Integer.valueOf(paths.size())), StyledString.COUNTER_STYLER);
			return styled;
		}
	}

	private final MergeResult result;

	/**
	 * Create dialog for merge result
	 *
	 * @param shell
	 * @param message
	 * @param result
	 */
	public RevertFailureDialog(Shell shell, String message, MergeResult result) {
		super(shell, UIText.RevertFailureDialog_Title, null, message, ERROR,
				new String[] { IDialogConstants.OK_LABEL }, 0);
		this.result = result;
	}

	protected Control createCustomArea(Composite parent) {
		Map<String, MergeFailureReason> reasons = result.getFailingPaths();
		if (reasons == null || reasons.isEmpty())
			return null;

		Composite fileArea = new Composite(parent, SWT.NONE);

		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 80)
				.applyTo(fileArea);
		GridLayoutFactory.fillDefaults().applyTo(fileArea);
		TreeViewer viewer = new TreeViewer(fileArea);
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(viewer.getControl());
		viewer.setContentProvider(new WorkbenchContentProvider() {

			public Object[] getElements(Object element) {
				return ((Collection) element).toArray();
			}

		});
		final IStyledLabelProvider styleProvider = new WorkbenchStyledLabelProvider() {

			public StyledString getStyledText(Object element) {
				// TODO Replace with use of IWorkbenchAdapter3 when is no longer
				// supported
				if (element instanceof RevertFailure)
					return ((RevertFailure) element).getStyledText(element);
				if (element instanceof Path)
					return ((Path) element).getStyledText(element);

				return super.getStyledText(element);
			}
		};
		viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				styleProvider));
		viewer.setSorter(new ViewerSorter());

		Map<MergeFailureReason, RevertFailure> failures = new HashMap<MergeFailureReason, RevertFailure>();
		for (Entry<String, MergeFailureReason> reason : reasons.entrySet()) {
			RevertFailure failure = failures.get(reason.getValue());
			if (failure == null) {
				failure = new RevertFailure(reason.getValue());
				failures.put(reason.getValue(), failure);
			}
			failure.add(reason.getKey());
		}
		viewer.setInput(failures.values());

		return fileArea;
	}
}
