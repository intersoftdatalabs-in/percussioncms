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

package com.percussion.rest.displayformat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** #4098: empty allowedCommunities array is all communities, not omit. */
class DisplayFormatJsonReaderTest {

  @Test
  void parse_emptyAllowedCommunitiesArrayIsEmptyListNotNull() {
    DisplayFormat df =
        DisplayFormatJsonReader.parse(
            "{\"DisplayFormat\":{\"name\":\"MyFmt\",\"allowedCommunities\":[]}}");
    assertEquals("MyFmt", df.getName());
    assertNotNull(df.getAllowedCommunities());
    assertTrue(df.getAllowedCommunities().isEmpty());
  }

  @Test
  void parse_omittedAllowedCommunitiesStaysNull() {
    DisplayFormat df = DisplayFormatJsonReader.parse("{\"DisplayFormat\":{\"name\":\"MyFmt\"}}");
    assertEquals("MyFmt", df.getName());
    assertNull(df.getAllowedCommunities());
  }

  @Test
  void parse_nullAllowedCommunitiesStaysNull() {
    DisplayFormat df =
        DisplayFormatJsonReader.parse(
            "{\"DisplayFormat\":{\"name\":\"MyFmt\",\"allowedCommunities\":null}}");
    assertEquals("MyFmt", df.getName());
    assertNull(df.getAllowedCommunities());
  }

  @Test
  void parse_singleCommunityArray() {
    DisplayFormat df =
        DisplayFormatJsonReader.parse(
            "{\"DisplayFormat\":{\"name\":\"MyFmt\",\"allowedCommunities\":[{\"guid\":\"0-13-10\",\"name\":\"Default\"}]}}");
    assertEquals(1, df.getAllowedCommunities().size());
    assertEquals("Default", df.getAllowedCommunities().get(0).getName());
    assertEquals("0-13-10", df.getAllowedCommunities().get(0).getGuid());
  }

  @Test
  void parse_flatBody() {
    DisplayFormat df =
        DisplayFormatJsonReader.parse(
            "{\"name\":\"MyFmt\",\"allowedCommunities\":[{\"guid\":\"Default\",\"name\":\"Default\"}]}");
    assertEquals("MyFmt", df.getName());
    assertEquals("Default", df.getAllowedCommunities().get(0).getName());
  }
}
