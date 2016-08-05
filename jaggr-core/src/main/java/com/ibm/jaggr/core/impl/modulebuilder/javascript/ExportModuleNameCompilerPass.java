/*
 * (C) Copyright IBM Corp. 2012, 2016 All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.jaggr.core.impl.modulebuilder.javascript;

import com.ibm.jaggr.core.util.JSSource;
import com.ibm.jaggr.core.util.JSSource.PositionLocator;

import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class ExportModuleNameCompilerPass implements CompilerPass {

	private JSSource source;

	public ExportModuleNameCompilerPass(JSSource source) {
		this.source = source;
	}

	/* (non-Javadoc)
	 * @see com.google.javascript.jscomp.CompilerPass#process(com.google.javascript.rhino.Node, com.google.javascript.rhino.Node)
	 */
	@Override
	public void process(Node externs, Node root) {
		processChildren(root);
	}

	/**
	 * Recursively called to process AST nodes looking for anonymous define calls. If an
	 * anonymous define call is found, then change it be a named define call, specifying
	 * the module name for the file being processed.
	 *
	 * @param node
	 *            The node being processed
	 */
	public void processChildren(Node node) {
		for (Node cursor = node.getFirstChild(); cursor != null; cursor = cursor.getNext()) {
			if (cursor.getType() == Token.CALL) {
				// The node is a function or method call
				Node name = cursor.getFirstChild();
				if (name != null && name.getType() == Token.NAME && // named function call
						name.getString().equals("define")) { // name is "define" //$NON-NLS-1$
					Node param = name.getNext();
					if (param != null && param.getType() != Token.STRING) {
						String expname = name.getProp(Node.SOURCENAME_PROP).toString();
						if (source != null) {
							PositionLocator locator = source.locate(name.getLineno(), name.getCharno()+6);
							char tok = locator.findNextJSToken();	// move cursor to the open paren
							if (tok == '(') {
								// Try to insert the module name immediately following the open paren for the
								// define call because the param location will be off if the argument list is parenthesized.
								source.insert("\"" + expname + "\",", locator.getLineno(), locator.getCharno()+1); //$NON-NLS-1$ //$NON-NLS-2$
							} else {
								// First token following 'define' name is not a paren, so fall back to inserting
								// before the first parameter.
								source.insert("\"" + expname + "\",", param.getLineno(), param.getCharno()); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						param.getParent().addChildBefore(Node.newString(expname), param);
					}
				}
			}
			// Recursively call this method to process the child nodes
			if (cursor.hasChildren())
				processChildren(cursor);
		}
	}
}
