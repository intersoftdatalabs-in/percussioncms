/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.deployer.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.jexl3.parser.SimpleNode;
import org.junit.jupiter.api.Test;

/**
 * Unit tests to verify that the JEXL visitor correctly traverses JEXL 3 specific nodes such as
 * ASTQualifiedIdentifier, ASTIdentifierAccess, and ASTNamespaceIdentifier.
 */
public class PSJexl3NodesTest {

  @Test
  public void testQualifiedIdentifier() throws Exception {
    // a.b.c = '123'
    // The visitor should find '123'
    String code = "a.b.c = '123';";
    PSGetIDsJexlVisitor visitor = new PSGetIDsJexlVisitor();
    PSJexlSimpleNode psExp = PSJexlParserUtils.createScriptNode(code);
    SimpleNode exp = psExp.getNode();
    exp.childrenAccept(visitor, exp);

    assertEquals(1, visitor.getIds().size());
    assertTrue(visitor.getIds().contains("123"));
  }

  @Test
  public void testIdentifierAccess() throws Exception {
    // a['123']
    // The visitor should find '123' as it is a numeric string literal
    String code = "a['123'];";
    PSGetIDsJexlVisitor visitor = new PSGetIDsJexlVisitor();
    PSJexlSimpleNode psExp = PSJexlParserUtils.createScriptNode(code);
    SimpleNode exp = psExp.getNode();
    exp.childrenAccept(visitor, exp);

    assertEquals(1, visitor.getIds().size());
    assertTrue(visitor.getIds().contains("123"));
  }

  @Test
  public void testNamespaceIdentifier() throws Exception {
    // ns:func('456')
    // The visitor should find '456'
    String code = "ns:func('456');";
    PSGetIDsJexlVisitor visitor = new PSGetIDsJexlVisitor();
    PSJexlSimpleNode psExp = PSJexlParserUtils.createScriptNode(code);
    SimpleNode exp = psExp.getNode();
    exp.childrenAccept(visitor, exp);

    assertEquals(1, visitor.getIds().size());
    assertTrue(visitor.getIds().contains("456"));
  }

  @Test
  public void testComplexQualifiedIdentifier() throws Exception {
    // $rx.codec.decodeFromXml("789")
    String code = "$rx.codec.decodeFromXml('789');";
    PSGetIDsJexlVisitor visitor = new PSGetIDsJexlVisitor();
    PSJexlSimpleNode psExp = PSJexlParserUtils.createScriptNode(code);
    SimpleNode exp = psExp.getNode();
    exp.childrenAccept(visitor, exp);

    assertEquals(1, visitor.getIds().size());
    assertTrue(visitor.getIds().contains("789"));
  }
}
