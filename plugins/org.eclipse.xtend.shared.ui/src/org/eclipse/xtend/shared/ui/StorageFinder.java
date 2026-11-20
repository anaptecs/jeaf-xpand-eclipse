/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.xtend.shared.ui.core.internal.ResourceID;

/**
 * @author Darius Jockel - Initial contribution and API
 */
public interface StorageFinder {
	/**
	 * A priority that indicates the order of StorageFinders. Note that the
	 * JDTStorageFinder, which is always available, has a priority of 1.
	 * Therefore each implementor should use a value different to 1.
	 */
	public int getPriority();

	public IStorage findStorage(IJavaProject project, ResourceID id,
			boolean searchJars);

}
