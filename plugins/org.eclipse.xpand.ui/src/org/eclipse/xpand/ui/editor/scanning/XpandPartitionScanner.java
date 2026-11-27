/*******************************************************************************
 * Copyright (c) 2005, 2007 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/

package org.eclipse.xpand.ui.editor.scanning;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.internal.xpand2.XpandTokens;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * @author Sven Efftinge (http://www.efftinge.de)
 * @author Karsten Thoms (Bug#322394)
 * 
 */
public class XpandPartitionScanner extends RuleBasedPartitionScanner {
	public final static String TAG = "__tag";
	public final static String COMMENT = "__comment";

	public XpandPartitionScanner() {

		final IToken tag = new Token(TAG);
		final IToken comment = new Token(COMMENT);

		final List<IRule> rules = new ArrayList<IRule>(2);

		// TODO we need to handle whitespace like this: '<< REM .... ENDREM >>'
		rules.add(new MultiLineRule(XpandTokens.LT + XpandTokens.REM,
				XpandTokens.ENDREM + XpandTokens.RT, comment, (char) 0, true) {
			/**
			 * Must detect ENDREM-»
			 */
			@Override
			protected boolean sequenceDetected(final ICharacterScanner scanner,
					final char[] sequence, final boolean eofAllowed) {
				for (int i = 1; i < sequence.length; i++) {
					int c = scanner.read();
					// Skip '-' before XpandTokens.RT
					if (c == '-' && sequence[i] == XpandTokens.RT_CHAR) {
						c = scanner.read();
					}
					if (c == ICharacterScanner.EOF && eofAllowed) {
						return true;
					}

					if (c != sequence[i]) {
						// Non-matching character detected, rewind the scanner
						// back to the start.
						// Do not unread the first character.
						scanner.unread();
						for (int j = i - 1; j > 0; j--)
							scanner.unread();
						return false;
					}
				}

				return true;
			}
		});

		rules.add(new MultiLineRule(XpandTokens.LT, XpandTokens.RT, tag));

		setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
	}
}
