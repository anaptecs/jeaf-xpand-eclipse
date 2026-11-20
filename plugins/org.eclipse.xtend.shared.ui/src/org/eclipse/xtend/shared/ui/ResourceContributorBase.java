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
package org.eclipse.xtend.shared.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.internal.xtend.xtend.parser.ErrorHandler;
import org.eclipse.internal.xtend.xtend.parser.XtendError;
import org.eclipse.xtend.expression.Resource;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandMarkerManager;

/**
 * Base implementation of the {@link ResourceContributor} interface with common
 * used methods.
 * 
 * @author kthoms
 */
public abstract class ResourceContributorBase implements ResourceContributor2 {

	protected ErrorHandler getErrorHandler(final IStorage source) {
		if (source instanceof IFile) {
			// XtendXpandMarkerManager.deleteMarkers((IFile) source);
		}
		return new ErrorHandler() {

			public void handleError(final XtendError e) {
				// BNI bug#312571
				// 1. start position can be 0 (missing semicolon at the end of a
				// file)
				// 2. why should only one error be handled (!hasErrors)
				int start = e.getStart();
				int end = e.getEnd();
				if (source instanceof IFile) {
					IFile f = (IFile) source;
					if (start == 0 && end == 1
							&& e.getMessage().contains("<EOF>")) {
						// BNI find the offset of the last charcter!
						try {
							Reader reader = createReader(source);
							while (reader.read() != -1) {
								start++;
								end++;
							}
							start--;
							end--;
						} catch (IOException e2) {
							logError(e2.getMessage(), e2);
						}
					}
					if (start > 0) {
						XtendXpandMarkerManager.addErrorMarker(f,
								e.getMessage(), IMarker.SEVERITY_ERROR, start,
								end);
					}
				}
			}
		};
	}

	protected abstract void logInfo(String message);

	protected abstract void logError(String message, Throwable t);

	@Deprecated
	public final IXtendXpandResource create(final IStorage file,
			final String fqn) {
		return create(null, file, fqn);
	}

	public IXtendXpandResource create(final IProject project,
			final IStorage storage, final String fqn) {
		Resource res = parse(project, storage, fqn);

		if (res != null) {
			return createExtXptResource(res, project, storage);
		}
		return null;
	}

	protected Reader createReader(final IStorage resource) {
		InputStream in;
		try {
			in = resource.getContents();
			return new InputStreamReader(in);
		} catch (final CoreException e1) {
			logInfo(e1.getMessage());
			return null;
		}
	}

	/**
	 * @deprecated Use {@link #parse(IProject, IStorage, String)}
	 */
	@Deprecated
	public Resource parse(final IStorage source, final String fqn) {
		return parse(null, source, fqn);
	}

	/**
	 * Parse the resource file.
	 */
	protected abstract Resource parse(IProject project, IStorage source,
			String fqn);

	/**
	 * @deprecated Use
	 *             {@link #createExtXptResource(Resource, IProject, IStorage)}
	 */
	@Deprecated
	protected IXtendXpandResource createExtXptResource(final Resource resource,
			final IStorage source) {
		return createExtXptResource(resource, null, source);
	}

	protected abstract IXtendXpandResource createExtXptResource(
			Resource resource, IProject project, IStorage source);

}
