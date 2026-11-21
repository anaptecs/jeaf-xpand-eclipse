/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.editor.search.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.search.internal.ui.text.NewTextSearchActionGroup;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.junit.jupiter.api.Test;

/**
 * Tests the actions of the search result page. They are compared with the internal classes of the Eclipse search
 * plug-in that were used before.
 */
@SuppressWarnings("restriction")
class SearchViewActionsTest {
	@Test
	void contextMenuIsSameAsOriginal() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("search-test");
		if (!project.exists()) {
			project.create(null);
		}
		project.open(null);
		IFile file = project.getFile("Test.ext");
		if (!file.exists()) {
			file.create(new ByteArrayInputStream("String test() : \"test\";".getBytes()), true, null);
		}

		List<List<String>> menus = runInUIThread(() -> {
			IWorkbenchPage page = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage();
			IViewPart view = page.showView(NewSearchUI.SEARCH_VIEW_ID);
			List<String> original = fillMenu(new NewTextSearchActionGroup(view), file);
			List<String> replacement = fillMenu(new OpenFileActionGroup(view), file);
			return List.of(original, replacement);
		});

		List<String> original = menus.get(0);
		List<String> replacement = menus.get(1);
		assertEquals(original, replacement);
		assertTrue(replacement.stream().anyMatch(item -> item.startsWith("action:&Open")), replacement.toString());
		assertTrue(replacement.stream().anyMatch(item -> item.startsWith("menu:Open Wit&h")), replacement.toString());
	}

	@Test
	void preferencesActionIsSameAsOriginal() throws Exception {
		List<String> labels = runInUIThread(() -> {
			org.eclipse.search2.internal.ui.OpenSearchPreferencesAction original = new org.eclipse.search2.internal.ui.OpenSearchPreferencesAction();
			OpenSearchPreferencesAction replacement = new OpenSearchPreferencesAction();
			return List.of(original.getText(), original.getToolTipText(), replacement.getText(),
					replacement.getToolTipText());
		});
		assertEquals(labels.get(0), labels.get(2));
		assertEquals(labels.get(1), labels.get(3));
	}

	private static List<String> fillMenu(final ActionGroup group, final IFile file) {
		MenuManager menu = new MenuManager();
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_OPEN));
		menu.add(new GroupMarker(IContextMenuConstants.GROUP_PROPERTIES));
		group.setContext(new ActionContext(new StructuredSelection(file)));
		group.fillContextMenu(menu);
		List<String> items = describe(menu);
		group.dispose();
		return items;
	}

	private static List<String> describe(final IMenuManager menu) {
		List<String> items = new ArrayList<>();
		for (IContributionItem item : menu.getItems()) {
			if (item instanceof ActionContributionItem) {
				ActionContributionItem action = (ActionContributionItem) item;
				items.add("action:" + action.getAction().getText() + ":" + action.getAction().getClass().getName() + ":"
						+ action.getAction().isEnabled());
			}
			else if (item instanceof MenuManager) {
				MenuManager submenu = (MenuManager) item;
				List<String> children = new ArrayList<>();
				for (IContributionItem child : submenu.getItems()) {
					children.add(child.getClass().getName());
				}
				items.add("menu:" + submenu.getMenuText() + ":" + children);
			}
			else {
				items.add(item.getClass().getSimpleName() + ":" + item.getId());
			}
		}
		return items;
	}

	private interface UITask<T> {
		T run() throws Exception;
	}

	private static <T> T runInUIThread(final UITask<T> task) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();
		Display.getDefault().syncExec(() -> {
			try {
				result.set(task.run());
			}
			catch (Exception e) {
				error.set(e);
			}
		});
		if (error.get() != null) {
			throw error.get();
		}
		return result.get();
	}
}
