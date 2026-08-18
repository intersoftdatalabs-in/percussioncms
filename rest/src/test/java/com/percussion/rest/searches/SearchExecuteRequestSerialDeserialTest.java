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

package com.percussion.rest.searches;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link SearchExecuteRequest} under WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE.
 *
 * <p>SPA execute (#3517) sends {@code {"SearchExecuteRequest":{…}}}. Flat {@code startIndex} /
 * {@code folderPath} is {@link SearchExecuteRequestJsonReader}, not this mapper.
 */
@Tag("UnitTest")
public class SearchExecuteRequestSerialDeserialTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(SearchExecuteRequest.class);

  @Test
  public void serializesUnderSearchExecuteRequestRoot() {
    SearchExecuteRequest req = new SearchExecuteRequest();
    req.setStartIndex(1);
    req.setMaxResults(50);
    String json = mapper.writeValueAsString(req);
    assertTrue(json.contains("\"SearchExecuteRequest\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"startIndex\""), json);
  }

  @Test
  public void deserializesWrappedBody() {
    String json =
        "{\"SearchExecuteRequest\":{\"startIndex\":4,\"maxResults\":25,\"folderPath\":\"//Sites\"}}";
    SearchExecuteRequest req = mapper.readValue(json, SearchExecuteRequest.class);
    assertEquals(4, req.getStartIndex());
    assertEquals(25, req.getMaxResults());
    assertEquals("//Sites", req.getFolderPath());
  }
}
