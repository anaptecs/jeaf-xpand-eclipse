/*******************************************************************************
 * Copyright (c) 2005, 2007 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/

package org.eclipse.xtend.shared.ui.core.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.internal.xtend.expression.parser.SyntaxConstants;
import org.eclipse.internal.xtend.xtend.XtendFile;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.ResourceContributor;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.shared.ui.core.internal.BuildState;
import org.eclipse.xtend.shared.ui.core.internal.JDTUtil;
import org.eclipse.xtend.shared.ui.core.internal.ResourceID;
import org.eclipse.xtend.shared.ui.core.preferences.PreferenceConstants;
import org.eclipse.xtend.shared.ui.internal.XtendLog;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;

/**
 * Xpand Project Builder. Traverses the project and searches for Xpand related
 * sources (file extensions contributed by ResourceContributors). Then loads and
 * analyzes the resources.
 *
 * @author karsten.thoms@itemis.de - Maintainance and bugfixing
 * @author ed.merks@gmail.com - Bug#388751
 * @author benedikt.niehues@itemis.de - Bug#339168
 * @author dhubner - Bugfixing
 * @author pschonbac - Bugfixing
 * @author jkohnlein - Bugfixing
 * @author bkolb - Initial
 * @author openArchitectureWare Team - Initial contribution and API
 */
public class XtendXpandBuilder extends IncrementalProjectBuilder {
	// --------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------
	class XtendXpandDeltaVisitor implements IResourceDeltaVisitor,
			IResourceVisitor {
		private final IProgressMonitor monitor;
		private IFolder outputFolder;
		private final List<IResource> toLoad = new LinkedList<IResource>();
		private final List<IResource> toUnload = new LinkedList<IResource>();

		public XtendXpandDeltaVisitor(final IProgressMonitor monitor) {
			this.monitor = monitor;
			IProject project = getProject();
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);
					IPath outputLocation = javaProject.getOutputLocation();
					if (outputLocation != null) {
						outputFolder = project.getWorkspace().getRoot()
								.getFolder(outputLocation);
					}
				}
			} catch (CoreException e) {
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
		 * .core.resources.IResourceDelta)
		 */
		public boolean visit(final IResourceDelta delta) throws CoreException {
			final IResource resource = delta.getResource();
			if (isXtendXpandResource(resource)) {
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					toLoad.add(resource);
					break;
				case IResourceDelta.REMOVED:
					toUnload.add(resource);
					break;
				case IResourceDelta.CHANGED:
					toLoad.add(resource);
					break;
				}
				monitor.worked(1);
			}
			// prune visit of the Java output folder
			return !resource.equals(outputFolder);
		}

		private boolean isXtendXpandResource(final IResource resource) {
			return resource.getType() == IResource.FILE
					&& extensions.contains(resource.getFileExtension());
		}

		public boolean visit(final IResource resource) {
			if (isXtendXpandResource(resource)) {
				toLoad.add(resource);
			}
			return !resource.equals(outputFolder);
		}

	}

	private class ResourceFilter implements Predicate<String> {
		private final Set<Pattern> patterns;

		public ResourceFilter(final IProject project) {
			patterns = new HashSet<Pattern>();
			ScopedPreferenceStore store = new ScopedPreferenceStore(
					new ProjectScope(project), Activator.getId());
			store.setSearchContexts(new IScopeContext[] {
					new ProjectScope(project), InstanceScope.INSTANCE });
			Splitter splitter = Splitter.on(",").omitEmptyStrings()
					.trimResults();
			Iterable<String> exclusionPaths = splitter.split(store
					.getString(PreferenceConstants.BUILDER_EXCLUSIONS));
			for (String s : exclusionPaths) {
				String path = project.getFullPath() + "/" + s;
				path = path.replace(".", "\\.");
				path = path.replace("*", ".*");
				try {
					Pattern pattern = Pattern.compile(path);
					patterns.add(pattern);
				} catch (PatternSyntaxException pse) {
					XtendLog.logInfo("Invalid exclusion pattern: " + s);
				}
			}
		}

		public boolean apply(final String input) {
			for (Pattern p : patterns) {
				if (p.matcher(input).matches()) {
					return false;
				}
			}
			return true;
		}

	}

	// --------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------

	private final Set<String> extensions;

	public XtendXpandBuilder() {
		extensions = new HashSet<String>();
		final ResourceContributor[] contributors = Activator
				.getRegisteredResourceContributors();
		for (final ResourceContributor resourceContributor : contributors) {
			extensions.add(resourceContributor.getFileExtension());
		}
	}

	private int incrementalAnalyzerStrategy;

	private void updateIncrementalAnalyzerStrategy() {
		final ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(
				InstanceScope.INSTANCE, Activator.getId());
		incrementalAnalyzerStrategy = scopedPreferenceStore
				.getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY);
	}

	private boolean analyzeReverseReferencedResources() {
		return incrementalAnalyzerStrategy == PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY_FILE_ONLY_WITH_REVERSE_REFERENCE;
	}

	private boolean analyzeWholeProjectWhenIncremental() {
		switch (incrementalAnalyzerStrategy) {
		case PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY_PROJECT:
		case PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY_PROJECT_AND_DEPENDENT:
			return true;
		default:
			return false;
		}
	}

	public static final String getBUILDER_ID() {
		return Activator.getId() + ".xtendBuilder";
	}

	private Set<IXtendXpandResource> toAnalyze = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IProject[] build(final int kind,
			@SuppressWarnings("rawtypes") final Map args,
			final IProgressMonitor monitor) throws CoreException {
		SubMonitor buildMonitor = SubMonitor.convert(monitor, 100);
		SubMonitor subMon = null;
		toAnalyze = new HashSet<IXtendXpandResource>();

		// load / unload resources; fill toAnalyze
		try {
			subMon = buildMonitor.newChild(40);
			final IResourceDelta delta = getDelta(getProject());
			if (kind == FULL_BUILD || kind == CLEAN_BUILD || delta == null) {
				fullBuild(subMon);
			} else {
				incrementalBuild(delta, subMon);
			}
		} catch (final Throwable e) {
			XtendLog.logError(e);
		}

		subMon = buildMonitor.newChild(55);
		analyzeResources(kind, buildMonitor);

		buildMonitor.subTask("Updating resource markers");
		XtendXpandMarkerManager.finishBuild();
		buildMonitor.worked(5);
		toAnalyze = null;
		return null;
	}

	private void analyzeResources(final int kind, final SubMonitor monitor) {
		final int work = toAnalyze.size() + 1;
		monitor.setWorkRemaining(work);
		monitor.subTask("Collecting resources for analysis");

		applyResourceFilter(toAnalyze);
		Map<IXtendXpandProject, List<IXtendXpandResource>> groupByProject = new HashMap<IXtendXpandProject, List<IXtendXpandResource>>();
		// we need one execution context per project, but resources might be
		// from several projects
		for (IXtendXpandResource resource : toAnalyze) {
			final IXtendXpandProject xtdxptProject = (IXtendXpandProject) resource
					.getAdapter(IXtendXpandProject.class);
			List<IXtendXpandResource> resources = groupByProject
					.get(xtdxptProject);
			if (resources == null) {
				resources = new ArrayList<IXtendXpandResource>();
				groupByProject.put(xtdxptProject, resources);
			}
			resources.add(resource);
		}
		monitor.worked(1);

		// Now analyze the resources per project. Create one execution context
		// per project.
		int i = 1;
		for (IXtendXpandProject project : groupByProject.keySet()) {
			final ExecutionContext execCtx = Activator
					.getExecutionContext(project.getProject());
			BuildState.set(execCtx);
			try {
				for (IXtendXpandResource resource : groupByProject.get(project)) {
					final String resName = resource.getUnderlyingStorage() != null ? resource
							.getUnderlyingStorage().getFullPath().toString()
							: resource.getFullyQualifiedName();
					final StringBuilder msg = new StringBuilder(
							"Analyzing resource ").append(i++).append('/')
							.append(work - 1).append(' ').append(resName);
					monitor.subTask(msg.toString());
					resource.analyze(execCtx);
					monitor.worked(1);
				}
			} finally {
				BuildState.remove(execCtx);
			}
		}
	}

	private void applyResourceFilter(final Set<IXtendXpandResource> resources) {
		if (resources.isEmpty())
			return;

		Set<IXtendXpandResource> toRemove = new HashSet<IXtendXpandResource>();

		Map<IProject, ResourceFilter> projectFilters = new HashMap<IProject, ResourceFilter>();
		// 1st pass: build filters
		for (IXtendXpandResource resource : resources) {
			IProject project = (IProject) resource.getAdapter(IProject.class);
			if (projectFilters.get(project) == null) {
				projectFilters.put(project, new ResourceFilter(project));
			}
		}

		for (IXtendXpandResource resource : resources) {
			IProject project = (IProject) resource.getAdapter(IProject.class);
			ResourceFilter filter = projectFilters.get(project);
			IStorage storage = resource.getUnderlyingStorage();

			if (!filter.apply(storage.getFullPath().toString())) {
				toRemove.add(resource);
			}
		}
		resources.removeAll(toRemove);
	}

	private void fillToAnalyzeWithReverseReferencedResources() {
		Set<IXtendXpandResource> reverseReferences = new HashSet<IXtendXpandResource>();
		for (final Object name : toAnalyze) {
			final IXtendXpandResource res = (IXtendXpandResource) name;
			final IResource resource = (IResource) res.getUnderlyingStorage();
			final IProject project = resource.getProject();
			if (!project.isLinked()) {
				final IXtendXpandProject xtdxptProject = Activator
						.getExtXptModelManager().findProject(project);
				IXtendXpandResource[] resources = xtdxptProject
						.getAllRegisteredResources();
				for (IXtendXpandResource iXtendXpandResource : resources) {
					if (iXtendXpandResource != null) {
						for (String string : iXtendXpandResource
								.getImportedExtensions()) {
							IXtendXpandResource importedResource = xtdxptProject
									.findExtXptResource(string,
											XtendFile.FILE_EXTENSION);
							if (importedResource != null
									&& importedResource.equals(res)) {
								reverseReferences.add(iXtendXpandResource);
								break;
							}
						}
					}
				}
			}
		}
		toAnalyze.addAll(reverseReferences);
	}

	void reloadResource(final IFile resource) {
		if (resource.exists()) {
			final IXtendXpandProject project = Activator
					.getExtXptModelManager().findProject(resource);
			if (project != null) {
				final IXtendXpandResource r = project
						.findXtendXpandResource(resource);
				if (r != null) {
					if (r.refresh()) {
						resource.getLocalTimeStamp();
					}
					if (!isInExternalPackageFragmentRoot(r)) {
						toAnalyze.add(r);
					}
				}
			}
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

	// TODO: Reduce visibility
	public void handleRemovement(final IFile resource) {
		// findProject creates a new project, avoid call if project is about to
		// be removed
		// this will be handled by the XtendXpandModelManager itself
		final IXtendXpandProject project = Activator.getExtXptModelManager()
				.findProject(resource);
		if (project != null) {
			project.unregisterXtendXpandResource(project
					.findXtendXpandResource(resource));
		} else {
			XtendLog.logInfo("No Xpand project found for "
					+ resource.getProject().getName());
		}
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		IProject iProject = getProject();
		final IXtendXpandProject project = (IXtendXpandProject) iProject
				.getAdapter(IXtendXpandProject.class);
		if (project != null) {
			XtendXpandDeltaVisitor visitor = new XtendXpandDeltaVisitor(monitor);
			iProject.accept(visitor);
			processResources(monitor, visitor.toLoad, visitor.toUnload);

			scanArchives(monitor, project);
			monitor.worked(1);
		} else {
			XtendLog.logInfo("Couldn't create Xpand project for project "
					+ iProject.getName());
		}
	}

	/**
	 * Load / unload the Xpand resources
	 */
	void processResources(final IProgressMonitor monitor,
			final List<IResource> toLoad, final List<IResource> toUnload) {
		final int work = toLoad.size() + toUnload.size();
		SubMonitor subMon = SubMonitor.convert(monitor, work);
		int i = 0;
		for (IResource r : toLoad) {
			final StringBuilder msg = new StringBuilder("Loading resource ")
					.append(i++).append('/').append(toLoad.size()).append(' ')
					.append(r.getFullPath());
			subMon.subTask(msg.toString());
			reloadResource((IFile) r);
			subMon.worked(1);
		}
		i = 0;
		for (IResource r : toUnload) {
			final StringBuilder msg = new StringBuilder("Unloading resource ")
					.append(i++).append('/').append(toUnload.size())
					.append(' ').append(r.getFullPath());
			subMon.subTask(msg.toString());
			handleRemovement((IFile) r);
			subMon.worked(1);
		}
	}

	/**
	 * Scans the IPackageFragmentRoot of type archive that are on the project's
	 * classpath.
	 */
	private void scanArchives(final IProgressMonitor monitor,
			final IXtendXpandProject project) throws JavaModelException {
		// scan Jars on classpath
		final IJavaProject jp = project.getProject();
		final IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
		List<IPackageFragmentRoot> archives = new ArrayList<IPackageFragmentRoot>(
				roots.length);
		for (IPackageFragmentRoot root : roots) {
			if (root.isArchive())
				archives.add(root);
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Scanning archives ", archives.size());
		for (final IPackageFragmentRoot root : archives) {
			try {
				String archiveName = root.getCorrespondingResource() != null ? root
						.getCorrespondingResource().getFullPath().toString()
						: root.getElementName();
				final ZipFile zip = JDTUtil.openArchive(root);
				// avoid scanning of Jars that are known to have no
				// Xpand resource
				if (Activator.getDefault().isBlacklistedJar(
						new File(zip.getName()).getName())) {
					continue;
				}
				subMonitor.subTask("Scanning archive " + archiveName);
				boolean addToBlacklist = true; // remember Jars that do not
				// contain Xpand resources
				final Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					final ZipEntry entry = entries.nextElement();
					if (entry.getSize() == 0) {
						continue; // skip directories and empty
						// files
					}
					final String name = entry.getName();
					final int fileExtOffset = name.lastIndexOf('.');
					final String ext = fileExtOffset > 0 ? name
							.substring(fileExtOffset + 1) : null;
					if (extensions.contains(ext)) {
						final String fqn = name.substring(0, fileExtOffset)
								.replace("/", SyntaxConstants.NS_DELIM);
						final ResourceID resourceID = new ResourceID(fqn, ext);
						final IStorage findStorage = JDTUtil.loadFromJar(
								resourceID, root);
						subMonitor.subTask("Scanning archive " + archiveName
								+ " - Loading resource "
								+ findStorage.getFullPath());
						project.findXtendXpandResource(findStorage);
						addToBlacklist = false;
					}
				}
				if (addToBlacklist) {
					Activator.getDefault().addToJarBlacklist(
							new File(zip.getName()).getName());
				}
			} catch (CoreException ex) {
				XtendLog.logError(ex);
			}
		}
	}

	protected void incrementalBuild(final IResourceDelta delta,
			final IProgressMonitor monitor) throws CoreException {
		updateIncrementalAnalyzerStrategy();
		final XtendXpandDeltaVisitor visitor = new XtendXpandDeltaVisitor(
				monitor);
		delta.accept(visitor);
		if (analyzeReverseReferencedResources()) {
			fillToAnalyzeWithReverseReferencedResources();
		}
		if (analyzeWholeProjectWhenIncremental()) {
			// collect all resources from the projects
			final XtendXpandDeltaVisitor visitor2 = new XtendXpandDeltaVisitor(
					new NullProgressMonitor());
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(final IResourceDelta delta)
						throws CoreException {
					if (delta.getResource().getType() == IResource.PROJECT) {
						visitor2.visit(delta.getResource());
					}
					return false;
				}
			});
			for (IResource res : visitor2.toLoad) {
				IXtendXpandResource xpr = (IXtendXpandResource) res
						.getAdapter(IXtendXpandResource.class);
				if (xpr != null) {
					toAnalyze.add(xpr);
				}
			}
		}
		processResources(monitor, visitor.toLoad, visitor.toUnload);
	}

}
