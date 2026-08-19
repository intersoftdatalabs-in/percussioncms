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
package com.percussion.searchmanagement.service.impl;

import org.apache.commons.lang3.StringUtils;

/**
 * Folder-path scope for sitemanage free-text / extended search.
 *
 * <p>{@code getIdByPath("//")} throws {@code IllegalArgumentException} ("path must have at least
 * two token") because the tokenizer sees only separators. Explorer root {@code /} is often
 * serialized as {@code //} — that must be <em>unscoped</em> search (HTTP 200), not HTTP 500
 * (#3617 / parent #3102).
 */
final class PSSearchFolderScope {
  private PSSearchFolderScope() {}

  /**
   * @param folderPath raw criteria path ({@code /Sites}, {@code //Sites}, {@code /}, {@code //})
   * @return path to use as a folder filter, or {@code null} when the search is unscoped
   */
  static String forSearch(String folderPath) {
    if (folderPath == null) {
      return null;
    }
    String p = folderPath.trim();
    if (p.isEmpty()) {
      return null;
    }
    String stripped = p;
    while (stripped.startsWith("/")) {
      stripped = stripped.substring(1);
    }
    if (StringUtils.isBlank(stripped)) {
      return null;
    }
    return p;
  }
}
