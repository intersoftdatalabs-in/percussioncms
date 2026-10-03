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
 * Wire contract for edition content-list association under WRAP_ROOT_VALUE (#5107).
 *
 * <p>The SPA must post {@code {"editionContentList":{"contentListId":"…","deliveryContextId":"…"}}}
 * . A flat body does not bind under {@code UNWRAP_ROOT_VALUE}, so the design service treats the
 * request as missing ids (HTTP 400) even when the editor sent them.
 */
@Tag("UnitTest")
class PSEditionContentListAssocJacksonTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(PSEditionContentListAssoc.class);

  @Test
  void deserializesWrappedEditionContentList() {
    String json =
        "{\"editionContentList\":{\"contentListId\":\"5\",\"deliveryContextId\":\"9\"}}";
    PSEditionContentListAssoc body = mapper.readValue(json, PSEditionContentListAssoc.class);
    assertEquals("5", body.getContentListId());
    assertEquals("9", body.getDeliveryContextId());
  }

  @Test
  void flatBodyDoesNotBindIds() {
    String flat = "{\"contentListId\":\"5\",\"deliveryContextId\":\"9\"}";
    try {
      PSEditionContentListAssoc body = mapper.readValue(flat, PSEditionContentListAssoc.class);
      assertTrue(
          body == null || body.getContentListId() == null || body.getContentListId().isBlank(),
          "flat contentListId root must not bind under UNWRAP_ROOT_VALUE; got contentListId="
              + (body == null ? "null" : body.getContentListId()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("editionContentList")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("contentListId")),
          "unexpected failure: " + expected);
    }
  }
}
