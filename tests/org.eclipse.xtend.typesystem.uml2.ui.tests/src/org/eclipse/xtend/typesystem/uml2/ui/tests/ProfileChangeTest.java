/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.typesystem.uml2.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.AbstractResource;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #13: after a UML profile used by the type system "UML2 profiles" has changed, calls of extensions with
 * stereotype types as parameters must still be resolved, without a clean of the project.
 */
public class ProfileChangeTest {
	/** Version of the profile without stereotype Special yet. */
	private static final String PROFILE_OLD = profile("Other");

	/** New version of the profile (e.g. exported again) with stereotype Special. */
	private static final String PROFILE_NEW = profile("Special");

	/**
	 * Extension with a stereotype type as parameter. The second extension calls it, so that its parameter types are
	 * resolved when A is analyzed (as by the many calls in real templates).
	 */
	private static final String EXTENSION_A = "Boolean isSpecial(Test::Special s): true;\n\n"
			+ "Boolean callsIt(): isSpecial(null);\n";

	/** Calls the extension of A with the stereotype type. */
	private static final String EXTENSION_B = "extension A;\n\nBoolean check(Test::Special s): s.isSpecial();\n";

	private IProject project;

	private static String profile(final String stereotype) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<uml:Profile xmi:version="20131001" xmlns:xmi="http://www.omg.org/spec/XMI/20131001" \
				xmlns:uml="http://www.eclipse.org/uml2/5.0.0/UML" xmi:id="profile" name="Test">
				  <packagedElement xmi:type="uml:Stereotype" xmi:id="stereotype" name="%s"/>
				</uml:Profile>
				""".formatted(stereotype);
	}

	private boolean autoBuilding;

	@BeforeEach
	void createProject() throws CoreException {
		IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
		autoBuilding = description.isAutoBuilding();
		description.setAutoBuilding(false);
		ResourcesPlugin.getWorkspace().setDescription(description);

		project = ResourcesPlugin.getWorkspace().getRoot().getProject("profile-change");
		if (project.exists()) {
			project.delete(true, true, null);
		}
		project.create(null);
		project.open(null);
		IProjectDescription projectDescription = project.getDescription();
		projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID, XtendXpandNature.NATURE_ID });
		project.setDescription(projectDescription, null);
		project.getFolder("src").create(true, true, null);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setRawClasspath(new IClasspathEntry[] { JavaCore.newSourceEntry(project.getFullPath().append("src")),
				JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")) },
				project.getFullPath().append("bin"), null);
		waitForJobs();
	}

	@AfterEach
	void deleteProject() throws CoreException {
		project.delete(true, true, null);
		IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
		description.setAutoBuilding(autoBuilding);
		ResourcesPlugin.getWorkspace().setDescription(description);
	}

	@Test
	void extensionsAreResolvedAfterProfileChange() throws Exception {
		// A and B are analyzed while the profile doesn't define their stereotype type yet.
		IFile profile = create("src/Test.profile.uml", PROFILE_OLD);
		IFile a = create("src/A.ext", EXTENSION_A);
		IFile b = create("src/B.ext", EXTENSION_B);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForJobs();
		assertEquals(1, errors(b).size(), "Unknown type before the new profile: " + errors(b));

		// New version of the profile with the stereotype.
		profile.setContents(new ByteArrayInputStream(PROFILE_NEW.getBytes(StandardCharsets.UTF_8)), true, false, null);
		waitForJobs();

		// Edit B: the call of the extension of A has to be resolved with the new profile, without a clean.
		b.setContents(new ByteArrayInputStream(EXTENSION_B.getBytes(StandardCharsets.UTF_8)), true, false, null);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		waitForJobs();
		assertEquals(List.of(), errors(b), "Errors of B after the new profile");
		assertEquals(List.of(), errors(a), "Errors of A after the new profile");
	}

	@Test
	void profileChangeParsesExtensionFilesAgain() throws Exception {
		IFile profile = create("src/Test.profile.uml", PROFILE_OLD);
		IFile a = create("src/A.ext", EXTENSION_A);
		IFile other = create("src/notes.txt", "Notes");
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForJobs();
		Object parsed = parsed(a);

		// A change of another file doesn't parse the extension files again.
		other.setContents(new ByteArrayInputStream("Other notes".getBytes(StandardCharsets.UTF_8)), true, false, null);
		waitForJobs();
		assertSame(parsed, parsed(a), "Parsed again after a change of another file");

		// After a change of the profile they are parsed again (like a clean), so that no extension keeps parameter
		// types that were resolved with the old profile. No build is started by the test (auto build is off).
		profile.setContents(new ByteArrayInputStream(PROFILE_NEW.getBytes(StandardCharsets.UTF_8)), true, false, null);
		waitForJobs();
		assertNotSame(parsed, parsed(a), "Not parsed again after the profile changed");
		assertEquals(List.of(), errors(a), "Errors of A after the new profile");
	}

	/**
	 * @return Object Parsed content of the extension file as held by the Xtend / Xpand project.
	 */
	private static Object parsed(final IFile file) {
		IXtendXpandProject xtendProject = Activator.getExtXptModelManager().findProject(file);
		return ((AbstractResource) xtendProject.findXtendXpandResource(file)).getExtXptResource();
	}

	private IFile create(final String path, final String content) throws CoreException {
		IFile file = project.getFile(path);
		file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
		return file;
	}

	private static List<String> errors(final IFile file) throws CoreException {
		List<String> errors = new ArrayList<>();
		for (IMarker marker : file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)) {
			if (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
				errors.add(marker.getAttribute(IMarker.MESSAGE, ""));
			}
		}
		return errors;
	}

	private static void waitForJobs() {
		long end = System.currentTimeMillis() + 60_000;
		while (!Job.getJobManager().isIdle() && System.currentTimeMillis() < end) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
