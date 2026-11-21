/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.eclipse.xtend.shared.ui.core.internal.preferences.BuilderPreferencePage;
import org.eclipse.xtend.shared.ui.core.preferences.PreferenceConstants;
import org.junit.jupiter.api.Test;

/**
 * Tests the exclusion patterns of the Xpand builder: editing them on the preference page and their effect on the
 * analysis.
 */
class BuilderExclusionsTest {
	/**
	 * Extension with an analysis error (unknown operation).
	 */
	private static final String BROKEN = "String broken() : unknownOperation();\n";

	@Test
	void exclusionPatternsOnPreferencePage() throws Exception {
		IEclipsePreferences workspace = InstanceScope.INSTANCE.getNode(Activator.getId());
		workspace.put(PreferenceConstants.BUILDER_EXCLUSIONS, "a/*, b/*");
		workspace.flush();
		try {
			UI.run(() -> {
				Shell shell = new Shell();
				BuilderPreferencePage page = new BuilderPreferencePage();
				page.init(PlatformUI.getWorkbench());
				page.createControl(shell);
				org.eclipse.swt.widgets.List list = UI.find(shell, org.eclipse.swt.widgets.List.class).get(0);
				assertEquals(List.of("a/*", "b/*"), Arrays.asList(list.getItems()));
				list.add("c/*");
				assertTrue(page.performOk());
				shell.dispose();
			});
			assertEquals("a/*,b/*,c/*", workspace.get(PreferenceConstants.BUILDER_EXCLUSIONS, null));
		}
		finally {
			workspace.remove(PreferenceConstants.BUILDER_EXCLUSIONS);
			workspace.flush();
		}
	}

	@Test
	void excludedResourcesAreNotAnalyzed() throws Exception {
		TestWorkspace.disableAutoBuild();
		IProject project = TestWorkspace.createJavaProject("builder-exclusions",
				new String[] { JavaCore.NATURE_ID, XtendXpandNature.NATURE_ID }, List.of()).getProject();
		IFile included = TestWorkspace.createFile(project, "src/included/Broken.ext",
				BROKEN.getBytes(StandardCharsets.UTF_8));
		IFile excluded = TestWorkspace.createFile(project, "src/excluded/Broken.ext",
				BROKEN.getBytes(StandardCharsets.UTF_8));
		// Patterns with blanks and empty entries
		IEclipsePreferences settings = new ProjectScope(project).getNode(Activator.getId());
		settings.put(PreferenceConstants.BUILDER_EXCLUSIONS, " src/excluded/* , ,other/* ");
		settings.flush();

		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		TestWorkspace.waitForJobs();

		assertTrue(problems(included) > 0, "Analysis error expected in " + included);
		assertEquals(0, problems(excluded), "No analysis of " + excluded);
	}

	private static int problems(final IFile file) throws Exception {
		return file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO).length;
	}
}
