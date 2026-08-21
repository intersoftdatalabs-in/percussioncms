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

/**
 * Registered Virtual Site adapter kinds. {@link #GIT_FILESYSTEM} and {@link #CSV_FILESYSTEM} are
 * both wired through {@link PSVirtualSiteSourceFactory} for CMS REST Build.
 */
public enum VirtualSiteSourceType {
  GIT_FILESYSTEM("git-filesystem"),
  /** Local CSV / directory of CSVs (stable {@code id} column; Markdown in {@code body}). */
  CSV_FILESYSTEM("csv-filesystem");

  private final String wireName;

  VirtualSiteSourceType(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  /**
   * Parse a property / config wire name.
   *
   * @param value may be null
   * @return matching type or null if unknown/blank
   */
  public static VirtualSiteSourceType fromWireName(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String v = value.trim();
    for (VirtualSiteSourceType t : values()) {
      if (t.wireName.equalsIgnoreCase(v)) {
        return t;
      }
    }
    return null;
  }
}
