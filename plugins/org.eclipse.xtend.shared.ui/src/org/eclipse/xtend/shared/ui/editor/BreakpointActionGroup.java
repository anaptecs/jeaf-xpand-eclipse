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

import java.lang.reflect.Method;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * Action group with 2 actions: "Toggle Breakpoints" and
 * "Enable/Disable Breakpoints".<br>
 * Despite of usual breakpoint actions these actions can be used not only for
 * vertical ruler context menu (incl. double click), but also for editor context
 * menu. That way "in line" breakpoints can be handled.
 * 
 * @author Dennis Hübner
 * @author Aykut Kilic (itemis) - Bug#465802
 */
public class BreakpointActionGroup extends ActionGroup {

	private EnableDisableBreakpointAction enableAction;

	private ToggleBreakpointAction toggleAction;

	private IVerticalRuler verticalRuler;

	private boolean rulerSelected;

	private StyledText textWidget;

	// -------------------------------------------------------------------------

	public BreakpointActionGroup(final TextEditor editor) {
		Assert.isNotNull(editor);

		// Note: We don't want to define a new "IOurOwnTextEditor" interface, so
		// we do it via Reflection
		Object obj = getterMethod("getSourceViewer", editor);
		if (obj == null) {
			return;
		}
		textWidget = ((ISourceViewer) obj).getTextWidget();

		obj = getterMethod("getVerticalRuler", editor);
		if (obj == null) {
			return;
		}
		verticalRuler = (IVerticalRuler) obj;

		enableAction = new EnableDisableBreakpointAction(editor, this);
		toggleAction = new ToggleBreakpointAction(editor, this);

		verticalRuler.getControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				toggleAction.run();
			}
		});
	}

	// -------------------------------------------------------------------------

	public boolean isRulerSelected() {
		return rulerSelected;
	}

	public int getLastSelectedLine() {
		return verticalRuler.getLineOfLastMouseButtonActivity();
	}

	public int getLastSelectedOffset() {
		return getOffsetAtLine(getLastSelectedLine());
	}

	public int getOffsetAtLine(final int line) {
		return textWidget.getOffsetAtLine(line);
	}

	/**
	 * @since 2.1
	 */
	public String getLine(final int line) {
		return textWidget.getLine(line);
	}

	/**
	 * @since 2.2
	 */
	public int getFirstCharPosOfLine(final int line) {
		int result = textWidget.getOffsetAtLine(line);
		String lineText = textWidget.getLine(line);

		for (char c : lineText.toCharArray()) {
			if (!Character.isWhitespace(c))
				break;
			result++;
		}

		return result;
	}

	// -------------------------------------------------------------------------

	@Override
	public void fillContextMenu(final IMenuManager manager) {
		toggleAction.updateText();
		manager.appendToGroup("Xpand", toggleAction);
		enableAction.updateText();
		manager.appendToGroup("Xpand", enableAction);
	}

	@Override
	public void dispose() {
		enableAction = null;
		toggleAction = null;
		super.dispose();
	}

	private Object getterMethod(final String name, final Object element) {
		try {
			Method m = findMethod(name, element.getClass());
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(element, new Object[] {});
			}
		} catch (Exception e) {
			System.out.println("BreakpointActionGroup.getterMethod() caused an error");
		}
		return null;
	}

	private Method findMethod(final String name, final Class<?> clazz) {
		if (!Object.class.equals(clazz)) {
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {
				if (method.getName().equals(name)) {
					return method;
				}
			}
			return findMethod(name, clazz.getSuperclass());
		}
		return null;
	}

}
