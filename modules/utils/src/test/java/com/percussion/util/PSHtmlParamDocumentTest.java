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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSHtmlParamDocumentTest {

  @Test
  public void mapCtorCopiesParamsAndBuildsXml() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("a", "1");
    params.put("b", List.of("x", "y"));

    PSHtmlParamDocument doc = new PSHtmlParamDocument(params);
    String xml = doc.getXmlString();
    assertTrue(xml.contains("<a>1</a>"));
    assertTrue(xml.contains("<b>x</b>"));
    assertTrue(xml.contains("<b>y</b>"));
  }

  @Test
  public void setParamReplacesAndRemoves() {
    PSHtmlParamDocument doc = new PSHtmlParamDocument();
    doc.setParam("k", "v");
    assertTrue(doc.getXmlString().contains("<k>v</k>"));
    doc.setParam("k", "v2");
    assertTrue(doc.getXmlString().contains("<k>v2</k>"));
    doc.setParam("k", null);
    assertEquals(
        true,
        !doc.getXmlString().contains("<k>"),
        "null value removes existing param");
  }

  @Test
  public void setParamsRejectsNull() {
    PSHtmlParamDocument doc = new PSHtmlParamDocument();
    assertThrows(IllegalArgumentException.class, () -> doc.setParams(null));
  }
}
