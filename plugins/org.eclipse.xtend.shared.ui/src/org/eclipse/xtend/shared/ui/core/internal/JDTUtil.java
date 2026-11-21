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
package org.eclipse.xtend.shared.ui.core.internal;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.sourcelookup.containers.ZipEntryStorage;
import org.eclipse.internal.xtend.expression.parser.SyntaxConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.internal.XtendLog;

public class JDTUtil {
	/**
	 * find the path for the oaw name space and extension
	 * 
	 * @param project
	 *            - the javaproject
	 * @param oawns
	 *            - oaw name space (i.e. 'my::xtend::File')
	 * @param ext
	 *            - file extension (i.e. 'ext')
	 * @return
	 */
	public static IStorage findStorage(final IJavaProject project,
			final ResourceID id, final boolean searchJars) {
		IPath p = path(id);
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (IPackageFragmentRoot root2 : roots) {
				IPackageFragmentRoot root = root2;
				if (!root.isArchive()) {

					IContainer rootFolder = null;
					IResource correspondingResource = root
							.getCorrespondingResource();
					if (correspondingResource instanceof IContainer) {
						rootFolder = (IContainer) correspondingResource;
					} else if (root.isExternal()) {
						IContainer externalFolder = getExternalFolder(root);
						if (externalFolder != null) {
							rootFolder = externalFolder;
							IStorage member = (IStorage) rootFolder
									.findMember(p);
							if (member != null) {
								return member;
							}
						}
					}
					if (rootFolder != null) {
						IResource r = project.getProject().findMember(
								rootFolder.getProjectRelativePath().append(p));
						if (r instanceof IFile)
							return (IFile) r;
					}
				} else if (searchJars) {
					IStorage storage = loadFromJar(id, root);
					if (storage != null)
						return storage;
				}
			}
		} catch (JavaModelException e) {
			XtendLog.logInfo(e);
		}
		return null;
	}

	public static IStorage loadFromJar(final ResourceID id,
			final IPackageFragmentRoot root) throws JavaModelException {
		if (root.isArchive()) {
			ZipFile zipFile;
			try {
				zipFile = openArchive(root);
			} catch (CoreException e) {
				XtendLog.logError(e);
				return null;
			}
			ZipEntry entry = zipFile.getEntry(id.toFileName());
			if (entry != null) {
				return new ZipEntryStorage(zipFile, entry);
			}
		}
		return null;
	}

	public static ResourceID findXtendXpandResourceID(
			final IJavaProject project, final IStorage file) {
		if (file == null)
			return null;
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (IPackageFragmentRoot root : roots) {
				if (root.getPath().isPrefixOf(file.getFullPath())) {
					IPath shortOne = file.getFullPath().removeFirstSegments(
							root.getPath().segmentCount());
					if (shortOne.getDevice() != null) {
						int j = shortOne.toString().indexOf(
								IPath.DEVICE_SEPARATOR);
						if (j != -1) {
							shortOne = new Path(shortOne.toString().substring(
									j + 1, shortOne.toString().length()));
						}
					}
					return toResourceID(shortOne);
				}
			}
		} catch (JavaModelException e1) {
			XtendLog.logInfo(e1);
		}
		return null;
	}

	public static IJavaProject getJProject(final IStorage s) {
		if (s instanceof IFile) {
			return JavaCore.create(((IFile) s).getProject());
		} else {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
					.getProjects();
			for (IProject project : projects) {
				IJavaProject p = JavaCore.create(project);
				if (p.exists()) {
					IPackageFragmentRoot[] roots;
					try {
						roots = p.getPackageFragmentRoots();
						for (IPackageFragmentRoot root : roots) {
							if (root.getPath().isPrefixOf(s.getFullPath()))
								return p;
						}
					} catch (JavaModelException e) {
						XtendLog.logError(e);
					}
				}
			}
		}
		return null;
	}

	private static IPath path(final ResourceID id) {
		return new Path(id.name.replace(SyntaxConstants.NS_DELIM, "/") + "."
				+ id.extension);
	}

	private static ResourceID toResourceID(final IPath path) {
		return new ResourceID(path.removeFileExtension().toString()
				.replace("/", SyntaxConstants.NS_DELIM),
				path.getFileExtension());
	}

	public static String getQualifiedName(final IStorage source) {
		ResourceID id = findXtendXpandResourceID(getJProject(source), source);
		if (id != null)
			return id.name;
		return null;
	}

	/**
	 * Opens the archive (e.g. JAR file) of the passed package fragment root. Replaces JDT's internal
	 * <code>JarPackageFragmentRoot.getJar()</code>, which also opens the archive by its path.
	 * 
	 * @param root
	 *            Package fragment root of an archive. The parameter must not be null.
	 * @return {@link ZipFile} Opened archive. The method never returns null.
	 * @throws CoreException
	 *             if the archive can't be opened.
	 */
	public static ZipFile openArchive(final IPackageFragmentRoot root) throws CoreException {
		// Archives within the workspace have a resource, external archives are identified by their file system path.
		IResource resource = root.getResource();
		IPath location = resource != null ? resource.getLocation() : root.getPath();
		if (location == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), "Archive not found: " + root.getPath()));
		}
		try {
			return new ZipFile(location.toFile());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), "Archive can't be opened: " + location, e));
		}
	}

	/**
	 * Returns the workspace folder of an external class folder. JDT links external class folders into a hidden project
	 * of the workspace. Replaces JDT's internal <code>ExternalPackageFragmentRoot.resource()</code>.
	 * 
	 * @param root
	 *            Package fragment root. The parameter must not be null.
	 * @return {@link IContainer} Folder of the external class folder or null if the root is no external class folder or
	 *         the folder isn't linked into the workspace.
	 */
	public static IContainer getExternalFolder(final IPackageFragmentRoot root) {
		IContainer folder = null;
		if (root.isExternal() && !root.isArchive()) {
			IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot()
					.findContainersForLocationURI(root.getPath().toFile().toURI(), IContainer.INCLUDE_HIDDEN);
			for (IContainer container : containers) {
				if (container.exists()) {
					folder = container;
					break;
				}
			}
		}
		return folder;
	}
}
