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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@Tag("UnitTest")
public class ExtensionMethodsJacksonTest {

  private final ObjectMapper mapper = new JacksonContextResolver().getContext(Extension.class);

  @Test
  public void unwrapsMethodList() {
    String json =
        """
        {"Extension":{"extensionName":"my_user_ext","methods":[{"name":"productVersion","returnType":"java.lang.String","description":"ver","parameters":[{"name":"n","dataType":"int"}]}]}}
        """;
    Extension ext = mapper.readValue(json, Extension.class);
    assertNotNull(ext.getMethods());
    assertEquals(1, ext.getMethods().size());
    ExtensionMethod m = ext.getMethods().get(0);
    assertEquals("productVersion", m.getName());
    assertEquals("java.lang.String", m.getReturnType());
  }

  @Test
  public void serializesMethodListRoundTrip() {
    Extension ext = new Extension();
    ext.setExtensionName("my_user_ext");
    ExtensionMethod m = new ExtensionMethod();
    m.setName("productVersion");
    m.setReturnType("java.lang.String");
    ext.setMethods(java.util.List.of(m));
    String json = mapper.writeValueAsString(ext);
    Extension back = mapper.readValue(json, Extension.class);
    assertNotNull(back.getMethods(), json);
    assertEquals(1, back.getMethods().size(), json);
    assertEquals("java.lang.String", back.getMethods().get(0).getReturnType());
  }
}
