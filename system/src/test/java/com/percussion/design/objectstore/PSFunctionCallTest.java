/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral unit tests for {@link PSFunctionCall} after rawtypes/unchecked parameterization
 * (#2309).
 */
class PSFunctionCallTest {

  @Test
  void constructsWithLiteralParams() {
    PSFunctionParamValue[] params =
        new PSFunctionParamValue[] {
          new PSFunctionParamValue(new PSTextLiteral("a")),
          new PSFunctionParamValue(new PSTextLiteral("b"))
        };
    PSFunctionCall call = new PSFunctionCall("UPPER", params, null, null);

    assertEquals("UPPER", call.getName());
    assertEquals("UPPER", call.getDatabaseFunctionName());
    assertEquals(PSFunctionCall.VALUE_TYPE, call.getValueType());
    assertEquals(2, call.getParamValues().length);
    assertEquals("a", call.getParamValues()[0].getValue().getValueText());
    assertEquals("b", call.getParamValues()[1].getValue().getValueText());
    assertTrue(call.hasStaticParamsOnly());
    assertEquals(0, call.getColumnsForSelect().length);
  }

  @Test
  void setParamValuesNullClearsParams() {
    PSFunctionCall call =
        new PSFunctionCall(
            "LEN",
            new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("x"))},
            null,
            null);
    call.setParamValues((PSFunctionParamValue[]) null);
    assertEquals(0, call.getParamValues().length);
    assertTrue(call.getParameters().isEmpty());
  }

  @Test
  void getParametersReturnsTypedCollection() {
    PSFunctionCall call =
        new PSFunctionCall(
            "TRIM",
            new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("z"))},
            null,
            null);
    Collection<PSFunctionParamValue> params = call.getParameters();
    assertEquals(1, params.size());
    for (PSFunctionParamValue p : params) {
      assertEquals("z", p.getValue().getValueText());
    }
  }

  @Test
  void xmlRoundTripPreservesNameAndParams() throws Exception {
    PSFunctionCall original =
        new PSFunctionCall(
            "CONCAT",
            new PSFunctionParamValue[] {
              new PSFunctionParamValue(new PSTextLiteral("hello")),
              new PSFunctionParamValue(new PSTextLiteral("world"))
            },
            null,
            null);
    original.setId(7);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);
    assertEquals(PSFunctionCall.XML_NODE_NAME, xml.getNodeName());

    List<IPSComponent> parents = new ArrayList<>();
    PSFunctionCall restored = new PSFunctionCall(xml, null, parents);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getId(), restored.getId());
    assertEquals(2, restored.getParamValues().length);
    assertEquals("hello", restored.getParamValues()[0].getValue().getValueText());
    assertEquals("world", restored.getParamValues()[1].getValue().getValueText());
    assertTrue(original.equals(restored));
  }

  @Test
  void cloneIsDeepForParams() {
    PSFunctionCall original =
        new PSFunctionCall(
            "LOWER",
            new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("ABC"))},
            null,
            null);
    PSFunctionCall copy = (PSFunctionCall) original.clone();
    assertNotSame(original, copy);
    assertEquals(original, copy);
    assertNotSame(original.getParamValues()[0], copy.getParamValues()[0]);
    assertEquals("ABC", copy.getParamValues()[0].getValue().getValueText());
  }

  @Test
  void equalsIsCaseInsensitiveOnFunctionName() {
    PSFunctionCall a =
        new PSFunctionCall(
            "upper",
            new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("x"))},
            null,
            null);
    PSFunctionCall b =
        new PSFunctionCall(
            "UPPER",
            new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("x"))},
            null,
            null);
    assertTrue(a.equals(b));
    assertEquals(a.hashCode(), b.hashCode());

    PSFunctionCall c =
        new PSFunctionCall(
            "LOWER",
            new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("x"))},
            null,
            null);
    assertFalse(a.equals(c));
  }

  @Test
  void valueDisplayTextIncludesParams() {
    PSFunctionCall call =
        new PSFunctionCall(
            "COALESCE",
            new PSFunctionParamValue[] {
              new PSFunctionParamValue(new PSTextLiteral("one")),
              new PSFunctionParamValue(new PSTextLiteral("two"))
            },
            null,
            null);
    String display = call.getValueDisplayText();
    assertTrue(display.startsWith("COALESCE("));
    assertTrue(display.contains("one"));
    assertTrue(display.contains("two"));
    assertEquals(display, call.toString());
  }
}
