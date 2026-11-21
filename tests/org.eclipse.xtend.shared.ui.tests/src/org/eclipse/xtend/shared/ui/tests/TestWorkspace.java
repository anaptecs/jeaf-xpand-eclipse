/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

/**
 * Helpers to set up Java projects, archives and external class folders in the test workspace.
 */
final class TestWorkspace {
	private TestWorkspace() {
	}

	static void disableAutoBuild() throws CoreException {
		IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
		description.setAutoBuilding(false);
		ResourcesPlugin.getWorkspace().setDescription(description);
	}

	/**
	 * Creates a Java project with source folder "src", the JRE and the passed additional classpath entries.
	 */
	static IJavaProject createJavaProject(final String name, final String[] natures,
			final List<IClasspathEntry> additionalEntries) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (project.exists()) {
			project.delete(true, true, null);
		}
		project.create(null);
		project.open(null);
		IProjectDescription description = project.getDescription();
		description.setNatureIds(natures);
		project.setDescription(description, null);
		IFolder src = project.getFolder("src");
		src.create(true, true, null);

		List<IClasspathEntry> entries = new ArrayList<>();
		entries.add(JavaCore.newSourceEntry(src.getFullPath()));
		entries.add(JavaCore.newContainerEntry(new org.eclipse.core.runtime.Path("org.eclipse.jdt.launching.JRE_CONTAINER")));
		entries.addAll(additionalEntries);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[0]), project.getFullPath().append("bin"), null);
		waitForJobs();
		return javaProject;
	}

	/**
	 * Writes an archive with the passed entries (file name -> content).
	 */
	static void writeArchive(final OutputStream out, final Map<String, String> entries) throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(out)) {
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue().getBytes(StandardCharsets.ISO_8859_1));
				zip.closeEntry();
			}
		}
	}

	static Path createExternalArchive(final Path directory, final String name, final Map<String, String> entries)
			throws IOException {
		Path archive = directory.resolve(name);
		try (OutputStream out = Files.newOutputStream(archive)) {
			writeArchive(out, entries);
		}
		return archive;
	}

	static IFile createFile(final IProject project, final String path, final byte[] content) throws CoreException {
		IFile file = project.getFile(path);
		createFolders(file.getParent());
		file.create(new ByteArrayInputStream(content), true, null);
		return file;
	}

	private static void createFolders(final org.eclipse.core.resources.IContainer container) throws CoreException {
		if (container instanceof IFolder && !container.exists()) {
			createFolders(container.getParent());
			((IFolder) container).create(true, true, null);
		}
	}

	static IPackageFragmentRoot findRoot(final IJavaProject project, final org.eclipse.core.runtime.IPath path)
			throws CoreException {
		for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) {
			if (root.getPath().equals(path)) {
				return root;
			}
		}
		throw new IllegalStateException("No package fragment root for " + path);
	}

	static String contents(final IStorage storage) throws CoreException, IOException {
		try (InputStream in = storage.getContents()) {
			return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
		}
	}

	/**
	 * Waits until all jobs (e.g. builds, refresh of external folders) are done.
	 */
	static void waitForJobs() {
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

	/**
	 * Collects errors that are logged by the passed plug-in.
	 */
	static final class ErrorCollector implements ILogListener {
		private final String pluginId;

		private final List<IStatus> errors = new ArrayList<>();

		ErrorCollector(final String pluginId) {
			this.pluginId = pluginId;
			Platform.addLogListener(this);
		}

		@Override
		public synchronized void logging(final IStatus status, final String plugin) {
			if (status.getSeverity() == IStatus.ERROR && (pluginId.equals(plugin) || pluginId.equals(status.getPlugin()))) {
				errors.add(status);
			}
		}

		synchronized List<IStatus> stop() {
			Platform.removeLogListener(this);
			return new ArrayList<>(errors);
		}
	}

}
