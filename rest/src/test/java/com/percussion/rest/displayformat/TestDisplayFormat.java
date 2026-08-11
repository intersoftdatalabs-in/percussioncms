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
