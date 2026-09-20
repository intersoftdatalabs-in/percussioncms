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

package com.percussion.rest.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.rest.JacksonContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 21 WorkflowUpdate wire-shape coverage (Jackson root wrap + JAXB round-trip). */
@Tag("UnitTest")
public class WorkflowUpdateSerialDeserialTest {

  @Test
  public void updateBodyRoundTripWithRootWrap() throws Exception {
    WorkflowUpdate body = new WorkflowUpdate();
    body.setName("Simple Workflow");
    body.setDescription("Edited by surface spec");

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowUpdate.class);
    String json = mapper.writeValueAsString(body);
    assertNotNull(json);
    WorkflowUpdate round = mapper.readValue(json, WorkflowUpdate.class);
    assertEquals("Simple Workflow", round.getName());
    assertEquals("Edited by surface spec", round.getDescription());
  }

  @Test
  public void updateBodyRoundTripWithoutDescription() throws Exception {
    WorkflowUpdate body = new WorkflowUpdate();
    body.setName("Simple Workflow");

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowUpdate.class);
    String json = mapper.writeValueAsString(body);
    WorkflowUpdate round = mapper.readValue(json, WorkflowUpdate.class);
    assertEquals("Simple Workflow", round.getName());
    assertNull(round.getDescription());
  }

  @Test
  public void jaxbRoundTripPreservesFields() throws Exception {
    WorkflowUpdate body = new WorkflowUpdate();
    body.setName("Simple Workflow");
    body.setDescription("Edited");

    JAXBContext ctx = JAXBContext.newInstance(WorkflowUpdate.class);
    Marshaller marshaller = ctx.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(body, writer);
    String xml = writer.toString();

    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    WorkflowUpdate round = (WorkflowUpdate) unmarshaller.unmarshal(new StringReader(xml));
    assertEquals("Simple Workflow", round.getName());
    assertEquals("Edited", round.getDescription());
  }
}
