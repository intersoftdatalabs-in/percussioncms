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
// REFACTORED: CP-JAVA11

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigRegistrationMgr;
import com.percussion.rx.config.IPSConfigService.ConfigTypes;
import com.percussion.rx.config.PSConfigServiceLocator;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.filemonitor.IPSFileMonitorService;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageAction;
import java.io.File;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class facilitates the registration of configurations. See interface for the details. It also
 * listens to the server initialization notification and then calls the package info service to get
 * all successfully installed packages. Calls the configuration service to apply the configuration
 * and registers those packages.
 */
public class PSConfigRegistrationMgr implements IPSConfigRegistrationMgr, IPSNotificationListener {

  /** Default constructor for use by Spring. */
  public PSConfigRegistrationMgr() {}

  @Override
  public void register(String configName) {
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName must not be blank");
    var cfgSrvs = PSConfigServiceLocator.getConfigService();

    // monitor local configure file changes
    var lcConfig = cfgSrvs.getConfigFile(ConfigTypes.LOCAL_CONFIG, configName);
    if (lcConfig.exists()) {
      m_fileList.add(lcConfig);
      getFileMonitorService().monitorFile(lcConfig);
    }
  }

  @Override
  public void unregister(String configName) {
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName must not be blank");

    var cfgSrvs = PSConfigServiceLocator.getConfigService();

    // remove monitoring the local configure file
    var lcConfig = cfgSrvs.getConfigFile(ConfigTypes.LOCAL_CONFIG, configName);
    if (lcConfig.exists()) getFileMonitorService().unmonitorFile(lcConfig);
    m_fileList.remove(lcConfig);
  }

  /**
   * Gets the notification from the notification service, cares about file change and server
   * initialization notifications. On file change notification calls the configuration service to
   * apply the configuration. On server initialization notification applies on all successfully
   * installed packages.
   *
   * @param event may be null.
   */
  @Override
  public void notifyEvent(PSNotificationEvent event) {
    if (event == null || event.getTarget() == null) {
      return;
    }
    var cfgSrvs = PSConfigServiceLocator.getConfigService();
    if (event.getType() == EventType.FILE) {
      var tgtFile = (File) event.getTarget();
      if (!m_fileList.contains(tgtFile)) return;
      var cfg = cfgSrvs.getConfigName(tgtFile);
      if (cfg != null) cfgSrvs.applyConfiguration(new String[] {cfg}, true);
    } else if (event.getType() == EventType.CORE_SERVER_INITIALIZED) {
      ms_logger.info("Processing package configurations.");
      processAllConfigs();
      ms_logger.info("Packages configuration complete.");
    }
  }

  /**
   * Helper method to process all configurations during the server initialization. Gets the
   * successfully installed packages from package info service and then calls the configuration
   * service to apply the configuration, registers the local configuration files for monitoring.
   */
  private void processAllConfigs() {
    var cfgSrvs = PSConfigServiceLocator.getConfigService();
    var pkgServ = PSPkgInfoServiceLocator.getPkgInfoService();
    var pkgList = pkgServ.findAllPkgInfos();
    var pkgs = new HashMap<String, Boolean>();
    for (var info : pkgList) {
      // We need not to configure or monitor the packages that have been
      // uninstalled or not successfully installed.
      if (!info.isSuccessfullyInstalled() || info.getLastAction().equals(PackageAction.UNINSTALL))
        continue;
      if (pkgs.containsKey(info.getPackageDescriptorName())) continue;
      pkgs.put(info.getPackageDescriptorName(), info.isSuccessfullyInstalled());
    }
    var sPkgs = new ArrayList<String>();
    for (var pkgName : pkgs.keySet()) {
      if (pkgs.get(pkgName)) {
        sPkgs.add(pkgName);
      }
    }
    // Apply all configurations.
    cfgSrvs.applyConfiguration(sPkgs.toArray(new String[0]), true);
    // Register the configurations so that the file changes are monitored.
    for (var pkg : sPkgs) {
      register(pkg);
    }
  }

  /**
   * Adds the file listener and server initialization listener during the notification service setup
   * by the spring framework.
   *
   * @param service notification service, must not be null.
   */
  public void setNotificationService(IPSNotificationService service) {
    Objects.requireNonNull(service, "service must not be null");
    service.addListener(EventType.FILE, this);
    service.addListener(EventType.CORE_SERVER_INITIALIZED, this);
  }

  /**
   * Returns the file monitor service used by this manager.
   *
   * @return Returns the file monitor service. May be null.
   */
  public IPSFileMonitorService getFileMonitorService() {
    return m_fileMonitorService;
  }

  /**
   * Set the file monitor service, wired by spring framework.
   *
   * @param service must not be null.
   */
  public void setFileMonitorService(IPSFileMonitorService service) {
    Objects.requireNonNull(service, "service must not be null");
    m_fileMonitorService = service;
  }

  /** Wired in by spring to file monitor service. */
  private IPSFileMonitorService m_fileMonitorService;

  /** List of the files this class monitors. */
  private final List<File> m_fileList = new ArrayList<>();

  /** The logger for this class. */
  private static final Logger ms_logger = LogManager.getLogger("PSConfigRegistrationMgr");
}
