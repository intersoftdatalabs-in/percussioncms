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
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.templates.TemplateSummary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * REST JSON must unwrap {@code Optional} DTO getters (Jdk8Module). Without it, content-type and
 * template catalogs serialize as hideFromMenu / templateId only — SPA tables look empty while
 * design webservices returned real names.
 */
@Tag("UnitTest")
class JacksonContextResolverOptionalTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(ContentType.class);

  @Test
  void contentType_serializesNameLabelNotOnlyHideFromMenu() throws Exception {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setHideFromMenu(false);

    String json = mapper.writeValueAsString(ct);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("Page"), json);
    assertFalse(
        json.replaceAll("\\s", "").matches(".*\\{\"hideFromMenu\":false\\}.*")
            && !json.contains("percPage"),
        "must not emit hideFromMenu-only payload: " + json);
  }

  @Test
  void contentTypeList_rootWrap_includesNames() throws Exception {
    ContentType ct = new ContentType();
    ct.setName("sys_File");
    ct.setLabel("File");
    ContentTypeList list = new ContentTypeList();
    list.add(ct);

    String json = mapper.writeValueAsString(list);
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
