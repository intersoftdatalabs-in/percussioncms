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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Minimal persist/meta coverage for the {@code InlineLinkTitleField} sys_tinymce control parameter
 * (issue #2241 / parent #946). Empty/absent remains backward compatible with product defaults
 * ({@code displaytitle} for assets; page link title for pages — resolved in #2242).
 */
@Tag("UnitTest")
public class PSInlineLinkTitleFieldControlParamTest {

  /** Control-settings param name (PascalCase peers: InlineLinkSlot). */
  public static final String PARAM_NAME = "InlineLinkTitleField";

  /** Control that hosts the setting. */
  public static final String CONTROL_NAME = "sys_tinymce";

  @Test
  public void controlMeta_parsesEmptyDefaultForInlineLinkTitleField() throws Exception {
    String xml =
        """
        <controls xmlns:psxctl="urn:percussion.com/control">
          <psxctl:ControlMeta name="sys_tinymce" dimension="single" choiceset="none">
            <psxctl:Description>TinyMCE</psxctl:Description>
            <psxctl:ParamList>
              <psxctl:Param name="InlineLinkTitleField" datatype="String" paramtype="generic">
                <psxctl:Description>inline link title field name</psxctl:Description>
                <psxctl:DefaultValue></psxctl:DefaultValue>
              </psxctl:Param>
            </psxctl:ParamList>
          </psxctl:ControlMeta>
        </controls>
        """;
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
    Element metaEl =
        (Element) doc.getElementsByTagName(PSControlMeta.XML_NODE_NAME).item(0);
    PSControlMeta meta = new PSControlMeta(metaEl);

    assertEquals(CONTROL_NAME, meta.getName());
    @SuppressWarnings("unchecked")
    List<PSControlParameter> params = meta.getParams();
    assertNotNull(params);
    assertEquals(1, params.size());

    PSControlParameter param = params.get(0);
    assertEquals(PARAM_NAME, param.getName());
    assertEquals("String", param.getDataType());
    assertEquals("generic", param.getParamType());
    // Empty default = use product displaytitle / resource_link_title defaults at resolve time
    assertEquals("", param.getDefaultValue());
  }

  @Test
  public void controlRef_roundTripsConfiguredTitleFieldParam() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCollection parameters = new PSCollection(PSParam.class);
    parameters.add(new PSParam(PARAM_NAME, new PSTextLiteral("pagetitle")));
    parameters.add(new PSParam("InlineLinkSlot", new PSTextLiteral("103")));

    PSControlRef original = new PSControlRef(CONTROL_NAME, parameters);
    Element elem = original.toXml(doc);

    PSControlRef restored = new PSControlRef(elem, null, null);
    assertEquals(original, restored);
    assertEquals(CONTROL_NAME, restored.getName());

    String titleField = null;
    String linkSlot = null;
    Iterator<?> it = restored.getParameters();
    while (it.hasNext()) {
      PSParam p = (PSParam) it.next();
      if (PARAM_NAME.equals(p.getName())) {
        titleField = p.getValue().getValueText();
      } else if ("InlineLinkSlot".equals(p.getName())) {
        linkSlot = p.getValue().getValueText();
      }
    }
    assertEquals("pagetitle", titleField);
    assertEquals("103", linkSlot);
  }

  @Test
  public void controlRef_roundTripsAbsentTitleFieldAsNoParam() throws Exception {
    // Content types that never set the param stay empty — no new required param on the control.
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCollection parameters = new PSCollection(PSParam.class);
    parameters.add(new PSParam("height", new PSTextLiteral("350")));

    PSControlRef original = new PSControlRef(CONTROL_NAME, parameters);
    PSControlRef restored = new PSControlRef(original.toXml(doc), null, null);

    boolean foundTitleField = false;
    Iterator<?> it = restored.getParameters();
    while (it.hasNext()) {
      PSParam p = (PSParam) it.next();
      if (PARAM_NAME.equals(p.getName())) {
        foundTitleField = true;
      }
    }
    assertTrue(!foundTitleField, "absent InlineLinkTitleField must not be invented on round-trip");
  }

  @Test
  public void controlRef_roundTripsExplicitEmptyTitleField() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCollection parameters = new PSCollection(PSParam.class);
    parameters.add(new PSParam(PARAM_NAME, new PSTextLiteral("")));

    PSControlRef original = new PSControlRef(CONTROL_NAME, parameters);
    PSControlRef restored = new PSControlRef(original.toXml(doc), null, null);
    assertEquals(original, restored);

    Iterator<?> it = restored.getParameters();
    assertTrue(it.hasNext());
    PSParam p = (PSParam) it.next();
    assertEquals(PARAM_NAME, p.getName());
    assertEquals("", p.getValue().getValueText());
  }
}
