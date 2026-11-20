/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ui.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.internal.xtend.expression.ast.ISyntaxElement;
import org.eclipse.internal.xtend.expression.ast.SyntaxElement;
import org.eclipse.internal.xtend.xtend.ast.Check;


/**
 * @author kia7si - Initial contribution and API
 * @author Karsten Thoms (itemis) - maintenance
 * @since 2.2
 */
public class CheckPluginAdapter extends ExpressionPluginAdapter {
  @Override
  protected String getRequiredExtension() {
    return "chk";
  }
  
  @Override
  protected ISyntaxElement findElementForPosition(IResource resource, int position, int line) {
    ISyntaxElement rootElem = getContainingRootElement(resource, position);

    if(rootElem instanceof Check)
      return getContainingElementOfCheckStatement((Check)rootElem, position);
    
    return super.getContainingElement((SyntaxElement)rootElem, position);
  }
  
  private ISyntaxElement getContainingElementOfCheckStatement(Check rootElem, int position) {
    List<SyntaxElement> children = new ArrayList<SyntaxElement>();
    children.add(rootElem.getMsg());
    children.add(rootElem.getConstraint());
    return getContainingChild(rootElem, children, position);
  }
}