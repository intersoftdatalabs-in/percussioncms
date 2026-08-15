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

package com.percussion.rest.contentexplorer.folders;

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

/** Live SPA envelope for POST /content-explorer/folders (#3360 / #3361). */
@Tag("UnitTest")
public class AddFolderRequestJsonReaderTest {

  private final AddFolderRequestJsonReader reader = new AddFolderRequestJsonReader();

  @Test
  public void parseAcceptsWrappedEnvelope() {
    AddFolderRequest out =
        AddFolderRequestJsonReader.parse(
            "{\"AddFolderRequest\":{\"name\":\"qa3360\",\"parentPath\":\"/Folders\","
                + "\"sourcePath\":\"/Folders/Template\"}}");
    assertEquals("qa3360", out.getName());
    assertEquals("/Folders", out.getParentPath());
    assertEquals("/Folders/Template", out.getSourcePath());
  }

  @Test
  public void parseAcceptsFlatSpaBody() {
    AddFolderRequest out =
        AddFolderRequestJsonReader.parse(
            "{\"name\":\"qa3360_flat\",\"parentPath\":\"/Folders\"}");
    assertEquals("qa3360_flat", out.getName());
    assertEquals("/Folders", out.getParentPath());
    assertNull(out.getSourcePath());
  }

  @Test
  public void parseAcceptsSitesParentPath() {
    AddFolderRequest out =
        AddFolderRequestJsonReader.parse(
            "{\"AddFolderRequest\":{\"name\":\"underSite\",\"parentPath\":\"/Sites/Help\"}}");
    assertEquals("underSite", out.getName());
    assertEquals("/Sites/Help", out.getParentPath());
  }

  @Test
  public void parseAcceptsCamelCaseRootAlias() {
    AddFolderRequest out =
        AddFolderRequestJsonReader.parse(
            "{\"addFolderRequest\":{\"name\":\"x\",\"parentPath\":\"//Folders\"}}");
    assertEquals("x", out.getName());
    assertEquals("//Folders", out.getParentPath());
  }

  @Test
  public void parseEmptyIsEmptyRequest() {
    AddFolderRequest empty = AddFolderRequestJsonReader.parse("  ");
    assertNull(empty.getName());
    assertNull(AddFolderRequestJsonReader.parse(null).getParentPath());
  }

  @Test
  public void parseRejectsNonObject() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> AddFolderRequestJsonReader.parse("[1,2]"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void parseRejectsInvalidJson() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> AddFolderRequestJsonReader.parse("{"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void isReadableOnlyForAddFolderRequest() {
    assertTrue(
        reader.isReadable(
            AddFolderRequest.class,
            AddFolderRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE));
    assertTrue(
        reader.isReadable(
            AddFolderRequest.class,
            AddFolderRequest.class,
            null,
            MediaType.valueOf("application/json; charset=UTF-8")));
    assertTrue(!reader.isReadable(RxFolder.class, RxFolder.class, null, null));
  }

  @Test
  public void readFromReadsUtf8Stream() throws Exception {
    byte[] raw =
        "{\"name\":\"fromStream\",\"parentPath\":\"/Sites\"}".getBytes(StandardCharsets.UTF_8);
    AddFolderRequest out =
        reader.readFrom(
            AddFolderRequest.class,
            AddFolderRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(raw));
    assertEquals("fromStream", out.getName());
    assertEquals("/Sites", out.getParentPath());
  }

  @Test
  public void readFromEmptyStreamIsEmptyRequest() throws Exception {
    AddFolderRequest out =
        reader.readFrom(
            AddFolderRequest.class,
            AddFolderRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(new byte[0]));
    assertNull(out.getName());
  }

  @Test
  public void parseNodeAcceptsEnvelope() {
    AddFolderRequest out =
        AddFolderRequestJsonReader.parseNode(
            tools.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(
                    "{\"AddFolderRequest\":{\"name\":\"n\",\"parentPath\":\"/Folders\"}}"));
    assertEquals("n", out.getName());
    assertEquals("/Folders", out.getParentPath());
  }
}
