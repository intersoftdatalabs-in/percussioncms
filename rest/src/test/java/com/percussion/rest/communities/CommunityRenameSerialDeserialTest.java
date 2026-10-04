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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Issue #5177 CommunityRename wire-shape coverage (Jackson root wrap + JAXB round-trip). */
@Tag("UnitTest")
public class CommunityRenameSerialDeserialTest {

  @Test
  public void renameBodyRoundTripWithRootWrap() throws Exception {
    CommunityRename body = new CommunityRename();
    body.setName("Enterprise");

    ObjectMapper mapper = new JacksonContextResolver().getContext(CommunityRename.class);
    String json = mapper.writeValueAsString(body);
    assertNotNull(json);
    assertTrue(json.contains("CommunityRename"), json);
    CommunityRename round = mapper.readValue(json, CommunityRename.class);
    assertEquals("Enterprise", round.getName());
  }

  @Test
  public void jaxbRoundTripPreservesName() throws Exception {
    CommunityRename body = new CommunityRename();
    body.setName("Enterprise");

    JAXBContext ctx = JAXBContext.newInstance(CommunityRename.class);
    Marshaller marshaller = ctx.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(body, writer);

    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    CommunityRename round =
        (CommunityRename) unmarshaller.unmarshal(new StringReader(writer.toString()));
    assertEquals("Enterprise", round.getName());
  }
}
