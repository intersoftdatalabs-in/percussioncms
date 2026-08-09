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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Substitutes {@code ${path}} placeholders in template source using assembly
 * binding maps (JEXL binding results). Used by HTML-first and Markdown
 * assemblers — deliberately smaller than Velocity (no directives/macros).
 *
 * <p>Path lookup rules:
 *
 * <ul>
 *   <li>{@code ${title}} looks up {@code title}, then {@code $title}
 *   <li>{@code ${sys.mimetype}} walks nested maps ({@code sys}/{@code $sys} then
 *       {@code mimetype})
 *   <li>Missing paths become empty string
 *   <li>JCR {@link Property} / {@link Value} are stringified when possible
 * </ul>
 *
 * @see PSHtmlAssembler
 * @see PSMarkdownAssembler
 */
public final class PSBindingPlaceholderRenderer {

  /** Matches {@code ${dotted.path}} only — no bare {@code $name} (avoids HTML/JS false positives). */
  private static final Pattern PLACEHOLDER =
      Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)}");

  private PSBindingPlaceholderRenderer() {}

  /**
   * Replace all {@code ${...}} placeholders in {@code source} using {@code bindings}.
   *
   * @param source template body, may be {@code null} (treated as empty)
   * @param bindings assembly bindings map, may be {@code null}
   * @return rendered text, never {@code null}
   */
  public static String render(String source, Map<String, ?> bindings) {
    if (source == null || source.isEmpty()) {
      return "";
    }
    // Always walk placeholders — empty/missing bindings resolve to ""
    Map<String, ?> map = bindings != null ? bindings : Map.of();
    Matcher m = PLACEHOLDER.matcher(source);
    StringBuilder out = new StringBuilder(source.length() + 32);
    while (m.find()) {
      String path = m.group(1);
      String replacement = Objects.toString(resolve(map, path), "");
      // quoteReplacement handles $ and \ in values
      m.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(out);
    return out.toString();
  }

  /**
   * Resolve a dotted path against the bindings map.
   *
   * @param bindings root bindings
   * @param path dotted path without {@code ${}}
   * @return resolved value or {@code null}
   */
  static Object resolve(Map<String, ?> bindings, String path) {
    if (path == null || path.isBlank() || bindings == null) {
      return null;
    }
    String[] parts = path.split("\\.");
    Object current = lookupKey(bindings, parts[0]);
    for (int i = 1; i < parts.length && current != null; i++) {
      if (current instanceof Map<?, ?> map) {
        current = lookupKey(map, parts[i]);
      } else {
        return null;
      }
    }
    return stringifyIfNeeded(current);
  }

  private static Object lookupKey(Map<?, ?> map, String name) {
    if (map.containsKey(name)) {
      return map.get(name);
    }
    String dollar = "$" + name;
    if (map.containsKey(dollar)) {
      return map.get(dollar);
    }
    return null;
  }

  private static Object stringifyIfNeeded(Object value) {
    if (value == null) {
      return null;
    }
    try {
      if (value instanceof Property prop) {
        return prop.getString();
      }
      if (value instanceof Value jcrValue) {
        return jcrValue.getString();
      }
    } catch (RepositoryException e) {
      return "";
    }
    return value;
  }
}
