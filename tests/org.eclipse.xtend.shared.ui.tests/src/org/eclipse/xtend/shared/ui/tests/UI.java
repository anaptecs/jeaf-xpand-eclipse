/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Helpers to run test code in the UI thread and to find and use widgets.
 */
public final class UI {
	private UI() {
	}

	public interface Task<T> {
		T run() throws Exception;
	}

	public interface VoidTask {
		void run() throws Exception;
	}

	public static <T> T call(final Task<T> task) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();
		AtomicReference<Error> assertionError = new AtomicReference<>();
		Display.getDefault().syncExec(() -> {
			try {
				result.set(task.run());
			}
			catch (Exception e) {
				error.set(e);
			}
			catch (Error e) {
				assertionError.set(e);
			}
		});
		if (assertionError.get() != null) {
			throw assertionError.get();
		}
		if (error.get() != null) {
			throw error.get();
		}
		return result.get();
	}

	public static void run(final VoidTask task) throws Exception {
		call(() -> {
			task.run();
			return null;
		});
	}

	/**
	 * Processes pending UI events (only in the UI thread).
	 */
	public static void processEvents() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
			// process all pending events
		}
	}

	public static <T extends Control> List<T> find(final Control root, final Class<T> type) {
		List<T> result = new ArrayList<>();
		if (type.isInstance(root)) {
			result.add(type.cast(root));
		}
		if (root instanceof Composite) {
			for (Control child : ((Composite) root).getChildren()) {
				result.addAll(find(child, type));
			}
		}
		return result;
	}

	public static Button findButton(final Control root, final String text) {
		for (Button button : find(root, Button.class)) {
			if (text.equals(button.getText())) {
				return button;
			}
		}
		return null;
	}

	/**
	 * Selects / deselects a check box and notifies the listeners as a click would do.
	 */
	public static void click(final Button button, final boolean selected) {
		button.setSelection(selected);
		button.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Clicks a push button.
	 */
	public static void click(final Button button) {
		button.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Fills a (context) menu as if it was opened by the user and returns the labels of its items.
	 */
	public static List<String> show(final Menu menu) {
		menu.notifyListeners(SWT.Show, new Event());
		List<String> labels = new ArrayList<>();
		for (MenuItem item : menu.getItems()) {
			labels.add(label(item));
		}
		return labels;
	}

	/**
	 * Returns the item with the passed label (without mnemonics and accelerators) or null.
	 */
	public static MenuItem item(final Menu menu, final String label) {
		for (MenuItem item : menu.getItems()) {
			if (label.equals(label(item))) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Clicks a menu item.
	 */
	public static void select(final MenuItem item) {
		item.notifyListeners(SWT.Selection, new Event());
	}

	private static String label(final MenuItem item) {
		String text = item.getText().replace("&", "");
		int tab = text.indexOf('\t');
		return tab >= 0 ? text.substring(0, tab) : text;
	}
}
