/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardRegistry;
import org.junit.jupiter.api.Test;

/**
 * Tests the wizards in File &gt; New: wizards for Xpand, Xtend and Check files, but no project wizard.
 */
class NewWizardsTest {
	@Test
	void fileWizardsAreAvailable() {
		IWizardRegistry registry = PlatformUI.getWorkbench().getNewWizardRegistry();
		for (String id : new String[] { "org.eclipse.xpand.ui.wizard.NewTemplateFileWizard",
				"org.eclipse.xtend.xtend.wizard.NewXtendFileWizard", "org.eclipse.xtend.check.ui.wizard.NewCheckFileWizard" }) {
			assertNotNull(registry.findWizard(id), id);
		}
	}

	@Test
	void projectWizardIsRemoved() {
		assertNull(PlatformUI.getWorkbench().getNewWizardRegistry().findWizard("org.eclipse.xpand.ui.wizard.NewXpandProjectWizard"));
	}
}
