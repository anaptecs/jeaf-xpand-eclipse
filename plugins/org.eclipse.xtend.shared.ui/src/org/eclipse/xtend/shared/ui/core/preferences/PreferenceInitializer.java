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

package org.eclipse.xtend.shared.ui.core.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.xtend.shared.ui.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		// Defaults for templates of JEAF Generator: UML2 profiles and oAW-Classic metamodel
		store.setDefault(PreferenceConstants.METAMODELCONTRIBUTORS,
				"org.eclipse.xtend.typesystem.uml2.ui.Uml2MetamodelContributor,"
						+ "org.eclipse.xtend.shared.ui.core.metamodel.jdt.oaw.OawClassicMetamodelContributor");
	}

}
