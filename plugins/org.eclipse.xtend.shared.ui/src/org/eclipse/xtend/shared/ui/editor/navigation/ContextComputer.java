/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.shared.ui.editor.navigation;

import org.eclipse.internal.xtend.expression.ast.Expression;
import org.eclipse.internal.xtend.expression.ast.FeatureCall;
import org.eclipse.internal.xtend.expression.ast.SyntaxElement;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtend.expression.VetoableCallback;

/**
 * Used by the {@link AbstractHyperlinkDetector} to compute the ExecutionContext
 * for a given Expression. Iff the expression is a {@link FeatureCall} also the
 * evaluated expression result of the target is stored.
 * 
 * @author jockel - Initial contribution and API
 * @since 2.0
 */
public class ContextComputer implements VetoableCallback {

	private final Expression se;
	ExecutionContext context = null;
	Object expressionResult = null;

	public ContextComputer(final Expression se) {
		this.se = se;
	}

	public ExecutionContext getContext() {
		return context;
	}

	public boolean pre(final SyntaxElement ele, final ExecutionContext ctx) {
		return true;
	}

	public void post(final SyntaxElement ele, final ExecutionContext ctx,
			final Object expressionResult) {
		if (se.equals(ele)) {
			context = ctx.cloneWithoutMonitor();
		}
		if ((se instanceof FeatureCall)
				&& ele.equals(((FeatureCall) se).getTarget())) {
			this.expressionResult = expressionResult;
		}
	}

}
