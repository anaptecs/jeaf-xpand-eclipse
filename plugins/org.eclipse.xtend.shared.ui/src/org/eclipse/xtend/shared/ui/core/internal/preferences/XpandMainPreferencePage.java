/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

/**
 * @author thoms - Initial contribution and API
 */
public class XpandMainPreferencePage extends PreferencePage implements
		IWorkbenchPropertyPage, IWorkbenchPreferencePage {

	public IAdaptable getElement() {
		return null;
	}

	public void setElement(final IAdaptable element) {
	}

	@Override
	protected Control createContents(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		return composite;
	}

	public void init(final IWorkbench workbench) {

	}

}
