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

/** Issue #5178 CommunityDescription wire-shape coverage (Jackson root wrap + JAXB round-trip). */
@Tag("UnitTest")
public class CommunityDescriptionSerialDeserialTest {

  @Test
  public void descriptionBodyRoundTripWithRootWrap() throws Exception {
    CommunityDescription body = new CommunityDescription();
    body.setDescription("Enterprise notes");

    ObjectMapper mapper = new JacksonContextResolver().getContext(CommunityDescription.class);
    String json = mapper.writeValueAsString(body);
    assertNotNull(json);
    assertTrue(json.contains("CommunityDescription"), json);
    CommunityDescription round = mapper.readValue(json, CommunityDescription.class);
    assertEquals("Enterprise notes", round.getDescription());
  }

  @Test
  public void emptyDescriptionRoundTripsAsClear() throws Exception {
    CommunityDescription body = new CommunityDescription();
    body.setDescription("");

    ObjectMapper mapper = new JacksonContextResolver().getContext(CommunityDescription.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("\"description\":\"\""), json);
    CommunityDescription round = mapper.readValue(json, CommunityDescription.class);
    assertEquals("", round.getDescription());
  }

  @Test
  public void jaxbRoundTripPreservesDescription() throws Exception {
    CommunityDescription body = new CommunityDescription();
    body.setDescription("Enterprise notes");

    JAXBContext ctx = JAXBContext.newInstance(CommunityDescription.class);
    Marshaller marshaller = ctx.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(body, writer);

    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    CommunityDescription round =
        (CommunityDescription) unmarshaller.unmarshal(new StringReader(writer.toString()));
    assertEquals("Enterprise notes", round.getDescription());
  }
}
