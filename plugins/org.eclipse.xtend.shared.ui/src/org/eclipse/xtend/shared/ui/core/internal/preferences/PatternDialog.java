/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal.preferences;

import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author thoms - Initial contribution and API
 */
public class PatternDialog extends Dialog {
	private Text patternField;
	private String patternText;

	public String getPatternText() {
		return patternText;
	}

	public void setPatternText(final String patternText) {
		this.patternText = patternText;
	}

	public PatternDialog(final Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		newShell.setMinimumSize(new Point(200, 20));
		super.configureShell(newShell);
		newShell.setText("Pattern");
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		comp.getLayout();

		Label lblNewLabel = new Label(comp, SWT.NONE);
		lblNewLabel
				.setText("Enter a path expression. Allowed wildcards are *, ?");
		patternField = new Text(comp, SWT.SINGLE | SWT.BORDER);

		final ControlDecoration txtDecorator = new ControlDecoration(
				patternField, SWT.TOP | SWT.RIGHT);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		Image img = fieldDecoration.getImage();
		txtDecorator.setImage(img);
		txtDecorator.setDescriptionText("Invalid regular expression");
		// hiding it initially
		txtDecorator.hide();

		patternField.addVerifyListener(new VerifyListener() {
			public void verifyText(final VerifyEvent event) {
				try {
					// TODO: Verify entered text

					if (txtDecorator.isVisible()) {
						txtDecorator.hide();
					}
				} catch (PatternSyntaxException e) {
					txtDecorator.setDescriptionText(e.getMessage());
					txtDecorator.show();
				}
			}
		});
		patternField.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {

			}
		});
		patternField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		if (patternText != null) {
			patternField.setText(patternText);
		}
		return comp;
	}

	@Override
	protected void okPressed() {
		patternText = patternField.getText();
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		patternField.setText("");
		super.cancelPressed();
	}

	public Text getPatternField() {
		return patternField;
	}

}