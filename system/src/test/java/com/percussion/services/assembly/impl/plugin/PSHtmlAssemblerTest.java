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
package com.percussion.services.assembly.impl.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.impl.plugin.PSTextAssemblerSupport.TextAssembleOutcome;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pure render + assemble path for HTML-first assembler (does not load {@link PSAssemblerBase} /
 * Spring). Covers the non-trivial branches of {@code assembleSingle} via {@link
 * PSTextAssemblerSupport#assembleHtmlFirst}.
 */
class PSHtmlAssemblerTest {

  @Test
  void renderHtmlFirst_placeholdersFromBindings() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$title", "Hello");
    assertEquals(
        "<p>Hello</p>",
        PSTextAssemblerSupport.renderHtmlFirst("<p>${title}</p>", bindings));
  }

  @Test
  void renderHtmlFirst_blankTemplate() {
    assertEquals("", PSTextAssemblerSupport.renderHtmlFirst("", Map.of()));
  }

  @Test
  void assembleHtmlFirst_successFromSysTemplate() {
    Map<String, Object> sys = new HashMap<>();
    sys.put("template", "<p>${title}</p>");
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);
    bindings.put("$title", "Hello");

    TextAssembleOutcome o = PSTextAssemblerSupport.assembleHtmlFirst(bindings, null);
    assertTrue(o.success());
    assertEquals("<p>Hello</p>", o.body());
    assertTrue(o.contentType().startsWith("text/html"));
    assertEquals(StandardCharsets.UTF_8, o.charset());
  }

  @Test
  void assembleHtmlFirst_fallsBackToTemplateSource() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$title", "X");
    TextAssembleOutcome o =
        PSTextAssemblerSupport.assembleHtmlFirst(bindings, "<span>${title}</span>");
    assertTrue(o.success());
    assertEquals("<span>X</span>", o.body());
  }

  @Test
  void assembleHtmlFirst_missingTemplateFails() {
    TextAssembleOutcome o = PSTextAssemblerSupport.assembleHtmlFirst(Map.of(), null);
    assertFalse(o.success());
    assertEquals("no HTML template present", o.errorMessage());
  }

  @Test
  void assembleHtmlFirst_respectsCharsetBinding() {
    Map<String, Object> sys = new HashMap<>();
    sys.put("template", "ok");
    sys.put("charset", "ISO-8859-1");
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);

    TextAssembleOutcome o = PSTextAssemblerSupport.assembleHtmlFirst(bindings, null);
    assertTrue(o.success());
    assertEquals("ISO-8859-1", o.charsetName());
    assertTrue(o.contentType().contains("ISO-8859-1"));
  }
}
