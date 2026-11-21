/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandNature;
import org.junit.jupiter.api.Test;

/**
 * Tests that Xpand, Xtend and Check resources are parsed again after their content changed.
 */
class ResourceRefreshTest {
	@Test
	void resourcesAreParsedAgainAfterChange() throws Exception {
		TestWorkspace.disableAutoBuild();
		IProject project = TestWorkspace.createJavaProject("resource-refresh",
				new String[] { JavaCore.NATURE_ID, XtendXpandNature.NATURE_ID }, List.of()).getProject();
		IXtendXpandProject xpandProject = Activator.getExtXptModelManager().findProject(project);
		assertNotNull(xpandProject);

		check(xpandProject, project, "src/templates/Test.xpt", "«IMPORT first»\n", "«IMPORT second»\n");
		check(xpandProject, project, "src/extensions/Test.ext", "import first;\n", "import second;\n");
		check(xpandProject, project, "src/checks/Test.chk", "import first;\n", "import second;\n");
	}

	private static void check(final IXtendXpandProject xpandProject, final IProject project, final String path,
			final String before, final String after) throws Exception {
		IFile file = TestWorkspace.createFile(project, path, before.getBytes(StandardCharsets.UTF_8));
		file.setCharset("UTF-8", null);
		IXtendXpandResource resource = xpandProject.findXtendXpandResource(file);
		assertNotNull(resource, path);
		List<String> imports = Arrays.asList(resource.getImportedNamespaces());
		assertTrue(imports.contains("first"), path + ": " + imports);

		file.setContents(new ByteArrayInputStream(after.getBytes(StandardCharsets.UTF_8)), true, false, null);
		resource.refresh();
		imports = Arrays.asList(resource.getImportedNamespaces());
		assertTrue(imports.contains("second") && !imports.contains("first"), path + ": " + imports);
	}
}
