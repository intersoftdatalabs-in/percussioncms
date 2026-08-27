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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@Tag("UnitTest")
public class ContentTypeItemExitsJsonReaderTest {

  @Test
  public void parseWrappedEnvelopePopulatesRequiredLists() {
    ContentTypeItemExits out =
        ContentTypeItemExitsJsonReader.parse(
            "{\"ContentTypeItemExits\":{"
                + "\"inputTranslations\":[{\"extension\":\"Java/global/percussion/generic/sys_ToUpperCase\","
                + "\"parameters\":[{\"value\":\"sys_title\"}]}],"
                + "\"outputTranslations\":[],\"validations\":[],\"preExits\":[],\"postExits\":[],"
                + "\"maxErrorsToStopValidation\":10}}");
    assertNotNull(out.getInputTranslations());
    assertEquals(1, out.getInputTranslations().size());
    assertEquals(
        "Java/global/percussion/generic/sys_ToUpperCase",
        out.getInputTranslations().get(0).getExtension());
    assertNotNull(out.getOutputTranslations());
    assertTrue(out.getOutputTranslations().isEmpty());
    assertNotNull(out.getValidations());
    assertEquals(Integer.valueOf(10), out.getMaxErrorsToStopValidation());
  }

  @Test
  public void parseFlatBodyPopulatesRequiredLists() {
    ContentTypeItemExits out =
        ContentTypeItemExitsJsonReader.parse(
            "{\"inputTranslations\":[],\"outputTranslations\":[],\"validations\":[]}");
    assertNotNull(out.getInputTranslations());
    assertTrue(out.getInputTranslations().isEmpty());
    assertNotNull(out.getValidations());
  }

  @Test
  public void parseBlankReturnsEmptyEnvelope() {
    ContentTypeItemExits empty = ContentTypeItemExitsJsonReader.parse("  ");
    assertNotNull(empty);
  }

  @Test
  public void parseArrayIs400() {
    assertThrows(
        WebApplicationException.class, () -> ContentTypeItemExitsJsonReader.parse("[1,2]"));
  }

  @Test
  public void jacksonContextResolverWrapsFasterxmlJsonRootName() {
    ContentTypeItemExits body = new ContentTypeItemExits();
    body.setInputTranslations(List.of());
    body.setOutputTranslations(List.of());
    body.setValidations(List.of());
    body.setPreExits(List.of());
    body.setPostExits(List.of());
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeItemExits.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("\"ContentTypeItemExits\""), "expected WRAP_ROOT_VALUE: " + json);
  }
}
