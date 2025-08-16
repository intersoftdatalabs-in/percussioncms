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

import java.io.StringReader;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ParseException;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.SimpleNode;
import org.apache.commons.jexl3.parser.TokenMgrError;

/**
 * A util class for parsing jexl expressions or scripts
 *
 * @author vamsinukala
 */
public class PSJexlParserUtils {
  /** the jexl parser */
  protected static Parser ms_parser = new Parser(new StringReader(";"));

  /**
   * With the JEXL script, parse it
   *
   * @param scriptText
   * @return the parsed expression as a simple node
   * @throws Exception any parser exception
   */
  public static PSJexlSimpleNode createScriptNode(String scriptText) throws Exception {
    var script = parseScript(scriptText);
    if (script instanceof ASTJexlScript) {
      return new PSJexlSimpleNode(script, scriptText);
    } else {
      throw new IllegalStateException("Parsed script is not a JEXL Script");
    }
  }

  /**
   * With the JEXL expression, parse it
   *
   * @param expression
   * @return the parsed expression as a simple node
   */
  public static PSJexlSimpleNode createNewExpression(final String expression, boolean isBoolean)
      throws ParseException {
    var expr = expression.trim();
    if (!expr.endsWith(";") && !expr.endsWith("}")) {
      expr += ";";
    }

    var tree = parseScript(expr, isBoolean);
    var node = (SimpleNode) tree.jjtGetChild(0);
    return new PSJexlSimpleNode(node, expression);
  }

  private static SimpleNode parseScript(String scriptText, boolean isBoolean)
      throws ParseException {
    try {
      return ms_parser.parse(null, scriptText, null, false, isBoolean);
    } catch (TokenMgrError tme) {
      throw new ParseException(tme.getMessage());
    }
  }
}
