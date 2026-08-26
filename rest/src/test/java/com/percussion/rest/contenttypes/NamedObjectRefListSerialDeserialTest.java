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

package com.percussion.rest.contenttypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@code GET}/{@code PUT .../allowedTemplates} {@link NamedObjectRefList}.
 *
 * <p>Live H2 returned JAXBException {@code NamedObjectRef nor any of its super class is known to
 * this context} when the list wrapper omitted {@code @XmlSeeAlso} and reused the item root name.
 * Peer: {@code SiteListSerialDeserialTest} (#3090).
 */
@Tag("UnitTest")
public class NamedObjectRefListSerialDeserialTest {

  private static NamedObjectRef sampleRef() {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    ref.setLabel("Page");
    return ref;
  }

  @Test
  public void productionMapperSerializesNamedObjectRefListEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(NamedObjectRefList.class);
    NamedObjectRefList list = new NamedObjectRefList();
    list.add(sampleRef());

    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("\"NamedObjectRefList\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("["), "list must be a JSON array, not a bean: " + json);
    assertTrue(json.contains("perc.page"), json);
    assertFalse(
        json.contains("\"empty\""),
        "ArrayList subclass must not serialize as {empty:false} bean: " + json);

    NamedObjectRefList roundTrip = mapper.readValue(json, NamedObjectRefList.class);
    assertEquals(1, roundTrip.size());
    assertEquals("perc.page", roundTrip.get(0).getName());
    assertEquals("Page", roundTrip.get(0).getLabel());
  }

  @Test
  public void productionMapperSerializesEmptyListEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(NamedObjectRefList.class);
    String json = mapper.writeValueAsString(new NamedObjectRefList());
    assertTrue(json.contains("NamedObjectRefList") || json.contains("["), json);
  }

  @Test
  public void jaxbContextKnowsNamedObjectRefFromList() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(NamedObjectRefList.class);
    Marshaller marshaller = ctx.createMarshaller();
    NamedObjectRefList list = new NamedObjectRefList();
    list.add(sampleRef());
    StringWriter writer = new StringWriter();
    marshaller.marshal(list, writer);
    String xml = writer.toString();
    assertFalse(xml.isBlank(), "marshalled XML must not be empty");
    assertTrue(
        xml.contains("NamedObjectRefList") || xml.contains("namedObjectRefList"),
        "expected NamedObjectRefList root in XML, got: " + xml);
    assertFalse(
        xml.toLowerCase().contains("known to this context"),
        "JAXB context must include NamedObjectRef: " + xml);
  }
}
