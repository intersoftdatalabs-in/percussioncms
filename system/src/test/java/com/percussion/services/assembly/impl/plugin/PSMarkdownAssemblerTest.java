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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pure render + assemble path for Markdown assembler (does not load {@link PSAssemblerBase} /
 * Spring). Covers the non-trivial branches of {@code assembleSingle} via {@link
 * PSTextAssemblerSupport#assembleMarkdown}.
 */
class PSMarkdownAssemblerTest {

  @Test
  void renderMarkdown_placeholdersThenHtml() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$title", "Greeting");
    String html =
        PSTextAssemblerSupport.renderMarkdown("# ${title}\n\nHello **world**", bindings);
    assertTrue(html.contains("<h1>Greeting</h1>"), html);
    assertTrue(html.contains("<strong>world</strong>"), html);
  }

  @Test
  void assembleMarkdown_successFromSysTemplate() {
    Map<String, Object> sys = new HashMap<>();
    sys.put("template", "# ${title}\n\nHello **world**");
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);
    bindings.put("$title", "Greeting");

    TextAssembleOutcome o = PSTextAssemblerSupport.assembleMarkdown(bindings, null);
    assertTrue(o.success());
    assertTrue(o.body().contains("<h1>Greeting</h1>"), o.body());
    assertTrue(o.body().contains("<strong>world</strong>"), o.body());
    assertTrue(o.contentType().startsWith("text/html"));
  }

  @Test
  void assembleMarkdown_fallsBackToTemplateSource() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$title", "T");
    TextAssembleOutcome o =
        PSTextAssemblerSupport.assembleMarkdown(bindings, "# ${title}");
    assertTrue(o.success());
    assertTrue(o.body().contains("<h1>T</h1>"), o.body());
  }

  @Test
  void assembleMarkdown_missingTemplateFails() {
    TextAssembleOutcome o = PSTextAssemblerSupport.assembleMarkdown(Map.of(), null);
    assertFalse(o.success());
    assertEquals("no Markdown template present", o.errorMessage());
  }
}
