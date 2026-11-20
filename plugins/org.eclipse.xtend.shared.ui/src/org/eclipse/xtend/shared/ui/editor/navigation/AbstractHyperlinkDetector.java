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
package org.eclipse.xtend.shared.ui.editor.navigation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.internal.xpand2.ast.AbstractDefinition;
import org.eclipse.internal.xpand2.ast.ExtensionImportDeclaration;
import org.eclipse.internal.xpand2.ast.Template;
import org.eclipse.internal.xtend.expression.ast.DeclaredParameter;
import org.eclipse.internal.xtend.expression.ast.Expression;
import org.eclipse.internal.xtend.expression.ast.Literal;
import org.eclipse.internal.xtend.expression.ast.OperationCall;
import org.eclipse.internal.xtend.expression.ast.SyntaxElement;
import org.eclipse.internal.xtend.expression.parser.SyntaxConstants;
import org.eclipse.internal.xtend.type.baseimpl.PolymorphicResolver;
import org.eclipse.internal.xtend.type.baseimpl.types.CollectionTypeImpl;
import org.eclipse.internal.xtend.xtend.XtendFile;
import org.eclipse.internal.xtend.xtend.ast.Around;
import org.eclipse.internal.xtend.xtend.ast.Check;
import org.eclipse.internal.xtend.xtend.ast.Extension;
import org.eclipse.internal.xtend.xtend.ast.ExtensionFile;
import org.eclipse.internal.xtend.xtend.ast.ExtensionImportStatement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.xpand2.XpandExecutionContext;
import org.eclipse.xtend.expression.AnalysationIssue;
import org.eclipse.xtend.expression.ExecutionContext;
import org.eclipse.xtend.expression.ExecutionContextImpl;
import org.eclipse.xtend.expression.Variable;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.shared.ui.core.search.SearchMatch;
import org.eclipse.xtend.shared.ui.core.search.XtendXpandSearchEngine;
import org.eclipse.xtend.typesystem.Type;

/**
 * GenericHyperlinkDetector is used to detect hyperlinkable words inside Xtend
 * editors.
 * 
 * @author Darius Jockel
 * @author Peter Friese
 */
public abstract class AbstractHyperlinkDetector implements IHyperlinkDetector {

	protected final IEditorPart editor;

	public AbstractHyperlinkDetector(final IEditorPart editor) {
		this.editor = editor;
	}

	protected List<GenericHyperlink> computeMatchesAndHyperlinks(
			final IRegion hyperlinkRegion, final String hyperlinkedWord,
			final IXtendXpandProject project) {
		IXtendXpandResource resource = Activator.getExtXptModelManager()
				.findExtXptResource(getFile());

		List<GenericHyperlink> links = createHyperlinkToImportedExtensions(
				resource, hyperlinkRegion, hyperlinkedWord);
		if (!links.isEmpty())
			return links;

		OperationCall expression = null;
		if (resource.getExtXptResource() instanceof XtendFile) {
			expression = XtendXpandSearchEngine.findExpressionInExtensionFile(
					hyperlinkRegion,
					(ExtensionFile) resource.getExtXptResource(),
					hyperlinkedWord);
		} else {
			expression = XtendXpandSearchEngine.findExpressionInTemplate(
					hyperlinkedWord, (Template) resource.getExtXptResource());
		}
		if (expression == null)
			return useGenericHyplerlinkDetector(hyperlinkRegion,
					hyperlinkedWord, project);

		List<Type> evaluatedParams = new ArrayList<Type>();
		ContextComputer helper = computeContext(resource, expression);
		if (helper == null || helper.getContext() == null) {
			return useGenericHyplerlinkDetector(hyperlinkRegion,
					hyperlinkedWord, project);
		}
		ExecutionContext filledContext = helper.getContext();
		if (expression.getParams().length > 0) {
			for (Expression ex : expression.getParams()) {
				if (ex instanceof Literal) {
					Type type = filledContext.getTypeForName(ex.getClass()
							.getSimpleName().replaceFirst("Literal", ""));
					evaluatedParams.add(type);
				} else {
					evaluatedParams.add(ex.analyze(filledContext,
							new HashSet<AnalysationIssue>()));
				}
			}
		}
		Type targetType = null;
		if (helper.expressionResult != null
				&& helper.expressionResult instanceof Type
				&& expression.getTarget() != null) {
			targetType = (Type) helper.expressionResult;
		} else {
			final Variable var = filledContext
					.getVariable(ExecutionContext.IMPLICIT_VARIABLE);
			if (var != null) {
				targetType = (Type) var.getValue();
			}
		}
		List<Extension> candidates = new ArrayList<Extension>();
		for (Extension candidate : filledContext.getAllExtensions()) {
			if (hyperlinkedWord.equals(candidate.getName()))
				candidates.add(candidate);
		}
		List<Extension> extmatches = new ArrayList<Extension>(candidates.size());
		for (Extension ext : candidates) {
			int compare = PolymorphicResolver.typesComparator.compare(
					ext.getParameterTypes(), evaluatedParams);
			if (compare == 0) {
				extmatches.add(0, ext);
			} else {
				if (compare == 1) {
					extmatches.add(ext);
				}
			}
		}
		evaluatedParams.add(0, targetType);
		for (Extension ext : candidates) {
			int compare = PolymorphicResolver.typesComparator.compare(
					ext.getParameterTypes(), evaluatedParams);
			if (compare == 0) {
				extmatches.add(0, ext);
			} else {
				if (compare == 1) {
					extmatches.add(ext);
				}
			}
		}
		if (targetType instanceof CollectionTypeImpl) {
			evaluatedParams.remove(0);
			evaluatedParams.add(((CollectionTypeImpl) targetType)
					.getInnerType());
			for (Extension ext : candidates) {
				int compare = PolymorphicResolver.typesComparator.compare(
						ext.getParameterTypes(), evaluatedParams);
				if (compare == 0) {
					extmatches.add(0, ext);
				} else {
					if (compare == 1) {
						extmatches.add(ext);
					}
				}
			}
		}
		return createHyperlink(hyperlinkRegion, hyperlinkedWord, links,
				extmatches);
	}

	/**
	 * @since 2.0
	 */
	protected List<GenericHyperlink> useGenericHyplerlinkDetector(
			final IRegion hyperlinkRegion, final String hyperlinkedWord,
			final IXtendXpandProject project) {
		GenericHyperlinkDetector genericHyperlinkDetector = new GenericHyperlinkDetector(
				editor);
		return genericHyperlinkDetector.computeMatchesAndHyperlinks(
				hyperlinkRegion, hyperlinkedWord, project);
	}

	/**
	 * @since 2.0
	 */
	protected SyntaxElement findOwningElement(final IXtendXpandResource file,
			final Expression expression) {
		int start = expression.getStart();
		if (file.getExtXptResource() == null) {
			return null;
		}
		if (file.getExtXptResource() instanceof ExtensionFile) {
			ExtensionFile extensionFile = (ExtensionFile) file
					.getExtXptResource();
			for (Extension ext : extensionFile.getExtensions()) {
				if (ext.getStart() <= start && ext.getEnd() >= start)
					return (SyntaxElement) ext;
			}
			for (Check ext : extensionFile.getChecks()) {
				if (ext.getStart() <= start && ext.getEnd() >= start)
					return ext;
			}
			for (Around ext : extensionFile.getArounds()) {
				if (ext.getStart() <= start && ext.getEnd() >= start)
					return ext;
			}
		}
		if (file.getExtXptResource() instanceof Template) {
			Template template = (Template) file.getExtXptResource();
			for (AbstractDefinition ext : template.getAllDefinitions()) {
				if (ext.getStart() <= start && ext.getEnd() >= start)
					return ext;
			}
		}
		return null;

	}

	/**
	 * @since 2.0
	 */
	protected ContextComputer computeContext(
			final IXtendXpandResource resource, final Expression expression) {
		final SyntaxElement element = findOwningElement(resource, expression);
		final IResource file = (IResource) editor.getEditorInput().getAdapter(
				IResource.class);
		final IProject p = file.getProject();
		ExecutionContext c = Activator.getExecutionContext(JavaCore.create(p));
		ExecutionContextImpl context = (ExecutionContextImpl) c
				.cloneWithResource(resource);
		ContextComputer callback = new ContextComputer(expression);
		context.setVetoableCallBack(callback);
		if (element instanceof Extension) {
			((Extension) element).analyze(context,
					new HashSet<AnalysationIssue>());
		}
		if (element instanceof AbstractDefinition) {
			((AbstractDefinition) element).analyze(
					(XpandExecutionContext) context,
					new HashSet<AnalysationIssue>());
		}
		if (element instanceof Check) {
			((Check) element).analyze(context, new HashSet<AnalysationIssue>());
		}
		if (element instanceof Around) {
			((Around) element)
			.analyze(context, new HashSet<AnalysationIssue>());
		}
		return callback;
	}

	/**
	 * {@inheritDoc}
	 */
	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer,
			final IRegion region, final boolean canShowMultipleHyperlinks) {
		if (region == null || textViewer == null) {
			return null;
		}

		// get hyperlinked region
		IRegion hyperlinkRegion = getHyperlinkRegion(textViewer,
				region.getOffset());
		if (hyperlinkRegion == null) {
			return null;
		}

		// get the word that is hyperlinked
		String hyperlinkedWord = textViewer
				.getDocument()
				.get()
				.substring(
						hyperlinkRegion.getOffset(),
						hyperlinkRegion.getOffset()
						+ hyperlinkRegion.getLength());

		IXtendXpandProject project = getXtendXpandProject();
		if (project != null) {
			List<GenericHyperlink> links = computeMatchesAndHyperlinks(
					hyperlinkRegion, hyperlinkedWord, project);
			// must return null if no links found
			if (!links.isEmpty()) {
				if (canShowMultipleHyperlinks) {
					return links.toArray(new IHyperlink[links.size()]);
				} else {
					return new IHyperlink[] { links.get(0) };
				}
			}
		}
		return null;
	}

	protected IWorkbenchPage getWorkbenchPage() {
		return editor.getSite().getWorkbenchWindow().getActivePage();
	}

	protected IXtendXpandProject getXtendXpandProject() {
		IFile file = getFile();
		if (file == null) {
			return null;
		}
		return Activator.getExtXptModelManager().findProject(file);
	}

	protected IFile getFile() {
		return (IFile) editor.getEditorInput().getAdapter(IFile.class);
	}

	/**
	 * Given a text viewer and an offset (i.e. the current cursor position),
	 * analyse the region around this location in order to find the word under
	 * the cursor.
	 * 
	 * @param textViewer
	 *            The underlying text viewer.
	 * @param offset
	 *            The cursor location.
	 * @return The document region defining the hyperlinked word.
	 */
	protected IRegion getHyperlinkRegion(final ITextViewer textViewer,
			final int offset) {
		String currDoc = textViewer.getDocument().get();

		// special handling if cursor is located at end of document
		if (offset == currDoc.length()) {
			return null;
		}

		// find word start
		int start = offset;
		while (start > -1 && WordDetector.isWordPart(currDoc.charAt(start))) {
			start--;
		}

		// find word end
		start++;
		int end = offset;
		while (end < currDoc.length()
				&& WordDetector.isWordPart(currDoc.charAt(end))) {
			end++;
		}

		if (start < 0) {
			start = 0;
		}

		if (end < start) {
			end = start;
		}

		return new Region(start, end - start);
	}

	/**
	 * Provides a label for a extension or definition with given parameter list.
	 * 
	 * @param name
	 *            The name of the element
	 * @param params
	 *            A list of parameters declared by the element
	 *            {@link org.eclipse.internal.xtend.expression.ast.DeclaredParameter
	 *            DeclaredParameter}
	 * 
	 * @return A label: name(params,...) - FileName
	 */
	protected String computeHyperlinkLabel(final String name,
			final String typeName, final List<DeclaredParameter> params,
			final IXtendXpandResource xxresource) {
		StringBuffer label = new StringBuffer(name);
		if (!params.isEmpty()) {
			label.append("(");
			for (Iterator<DeclaredParameter> iter = params.iterator(); iter
					.hasNext();) {
				DeclaredParameter param = iter.next();
				label.append(param.getType().toString());
				label.append(" " + param.getName().toString());
				if (iter.hasNext())
					label.append(", ");

			}
			label.append(")");
		}
		if (typeName != null)
			label.append(" : " + typeName);
		if (!xxresource.getUnderlyingStorage().equals(getFile()))
			label.append(" - "
					+ xxresource.getUnderlyingStorage().getName().toString()
					.replace("." + xxresource.getFileExtension(), ""));
		return label.toString();
	}

	protected IXtendXpandResource _xxresource = null;

	/**
	 * A cached getter for a
	 * {@link org.eclipse.xtend.shared.ui.core.IXtendXpandResource
	 * IXtendXpandResource}.
	 * 
	 * @param fqName
	 *            The full qualified name of the resource
	 * @param extension
	 *            The kind of resource
	 * 
	 * @return The value of _xxresource if _xxresource is filled and the names
	 *         are equal. Otherwise the ModelManager tries to find that file.
	 */
	protected IXtendXpandResource getXXResourceByName(final String fqName,
			final String extension) {
		if (_xxresource != null
				&& _xxresource.getFileExtension().equals(extension)
				&& _xxresource.getFullyQualifiedName().equals(fqName))
			return _xxresource;
		else {
			_xxresource = getXtendXpandProject().findExtXptResource(fqName,
					extension);
			return _xxresource;
		}
	}

	private List<GenericHyperlink> createHyperlink(
			final IRegion hyperlinkRegion, final String hyperlinkedWord,
			final List<GenericHyperlink> links, final List<Extension> extmatches) {
		for (Extension ext : extmatches) {
			{
				SearchMatch match = new SearchMatch(ext.getStart(),
						ext.getEnd() - ext.getStart() + 1 /*
						 * sorry for the
						 * "+1"-hack
						 */,
						 getXXResourceByName(
								ext.getFileName().replaceAll(
										SyntaxConstants.NS_DELIM, "/"),
										 XtendFile.FILE_EXTENSION)
										 .getUnderlyingStorage());
				GenericHyperlink genericHyperlink = new GenericHyperlink(
						getWorkbenchPage(), match, hyperlinkRegion,
						computeHyperlinkLabel(
								hyperlinkedWord,
								(ext.getReturnTypeIdentifier() == null) ? null
										: ext.getReturnTypeIdentifier()
										.toString(),
										ext.getFormalParameters(),
										getXXResourceByName(
										ext.getFileName().replaceAll(
												SyntaxConstants.NS_DELIM, "/"),
														XtendFile.FILE_EXTENSION)));

				links.add(genericHyperlink);
			}
		}
		return links;
	}

	protected List<GenericHyperlink> createHyperlinkToImportedExtensions(
			final IXtendXpandResource resource, final IRegion region,
			final String hyperlinkedName) {
		List<GenericHyperlink> links = new ArrayList<GenericHyperlink>();
		IXtendXpandResource xxresource = null;
		if (resource.getExtXptResource() instanceof ExtensionFile) {
			ExtensionFile extensionFile = (ExtensionFile) resource
					.getExtXptResource();
			for (ExtensionImportStatement importStatement : extensionFile
					.getExtImports()) {
				if (importStatement.getImportedId().toString()
						.contains(hyperlinkedName)
						&& importStatement.getStart() <= region.getOffset()
						&& importStatement.getEnd() >= region.getOffset()
						+ region.getLength()) {
					xxresource = getXtendXpandProject().findExtXptResource(
							importStatement.getImportedId().toString(),
							XtendFile.FILE_EXTENSION);
					if (xxresource != null) {
						GenericHyperlink genericHyperlink = new GenericHyperlink(
								getWorkbenchPage(), new SearchMatch(0, 0,
										xxresource.getUnderlyingStorage()),
										new Region(importStatement.getImportedId()
												.getStart(), importStatement
												.getImportedId().getEnd()
												- importStatement.getImportedId()
												.getStart() + 1),
												importStatement.getImportedId().toString());
						links.add(genericHyperlink);
					}
				}
			}
		}
		if (resource.getExtXptResource() instanceof Template) {
			Template template = (Template) resource.getExtXptResource();
			for (ExtensionImportDeclaration importStatement : template
					.getExtensions()) {
				if (importStatement.getImportString().toString()
						.contains(hyperlinkedName)
						&& importStatement.getStart() <= region.getOffset()
						&& importStatement.getEnd() >= region.getOffset()
						+ region.getLength()) {
					xxresource = getXtendXpandProject().findExtXptResource(
							importStatement.getImportString().toString(),
							XtendFile.FILE_EXTENSION);
					if (xxresource != null) {
						GenericHyperlink genericHyperlink = new GenericHyperlink(
								getWorkbenchPage(), new SearchMatch(0, 0,
										xxresource.getUnderlyingStorage()),
										new Region(importStatement.getImportString()
												.getStart(), importStatement
												.getImportString().getEnd()
												- importStatement.getImportString()
												.getStart() + 1),
												importStatement.getImportString().toString());
						links.add(genericHyperlink);
					}
				}
			}
		}
		return links;
	}

}
