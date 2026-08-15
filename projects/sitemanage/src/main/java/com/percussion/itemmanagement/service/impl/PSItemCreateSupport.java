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
package com.percussion.itemmanagement.service.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;

/** Folder path and item name helpers for Explorer New Item. */
public final class PSItemCreateSupport {

  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

  private PSItemCreateSupport() {}

  /**
   * Repository folder path ({@code //Sites/...}). CMS paths always use {@code /}, not OS
   * separators.
   */
  public static String toRepositoryFolderPath(String raw) {
    if (StringUtils.isBlank(raw)) {
      return null;
    }
    String p = raw.trim().replace('\\', '/');
    p = p.replaceAll("/{2,}", "/");
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    return "/" + p;
  }

  public static String sanitizeItemName(String raw, String contentType) {
    String base = StringUtils.trimToEmpty(raw);
    if (base.isEmpty()) {
      String type = StringUtils.defaultIfBlank(contentType, "Item");
      type = type.replaceAll("[^A-Za-z0-9._-]", "-");
      base = "New-" + type + "-" + STAMP.format(Instant.now());
    }
    base = base.replace('\\', '/');
    int slash = base.lastIndexOf('/');
    if (slash >= 0) {
      base = base.substring(slash + 1);
    }
    base = base.replaceAll("[^A-Za-z0-9._-]", "-");
    base = base.replaceAll("-{2,}", "-");
    base = StringUtils.strip(base, "-");
    if (base.isEmpty()) {
      base = "New-Item";
    }
    if (isPageType(contentType) && !base.contains(".")) {
      base = base + ".html";
    }
    return base;
  }

  public static boolean isPageType(String contentType) {
    if (StringUtils.isBlank(contentType)) {
      return false;
    }
    String n = contentType.replaceAll("[\\s_-]", "").toLowerCase();
    return n.equals("percpage") || n.equals("page");
  }

  /**
   * Site name from a CMS folder ({@code /Sites/Demo/...} or {@code //Sites/Demo/...}).
   */
  public static String siteNameFromFolderPath(String folderPath) {
    if (StringUtils.isBlank(folderPath)) {
      return null;
    }
    String p = folderPath.replace('\\', '/');
    String[] parts = p.split("/");
    for (int i = 0; i < parts.length; i++) {
      if ("Sites".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
        String site = parts[i + 1].trim();
        return site.isEmpty() ? null : site;
      }
    }
    return null;
  }

  public static String titleFromItemName(String name) {
    String base = StringUtils.defaultIfBlank(name, "New Page");
    if (base.toLowerCase().endsWith(".html")) {
      return base.substring(0, base.length() - 5);
    }
    return base;
  }
}
