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
package com.percussion.services.virtualsite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Parses Markdown files with optional YAML frontmatter between {@code ---} fences.
 *
 * <p>Pure helper — no Spring.
 */
public final class VirtualFrontmatterParser {

  private VirtualFrontmatterParser() {}

  /**
   * Parse raw file text into frontmatter + body.
   *
   * @param text full file content, never null
   * @param defaultVersion version inherited from folder when frontmatter omits {@code version}
   * @param sourceLabel path label for error messages
   * @return parse result
   * @throws VirtualSiteException if required fields missing or YAML invalid
   */
  public static Parsed parse(String text, String defaultVersion, String sourceLabel)
      throws VirtualSiteException {
    if (text == null) {
      throw new VirtualSiteException("Page text is null: " + sourceLabel);
    }
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    if (!normalized.startsWith("---\n") && !normalized.equals("---")) {
      throw new VirtualSiteException(
          "Missing YAML frontmatter (expected leading ---) in " + sourceLabel);
    }
    int end = normalized.indexOf("\n---\n", 4);
    String yamlBlock;
    String body;
    if (end < 0) {
      // frontmatter only or closing --- at EOF
      int endEof = normalized.indexOf("\n---", 4);
      if (endEof < 0) {
        throw new VirtualSiteException("Unclosed frontmatter in " + sourceLabel);
      }
      yamlBlock = normalized.substring(4, endEof);
      body = normalized.substring(endEof + 4).replaceFirst("^\n", "");
    } else {
      yamlBlock = normalized.substring(4, end);
      body = normalized.substring(end + 5);
    }

    Map<String, Object> map = loadYamlMap(yamlBlock, sourceLabel);
    String id = stringVal(map.get("id"));
    String title = stringVal(map.get("title"));
    if (id == null || id.isBlank()) {
      throw new VirtualSiteException("Frontmatter 'id' is required in " + sourceLabel);
    }
    if (title == null || title.isBlank()) {
      throw new VirtualSiteException("Frontmatter 'title' is required in " + sourceLabel);
    }
    String description = stringVal(map.get("description"));
    String version = stringVal(map.get("version"));
    if (version == null || version.isBlank()) {
      version = defaultVersion;
    }
    boolean sidebar = booleanVal(map.get("sidebar"), true);
    int order = intVal(map.get("order"), 0);
    List<String> tags = stringList(map.get("tags"));
    boolean deprecated = booleanVal(map.get("deprecated"), false);

    VirtualFrontmatter fm =
        new VirtualFrontmatter(
            id.trim(), title.trim(), description, version, sidebar, order, tags, deprecated);
    return new Parsed(fm, body != null ? body : "");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> loadYamlMap(String yamlBlock, String sourceLabel)
      throws VirtualSiteException {
    try {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      Object loaded = yaml.load(yamlBlock);
      if (loaded == null) {
        return new LinkedHashMap<>();
      }
      if (!(loaded instanceof Map)) {
        throw new VirtualSiteException("Frontmatter must be a YAML mapping in " + sourceLabel);
      }
      return (Map<String, Object>) loaded;
    } catch (VirtualSiteException e) {
      throw e;
    } catch (Exception e) {
      throw new VirtualSiteException("Invalid frontmatter YAML in " + sourceLabel, e);
    }
  }

  private static String stringVal(Object o) {
    return o == null ? null : String.valueOf(o).trim();
  }

  private static boolean booleanVal(Object o, boolean defaultValue) {
    if (o == null) {
      return defaultValue;
    }
    if (o instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(String.valueOf(o));
  }

  private static int intVal(Object o, int defaultValue) {
    if (o == null) {
      return defaultValue;
    }
    if (o instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(o).trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static List<String> stringList(Object o) {
    if (o == null) {
      return List.of();
    }
    if (o instanceof List<?> list) {
      List<String> out = new ArrayList<>();
      for (Object item : list) {
        if (item != null) {
          out.add(String.valueOf(item));
        }
      }
      return out;
    }
    return List.of(String.valueOf(o));
  }

  /** Frontmatter + Markdown body after fences. */
  public static final class Parsed {
    private final VirtualFrontmatter frontmatter;
    private final String body;

    public Parsed(VirtualFrontmatter frontmatter, String body) {
      this.frontmatter = frontmatter;
      this.body = body;
    }

    public VirtualFrontmatter frontmatter() {
      return frontmatter;
    }

    public String body() {
      return body;
    }
  }
}
