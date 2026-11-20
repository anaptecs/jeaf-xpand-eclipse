/*******************************************************************************
 * Copyright (c) 2005, 2006 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Platform;
import org.eclipse.xtend.expression.AnalysationIssue;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtend.expression.Resource;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandMarkerManager;

public abstract class AbstractResource implements IXtendXpandResource {
	private class StubResource implements Resource {
		private String fullyQualifiedName;

		public String getFullyQualifiedName() {
			return fullyQualifiedName;
		}

		public String[] getImportedExtensions() {
			return new String[0];
		}

		public String[] getImportedNamespaces() {
			return new String[0];
		}

		public void setFullyQualifiedName(final String fqn) {
			fullyQualifiedName = fqn;
		}

	}

	private final IStorage underlyingStorage;

	private Resource resource;

	protected AbstractResource(final IStorage res) {
		underlyingStorage = res;
	}

	public void setExtXptResource(final Resource res) {
		resource = res;
	}

	public Resource getExtXptResource() {
		if (resource == null)
			resource = new StubResource();
		return resource;
	}

	public IStorage getUnderlyingStorage() {
		return underlyingStorage;
	}

	public String getFullyQualifiedName() {
		return getExtXptResource().getFullyQualifiedName();
	}

	public String[] getImportedNamespaces() {
		return getExtXptResource().getImportedNamespaces();
	}

	public void setFullyQualifiedName(final String fqn) {
		getExtXptResource().setFullyQualifiedName(fqn);
	}

	public String[] getImportedExtensions() {
		return getExtXptResource().getImportedExtensions();
	}

	// private boolean hasSemanticErrors = false;

	public final void analyze(final ExecutionContext ctx) {
		if (getUnderlyingStorage() instanceof IFile) {
			IFile f = (IFile) getUnderlyingStorage();
			final Set<AnalysationIssue> issues = new HashSet<AnalysationIssue>();
			analyze(ctx, issues);
			// BNI bug#312569 why should markers be deleted here?
			// They will be deleted by creation of errorHandler before parsing
			// the file.
			// if they are deleted here, parser errors will not appear as
			// ErrorMarkers
			// if (hasSemanticErrors) {
			// XtendXpandMarkerManager.deleteMarkers(f);
			// }
			// hasSemanticErrors = !issues.isEmpty();
			for (AnalysationIssue analysationIssue : issues) {
				XtendXpandMarkerManager.addMarker(f, analysationIssue);
			}
			// BNI delete markers if no issues were found
			// if syntax errors were found markers will be added in
			// XtendXpandMarkerManager.finishBuild() after build
			if (issues.isEmpty()) {
				XtendXpandMarkerManager.deleteMarkers(f);
			}
		}
	}

	protected abstract void analyze(ExecutionContext ctx,
			Set<AnalysationIssue> issues);

	public final boolean refresh() {
		return internalRefresh();
	}

	protected abstract boolean internalRefresh();

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
		Object result;
		result = Platform.getAdapterManager().getAdapter(this, adapter);
		if (result == null) {
			result = underlyingStorage.getAdapter(adapter);
		}
		return result;
	}

	@Override
	public String toString() {
		return resource.toString();
	}

}
