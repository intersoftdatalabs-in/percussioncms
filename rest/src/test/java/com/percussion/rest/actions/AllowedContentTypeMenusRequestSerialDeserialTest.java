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

package com.percussion.rest.actions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link AllowedContentTypeMenusRequest} under WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE
 * (#3855). Flat SPA bodies bind via {@link AllowedContentTypeMenusRequestJsonReader}.
 */
@Tag("UnitTest")
public class AllowedContentTypeMenusRequestSerialDeserialTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(AllowedContentTypeMenusRequest.class);

  @Test
  public void serializesUnderAllowedContentTypeMenusRequestRoot() {
    AllowedContentTypeMenusRequest request = new AllowedContentTypeMenusRequest();
    request.setContentIds(new int[] {551});
    String json = mapper.writeValueAsString(request);
    assertTrue(
        json.contains("\"AllowedContentTypeMenusRequest\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"contentIds\""), json);
    assertTrue(json.contains("551"), json);
  }

  @Test
  public void deserializesWrappedBody() {
    String json = "{\"AllowedContentTypeMenusRequest\":{\"contentIds\":[551]}}";
    AllowedContentTypeMenusRequest req =
        mapper.readValue(json, AllowedContentTypeMenusRequest.class);
    assertArrayEquals(new int[] {551}, req.getContentIds());
  }

  @Test
  public void productionMapperRejectsBareContentIdsRoot() {
    String flat = "{\"contentIds\":[551]}";
    try {
      AllowedContentTypeMenusRequest result =
          mapper.readValue(flat, AllowedContentTypeMenusRequest.class);
      assertTrue(
          result == null
              || result.getContentIds() == null
              || result.getContentIds().length == 0,
          "flat contentIds root must not bind under UNWRAP_ROOT_VALUE; got "
              + (result == null ? "null" : java.util.Arrays.toString(result.getContentIds())));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("AllowedContentTypeMenusRequest")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("contentIds")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  public void productionMapperRejectsGuidStringArray() {
    String guid = "{\"AllowedContentTypeMenusRequest\":{\"contentIds\":[\"16777215-101-551\"]}}";
    try {
      AllowedContentTypeMenusRequest result =
          mapper.readValue(guid, AllowedContentTypeMenusRequest.class);
      fail(
          "GUID string array must not bind as int[] on the production mapper; got "
              + (result == null ? "null" : java.util.Arrays.toString(result.getContentIds())));
    } catch (Exception expected) {
      assertTrue(expected.getMessage() != null, "unexpected empty failure");
    }
  }
}
