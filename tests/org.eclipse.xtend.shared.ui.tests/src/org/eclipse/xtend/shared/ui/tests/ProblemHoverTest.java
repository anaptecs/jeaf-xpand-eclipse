/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.xtend.shared.ui.Messages;
import org.eclipse.xtend.shared.ui.core.builder.XtendXpandProblemHover;
import org.eclipse.xtend.shared.ui.editor.hover.ProblemHover;
import org.junit.jupiter.api.Test;

/**
 * Tests the hovers that show problem markers in the Xpand, Xtend and Check editors.
 */
class ProblemHoverTest {
	private static final String TEXT = "first line\nsecond line with problems\nthird line\n";

	private static final String MESSAGE_1 = "Type 'Foo' is unknown";

	private static final String MESSAGE_2 = "Extension 'bar' is unknown";

	@Test
	void hoversShowProblemMarkers() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("hover-test");
		if (!project.exists()) {
			project.create(null);
		}
		project.open(null);
		IFile file = project.getFile("Test.ext");
		if (!file.exists()) {
			file.create(new ByteArrayInputStream(TEXT.getBytes()), true, null);
		}
		file.deleteMarkers(IMarker.PROBLEM, false, 0);
		int problem1 = TEXT.indexOf("with");
		int problem2 = TEXT.indexOf("problems");
		IMarker marker1 = createMarker(file, MESSAGE_1, problem1, 4);
		IMarker marker2 = createMarker(file, MESSAGE_2, problem2, 8);

		UI.run(() -> {
			Shell shell = new Shell();
			SourceViewer viewer = new SourceViewer(shell, null, SWT.NONE);
			AnnotationModel model = new AnnotationModel();
			viewer.setDocument(new Document(TEXT), model);
			model.addAnnotation(new MarkerAnnotation(marker1), new Position(problem1, 4));
			model.addAnnotation(new MarkerAnnotation(marker2), new Position(problem2, 8));

			for (Object hover : List.of(new XtendXpandProblemHover(viewer), new ProblemHover(viewer))) {
				String name = hover.getClass().getSimpleName();
				// Text hover: only problems at the offset
				assertEquals(MESSAGE_1, textHover(hover, viewer, problem1 + 1), name);
				assertEquals(MESSAGE_2, textHover(hover, viewer, problem2 + 1), name);
				assertEquals("", textHover(hover, viewer, TEXT.indexOf("third")), name);

				// Annotation hover (vertical ruler): all problems of the line
				String lineInfo = ((IAnnotationHover) hover).getHoverInfo(viewer, 1);
				assertTrue(lineInfo.startsWith(expectedMultipleMarkersLabel(hover)), name + ": " + lineInfo);
				assertTrue(lineInfo.contains(MESSAGE_1) && lineInfo.contains(MESSAGE_2), name + ": " + lineInfo);
			}
			shell.dispose();
		});
	}

	/**
	 * Hover information as determined by Eclipse's text hover: {@link ITextHoverExtension2} if available.
	 */
	@SuppressWarnings("deprecation")
	private static String textHover(final Object hover, final SourceViewer viewer, final int offset) {
		Region region = new Region(offset, 0);
		Object info = hover instanceof ITextHoverExtension2 ? ((ITextHoverExtension2) hover).getHoverInfo2(viewer, region)
				: ((ITextHover) hover).getHoverInfo(viewer, region);
		return String.valueOf(info);
	}

	private static String expectedMultipleMarkersLabel(final Object hover) {
		return hover instanceof XtendXpandProblemHover ? Messages.XtendXpandProblemHover_MultipleMarkers
				: Messages.AbstractHover_MultipleMarkers;
	}

	private static IMarker createMarker(final IFile file, final String message, final int start, final int length)
			throws Exception {
		IMarker marker = file.createMarker(IMarker.PROBLEM);
		marker.setAttributes(Map.of(IMarker.MESSAGE, message, IMarker.SEVERITY, IMarker.SEVERITY_ERROR,
				IMarker.CHAR_START, start, IMarker.CHAR_END, start + length, IMarker.LINE_NUMBER, 2));
		return marker;
	}
}
