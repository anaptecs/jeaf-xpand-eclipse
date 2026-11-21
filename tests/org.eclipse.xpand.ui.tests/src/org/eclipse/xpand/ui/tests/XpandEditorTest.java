/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xpand.ui.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.xpand.ui.XpandEditorPlugin;
import org.eclipse.xpand.ui.editor.XpandEditor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests of the Xpand editor.
 */
@TestInstance(Lifecycle.PER_CLASS)
class XpandEditorTest {
	private static final String EDITOR_ID = "org.eclipse.xpand.ui.XpandEditor";

	/**
	 * Commands with key bindings in the Xpand editor (see plugin.xml): refactoring quick menu (Alt+Shift+T) and rename
	 * (Alt+Shift+R).
	 */
	private static final List<String> COMMANDS = List.of("org.eclipse.xpand.ui.refactoring.quickMenu",
			"org.eclipse.xpand.ui.refactoring.rename.define");

	private IFile template;

	@BeforeAll
	void setUp() throws Exception {
		TestSupport.disableAutoBuild();
		IProject project = TestSupport.createXpandProject("editor-test");
		project.getFolder("src/templates").create(true, true, null);
		template = project.getFile("src/templates/Test.xpt");
		template.create(new ByteArrayInputStream(new byte[0]), true, null);
	}

	@Test
	void refactoringCommandsAreHandledWhileEditorIsOpen() throws Exception {
		TestSupport.ui(() -> {
			IWorkbenchPage page = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage();
			ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			IEditorPart editor = IDE.openEditor(page, template, EDITOR_ID, true);
			assertTrue(editor instanceof XpandEditor);
			page.activate(editor);
			TestSupport.processEvents();
			for (String id : COMMANDS) {
				Command command = commandService.getCommand(id);
				assertTrue(command.isDefined(), id);
				assertTrue(command.isHandled(), id + " has to be handled while the editor is active");
			}

			page.closeEditor(editor, false);
			TestSupport.processEvents();
			for (String id : COMMANDS) {
				assertFalse(commandService.getCommand(id).isHandled(), id + " must not be handled after the editor was closed");
			}
		});
	}

	@Test
	void templateStore() throws Exception {
		TestSupport.ui(() -> {
			// No context types and templates are contributed, registry and store have to work nevertheless.
			XpandEditorPlugin plugin = XpandEditorPlugin.getDefault();
			assertNotNull(plugin.getContextTypeRegistry());
			assertNotNull(plugin.getContextTypeRegistry().contextTypes());
			assertNotNull(plugin.getTemplateStore());
			assertNotNull(plugin.getTemplateStore().getTemplates());
		});
	}
}
