/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.internal.XtendLog;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Base class for Xpand project settings and workspace preferences. As preference page it edits the workspace settings
 * and offers a link to the project specific settings. As property page it edits the settings of a project, which can be
 * enabled / disabled by a check box, and offers a link to the workspace settings.
 *
 * @author thoms - Initial contribution and API
 * @since 2.1
 */
public abstract class AbstractPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
	/**
	 * Key of the data passed by {@link #applyData(Object)}: if set to {@link Boolean#TRUE}, no link to the workspace /
	 * project settings is shown (e.g. if the page was opened by such a link).
	 */
	public static final String DATA_NO_LINK = "PropertyAndPreferencePage.nolink";

	private IAdaptable element;

	private IProject project;

	private Map<String, Object> data;

	private Button useProjectSettings;

	private Link changeSettingsLink;

	private Control preferenceContent;

	private ControlEnableState preferenceContentEnableState;

	/**
	 * Creates the content of the page below the project specific settings check box.
	 */
	protected abstract Control createPreferenceContent(Composite parent);

	/**
	 * ID of the page as preference page (workspace settings).
	 */
	protected abstract String getPreferencePageID();

	/**
	 * ID of the page as property page (project settings).
	 */
	protected abstract String getPropertyPageID();

	/**
	 * Get the configuration keys managed by the page
	 */
	protected abstract String[] getKeys();

	@Override
	public void init(final IWorkbench workbench) {
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	@Override
	public void setElement(final IAdaptable element) {
		this.element = element;
		IResource resource = element != null ? element.getAdapter(IResource.class) : null;
		project = resource != null ? resource.getProject() : null;
	}

	/**
	 * @return <code>true</code> if the page edits the settings of a project (property page)
	 */
	protected boolean isProjectPreferencePage() {
		return project != null;
	}

	protected IProject getProject() {
		return project;
	}

	/**
	 * @return <code>true</code> if the page edits project settings and project specific settings are enabled
	 */
	protected boolean useProjectSettings() {
		return isProjectPreferencePage() && useProjectSettings != null && useProjectSettings.getSelection();
	}

	protected boolean supportsProjectSpecificOptions() {
		return getPropertyPageID() != null;
	}

	protected boolean offerLink() {
		return data == null || !Boolean.TRUE.equals(data.get(DATA_NO_LINK));
	}

	@Override
	protected Label createDescriptionLabel(final Composite parent) {
		if (isProjectPreferencePage()) {
			Composite header = new Composite(parent, SWT.NONE);
			header.setFont(parent.getFont());
			GridLayout layout = new GridLayout(2, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			header.setLayout(layout);
			header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			useProjectSettings = new Button(header, SWT.CHECK);
			useProjectSettings.setFont(parent.getFont());
			useProjectSettings.setText("Enable pr&oject specific settings");
			GridData checkBoxData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			useProjectSettings.setLayoutData(checkBoxData);
			useProjectSettings.addSelectionListener(SelectionListener
					.widgetSelectedAdapter(e -> enableProjectSpecificSettings(useProjectSettings.getSelection())));
			if (offerLink()) {
				changeSettingsLink = createLink(header, "Configure Workspace Settings...");
				changeSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			}
			else {
				checkBoxData.horizontalSpan = 2;
			}
			Label separator = new Label(header, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			separator.setFont(header.getFont());
		}
		else if (supportsProjectSpecificOptions() && offerLink()) {
			changeSettingsLink = createLink(parent, "Configure Project Specific Settings...");
			changeSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		}
		return super.createDescriptionLabel(parent);
	}

	private Link createLink(final Composite parent, final String text) {
		Link link = new Link(parent, SWT.NONE);
		link.setFont(parent.getFont());
		link.setText("<A>" + text + "</A>");
		link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doLinkActivated()));
		return link;
	}

	private void doLinkActivated() {
		Map<String, Object> linkData = getData() != null ? new HashMap<>(getData()) : new HashMap<>();
		linkData.put(DATA_NO_LINK, Boolean.TRUE);
		if (isProjectPreferencePage()) {
			openWorkspacePreferences(linkData);
		}
		else {
			IProject selectedProject = selectProject();
			if (selectedProject != null) {
				openProjectProperties(selectedProject, linkData);
			}
		}
	}

	/**
	 * Lets the user select the Java project whose settings should be configured.
	 */
	private IProject selectProject() {
		List<IProject> projects = new ArrayList<>();
		try {
			for (IJavaProject javaProject : JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects()) {
				projects.add(javaProject.getProject());
			}
		}
		catch (JavaModelException e) {
			XtendLog.logError(e);
		}
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new WorkbenchLabelProvider());
		dialog.setTitle("Project Specific Configuration");
		dialog.setMessage("&Select the project to configure:");
		dialog.setElements(projects.toArray());
		dialog.setMultipleSelection(false);
		return dialog.open() == Window.OK ? (IProject) dialog.getFirstResult() : null;
	}

	protected final void openWorkspacePreferences(final Object data) {
		String id = getPreferencePageID();
		PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, data).open();
	}

	protected final void openProjectProperties(final IProject project, final Object data) {
		String id = getPropertyPageID();
		if (id != null) {
			PreferencesUtil.createPropertyDialogOn(getShell(), project, id, new String[] { id }, data).open();
		}
	}

	@Override
	protected Control createContents(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());

		preferenceContent = createPreferenceContent(composite);
		preferenceContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (isProjectPreferencePage()) {
			enableProjectSpecificSettings(hasProjectSpecificOptions(getProject()));
		}
		Dialog.applyDialogFont(composite);
		return composite;
	}

	/**
	 * Enables / disables the project specific settings incl. the content of the page.
	 */
	protected void enableProjectSpecificSettings(final boolean useProjectSpecificSettings) {
		useProjectSettings.setSelection(useProjectSpecificSettings);
		enablePreferenceContent(useProjectSpecificSettings);
		updateLinkVisibility();
	}

	protected void enablePreferenceContent(final boolean enable) {
		if (enable) {
			if (preferenceContentEnableState != null) {
				preferenceContentEnableState.restore();
				preferenceContentEnableState = null;
			}
		}
		else if (preferenceContentEnableState == null) {
			preferenceContentEnableState = ControlEnableState.disable(preferenceContent);
		}
	}

	private void updateLinkVisibility() {
		if (changeSettingsLink != null && !changeSettingsLink.isDisposed() && isProjectPreferencePage()) {
			changeSettingsLink.setEnabled(!useProjectSettings());
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		IPreferenceStore store;
		if (isProjectPreferencePage()) {
			store = new ScopedPreferenceStore(new ProjectScope(getProject()), Activator.getId());
		}
		else {
			store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.getId());
		}
		return store;
	}

	/**
	 * @return <code>true</code> if the project has settings of this page
	 */
	protected boolean hasProjectSpecificOptions(final IProject project) {
		if (!isProjectPreferencePage()) {
			return false;
		}
		ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(project), Activator.getId());
		for (IEclipsePreferences prefs : store.getPreferenceNodes(false)) {
			try {
				List<String> keys = Arrays.asList(prefs.keys());
				for (String key : getKeys()) {
					if (keys.contains(key)) {
						return true;
					}
				}
			}
			catch (BackingStoreException e) {
				return false;
			}
		}
		return false;
	}

	@Override
	protected void performDefaults() {
		if (useProjectSettings()) {
			enableProjectSpecificSettings(false);
		}
		super.performDefaults();
	}

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
		}
		catch (final IOException e1) {
			XtendLog.logError(e1);
		}
		return super.performOk();
	}

	@Override
	public void applyData(final Object data) {
		if (data instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) data;
			this.data = map;
		}
		if (changeSettingsLink != null && !changeSettingsLink.isDisposed() && !offerLink()) {
			changeSettingsLink.dispose();
		}
	}

	/**
	 * Data passed by {@link #applyData(Object)}.
	 */
	protected Map<String, Object> getData() {
		return data;
	}
}
