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
package com.percussion.publishingdesign.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.json.JacksonContextResolver;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for location-scheme create under WRAP_ROOT_VALUE (#5109).
 *
 * <p>The SPA must post {@code {"locationScheme":{...}}}. A flat {@code name} root does not bind,
 * so create fails before the scheme is saved.
 */
@Tag("UnitTest")
class PSLocationSchemeSummaryJacksonTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(PSLocationSchemeSummary.class);

  @Test
  void roundTripsWrappedSchemeWithPathParameter() {
    PSSchemeParameter path = new PSSchemeParameter();
    path.setName("path");
    path.setType("String");
    path.setValue("$sys.site.path");
    path.setSequence(0);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Article copy");
    body.setGenerator("Java/global/percussion/contentassembler/sys_JexlAssemblyLocation");
    body.setContextId("3");
    body.setParameters(List.of(path));

    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("\"locationScheme\""), json);
    PSLocationSchemeSummary round = mapper.readValue(json, PSLocationSchemeSummary.class);
    assertEquals("Article copy", round.getName());
    assertEquals(
        "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation",
        round.getGenerator());
    assertNotNull(round.getParameters());
    assertEquals(1, round.getParameters().size());
    assertEquals("path", round.getParameters().get(0).getName());
    assertEquals("$sys.site.path", round.getParameters().get(0).getValue());
  }

  @Test
  void schemeParameterArrayBindsPath() {
    String json =
        """
        {"locationScheme":{"name":"Article copy","generator":"gen","schemeParameter":[{"name":"path","type":"String","value":"$sys.site.path","sequence":0}]}}
        """;
    PSLocationSchemeSummary body = mapper.readValue(json, PSLocationSchemeSummary.class);
    assertNotNull(body.getParameters());
    assertEquals("path", body.getParameters().get(0).getName());
    assertEquals("$sys.site.path", body.getParameters().get(0).getValue());
  }

  @Test
  void parametersArrayDoesNotBindPath() {
    String json =
        """
        {"locationScheme":{"name":"Article copy","generator":"gen","parameters":[{"name":"path","type":"String","value":"$sys.site.path","sequence":0}]}}
        """;
    PSLocationSchemeSummary body = mapper.readValue(json, PSLocationSchemeSummary.class);
    assertTrue(body.getParameters() == null || body.getParameters().isEmpty());
  }

  @Test
  void addParameterFlagBindsOneSchemeParameter() {
    String json =
        """
        {"locationScheme":{"addParameter":true,"schemeParameter":[{"name":"suffix","type":"BackendColumn","value":"Contentstatus.contentid"}]}}
        """;
    PSLocationSchemeSummary body = mapper.readValue(json, PSLocationSchemeSummary.class);
    assertEquals(Boolean.TRUE, body.getAddParameter());
    assertTrue(body.getName() == null || body.getName().isBlank());
    assertNotNull(body.getParameters());
    assertEquals(1, body.getParameters().size());
    assertEquals("suffix", body.getParameters().get(0).getName());
    assertEquals("BackendColumn", body.getParameters().get(0).getType());
    assertEquals("Contentstatus.contentid", body.getParameters().get(0).getValue());
  }

  @Test
  void removeParameterFlagBindsOneSchemeParameterName() {
    String json =
        """
        {"locationScheme":{"removeParameter":true,"schemeParameter":[{"name":"suffix","type":"BackendColumn","value":"article"}]}}
        """;
    PSLocationSchemeSummary body = mapper.readValue(json, PSLocationSchemeSummary.class);
    assertEquals(Boolean.TRUE, body.getRemoveParameter());
    assertTrue(body.getAddParameter() == null || !body.getAddParameter());
    assertTrue(body.getName() == null || body.getName().isBlank());
    assertNotNull(body.getParameters());
    assertEquals(1, body.getParameters().size());
    assertEquals("suffix", body.getParameters().get(0).getName());
  }

  @Test
  void flatBodyDoesNotBindName() {
    String flat =
        "{\"name\":\"Article copy\",\"generator\":\"Java/global/percussion/contentassembler/sys_JexlAssemblyLocation\"}";
    try {
      PSLocationSchemeSummary body = mapper.readValue(flat, PSLocationSchemeSummary.class);
      assertTrue(
          body == null || body.getName() == null || body.getName().isBlank(),
          "flat name root must not bind under UNWRAP_ROOT_VALUE; got name="
              + (body == null ? "null" : body.getName()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null && expected.getMessage().contains("locationScheme"),
          "unexpected failure: " + expected);
    }
  }
}
