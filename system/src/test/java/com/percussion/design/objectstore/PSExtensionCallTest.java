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

import com.percussion.extension.PSExtensionRef;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral unit tests for {@link PSExtensionCall} after rawtypes/unchecked parameterization
 * (#2309).
 */
class PSExtensionCallTest {

  private static PSExtensionRef sampleRef() {
    return new PSExtensionRef("Java", "global/percussion/generic/", "sys_MakeLink");
  }

  @Test
  void constructsWithParamsAndRef() {
    PSExtensionParamValue[] params =
        new PSExtensionParamValue[] {
          new PSExtensionParamValue(new PSTextLiteral("p1")),
          new PSExtensionParamValue(new PSTextLiteral("p2"))
        };
    PSExtensionCall call = new PSExtensionCall(sampleRef(), params);

    assertEquals("sys_MakeLink", call.getName());
    assertEquals(PSExtensionCall.VALUE_TYPE, call.getType());
    assertEquals(2, call.getParamValues().length);
    assertEquals("p1", call.getParamValues()[0].getValue().getValueText());
    assertEquals(0, call.getColumnsForSelect().length);
  }

  @Test
  void setParamValuesNullClearsParams() {
    PSExtensionCall call =
        new PSExtensionCall(
            sampleRef(),
            new PSExtensionParamValue[] {new PSExtensionParamValue(new PSTextLiteral("x"))});
    call.setParamValues((PSExtensionParamValue[]) null);
    assertEquals(0, call.getParamValues().length);
  }

  @Test
  void getParametersReturnsIpsParameterCollection() {
    PSExtensionCall call =
        new PSExtensionCall(
            sampleRef(),
            new PSExtensionParamValue[] {new PSExtensionParamValue(new PSTextLiteral("z"))});
    Collection<? extends IPSParameter> params = call.getParameters();
    assertEquals(1, params.size());
    for (IPSParameter p : params) {
      assertEquals("z", p.getValue().getValueText());
    }
  }

  @Test
  void applyToListIsTyped() {
    PSExtensionCall call = new PSExtensionCall(sampleRef(), null);
    assertFalse(call.getApplyTo().hasNext());

    List<String> handlers = new ArrayList<>(Arrays.asList("edit", "preview"));
    call.setApplyTo(handlers);

    Iterator<String> it = call.getApplyTo();
    assertTrue(it.hasNext());
    assertEquals("edit", it.next());
    assertEquals("preview", it.next());
    assertFalse(it.hasNext());
  }

  @Test
  void xmlRoundTripPreservesRefAndParams() throws Exception {
    PSExtensionCall original =
        new PSExtensionCall(
            sampleRef(),
            new PSExtensionParamValue[] {
              new PSExtensionParamValue(new PSTextLiteral("alpha")),
              new PSExtensionParamValue(new PSTextLiteral("beta"))
            });
    original.setId(3);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);
    assertEquals(PSExtensionCall.ms_NodeType, xml.getNodeName());

    List<IPSComponent> parents = new ArrayList<>();
    PSExtensionCall restored = new PSExtensionCall(xml, null, parents);
    assertEquals(original.getExtensionRef().toString(), restored.getExtensionRef().toString());
    assertEquals(original.getId(), restored.getId());
    assertEquals(2, restored.getParamValues().length);
    assertEquals("alpha", restored.getParamValues()[0].getValue().getValueText());
    assertEquals("beta", restored.getParamValues()[1].getValue().getValueText());
    assertTrue(original.equals(restored));
  }

  @Test
  void cloneIsDeepForParams() {
    PSExtensionCall original =
        new PSExtensionCall(
            sampleRef(),
            new PSExtensionParamValue[] {new PSExtensionParamValue(new PSTextLiteral("ABC"))});
    List<String> apply = new ArrayList<>();
    apply.add("edit");
    original.setApplyTo(apply);

    PSExtensionCall copy = (PSExtensionCall) original.clone();
    assertNotSame(original, copy);
    assertEquals(original, copy);
    assertNotSame(original.getParamValues()[0], copy.getParamValues()[0]);
    assertEquals("ABC", copy.getParamValues()[0].getValue().getValueText());
  }

  @Test
  void toStringIncludesNameAndParams() {
    PSExtensionCall call =
        new PSExtensionCall(
            sampleRef(),
            new PSExtensionParamValue[] {
              new PSExtensionParamValue(new PSTextLiteral("one")),
              new PSExtensionParamValue(new PSTextLiteral("two"))
            });
    String text = call.toString();
    assertTrue(text.contains("sys_MakeLink"));
    assertTrue(text.contains("one"));
    assertTrue(text.contains("two"));
  }
}
