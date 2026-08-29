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

package com.percussion.rest.contenttypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link ContentTypeSearchIndexing} under WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE.
 *
 * <p>Peer: {@link ContentTypeNameSerialDeserialTest}.
 */
@Tag("UnitTest")
public class ContentTypeSearchIndexingSerialDeserialTest {

  @Test
  public void productionMapperSerializesContentTypeSearchIndexingEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeSearchIndexing.class);
    String json = mapper.writeValueAsString(new ContentTypeSearchIndexing(false));
    assertTrue(json.contains("\"ContentTypeSearchIndexing\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"searchIndexing\""), json);
    assertTrue(json.contains("false"), json);

    ContentTypeSearchIndexing roundTrip = mapper.readValue(json, ContentTypeSearchIndexing.class);
    assertEquals(Boolean.FALSE, roundTrip.getSearchIndexing());
  }

  @Test
  public void productionMapperRoundTripsDefaultOn() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeSearchIndexing.class);
    String json = mapper.writeValueAsString(new ContentTypeSearchIndexing(true));
    ContentTypeSearchIndexing roundTrip = mapper.readValue(json, ContentTypeSearchIndexing.class);
    assertTrue(roundTrip.getSearchIndexing());
    assertFalse(json.contains("\"enabled\""), "must not collide with CD-13 enabled: " + json);
  }
}
