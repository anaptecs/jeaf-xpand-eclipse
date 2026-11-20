/*
 * Copyright (c) 2014 Yatta Solutions GmbH and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Manuel Bork <bork@yatta.de> (Yatta Solutions GmbH) - initial API and implementation
 */
package org.eclipse.xtend.typesystem.uml2.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.uml2.uml.Profile;

/**
 * Implementors are notified whenever a new Profile in the workspace has been loaded by the {@link Uml2AdapterPlugin}.
 *
 * @since 2.0
 */
public interface UmlPluginListener {

	void profileLoaded(IFile file, Profile profile);

}
