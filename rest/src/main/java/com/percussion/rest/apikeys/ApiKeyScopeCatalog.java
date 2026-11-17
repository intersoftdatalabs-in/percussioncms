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

package com.percussion.rest.apikeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Product-defined API key scope catalog and least-privilege presets (#6481).
 *
 * <p>The catalog lives in {@code rest} (not sitemanage) so the JAX-RS resource, the sitemanage
 * registry validation, and resource unit tests share one definition. Presets are ordered from
 * least to most privilege; {@link #matchPreset(List)} reports the preset whose scope set exactly
 * matches a key assignment (or {@code null} for custom combinations).
 */
public final class ApiKeyScopeCatalog {

  private static final List<ApiKeyScope> SCOPES;
  private static final List<ApiKeyScopePreset> PRESETS;
  private static final Map<String, ApiKeyScope> BY_KEY;

  static {
    List<ApiKeyScope> scopes = new ArrayList<>();
    scopes.add(
        new ApiKeyScope("content:read", "Read content", "Read content items and folders", "content"));
    scopes.add(
        new ApiKeyScope(
            "content:write", "Write content", "Create and update content items", "content"));
    scopes.add(
        new ApiKeyScope("content:delete", "Delete content", "Delete content items", "content"));
    scopes.add(new ApiKeyScope("asset:read", "Read assets", "Read binaries and images", "assets"));
    scopes.add(
        new ApiKeyScope("asset:write", "Write assets", "Upload and update binaries", "assets"));
    scopes.add(
        new ApiKeyScope(
            "template:read", "Read templates", "Read template and slot designs", "design"));
    scopes.add(
        new ApiKeyScope(
            "template:write", "Write templates", "Create and update template designs", "design"));
    scopes.add(
        new ApiKeyScope(
            "publish", "Publish", "Submit editions and publish sites", "publishing"));
    scopes.add(
        new ApiKeyScope(
            "publish:status",
            "Publishing status",
            "Read publishing history and queue status",
            "publishing"));
    scopes.add(new ApiKeyScope("site:read", "Read sites", "Read site and section trees", "sites"));
    scopes.add(
        new ApiKeyScope("site:write", "Write sites", "Create and update sites", "sites"));
    scopes.add(
        new ApiKeyScope("user:read", "Read users", "Read users and role membership", "admin"));
    scopes.add(
        new ApiKeyScope(
            "system:config",
            "System config",
            "Change server configuration (Admin only)",
            "admin"));
    SCOPES = Collections.unmodifiableList(scopes);

    Map<String, ApiKeyScope> byKey = new LinkedHashMap<>();
    for (ApiKeyScope scope : SCOPES) {
      byKey.put(scope.getKey(), scope);
    }
    BY_KEY = Collections.unmodifiableMap(byKey);

    List<ApiKeyScopePreset> presets = new ArrayList<>();
    presets.add(
        new ApiKeyScopePreset(
            "readonly",
            "Read-only",
            "Dashboards and status checks; no writes",
            List.of("content:read", "asset:read", "template:read", "site:read", "publish:status")));
    presets.add(
        new ApiKeyScopePreset(
            "author",
            "Content author",
            "Create and update content and assets; no publish",
            List.of(
                "content:read",
                "content:write",
                "asset:read",
                "asset:write",
                "template:read",
                "site:read",
                "publish:status")));
    presets.add(
        new ApiKeyScopePreset(
            "publisher",
            "Publisher",
            "Author plus edition publish",
            List.of(
                "content:read",
                "content:write",
                "asset:read",
                "asset:write",
                "template:read",
                "site:read",
                "publish",
                "publish:status")));
    presets.add(
        new ApiKeyScopePreset(
            "builder",
            "Site builder",
            "Publisher plus template and site design writes",
            List.of(
                "content:read",
                "content:write",
                "asset:read",
                "asset:write",
                "template:read",
                "template:write",
                "site:read",
                "site:write",
                "publish",
                "publish:status")));
    presets.add(
        new ApiKeyScopePreset(
            "integrator",
            "Integrator",
            "Content sync plus user directory reads; no publish, no design writes",
            List.of(
                "content:read",
                "content:write",
                "asset:read",
                "site:read",
                "publish:status",
                "user:read")));
    presets.add(
        new ApiKeyScopePreset(
            "admin",
            "Administrator",
            "Every scope including deletes and system config",
            new ArrayList<>(byKey.keySet())));
    PRESETS = Collections.unmodifiableList(presets);
  }

  private ApiKeyScopeCatalog() {}

  /** All assignable scopes in catalog order. */
  public static List<ApiKeyScope> scopes() {
    return SCOPES;
  }

  /** Least-privilege presets, least to most privilege. */
  public static List<ApiKeyScopePreset> presets() {
    return PRESETS;
  }

  /** True when {@code key} is a catalog scope. */
  public static boolean isKnownScope(String key) {
    return key != null && BY_KEY.containsKey(key);
  }

  /** Catalog order index for stable sorting; unknown keys sort last. */
  public static int scopeOrder(String key) {
    int i = 0;
    for (ApiKeyScope scope : SCOPES) {
      if (scope.getKey().equals(key)) {
        return i;
      }
      i++;
    }
    return Integer.MAX_VALUE;
  }

  /**
   * Id of the preset whose scope set exactly equals {@code scopes}, or {@code null} for custom
   * combinations. Blank/null entries are ignored; duplicates collapse.
   */
  public static String matchPreset(List<String> scopes) {
    TreeSet<String> want = new TreeSet<>();
    if (scopes != null) {
      for (String s : scopes) {
        if (s != null && !s.isBlank()) {
          want.add(s.trim());
        }
      }
    }
    for (ApiKeyScopePreset preset : PRESETS) {
      if (want.equals(new TreeSet<>(preset.getScopes()))) {
        return preset.getId();
      }
    }
    return null;
  }
}
