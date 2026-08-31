/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.displayformat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.Guid;
import com.percussion.rest.JacksonContextResolver;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Tag("UnitTest")
public class TestDisplayFormat {

  @Test
  public void testToAndFromJson() throws IOException {
    var f = new DisplayFormat();
    f.setDescription("DescriptionTest");
    f.setDisplayName("DisplayNameTest");
    f.setInternalName("InternalNameTest");

    var mapper = JsonMapper.builder().build();
    var json = mapper.writeValueAsString(f);

    var d2 = mapper.readValue(json, DisplayFormat.class);

    assertEquals("DescriptionTest", d2.getDescription());
    assertEquals("DisplayNameTest", d2.getDisplayName());
    assertEquals("InternalNameTest", d2.getInternalName());
  }

  /**
   * Developer SPA reads {@code detail.guid.stringValue} for Object ACL. REST Jackson mapper must
   * emit {@code stringValue} under the DisplayFormat root wrap (issue #2689 / #2951).
   */
  @Test
  public void jacksonContextResolver_serializesGuidStringValueUnderRootWrap() {
    DisplayFormat f = new DisplayFormat();
    f.setName("By_Author");
    f.setLabel("By Author");
    f.setGuid(new Guid("0-11-5"));

    ObjectMapper mapper = new JacksonContextResolver().getContext(DisplayFormat.class);
    String json = mapper.writeValueAsString(f);

    assertTrue(json.contains("\"DisplayFormat\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"stringValue\""), json);
    assertTrue(json.contains("0-11-5"), json);
    assertTrue(json.contains("By_Author"), json);
    // Nested Guid must not re-wrap under WRAP_ROOT_VALUE (would hide stringValue from SPA)
    assertTrue(
        json.contains("\"guid\":{") || json.contains("\"guid\" : {"),
        "guid should be a nested object, not re-wrapped: " + json);
    // #3200: stringValue must be a JSON string, not an Optional object
    assertTrue(
        json.contains("\"stringValue\":\"0-11-5\"") || json.contains("\"stringValue\" : \"0-11-5\""),
        "stringValue must serialize as a JSON string: " + json);
  }

  @Test
  public void copyForCreateOmitsIdentityAndDoesNotMutateSource() {
    DisplayFormat source = new DisplayFormat();
    source.setName("MyFmt");
    source.setInternalName("MyFmt");
    source.setLabel("Mine");
    source.setDisplayName("Mine");
    source.setDescription("desc");
    source.setDisplayId(9);
    source.setGuid(new Guid("0-11-9"));
    source.setGuidString("0-11-9");

    DisplayFormat copy = DisplayFormat.copyForCreate(source);

    assertEquals("MyFmt", copy.getName());
    assertEquals("MyFmt", copy.getInternalName());
    assertEquals("Mine", copy.getLabel());
    assertEquals("desc", copy.getDescription());
    assertEquals(0, copy.getDisplayId());
    assertNull(copy.getGuid());
    assertNull(copy.getGuidString());
    assertEquals(9, source.getDisplayId());
    assertEquals("0-11-9", source.getGuidString());
  }

  @Test
  public void equalsIgnoresUnsetGuidStringCompanion() {
    DisplayFormat withCompanion = new DisplayFormat();
    withCompanion.setName("By_Author");
    withCompanion.setDisplayId(5);
    withCompanion.setGuidString("0-31-5");
    DisplayFormat withoutCompanion = new DisplayFormat();
    withoutCompanion.setName("By_Author");
    withoutCompanion.setDisplayId(5);
    assertEquals(withCompanion, withoutCompanion);
    assertEquals(withCompanion.hashCode(), withoutCompanion.hashCode());
  }

  @Test
  public void jacksonContextResolver_serializesGuidStringCompanion() {
    DisplayFormat f = new DisplayFormat();
    f.setName("By_Author");
    f.setGuid(new Guid("0-31-5"));
    f.setGuidString("0-31-5");

    ObjectMapper mapper = new JacksonContextResolver().getContext(DisplayFormat.class);
    String json = mapper.writeValueAsString(f);

    assertTrue(
        json.contains("\"guidString\":\"0-31-5\"") || json.contains("\"guidString\" : \"0-31-5\""),
        json);
  }

  @Test
  public void jacksonContextResolver_serializesGuidPartsAndStringValue() {
    Guid g = new Guid();
    g.setHostId(0);
    g.setType((short) 11);
    g.setUuid(301);
    g.setLongValue(301L);
    g.setStringValue("0-11-301");

    DisplayFormat f = new DisplayFormat();
    f.setName("By_Author");
    f.setGuid(g);

    ObjectMapper mapper = new JacksonContextResolver().getContext(DisplayFormat.class);
    String json = mapper.writeValueAsString(f);

    assertTrue(json.contains("\"stringValue\""), json);
    assertTrue(json.contains("0-11-301"), json);
    assertTrue(json.contains("\"uuid\""), json);
  }
}
