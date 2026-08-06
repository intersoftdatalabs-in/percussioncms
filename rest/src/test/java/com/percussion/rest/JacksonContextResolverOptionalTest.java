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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.templates.TemplateSummary;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). ContentType now uses plain getters; TemplateSummary still uses Optional and relies on
 * Jackson 3 databind unwrapping.
 */
@Tag("UnitTest")
class JacksonContextResolverOptionalTest {

  private final ObjectMapper mapper = new JacksonContextResolver().getContext(ContentType.class);

  @Test
  void contentType_serializesNameLabelGuidNotOnlyHideFromMenu() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setHideFromMenu(false);
    ct.setGuid(new Guid("0-2-311"));

    String json = mapper.writeValueAsString(ct);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-2-311") || json.contains("311"), json);
  }

  @Test
  void contentTypeList_serializesNamesNotHideFromMenuOnly() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setGuid(new Guid("0-2-311"));
    ct.setHideFromMenu(false);

    ContentTypeList list = new ContentTypeList(List.of(ct));
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"label\""), json);
    // List root wrap uses ContentType (XmlRootElement on ContentTypeList) or plain array
    assertTrue(json.contains("ContentType") || json.startsWith("["), json);
  }

  @Test
  void templateSummary_serializesNameNotOnlyId() {
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
