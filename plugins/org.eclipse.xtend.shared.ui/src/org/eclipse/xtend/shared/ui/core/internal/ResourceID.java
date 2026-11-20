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

import org.eclipse.internal.xtend.expression.parser.SyntaxConstants;

public class ResourceID {
	public final String name;
	public final String extension;
	private final int hashCode;

	public ResourceID(final String name, final String extension) {
		assert name != null;
		assert extension != null;
		this.name = name;
		this.extension = extension;
		hashCode = name.hashCode() + extension.hashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj != null && obj instanceof ResourceID) {
			ResourceID id = (ResourceID) obj;
			return name.equals(id.name) && extension.equals(id.extension);
		}
		return false;
	}

	@Override
	public String toString() {
		return name + "." + extension;
	}

	public String toFileName() {
		return name.replace(SyntaxConstants.NS_DELIM, "/") + "." + extension;
	}
}
