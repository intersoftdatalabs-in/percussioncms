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
package com.percussion.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Save contract for legacy inline-template ({@code rxvariant}) wrappers
 * round-tripped through the React (TipTap) editor (#7104, parent #5622).
 *
 * <p>The editor submits {@code editor.getHTML()}; on save the field pipeline
 * joins each stored wrapper to its inline relationship through
 * {@code rxinlineslot} and {@link PSInlineLinkField#modifyField(Element, java.util.Map,
 * java.util.Collection) PSInlineLinkField.modifyField}, which rewrites the
 * wrapper's slot/variant/dependent ids from the relationship. If the editor
 * flattened the wrapper into static HTML, the save path would find no inline
 * slot to process and the inline relationship would be lost
 * ({@code PSInlineLinkField} then deletes it on save).
 *
 * <p>These tests parse the exact TipTap-serialized HTML for the stored
 * legacy shapes (block {@code div.rx_ephox_inlinevariant} written by
 * {@code PSSingleValueBuilder.updateLegacyVariant}, inline
 * {@code span.rx_inlinevariant} written by classic {@code editorinline.js})
 * into a w3c DOM, run them through the production {@code modifyField} save
 * routine with a real relationship map, and assert the wrapper is recognized
 * as an inline slot and rewritten from the relationship — using the
 * production attribute-name constants so a rename on either side breaks the
 * test.
 */
@Tag("UnitTest")
public class PSRxVariantWrapperSaveContractTest {

  /**
   * TipTap serialization of the #7104 block repro after the wrapper-preserving
   * schema parses it: the wrapper and its relationship attributes survive,
   * the snippet payload keeps its text.
   */
  private static final String BLOCK_SERIALIZED =
      "<div class=\"rx_ephox_inlinevariant\" rxinlineslot=\"105\" "
          + "inlinetype=\"rxvariant\" sys_dependentid=\"77\" "
          + "sys_dependentvariantid=\"512\"><p>Snippet body</p></div><p>after</p>";

  /**
   * TipTap serialization of the #7104 inline repro: the span wrapper and its
   * relationship attributes survive around the managed link.
   */
  private static final String INLINE_SERIALIZED =
      "<p>See <span class=\"rx_inlinevariant\" rxinlineslot=\"105\" "
          + "inlinetype=\"rxvariant\" sys_dependentid=\"77\" "
          + "sys_dependentvariantid=\"512\"><a href=\"/x\">X</a></span> ok</p>";

  @Test
  public void blockVariantWrapperIsRewrittenFromRelationshipOnSave() throws Exception {
    Element wrapper = parseWrapper(BLOCK_SERIALIZED, "div", "rx_ephox_inlinevariant");

    // The TipTap round-trip must deliver the wrapper with the attributes
    // the save path joins on — otherwise there is nothing to process.
    assertEquals(
        "105",
        wrapper.getAttribute(PSInlineLinkField.RX_INLINESLOT),
        "rxinlineslot must survive the editor round-trip");
    assertEquals(
        "rxvariant",
        wrapper.getAttribute(PSInlineLinkField.RX_INLINETYPE),
        "inlinetype must survive the editor round-trip");

    // The stored field carries the relationship id the save pipeline joins
    // on; the relationship contributes the authoritative slot/variant ids.
    wrapper.setAttribute(IPSHtmlParameters.SYS_RELATIONSHIPID, "7");
    PSRelationship rel = variantRelationship(7, 305);
    List<PSRelationship> modified = new ArrayList<>();

    PSInlineLinkField.modifyField(wrapper, Collections.singletonMap(7, rel), modified);

    assertEquals(
        "105",
        wrapper.getAttribute(PSInlineLinkField.RX_INLINESLOT),
        "save must keep the wrapper on the relationship slot");
    assertEquals(
        "305",
        wrapper.getAttribute(IPSHtmlParameters.SYS_DEPENDENTID),
        "save must rewrite the dependent id from the relationship");
    assertEquals(
        "512",
        wrapper.getAttribute(IPSHtmlParameters.SYS_DEPENDENTVARIANTID),
        "save must rewrite the dependent variant id from the relationship");
    assertEquals(1, modified.size(), "processed relationship must be reported as modified");
    assertSame(rel, modified.get(0), "processed relationship must be the mapped one");
  }

  @Test
  public void inlineVariantWrapperIsRewrittenFromRelationshipOnSave() throws Exception {
    Element wrapper = parseWrapper(INLINE_SERIALIZED, "span", "rx_inlinevariant");

    assertEquals(
        "105",
        wrapper.getAttribute(PSInlineLinkField.RX_INLINESLOT),
        "rxinlineslot must survive the editor round-trip");
    assertEquals(
        "rxvariant",
        wrapper.getAttribute(PSInlineLinkField.RX_INLINETYPE),
        "inlinetype must survive the editor round-trip");

    wrapper.setAttribute(IPSHtmlParameters.SYS_RELATIONSHIPID, "9");
    PSRelationship rel = variantRelationship(9, 311);
    List<PSRelationship> modified = new ArrayList<>();

    PSInlineLinkField.modifyField(wrapper, Collections.singletonMap(9, rel), modified);

    assertEquals(
        "105",
        wrapper.getAttribute(PSInlineLinkField.RX_INLINESLOT),
        "save must keep the wrapper on the relationship slot");
    assertEquals(
        "311",
        wrapper.getAttribute(IPSHtmlParameters.SYS_DEPENDENTID),
        "save must rewrite the dependent id from the relationship");
    assertEquals(
        "512",
        wrapper.getAttribute(IPSHtmlParameters.SYS_DEPENDENTVARIANTID),
        "save must rewrite the dependent variant id from the relationship");
    assertEquals(1, modified.size(), "processed relationship must be reported as modified");
    assertSame(rel, modified.get(0), "processed relationship must be the mapped one");
  }

  /**
   * Builds the inline-template relationship the save path would supply for
   * the stored wrapper: an ActiveAssembly relationship carrying the
   * authoritative slot and variant ids as user properties.
   */
  private static PSRelationship variantRelationship(int id, int dependentId) throws Exception {
    PSRelationship rel =
        new PSRelationship(
            id,
            new PSLocator(300, 1),
            new PSLocator(dependentId, 2),
            PSRelationshipConfigTest.getConfigs()
                .getConfig(PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY));
    rel.setProperty(IPSHtmlParameters.SYS_SLOTID, "105");
    rel.setProperty(IPSHtmlParameters.SYS_VARIANTID, "512");
    return rel;
  }

  /**
   * Parses a TipTap-serialized fragment into a w3c DOM (the element model
   * the save path operates on) and returns the wrapper element.
   */
  private static Element parseWrapper(String fragment, String tag, String cssClass)
      throws Exception {
    String xml = "<root>" + fragment + "</root>";
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
    NodeList nodes = doc.getDocumentElement().getElementsByTagName(tag);
    for (int i = 0; i < nodes.getLength(); i++) {
      Element el = (Element) nodes.item(i);
      if (cssClass.equals(el.getAttribute("class"))) {
        return el;
      }
    }
    throw new IllegalStateException(
        "wrapper <" + tag + " class=\"" + cssClass + "\"> not found in fragment");
  }
}
