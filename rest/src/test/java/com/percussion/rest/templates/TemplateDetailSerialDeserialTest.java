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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.contenttypes.NamedObjectRef;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for TemplateDetail under WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE (#3039).
 *
 * <p>SPA clients must unwrap {@code {"TemplateDetail":{…}}} or source binds empty.
 */
@Tag("UnitTest")
public class TemplateDetailSerialDeserialTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(TemplateDetail.class);

  @Test
  public void serializesTemplateSourceUnderRoot() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    d.setLabel("Page");
    d.setTemplateSource("#header()\n$body\n");

    String json = mapper.writeValueAsString(d);

    assertTrue(json.contains("\"TemplateDetail\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"templateSource\""), json);
    assertTrue(json.contains("#header()"), json);
  }

  @Test
  public void deserializesWrappedBodyWithTemplateSource() {
    String json =
        "{\"TemplateDetail\":{"
            + "\"name\":\"site.base\","
            + "\"templateSource\":\"#footer()\\n\""
            + "}}";

    TemplateDetail d = mapper.readValue(json, TemplateDetail.class);
    assertEquals("site.base", d.getName());
    assertEquals("#footer()\n", d.getTemplateSource());
  }

  @Test
  public void roundTripPreservesEmptySource() {
    TemplateDetail d = new TemplateDetail();
    d.setName("empty.tpl");
    d.setTemplateSource("");

    String json = mapper.writeValueAsString(d);
    TemplateDetail back = mapper.readValue(json, TemplateDetail.class);
    assertEquals("empty.tpl", back.getName());
    // empty string is not null; NON_NULL still emits it
    assertEquals("", back.getTemplateSource());
  }

  @Test
  public void omitAssociatedContentTypesStaysNullOnDeserialize() {
    String json = "{\"TemplateDetail\":{\"name\":\"perc.page\"}}";
    TemplateDetail d = mapper.readValue(json, TemplateDetail.class);
    assertNull(d.getAssociatedContentTypes());
  }

  @Test
  public void spaShapedPutWithEmptyAssociatedContentTypesBindsEmptyListNotNull() {
    String json =
        "{\"TemplateDetail\":{\"label\":\"Page\",\"description\":\"d\",\"templateSource\":\"#x\","
            + "\"associatedContentTypes\":[]}}";
    TemplateDetail d = mapper.readValue(json, TemplateDetail.class);
    assertEquals(0, d.getAssociatedContentTypes().size());
  }

  @Test
  public void emptyAssociatedContentTypesRoundTrips() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    d.setAssociatedContentTypes(List.of());
    String json = mapper.writeValueAsString(d);
    assertTrue(json.contains("associatedContentTypes"), json);
    TemplateDetail back = mapper.readValue(json, TemplateDetail.class);
    assertEquals(0, back.getAssociatedContentTypes().size());
  }

  @Test
  public void associatedContentTypesNameAndGuidRoundTrip() {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("percPage");
    ref.setLabel("Page");
    com.percussion.rest.Guid g = new com.percussion.rest.Guid();
    g.setStringValue("0-6-311");
    g.setUuid(311);
    ref.setGuid(g);
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    d.setAssociatedContentTypes(List.of(ref));
    String json = mapper.writeValueAsString(d);
    assertTrue(json.contains("percPage"), json);
    TemplateDetail back = mapper.readValue(json, TemplateDetail.class);
    assertEquals(1, back.getAssociatedContentTypes().size());
    assertEquals("percPage", back.getAssociatedContentTypes().get(0).getName());
    assertEquals(311, back.getAssociatedContentTypes().get(0).getGuid().getUuid());
  }
}
