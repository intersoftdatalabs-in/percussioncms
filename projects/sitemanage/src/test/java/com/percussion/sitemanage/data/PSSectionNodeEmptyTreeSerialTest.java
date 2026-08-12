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
package com.percussion.sitemanage.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Empty NavTree payload must serialize as a SectionNode with empty children
 * (200 wire), not omit the list or throw (#3218).
 */
@Tag("UnitTest")
public class PSSectionNodeEmptyTreeSerialTest {

  @Test
  void emptyTreeRoundTripsWithEmptyChildren() {
    JsonMapper mapper =
        JsonMapper.builder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .build();

    PSSectionNode empty = PSSectionNode.emptyTree("BareSite", "//Sites/BareSite");
    String json = mapper.writeValueAsString(empty);

    assertTrue(json.contains("BareSite"), json);
    assertTrue(
        json.contains("childNodes") || json.contains("[]"),
        "empty tree must serialize children list, got: " + json);

    PSSectionNode roundTrip = mapper.readValue(json, PSSectionNode.class);
    assertNotNull(roundTrip);
    assertEquals("BareSite", roundTrip.getTitle());
    assertNotNull(roundTrip.getChildNodes());
    assertTrue(roundTrip.getChildNodes().isEmpty());
    assertEquals("//Sites/BareSite", roundTrip.getFolderPath().orElse(null));
  }
}
