/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xpand.ui.tests;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;

/**
 * Helpers for workspace setup and the UI thread.
 */
final class TestSupport {
	private TestSupport() {
	}

	interface Task {
		void run() throws Exception;
	}

	static void ui(final Task task) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		Display.getDefault().syncExec(() -> {
			try {
				task.run();
			}
			catch (Throwable e) {
				error.set(e);
			}
		});
		if (error.get() instanceof Error) {
			throw (Error) error.get();
		}
		if (error.get() != null) {
			throw (Exception) error.get();
		}
	}

	static void processEvents() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
			// process all pending events
		}
	}

	static void disableAutoBuild() throws CoreException {
		IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
		description.setAutoBuilding(false);
		ResourcesPlugin.getWorkspace().setDescription(description);
	}

	/**
	 * Creates a Java project with Xpand nature and source folder "src".
	 */
	static IProject createXpandProject(final String name) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (project.exists()) {
			project.delete(true, true, null);
		}
		project.create(null);
		project.open(null);
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID, XtendXpandNature.NATURE_ID });
		project.setDescription(description, null);
		IFolder src = project.getFolder("src");
		src.create(true, true, null);
		JavaCore.create(project).setRawClasspath(
				new IClasspathEntry[] { JavaCore.newSourceEntry(src.getFullPath()),
						JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")) },
				project.getFullPath().append("bin"), null);
		waitForJobs();
		return project;
	}

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
}
