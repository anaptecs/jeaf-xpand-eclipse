/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests the context menu entries of the Xpand plug-ins as a user would use them.
 */
@TestInstance(Lifecycle.PER_CLASS)
class ContextMenuTest {
	private static final String PACKAGE_EXPLORER = "org.eclipse.jdt.ui.PackageExplorer";

	private static final String CONFIGURE = "Configure";

	private static final String ADD_NATURE = "Add Xpand Nature";

	private static final String REMOVE_NATURE = "Remove Xpand Nature";

	private static final String ECORE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ "    xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"test\" nsURI=\"http://test\" nsPrefix=\"test\">\n"
			+ "  <eClassifiers xsi:type=\"ecore:EClass\" name=\"Customer\">\n"
			+ "    <eOperations name=\"fullName\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>\n"
			+ "  </eClassifiers>\n" + "</ecore:EPackage>\n";

	@BeforeAll
	void setUp() throws Exception {
		TestWorkspace.disableAutoBuild();
	}

	@Test
	void addAndRemoveNatureInProjectExplorer() throws Exception {
		IProject project = TestWorkspace
				.createJavaProject("menu-project-explorer", new String[] { JavaCore.NATURE_ID }, List.of()).getProject();
		addAndRemoveNature(IPageLayout.ID_PROJECT_EXPLORER, project, project);
	}

	@Test
	void addAndRemoveNatureInPackageExplorer() throws Exception {
		IProject project = TestWorkspace
				.createJavaProject("menu-package-explorer", new String[] { JavaCore.NATURE_ID }, List.of()).getProject();
		addAndRemoveNature(PACKAGE_EXPLORER, project, JavaCore.create(project));
	}

	private void addAndRemoveNature(final String viewId, final IProject project, final Object element)
			throws Exception {
		assertFalse(project.hasNature(XtendXpandNature.NATURE_ID));
		UI.run(() -> {
			Menu configure = configureMenu(viewId, element);
			List<String> labels = UI.show(configure);
			assertTrue(labels.contains(ADD_NATURE), labels.toString());
			assertFalse(labels.contains(REMOVE_NATURE), labels.toString());
			UI.select(UI.item(configure, ADD_NATURE));
		});
		TestWorkspace.waitForJobs();
		assertTrue(project.hasNature(XtendXpandNature.NATURE_ID), "Nature added in " + viewId);

		UI.run(() -> {
			Menu configure = configureMenu(viewId, element);
			List<String> labels = UI.show(configure);
			assertTrue(labels.contains(REMOVE_NATURE), labels.toString());
			assertFalse(labels.contains(ADD_NATURE), labels.toString());
			UI.select(UI.item(configure, REMOVE_NATURE));
		});
		TestWorkspace.waitForJobs();
		assertFalse(project.hasNature(XtendXpandNature.NATURE_ID), "Nature removed in " + viewId);
	}

	@Test
	void ecoreFileOffersOperationDelegation() throws Exception {
		IProject project = TestWorkspace
				.createJavaProject("menu-ecore", new String[] { JavaCore.NATURE_ID }, List.of()).getProject();
		IFile ecore = TestWorkspace.createFile(project, "model/test.ecore", ECORE.getBytes(StandardCharsets.UTF_8));
		IFile other = TestWorkspace.createFile(project, "model/test.txt", "text".getBytes(StandardCharsets.UTF_8));

		UI.run(() -> {
			Menu menu = contextMenu(IPageLayout.ID_PROJECT_EXPLORER, other);
			assertFalse(UI.show(menu).contains("Xtend"), "No Xtend menu for other files");

			menu = contextMenu(IPageLayout.ID_PROJECT_EXPLORER, ecore);
			List<String> labels = UI.show(menu);
			assertTrue(labels.contains("Xtend"), labels.toString());
			Menu xtend = UI.item(menu, "Xtend").getMenu();
			List<String> xtendLabels = UI.show(xtend);
			MenuItem delegation = UI.item(xtend, "Add EOperation delegation");
			assertNotNull(delegation, xtendLabels.toString());
			UI.select(delegation);
		});
		TestWorkspace.waitForJobs();
		ecore.refreshLocal(0, null);
		String content = TestWorkspace.contents(ecore);
		assertTrue(content.contains("CustomerLogic.fullName(this);"), content);
	}

	/**
	 * Opens the view, selects the element and returns the "Configure" sub menu of the context menu.
	 */
	private static Menu configureMenu(final String viewId, final Object element) throws Exception {
		Menu menu = contextMenu(viewId, element);
		List<String> labels = UI.show(menu);
		MenuItem configure = UI.item(menu, CONFIGURE);
		assertNotNull(configure, labels.toString());
		return configure.getMenu();
	}

	private static Menu contextMenu(final String viewId, final Object element) throws Exception {
		IWorkbenchPage page = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage();
		IViewPart view = page.showView(viewId);
		page.activate(view);
		((ISetSelectionTarget) view).selectReveal(new StructuredSelection(element));
		UI.processEvents();
		Control control = view instanceof CommonNavigator ? ((CommonNavigator) view).getCommonViewer().getControl()
				: ((IPackagesViewPart) view).getTreeViewer().getControl();
		Menu menu = control.getMenu();
		assertNotNull(menu);
		return menu;
	}
}
