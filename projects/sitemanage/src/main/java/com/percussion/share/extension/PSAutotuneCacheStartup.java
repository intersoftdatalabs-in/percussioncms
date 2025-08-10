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
package com.percussion.share.extension;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSStartupProcess;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.cache.PSAutotuneCacheLocator;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Startup process to auto tune the ehcache.xml. {@link com.percussion.server.cache.PSAutotuneCache}
 *
 * @author chriswright
 */
public class PSAutotuneCacheStartup implements IPSStartupProcess {

  private static final Logger log = LogManager.getLogger(PSAutotuneCacheStartup.class);

  @Override
  public void doStartupWork(Properties startupProps) {
    var propName = getPropName();
    if (!"true".equalsIgnoreCase(startupProps.getProperty(propName))) {
      log.info(
          "{} is set to false or missing from startup properties file. Nothing to run.", propName);
      return;
    }

    try {
      var cache = PSAutotuneCacheLocator.getAutotuneCache();
      cache.updateEhcache();
    } catch (Exception e) {
      log.error("Error updating ehcache.xml file. Error: {}", PSExceptionUtils.getMessageForLog(e));
    }

    log.info("{} has completed.", propName);
  }

  @Override
  public void setStartupProcessManager(IPSStartupProcessManager mgr) {
    mgr.addStartupProcess(this);
  }

  static String getPropName() {
    return PSAutotuneCacheStartup.class.getSimpleName();
  }
}
