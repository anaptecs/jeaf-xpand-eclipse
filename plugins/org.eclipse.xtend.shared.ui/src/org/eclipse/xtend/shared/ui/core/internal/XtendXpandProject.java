/*******************************************************************************
 * Copyright (c) 2005, 2007 committers of openArchitectureWare and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors: committers of
 * openArchitectureWare - initial API and implementation
 *******************************************************************************/

package org.eclipse.xtend.shared.ui.core.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.expression.AnalysationIssue;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.ResourceContributor;
import org.eclipse.xtend.shared.ui.ResourceContributor2;
import org.eclipse.xtend.shared.ui.core.AbstractResource;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandBuilder;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandMarkerManager;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.eclipse.xtend.shared.ui.internal.XtendLog;

public class XtendXpandProject implements IXtendXpandProject {

	private static final Set<IJavaProject> initializing = new HashSet<IJavaProject>();
	private static final IXtendXpandResource NULL_RESOURCE = new AbstractResource(
			null) {
		public String getFileExtension() {
			return null;
		}

		@Override
		protected void analyze(final ExecutionContext ctx,
				final Set<AnalysationIssue> issues) {
		}

		@Override
		protected boolean internalRefresh() {
			return false;
		}
	};

	final IJavaProject project;
	private IElementChangedListener changeListener;
	private IResourceChangeListener resourceChangeListener;

	public XtendXpandProject(final IJavaProject resource) {
		project = resource;
		try {
			final IProject[] ps = project.getProject().getReferencedProjects();
			for (IProject element : ps) {
				if (initializing.add(project)) {
					try {
						Activator.getExtXptModelManager().findProject(element);
					} finally {
						initializing.remove(project);
					}
				}
			}
		} catch (final CoreException e) {
			XtendLog.logError(e);
		}
		installListeners();

		Job j = new Job("Initializing " + project.getElementName()) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					final IProject p = project.getProject();
					if (p.isAccessible()
							&& p.isNatureEnabled(XtendXpandNature.NATURE_ID)) {
						p.build(IncrementalProjectBuilder.CLEAN_BUILD,
								XtendXpandBuilder.getBUILDER_ID(),
								new HashMap<String, String>(), monitor);
					}
				} catch (final CoreException e) {
					XtendLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		j.setRule(project.getResource().getWorkspace().getRuleFactory()
				.buildRule());
		j.schedule();
	}

	private void installListeners() {
		changeListener = new IElementChangedListener() {

			public void elementChanged(final ElementChangedEvent event) {
				if (fromJar.isEmpty()) {
					return;
				}
				if (containsRemovedClassPathEntry(event.getDelta()
						.getAffectedChildren())) {
					removeResourcesFromJar();
				}
			}
		};
		JavaCore.addElementChangedListener(changeListener,
				ElementChangedEvent.POST_CHANGE);
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(final IResourceChangeEvent event) {
				if (event.getType() == IResourceChangeEvent.PRE_DELETE
						&& event.getResource().getFullPath()
						.equals(project.getPath())) {
					dispose();
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener);
	}

	/**
	 * Release resources on project delete
	 */
	private void dispose() {
		// this project is about to be removed
		JavaCore.removeElementChangedListener(changeListener);
		changeListener = null;
		// unregister
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
		resourceChangeListener = null;
		resources.clear();
		fromJar.clear();
	}

	protected boolean containsRemovedClassPathEntry(
			final IJavaElementDelta[] affectedChildren) {
		for (IJavaElementDelta delta : affectedChildren) {
			if ((delta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
				return true;
			}
			if (containsRemovedClassPathEntry(delta.getAffectedChildren())) {
				return true;
			}
		}
		return false;
	}

	protected void removeResourcesFromJar() {
		for (ResourceID id : fromJar) {
			resources.remove(id);
		}
	}

	private final Map<ResourceID, IXtendXpandResource> resources = new HashMap<ResourceID, IXtendXpandResource>();

	private final Set<ResourceID> fromJar = new HashSet<ResourceID>();

	/**
	 * @see IXtendXpandProject#getRegisteredResources()
	 */
	public IXtendXpandResource[] getRegisteredResources() {
		return resources.values().toArray(
				new IXtendXpandResource[resources.size()]);
	}

	/**
	 * @see IXtendXpandProject#getAllRegisteredResources()
	 */
	public IXtendXpandResource[] getAllRegisteredResources() {
		Set<IXtendXpandResource> result = new HashSet<IXtendXpandResource>();
		result.addAll(Arrays.asList(getRegisteredResources()));
		for (IXtendXpandProject p : getAllReferencedProjects()) {
			result.addAll(Arrays.asList(p.getRegisteredResources()));
		}
		return result.toArray(new IXtendXpandResource[result.size()]);
	}

	/**
	 * @see IXtendXpandProject#getReferencedProjects()
	 */
	public IXtendXpandProject[] getReferencedProjects() {
		Set<IXtendXpandProject> result = new HashSet<IXtendXpandProject>();
		try {
			IProject[] projects = getProject().getProject()
					.getReferencedProjects();
			for (IProject project : projects) {
				IXtendXpandProject p = Activator.getExtXptModelManager()
						.findProject(project);
				if (p != null) {
					result.add(p);
				}
			}
		} catch (CoreException e) {
			XtendLog.logError(e);
		}
		return result.toArray(new IXtendXpandProject[result.size()]);
	}

	/**
	 * @see IXtendXpandProject#getAllReferencedProjects()
	 */
	public IXtendXpandProject[] getAllReferencedProjects() {
		Set<IXtendXpandProject> result = new HashSet<IXtendXpandProject>();
		IXtendXpandProject[] projects = getReferencedProjects();
		result.addAll(Arrays.asList(projects));
		for (IXtendXpandProject project : projects) {
			result.addAll(Arrays.asList(project.getAllReferencedProjects()));
		}
		return result.toArray(new IXtendXpandProject[result.size()]);
	}

	public IJavaProject getProject() {
		return project;
	}

	/**
	 * @see IXtendXpandProject#unregisterXtendXpandResource(IXtendXpandResource)
	 */
	public void unregisterXtendXpandResource(final IXtendXpandResource res) {
		if (res != null) {
			if (res.getUnderlyingStorage() instanceof IFile) {
				IFile file = (IFile) res.getUnderlyingStorage();
				XtendXpandMarkerManager.deleteMarkers(file);
				ResourceID toRemove = null;
				for (Entry<ResourceID, IXtendXpandResource> entry : resources
						.entrySet()) {
					if (entry.getValue() == toRemove) {
						toRemove = entry.getKey();
						break;
					}
				}
				if (toRemove != null) {
					resources.remove(toRemove);
				}
			}
		}
	}

	/**
	 * @see IXtendXpandProject#findExtXptResource(String, String)
	 */
	public IXtendXpandResource findExtXptResource(final String fqn,
			final String extension) {
		assert (fqn != null);
		assert (extension != null);

		if (Activator.getRegisteredResourceContributorFor(extension) == null) {
			throw new IllegalArgumentException(
					"No resource contributor available for extension "
							+ extension);
		}
		IXtendXpandResource res = findCachedXtendXpandResource(fqn, extension);
		if (res == NULL_RESOURCE) {
			return null;
		}
		if (res != null) {
			// ignore stale resources, i.e., resources corresponding to
			// workspace resources that don't exist anymore
			if (res.getUnderlyingStorage() instanceof IFile) {
				IFile workspaceFile = (IFile) res.getUnderlyingStorage();
				if (!workspaceFile.exists()) {
					return null;
				}
			}
			return res;
		}
		// ask to load the resource without looking into jars
		res = loadXtendXpandResource(fqn, extension, false);
		if (res == null) {
			// look into jars
			res = loadXtendXpandResource(fqn, extension, true);
		}
		final ResourceID resId = new ResourceID(fqn, extension);
		resources.put(resId, res != null ? res : NULL_RESOURCE);
		return res;
	}

	/**
	 * @param fqn
	 *            Full qualified name of the searched resource
	 * @param extension
	 * @return The cached resource or <code>null</code> if the resource is not
	 *         known
	 */
	private IXtendXpandResource findCachedXtendXpandResource(final String fqn,
			final String extension) {
		IXtendXpandResource resource = resources.get(new ResourceID(fqn,
				extension));
		if (resource != null) {
			return resource;
		}
		return null;
	}

	/**
	 * Loads an Xtend Resource. Searches the project and all referenced
	 * projects.
	 * 
	 * @param fqn
	 *            Qualified name of the resource, e.g. '
	 *            <tt>org::eclipse::xtend::util::stdlib::io.ext'
	 * @param extension
	 *            The resource's file extension
	 * @param searchJars
	 *            <tt>true</tt> search also in referenced Jar files on the
	 *            classpath
	 * @return The resource or <code>null</code> if not found
	 */
	private IXtendXpandResource loadXtendXpandResource(final String fqn,
			final String extension, final boolean searchJars) {
		return loadXtendXpandResource(fqn, extension, searchJars,
				new HashSet<XtendXpandProject>(5));
	}

	@SuppressWarnings("deprecation")
	private IXtendXpandResource loadXtendXpandResource(final String fqn,
			final String extension, final boolean searchJars,
			final Set<XtendXpandProject> projects) {
		assert (fqn != null);
		assert (extension != null);

		final ResourceID resourceID = new ResourceID(fqn, extension);
		// prevent stackoverflow with recursive project dependencies and
		// resources that could not be found
		if (!projects.add(this)) {
			return null;
		}

		// search the resource. ResourceFinder can be registered using the
		// extension point
		// org.eclipse.xtend.shared.ui.storageFinder
		IStorage storage = Activator.findStorage(project, new ResourceID(fqn,
				extension), searchJars);

		// Found in this project?
		if ((storage != null) && (searchJars || (storage instanceof IFile))) {
			// Find out if the storage is already under another key in the cache
			ResourceID jdtResourceID = JDTUtil.findXtendXpandResourceID(
					getProject(), storage);
			IXtendXpandResource result = null;

			// get the file extension and find the appropriate
			// ResourceContributor for this
			// kind of resource (Xpand/Xtend)
			String fileExtension = storage.getFullPath().getFileExtension();
			final ResourceContributor contr = Activator
					.getRegisteredResourceContributorFor(fileExtension);

			// we have a registered contributor for this resource
			if (contr != null) {
				// load the resource using the ResourceContributor
				if (contr instanceof ResourceContributor2) {
					result = ((ResourceContributor2) contr).create(
							project.getProject(), storage, fqn); // legacy
				} else {
					result = contr.create(storage, fqn); // legacy
				}

				if (result != null) {
					// cache this resource

					resources.put(jdtResourceID, result);
					// remember, because we need to clean the cache, if
					// a jar has been removed
					if (!(storage instanceof IFile)) {
						fromJar.add(jdtResourceID);
					}
					return result;
				}
			}
		}

		// if reached here then the resource was not found in the current
		// project.
		// Now we perform the same search on all referenced projects.
		try {
			final IProject[] p = project.getProject().getReferencedProjects();
			for (IProject project : p) {
				final XtendXpandProject extxptp = (XtendXpandProject) Activator
						.getExtXptModelManager().findProject(project);
				if (extxptp != null) {
					IXtendXpandResource result = extxptp
							.loadXtendXpandResource(fqn, extension, searchJars,
									projects);
					if (result != null) {
						// cache this resource
						// the storage might still be null here, so get it from
						// the result
						if (storage == null) {
							storage = result.getUnderlyingStorage();
						}
						resources.put(resourceID, result);
						return result;
					}
				}
			}
		} catch (final CoreException e) {
			XtendLog.logError(e);
		}
		return null;
	}

	/**
	 * @see IXtendXpandProject#findExtXptResource(IPath, boolean)
	 */
	public IXtendXpandResource findXtendXpandResource(final IStorage file) {
		if (file == null) {
			return null;
		}

		// search the resource ID using storage finders. New resource finders
		// can be registered using the extension point
		// org.eclipse.xtend.shared.ui.storageFinder
		ResourceID id = Activator.findXtendXpandResourceID(project, file);
		if (id == null) {
			return null;
		}
		return findExtXptResource(id.name, id.extension);
	}

	/**
	 * @see IXtendXpandProject#analyze(IProgressMonitor)
	 */
	public void analyze(final IProgressMonitor monitor,
			final ExecutionContext ctx) {
		SubMonitor subMon = SubMonitor.convert(monitor, resources.size());

		for (IXtendXpandResource resource : resources.values()) {
			if (monitor.isCanceled()) {
				return;
			}

			synchronized (resource) {
				if (!isInExternalPackageFragmentRoot(resource)) {
					final String resName = resource.getUnderlyingStorage() != null ? resource
							.getUnderlyingStorage().getFullPath().toString()
							: resource.getFullyQualifiedName();
							subMon.subTask("Analyzing resource " + resName);
							resource.analyze(ctx);
				}
			}
			subMon.worked(1);
		}
	}

	private boolean isInExternalPackageFragmentRoot(
			final IXtendXpandResource resource) {
		IStorage underlyingStorage = resource.getUnderlyingStorage();
		IProject containerProject = null;
		if (underlyingStorage instanceof IJarEntryResource) {
			containerProject = ((IJarEntryResource) underlyingStorage)
					.getPackageFragmentRoot().getJavaProject().getProject();
		}
		if (underlyingStorage instanceof IFile) {
			containerProject = ((IFile) underlyingStorage).getProject();
		}
		return (containerProject != null) && containerProject.isHidden();
	}

	/**
	 * Returns the name of the underlying project.
	 */
	@Override
	public String toString() {
		return project.getPath().toString();
	}

	/**
	 * Returns an object which is an instance of the given class associated with
	 * this object. Returns <code>null</code> if no such object can be found.
	 * <p>
	 * This implementation of the method declared by <code>IAdaptable</code>
	 * passes the request along to the platform's adapter manager; roughly
	 * <code>Platform.getAdapterManager().getAdapter(this, adapter)</code>.
	 * </p>
	 * 
	 * @param adapter
	 *            the class to adapt to
	 * @return the adapted object or <code>null</code>
	 * @see IAdaptable#getAdapter(Class)
	 */
	@SuppressWarnings("rawtypes")
	public final Object getAdapter(final Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}
