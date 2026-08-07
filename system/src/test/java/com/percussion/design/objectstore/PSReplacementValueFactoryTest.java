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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral unit tests for {@link PSReplacementValueFactory} after rawtypes/unchecked
 * parameterization (#2295).
 */
class PSReplacementValueFactoryTest {

  @Test
  void getReplacementValueFromXmlFieldName_htmlParam() {
    IPSReplacementValue value =
        PSReplacementValueFactory.getReplacementValueFromXmlFieldName("PSXParam/sys_title");
    assertInstanceOf(PSHtmlParameter.class, value);
    assertEquals("sys_title", ((PSHtmlParameter) value).getName());
  }

  @Test
  void getReplacementValueFromXmlFieldName_legacyAlias() {
    IPSReplacementValue value =
        PSReplacementValueFactory.getReplacementValueFromXmlFieldName("psxparam/foo");
    assertInstanceOf(PSHtmlParameter.class, value);
    assertEquals("foo", ((PSHtmlParameter) value).getName());
  }

  @Test
  void getReplacementValueFromXmlFieldName_cgiVariable() {
    IPSReplacementValue value =
        PSReplacementValueFactory.getReplacementValueFromXmlFieldName("PSXCgiVariable/REMOTE_ADDR");
    assertInstanceOf(PSCgiVariable.class, value);
    assertEquals("REMOTE_ADDR", ((PSCgiVariable) value).getName());
  }

  @Test
  void getReplacementValueFromXmlFieldName_unknownBecomesXmlField() {
    IPSReplacementValue value =
        PSReplacementValueFactory.getReplacementValueFromXmlFieldName("plainField");
    assertInstanceOf(PSXmlField.class, value);
    assertEquals("plainField", value.getValueText());
  }

  @Test
  void getReplacementValueFromXmlFieldName_emptyThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSReplacementValueFactory.getReplacementValueFromXmlFieldName(""));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSReplacementValueFactory.getReplacementValueFromXmlFieldName(null));
  }

  @Test
  void getReplacementValueFromString_htmlParam() {
    IPSReplacementValue value =
        PSReplacementValueFactory.getReplacementValueFromString("PSXHtmlParameter/bar");
    assertInstanceOf(PSHtmlParameter.class, value);
    assertEquals("bar", ((PSHtmlParameter) value).getName());
  }

  @Test
  void getReplacementValueFromString_blankThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSReplacementValueFactory.getReplacementValueFromString(" "));
  }

  @Test
  void getReplacementValueFromString_unknownTypeThrows() {
    // Unknown types throw IllegalArgumentException inside the try, which is wrapped as
    // RuntimeException by the factory's catch-all (pre-existing behavior).
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> PSReplacementValueFactory.getReplacementValueFromString("PSXNoSuchType/x"));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
  }

  @Test
  void isValidFieldName_acceptsMappedAndPlainFields() {
    assertTrue(PSReplacementValueFactory.isValidFieldName("PSXParam/x"));
    assertTrue(PSReplacementValueFactory.isValidFieldName("someXmlField"));
  }

  @Test
  void isValidFieldName_nullOrEmptyThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> PSReplacementValueFactory.isValidFieldName(null));
    assertThrows(
        IllegalArgumentException.class, () -> PSReplacementValueFactory.isValidFieldName("  "));
  }

  @Test
  void getReplacementValueFromXml_htmlParameterRoundTrip() throws Exception {
    PSHtmlParameter original = new PSHtmlParameter("sys_contentid");
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = original.toXml(doc);

    List<IPSComponent> parents = new ArrayList<>();
    IPSReplacementValue restored =
        PSReplacementValueFactory.getReplacementValueFromXml(
            null, parents, el, "container", "value");

    assertInstanceOf(PSHtmlParameter.class, restored);
    assertEquals(original, restored);
  }

  @Test
  void getReplacementValueFromXml_nullNodeThrows() {
    assertThrows(
        PSUnknownNodeTypeException.class,
        () ->
            PSReplacementValueFactory.getReplacementValueFromXml(
                null, null, null, "container", "value"));
  }
}
