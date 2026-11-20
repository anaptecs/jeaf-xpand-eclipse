/*******************************************************************************
 * Copyright (c) 2005 - 2007 committers of openArchitectureWare and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: committers of openArchitectureWare - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.xpand.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.internal.xpand2.XpandUtil;
import org.eclipse.internal.xpand2.ast.AbstractDefinition;
import org.eclipse.internal.xpand2.ast.ExpandStatement;
import org.eclipse.internal.xpand2.ast.Template;
import org.eclipse.internal.xtend.expression.ast.DeclaredParameter;
import org.eclipse.internal.xtend.expression.parser.SyntaxConstants;
import org.eclipse.internal.xtend.type.baseimpl.PolymorphicResolver;
import org.eclipse.internal.xtend.type.baseimpl.types.CollectionTypeImpl;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorPart;
import org.eclipse.xpand.ui.core.IXpandResource;
import org.eclipse.xtend.expression.AnalysationIssue;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.search.SearchMatch;
import org.eclipse.xtend.shared.ui.core.search.XtendXpandSearchEngine;
import org.eclipse.xtend.shared.ui.editor.navigation.AbstractHyperlinkDetector;
import org.eclipse.xtend.shared.ui.editor.navigation.ContextComputer;
import org.eclipse.xtend.shared.ui.editor.navigation.GenericHyperlink;
import org.eclipse.xtend.typesystem.Type;

/**
 * @author Darius Jockel - Initial contribution and API
 */
public class XpandHyperlinkDetector extends AbstractHyperlinkDetector {

	public XpandHyperlinkDetector(final IEditorPart editor) {
		super(editor);
	}

	@Override
	protected List<GenericHyperlink> computeMatchesAndHyperlinks(
			final IRegion hyperlinkRegion, final String hyperlinkedWord,
			final IXtendXpandProject project) {

		try {
			List<GenericHyperlink> links = super.computeMatchesAndHyperlinks(
					hyperlinkRegion, hyperlinkedWord, project);
			if (!links.isEmpty()) {
				return links;
			}
			links = new ArrayList<GenericHyperlink>();
			IXpandResource xpandResource = (IXpandResource) Activator
					.getExtXptModelManager().findExtXptResource(getFile());
			ExpandStatement statement = XtendXpandSearchEngine.findDefinition(
					hyperlinkedWord, hyperlinkRegion,
					(Template) xpandResource.getExtXptResource());

			if (statement == null) {
				return Collections.<GenericHyperlink> emptyList();
			}
			List<AbstractDefinition> defmatches = XtendXpandSearchEngine
					.findDefinitionsByNameInResourceAndImports(project,
							statement.getDefinition().toString(), xpandResource);
			ContextComputer helper = computeContext(xpandResource,
					statement.getTarget());
			if (helper == null || helper.getContext() == null) {
				return useGenericHyplerlinkDetector(hyperlinkRegion,
						hyperlinkedWord, project);
			}
			ExecutionContext ctx = helper.getContext();
			Type expandTargetType = null;
			if (statement.getTarget() == null)
				expandTargetType = ctx.getTypeForName(statement
						.getContainingDefinition().getType().toString());
			else if (statement.getTarget().equals(
					ExecutionContext.IMPLICIT_VARIABLE))
				expandTargetType = ctx.getTypeForName(statement
						.getContainingDefinition().getType().toString());
			else
				expandTargetType = statement.getTarget().analyze(ctx,
						new HashSet<AnalysationIssue>());

			if (expandTargetType == null)
				expandTargetType = ctx.getTypeForName(statement
						.getContainingDefinition().getType().toString());
			if (expandTargetType instanceof CollectionTypeImpl)
				expandTargetType = ((CollectionTypeImpl) expandTargetType)
						.getInnerType();
			Type[] statementTypes = new Type[statement.getParameters().length];
			for (int i = 0; i < statementTypes.length; i++) {
				statementTypes[i] = statement.getParameters()[i].analyze(ctx,
						new HashSet<AnalysationIssue>());
				if (statementTypes[i] == null)
					statementTypes[i] = ctx.getObjectType();
			}
			for (AbstractDefinition definition : defmatches) {
				int compare = isDefinitionAssignableFromExpandStatement(
						definition, statementTypes, expandTargetType, ctx);
				if (compare != -1) {
					GenericHyperlink genericHyperlink = createGenericTemplateHyperlink(
							hyperlinkedWord, statement, definition);
					if (compare == 0)
						links.add(0, genericHyperlink);
					else
						links.add(genericHyperlink);
				}
			}
			return links;
		} catch (Exception e) {
			return useGenericHyplerlinkDetector(hyperlinkRegion,
					hyperlinkedWord, project);
		}
	}

	/**
	 * @since 2.0
	 */
	protected GenericHyperlink createGenericTemplateHyperlink(
			final String hyperlinkedWord, final ExpandStatement statement,
			final AbstractDefinition definition) {
		SearchMatch match = new SearchMatch(definition.getDefName().getStart(),
				definition.getDefName().getEnd()
				- definition.getDefName().getStart(),
				getXXResourceByName(
						definition.getFileName().replaceAll(
								SyntaxConstants.NS_DELIM, "/"),
						XpandUtil.TEMPLATE_EXTENSION).getUnderlyingStorage());
		GenericHyperlink genericHyperlink = new GenericHyperlink(
				getWorkbenchPage(), match, new Region(statement.getDefinition()
						.getStart(), statement.getDefinition().getEnd()
						- statement.getDefinition().getStart()),
				computeHyperlinkLabel(
						hyperlinkedWord,
						definition.getTargetType(),
						definition.getParamsAsList(),
						getXXResourceByName(definition.getFileName()
								.replaceAll(SyntaxConstants.NS_DELIM, "/"),
								XpandUtil.TEMPLATE_EXTENSION)));
		return genericHyperlink;
	}

	/**
	 * Determines for a definitions, iff the definition can be called from an
	 * expand statement or not. Therefore the target type of the expand
	 * statement and all types of the parameters of the expand statement are
	 * computed. If no type can be computered, the type is set to
	 * <code>ObjectType</code>.
	 * 
	 * @param expandStatement
	 *            The expand statement
	 * @param definition
	 *            The definitions to check
	 */
	private int isDefinitionAssignableFromExpandStatement(
			final AbstractDefinition definition, final Type[] statementTypes,
			final Type statementType, final ExecutionContext ctx) {

		Type defineType = ctx.getTypeForName(definition.getType().toString());
		if (defineType == null)
			defineType = ctx.getObjectType();
		if (!defineType.isAssignableFrom(statementType))
			return -1;

		List<Type> definitionTypes = new ArrayList<Type>();
		for (DeclaredParameter param : definition.getParams()) {
			Type paramType = ctx.getTypeForName(param.getType().toString());
			if (paramType != null)
				definitionTypes.add(paramType);
			else
				definitionTypes.add(ctx.getObjectType());
		}
		return PolymorphicResolver.typesComparator.compare(definitionTypes,
				Arrays.asList(statementTypes));
	}

}
