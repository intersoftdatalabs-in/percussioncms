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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Live SPA envelope for POST /actions/find/types (#3855). */
@Tag("UnitTest")
public class AllowedContentTypeMenusRequestJsonReaderTest {

  private final AllowedContentTypeMenusRequestJsonReader reader =
      new AllowedContentTypeMenusRequestJsonReader();

  @Test
  public void parseAcceptsWrappedEnvelope() {
    AllowedContentTypeMenusRequest out =
        AllowedContentTypeMenusRequestJsonReader.parse(
            "{\"AllowedContentTypeMenusRequest\":{\"contentIds\":[551,552]}}");
    assertArrayEquals(new int[] {551, 552}, out.getContentIds());
  }

  @Test
  public void parseAcceptsFlatSpaBody() {
    AllowedContentTypeMenusRequest out =
        AllowedContentTypeMenusRequestJsonReader.parse("{\"contentIds\":[551]}");
    assertArrayEquals(new int[] {551}, out.getContentIds());
  }

  @Test
  public void parseAcceptsGuidLastSegment() {
    AllowedContentTypeMenusRequest out =
        AllowedContentTypeMenusRequestJsonReader.parse(
            "{\"contentIds\":[\"16777215-101-551\"]}");
    assertArrayEquals(new int[] {551}, out.getContentIds());
  }

  @Test
  public void parseAcceptsJacksonGuidObject() {
    AllowedContentTypeMenusRequest out =
        AllowedContentTypeMenusRequestJsonReader.parse(
            "{\"contentIds\":[{\"stringValue\":\"16777215-101-551\"}]}");
    assertArrayEquals(new int[] {551}, out.getContentIds());
  }

  @Test
  public void parseAcceptsCamelCaseRootAlias() {
    AllowedContentTypeMenusRequest out =
        AllowedContentTypeMenusRequestJsonReader.parse(
            "{\"allowedContentTypeMenusRequest\":{\"contentIds\":[42]}}");
    assertArrayEquals(new int[] {42}, out.getContentIds());
  }

  @Test
  public void parseEmptyIsEmptyRequest() {
    AllowedContentTypeMenusRequest empty = AllowedContentTypeMenusRequestJsonReader.parse("  ");
    assertTrue(empty.getContentIds() == null || empty.getContentIds().length == 0);
    assertTrue(
        AllowedContentTypeMenusRequestJsonReader.parse(null).getContentIds() == null
            || AllowedContentTypeMenusRequestJsonReader.parse(null).getContentIds().length == 0);
  }

  @Test
  public void parseRejectsNonObject() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> AllowedContentTypeMenusRequestJsonReader.parse("[1,2]"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void parseRejectsInvalidJson() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> AllowedContentTypeMenusRequestJsonReader.parse("{"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void parseContentIdStringCoercesGuidAndInt() {
    assertEquals(551, AllowedContentTypeMenusRequestJsonReader.parseContentIdString("551"));
    assertEquals(
        551, AllowedContentTypeMenusRequestJsonReader.parseContentIdString("16777215-101-551"));
    assertEquals(0, AllowedContentTypeMenusRequestJsonReader.parseContentIdString("nope"));
    assertEquals(0, AllowedContentTypeMenusRequestJsonReader.parseContentIdString("0"));
  }

  @Test
  public void isReadableOnlyForAllowedContentTypeMenusRequest() {
    assertTrue(
        reader.isReadable(
            AllowedContentTypeMenusRequest.class,
            AllowedContentTypeMenusRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE));
    assertTrue(
        !reader.isReadable(ActionMenu.class, ActionMenu.class, null, MediaType.APPLICATION_JSON_TYPE));
  }

  @Test
  public void readFromReadsUtf8Stream() throws Exception {
    byte[] raw = "{\"contentIds\":[551]}".getBytes(StandardCharsets.UTF_8);
    AllowedContentTypeMenusRequest out =
        reader.readFrom(
            AllowedContentTypeMenusRequest.class,
            AllowedContentTypeMenusRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(raw));
    assertArrayEquals(new int[] {551}, out.getContentIds());
  }

  @Test
  public void readFromEmptyStreamIsEmptyRequest() throws Exception {
    AllowedContentTypeMenusRequest out =
        reader.readFrom(
            AllowedContentTypeMenusRequest.class,
            AllowedContentTypeMenusRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(new byte[0]));
    assertTrue(out.getContentIds() == null || out.getContentIds().length == 0);
  }
}
