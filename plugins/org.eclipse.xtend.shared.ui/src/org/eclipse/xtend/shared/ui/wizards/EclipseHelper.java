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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.xtend.shared.ui.internal.XtendLog;

public class EclipseHelper {

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
