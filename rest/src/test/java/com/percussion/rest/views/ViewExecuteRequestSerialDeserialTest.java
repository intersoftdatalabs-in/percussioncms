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

package com.percussion.rest.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link ViewExecuteRequest} under WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE.
 *
 * <p>SPA execute (#3318) sends {@code {"ViewExecuteRequest":{…}}}. Inbox (#3323) must not 400
 * when a client still posts a flat {@code startIndex} — that path is {@link
 * ViewExecuteRequestJsonReader}, not this mapper.
 */
@Tag("UnitTest")
public class ViewExecuteRequestSerialDeserialTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(ViewExecuteRequest.class);

  @Test
  public void serializesUnderViewExecuteRequestRoot() {
    ViewExecuteRequest req = new ViewExecuteRequest();
    req.setStartIndex(1);
    req.setMaxResults(50);
    String json = mapper.writeValueAsString(req);
    assertTrue(json.contains("\"ViewExecuteRequest\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"startIndex\""), json);
  }

  @Test
  public void deserializesWrappedBody() {
    String json = "{\"ViewExecuteRequest\":{\"startIndex\":4,\"maxResults\":25}}";
    ViewExecuteRequest req = mapper.readValue(json, ViewExecuteRequest.class);
    assertEquals(4, req.getStartIndex());
    assertEquals(25, req.getMaxResults());
  }
}
