/*******************************************************************************
 * Copyright (c) 2007 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.emf.mwe.ui.debug.model.MWEBreakpoint;
import org.eclipse.emf.mwe.ui.debug.processing.PluginAdapter;
import org.eclipse.emf.mwe.ui.debug.processing.PluginExtensionManager;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.xtend.shared.ui.Messages;

/**
 * Toggle breakpoint action that can be used both at vertical ruler and editor context menu.
 * 
 * @author Aykut Kilic (itemis) - Bug#465802
 */
public class ToggleBreakpointAction extends Action {

	private final TextEditor editor;

	private final BreakpointActionGroup group;

	public ToggleBreakpointAction(final TextEditor editor, final BreakpointActionGroup group) {
		this.editor = editor;
		this.group = group;
		setText(Messages.ToggleBreakpointAction_ToggleAction);
		setToolTipText(Messages.ToggleBreakpointAction_Description);
	}

	public void updateText() {
		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

		PluginAdapter adapter = PluginExtensionManager.getDefault().getAdapterByResourceExtension(resource.getFileExtension());
		if (adapter == null) {
			return;
		}

		int line = group.getLastSelectedLine() + 1;
		int start = group.getFirstCharPosOfLine(line);
		int end = start;

		if (group.isRulerSelected()) {
			setEnabled(true);
		} else {
			try {
				setEnabled(adapter.isToggleBpEnabled(resource, start, end, line));
			} catch (NullPointerException npe) {
				/* ignore NPE caused by erroneous line numbers */
			}
		}
	}

	@Override
	public void run() {
		try {
			toggleBreakpoint();
		} catch (CoreException e) {
		}
	}

	protected void toggleBreakpoint() throws CoreException {
		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

		// THIS IS THE CORRECT FILE. (THERE ARE SOME DUPLICATES :/ )

		int line = group.getLastSelectedLine() + 1;
		int start = group.getFirstCharPosOfLine(line - 1);
		int end = start;

		PluginAdapter adapter = PluginExtensionManager.getDefault().getAdapterByResourceExtension(resource.getFileExtension());
		if (adapter == null) {
			return;
		}

		// check if a BP already exists on that line and remove it
		IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] breakpoints = breakpointManager.getBreakpoints(MWEBreakpoint.DEBUG_MODEL_ID);
		IBreakpoint bp = adapter.checkBreakpoints(breakpoints, resource, start, end, line);
		if (bp != null) {
			bp.delete();
			return;
		}
		// else: register it
		bp = adapter.createBreakpoint(resource, start, end, line);
		if (bp == null) {
			return;
		}
		breakpointManager.addBreakpoint(bp);
	}
}
