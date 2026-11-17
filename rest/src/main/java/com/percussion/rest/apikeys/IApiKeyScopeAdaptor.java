/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.apikeys;

import java.util.List;

/** Adaptor for the Developer API key scope registry (#6481 read + Admin upsert). */
public interface IApiKeyScopeAdaptor {

  /** All assignable scopes in catalog order. */
  List<ApiKeyScope> listScopes();

  /** Least-privilege presets, least to most privilege. */
  List<ApiKeyScopePreset> listPresets();

  /** All key assignments, name-sorted. */
  List<ApiKeyAssignment> listKeys();

  /**
   * One key assignment, or {@code null} when no key with that name is registered.
   *
   * @param name key name (trimmed by the caller contract; blank is illegal)
   */
  ApiKeyAssignment getKey(String name);

  /**
   * Create or replace the scope set for a named key (Admin).
   *
   * @param name key name from the path; the body name (when present) must match
   * @param body scopes to store; unknown scope keys are rejected
   * @return stored assignment with recomputed {@code presetId}
   * @throws IllegalArgumentException blank name, name mismatch, or unknown scope (HTTP 400)
   * @throws jakarta.ws.rs.WebApplicationException 403 when the caller is not an Admin
   */
  ApiKeyAssignment saveKey(String name, ApiKeyAssignment body);
}
