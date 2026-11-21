/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.eclipse.xtend.shared.ui.core.internal.preferences.BuilderPreferencePage;
import org.eclipse.xtend.shared.ui.core.preferences.PreferenceConstants;
import org.eclipse.xtend.shared.ui.core.properties.MetamodelContributorsPropertyAndPreferencePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests the Xpand preference pages (workspace) and property pages (project) as a user would use them.
 */
@TestInstance(Lifecycle.PER_CLASS)
class PreferencePagesTest {
	private static final String PROJECT_SPECIFIC = "Enable pr&oject specific settings";

	private static final String WORKSPACE_LINK = "Configure Workspace Settings...";

	private static final String PROJECT_LINK = "Configure Project Specific Settings...";

	private static final String OAW_CLASSIC = "org.eclipse.xtend.shared.ui.core.metamodel.jdt.oaw.OawClassicMetamodelContributor";

	private IProject project;

	@BeforeAll
	void setUp() throws Exception {
		TestWorkspace.disableAutoBuild();
		project = TestWorkspace.createJavaProject("preferences-test",
				new String[] { JavaCore.NATURE_ID, XtendXpandNature.NATURE_ID }, List.of()).getProject();
	}

	@AfterEach
	void removeSettings() throws Exception {
		for (IEclipsePreferences node : List.of(InstanceScope.INSTANCE.getNode(Activator.getId()),
				new ProjectScope(project).getNode(Activator.getId()))) {
			node.remove(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY);
			node.remove(PreferenceConstants.METAMODELCONTRIBUTORS);
			node.flush();
		}
		TestWorkspace.waitForJobs();
	}

	@Test
	void workspacePreferencePage() throws Exception {
		UI.run(() -> {
			Shell shell = new Shell();
			BuilderPreferencePage page = new BuilderPreferencePage();
			open(page, shell, null);
			assertTrue(hasLink(shell, PROJECT_LINK));
			assertNull(UI.findButton(shell, PROJECT_SPECIFIC));
			Combo combo = UI.find(shell, Combo.class).get(0);
			assertTrue(combo.isEnabled());
			combo.select(2);
			assertTrue(page.performOk());
			shell.dispose();
		});
		assertEquals(2, workspaceSettings().getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY, -1));
		assertEquals(-1, projectSettings().getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY, -1));
	}

	@Test
	void projectPropertyPage() throws Exception {
		// No project specific settings yet
		UI.run(() -> {
			Shell shell = new Shell();
			BuilderPreferencePage page = new BuilderPreferencePage();
			open(page, shell, project);
			Button projectSpecific = UI.findButton(shell, PROJECT_SPECIFIC);
			assertNotNull(projectSpecific);
			assertFalse(projectSpecific.getSelection());
			Combo combo = UI.find(shell, Combo.class).get(0);
			assertFalse(combo.isEnabled());
			Link workspaceLink = link(shell, WORKSPACE_LINK);
			assertTrue(workspaceLink.isEnabled());

			UI.click(projectSpecific, true);
			assertTrue(combo.isEnabled());
			assertFalse(workspaceLink.isEnabled());
			combo.select(3);
			assertTrue(page.performOk());
			shell.dispose();
		});
		assertEquals(3, projectSettings().getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY, -1));
		assertEquals(-1, workspaceSettings().getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY, -1));

		// Project specific settings exist, disable them again
		UI.run(() -> {
			Shell shell = new Shell();
			BuilderPreferencePage page = new BuilderPreferencePage();
			open(page, shell, project);
			Button projectSpecific = UI.findButton(shell, PROJECT_SPECIFIC);
			assertTrue(projectSpecific.getSelection());
			Combo combo = UI.find(shell, Combo.class).get(0);
			assertTrue(combo.isEnabled());
			assertEquals(3, combo.getSelectionIndex());

			UI.click(projectSpecific, false);
			assertFalse(combo.isEnabled());
			assertTrue(page.performOk());
			shell.dispose();
		});
		assertEquals(-1, projectSettings().getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY, -1));
	}

	@Test
	void restoreDefaultsDisablesProjectSpecificSettings() throws Exception {
		projectSettings().putInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY, 2);
		projectSettings().flush();
		UI.run(() -> {
			Shell shell = new Shell();
			BuilderPreferencePage page = new BuilderPreferencePage();
			open(page, shell, project);
			Button projectSpecific = UI.findButton(shell, PROJECT_SPECIFIC);
			assertTrue(projectSpecific.getSelection());
			Button restoreDefaults = UI.find(shell, Button.class).stream()
					.filter(b -> b.getText().replace("&", "").equals("Restore Defaults")).findFirst().orElseThrow();
			UI.click(restoreDefaults);
			assertFalse(projectSpecific.getSelection());
			assertFalse(UI.find(shell, Combo.class).get(0).isEnabled());
			shell.dispose();
		});
	}

	@Test
	void metamodelContributorsPropertyPage() throws Exception {
		UI.run(() -> {
			Shell shell = new Shell();
			MetamodelContributorsPropertyAndPreferencePage page = new MetamodelContributorsPropertyAndPreferencePage();
			open(page, shell, project);
			UI.click(UI.findButton(shell, PROJECT_SPECIFIC), true);
			Table table = UI.find(shell, Table.class).stream().filter(t -> (t.getStyle() & SWT.CHECK) != 0)
					.findFirst().orElseThrow();
			assertTrue(table.isEnabled());
			boolean found = false;
			for (TableItem item : table.getItems()) {
				boolean oawClassic = "oAW-Classic Metamodel".equals(item.getText());
				item.setChecked(oawClassic);
				found |= oawClassic;
			}
			assertTrue(found);
			assertTrue(page.performOk());
			shell.dispose();
		});
		TestWorkspace.waitForJobs();
		assertEquals(OAW_CLASSIC, projectSettings().get(PreferenceConstants.METAMODELCONTRIBUTORS, null));
	}

	private static void open(final PreferencePage page, final Shell shell, final IProject element) {
		if (element != null) {
			((IWorkbenchPropertyPage) page).setElement(element);
		}
		else {
			((IWorkbenchPreferencePage) page).init(PlatformUI.getWorkbench());
		}
		page.createControl(shell);
	}

	private static boolean hasLink(final Shell shell, final String text) {
		return UI.find(shell, Link.class).stream().anyMatch(l -> l.getText().contains(text));
	}

	private static Link link(final Shell shell, final String text) {
		return UI.find(shell, Link.class).stream().filter(l -> l.getText().contains(text)).findFirst().orElseThrow();
	}

	private IEclipsePreferences workspaceSettings() {
		return InstanceScope.INSTANCE.getNode(Activator.getId());
	}

	private IEclipsePreferences projectSettings() {
		return new ProjectScope(project).getNode(Activator.getId());
	}
}
