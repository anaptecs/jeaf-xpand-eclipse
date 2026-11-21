/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.core.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNatureUtil;

/**
 * Adds the Xpand nature to the selected projects.
 */
public class AddXtendXpandNatureHandler extends AbstractHandler {
	@Override
	public Object execute(final ExecutionEvent event) {
		for (IProject project : SelectedProjects.of(event)) {
			XtendXpandNatureUtil.addExtXptNature(project);
		}
		return null;
	}
}
