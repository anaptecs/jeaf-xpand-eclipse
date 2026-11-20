/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal.preferences;

import java.io.IOException;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.internal.XtendLog;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.Sets;

/**
 * Base class for Xpand project settings and workspace preferences.
 *
 * @author thoms - Initial contribution and API
 * @since 2.1
 */
public abstract class AbstractPreferencePage extends PropertyAndPreferencePage {
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		IPreferenceStore store;
		if (isProjectPreferencePage()) {
			// get java project
			final IProject project = getProject();
			store = new ScopedPreferenceStore(new ProjectScope(project),
					Activator.getId());
		} else {
			store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
					Activator.getId());
		}
		return store;
	}

	@Override
	protected boolean hasProjectSpecificOptions(final IProject project) {
		if (!isProjectPreferencePage())
			return false;
		ScopedPreferenceStore store = new ScopedPreferenceStore(
				new ProjectScope(project), Activator.getId());

		for (IEclipsePreferences prefs : store.getPreferenceNodes(false)) {
			try {
				HashSet<String> keys = Sets.newHashSet(prefs.keys());
				for (String key : getKeys()) {
					if (keys.contains(key))
						return true;
				}
			} catch (BackingStoreException e) {
				return false;
			}
		}
		return false;
	}

	@Override
	protected void enableProjectSpecificSettings(
			final boolean useProjectSpecificSettings) {
		super.enableProjectSpecificSettings(useProjectSpecificSettings);
	}

	/**
	 * Get the configuration keys managed by the page
	 */
	protected abstract String[] getKeys();

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		// project specific settings are disabled => set to default
		if (isProjectPreferencePage() && !useProjectSettings()) {
			for (String key : getKeys()) {
				store.setToDefault(key);
			}
		}
		try {
			if (store instanceof IPersistentPreferenceStore) {
				((IPersistentPreferenceStore) store).save();
			}
		} catch (final IOException e1) {
			XtendLog.logError(e1);
		}

		return super.performOk();
	}
}
