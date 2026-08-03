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

package com.percussion.deployer.services;

import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.services.PSBaseServiceLocator;

/** Service locator for deployment service. */
public class PSDeployServiceLocator extends PSBaseServiceLocator {

  /** Default constructor for the locator. */
  public PSDeployServiceLocator() {}

  private static volatile IPSDeployService dsr = null;

  /**
   * Returns the singleton deploy service instance.
   *
   * @return the deploy service, never <code>null</code>.
   * @throws PSMissingBeanConfigurationException if the bean is not configured.
   */
  public static IPSDeployService getDeployService() throws PSMissingBeanConfigurationException {
    if (dsr == null) {
      synchronized (PSDeployServiceLocator.class) {
        if (dsr == null) {
          var bean = getBean("sys_deployerService");
          dsr = (IPSDeployService) bean;
        }
      }
    }
    return dsr;
  }
}
