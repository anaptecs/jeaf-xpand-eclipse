/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.editor.search.view;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.dialogs.PropertyDialogAction;

/**
 * Context menu actions "Open", "Open With" and "Properties" for files in the search result. Replaces the internal class
 * <code>NewTextSearchActionGroup</code> of the Eclipse search plug-in with the same behavior.
 */
class OpenFileActionGroup extends ActionGroup {
	private final IWorkbenchPage page;

	private final OpenFileAction openAction;

	private final PropertyDialogAction propertyDialogAction;

	OpenFileActionGroup(final IViewPart part) {
		IWorkbenchPartSite site = part.getSite();
		ISelectionProvider selectionProvider = site.getSelectionProvider();
		page = site.getPage();
		propertyDialogAction = new PropertyDialogAction(site, selectionProvider);
		openAction = new OpenFileAction(page);
		ISelection selection = selectionProvider.getSelection();
		if (selection instanceof IStructuredSelection) {
			propertyDialogAction.selectionChanged((IStructuredSelection) selection);
		}
		else {
			propertyDialogAction.selectionChanged(selection);
		}
	}

	@Override
	public void fillContextMenu(final IMenuManager menu) {
		ActionContext context = getContext();
		ISelection selection = context != null ? context.getSelection() : null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			addOpenWithMenu(menu, structuredSelection);
			if (propertyDialogAction.isEnabled()
					&& propertyDialogAction.isApplicableForSelection(structuredSelection)) {
				menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, propertyDialogAction);
			}
		}
	}

	private void addOpenWithMenu(final IMenuManager menu, final IStructuredSelection selection) {
		openAction.selectionChanged(selection);
		if (openAction.isEnabled()) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, openAction);
		}
		if (selection.size() == 1 && selection.getFirstElement() instanceof IAdaptable) {
			MenuManager submenu = new MenuManager("Open Wit&h");
			submenu.add(new OpenWithMenu(page, (IAdaptable) selection.getFirstElement()));
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
		}
	}

	@Override
	public void fillActionBars(final IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertyDialogAction);
	}
}
