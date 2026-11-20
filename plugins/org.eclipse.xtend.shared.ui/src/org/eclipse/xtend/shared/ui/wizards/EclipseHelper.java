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
package org.eclipse.xtend.shared.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.xtend.shared.ui.Messages;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandBuilder;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.eclipse.xtend.shared.ui.internal.XtendLog;

public class EclipseHelper {

	public static IProject createExtXptProject(final String projectName,
			final List<String> srcFolders,
			final List<IProject> referencedProjects,
			final Set<String> requiredBundles,
			final List<String> exportedPackages,
			final IProgressMonitor progressMonitor, final Shell theShell) {
		IProject project = null;
		try {
			progressMonitor.beginTask("", 10);
			progressMonitor.subTask(Messages.EclipseHelper_TaskTitle
					+ projectName);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			project = workspace.getRoot().getProject(projectName);

			// Clean up any old project information.
			if (project.exists()) {
				final boolean[] result = new boolean[1];
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					public void run() {
						result[0] = MessageDialog.openQuestion(theShell,
								Messages.EclipseHelper_OvewritePrompt
										+ projectName,
								Messages.EclipseHelper_Warning1 + projectName
										+ Messages.EclipseHelper_Warning2);
					}
				});
				if (result[0]) {
					project.delete(true, true, new SubProgressMonitor(
							progressMonitor, 1));
				} else {
					return null;
				}
			}

			IJavaProject javaProject = JavaCore.create(project);
			IProjectDescription projectDescription = ResourcesPlugin
					.getWorkspace().newProjectDescription(projectName);
			projectDescription.setLocation(null);
			project.create(projectDescription, new SubProgressMonitor(
					progressMonitor, 1));
			List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
			if (referencedProjects.size() != 0) {
				projectDescription.setReferencedProjects(referencedProjects
						.toArray(new IProject[referencedProjects.size()]));
				for (IProject referencedProject : referencedProjects) {
					IClasspathEntry referencedProjectClasspathEntry = JavaCore
							.newProjectEntry(referencedProject.getFullPath());
					classpathEntries.add(referencedProjectClasspathEntry);
				}
			}

			// Set Natures
			List<String> natures = new ArrayList<String>(3);
			natures.add(JavaCore.NATURE_ID);
			natures.add("org.eclipse.pde.PluginNature");
			natures.add(XtendXpandNature.NATURE_ID);
			if (Platform.getBundle("org.eclipse.xtext.ui.shared") != null) {
				natures.add("org.eclipse.xtext.ui.shared.xtextNature");
			}
			projectDescription.setNatureIds(natures.toArray(new String[0]));

			ICommand java = projectDescription.newCommand();
			java.setBuilderName(JavaCore.BUILDER_ID);

			ICommand manifest = projectDescription.newCommand();
			manifest.setBuilderName("org.eclipse.pde.ManifestBuilder");

			ICommand schema = projectDescription.newCommand();
			schema.setBuilderName("org.eclipse.pde.SchemaBuilder");

			ICommand extxpt = projectDescription.newCommand();
			extxpt.setBuilderName(XtendXpandBuilder.getBUILDER_ID());

			projectDescription.setBuildSpec(new ICommand[] { java, manifest,
					schema, extxpt });

			project.open(new SubProgressMonitor(progressMonitor, 1));
			project.setDescription(projectDescription, new SubProgressMonitor(
					progressMonitor, 1));

			Collections.reverse(srcFolders);
			for (String src : srcFolders) {
				IFolder srcContainer = project.getFolder(src);
				if (!srcContainer.exists()) {
					srcContainer.create(false, true, new SubProgressMonitor(
							progressMonitor, 1));
				}
				IClasspathEntry srcClasspathEntry = JavaCore
						.newSourceEntry(srcContainer.getFullPath());
				classpathEntries.add(0, srcClasspathEntry);
			}

			classpathEntries.add(JavaCore.newContainerEntry(new Path(
					"org.eclipse.jdt.launching.JRE_CONTAINER")));
			classpathEntries.add(JavaCore.newContainerEntry(new Path(
					"org.eclipse.pde.core.requiredPlugins")));

			javaProject.setRawClasspath(classpathEntries
					.toArray(new IClasspathEntry[classpathEntries.size()]),
					new SubProgressMonitor(progressMonitor, 1));

			javaProject.setOutputLocation(new Path("/" + projectName + "/bin"),
					new SubProgressMonitor(progressMonitor, 1));
			createManifest(projectName, requiredBundles, exportedPackages,
					progressMonitor, project);
			createBuildProps(progressMonitor, project);
		} catch (Exception exception) {
			exception.printStackTrace();
			XtendLog.logError(exception);
		} finally {
			progressMonitor.done();
		}

		return project;
	}

	public static IFile createFile(final String name,
			final IContainer container, final String content,
			final IProgressMonitor progressMonitor) {
		IFile file = container.getFile(new Path(name));
		assertExist(file.getParent());
		try {
			InputStream stream = new ByteArrayInputStream(content.getBytes(file
					.getCharset()));
			if (file.exists()) {
				file.setContents(stream, true, true, progressMonitor);
			} else {
				file.create(stream, true, progressMonitor);
				// file.setCharset(ResourcesPlugin.getEncoding(),
				// progressMonitor);
			}
			stream.close();
		} catch (Exception e) {
			XtendLog.logError(e);
		}
		progressMonitor.worked(1);

		return file;
	}

	private static void createBuildProps(
			final IProgressMonitor progressMonitor, final IProject project) {
		StringBuilder bpContent = new StringBuilder("source.. = src/,\\\n");
		bpContent.append("           src-gen\n");
		bpContent.append("bin.includes = META-INF/,.\n");
		createFile("build.properties", project, bpContent.toString(),
				new NullProgressMonitor());
	}

	private static void createManifest(final String projectName,
			final Set<String> requiredBundles,
			final List<String> exportedPackages,
			final IProgressMonitor progressMonitor, final IProject project)
			throws CoreException {
		StringBuilder mainContent = new StringBuilder("Manifest-Version: 1.0\n");
		mainContent.append("Bundle-ManifestVersion: 2\n");
		mainContent.append("Bundle-Name: " + projectName + "\n");
		mainContent.append("Bundle-SymbolicName: " + projectName
				+ "; singleton:=true\n");
		mainContent.append("Bundle-Version: 1.0.0\n");
		// maniContent.append("Bundle-Localization: plugin\n");

		// Add required bundles
		mainContent.append("Require-Bundle: ");
		for (String entry : requiredBundles) {
			mainContent.append(" " + entry + ",\n");
		}
		mainContent.append(" com.ibm.icu;bundle-version=\"4.0.1\",\n");
		mainContent.append(" org.antlr.runtime;bundle-version=\"3.0.0\",\n");
		mainContent
				.append(" org.eclipse.core.runtime;bundle-version=\"3.5.0\",\n");
		mainContent
				.append(" org.eclipse.emf.mwe.utils;bundle-version=\"1.0.0\",\n");
		mainContent
				.append(" org.eclipse.emf.ecore.xmi;bundle-version=\"2.5.0\",\n");
		mainContent
				.append(" org.eclipse.jface.text;bundle-version=\"3.5.0\",\n");
		mainContent.append(" org.eclipse.xpand;bundle-version=\"1.0.0\",\n");
		mainContent.append(" org.eclipse.xtend;bundle-version=\"1.0.0\",\n");
		mainContent
				.append(" org.eclipse.xtend.typesystem.emf;bundle-version=\"1.0.0\",\n");
		mainContent
				.append(" org.eclipse.xtend.backend;bundle-version=\"1.0.0\";resolution:=optional,\n");
		mainContent
				.append(" org.eclipse.xtend.middleend.xpand;bundle-version=\"1.0.0\";resolution:=optional,\n");
		mainContent
				.append(" org.eclipse.xtend.middleend.xtend;bundle-version=\"1.0.0\";resolution:=optional,\n");
		mainContent
				.append(" org.eclipse.emf.mwe2.launch;resolution:=optional\n");
		mainContent.append("Bundle-RequiredExecutionEnvironment: J2SE-1.5\n");

		if (exportedPackages != null && !exportedPackages.isEmpty()) {
			mainContent.append("Require-Bundle: " + exportedPackages.get(0));
			for (int i = 1, x = exportedPackages.size(); i < x; i++) {
				mainContent.append(",\n " + exportedPackages.get(i));
			}
			mainContent.append("\n");
		}

		IFolder metaInf = project.getFolder("META-INF");
		if (metaInf.exists())
			metaInf.delete(false, progressMonitor);
		metaInf.create(false, true, new SubProgressMonitor(progressMonitor, 1));
		createFile("MANIFEST.MF", metaInf, mainContent.toString(),
				new NullProgressMonitor());
	}

	/**
	 * @param name
	 *            of the destination file
	 * @param container
	 *            directory containing the the destination file
	 * @param contentUrl
	 *            Url pointing to the src of the content
	 * @param progressMonitor
	 *            used to interact with and show the user the current operation
	 *            status
	 * @return
	 */
	public static IFile createFile(final String name,
			final IContainer container, final URL contentUrl,
			final IProgressMonitor progressMonitor) {

		IFile file = container.getFile(new Path(name));
		InputStream inputStream = null;
		try {
			inputStream = contentUrl.openStream();
			if (file.exists()) {
				file.setContents(inputStream, true, true, progressMonitor);
			} else {
				file.create(inputStream, true, progressMonitor);
			}
			inputStream.close();
		} catch (Exception e) {
			XtendLog.logError(e);
		} finally {
			if (null != inputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					XtendLog.logError(e);
				}
			}
		}
		progressMonitor.worked(1);

		return file;
	}

	private static void assertExist(final IContainer c) {
		if (!c.exists()) {
			if (!c.getParent().exists()) {
				assertExist(c.getParent());
			}
			if (c instanceof IFolder) {
				try {
					((IFolder) c)
							.create(false, true, new NullProgressMonitor());
				} catch (CoreException e) {
					XtendLog.logError(e);
				}
			}

		}

	}

	public static void openFileToEdit(final Shell s, final IFile file) {
		s.getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
					XtendLog.logError(e);
				}
			}
		});
	}
}
