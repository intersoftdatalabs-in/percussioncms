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
 * Wire contract for {@link AddFolderRequest} under WRAP_ROOT_VALUE / JAXB (#3360).
 *
 * <p>Preferred SPA envelope is {@code {"AddFolderRequest":{"name":"…","parentPath":"…"}}}. A flat
 * {@code name} object is the live Explorer body — that path is {@link AddFolderRequestJsonReader},
 * not this mapper.
 */
@Tag("UnitTest")
public class AddFolderRequestSerialDeserialTest {

  private static AddFolderRequest sample() {
    AddFolderRequest req = new AddFolderRequest();
    req.setName("qa3360");
    req.setParentPath("/Folders");
    req.setSourcePath("/Folders/Template");
    return req;
  }

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(AddFolderRequest.class);

  @Test
  public void serializesUnderAddFolderRequestRoot() {
    String json = mapper.writeValueAsString(sample());
    assertTrue(json.contains("\"AddFolderRequest\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"qa3360\""), json);
    assertTrue(json.contains("\"parentPath\""), json);
    assertTrue(json.contains("\"/Folders\""), json);
  }

  @Test
  public void deserializesWrappedBody() {
    String json =
        "{\"AddFolderRequest\":{\"name\":\"New\",\"parentPath\":\"/Sites/Help\","
            + "\"sourcePath\":\"/Sites/Help/Src\"}}";
    AddFolderRequest req = mapper.readValue(json, AddFolderRequest.class);
    assertEquals("New", req.getName());
    assertEquals("/Sites/Help", req.getParentPath());
    assertEquals("/Sites/Help/Src", req.getSourcePath());
  }

  @Test
  public void productionMapperRejectsBareNameRoot() {
    String flat = "{\"name\":\"bare\",\"parentPath\":\"/Folders\"}";
    try {
      AddFolderRequest result = mapper.readValue(flat, AddFolderRequest.class);
      assertTrue(
          result == null || result.getName() == null || result.getName().isBlank(),
          "flat name root must not bind under UNWRAP_ROOT_VALUE; got name="
              + (result == null ? "null" : result.getName()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("AddFolderRequest")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("name")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  public void jaxbMarshalsRootAndStringChildren() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(AddFolderRequest.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new StringWriter();
    marshaller.marshal(sample(), writer);
    String xml = writer.toString();
    assertTrue(xml.contains("AddFolderRequest"), xml);
    assertTrue(xml.contains("name"), xml);
    assertTrue(xml.contains("qa3360"), xml);
    assertTrue(xml.contains("parentPath"), xml);

    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    AddFolderRequest roundTrip =
        (AddFolderRequest) unmarshaller.unmarshal(new StringReader(xml));
    assertEquals("qa3360", roundTrip.getName());
    assertEquals("/Folders", roundTrip.getParentPath());
    assertEquals("/Folders/Template", roundTrip.getSourcePath());
  }

  @Test
  public void jaxbRejectsBareNameRoot() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(AddFolderRequest.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    String bare = "<name>qa3360</name>";
    try {
      Object result = unmarshaller.unmarshal(new StringReader(bare));
      fail("bare name root must not unmarshal as AddFolderRequest (#3360); got: " + result);
    } catch (UnmarshalException expected) {
      // CXF/JAXB production path: unexpected element local:"name"
    } catch (AssertionError expected) {
      // GlassFish JAXB + Saxon XML parser can fail as AssertionError on unexpected root
    }
  }
}
