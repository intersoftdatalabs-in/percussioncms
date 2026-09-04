/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.serverconfigs;

import java.util.List;

/** Adaptor for server configuration catalog (SY-02 read + allow-listed write). */
public interface IServerConfigAdaptor {

  List<ServerConfigSummary> listConfigs();

  /** Resolve by enum name (e.g. LOG_CONFIG). Null if missing/unsafe. */
  ServerConfigSummary findConfigByName(String name);

  /**
   * Admin update of an allow-listed configuration file body.
   *
   * @param name catalog key ({@code PSConfigurationTypes} enum name)
   * @param body must include {@code content} (file text); other fields are ignored for persistence
   * @return updated detail summary, or {@code null} when name is unknown/unsafe
   */
  ServerConfigSummary updateConfig(String name, ServerConfigSummary body);
}
