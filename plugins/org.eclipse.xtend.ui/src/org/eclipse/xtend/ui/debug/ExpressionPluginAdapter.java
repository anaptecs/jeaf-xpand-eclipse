/*******************************************************************************
 * Copyright (c) 2005 - 2007 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/
package org.eclipse.xtend.ui.debug;

import static org.eclipse.internal.xtend.expression.debug.ExpressionElementAdapter.TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.emf.mwe.ui.debug.model.MWEBreakpoint;
import org.eclipse.emf.mwe.ui.debug.processing.PluginAdapter;
import org.eclipse.emf.mwe.ui.workflow.util.PluginConfigurationElementUtil;
import org.eclipse.internal.xpand2.ast.Statement;
import org.eclipse.internal.xpand2.ast.TextStatement;
import org.eclipse.internal.xtend.expression.ast.BooleanOperation;
import org.eclipse.internal.xtend.expression.ast.Case;
import org.eclipse.internal.xtend.expression.ast.Cast;
import org.eclipse.internal.xtend.expression.ast.ChainExpression;
import org.eclipse.internal.xtend.expression.ast.CollectionExpression;
import org.eclipse.internal.xtend.expression.ast.Expression;
import org.eclipse.internal.xtend.expression.ast.FeatureCall;
import org.eclipse.internal.xtend.expression.ast.ISyntaxElement;
import org.eclipse.internal.xtend.expression.ast.IfExpression;
import org.eclipse.internal.xtend.expression.ast.LetExpression;
import org.eclipse.internal.xtend.expression.ast.ListLiteral;
import org.eclipse.internal.xtend.expression.ast.OperationCall;
import org.eclipse.internal.xtend.expression.ast.SwitchExpression;
import org.eclipse.internal.xtend.expression.ast.SyntaxElement;
import org.eclipse.internal.xtend.expression.debug.BaseSpecialTreatment;
import org.eclipse.internal.xtend.expression.debug.EvaluatedElementWrapper;
import org.eclipse.internal.xtend.expression.debug.ExpressionModelPresentation;
import org.eclipse.internal.xtend.xtend.ast.Around;
import org.eclipse.internal.xtend.xtend.ast.CreateExtensionStatement;
import org.eclipse.internal.xtend.xtend.ast.ExpressionExtensionStatement;
import org.eclipse.internal.xtend.xtend.ast.ExtensionFile;
import org.eclipse.internal.xtend.xtend.ast.JavaExtensionStatement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.core.IXtendXpandProject;
import org.eclipse.xtend.shared.ui.core.IXtendXpandResource;
import org.eclipse.xtend.ui.XtendEditorPlugin;
import org.eclipse.xtend.ui.editor.XtendEditor;

/**
 * The IPluginAdapter implementation for Expressions.
 *
 * @author Clemens Kadura (zAJKa)
 * @author Karsten Thoms - maintenance
 * @author Aykut Kilic (itemis) - Bug#465802,480646
 */
public class ExpressionPluginAdapter implements PluginAdapter {

	protected ExpressionModelPresentation pres;

	protected Set<BaseSpecialTreatment> specials = new HashSet<BaseSpecialTreatment>();

	// -------------------------------------------------------------------------

	public ExpressionPluginAdapter() {
		pres = new ExpressionModelPresentation(specials);
	}

	// -------------------------------------------------------------------------

	protected String getRequiredExtension() {
		return "ext"; // TODO: CK: high: handle chk as well
	}

	public boolean canHandleResourceExtension(String ext) {
		return getRequiredExtension().equals(ext);
	}

	public boolean canHandleType(String type) {
		return TYPE.equals(type);
	}

	public String getEditorId() {
		// TODO: CK: next: integrate Check editor
		return PluginConfigurationElementUtil.getConfigAttribute("org.eclipse.ui.editors/" + "editor[class=" + XtendEditor.class.getName() + "]/id");
	}

	public boolean isToggleBpEnabled(IResource resource, int start, int end, int line) {
		ISyntaxElement element = findElementForPosition(resource, start, line);
		if (element == null)
			return false;

		return pres.getStart(element) <= start && pres.getStartingEndPosition(element) >= end;
	}

	public MWEBreakpoint createBreakpoint(IResource resource, int start, int end, int line) throws CoreException {
		ISyntaxElement element = findElementForPosition(resource, start, line);
		if (element == null || (!(element instanceof FeatureCall)
		        && !(element instanceof Statement)) || element instanceof TextStatement
		        ||  element.getEnd() == 0 )
			// TODO: CK: Message that no BP could be created
			return null;

		return new MWEBreakpoint(resource, element.getNameString(null), element.getLine(), element.getStart(), element.getEnd());
	}

	public IBreakpoint checkBreakpoints(IBreakpoint[] bps, IResource resource, int start, int end, int line) throws CoreException {
		ISyntaxElement element = findElementForPosition(resource, start, line);

		if(element == null) return null;
		
		for (IBreakpoint bp1 : bps) {
			MWEBreakpoint bp = (MWEBreakpoint) bp1;
			if (bp.getResource().equals(resource.getFullPath().toString())
					&& bp.getLine() == element.getLine())
				return bp;
		}
		return null;
	}

	/**
	 * @since 2.2
	 */
	protected ISyntaxElement findElementForPosition(final IResource resource, final int position, final int line) {
		ISyntaxElement rootElem = getContainingRootElement(resource, position);
		if (rootElem == null)
			return null;

		ISyntaxElement body = getBodyOfRootElement(rootElem);

		if (body instanceof ChainExpression) {
			return getContainingElementOfChainExpression(
					(ChainExpression) body, position);
		} else if (body instanceof LetExpression) {
			return getContainingElementOfLetExpression((LetExpression) body,
					position);
		} else if (body instanceof IfExpression) {
			return getContainingElementOfIfExpression((IfExpression) body, position);
		} else
			return getContainingElement((SyntaxElement)body,position);
	}

	private boolean containsPosition(final ISyntaxElement elem, final int position) {
		return elem.getStart() <= position && elem.getEnd() > position;
	}
	
	/**
	 * @since 2.2
	 */
	protected int shiftPositionIfInside(final int currentPosition,
			final SyntaxElement lowerBound, final SyntaxElement upperBound) {
		return currentPosition >= lowerBound.getStart()
				&& currentPosition <= upperBound.getStart() ? upperBound
				.getStart() : currentPosition;
	}

	/**
	 * @since 2.2
	 */
	protected ISyntaxElement getContainingElement(final SyntaxElement se, final int position) {
		if (!containsPosition(se, position)) return null;
		
		if ( se instanceof ChainExpression )
			return getContainingElementOfChainExpression((ChainExpression) se, position);
		else if ( se instanceof LetExpression ) {
			return getContainingElementOfLetExpression((LetExpression) se, position);
		} else if ( se instanceof SwitchExpression ) {
			return getContainingElementOfSwitchExpression((SwitchExpression) se, position);
		} else if ( se instanceof ListLiteral ) {
			return getContainingElementOfListLiteral((ListLiteral) se, position);
		} else if ( se instanceof IfExpression ) {
			return getContainingElementOfIfExpression((IfExpression) se, position );
		} else if ( se instanceof CollectionExpression ) {
			return getContainingElementOfCollectionExpression((CollectionExpression)se, position);
		} else if ( se instanceof BooleanOperation ) {
			return getContainingElementOfBooleanOperation( (BooleanOperation)se, position );
		} else if ( se instanceof FeatureCall ) {
			return getContainingElementOfFeatureCall( (FeatureCall)se, position );
		}

		return null;
	}
	
	/**
	 * @since 2.2
	 */
	protected ISyntaxElement getContainingChild(final SyntaxElement parent, final Collection<SyntaxElement> children, int position) {
		if(!containsPosition(parent, position)) return null;
		
		ISyntaxElement result = null;
		int newPosition = position;
		SyntaxElement lastElem = parent;
		for( SyntaxElement child : children) {
			if( child == null ) continue;
			fixBoundaries(child);
			if( child.getEnd()==0 ) {
				System.out.println(child.getClass().getSimpleName() + "  " + child.toString());
			}
			
			newPosition = shiftPositionIfInside(newPosition, lastElem, child);
			
			result = getContainingElement(child, newPosition);
			if(result != null) return result;
			lastElem = child;
		}
		
		return parent;
	}
	
	private ISyntaxElement getContainingElementOfLetExpression(final LetExpression le, int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(le.getVarExpression());
		children.add(le.getTargetExpression());
		return getContainingChild(le, children, position);
	}

	private ISyntaxElement getContainingElementOfIfExpression(final IfExpression ie, int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(ie.getCondition());
		children.add(ie.getThenPart());
		children.add(ie.getElsePart());
		return getContainingChild(ie, children, position);
	}
	
	private ISyntaxElement getContainingElementOfCollectionExpression(final CollectionExpression ce, int position ) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(ce.getClosure());
		return getContainingChild(ce, children, position);    
	}
	
	private SyntaxElement fixBoundaries(SyntaxElement e) {
		if( !( e.getStart() == 0 && e.getEnd() == 0) )
			return e;
		
		if( e instanceof BooleanOperation ) {
			BooleanOperation be = (BooleanOperation)e;
			SyntaxElement l = fixBoundaries(be.getLeft());
			SyntaxElement r = fixBoundaries(be.getRight());
			be.setLine(l.getLine());
			be.setStart(l.getStart());
			be.setEnd(r.getEnd());
		}
		
		return e;
	}
	
	private ISyntaxElement getContainingElementOfBooleanOperation( final BooleanOperation be, int position ) {
		fixBoundaries(be);
		
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(be.getLeft());
		children.add(be.getRight());
		return getContainingChild(be, children, position);
	}
	
	private ISyntaxElement getContainingElementOfFeatureCall( final FeatureCall fe, int position ) {
		if(!containsPosition(fe,position))
		  return null;
		
		Stack<FeatureCall> fcStack = new Stack<FeatureCall>(); 
		Expression e = fe;
		do {
			FeatureCall fc = (FeatureCall)e;
			fcStack.push(fc);
			e = fc.getTarget();
		} while( e instanceof FeatureCall );
		
		FeatureCall fallback = fcStack.peek();
		
		while( !fcStack.isEmpty() ) {
			FeatureCall fc = fcStack.pop();
			if(fc instanceof OperationCall) {
				ISyntaxElement result = getContainingElementOfOperationCall((OperationCall)fc, position);
				if(result != null) 
					return result;
			} else if( fc instanceof CollectionExpression) {
				if(containsPosition(fc, position) ) return getContainingElement(((CollectionExpression)fc).getClosure(),position);
			}
		}
		
		return fallback;
	}
	
	private ISyntaxElement getContainingElementOfOperationCall( final OperationCall oe, int position ) {
		if(isFunctionCall(oe)) return oe;
		List<SyntaxElement> children = new ArrayList<SyntaxElement>(oe.getParamsAsList());
		return getContainingChild(oe, children, position);
	}
	
	private boolean isFunctionCall(OperationCall op) {
		char[] name = op.getName().toString().toCharArray();
		
		if( name.length == 0 ) return false;
		
		for( int i=0; i<name.length; i++ ) {
			if(i==0) {
				if(!Character.isJavaIdentifierStart(name[i])) return false;
			} else{
				if(!Character.isJavaIdentifierPart(name[i])) return false;
			}
		}
		
		return true;
	}

	private ISyntaxElement getContainingElementOfListLiteral(final ListLiteral ll, int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>(ll.getElementsAsList());
		return getContainingChild(ll, children, position);
	}

	private ISyntaxElement getContainingElementOfSwitchExpression(final SwitchExpression se, int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(se.getSwitchExpr());
		for (Case caze : se.getCases()) {
			children.add(caze.getCondition());
			children.add(caze.getThenPart());
		}
		children.add(se.getDefaultExpr());
		return getContainingChild(se, children, position);
	}

	private ISyntaxElement getContainingElementOfChainExpression(final ChainExpression ce, final int position) {
		List<SyntaxElement> children = new ArrayList<SyntaxElement>();
		children.add(ce.getFirst());
		children.add(ce.getNext());
		return getContainingChild(ce, children, position); 
	}

	/**
	 * @since 2.2
	 */
	protected ISyntaxElement getContainingRootElement(final IResource resource,
			final int position) {
		IXtendXpandResource file = getXtendXpandResource(resource);
		List<ISyntaxElement> elems = collectFirstLevelElements(file);
		for (ISyntaxElement elem : elems)
			if (containsPosition(elem, position))
				return elem;

		return null;
	}

	private IXtendXpandResource getXtendXpandResource(final IResource resource) {
		final IXtendXpandProject project = Activator.getExtXptModelManager().findProject(resource.getProject().getFullPath());
		if (project != null) {
			for (IXtendXpandResource res : project.getAllRegisteredResources()) {
				if (!resource.equals(res.getUnderlyingStorage()))
					continue;
				return res;
			}
		}

		return null;
	}

	// -------------------------------------------------------------------------
	// Expression specific methods

	protected List<ISyntaxElement> collectFirstLevelElements(IXtendXpandResource res) {
		ExtensionFile file = (ExtensionFile) res.getExtXptResource();
		List<ISyntaxElement> result = new ArrayList<ISyntaxElement>();

		result.addAll(file.getArounds());
		result.addAll(file.getChecks());

		for (ISyntaxElement ext : file.getExtensions()) {
			// Hint: we don't create BP internal of Java extension statements
			// Java Debugger to be used instead
			if (ext instanceof JavaExtensionStatement)
				continue;

			result.add(ext);
		}

		return result;
	}

	/**
	 * @since 2.2
	 */
	protected ISyntaxElement getBodyOfRootElement(final ISyntaxElement elem) {
		if (elem instanceof ExpressionExtensionStatement)
			return ((ExpressionExtensionStatement) elem).getExpression();
		else if (elem instanceof CreateExtensionStatement)
			return ((CreateExtensionStatement) elem).getExpression();
		else if (elem instanceof Around)
			return ((Around) elem).getExpression();

		return null;
	}

	protected List<ISyntaxElement> getBody(ISyntaxElement exp) {
		// Hint: we accept all kinds of body elements here, because their
		// children could be OperationCalls
		ISyntaxElement[] body = new ISyntaxElement[0];
		if (exp instanceof IfExpression) {
			IfExpression op = (IfExpression) exp;
			body = new ISyntaxElement[] { op.getCondition(), op.getThenPart(), op.getElsePart() };
		} else if (exp instanceof OperationCall) {
			// Note: Check first for OperationCall because it extends
			// FeatureCall
			OperationCall op = (OperationCall) exp;
			List<ISyntaxElement> kids = new ArrayList<ISyntaxElement>();
			kids.add(op.getTarget());
			kids.add(new EvaluatedElementWrapper(op));
			kids.addAll(op.getParamsAsList());
			body = kids.toArray(new ISyntaxElement[0]);
		} else if (exp instanceof FeatureCall)
			body = new ISyntaxElement[] { ((FeatureCall) exp).getTarget(), new EvaluatedElementWrapper(exp) };
		else if (exp instanceof ExpressionExtensionStatement)
			body = new ISyntaxElement[] { ((ExpressionExtensionStatement) exp).getExpression() };
		else if (exp instanceof CreateExtensionStatement)
			body = new ISyntaxElement[] { ((CreateExtensionStatement) exp).getExpression() };
		else if (exp instanceof Around)
			body = new ISyntaxElement[] { ((Around) exp).getExpression() };
		else if (exp instanceof BooleanOperation) {
			BooleanOperation op = (BooleanOperation) exp;
			body = new ISyntaxElement[] { op.getLeft(), op.getRight() };
		} else if (exp instanceof SwitchExpression) {
			SwitchExpression op = (SwitchExpression) exp;
			body = new ISyntaxElement[] { op.getSwitchExpr(), op.getDefaultExpr() };
		} else if (exp instanceof ChainExpression) {
			ChainExpression op = (ChainExpression) exp;
			body = new ISyntaxElement[] { op.getFirst(), op.getNext() };
		} else if (exp instanceof LetExpression) {
			LetExpression op = (LetExpression) exp;
			body = new ISyntaxElement[] { op.getVarExpression(), op.getTargetExpression() };
		} else if (exp instanceof Cast) {
			Cast op = (Cast) exp;
			body = new ISyntaxElement[] { op.getTarget() };
		}
		List<ISyntaxElement> result = new ArrayList<ISyntaxElement>();
		for (ISyntaxElement element : body)
			if (element != null)
				result.add(element);
		return result;
	}

	protected boolean isValidReturnElement(final ISyntaxElement se) {
		return (se instanceof OperationCall || se instanceof ChainExpression);
	}

	// -------------------------------------------------------------------------

	private Image icon = null;

	public Image getIcon() {
		if (icon != null)
			return icon;
		IPath path = new Path("/icons/extensionfile.gif");
		InputStream is = null;
		try {
			is = FileLocator.openStream(XtendEditorPlugin.getDefault().getBundle(), path, false);
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
