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
package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class GuidListJsonReaderTest {

  @Test
  public void parseAcceptsGuidListEnvelope() {
    GuidList list =
        GuidListJsonReader.parse("{\"GuidList\":[{\"stringValue\":\"0-13-42\"}]}");
    assertInstanceOf(GuidList.class, list);
    assertEquals(1, list.size());
    assertEquals("0-13-42", list.get(0).getStringValue());
  }

  @Test
  public void parseAcceptsBareArray() {
    GuidList list = GuidListJsonReader.parse("[{\"stringValue\":\"0-13-10\"}]");
    assertEquals(1, list.size());
    assertEquals("0-13-10", list.get(0).getStringValue());
  }

  @Test
  public void parseEmptyIsEmptyList() {
    assertTrue(GuidListJsonReader.parse("  ").isEmpty());
    assertTrue(GuidListJsonReader.parse(null).isEmpty());
  }

  @Test
  public void parseRejectsNonListObject() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> GuidListJsonReader.parse("{\"foo\":1}"));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
