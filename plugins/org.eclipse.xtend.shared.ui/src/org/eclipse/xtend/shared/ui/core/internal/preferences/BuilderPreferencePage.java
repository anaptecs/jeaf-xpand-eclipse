/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.xtend.shared.ui.core.preferences.PreferenceConstants;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * @author thoms - Initial contribution and API
 */
public class BuilderPreferencePage extends AbstractPreferencePage {
	private Combo incrementalAnalysisCombo;
	private PatternEditComposite exclusionPathsComposite;

	/**
	 * Create the preference page.
	 */
	public BuilderPreferencePage() {
		setImageDescriptor(ResourceManager.getPluginImageDescriptor(
				"org.eclipse.xtend.shared.ui", "icons/build_exec.gif"));
		setTitle("Builder");
	}

	@Override
	protected Control createPreferenceContent(final Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(1, false));

		Group grpSyntaxAnalysis = new Group(container, SWT.NONE);
		grpSyntaxAnalysis.setLayout(new GridLayout(1, false));
		GridData gd_grpSyntaxAnalysis = new GridData(SWT.LEFT, SWT.TOP, false,
				false, 1, 1);
		gd_grpSyntaxAnalysis.heightHint = 286;
		gd_grpSyntaxAnalysis.minimumWidth = 300;
		grpSyntaxAnalysis.setLayoutData(gd_grpSyntaxAnalysis);
		grpSyntaxAnalysis.setText("Syntax Analysis");

		Label lblIncrementalAnalysisStrategy = new Label(grpSyntaxAnalysis,
				SWT.NONE);
		lblIncrementalAnalysisStrategy.setText("Incremental Analysis Strategy");

		incrementalAnalysisCombo = new Combo(grpSyntaxAnalysis, SWT.NONE);
		incrementalAnalysisCombo.setItems(new String[] {
				"On save, analyze current and dependent projects",
				"On save, analyze whole project", "On save, analyze file only",
				"On save, analyze file and reverse referenced only" });
		incrementalAnalysisCombo.select(0);

		Label lblExcludeFromAnalysis = new Label(grpSyntaxAnalysis, SWT.NONE);
		lblExcludeFromAnalysis
				.setToolTipText("Enter path expressions here for resources that should be excluded from syntax analysis. Wildcard expressions are allowed, e.g. \"*/templates/*.ext\"");
		lblExcludeFromAnalysis.setText("Exclusion Patterns");

		exclusionPathsComposite = new PatternEditComposite(grpSyntaxAnalysis,
				SWT.NONE);
		GridData gd_composite_2 = new GridData(SWT.LEFT, SWT.TOP, true, true,
				1, 1);
		gd_composite_2.widthHint = 359;
		exclusionPathsComposite.setLayoutData(gd_composite_2);

		applyData(getData());
		return container;
	}

	@Override
	public boolean performOk() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		preferenceStore.setValue(
				PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY,
				incrementalAnalysisCombo.getSelectionIndex());
		// builder exclusion paths
		Joiner joiner = Joiner.on(",").skipNulls();
		String exclusionPaths = joiner.join(exclusionPathsComposite.getList()
				.getItems());
		preferenceStore.setValue(PreferenceConstants.BUILDER_EXCLUSIONS,
				exclusionPaths);

		return super.performOk();
	}

	@Override
	protected Map<String, Object> getData() {
		Map<String, Object> data = new HashMap<String, Object>();
		IPreferenceStore preferenceStore = getPreferenceStore();
		data.put(
				PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY,
				preferenceStore
						.getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY));
		data.put(PreferenceConstants.BUILDER_EXCLUSIONS, preferenceStore
				.getString(PreferenceConstants.BUILDER_EXCLUSIONS));
		return data;
	}

	@Override
	public void applyData(final Object data) {
		IPreferenceStore preferenceStore = getPreferenceStore();
		incrementalAnalysisCombo.select(preferenceStore
				.getInt(PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY));
		Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();
		Iterable<String> exclusionPaths = splitter.split(preferenceStore
				.getString(PreferenceConstants.BUILDER_EXCLUSIONS));
		exclusionPathsComposite.getList().setItems(
				Iterables.toArray(exclusionPaths, String.class));
	}

	@Override
	protected String getPreferencePageID() {
		return "org.eclipse.xtend.shared.ui.properties.BuilderPreferencePage";
	}

	@Override
	protected String getPropertyPageID() {
		return "org.eclipse.xtend.shared.ui.properties.BuilderPreferencePage";
	}

	@Override
	protected String[] getKeys() {
		return new String[] {
				PreferenceConstants.INCREMENTAL_ANALYZER_STRATEGY,
				PreferenceConstants.BUILDER_EXCLUSIONS };
	}

}
