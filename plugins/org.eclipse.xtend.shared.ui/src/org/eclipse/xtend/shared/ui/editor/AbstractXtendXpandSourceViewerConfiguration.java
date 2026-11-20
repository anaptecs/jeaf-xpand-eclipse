/*******************************************************************************
 * Copyright (c) 2005, 2007 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.editor;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.editor.hover.ProblemHover;
import org.eclipse.xtend.shared.ui.editor.navigation.GenericHyperlinkDetector;

public abstract class AbstractXtendXpandSourceViewerConfiguration extends
TextSourceViewerConfiguration {
	private static final String DIALOG_SETTINGS = Activator.PLUGIN_ID
			+ "DIALOG_SETTINGS"; //$NON-NLS-1$

	private final IEditorPart editor;

	public IEditorPart getEditor() {
		return editor;
	}

	public AbstractXtendXpandSourceViewerConfiguration(
			final IEditorPart editor, final IPreferenceStore preferenceStore) {
		super(preferenceStore);
		this.editor = editor;
	}

	public AbstractXtendXpandSourceViewerConfiguration(final IEditorPart editor) {
		this.editor = editor;
	}

	/**
	 * @see org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IAnnotationHover getAnnotationHover(final ISourceViewer sourceViewer) {
		return new ProblemHover(sourceViewer);
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(
			final ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] { new GenericHyperlinkDetector(
				getEditor()) };
	}

	@Override
	public IContentAssistant getContentAssistant(
			final ISourceViewer sourceViewer) {
		ContentAssistant contentAssistant;
		contentAssistant = new ContentAssistant();

		createContentAssistProcessor(contentAssistant);

		configureContentAssistant(contentAssistant, sourceViewer);
		return contentAssistant;
	}

	protected abstract void createContentAssistProcessor(
			ContentAssistant contentAssistant);

	protected void configureContentAssistant(final ContentAssistant assistant,
			final ISourceViewer sourceViewer) {
		// colors
		ISharedTextColors sharedTextColors = EditorsUI.getSharedTextColors();

		Color white = sharedTextColors.getColor(new RGB(255, 255, 255));
		Color black = sharedTextColors.getColor(new RGB(0, 0, 0));

		assistant.setContextInformationPopupBackground(white);
		assistant.setContextInformationPopupForeground(black);

		assistant.setContextSelectorBackground(white);
		assistant.setContextSelectorForeground(black);

		assistant.setProposalSelectorBackground(white);
		assistant.setProposalSelectorForeground(black);

		// activation
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(200);

		// popup
		assistant
		.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		assistant
		.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant
		.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		// enable storing of dialog size
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings fSettings = settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings = new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
		}

		assistant.setRestoreCompletionProposalSize(fSettings);
	}

	@Override
	public IReconciler getReconciler(final ISourceViewer sourceViewer) {
		return null; // we don't want spell checking
	}

	public void refresh() {
		/* No-op */
	}
}
