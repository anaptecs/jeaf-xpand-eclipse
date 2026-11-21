/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.editor.search.view;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Opens the search preferences. Replaces the internal class <code>OpenSearchPreferencesAction</code> of the Eclipse
 * search plug-in with the same behavior.
 */
class OpenSearchPreferencesAction extends Action {
	private static final String SEARCH_PREFERENCE_PAGE = "org.eclipse.search.preferences.SearchPreferencePage";

	OpenSearchPreferencesAction() {
		super("Preferences...");
		setToolTipText("Open Search Preference Page");
	}

	@Override
	public void run() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell shell = window != null ? window.getShell() : null;
		String[] displayedPages = { SEARCH_PREFERENCE_PAGE, "org.eclipse.ui.editors.preferencePages.Annotations",
				"org.eclipse.ui.preferencePages.ColorsAndFonts" };
		PreferencesUtil.createPreferenceDialogOn(shell, SEARCH_PREFERENCE_PAGE, displayedPages, null).open();
	}
}
