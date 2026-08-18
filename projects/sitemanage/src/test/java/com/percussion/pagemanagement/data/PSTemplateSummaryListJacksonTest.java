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
package com.percussion.pagemanagement.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.json.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire shape for GET {@code /sitemanage/sitetemplates/templates/{site}} must be a JSON array of
 * template summaries, not an ArrayList bean ({@code {"empty":false}}) (#3529 / #3368).
 */
@Tag("UnitTest")
class PSTemplateSummaryListJacksonTest {

  private static PSTemplateSummary sample() {
    PSTemplateSummary t = new PSTemplateSummary();
    t.setId("1-101-7");
    t.setName("perc.page");
    t.setLabel("Page");
    return t;
  }

  @Test
  void productionMapperSerializesTemplateListAsArray() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(PSTemplateSummaryList.class);
    PSTemplateSummaryList list = new PSTemplateSummaryList();
    list.add(sample());

    String json = mapper.writeValueAsString(list);
    assertTrue(
        json.contains("TemplateSummary") || json.contains("["),
        "expected list wire shape, got: " + json);
    assertTrue(json.contains("["), "TemplateSummary list must be a JSON array: " + json);
    assertTrue(json.contains("\"id\""), "template id property must be present: " + json);
    assertTrue(json.contains("\"name\""), "template name property must be present: " + json);
    assertTrue(json.contains("1-101-7"), json);
    assertTrue(json.contains("perc.page"), json);
    assertFalse(
        json.contains("\"empty\""),
        "ArrayList subclass must not serialize as {empty:false} bean (#3529): " + json);

    PSTemplateSummaryList roundTrip = mapper.readValue(json, PSTemplateSummaryList.class);
    assertEquals(1, roundTrip.size());
    assertEquals("1-101-7", roundTrip.get(0).getId());
    assertEquals("perc.page", roundTrip.get(0).getName());
  }

  @Test
  void productionMapperSerializesEmptyTemplateListEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(PSTemplateSummaryList.class);
    String json = mapper.writeValueAsString(new PSTemplateSummaryList());
    assertTrue(json.contains("TemplateSummary") || json.contains("["), json);
    assertFalse(json.contains("\"empty\""), json);
  }
}
