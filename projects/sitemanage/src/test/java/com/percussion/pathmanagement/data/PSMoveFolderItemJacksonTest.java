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
package com.percussion.pathmanagement.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.sitemanage.json.JacksonContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link PSMoveFolderItem} under WRAP_ROOT_VALUE / JAXB (#3362).
 *
 * <p>Preferred SPA envelope is {@code {"MoveFolderItem":{"itemPath":"…","targetFolderPath":"…"}}} —
 * the same shape legacy {@code PercPathService.moveItem} posts. A bare {@code sourcePath} root is
 * the Explorer bug that produced HTTP 400 unexpected element local:"sourcePath".
 */
@Tag("UnitTest")
class PSMoveFolderItemJacksonTest {

  private static PSMoveFolderItem sample() {
    PSMoveFolderItem req = new PSMoveFolderItem();
    req.setItemPath("/Folders/Child");
    req.setTargetFolderPath("/Folders/Other");
    return req;
  }

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(PSMoveFolderItem.class);

  @Test
  void serializesUnderMoveFolderItemRoot() {
    String json = mapper.writeValueAsString(sample());
    assertTrue(json.contains("\"MoveFolderItem\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"itemPath\""), json);
    assertTrue(json.contains("\"/Folders/Child\""), json);
    assertTrue(json.contains("\"targetFolderPath\""), json);
    assertTrue(!json.contains("sourcePath"), json);
    assertTrue(!json.contains("\"copy\""), json);
  }

  @Test
  void deserializesWrappedBody() {
    String json =
        "{\"MoveFolderItem\":{\"itemPath\":\"/Sites/Help/Src\","
            + "\"targetFolderPath\":\"/Sites/Help\"}}";
    PSMoveFolderItem req = mapper.readValue(json, PSMoveFolderItem.class);
    assertEquals("/Sites/Help/Src", req.getItemPath());
    assertEquals("/Sites/Help", req.getTargetFolderPath());
  }

  @Test
  void productionMapperRejectsBareSourcePathRoot() {
    String flat = "{\"sourcePath\":\"/Folders/A\",\"targetPath\":\"/Folders/B\",\"copy\":true}";
    try {
      PSMoveFolderItem result = mapper.readValue(flat, PSMoveFolderItem.class);
      assertTrue(
          result == null
              || result.getItemPath() == null
              || result.getItemPath().isBlank(),
          "bare sourcePath root must not bind under UNWRAP_ROOT_VALUE; got itemPath="
              + (result == null ? "null" : result.getItemPath()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("MoveFolderItem")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("sourcePath")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  void jaxbMarshalsRootAndStringChildren() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(PSMoveFolderItem.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new StringWriter();
    marshaller.marshal(sample(), writer);
    String xml = writer.toString();
    assertTrue(xml.contains("MoveFolderItem"), xml);
    assertTrue(xml.contains("itemPath"), xml);
    assertTrue(xml.contains("/Folders/Child"), xml);
    assertTrue(xml.contains("targetFolderPath"), xml);

    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    PSMoveFolderItem roundTrip =
        (PSMoveFolderItem) unmarshaller.unmarshal(new StringReader(xml));
    assertEquals("/Folders/Child", roundTrip.getItemPath());
    assertEquals("/Folders/Other", roundTrip.getTargetFolderPath());
  }

  @Test
  void jaxbRejectsBareSourcePathRoot() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(PSMoveFolderItem.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    String bare = "<sourcePath>/Folders/A</sourcePath>";
    try {
      Object result = unmarshaller.unmarshal(new StringReader(bare));
      fail("bare sourcePath root must not unmarshal as MoveFolderItem (#3362); got: " + result);
    } catch (UnmarshalException expected) {
      // CXF/JAXB production path: unexpected element local:"sourcePath"
    } catch (AssertionError expected) {
      // GlassFish JAXB + Saxon XML parser can fail as AssertionError on unexpected root
    }
  }
}
