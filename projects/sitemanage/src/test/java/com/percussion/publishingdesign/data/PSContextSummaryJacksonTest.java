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
package com.percussion.publishingdesign.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.json.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for publishing-context create under WRAP_ROOT_VALUE (#5183).
 *
 * <p>The SPA must post {@code {"context":{...}}}. A flat {@code name} root does not bind, so
 * create fails before the context is saved. Copy sends the new name and the source description
 * and does not send a default scheme id.
 */
@Tag("UnitTest")
class PSContextSummaryJacksonTest {

  private final ObjectMapper mapper = new JacksonContextResolver().getContext(PSContextSummary.class);

  @Test
  void roundTripsWrappedContextWithDescription() {
    PSContextSummary body = new PSContextSummary();
    body.setName("Publish copy");
    body.setDescription("Public site");

    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("\"context\""), json);
    PSContextSummary round = mapper.readValue(json, PSContextSummary.class);
    assertEquals("Publish copy", round.getName());
    assertEquals("Public site", round.getDescription());
    assertNull(round.getContextId());
    assertNull(round.getDefaultSchemeId());
  }

  @Test
  void wrappedCopyBodyBindsDescriptionWithoutScheme() {
    String json =
        """
        {"context":{"name":"Publish copy","description":"Public site"}}
        """;
    PSContextSummary body = mapper.readValue(json, PSContextSummary.class);
    assertEquals("Publish copy", body.getName());
    assertEquals("Public site", body.getDescription());
    assertNull(body.getDefaultSchemeId());
    assertNull(body.getContextId());
  }

  @Test
  void flatBodyDoesNotBindName() {
    String flat = "{\"name\":\"Publish copy\",\"description\":\"Public site\"}";
    try {
      PSContextSummary body = mapper.readValue(flat, PSContextSummary.class);
      assertTrue(
          body == null || body.getName() == null || body.getName().isBlank(),
          "flat name root must not bind under UNWRAP_ROOT_VALUE; got name="
              + (body == null ? "null" : body.getName()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null && expected.getMessage().contains("context"),
          "unexpected failure: " + expected);
    }
  }
}
