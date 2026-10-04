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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.json.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for content-list copy under WRAP_ROOT_VALUE (#5159).
 *
 * <p>The SPA must post {@code {"copyContentListRequest":{"sourceContentListId":"…","newName":"…"}}}
 * . A flat body does not bind under {@code UNWRAP_ROOT_VALUE}, so the design service treats the
 * request as missing ids (HTTP 400) even when the form sent them.
 */
@Tag("UnitTest")
class PSCopyContentListRequestJacksonTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(PSCopyContentListRequest.class);

  @Test
  void deserializesWrappedCopyContentListRequest() {
    String json =
        "{\"copyContentListRequest\":{\"sourceContentListId\":\"5\",\"newName\":\"NightCl copy\"}}";
    PSCopyContentListRequest body = mapper.readValue(json, PSCopyContentListRequest.class);
    assertEquals("5", body.getSourceContentListId());
    assertEquals("NightCl copy", body.getNewName());
  }

  @Test
  void flatBodyDoesNotBindFields() {
    String flat = "{\"sourceContentListId\":\"5\",\"newName\":\"NightCl copy\"}";
    try {
      PSCopyContentListRequest body = mapper.readValue(flat, PSCopyContentListRequest.class);
      assertTrue(
          body == null
              || body.getSourceContentListId() == null
              || body.getSourceContentListId().isBlank(),
          "flat sourceContentListId root must not bind under UNWRAP_ROOT_VALUE; got sourceContentListId="
              + (body == null ? "null" : body.getSourceContentListId()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("copyContentListRequest")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("sourceContentListId")),
          "unexpected failure: " + expected);
    }
  }
}
