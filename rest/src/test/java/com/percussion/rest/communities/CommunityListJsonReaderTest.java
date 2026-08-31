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
package com.percussion.rest.communities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class CommunityListJsonReaderTest {

  private static final String ENVELOPE =
      "{\"CommunityList\":[{\"name\":\"QA\",\"label\":\"QA\",\"id\":42,"
          + "\"guid\":{\"stringValue\":\"0-13-42\"}}]}";

  @Test
  public void parseAcceptsCommunityListEnvelope() {
    CommunityList list = CommunityListJsonReader.parse(ENVELOPE);
    assertInstanceOf(CommunityList.class, list);
    assertEquals(1, list.size());
    assertEquals("QA", list.get(0).getName());
    assertEquals("QA", list.get(0).getLabel());
  }

  @Test
  public void parseAcceptsBareArray() {
    CommunityList list = CommunityListJsonReader.parse("[{\"name\":\"QA2\"}]");
    assertEquals(1, list.size());
    assertEquals("QA2", list.get(0).getName());
  }

  @Test
  public void parseEmptyIsEmptyList() {
    assertTrue(CommunityListJsonReader.parse("  ").isEmpty());
    assertTrue(CommunityListJsonReader.parse(null).isEmpty());
  }

  @Test
  public void parseRejectsNonListObject() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> CommunityListJsonReader.parse("{\"foo\":1}"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void isReadableOnlyForCommunityList() {
    CommunityListJsonReader reader = new CommunityListJsonReader();
    assertTrue(reader.isReadable(CommunityList.class, CommunityList.class, null, null));
    assertTrue(
        !reader.isReadable(Community.class, Community.class, null, null));
  }
}
