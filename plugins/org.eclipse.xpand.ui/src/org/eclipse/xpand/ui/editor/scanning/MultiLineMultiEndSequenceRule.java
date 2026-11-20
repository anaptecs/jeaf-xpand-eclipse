/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xpand.ui.editor.scanning;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;

/**
 * @author Benedikt Niehues - Initial contribution and API
 */
public class MultiLineMultiEndSequenceRule extends MultiLineRule {

	private String[] fEndSequences;
	private boolean breaksOnEOF;
	//(endSequence == null ? new char[0] : endSequence.toCharArray())
	
	@Override
	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		for (String endSequence : fEndSequences) {
			fEndSequence=(endSequence == null ? new char[0] : endSequence.toCharArray());
			if (super.endSequenceDetected(scanner)){
				return true;
			}
		}
		return breaksOnEOF;
	}

	
	public MultiLineMultiEndSequenceRule(String startSequence, String[] endSequences, IToken token, char escapeCharacter, boolean breaksOnEOF) {
		super(startSequence, "", token, escapeCharacter,false);
		this.breaksOnEOF = breaksOnEOF;
		fEndSequences = endSequences; 
	}

	
}