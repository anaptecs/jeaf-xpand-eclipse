/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.eclipse.xtend.shared.ui.wizards.EclipseHelper;
import org.junit.jupiter.api.Test;

/**
 * Tests the creation of Xpand projects as done by the project wizard, incl. the reporting of progress.
 */
class EclipseHelperTest {
	/**
	 * Progress monitor that records the calls.
	 */
	private static final class RecordingMonitor extends NullProgressMonitor {
		int beginTaskCalls;

		int totalWork;

		double worked;

		int doneCalls;

		@Override
		public void beginTask(final String name, final int work) {
			beginTaskCalls++;
			totalWork = work;
		}

		@Override
		public void internalWorked(final double work) {
			worked += work;
		}

		@Override
		public void worked(final int work) {
			internalWorked(work);
		}

		@Override
		public void done() {
			doneCalls++;
		}
	}

	@Test
	void createExtXptProject() throws Exception {
		TestWorkspace.disableAutoBuild();
		RecordingMonitor monitor = new RecordingMonitor();
		List<String> srcFolders = new ArrayList<>(List.of("src", "src-gen"));
		Set<String> requiredBundles = new HashSet<>(Set.of("org.eclipse.jdt.core"));

		IProject project = EclipseHelper.createExtXptProject("wizard-test", srcFolders,
				Collections.<IProject> emptyList(), requiredBundles, null, monitor, null);
		TestWorkspace.waitForJobs();

		assertNotNull(project);
		assertTrue(project.isOpen());
		assertTrue(project.hasNature(JavaCore.NATURE_ID));
		assertTrue(project.hasNature(XtendXpandNature.NATURE_ID));
		assertTrue(project.getFolder("src").exists());
		assertTrue(project.getFolder("src-gen").exists());
		assertTrue(project.getFile("META-INF/MANIFEST.MF").exists());
		assertTrue(project.getFile("build.properties").exists());
		assertTrue(JavaCore.create(project).getOutputLocation().equals(project.getFullPath().append("bin")));

		// Progress: one task that isn't exceeded and is finished
		assertEquals(1, monitor.beginTaskCalls);
		assertTrue(monitor.worked > 0 && monitor.worked <= monitor.totalWork,
				monitor.worked + " of " + monitor.totalWork);
		assertTrue(monitor.doneCalls >= 1);
	}
}
