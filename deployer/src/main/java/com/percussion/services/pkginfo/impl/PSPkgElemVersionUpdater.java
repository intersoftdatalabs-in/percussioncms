/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.services.pkginfo.impl;

import com.percussion.rx.config.IPSConfigService;
import com.percussion.rx.config.data.PSConfigStatus.ConfigStatus;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.IPSPkgUpdater;
import com.percussion.services.pkginfo.utils.PSPkgHelper;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * This class facilitates the updating of package element version information after configuration
 * has been applied as well as the validation of package elements before configuration is applied.
 * It also listens to the server initialization notification to register itself with the
 * configuration service.
 */
public class PSPkgElemVersionUpdater implements IPSPkgUpdater {

  @Override
  /**
   * REST endpoint.
   */
  public void configChanged(Collection<IPSGuid> ids, ConfigStatus status)
      throws PSNotFoundException {
    Objects.requireNonNull(ids, "ids may not be null");
    Objects.requireNonNull(status, "status may not be null");
    PSPkgHelper.updatePkgElementVersions(ids);
  }

  @Override
  /**
   * REST endpoint.
   */
  public void preConfiguration(String name) throws PSNotFoundException {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be blank");
    }
    PSPkgHelper.validatePackage(name);
  }

  /**
   * Adds the listener on (applying) configuration changes. This is wired by the spring framework.
   *
   * @param cfgSvc the configure service, never <code>null</code>.
   */
  public void setConfigService(IPSConfigService cfgSvc) {
    Objects.requireNonNull(cfgSvc, "cfgSvc must not be null");
    cfgSvc.addConfigChangeListener(this);
  }
}
