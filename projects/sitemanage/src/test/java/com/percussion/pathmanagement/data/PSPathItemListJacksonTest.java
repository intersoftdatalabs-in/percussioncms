/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression for #2989: path/folder Sites non-empty listing must use Jackson-friendly {@link
 * PSPathItemList} (JsonRootName PathItem, no JAXB XmlRootElement on ArrayList).
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
}
