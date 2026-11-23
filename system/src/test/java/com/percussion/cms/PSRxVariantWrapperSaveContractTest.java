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

import com.percussion.system.utils.IPSHtmlParameters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Save contract for legacy inline-template ({@code rxvariant}) wrappers
 * round-tripped through the React (TipTap) editor (#7104, parent #5622).
 *
 * <p>The editor submits {@code editor.getHTML()}; on save
 * {@code PSInlineLinkField.processField} keeps the inline relationship only
 * when the wrapper element still carries {@code rxinlineslot} +
 * {@code inlinetype} (and {@code processInlineLink} additionally requires
 * {@code sys_dependentid} + {@code sys_dependentvariantid}). If the editor
 * flattened the wrapper into static HTML, those attributes would be gone and
 * the save would delete the inline relationship.
 *
 * <p>These tests parse the exact TipTap-serialized HTML for the stored
 * legacy shapes (block {@code div.rx_ephox_inlinevariant} written by
 * {@code PSSingleValueBuilder.updateLegacyVariant}, inline
 * {@code span.rx_inlinevariant} written by classic {@code editorinline.js})
 * and assert every attribute the save path keys on is present with the
 * submitted values, using the production attribute-name constants so a
 * rename on either side breaks the test.
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
  public void blockVariantWrapperKeepsSaveRequiredAttributes() {
    Document doc = Jsoup.parseBodyFragment(BLOCK_SERIALIZED);
    Element wrapper = doc.selectFirst("div.rx_ephox_inlinevariant");

    // The wrapper itself must survive: without it there is no element for
    // processField to match the inline relationship against.
    assertEquals(
        "Snippet body",
        wrapper.text(),
        "snippet text must survive the editor round-trip");

    assertSaveRequiredAttributes(wrapper);
  }

  @Test
  public void inlineVariantWrapperKeepsSaveRequiredAttributes() {
    Document doc = Jsoup.parseBodyFragment(INLINE_SERIALIZED);
    Element wrapper = doc.selectFirst("span.rx_inlinevariant");

    assertEquals("X", wrapper.selectFirst("a[href=/x]").text());
    assertSaveRequiredAttributes(wrapper);
  }

  /**
   * Asserts the exact attributes {@code PSInlineLinkField.processField} and
   * {@code processInlineLink} require to keep (rather than delete) the inline
   * relationship for an element on save.
   */
  private static void assertSaveRequiredAttributes(Element wrapper) {
    // processField matches inline content on these two attributes.
    assertEquals(
        "105", wrapper.attr(PSInlineLinkField.RX_INLINESLOT), "rxinlineslot must survive");
    assertEquals(
        "rxvariant",
        wrapper.attr(PSInlineLinkField.RX_INLINETYPE),
        "inlinetype must survive");

    // processInlineLink throws when either id is missing and deletes the
    // relationship when the wrapper carrying them is gone.
    assertEquals(
        "77",
        wrapper.attr(IPSHtmlParameters.SYS_DEPENDENTID),
        "sys_dependentid must survive");
    assertEquals(
        "512",
        wrapper.attr(IPSHtmlParameters.SYS_DEPENDENTVARIANTID),
        "sys_dependentvariantid must survive");
  }
}
