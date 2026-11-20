/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;

/**
 * Adapter factory for Xtend resources. Provides adapters for
 * <ul>
 * <li> {@link IResource} -> {@link IXtendXpandProject}
 * <li> {@link IResource} -> {@link IXtendXpandResource}
 * <li> {@link IXtendXpandResource} -> {@link IResource}
 * <li> {@link IXtendXpandResource} -> {@link IStorage}
 * <li> {@link IXtendXpandResource} -> {@link IProject}
 * <li> {@link IXtendXpandResource} -> {@link IXtendXpandProject}
 * <li> {@link IXtendXpandProject} -> {@link IProject}
 * <li> {@link IXtendXpandProject} -> {@link IJavaProject}
 * </ul>
 * 
 * @author Karsten Thoms (karsten.thoms@itemis.de) - Initial contribution and
 *         API
 */
@SuppressWarnings("rawtypes")
public class XtendAdapterFactory implements IAdapterFactory {
	private static final Class[] ADAPTABLE_CLASSES = new Class[] {
			IXtendXpandProject.class, IXtendXpandResource.class,
			IProject.class, IResource.class, IStorage.class, IJavaProject.class };

	public Class[] getAdapterList() {
		return ADAPTABLE_CLASSES;
	}

	public Object getAdapter(final Object adaptableObject,
			final Class adapterType) {
		if (adaptableObject instanceof IResource) {
			return handleIResource((IResource) adaptableObject, adapterType);
		}
		if (adaptableObject instanceof IXtendXpandResource) {
			return handleIXtendXpandResource(
					(IXtendXpandResource) adaptableObject, adapterType);
		}
		if (adaptableObject instanceof IXtendXpandProject) {
			return handleIXtendXpandProject(
					(IXtendXpandProject) adaptableObject, adapterType);
		}
		return null;
	}

	private Object handleIResource(final IResource res, final Class adapterType) {
		IXtendXpandProject project = Activator.getExtXptModelManager()
				.findProject(res);
		if (IXtendXpandProject.class.equals(adapterType)) {
			return project;
		}
		if (IXtendXpandResource.class.equals(adapterType)) {
			return project != null && res instanceof IStorage ? project
					.findXtendXpandResource((IStorage) res) : null;
		}
		return null;
	}

	private Object handleIXtendXpandResource(final IXtendXpandResource res,
			final Class adapterType) {
		if (IStorage.class.equals(adapterType)) {
			return res.getUnderlyingStorage();
		}
		if (IResource.class.equals(adapterType)) {
			IStorage st = res.getUnderlyingStorage();
			return st != null && st instanceof IResource ? st : null;
		}
		if (IProject.class.equals(adapterType)) {
			IResource iResource = (IResource) (res.getUnderlyingStorage() != null
					&& res.getUnderlyingStorage() instanceof IResource ? res
					.getUnderlyingStorage() : null);
			return iResource != null ? iResource.getProject() : null;
		}
		if (IXtendXpandProject.class.equals(adapterType)) {
			IProject project = (IProject) handleIXtendXpandResource(res,
					IProject.class);
			return Activator.getExtXptModelManager().findProject(project);
		}
		return null;
	}

	private Object handleIXtendXpandProject(final IXtendXpandProject project,
			final Class adapterType) {
		if (IProject.class.equals(adapterType)) {
			return project.getProject().getProject();
		}
		if (IJavaProject.class.equals(adapterType)) {
			return project.getProject();
		}
		return null;
	}

}
