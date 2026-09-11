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

package com.percussion.rest.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** #4465: empty associatedContentTypes array is clear, not omit. */
class TemplateDetailJsonReaderTest {

  @Test
  void parse_emptyAssociatedContentTypesArrayIsEmptyListNotNull() {
    TemplateDetail d =
        TemplateDetailJsonReader.parse(
            "{\"TemplateDetail\":{\"label\":\"Page\",\"associatedContentTypes\":[]}}");
    assertEquals("Page", d.getLabel());
    assertNotNull(d.getAssociatedContentTypes());
    assertTrue(d.getAssociatedContentTypes().isEmpty());
  }

  @Test
  void parse_omittedAssociatedContentTypesStaysNull() {
    TemplateDetail d =
        TemplateDetailJsonReader.parse("{\"TemplateDetail\":{\"label\":\"Page\"}}");
    assertEquals("Page", d.getLabel());
    assertNull(d.getAssociatedContentTypes());
  }

  @Test
  void parse_nullAssociatedContentTypesStaysNull() {
    TemplateDetail d =
        TemplateDetailJsonReader.parse(
            "{\"TemplateDetail\":{\"label\":\"Page\",\"associatedContentTypes\":null}}");
    assertEquals("Page", d.getLabel());
    assertNull(d.getAssociatedContentTypes());
  }

  @Test
  void parse_singleRefArray() {
    TemplateDetail d =
        TemplateDetailJsonReader.parse(
            "{\"TemplateDetail\":{\"associatedContentTypes\":[{\"name\":\"percFileAsset\","
                + "\"guid\":{\"stringValue\":\"0-2-1\",\"uuid\":1}}]}}");
    assertEquals(1, d.getAssociatedContentTypes().size());
    assertEquals("percFileAsset", d.getAssociatedContentTypes().get(0).getName());
    assertEquals("0-2-1", d.getAssociatedContentTypes().get(0).getGuid().getStringValue());
  }

  @Test
  void parse_singleObjectNotArray() {
    TemplateDetail d =
        TemplateDetailJsonReader.parse(
            "{\"TemplateDetail\":{\"associatedContentTypes\":{\"name\":\"percImageAsset\"}}}");
    assertEquals(1, d.getAssociatedContentTypes().size());
    assertEquals("percImageAsset", d.getAssociatedContentTypes().get(0).getName());
  }
}
