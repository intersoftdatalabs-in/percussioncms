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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.sitemanage.json.JacksonContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Regression for path/folder JSON (#2989, #3196): {@link PSPathItemList} uses Jackson {@code
 * JsonRootName PathItem} (no JAXB {@code XmlRootElement} on the ArrayList subclass), and {@link
 * PSPathItem} must be a legal JAXB type so CXF/Jackson JAXB introspector does not return HTTP 500
 * {@code IllegalAnnotationExceptions}.
 */
@Tag("UnitTest")
class PSPathItemListJacksonTest {

  @Test
  void pathItemListJacksonAnnotations() {
    JsonRootName root = PSPathItemList.class.getAnnotation(JsonRootName.class);
    assertNotNull(root);
    assertEquals("PathItem", root.value());
    assertNotNull(PSPathItemList.class.getAnnotation(JsonAutoDetect.class));
    assertNull(
        PSPathItemList.class.getAnnotation(XmlRootElement.class),
        "JAXB XmlRootElement on ArrayList subclass causes IllegalAnnotationExceptions");
  }

  @Test
  void pathItemListHoldsSiteChildren() {
    PSPathItem site = new PSPathItem();
    site.setName("Corporate_Investments");
    site.setId("Corporate_Investments");
    site.setType("site");
    site.setLeaf(false);
    PSPathItemList list = new PSPathItemList(List.of(site));
    assertEquals(1, list.size());
    assertTrue(new PSPathItemList().isEmpty());
  }

  @Test
  void jaxbContextAcceptsPathItemAndPathItemList() throws Exception {
    assertNotNull(JAXBContext.newInstance(PSPathItem.class, PSPathItemList.class));
  }

  @Test
  void serializesRepresentativeTreeWithSiteManageObjectMapper() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(PSPathItemList.class);
    PSPathItem sites = new PSPathItem();
    sites.setName("Sites");
    sites.setId("Sites");
    sites.setType("site");
    sites.setPath("/Sites/");
    sites.setFolderPath("//Sites");
    sites.setLeaf(false);
    sites.setHasFolderChildren(true);
    sites.setTypeProperty("siteId", "1");
    sites.getDisplayProperties().put("sys_title", "Sites");
    sites.setRelatedObject(new Object());

    String json = mapper.writeValueAsString(new PSPathItemList(List.of(sites)));
    assertTrue(json.contains("\"PathItem\""), json);
    assertTrue(json.contains("Sites"), json);
    assertFalse(json.contains("relatedObject"), json);
  }

  @Test
  void serializesSinglePathItemWithSiteManageObjectMapper() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(PSPathItem.class);
    PSPathItem item = new PSPathItem();
    item.setName("Corporate_Investments");
    item.setId("guid-1");
    item.setType("site");
    item.setPath("/Sites/Corporate_Investments/");
    item.setLeaf(false);
    item.setRelatedObject(new Object());
    String json = mapper.writeValueAsString(item);
    assertTrue(json.contains("\"PathItem\""), json);
    assertTrue(json.contains("Corporate_Investments"), json);
    assertFalse(json.contains("relatedObject"), json);
  }
}
