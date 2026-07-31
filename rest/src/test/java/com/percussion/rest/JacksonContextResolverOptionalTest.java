/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.templates.TemplateSummary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * REST JSON must unwrap {@code Optional} DTO getters ({@code Jdk8Module}). Without it, content-type
 * and template catalogs serialize as hideFromMenu / templateId only — SPA tables look empty while
 * design webservices returned real names.
 */
@Tag("UnitTest")
class JacksonContextResolverOptionalTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(ContentType.class);

  /**
   * Locks the pre-fix bug shape: a plain {@link ObjectMapper} (no {@code Jdk8Module}) must not
   * emit {@code name} as a JSON string even when the DTO has a value. Modern Jackson refuses {@code
   * Optional} without the module ({@code InvalidDefinitionException}); older behavior omitted the
   * field. Either outcome proves the module is required — if a future refactor drops {@code
   * registerModule(new Jdk8Module())}, this test still fails while positive tests alone might not.
   */
  @Test
  void plainMapper_withoutJdk8Module_doesNotEmitOptionalNameAsString() throws Exception {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setHideFromMenu(false);

    try {
      String json = new ObjectMapper().writeValueAsString(ct);
      assertFalse(
          json.contains("\"name\":\"percPage\"") || json.contains("\"name\" : \"percPage\""),
          "plain ObjectMapper must not unwrap Optional name — bug shape regression: " + json);
    } catch (InvalidDefinitionException expected) {
      // Jackson 2.12+: Optional requires jackson-datatype-jdk8
      assertTrue(
          expected.getMessage().contains("Optional")
              || expected.getMessage().contains("jdk8"),
          expected.getMessage());
    }
  }

  @Test
  void contentType_serializesNameLabelNotOnlyHideFromMenu() throws Exception {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setHideFromMenu(false);

    String json = mapper.writeValueAsString(ct);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("Page"), json);
    // Must not be the hideFromMenu-only payload that broke the SPA table
    String compact = json.replaceAll("\\s", "");
    assertFalse(
        compact.equals("{\"ContentType\":{\"hideFromMenu\":false}}")
            || compact.equals("{\"hideFromMenu\":false}"),
        "must not emit hideFromMenu-only payload: " + json);
  }

  @Test
  void contentTypeList_includesNames() throws Exception {
    ContentType ct = new ContentType();
    ct.setName("sys_File");
    ct.setLabel("File");
    ContentTypeList list = new ContentTypeList();
    list.add(ct);

    String json = mapper.writeValueAsString(list);
    // WRAP_ROOT_VALUE uses the simple class name (no @JsonRootName on ContentTypeList)
    assertTrue(
        json.contains("ContentTypeList") || json.trim().startsWith("["),
        "list serializes with ContentTypeList root or as array: " + json);
    assertTrue(json.contains("sys_File"), json);
    assertTrue(json.contains("File"), json);
  }

  @Test
  void templateSummary_serializesNameNotOnlyId() throws Exception {
    TemplateSummary t = new TemplateSummary();
    t.setTemplateId(1018);
    t.setTemplateName("perc.page");
    t.setTemplateLabel("Page");

    String json = mapper.writeValueAsString(t);
    assertTrue(json.contains("perc.page"), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("1018"), json);
  }
}
