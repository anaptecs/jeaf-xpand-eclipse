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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.junit.jupiter.api.Test;

/**
 * Tests that a full build of the Xpand builder registers the Xpand resources of archives on the classpath.
 */
class XtendXpandBuilderTest {
	@Test
	void fullBuildRegistersResourcesOfArchives() throws Exception {
		TestWorkspace.disableAutoBuild();
		Path directory = Files.createTempDirectory("builder-test");
		Path archive = TestWorkspace.createExternalArchive(directory, "templates.jar",
				Map.of("templates/FromJar.ext", "String fromJar() : \"from jar\";"));

		TestWorkspace.ErrorCollector errors = new TestWorkspace.ErrorCollector(Activator.getId());
		IJavaProject project = TestWorkspace.createJavaProject("builder-test",
				new String[] { JavaCore.NATURE_ID, XtendXpandNature.NATURE_ID },
				List.of(JavaCore.newLibraryEntry(org.eclipse.core.runtime.Path.fromOSString(archive.toString()), null,
						null)));
		project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		TestWorkspace.waitForJobs();
		List<IStatus> loggedErrors = errors.stop();

		IXtendXpandProject xpandProject = Activator.getExtXptModelManager().findProject(project.getProject());
		assertNotNull(xpandProject);
		Set<String> resources = Arrays.stream(xpandProject.getRegisteredResources())
				.map(r -> r.getFullyQualifiedName()).collect(Collectors.toSet());
		assertTrue(resources.contains("templates::FromJar"), resources.toString());
		assertEquals(List.of(), loggedErrors.stream().map(IStatus::toString).toList());
	}
}
