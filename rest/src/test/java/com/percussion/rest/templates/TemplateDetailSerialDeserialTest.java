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

package com.percussion.rest.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for TemplateDetail under WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE (#3039).
 *
 * <p>SPA clients must unwrap {@code {"TemplateDetail":{…}}} or source binds empty.
 */
@Tag("UnitTest")
public class TemplateDetailSerialDeserialTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(TemplateDetail.class);

  @Test
  public void serializesTemplateSourceUnderRoot() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    d.setLabel("Page");
    d.setTemplateSource("#header()\n$body\n");

    String json = mapper.writeValueAsString(d);

    assertTrue(json.contains("\"TemplateDetail\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"templateSource\""), json);
    assertTrue(json.contains("#header()"), json);
  }

  @Test
  public void deserializesWrappedBodyWithTemplateSource() {
    String json =
        "{\"TemplateDetail\":{"
            + "\"name\":\"site.base\","
            + "\"templateSource\":\"#footer()\\n\""
            + "}}";

    TemplateDetail d = mapper.readValue(json, TemplateDetail.class);
    assertEquals("site.base", d.getName());
    assertEquals("#footer()\n", d.getTemplateSource());
  }

  @Test
  public void roundTripPreservesEmptySource() {
    TemplateDetail d = new TemplateDetail();
    d.setName("empty.tpl");
    d.setTemplateSource("");

    String json = mapper.writeValueAsString(d);
    TemplateDetail back = mapper.readValue(json, TemplateDetail.class);
    assertEquals("empty.tpl", back.getName());
    // empty string is not null; NON_NULL still emits it
    assertEquals("", back.getTemplateSource());
  }
}
