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
package com.percussion.category.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.dom4j.DocumentHelper;
import org.junit.jupiter.api.Test;

/** Behavioral coverage for category XML conversion after removing redundant attribute casts. */
public class PSCategoryControlUtilsConvertTest {

  @Test
  public void convertToOldFormatXmlMapsTitleAndSelectable() {
    var source = DocumentHelper.createDocument();
    var root = source.addElement("Category").addAttribute("title", "RootCats");
    root.addElement("Children")
        .addAttribute("id", "c1")
        .addAttribute("title", "Child One")
        .addAttribute("selectable", "true");

    var converted = PSCategoryControlUtils.convertToOldFormatXml(source);
    assertNotNull(converted);
    assertEquals("Tree", converted.getRootElement().getName());
    assertEquals("RootCats", converted.getRootElement().attributeValue("label"));

    var node = converted.getRootElement().element("Node");
    assertNotNull(node);
    assertEquals("c1", node.attributeValue("id"));
    assertEquals("Child One", node.attributeValue("label"));
    assertEquals("yes", node.attributeValue("selectable"));
  }
}
