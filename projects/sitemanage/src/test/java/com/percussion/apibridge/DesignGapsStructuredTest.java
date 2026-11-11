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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.DesignGap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** REST-GAPS-01: designGaps are structured {code,message} on CT / Template / Slot. */
@Tag("UnitTest")
class DesignGapsStructuredTest {

  @Test
  void contentTypeDesignGaps_includeCodesAndMessages() {
    List<DesignGap> gaps = ContentTypeAdaptor.contentTypeDesignGaps(true);
    assertFalse(gaps.isEmpty());
    Set<String> codes = gapCodes(gaps);
    for (String code :
        List.of(
            "CT_FIELD_RULE_EXPR",
            "CT_ITEM_EXITS",
            "CT_CREATE_DELETE",
            "CT_FIELD_CREATE_DELETE",
            "CT_SEARCH_INDEXING",
            "CT_ICON_STRATEGY",
            "CT_IMPORT_EXPORT")) {
      assertTrue(codes.contains(code), () -> "missing gap " + code + ": " + gaps);
    }
    assertGapMessageContains(gaps, "CT_IMPORT_EXPORT", "/contenttypes/import", "create-only");
    assertGapMessageContains(gaps, "CT_SEARCH_INDEXING", "searchIndexing", "held", "default");
    assertGapMessageContains(gaps, "CT_ICON_STRATEGY", "/icon", "held");
    assertGapMessageContains(
        gaps,
        "CT_CREATE_DELETE",
        "POST /contenttypes",
        "PUT /contenttypes/{idOrName}/name",
        "DELETE",
        "held");
    assertGapMessageContains(gaps, "CT_FIELD_CREATE_DELETE", "POST/DELETE", "/fields", "held");
    assertGapMessageContains(gaps, "CT_SHARED_FIELD_INCLUSION", "fields/include", "held");
    assertFalse(
        gapMessage(gaps, "CT_CREATE_DELETE").startsWith("Create / delete not supported"),
        () -> gaps.toString());
    assertFalse(codes.contains("CT_CONTROL_RESOLUTION"));
    assertFalse(
        codes.contains("CT_FIELD_LABELS_WRITE"),
        "local field display labels are writable on PUT detail");
    assertGapsWellFormed(gaps);
  }

  private static Set<String> gapCodes(List<DesignGap> gaps) {
    return gaps.stream().map(DesignGap::getCode).collect(Collectors.toSet());
  }

  private static String gapMessage(List<DesignGap> gaps, String code) {
    return gaps.stream()
        .filter(g -> code.equals(g.getCode()))
        .map(DesignGap::getMessage)
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing gap " + code + ": " + gaps));
  }

  private static void assertGapMessageContains(
      List<DesignGap> gaps, String code, String... fragments) {
    String message = gapMessage(gaps, code);
    String lowered = message.toLowerCase();
    for (String fragment : fragments) {
      assertTrue(
          lowered.contains(fragment.toLowerCase()),
          () -> code + " message missing <" + fragment + ">: " + message);
    }
  }

  private static void assertGapsWellFormed(List<DesignGap> gaps) {
    for (DesignGap g : gaps) {
      assertNotNull(g.getCode());
      assertFalse(g.getCode().isBlank());
      assertNotNull(g.getMessage());
      assertFalse(g.getMessage().isBlank());
    }
  }

  @Test
  void contentTypeDesignGaps_retiredClaimsStayShipped() {
    List<DesignGap> gaps = ContentTypeAdaptor.contentTypeDesignGaps(true);
    for (String code : List.of("CT_ICON_STRATEGY", "CT_SEARCH_INDEXING", "CT_IMPORT_EXPORT")) {
      String message =
          gaps.stream()
              .filter(g -> code.equals(g.getCode()))
              .map(DesignGap::getMessage)
              .findFirst()
              .orElseThrow(() -> new AssertionError("missing gap " + code + ": " + gaps));
      assertFalse(
          message.contains("SPA picker remain Workbench"),
          () -> code + " still claims the SPA picker is Workbench-only: " + message);
      assertFalse(
          message.contains("is not exposed"),
          () -> code + " still claims the SPA surface is not exposed: " + message);
      assertFalse(
          message.contains("remains Workbench until the export slice is installed"),
          () -> code + " still claims export awaits a slice: " + message);
    }
    assertTrue(
        gaps.stream()
            .anyMatch(
                g ->
                    "CT_ICON_STRATEGY".equals(g.getCode())
                        && g.getMessage().contains("SPA picker")),
        () -> gaps.toString());
    assertTrue(
        gaps.stream()
            .anyMatch(
                g ->
                    "CT_SEARCH_INDEXING".equals(g.getCode())
                        && g.getMessage().contains("Developer editor")),
        () -> gaps.toString());
    assertTrue(
        gaps.stream()
            .anyMatch(
                g ->
                    "CT_IMPORT_EXPORT".equals(g.getCode())
                        && g.getMessage().contains("import wizard")),
        () -> gaps.toString());
  }

  @Test
  void contentTypeDesignGaps_correctedClaimsStayShipped() {
    List<DesignGap> gaps = ContentTypeAdaptor.contentTypeDesignGaps(true);
    String ruleExpr = gapMessage(gaps, "CT_FIELD_RULE_EXPR");
    assertFalse(
        ruleExpr.contains("Apply-when on field validation is read-only"),
        () -> "CT_FIELD_RULE_EXPR still claims field-validation apply-when is read-only: "
            + ruleExpr);
    assertGapMessageContains(
        gaps, "CT_FIELD_RULE_EXPR", "apply-when is writable", "ruleExpressions", "held");
    String inclusion = gapMessage(gaps, "CT_SHARED_FIELD_INCLUSION");
    assertFalse(
        inclusion.contains("SPA field picker remains Workbench"),
        () -> "CT_SHARED_FIELD_INCLUSION still claims the SPA picker is Workbench-only: "
            + inclusion);
    assertGapMessageContains(
        gaps, "CT_SHARED_FIELD_INCLUSION", "include picker", "fields/include", "detach");
  }

  @Test
  void contentTypeDesignGaps_addsControlResolutionWhenUnresolved() {
    List<DesignGap> gaps = ContentTypeAdaptor.contentTypeDesignGaps(false);
    assertTrue(
        gaps.stream().anyMatch(g -> "CT_CONTROL_RESOLUTION".equals(g.getCode())),
        () -> gaps.toString());
  }

  @Test
  void templateDesignGaps_dropContentTypeAssocWhenShipped() {
    assertTrue(
        TemplateAdaptor.TEMPLATE_DESIGN_GAPS.stream()
            .noneMatch(g -> "TPL_CONTENT_TYPE_ASSOC".equals(g.getCode())),
        () -> TemplateAdaptor.TEMPLATE_DESIGN_GAPS.toString());
    assertEquals(0, TemplateAdaptor.TEMPLATE_DESIGN_GAPS.size());
  }

  @Test
  void slotDesignGaps_dropAssocGuidsOnlyWhenShipped() {
    assertTrue(
        SlotsAdaptor.SLOT_DESIGN_GAPS.stream()
            .noneMatch(g -> "SLOT_ASSOC_GUIDS_ONLY".equals(g.getCode())),
        () -> SlotsAdaptor.SLOT_DESIGN_GAPS.toString());
    assertEquals(0, SlotsAdaptor.SLOT_DESIGN_GAPS.size());
  }

  @Test
  void sharedFieldControlPropertyDesignGaps_matchGroupRemainingWork() {
    assertEquals(2, SharedFieldsAdaptor.CONTROL_PROPERTY_DESIGN_GAPS.size());
    assertEquals("SF_SPA_EDITOR", SharedFieldsAdaptor.CONTROL_PROPERTY_DESIGN_GAPS.get(0).getCode());
    assertTrue(
        SharedFieldsAdaptor.CONTROL_PROPERTY_DESIGN_GAPS
            .get(0)
            .getMessage()
            .contains("SPA shared-field editor"));
    assertEquals(
        "SF_SYSTEM_DEF_SEPARATE",
        SharedFieldsAdaptor.CONTROL_PROPERTY_DESIGN_GAPS.get(1).getCode());
  }

  @Test
  void systemDefControlPropertyDesignGaps_dropStylesheetAndAppFlow() {
    assertTrue(SystemDefAdaptor.CONTROL_PROPERTY_DESIGN_GAPS.isEmpty());
    assertEquals(
        SystemDefAdaptor.CONTROL_PROPERTY_DESIGN_GAPS, SystemDefAdaptor.STYLESHEET_DESIGN_GAPS);
    assertEquals(
        SystemDefAdaptor.CONTROL_PROPERTY_DESIGN_GAPS,
        SystemDefAdaptor.APPLICATION_FLOW_DESIGN_GAPS);
  }
}
