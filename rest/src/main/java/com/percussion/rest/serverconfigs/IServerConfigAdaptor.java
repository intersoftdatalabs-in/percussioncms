/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.serverconfigs;

import java.util.List;

/** Adaptor for server configuration catalog (SY-02 read). */
public interface IServerConfigAdaptor {

  List<ServerConfigSummary> listConfigs();

  /** Resolve by enum name (e.g. LOG_CONFIG). Null if missing/unsafe. */
  ServerConfigSummary findConfigByName(String name);
}
