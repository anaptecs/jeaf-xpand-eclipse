/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.core.internal.JDTUtil;
import org.eclipse.xtend.shared.ui.core.internal.ResourceID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests access to Xpand resources in archives and external class folders. Results are compared with JDT's internal
 * implementation that was used before (called by reflection, as JDT doesn't export it).
 */
@TestInstance(Lifecycle.PER_CLASS)
class JDTUtilTest {
	private static final String EXTERNAL_JAR_CONTENT = "String externalJar() : \"external jar\";";

	private static final String INTERNAL_JAR_CONTENT = "String internalJar() : \"internal jar\";";

	private static final String FOLDER_CONTENT = "String externalFolder() : \"external folder\";";

	private IJavaProject project;

	private IPackageFragmentRoot externalJar;

	private IPackageFragmentRoot internalJar;

	private IPackageFragmentRoot externalFolder;

	private IPackageFragmentRoot sourceFolder;

	@BeforeAll
	void setUp() throws Exception {
		TestWorkspace.disableAutoBuild();
		Path directory = Files.createTempDirectory("jdtutil-test");
		Path externalJarFile = TestWorkspace.createExternalArchive(directory, "external.jar",
				Map.of("templates/ExternalJar.ext", EXTERNAL_JAR_CONTENT));
		Path folder = Files.createDirectories(directory.resolve("classes/templates"));
		Files.writeString(folder.resolve("ExternalFolder.ext"), FOLDER_CONTENT, StandardCharsets.ISO_8859_1);
		IPath externalJarPath = org.eclipse.core.runtime.Path.fromOSString(externalJarFile.toString());
		IPath externalFolderPath = org.eclipse.core.runtime.Path.fromOSString(directory.resolve("classes").toString());

		project = TestWorkspace.createJavaProject("jdtutil-test", new String[] { JavaCore.NATURE_ID },
				List.of(JavaCore.newLibraryEntry(externalJarPath, null, null),
						JavaCore.newLibraryEntry(externalFolderPath, null, null)));

		// Archive within the workspace
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		TestWorkspace.writeArchive(archive, Map.of("templates/InternalJar.ext", INTERNAL_JAR_CONTENT));
		IFile internalJarFile = TestWorkspace.createFile(project.getProject(), "lib/internal.jar", archive.toByteArray());
		List<IClasspathEntry> entries = new ArrayList<>(List.of(project.getRawClasspath()));
		entries.add(JavaCore.newLibraryEntry(internalJarFile.getFullPath(), null, null));
		project.setRawClasspath(entries.toArray(new IClasspathEntry[0]), null);
		TestWorkspace.waitForJobs();

		externalJar = TestWorkspace.findRoot(project, externalJarPath);
		internalJar = TestWorkspace.findRoot(project, internalJarFile.getFullPath());
		externalFolder = TestWorkspace.findRoot(project, externalFolderPath);
		sourceFolder = TestWorkspace.findRoot(project, project.getPath().append("src"));
	}

	@Test
	void openArchiveOpensSameFileAsJDT() throws Exception {
		for (IPackageFragmentRoot root : List.of(externalJar, internalJar)) {
			try (ZipFile expected = (ZipFile) root.getClass().getMethod("getJar").invoke(root);
					ZipFile actual = JDTUtil.openArchive(root)) {
				assertEquals(expected.getName(), actual.getName(), root.toString());
				assertEquals(expected.size(), actual.size(), root.toString());
			}
		}
		assertInstanceOf(IFile.class, internalJar.getResource());
		assertNull(externalJar.getResource());
	}

	@Test
	void loadFromJar() throws Exception {
		IStorage external = JDTUtil.loadFromJar(new ResourceID("templates::ExternalJar", "ext"), externalJar);
		assertNotNull(external);
		assertEquals(EXTERNAL_JAR_CONTENT, TestWorkspace.contents(external));

		IStorage internal = JDTUtil.loadFromJar(new ResourceID("templates::InternalJar", "ext"), internalJar);
		assertNotNull(internal);
		assertEquals(INTERNAL_JAR_CONTENT, TestWorkspace.contents(internal));

		assertNull(JDTUtil.loadFromJar(new ResourceID("templates::Missing", "ext"), externalJar));
		assertNull(JDTUtil.loadFromJar(new ResourceID("templates::ExternalJar", "ext"), sourceFolder));
	}

	@Test
	void getExternalFolderReturnsSameFolderAsJDT() throws Exception {
		assertTrue(externalFolder.isExternal());
		IContainer actual = JDTUtil.getExternalFolder(externalFolder);
		assertNotNull(actual);
		IResource expected = (IResource) externalFolder.getClass().getMethod("resource").invoke(externalFolder);
		assertEquals(expected, actual);
		assertTrue(actual.exists());

		assertNull(JDTUtil.getExternalFolder(sourceFolder));
		assertNull(JDTUtil.getExternalFolder(externalJar));
		assertNull(JDTUtil.getExternalFolder(internalJar));
	}

	@Test
	void findStorage() throws Exception {
		IStorage folder = JDTUtil.findStorage(project, new ResourceID("templates::ExternalFolder", "ext"), false);
		assertInstanceOf(IFile.class, folder);
		assertEquals(FOLDER_CONTENT, TestWorkspace.contents(folder));

		assertNull(JDTUtil.findStorage(project, new ResourceID("templates::ExternalJar", "ext"), false));
		IStorage jar = JDTUtil.findStorage(project, new ResourceID("templates::ExternalJar", "ext"), true);
		assertNotNull(jar);
		assertEquals(EXTERNAL_JAR_CONTENT, TestWorkspace.contents(jar));
	}
}
