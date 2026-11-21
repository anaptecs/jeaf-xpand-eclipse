/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xpand.ui.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.eclipse.xtend.shared.ui.wizards.EclipseHelper;
import org.junit.jupiter.api.Test;

/**
 * Tests the project wizard in an Eclipse without PDE (e.g. "Eclipse IDE for Java Developers").
 */
class ProjectWizardWithoutPdeTest {
	@Test
	void createExtXptProjectWithoutPde() throws Exception {
		assertNull(Platform.getBundle("org.eclipse.pde.core"), "Test runtime must not contain PDE");
		TestSupport.disableAutoBuild();

		IProject project = EclipseHelper.createExtXptProject("wizard-nopde-test",
				new ArrayList<>(List.of("src", "src-gen")), Collections.<IProject> emptyList(),
				new HashSet<>(Set.of("org.eclipse.jdt.core")), null, new NullProgressMonitor(), null);
		TestSupport.waitForJobs();

		assertNotNull(project);
		assertTrue(project.isOpen());
		assertTrue(project.hasNature(JavaCore.NATURE_ID), "Java nature");
		assertTrue(project.hasNature(XtendXpandNature.NATURE_ID), "Xpand nature");
		assertFalse(Arrays.asList(project.getDescription().getNatureIds()).contains("org.eclipse.pde.PluginNature"));
		assertTrue(project.getFolder("src").exists());
		assertTrue(project.getFolder("src-gen").exists());
		assertFalse(Arrays.stream(JavaCore.create(project).getRawClasspath())
				.anyMatch(e -> e.getPath().toString().equals("org.eclipse.pde.core.requiredPlugins")));
	}
}
