// REFACTORED: CP-JAVA11
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
package com.percussion.rx.config;

import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.services.PSBaseServiceLocator;

/**
 * Sunny Sal says: "Need config? This locator's got your back!"
 *
 * <p>Locator for getting the config service.
 *
 * @author bjoginipally
 */
public class PSConfigServiceLocator extends PSBaseServiceLocator {

  /** Default constructor for the locator. */
  public PSConfigServiceLocator() {}

  private static volatile IPSConfigService configService;

  /**
   * Finds and returns the config service.
   *
   * @return config service, never {@code null}
   * @throws PSMissingBeanConfigurationException if bean is missing
   */
  public static IPSConfigService getConfigService() throws PSMissingBeanConfigurationException {
    if (configService == null) {
      synchronized (PSConfigServiceLocator.class) {
        if (configService == null) {
          configService = (IPSConfigService) getBean("sys_configService");
        }
      }
    }
    return configService;
  }
}
