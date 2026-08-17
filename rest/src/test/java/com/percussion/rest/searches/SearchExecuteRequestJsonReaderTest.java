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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SearchExecuteRequestJsonReaderTest {

  private final SearchExecuteRequestJsonReader reader = new SearchExecuteRequestJsonReader();

  @Test
  public void parseAcceptsWrappedEnvelope() {
    SearchExecuteRequest out =
        SearchExecuteRequestJsonReader.parse(
            "{\"SearchExecuteRequest\":{\"folderPath\":\"//Sites/Foo\","
                + "\"startIndex\":1,\"maxResults\":50,\"sortColumn\":\"title\","
                + "\"sortOrder\":\"desc\"}}");
    assertEquals("//Sites/Foo", out.getFolderPath());
    assertEquals(1, out.getStartIndex());
    assertEquals(50, out.getMaxResults());
    assertEquals("title", out.getSortColumn());
    assertEquals("desc", out.getSortOrder());
  }

  @Test
  public void parseAcceptsFlatStartIndexBody() {
    SearchExecuteRequest out =
        SearchExecuteRequestJsonReader.parse("{\"startIndex\":1,\"maxResults\":50}");
    assertEquals(1, out.getStartIndex());
    assertEquals(50, out.getMaxResults());
    assertNull(out.getFolderPath());
  }

  @Test
  public void parseAcceptsCamelCaseRootAlias() {
    SearchExecuteRequest out =
        SearchExecuteRequestJsonReader.parse(
            "{\"searchExecuteRequest\":{\"startIndex\":2,\"maxResults\":10}}");
    assertEquals(2, out.getStartIndex());
    assertEquals(10, out.getMaxResults());
  }

  @Test
  public void parseEmptyIsEmptyRequest() {
    SearchExecuteRequest empty = SearchExecuteRequestJsonReader.parse("  ");
    assertNull(empty.getStartIndex());
    assertNull(SearchExecuteRequestJsonReader.parse(null).getMaxResults());
  }

  @Test
  public void parseRejectsNonObject() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> SearchExecuteRequestJsonReader.parse("[1,2]"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void parseRejectsInvalidJson() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> SearchExecuteRequestJsonReader.parse("{"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void readFromReadsUtf8Stream() throws Exception {
    assertTrue(
        reader.isReadable(
            SearchExecuteRequest.class, null, null, MediaType.APPLICATION_JSON_TYPE));
    byte[] raw = "{\"startIndex\":3}".getBytes(StandardCharsets.UTF_8);
    SearchExecuteRequest out =
        reader.readFrom(
            SearchExecuteRequest.class,
            SearchExecuteRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(raw));
    assertEquals(3, out.getStartIndex());
  }

  @Test
  public void readFromEmptyStreamIsEmptyRequest() throws Exception {
    SearchExecuteRequest out =
        reader.readFrom(
            SearchExecuteRequest.class,
            SearchExecuteRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(new byte[0]));
    assertNull(out.getStartIndex());
  }
}
