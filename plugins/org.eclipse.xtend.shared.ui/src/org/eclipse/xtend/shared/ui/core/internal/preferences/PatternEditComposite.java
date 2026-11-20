/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal.preferences;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

/**
 * @author thoms - Initial contribution and API
 */
public class PatternEditComposite extends Composite {
	private final List list;

	/**
	 * Create the composite.
	 *
	 * @param parent
	 * @param style
	 */
	public PatternEditComposite(final Composite parent, final int style) {
		super(parent, SWT.NONE);
		FormLayout formLayout = new FormLayout();
		formLayout.marginBottom = 5;
		formLayout.marginRight = 5;
		formLayout.marginTop = 5;
		formLayout.marginLeft = 5;
		setLayout(formLayout);

		list = new List(this, SWT.BORDER | SWT.V_SCROLL);
		list.setItems(new String[] {});
		FormData fd_list = new FormData();
		fd_list.top = new FormAttachment(0);
		fd_list.bottom = new FormAttachment(100);
		fd_list.height = 200;
		fd_list.left = new FormAttachment(0);
		list.setLayoutData(fd_list);

		Button btnAdd = new Button(this, SWT.NONE);
		fd_list.right = new FormAttachment(btnAdd);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				PatternDialog dialog = new PatternDialog(parent.getShell());
				if (dialog.open() == Window.OK
						&& !dialog.getPatternText().trim().isEmpty()) {
					list.add(dialog.getPatternText());
				}
			}
		});
		FormData fd_btnAdd = new FormData();
		fd_btnAdd.top = new FormAttachment(0);
		fd_btnAdd.width = 80;
		fd_btnAdd.right = new FormAttachment(100);
		btnAdd.setLayoutData(fd_btnAdd);
		btnAdd.setText("Add");

		Button btnEdit = new Button(this, SWT.NONE);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (list.getSelectionIndex() >= 0) {
					PatternDialog dialog = new PatternDialog(parent.getShell());
					dialog.setPatternText(list.getSelection()[0]);
					if (dialog.open() == Window.OK
							&& !dialog.getPatternText().trim().isEmpty()) {
						list.setItem(list.getSelectionIndex(),
								dialog.getPatternText());
					}
				}
			}
		});
		FormData fd_btnEdit = new FormData();
		fd_btnEdit.width = 80;
		fd_btnEdit.top = new FormAttachment(btnAdd, 5);
		fd_btnEdit.left = new FormAttachment(btnAdd, 0, SWT.LEFT);
		btnEdit.setLayoutData(fd_btnEdit);
		btnEdit.setText("Edit");

		Button btnRemove = new Button(this, SWT.NONE);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				int selectionIndex = list.getSelectionIndex();
				if (selectionIndex >= 0) {
					list.remove(selectionIndex);
				}
				// keep next item selected
				if (selectionIndex < list.getItemCount()) {
					list.select(selectionIndex);
				}
			}
		});
		FormData fd_btnRemove = new FormData();
		fd_btnRemove.width = 80;
		fd_btnRemove.top = new FormAttachment(btnEdit, 5);
		fd_btnRemove.left = new FormAttachment(btnEdit, 0, SWT.LEFT);
		btnRemove.setLayoutData(fd_btnRemove);
		btnRemove.setText("Remove");

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public List getList() {
		return list;
	}
}
