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

import java.util.Map;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Pure render helpers for HTML-first and Markdown assemblers (no Spring / assembly
 * service). Unit-tested without {@link PSAssemblerBase} static initialization.
 */
public final class PSTextAssemblerSupport {

  private static final Parser PARSER = Parser.builder().build();
  private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

  private PSTextAssemblerSupport() {}

  /**
   * Apply {@code ${path}} placeholders only.
   *
   * @param template source, may be null
   * @param bindings assembly bindings, may be null
   * @return rendered HTML/text body
   */
  public static String renderHtmlFirst(String template, Map<String, ?> bindings) {
    return PSBindingPlaceholderRenderer.render(template, bindings);
  }

  /**
   * Apply placeholders, then CommonMark → HTML.
   *
   * @param template Markdown source, may be null
   * @param bindings assembly bindings, may be null
   * @return HTML body
   */
  public static String renderMarkdown(String template, Map<String, ?> bindings) {
    String withPlaceholders = PSBindingPlaceholderRenderer.render(template, bindings);
    Node document = PARSER.parse(withPlaceholders);
    return RENDERER.render(document);
  }
}
