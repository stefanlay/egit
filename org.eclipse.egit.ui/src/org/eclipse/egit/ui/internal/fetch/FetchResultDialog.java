/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.core.op.FetchOperationResult;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog displaying result of fetch operation.
 */
public class FetchResultDialog extends TitleAreaDialog {
	private static final int CONFIGURE = 99;

	private final Repository localDb;

	private final FetchOperationResult result;

	private final String sourceString;

	private boolean hideConfigure;

	/**
	 * @param parentShell
	 * @param localDb
	 * @param result
	 * @param sourceString
	 */
	public FetchResultDialog(final Shell parentShell, final Repository localDb,
			final FetchOperationResult result, final String sourceString) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.localDb = localDb;
		this.result = result;
		this.sourceString = sourceString;
	}

	/**
	 * Shows this dialog asynchronously
	 *
	 * @param repository
	 * @param result
	 * @param sourceString
	 */
	public static void show(final Repository repository,
			final FetchResult result, final String sourceString) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getDisplay().asyncExec(
						new Runnable() {
							public void run() {
								Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								new FetchResultDialog(shell, repository,
										result, sourceString).open();
							}
						});
			}
		});
	}

	/**
	 * @param parentShell
	 * @param localDb
	 * @param result
	 * @param sourceString
	 */
	public FetchResultDialog(final Shell parentShell, final Repository localDb,
			final FetchResult result, final String sourceString) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.localDb = localDb;
		this.result = new FetchOperationResult(result.getURI(), result);
		this.sourceString = sourceString;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		if (!hideConfigure
				&& SimpleConfigureFetchDialog.getConfiguredRemote(localDb) != null)
			createButton(parent, CONFIGURE,
					UIText.FetchResultDialog_ConfigureButton, false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == CONFIGURE) {
			super.buttonPressed(IDialogConstants.OK_ID);
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					Dialog dlg = SimpleConfigureFetchDialog.getDialog(
							PlatformUI.getWorkbench().getDisplay()
									.getActiveShell(), localDb);
					dlg.open();
				}
			});
		}
	}

	@Override
	public Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);

		setTitle(NLS.bind(UIText.FetchResultDialog_labelNonEmptyResult,
				sourceString));

		if (result.getErrorMessage() != null)
			setErrorMessage(result.getErrorMessage());
		else if (result.getFetchResult() != null
				&& result.getFetchResult().getTrackingRefUpdates().isEmpty()) {
			setMessage(NLS.bind(UIText.FetchResultDialog_labelEmptyResult,
					sourceString));
		}

		createFetchResultTable(composite);

		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Create fetch result table under given parent composite
	 *
	 * @param parent
	 * @return main result table control
	 */
	public Control createFetchResultTable(Composite parent) {
		final FetchResultTable table = new FetchResultTable(parent);
		if (result.getFetchResult() != null)
			table.setData(localDb, result.getFetchResult());
		final Control tableControl = table.getControl();
		GridDataFactory.fillDefaults().grab(true, true).hint(600, 300)
				.applyTo(tableControl);
		return table.getControl();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell
				.setText(NLS.bind(UIText.FetchResultDialog_title, sourceString));
	}

	/**
	 * @param show
	 */
	public void showConfigureButton(boolean show) {
		this.hideConfigure = !show;
	}

	protected IDialogSettings getDialogBoundsSettings() {
		return UIUtils.getDialogBoundSettings(getClass());
	}
}
