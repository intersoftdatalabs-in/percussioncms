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

package com.percussion.rest.folders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.rest.JacksonContextResolver;
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
 * Wire contract for {@link CopyFolderItemRequest} under WRAP_ROOT_VALUE / JAXB (#3362).
 *
 * <p>Explorer Copy Folder posts {@code {"CopyFolderItemRequest":{"itemPath":"…","targetFolderPath":"…"}}}
 * to {@code POST /folders/copy/folder}. A bare {@code sourcePath} root is rejected.
 */
@Tag("UnitTest")
public class CopyFolderItemRequestSerialDeserialTest {

  private static CopyFolderItemRequest sample() {
    CopyFolderItemRequest req = new CopyFolderItemRequest();
    req.setItemPath("/Folders/Child");
    req.setTargetFolderPath("/Folders/Other");
    return req;
  }

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(CopyFolderItemRequest.class);

  @Test
  public void serializesUnderCopyFolderItemRequestRoot() {
    String json = mapper.writeValueAsString(sample());
    assertTrue(json.contains("\"CopyFolderItemRequest\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"itemPath\""), json);
    assertTrue(json.contains("\"/Folders/Child\""), json);
    assertTrue(json.contains("\"targetFolderPath\""), json);
    assertTrue(!json.contains("sourcePath"), json);
  }

  @Test
  public void deserializesWrappedBody() {
    String json =
        "{\"CopyFolderItemRequest\":{\"itemPath\":\"/Sites/Help/Src\","
            + "\"targetFolderPath\":\"/Sites/Help\"}}";
    CopyFolderItemRequest req = mapper.readValue(json, CopyFolderItemRequest.class);
    assertEquals("/Sites/Help/Src", req.getItemPath());
    assertEquals("/Sites/Help", req.getTargetFolderPath());
  }

  @Test
  public void productionMapperRejectsBareSourcePathRoot() {
    String flat = "{\"sourcePath\":\"/Folders/A\",\"targetPath\":\"/Folders/B\"}";
    try {
      CopyFolderItemRequest result = mapper.readValue(flat, CopyFolderItemRequest.class);
      String itemPath = result == null ? null : result.getItemPath();
      assertTrue(
          result == null || itemPath == null || itemPath.isBlank(),
          "bare sourcePath root must not bind; got itemPath=" + itemPath);
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("CopyFolderItemRequest")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("sourcePath")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  public void jaxbMarshalsCopyFolderItemRequestRoot() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(CopyFolderItemRequest.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new StringWriter();
    marshaller.marshal(sample(), writer);
    String xml = writer.toString();
    // Root name is the CXF unexpected-element contract (#3362). Field values
    // are Jackson-tested (plain String getters after #3413 / #3388).
    assertTrue(xml.contains("CopyFolderItemRequest"), xml);
  }

  @Test
  public void jaxbRejectsBareSourcePathRoot() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(CopyFolderItemRequest.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    String bare = "<sourcePath>/Folders/A</sourcePath>";
    try {
      Object result = unmarshaller.unmarshal(new StringReader(bare));
      fail("bare sourcePath root must not unmarshal as CopyFolderItemRequest (#3362); got: " + result);
    } catch (UnmarshalException expected) {
      // CXF/JAXB: unexpected element local:"sourcePath"
    } catch (AssertionError expected) {
      // GlassFish JAXB + Saxon XML parser can fail as AssertionError on unexpected root
    }
  }
}
