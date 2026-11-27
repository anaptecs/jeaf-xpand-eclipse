/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.typesystem.uml2.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandBuilder;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;

/**
 * Full build of the Xtend / Xpand builder of all projects with the Xtend / Xpand nature after a UML profile has changed
 * (issue #13): extensions keep the parameter types they resolved once, also if a type couldn't be resolved with the old
 * profile. A full build parses all extension files again, like a clean of the projects.
 */
final class ProfileChangeBuild extends WorkspaceJob {
	/**
	 * Delay, so that several changed profiles (e.g. an export of a model) lead to one build.
	 */
	private static final long DELAY = 500;

	private static final ProfileChangeBuild INSTANCE = new ProfileChangeBuild();

	private ProfileChangeBuild() {
		super("Analyzing Xtend / Xpand files after a UML profile has changed");
		setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
	}

	/**
	 * Schedules the build; a build that is already waiting is only delayed.
	 */
	static void request() {
		INSTANCE.schedule(DELAY);
	}

	@Override
	public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		SubMonitor progress = SubMonitor.convert(monitor, projects.length);
		for (IProject project : projects) {
			if (project.isOpen() && project.hasNature(XtendXpandNature.NATURE_ID)) {
				project.build(IncrementalProjectBuilder.FULL_BUILD, XtendXpandBuilder.getBUILDER_ID(), null,
						progress.split(1));
			}
			else {
				progress.worked(1);
			}
		}
		return Status.OK_STATUS;
	}
}
