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
package org.eclipse.xpand.ui.debug;

import static org.eclipse.internal.xpand2.debug.XpandElementAdapter.TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.mwe.ui.workflow.util.PluginConfigurationElementUtil;
import org.eclipse.internal.xpand2.ast.AbstractDefinition;
import org.eclipse.internal.xpand2.ast.ErrorStatement;
import org.eclipse.internal.xpand2.ast.ExpandStatement;
import org.eclipse.internal.xpand2.ast.ExpressionStatement;
import org.eclipse.internal.xpand2.ast.FileStatement;
import org.eclipse.internal.xpand2.ast.ForEachStatement;
import org.eclipse.internal.xpand2.ast.IfStatement;
import org.eclipse.internal.xpand2.ast.LetStatement;
import org.eclipse.internal.xpand2.ast.ProtectStatement;
import org.eclipse.internal.xpand2.ast.Statement;
import org.eclipse.internal.xpand2.ast.Template;
import org.eclipse.internal.xpand2.ast.TextStatement;
import org.eclipse.internal.xpand2.debug.ExpandSpecial;
import org.eclipse.internal.xpand2.debug.ForEachSpecial;
import org.eclipse.internal.xpand2.debug.IfSpecial;
import org.eclipse.internal.xpand2.debug.XpandModelPresentation;
import org.eclipse.internal.xpand2.model.XpandAdvice;
import org.eclipse.internal.xpand2.model.XpandDefinition;
import org.eclipse.internal.xtend.expression.ast.Expression;
import org.eclipse.internal.xtend.expression.ast.ISyntaxElement;
import org.eclipse.internal.xtend.expression.ast.SyntaxElement;
import org.eclipse.internal.xtend.expression.debug.EvaluatedElementWrapper;
import org.eclipse.internal.xtend.expression.debug.NoResourceSpecial;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xpand.ui.XpandEditorPlugin;
import org.eclipse.xpand.ui.editor.XpandEditor;
import org.eclipse.xpand.ui.internal.XpandLog;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.ui.debug.ExpressionPluginAdapter;

/**
 * The IPluginAdapter implementation for Xpand statements.
 *
 * @author Clemens Kadura (zAJKa)
 * @author Karsten Thoms (itemis) - maintenance
 * @author Aykut Kilic (itemis) - Bug#465802, 484406, 480679
 */
public class XpandPluginAdapter extends ExpressionPluginAdapter {

	public XpandPluginAdapter() {
		super();
		specials.add(new ForEachSpecial());
		specials.add(new ExpandSpecial());
		specials.add(new IfSpecial());
		specials.add(new NoResourceSpecial());
		pres = new XpandModelPresentation(specials);
	}

	// -------------------------------------------------------------------------

	@Override
	protected String getRequiredExtension() {
		return "xpt";
	}

	@Override
	public boolean canHandleType(final String type) {
		return TYPE.equals(type);
	}

	@Override
	public String getEditorId() {
		return PluginConfigurationElementUtil
				.getConfigAttribute("org.eclipse.ui.editors/" + "editor[class="
						+ XpandEditor.class.getName() + "]/id");
	}

	// -------------------------------------------------------------------------

	@Override
	protected List<ISyntaxElement> collectFirstLevelElements(
			final IXtendXpandResource res) {
		if (res == null)
			return Collections.emptyList();
		List<ISyntaxElement> result = new ArrayList<ISyntaxElement>();
		XpandDefinition[] defs = ((Template) res.getExtXptResource())
				.getDefinitions();
		for (XpandDefinition def : defs)
			result.add(def);
		XpandAdvice[] advs = ((Template) res.getExtXptResource()).getAdvices();
		for (XpandAdvice adv : advs)
			result.add(adv);
		return result;
	}

	@Override
	protected List<ISyntaxElement> getBody(final ISyntaxElement parent) {
		List<ISyntaxElement> result = new ArrayList<ISyntaxElement>();

		// Definition or Advice
		if (parent instanceof AbstractDefinition) {
			result.addAll(((AbstractDefinition) parent).getBodyAsList());
		}

		// TextStatement
		else if (parent instanceof TextStatement) {
			// don't handle text
		}

		// FileStatement
		else if (parent instanceof FileStatement) {
			FileStatement stmt = (FileStatement) parent;
			result.add(new EvaluatedElementWrapper(parent));
			result.add(stmt.getTargetFileName());
			result.addAll(stmt.getBodyAsList());
		}

		// ForEachStatement
		else if (parent instanceof ForEachStatement) {
			ForEachStatement stmt = (ForEachStatement) parent;
			result.add(new EvaluatedElementWrapper(parent));
			result.add(stmt.getTarget());
			result.add(stmt.getSeparator());
			result.addAll(stmt.getBodyAsList());
		}
		// IfStatement
		else if (parent instanceof IfStatement) {
			IfStatement stmt = (IfStatement) parent;
			result.add(new EvaluatedElementWrapper(parent));
			result.add(stmt.getCondition());
			result.addAll(stmt.getBodyAsList());
			result.add(stmt.getElseIf());
		}

		// LetStatement
		else if (parent instanceof LetStatement) {
			LetStatement stmt = (LetStatement) parent;
			result.add(new EvaluatedElementWrapper(parent));
			result.add(stmt.getVarValue());
			result.addAll(stmt.getBodyAsList());
		}

		// ProtectStatement
		else if (parent instanceof ProtectStatement) {
			ProtectStatement stmt = (ProtectStatement) parent;
			result.add(new EvaluatedElementWrapper(parent));
			result.add(stmt.getCommentStart());
			result.add(stmt.getCommentEnd());
			result.add(stmt.getId());
			result.addAll(stmt.getBodyAsList());
		}

		// ErrorStatement
		else if (parent instanceof ErrorStatement) {
			result.add(new EvaluatedElementWrapper(parent));
			result.add(((ErrorStatement) parent).getMessage());
		}

		// ExpandStatement
		else if (parent instanceof ExpandStatement) {
			ExpandStatement stmt = (ExpandStatement) parent;
			result.add(new EvaluatedElementWrapper(parent));
			result.addAll(stmt.getParametersAsList());
			Expression target = ((ExpandStatement) parent).getTarget();
			if (target != null)
				result.add(target);
			Expression sep = ((ExpandStatement) parent).getSeparator();
			if (sep != null)
				result.add(sep);
		}

		// ExpressionStmt
		else if (parent instanceof ExpressionStatement) {
			result.add(new EvaluatedElementWrapper(parent));
			result.add(((ExpressionStatement) parent).getExpression());
		}

		else if (parent instanceof Statement)
			XpandLog.logInfo("Implementation error: Wanted to set Breakpoint for unknown statement: "
					+ parent.getClass().getSimpleName());
		else
			result.addAll(super.getBody(parent));
		return result;
	}

	@Override
	protected ISyntaxElement findElementForPosition(final IResource resource,
			final int position, final int line) {
		ISyntaxElement rootElem = getContainingRootElement(resource, position);
		if (rootElem == null)
			return null;

		// this is not body but is root element.
		ISyntaxElement element = null;
		List<ISyntaxElement> bodyElements = getBody(rootElem);
		for (ISyntaxElement item : bodyElements) {
			if (contains(item, position)) {
				element = item;
				break;
			}
		}
		if (element == null)
			return rootElem;

		return getContainingElement((SyntaxElement) element, position);
	}

	@Override
	protected ISyntaxElement getContainingElement(final SyntaxElement element,
			final int position) {
		if (!contains(element, position))
			return null;

		if (element instanceof ExpandStatement) {
			return getContainingElementOfExpandStatement(
					(ExpandStatement) element, position);
		} else if (element instanceof ExpressionStatement) {
			return getContainingElementOfExpressionStatement(
					(ExpressionStatement) element, position);
		} else if (element instanceof FileStatement)
			return getContainingElementOfFileStatement((FileStatement) element,
					position);
		else if (element instanceof ForEachStatement)
			return getContainingElementOfForEachStatement(
					(ForEachStatement) element, position);
		else if (element instanceof IfStatement)
			return getContainingElementOfIfStatement((IfStatement) element,
					position);
		else if (element instanceof LetStatement)
			return getContainingElementOfLetStatement((LetStatement) element,
					position);
		else if (element instanceof ProtectStatement)
			return getContainingElementOfProtectStatement(
					(ProtectStatement) element, position);

		return super.getContainingElement(element, position);
	}

	private ISyntaxElement getContainingElementOfExpressionStatement(
			final ExpressionStatement exprStatement, final int position) {
		return getContainingElement(exprStatement.getExpression(), position);
	}

	private boolean contains(final ISyntaxElement elem, final int position) {
		return elem.getStart() <= position && elem.getEnd() > position;
	}

	private ISyntaxElement getContainingElementOfExpandStatement(
			final ExpandStatement expandStatement, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		// children.addAll(expandStatement.getParametersAsList());
		children.add(expandStatement.getTarget());
		return getContainingChild(expandStatement, children, position);
	}

	private ISyntaxElement getContainingElementOfFileStatement(
			final FileStatement fileStatement, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(fileStatement.getTargetFileName());
		children.addAll(fileStatement.getBodyAsList());
		return getContainingChild(fileStatement, children, position);
	}

	private ISyntaxElement getContainingElementOfForEachStatement(
			final ForEachStatement forStatement, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(forStatement.getTarget());
		children.add(forStatement.getSeparator());
		children.addAll(forStatement.getBodyAsList());
		return getContainingChild(forStatement, children, position);
	}

	private ISyntaxElement getContainingElementOfIfStatement(
			final IfStatement ifStatement, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(ifStatement.getCondition());
		children.addAll(ifStatement.getBodyAsList());
		children.add(ifStatement.getElseIf());
		return getContainingChild(ifStatement, children, position);
	}

	private ISyntaxElement getContainingElementOfLetStatement(
			final LetStatement letStatement, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(letStatement.getVarValue());
		children.addAll(letStatement.getBodyAsList());
		return getContainingChild(letStatement, children, position);
	}

	private ISyntaxElement getContainingElementOfProtectStatement(
			final ProtectStatement protStatement, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.addAll(protStatement.getBodyAsList());
		return getContainingChild(protStatement, children, position);
	}

	@Override
	protected boolean isValidReturnElement(final ISyntaxElement element) {
		if (element instanceof TextStatement)
			return false;
		if (element instanceof Statement)
			return true;
		return super.isValidReturnElement(element);
	}

	// -------------------------------------------------------------------------

	private Image icon = null;

	@Override
	public Image getIcon() {
		if (icon != null)
			return icon;
		IPath path = new Path("/icons/templatefile.gif");
		InputStream is = null;
		try {
			is = FileLocator.openStream(XpandEditorPlugin.getDefault()
					.getBundle(), path, false);
		} catch (IOException e) {
		}
		if (is != null) {
			final ImageData iData = new ImageData(is);
			icon = new Image(Display.getCurrent(), iData);
			return icon;
		}
		return null;
	}

}
