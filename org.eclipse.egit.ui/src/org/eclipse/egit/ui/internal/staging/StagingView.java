/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.eclipse.egit.ui.internal.CommonUtils.runCommand;
import static org.eclipse.ui.ISources.ACTIVE_MENU_SELECTION_NAME;
import static org.eclipse.ui.menus.CommandContributionItem.STYLE_PUSH;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentState;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentStateManager;
import org.eclipse.egit.ui.internal.dialogs.ICommitMessageComponentNotifications;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.Team;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * A GitX style staging view with embedded commit dialog.
 */
public class StagingView extends ViewPart {

	/**
	 * Staging view id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.StagingView"; //$NON-NLS-1$

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private Form form;

	private Section stagedSection;

	private Section unstagedSection;

	private TableViewer stagedTableViewer;

	private TableViewer unstagedTableViewer;

	private SpellcheckableMessageArea commitMessageText;

	private Text committerText;

	private Text authorText;

	private Action commitAction;

	private CommitMessageComponent commitMessageComponent;

	private boolean reactOnSelection = true;

	private final List<ListenerHandle> myListeners = new LinkedList<ListenerHandle>();

	private ISelectionListener selectionChangedListener;

	private IResourceChangeListener resourceChangeListener;

	private Repository currentRepository;

	static class StagingViewUpdate {
		Repository repository;
		IndexDiff indexDiff;
		Collection<String> changedResources;

		StagingViewUpdate(Repository theRepository,
				IndexDiff theIndexDiff, Collection<String> theChanges) {
			this.repository = theRepository;
			this.indexDiff = theIndexDiff;
			this.changedResources = theChanges;
		}
	}

	static class StagingDragListener extends DragSourceAdapter {

		private ISelectionProvider provider;

		public StagingDragListener(ISelectionProvider provider) {
			this.provider = provider;
		}

		public void dragStart(DragSourceEvent event) {
			event.doit = !provider.getSelection().isEmpty();
		}

		public void dragFinished(DragSourceEvent event) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(
					event.dataType))
				LocalSelectionTransfer.getTransfer().setSelection(null);
		}

		public void dragSetData(DragSourceEvent event) {
			IStructuredSelection selection = (IStructuredSelection) provider
					.getSelection();
			if (selection.isEmpty())
				return;

			if (LocalSelectionTransfer.getTransfer().isSupportedType(
					event.dataType)) {
				LocalSelectionTransfer.getTransfer().setSelection(selection);
				return;
			}

			if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
				List<String> files = new ArrayList<String>();
				for (Object selected : selection.toList())
					if (selected instanceof StagingEntry) {
						StagingEntry entry = (StagingEntry) selected;
						File file = new File(
								entry.getRepository().getWorkTree(),
								entry.getPath());
						if (file.exists())
							files.add(file.getAbsolutePath());
					}
				if (!files.isEmpty()) {
					event.data = files.toArray(new String[files.size()]);
					return;
				}
			}
		}
	}

	/**
	 * Bit-mask describing interesting changes for IResourceChangeListener
	 * events
	 */
	private static int INTERESTING_CHANGES = IResourceDelta.CONTENT
			| IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
			| IResourceDelta.OPEN | IResourceDelta.REPLACED
			| IResourceDelta.TYPE;

	private final RefsChangedListener myRefsChangedListener = new RefsChangedListener() {
		public void onRefsChanged(RefsChangedEvent event) {
			// refs change when files are committed, we naturally want to remove
			// committed files from the view
			reload(event.getRepository());
		}
	};

	private final IndexChangedListener myIndexChangedListener = new IndexChangedListener() {
		public void onIndexChanged(IndexChangedEvent event) {
			reload(event.getRepository());
		}
	};

	private final IPreferenceChangeListener prefListener = new IPreferenceChangeListener() {

		public void preferenceChange(PreferenceChangeEvent event) {
			if (!RepositoryUtil.PREFS_DIRECTORIES.equals(event.getKey()))
				return;

			final Repository repo = currentRepository;
			if (repo == null)
				return;

			if (Activator.getDefault().getRepositoryUtil().contains(repo))
				return;

			reload(null);
		}

	};

	private Action signedOffByAction;

	private Action addChangeIdAction;

	private Action amendPreviousCommitAction;

	private Action openNewCommitsAction;

	private Action columnLayoutAction;

	private Action fileNameModeAction;

	private Action refreshAction;

	private SashForm stagingSashForm;

	private Job reloadJob;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});

		form = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(form, repoImage);
		form.setImage(repoImage);
		form.setText(UIText.StagingView_NoSelectionTitle);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.swtDefaults().applyTo(form.getBody());

		SashForm horizontalSashForm = new SashForm(form.getBody(), SWT.NONE);
		toolkit.adapt(horizontalSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(horizontalSashForm);

		stagingSashForm = new SashForm(horizontalSashForm,
				getStagingFormOrientation());
		toolkit.adapt(stagingSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(stagingSashForm);

		unstagedSection = toolkit.createSection(stagingSashForm,
				ExpandableComposite.TITLE_BAR);

		Composite unstagedTableComposite = toolkit
				.createComposite(unstagedSection);
		toolkit.paintBordersFor(unstagedTableComposite);
		unstagedSection.setClient(unstagedTableComposite);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(unstagedTableComposite);

		unstagedTableViewer = new TableViewer(toolkit.createTable(
				unstagedTableComposite, SWT.FULL_SELECTION | SWT.MULTI));
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(unstagedTableViewer.getControl());
		unstagedTableViewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		unstagedTableViewer.getTable().setLinesVisible(true);
		unstagedTableViewer.setLabelProvider(createLabelProvider());
		unstagedTableViewer.setContentProvider(new StagingViewContentProvider(
				true));
		unstagedTableViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY
				| DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer(),
						FileTransfer.getInstance() }, new StagingDragListener(
						unstagedTableViewer));
		unstagedTableViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) stagedTableViewer
								.getSelection();
						unstage(selection);
					}

					public void dragOver(DropTargetEvent event) {
						event.detail = DND.DROP_MOVE;
					}
				});
		unstagedTableViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				compareWith(event);
			}
		});

		Section commitMessageSection = toolkit.createSection(
				horizontalSashForm, ExpandableComposite.TITLE_BAR);
		commitMessageSection.setText(UIText.StagingView_CommitMessage);

		Composite commitMessageComposite = toolkit
				.createComposite(commitMessageSection);
		toolkit.paintBordersFor(commitMessageComposite);
		commitMessageSection.setClient(commitMessageComposite);
		GridLayoutFactory.fillDefaults().numColumns(1)
				.extendedMargins(2, 2, 2, 2).applyTo(commitMessageComposite);

		commitMessageText = new SpellcheckableMessageArea(
				commitMessageComposite, EMPTY_STRING, toolkit.getBorderStyle());
		commitMessageText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageText);

		Composite composite = toolkit.createComposite(commitMessageComposite);
		toolkit.paintBordersFor(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(composite);

		toolkit.createLabel(composite, UIText.StagingView_Author)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		authorText = toolkit.createText(composite, null);
		authorText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		authorText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		toolkit.createLabel(composite, UIText.StagingView_Committer)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		committerText = toolkit.createText(composite, null);
		committerText.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		committerText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		stagedSection = toolkit.createSection(stagingSashForm,
				ExpandableComposite.TITLE_BAR);
		Composite stagedTableComposite = toolkit.createComposite(stagedSection);
		toolkit.paintBordersFor(stagedTableComposite);
		stagedSection.setClient(stagedTableComposite);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(stagedTableComposite);

		stagedTableViewer = new TableViewer(toolkit.createTable(
				stagedTableComposite, SWT.FULL_SELECTION | SWT.MULTI));
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(stagedTableViewer.getControl());
		stagedTableViewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		stagedTableViewer.getTable().setLinesVisible(true);
		stagedTableViewer.setLabelProvider(createLabelProvider());
		stagedTableViewer.setContentProvider(new StagingViewContentProvider(
				false));
		stagedTableViewer.addDragSupport(
				DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK,
				new Transfer[] { LocalSelectionTransfer.getTransfer(),
						FileTransfer.getInstance() }, new StagingDragListener(
						stagedTableViewer));
		stagedTableViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) unstagedTableViewer
								.getSelection();
						stage(selection);
					}

					public void dragOver(DropTargetEvent event) {
						event.detail = DND.DROP_MOVE;
					}
				});
		stagedTableViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				compareWith(event);
			}
		});

		selectionChangedListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!reactOnSelection || part == getSite().getPart())
					return;

				// this may happen if we switch between editors
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput)
						reactOnSelection(new StructuredSelection(
								((IFileEditorInput) input).getFile()));
				} else
					reactOnSelection(selection);
			}
		};

		IPreferenceStore preferenceStore = getPreferenceStore();
		if (preferenceStore.contains(UIPreferences.STAGING_VIEW_SYNC_SELECTION))
			reactOnSelection = preferenceStore.getBoolean(
					UIPreferences.STAGING_VIEW_SYNC_SELECTION);
		else
			preferenceStore.setDefault(UIPreferences.STAGING_VIEW_SYNC_SELECTION, true);

		new InstanceScope().getNode(
				org.eclipse.egit.core.Activator.getPluginId())
				.addPreferenceChangeListener(prefListener);

		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				final Collection<String> resourcesToUpdate = new HashSet<String>();

				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta) throws CoreException {
							// If the file has changed but not in a way that we care
							// about (e.g. marker changes to files) then ignore
							if (delta.getKind() == IResourceDelta.CHANGED
									&& (delta.getFlags() & INTERESTING_CHANGES) == 0)
								return true;

							final IResource resource = delta.getResource();

							// skip any non-FILE resources
							if (resource.getType() != IResource.FILE)
								return true;

							// If the resource is not part of a project under Git
							// revision control
							final RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
							if (mapping == null || mapping.getRepository() != currentRepository)
								// Ignore the change
								return true;

							// Don't include ignored resources
							if (Team.isIgnoredHint(resource))
								return false;

							String repoRelativePath = mapping.getRepoRelativePath(resource);
							resourcesToUpdate.add(repoRelativePath);

							return true;
						}
					});
				} catch (CoreException e) {
					MessageDialog.openError(getSite().getShell(),
							UIText.StagingView_exceptionTitle,
							UIText.StagingView_exceptionMessage);
				}

				if (!resourcesToUpdate.isEmpty()) {
					final IndexDiff indexDiff;
					try {
						WorkingTreeIterator iterator = new FileTreeIterator(
								currentRepository);
						indexDiff = new IndexDiff(currentRepository, Constants.HEAD, iterator);
						indexDiff.setFilter(PathFilterGroup.createFromStrings(resourcesToUpdate));
						indexDiff.diff();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					final StagingViewUpdate update = new StagingViewUpdate(currentRepository, indexDiff, resourcesToUpdate);
					asyncExec(new Runnable() {
						public void run() {
							if (form.isDisposed())
								return;

							unstagedTableViewer.setInput(update);
							stagedTableViewer.setInput(update);

							updateSectionText();
						}
					});
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
				IResourceChangeEvent.POST_CHANGE);

		updateSectionText();
		updateToolbar();
		enableCommitWidgets(false);

		createPopupMenu(unstagedTableViewer);
		createPopupMenu(stagedTableViewer);

		final ICommitMessageComponentNotifications listener = new ICommitMessageComponentNotifications() {

			public void updateSignedOffToggleSelection(boolean selection) {
				signedOffByAction.setChecked(selection);
			}

			public void updateChangeIdToggleSelection(boolean selection) {
				addChangeIdAction.setChecked(selection);
			}
		};
		commitMessageComponent = new CommitMessageComponent(listener);
		commitMessageComponent.attachControls(commitMessageText, authorText,
				committerText);

		// react on selection changes
		IWorkbenchPartSite site = getSite();
		ISelectionService srv = (ISelectionService) site
				.getService(ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);

		// Use current selection to populate staging view
		ISelection selection = srv.getSelection();
		if (selection != null && !selection.isEmpty()) {
			IWorkbenchPart part = site.getPage().getActivePart();
			if (part != null)
				selectionChangedListener.selectionChanged(part, selection);
		}

		site.setSelectionProvider(unstagedTableViewer);
	}

	private int getStagingFormOrientation() {
		boolean columnLayout = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.STAGING_VIEW_COLUMN_LAYOUT);
		if (columnLayout)
			return SWT.HORIZONTAL;
		else
			return SWT.VERTICAL;
	}

	private void enableCommitWidgets(boolean enabled) {
		if (!enabled) {
			commitMessageText.setText(""); //$NON-NLS-1$
			committerText.setText(""); //$NON-NLS-1$
			authorText.setText(""); //$NON-NLS-1$
		}

		commitMessageText.setEnabled(enabled);
		committerText.setEnabled(enabled);
		authorText.setEnabled(enabled);
		refreshAction.setEnabled(enabled);
		amendPreviousCommitAction.setEnabled(enabled);
		signedOffByAction.setEnabled(enabled);
		addChangeIdAction.setEnabled(enabled);
		commitAction.setEnabled(enabled);
	}

	private void updateToolbar() {
		IToolBarManager toolbar = getViewSite().getActionBars()
				.getToolBarManager();

		refreshAction = new Action(UIText.StagingView_Refresh, IAction.AS_PUSH_BUTTON) {
			public void run() {
				reload(currentRepository);
			}
		};
		refreshAction.setImageDescriptor(UIIcons.ELCL16_REFRESH);
		toolbar.add(refreshAction);

		// link with selection
		Action linkSelectionAction = new BooleanPrefAction(
				(IPersistentPreferenceStore) getPreferenceStore(),
				UIPreferences.STAGING_VIEW_SYNC_SELECTION,
				UIText.StagingView_LinkSelection) {
			@Override
			public void apply(boolean value) {
				reactOnSelection = value;
			}
		};
		linkSelectionAction.setImageDescriptor(UIIcons.ELCL16_SYNCED);
		toolbar.add(linkSelectionAction);

		toolbar.add(new Separator());

		amendPreviousCommitAction = new Action(
				UIText.StagingView_Ammend_Previous_Commit, IAction.AS_CHECK_BOX) {

			public void run() {
				commitMessageComponent.setAmendingButtonSelection(isChecked());
			}
		};
		amendPreviousCommitAction.setImageDescriptor(UIIcons.AMEND_COMMIT);
		toolbar.add(amendPreviousCommitAction);

		signedOffByAction = new Action(UIText.StagingView_Add_Signed_Off_By,
				IAction.AS_CHECK_BOX) {

			public void run() {
				commitMessageComponent.setSignedOffButtonSelection(isChecked());
			}
		};
		signedOffByAction.setImageDescriptor(UIIcons.SIGNED_OFF);
		toolbar.add(signedOffByAction);

		addChangeIdAction = new Action(UIText.StagingView_Add_Change_ID,
				IAction.AS_CHECK_BOX) {

			public void run() {
				commitMessageComponent.setChangeIdButtonSelection(isChecked());
			}
		};
		addChangeIdAction.setImageDescriptor(UIIcons.GERRIT);
		toolbar.add(addChangeIdAction);

		toolbar.add(new Separator());

		commitAction = new Action(UIText.StagingView_Commit,
				IAction.AS_PUSH_BUTTON) {
			public void run() {
				commit();
			}
		};
		commitAction.setImageDescriptor(UIIcons.COMMIT);
		toolbar.add(commitAction);

		openNewCommitsAction = new Action(UIText.StagingView_OpenNewCommits,
				IAction.AS_CHECK_BOX) {

			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_SHOW_NEW_COMMITS, isChecked());
			}
		};
		openNewCommitsAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_SHOW_NEW_COMMITS));

		columnLayoutAction = new Action(UIText.StagingView_ColumnLayout,
				IAction.AS_CHECK_BOX) {

			public void run() {
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_COLUMN_LAYOUT, isChecked());
				stagingSashForm.setOrientation(isChecked() ? SWT.HORIZONTAL
						: SWT.VERTICAL);
			}
		};
		columnLayoutAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_COLUMN_LAYOUT));

		fileNameModeAction = new Action(UIText.StagingView_ShowFileNamesFirst,
				IAction.AS_CHECK_BOX) {

			public void run() {
				final boolean enable = isChecked();
				getLabelProvider(stagedTableViewer).setFileNameMode(enable);
				getLabelProvider(unstagedTableViewer).setFileNameMode(enable);
				stagedTableViewer.refresh();
				unstagedTableViewer.refresh();
				getPreferenceStore().setValue(
						UIPreferences.STAGING_VIEW_FILENAME_MODE, enable);
			}
		};
		fileNameModeAction.setChecked(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));

		IMenuManager dropdownMenu = getViewSite().getActionBars()
				.getMenuManager();
		dropdownMenu.add(openNewCommitsAction);
		dropdownMenu.add(columnLayoutAction);
		dropdownMenu.add(fileNameModeAction);
	}

	private IBaseLabelProvider createLabelProvider() {
		StagingViewLabelProvider baseProvider = new StagingViewLabelProvider();
		baseProvider.setFileNameMode(getPreferenceStore().getBoolean(
				UIPreferences.STAGING_VIEW_FILENAME_MODE));
		return new DelegatingStyledCellLabelProvider(baseProvider);
	}

	private IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private StagingViewLabelProvider getLabelProvider(ContentViewer viewer) {
		IBaseLabelProvider base = viewer.getLabelProvider();
		IStyledLabelProvider styled = ((DelegatingStyledCellLabelProvider) base)
				.getStyledStringProvider();
		return (StagingViewLabelProvider) styled;
	}

	private void updateSectionText() {
		Integer stagedCount = Integer.valueOf(stagedTableViewer.getTable()
				.getItemCount());
		stagedSection.setText(MessageFormat.format(
				UIText.StagingView_StagedChanges, stagedCount));
		Integer unstagedCount = Integer.valueOf(unstagedTableViewer.getTable()
				.getItemCount());
		unstagedSection.setText(MessageFormat.format(
				UIText.StagingView_UnstagedChanges, unstagedCount));
	}

	private void compareWith(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.isEmpty())
			return;
		StagingEntry stagingEntry = (StagingEntry) selection.getFirstElement();
		switch (stagingEntry.getState()) {
		case ADDED:
		case CHANGED:
		case REMOVED:
			runCommand(ActionCommands.COMPARE_INDEX_WITH_HEAD_ACTION, selection);
			break;

		case MISSING:
		case MODIFIED:
		case PARTIALLY_MODIFIED:
		case CONFLICTING:
		case UNTRACKED:
		default:
			// compare with index
			runCommand(ActionCommands.COMPARE_WITH_INDEX_ACTION, selection);
		}
	}

	private void createPopupMenu(final TableViewer tableViewer) {
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		Control control = tableViewer.getControl();
		control.setMenu(menuMgr.createContextMenu(control));
		menuMgr.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				if (selection.isEmpty())
					return;

				menuMgr.add(new Action(
						UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel) {
							@Override
							public void run() {
								openSelectionInEditor(tableViewer.getSelection());
							}
						});

				StagingEntry stagingEntry = (StagingEntry) selection.getFirstElement();
				switch (stagingEntry.getState()) {
				case ADDED:
				case CHANGED:
				case REMOVED:
					menuMgr.add(new Action(UIText.StagingView_UnstageItemMenuLabel) {
						@Override
						public void run() {
							unstage((IStructuredSelection) tableViewer.getSelection());
						}
					});
					break;

				case CONFLICTING:
					menuMgr.add(createItem(ActionCommands.MERGE_TOOL_ACTION, tableViewer));
					//$FALL-THROUGH$
				case MISSING:
				case MODIFIED:
				case PARTIALLY_MODIFIED:
				case UNTRACKED:
				default:
					menuMgr.add(createItem(ActionCommands.DISCARD_CHANGES_ACTION, tableViewer));	// replace with index
					menuMgr.add(new Action(UIText.StagingView_StageItemMenuLabel) {
						@Override
						public void run() {
							stage((IStructuredSelection) tableViewer.getSelection());
						}
					});
				}
			}
		});

	}

	@SuppressWarnings("unchecked")
	private void openSelectionInEditor(ISelection s) {
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;
		final IStructuredSelection iss = (IStructuredSelection) s;
		for (Iterator<StagingEntry> it = iss.iterator(); it.hasNext();) {
			String relativePath = it.next().getPath();
			String path = new Path(currentRepository.getWorkTree()
					.getAbsolutePath()).append(relativePath)
					.toOSString();
			openFileInEditor(path);
		}
	}

	private void openFileInEditor(String filePath) {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		File file = new File(filePath);
		if (!file.exists()) {
			String message = NLS.bind(UIText.CommitFileDiffViewer_FileDoesNotExist, filePath);
			Activator.showError(message, null);
		}
		IWorkbenchPage page = window.getActivePage();
		EgitUiEditorUtils.openEditor(file, page);
	}

	private CommandContributionItem createItem(String itemAction, final TableViewer tableViewer) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		CommandContributionItemParameter itemParam = new CommandContributionItemParameter(
				workbench, null, itemAction, STYLE_PUSH);

		IWorkbenchWindow activeWorkbenchWindow = workbench
				.getActiveWorkbenchWindow();
		IHandlerService hsr = (IHandlerService) activeWorkbenchWindow
				.getService(IHandlerService.class);
		IEvaluationContext ctx = hsr.getCurrentState();
		ctx.addVariable(ACTIVE_MENU_SELECTION_NAME, tableViewer.getSelection());

		return new CommandContributionItem(itemParam);
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			Object firstElement = ssel.getFirstElement();
			if (firstElement instanceof IResource)
				showResource((IResource) firstElement);
			else if (firstElement instanceof RepositoryTreeNode) {
				RepositoryTreeNode repoNode = (RepositoryTreeNode) firstElement;
				reload(repoNode.getRepository());
			} else if (firstElement instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) firstElement).getAdapter(IResource.class);
				if (adapted != null)
					showResource(adapted);
			}
		}
	}

	private void showResource(final IResource resource) {
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null)
			return;
		if (mapping.getRepository() != currentRepository) {
			reload(mapping.getRepository());
		}
	}

	private void attachListeners(Repository repository) {
		myListeners.add(repository.getListenerList().addIndexChangedListener(
				myIndexChangedListener));
		myListeners.add(repository.getListenerList().addRefsChangedListener(
				myRefsChangedListener));
	}

	private void removeListeners() {
		for (ListenerHandle lh : myListeners)
			lh.remove();
		myListeners.clear();
	}

	private void stage(IStructuredSelection selection) {
		Git git = new Git(currentRepository);
		AddCommand add = null;
		RmCommand rm = null;
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			StagingEntry entry = (StagingEntry) iterator.next();
			switch (entry.getState()) {
			case ADDED:
			case CHANGED:
			case REMOVED:
				// already staged
				break;
			case CONFLICTING:
			case MODIFIED:
			case PARTIALLY_MODIFIED:
			case UNTRACKED:
				if (add == null)
					add = git.add();
				add.addFilepattern(entry.getPath());
				break;
			case MISSING:
				if (rm == null)
					rm = git.rm();
				rm.addFilepattern(entry.getPath());
				break;
			}
		}

		if (add != null)
			try {
				add.call();
			} catch (NoFilepatternException e1) {
				// cannot happen
			}
		if (rm != null)
			try {
				rm.call();
			} catch (NoFilepatternException e) {
				// cannot happen
			}

		reload(currentRepository);
	}

	private void unstage(IStructuredSelection selection) {
		if (selection.isEmpty())
			return;

		RevCommit headRev = null;
		try {
			final Ref head = currentRepository.getRef(Constants.HEAD);
			// head.getObjectId() is null if the repository does not contain any
			// commit
			if (head.getObjectId() != null)
				headRev = new RevWalk(currentRepository).parseCommit(head
						.getObjectId());
		} catch (IOException e1) {
			// TODO fix text
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_ErrorReadingMergeMsg);
			return;
		}

		final DirCache dirCache;
		final DirCacheEditor edit;
		try {
			dirCache = currentRepository.lockDirCache();
			edit = dirCache.editor();
		} catch (IOException e) {
			// TODO fix text
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_ErrorReadingMergeMsg);
			return;
		}

		try {
			updateDirCache(selection, headRev, edit);

			try {
				edit.commit();
			} catch (IOException e) {
				// TODO fix text
				MessageDialog.openError(getSite().getShell(),
						UIText.CommitAction_MergeHeadErrorTitle,
						UIText.CommitAction_ErrorReadingMergeMsg);
			}
		} finally {
			dirCache.unlock();
		}

		reload(currentRepository);
	}

	private void updateDirCache(IStructuredSelection selection,
			final RevCommit headRev, final DirCacheEditor edit) {
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			StagingEntry entry = (StagingEntry) iterator.next();
			switch (entry.getState()) {
			case ADDED:
				edit.add(new DirCacheEditor.DeletePath(entry.getPath()));
				break;
			case CHANGED:
			case REMOVED:
				// set the index object id/file mode back to our head revision
				try {
					final TreeWalk tw = TreeWalk.forPath(currentRepository,
							entry.getPath(), headRev.getTree());
					if (tw != null) {
						edit.add(new DirCacheEditor.PathEdit(entry.getPath()) {
							@Override
							public void apply(DirCacheEntry ent) {
								ent.setFileMode(tw.getFileMode(0));
								ent.setObjectId(tw.getObjectId(0));
								// for index & working tree compare
								ent.setLastModified(0);
							}
						});
					}
				} catch (IOException e) {
					// TODO fix text
					MessageDialog.openError(getSite().getShell(),
							UIText.CommitAction_MergeHeadErrorTitle,
							UIText.CommitAction_ErrorReadingMergeMsg);
				}
				break;
			default:
				// unstaged
			}
		}
	}

	private boolean isValidRepo(final Repository repository) {
		return repository != null
				&& !repository.isBare()
				&& repository.getWorkTree().exists()
				&& org.eclipse.egit.core.Activator.getDefault()
						.getRepositoryUtil().contains(repository);
	}

	/**
	 * Clear the view's state.
	 * <p>
	 * This method must be called from the UI-thread
	 */
	private void clearRepository() {
		saveCommitMessageComponentState();
		currentRepository = null;
		removeListeners();
		StagingViewUpdate update = new StagingViewUpdate(null, null, null);
		unstagedTableViewer.setInput(update);
		stagedTableViewer.setInput(update);
		enableCommitWidgets(false);
		updateSectionText();
		form.setText(UIText.StagingView_NoSelectionTitle);
	}

	private void reload(final Repository repository) {
		if (form.isDisposed())
			return;
		if (repository == null) {
			asyncExec(new Runnable() {

				public void run() {
					clearRepository();
				}
			});
			return;
		}

		if (!isValidRepo(repository))
			return;

		final boolean repositoryChanged = currentRepository != repository;

		final AtomicReference<IndexDiff> results = new AtomicReference<IndexDiff>();

		final String jobTitle = MessageFormat.format(UIText.StagingView_IndexDiffReload,
				StagingView.getRepositoryName(repository));

		if (reloadJob != null)
			reloadJob.cancel();
		reloadJob = new Job(jobTitle) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IndexDiff indexDiff = doReload(repository, monitor, jobTitle);
				results.set(indexDiff);
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.STAGING_VIEW_REFRESH))
					return true;
				return super.belongsTo(family);
			}

		};

		reloadJob.setUser(false);
		reloadJob.setRule(ResourcesPlugin.getWorkspace().getRoot());

		reloadJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(final IJobChangeEvent event) {
				if (!event.getResult().isOK())
					return;
				asyncExec(new Runnable() {
					public void run() {
						if (form.isDisposed())
							return;
						final IndexDiff indexDiff = results.get();
						final StagingViewUpdate update = new StagingViewUpdate(currentRepository, indexDiff, null);
						unstagedTableViewer.setInput(update);
						stagedTableViewer.setInput(update);
						enableCommitWidgets(true);
						commitAction.setEnabled(repository.getRepositoryState()
								.canCommit());
						form.setText(StagingView.getRepositoryName(repository));
						updateCommitMessageComponent(repositoryChanged);
						updateSectionText();
					}

				});
			}
		});

		schedule(reloadJob);
	}

	private void schedule(final Job j) {
		IWorkbenchPartSite site = getSite();
		if (site != null) {
			final IWorkbenchSiteProgressService p;
			p = (IWorkbenchSiteProgressService) site
					.getAdapter(IWorkbenchSiteProgressService.class);
			if (p != null) {
				p.schedule(j, 0, true /* use half-busy cursor */);
				return;
			}
		}
		j.schedule();
	}

	private IndexDiff doReload(final Repository repository, IProgressMonitor monitor, String jobTitle) {
		currentRepository = repository;

		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);

		final IndexDiff indexDiff;
		try {
			WorkingTreeIterator iterator = new FileTreeIterator(repository);
			indexDiff = new IndexDiff(repository, Constants.HEAD, iterator);
			indexDiff.diff(jgitMonitor, 0, 0, jobTitle);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		removeListeners();
		attachListeners(repository);

		return indexDiff;
	}

	private void clearCommitMessageToggles() {
		amendPreviousCommitAction.setChecked(false);
		addChangeIdAction.setChecked(false);
		signedOffByAction.setChecked(false);
	}

	void updateCommitMessageComponent(boolean repositoryChanged) {
		CommitHelper helper = new CommitHelper(currentRepository);
		CommitMessageComponentState oldState = null;
		if (repositoryChanged) {
			if (userEnteredCommmitMessage())
				saveCommitMessageComponentState();
			else
				deleteCommitMessageComponentState();
			oldState = loadCommitMessageComponentState();
			commitMessageComponent.setRepository(currentRepository);
			if (oldState == null) {
				loadInitialState(helper);
			} else {
				loadExistingState(helper, oldState);
			}
		} else {
			// repository did not change
			if (userEnteredCommmitMessage()) {
				if (!commitMessageComponent.getHeadCommit().equals(
						helper.getPreviousCommit()))
					addHeadChangedWarning(commitMessageComponent
							.getCommitMessage());
			} else
				loadInitialState(helper);
		}
		amendPreviousCommitAction.setChecked(commitMessageComponent
				.isAmending());
		amendPreviousCommitAction.setEnabled(helper.amendAllowed());
	}

	private void loadExistingState(CommitHelper helper,
			CommitMessageComponentState oldState) {
		boolean headCommitChanged = !oldState.getHeadCommit().equals(
				getCommitId(helper.getPreviousCommit()));
		commitMessageComponent.enableListers(false);
		commitMessageComponent.setAuthor(oldState.getAuthor());
		if (headCommitChanged)
			addHeadChangedWarning(oldState.getCommitMessage());
		else
			commitMessageComponent
					.setCommitMessage(oldState.getCommitMessage());
		commitMessageComponent.setCommitter(oldState.getCommitter());
		commitMessageComponent.setHeadCommit(getCommitId(helper
				.getPreviousCommit()));
		boolean amendAllowed = helper.amendAllowed();
		commitMessageComponent.setAmendAllowed(amendAllowed);
		if (!amendAllowed) {
			commitMessageComponent.setAmending(false);
		} else {
			if (!headCommitChanged && oldState.getAmend())
				commitMessageComponent.setAmending(true);
			else
				commitMessageComponent.setAmending(false);
		}
		commitMessageComponent.updateUIFromState();
		commitMessageComponent.updateSignedOffAndChangeIdButton();
		commitMessageComponent.enableListers(true);
	}

	private void addHeadChangedWarning(String commitMessage) {
		String message = UIText.StagingView_headCommitChanged + Text.DELIMITER
				+ Text.DELIMITER + commitMessage;
		commitMessageComponent.setCommitMessage(message);
	}

	private void loadInitialState(CommitHelper helper) {
		commitMessageComponent.enableListers(false);
		commitMessageComponent.resetState();
		commitMessageComponent.setAuthor(helper.getAuthor());
		commitMessageComponent.setCommitMessage(helper.getCommitMessage());
		commitMessageComponent.setCommitter(helper.getCommitter());
		commitMessageComponent.setHeadCommit(getCommitId(helper
				.getPreviousCommit()));
		commitMessageComponent.setAmendAllowed(helper.amendAllowed());
		commitMessageComponent.setAmending(false);
		commitMessageComponent.setSignedOff(false);
		commitMessageComponent.setCreateChangeId(false);
		commitMessageComponent.updateUI();
		commitMessageComponent.enableListers(true);
	}

	private boolean userEnteredCommmitMessage() {
		if (commitMessageComponent.getRepository() == null)
			return false;
		String message = commitMessageComponent.getCommitMessage();
		if (message == null || message.trim().length() == 0)
			return false;
		return true;
	}

	private ObjectId getCommitId(RevCommit commit) {
		if (commit == null)
			return ObjectId.zeroId();
		return commit.getId();
	}

	private void saveCommitMessageComponentState() {
		final Repository repo = commitMessageComponent.getRepository();
		if (repo != null)
			CommitMessageComponentStateManager.persistState(repo,
					commitMessageComponent.getState());
	}

	private void deleteCommitMessageComponentState() {
		if (commitMessageComponent.getRepository() != null)
			CommitMessageComponentStateManager
					.deleteState(commitMessageComponent.getRepository());
	}

	private CommitMessageComponentState loadCommitMessageComponentState() {
		return CommitMessageComponentStateManager.loadState(currentRepository);
	}

	private static String getRepositoryName(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = repository.getRepositoryState();
		if (state != RepositoryState.SAFE)
			return repoName + '|' + state.getDescription();
		else
			return repoName;
	}

	private void commit() {
		if (stagedTableViewer.getTable().getItemCount() == 0
				&& !amendPreviousCommitAction.isChecked()) {
			MessageDialog.openError(getSite().getShell(),
					UIText.StagingView_committingNotPossible,
					UIText.StagingView_noStagedFiles);
			return;
		}
		if (!commitMessageComponent.checkCommitInfo())
			return;
		final Repository repository = currentRepository;
		CommitOperation commitOperation = null;
		try {
			commitOperation = new CommitOperation(repository,
					commitMessageComponent.getAuthor(),
					commitMessageComponent.getCommitter(),
					commitMessageComponent.getCommitMessage());
		} catch (CoreException e) {
			Activator.handleError(UIText.StagingView_commitFailed, e, true);
			return;
		}
		if (amendPreviousCommitAction.isChecked())
			commitOperation.setAmending(true);
		commitOperation.setComputeChangeId(addChangeIdAction.isChecked());
		CommitUI.performCommit(currentRepository, commitOperation,
				openNewCommitsAction.isChecked());
		clearCommitMessageToggles();
		commitMessageText.setText(EMPTY_STRING);
	}

	@Override
	public void setFocus() {
		unstagedTableViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.removePostSelectionListener(selectionChangedListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		new InstanceScope().getNode(
				org.eclipse.egit.core.Activator.getPluginId())
				.removePreferenceChangeListener(prefListener);

		removeListeners();
		if (reloadJob != null) {
			reloadJob.cancel();
			reloadJob = null;
		}
	}

	private void asyncExec(Runnable runnable) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
	}

}
