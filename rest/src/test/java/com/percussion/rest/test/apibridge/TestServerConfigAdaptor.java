/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.test.apibridge;

import com.percussion.rest.serverconfigs.IServerConfigAdaptor;
import com.percussion.rest.serverconfigs.ServerConfigSummary;
import java.util.List;
import org.springframework.stereotype.Component;

/** Test adaptor for Server Configs API bridge (MainTest Spring context). */
@Component
public class TestServerConfigAdaptor implements IServerConfigAdaptor {

  @Override
  public List<ServerConfigSummary> listConfigs() {
    return List.of();
  }

  @Override
  public ServerConfigSummary findConfigByName(String name) {
    return null;
  }
}
