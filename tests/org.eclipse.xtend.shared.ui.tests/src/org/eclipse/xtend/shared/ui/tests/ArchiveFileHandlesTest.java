/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.core.internal.JDTUtil;
import org.eclipse.xtend.shared.ui.core.internal.ResourceID;
import org.junit.jupiter.api.Test;

/**
 * Tests that Xpand resources loaded from archives don't keep the archive open. Resources keep their storage as long as
 * they are cached, so an open archive would stay open (and locked on Windows) as long as Eclipse runs.
 */
class ArchiveFileHandlesTest {
	private static final int RESOURCES = 20;

	private static final String CONTENT = "String fromArchive() : \"from archive\";";

	@Test
	void storagesDoNotKeepArchiveOpen() throws Exception {
		Path directory = Files.createTempDirectory("handles-test");
		Path archive = TestWorkspace.createExternalArchive(directory, "templates.jar",
				Map.of("templates/FromArchive.ext", CONTENT)).toRealPath();
		IPath archivePath = org.eclipse.core.runtime.Path.fromOSString(archive.toString());
		IJavaProject project = TestWorkspace.createJavaProject("handles-test", new String[] { JavaCore.NATURE_ID },
				List.of(JavaCore.newLibraryEntry(archivePath, null, null)));
		IPackageFragmentRoot root = TestWorkspace.findRoot(project, archivePath);
		ResourceID id = new ResourceID("templates::FromArchive", "ext");

		List<IStorage> storages = new ArrayList<>();
		for (int i = 0; i < RESOURCES; i++) {
			IStorage storage = JDTUtil.loadFromJar(id, root);
			assertNotNull(storage);
			storages.add(storage);
		}
		for (IStorage storage : storages) {
			assertEquals(CONTENT, TestWorkspace.contents(storage));
		}

		// Storages are still referenced (like cached resources do), but the archive must not be open anymore.
		assertEquals(0, openHandles(archive), "Open handles of " + archive);
		assertEquals(RESOURCES, storages.size());
	}

	/**
	 * Number of open file handles of this process for the passed file (Linux: /proc, macOS: lsof).
	 */
	private static long openHandles(final Path file) throws IOException, InterruptedException {
		Path fds = Path.of("/proc/self/fd");
		if (Files.isDirectory(fds)) {
			try (Stream<Path> links = Files.list(fds)) {
				return links.filter(link -> {
					try {
						return Files.readSymbolicLink(link).equals(file);
					}
					catch (IOException e) {
						return false;
					}
				}).count();
			}
		}
		Process lsof = new ProcessBuilder("lsof", "-Fn", "-p", String.valueOf(ProcessHandle.current().pid()))
				.redirectErrorStream(true).start();
		long count;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(lsof.getInputStream(), StandardCharsets.UTF_8))) {
			count = reader.lines().filter(line -> line.equals("n" + file)).count();
		}
		lsof.waitFor();
		return count;
	}
}
