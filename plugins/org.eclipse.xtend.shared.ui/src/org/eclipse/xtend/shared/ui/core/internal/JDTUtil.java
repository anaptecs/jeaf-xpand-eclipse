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
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.sourcelookup.containers.ZipEntryStorage;
import org.eclipse.internal.xtend.expression.parser.SyntaxConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ExternalPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.xtend.shared.ui.internal.XtendLog;

@SuppressWarnings("restriction")
// Discouraged access: ExternalPackageFragmentRoot, JarPackageFragmentRoot
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
					} else if (root instanceof ExternalPackageFragmentRoot) {
						IResource resource = ((ExternalPackageFragmentRoot) root)
								.resource();
						if (resource instanceof IContainer) {
							rootFolder = (IContainer) resource;
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
		if (root instanceof JarPackageFragmentRoot) {
			JarPackageFragmentRoot jar = (JarPackageFragmentRoot) root;
			ZipFile zipFile;
			try {
				zipFile = jar.getJar();
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

}
