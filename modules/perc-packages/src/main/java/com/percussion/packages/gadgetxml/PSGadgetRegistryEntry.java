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

package com.percussion.packages.gadgetxml;

import java.util.Objects;

/**
 * One gadget row from legacy {@code GadgetRegistry.xml} ({@code <gadget name baseuri file/>} inside
 * a named {@code <group>}).
 */
public final class PSGadgetRegistryEntry {

  private String name;
  private String group;
  private String baseUri;
  private String legacyDefinitionFile;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public String getLegacyDefinitionFile() {
    return legacyDefinitionFile;
  }

  public void setLegacyDefinitionFile(String legacyDefinitionFile) {
    this.legacyDefinitionFile = legacyDefinitionFile;
  }

  /**
   * Stable gadget id derived from the last segment of {@code baseUri} (classic OpenSocial folder
   * name), falling back to a slug of the display name.
   */
  public String gadgetId() {
    String fromUri = lastUriSegment(baseUri);
    if (fromUri != null && !fromUri.isBlank()) {
      return fromUri;
    }
    return slugify(name);
  }

  /**
   * Whether this entry is under a Deprecated (or similarly retired) group — product still ships the
   * row for layout prefs but palette defaults hide it.
   */
  public boolean isDeprecated() {
    if (group == null) {
      return false;
    }
    return "deprecated".equalsIgnoreCase(group.trim());
  }

  static String lastUriSegment(String baseUri) {
    if (baseUri == null || baseUri.isBlank()) {
      return null;
    }
    String p = baseUri.trim().replace('\\', '/');
    while (p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    int idx = p.lastIndexOf('/');
    return idx >= 0 ? p.substring(idx + 1) : p;
  }

  static String slugify(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return "gadget";
    }
    String s =
        displayName
            .trim()
            .toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
    return s.isEmpty() ? "gadget" : s;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSGadgetRegistryEntry that)) {
      return false;
    }
    return Objects.equals(name, that.name)
        && Objects.equals(group, that.group)
        && Objects.equals(baseUri, that.baseUri)
        && Objects.equals(legacyDefinitionFile, that.legacyDefinitionFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, group, baseUri, legacyDefinitionFile);
  }
}
