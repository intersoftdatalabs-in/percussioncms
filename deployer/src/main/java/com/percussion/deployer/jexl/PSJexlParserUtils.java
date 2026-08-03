/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.deployer.jexl;

import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ParseException;
import org.apache.commons.jexl3.parser.SimpleNode;

/**
 * A util class for parsing jexl expressions or scripts.
 *
 * @author vamsinukala
 */
public class PSJexlParserUtils {

  /** Default constructor for use via static methods. */
  public PSJexlParserUtils() {}

  /**
   * Internal Engine subclass used to expose the protected {@code parse()} method for AST
   * construction without using reflection. This is required because the internal JEXL3 {@code
   * Parser} class changed its constructor signature in 3.6.1 to require a {@code JexlParser}
   * parent, removing the previous {@code StringReader}-based initialization.
   */
  private static final class JexlParseHelper extends Engine {
    /**
     * Parses the given script text and returns the root AST node.
     *
     * @param scriptText the JEXL script or expression text, not {@code null}
     * @return the parsed {@link ASTJexlScript}
     */
    public ASTJexlScript parseScript(String scriptText) {
      return super.parse(null, null, scriptText, null);
    }
  }

  /** Singleton parse helper, reuses the same Engine for all parsing operations. */
  private static final JexlParseHelper PARSE_HELPER = new JexlParseHelper();

  /**
   * Parses a JEXL script and returns a {@link PSJexlSimpleNode} wrapping the root AST.
   *
   * @param scriptText the JEXL script text to parse, not {@code null}
   * @return a {@link PSJexlSimpleNode} wrapping the parsed script
   * @throws Exception if any parsing error occurs
   */
  public static PSJexlSimpleNode createScriptNode(String scriptText) throws Exception {
    var script = parseToAst(scriptText);
    return new PSJexlSimpleNode(script, scriptText);
  }

  /**
   * Parses a JEXL expression and returns a {@link PSJexlSimpleNode} wrapping the first child of the
   * root AST, which represents the expression statement.
   *
   * @param expression the JEXL expression text, not {@code null}
   * @param isBoolean unused in JEXL3 3.6+ (kept for API compatibility)
   * @return a {@link PSJexlSimpleNode} wrapping the parsed expression
   * @throws ParseException if any parsing error occurs
   */
  public static PSJexlSimpleNode createNewExpression(final String expression, boolean isBoolean)
      throws ParseException {
    var expr = expression.trim();
    if (!expr.endsWith(";") && !expr.endsWith("}")) {
      expr += ";";
    }

    var tree = parseToAst(expr);
    var node = (SimpleNode) tree.jjtGetChild(0);
    return new PSJexlSimpleNode(node, expression);
  }

  /**
   * Parses the given script text to an {@link ASTJexlScript}.
   *
   * @param scriptText the text to parse, not {@code null}
   * @return the root {@link ASTJexlScript} node
   * @throws ParseException if the text cannot be parsed
   */
  private static ASTJexlScript parseToAst(String scriptText) throws ParseException {
    try {
      return PARSE_HELPER.parseScript(scriptText);
    } catch (org.apache.commons.jexl3.JexlException e) {
      throw new ParseException(e.getMessage());
    }
  }
}
