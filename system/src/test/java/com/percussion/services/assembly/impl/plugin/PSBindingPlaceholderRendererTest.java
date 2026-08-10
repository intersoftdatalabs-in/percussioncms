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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSBindingPlaceholderRenderer}. */
class PSBindingPlaceholderRendererTest {

  @Test
  void render_simplePlaceholder_dollarPrefixedBinding() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$title", "Hello");
    assertEquals("Hello world", PSBindingPlaceholderRenderer.render("${title} world", bindings));
  }

  @Test
  void render_nestedMapPath() {
    Map<String, Object> sys = new HashMap<>();
    sys.put("mimetype", "text/html");
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);
    assertEquals(
        "type=text/html",
        PSBindingPlaceholderRenderer.render("type=${sys.mimetype}", bindings));
  }

  @Test
  void render_missingPath_empty() {
    Map<String, Object> bindings = new HashMap<>();
    assertEquals("x", PSBindingPlaceholderRenderer.render("x${missing}", bindings));
    assertEquals("x", PSBindingPlaceholderRenderer.render("x${missing}", null));
  }

  @Test
  void render_nullSource_empty() {
    assertEquals("", PSBindingPlaceholderRenderer.render(null, Map.of()));
  }

  @Test
  void render_leavesBareDollarAlone() {
    // HTML-first must not treat bare $var as a placeholder
    assertEquals("$title", PSBindingPlaceholderRenderer.render("$title", Map.of("$title", "X")));
  }

  @Test
  void render_escapesReplacementDollars() {
    Map<String, Object> bindings = Map.of("price", "$5");
    assertEquals("cost $5", PSBindingPlaceholderRenderer.render("cost ${price}", bindings));
  }

  @Test
  void render_multiple() {
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("a", "1");
    bindings.put("b", "2");
    assertEquals("1-2", PSBindingPlaceholderRenderer.render("${a}-${b}", bindings));
  }

  @Test
  void resolve_withoutDollarOnMapKey() {
    Map<String, Object> bindings = Map.of("title", "T");
    assertEquals("T", PSBindingPlaceholderRenderer.resolve(bindings, "title"));
  }

  @Test
  void render_jcrPropertyReadFailure_rendersEmpty() throws Exception {
    javax.jcr.Property prop = org.mockito.Mockito.mock(javax.jcr.Property.class);
    org.mockito.Mockito.when(prop.getString())
        .thenThrow(new javax.jcr.RepositoryException("access denied"));
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("body", prop);
    // Present-but-unreadable JCR values fall back to empty (warn logged); must not throw.
    assertEquals("x", PSBindingPlaceholderRenderer.render("x${body}", bindings));
  }
}
