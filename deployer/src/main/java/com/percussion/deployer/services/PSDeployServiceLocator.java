/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

/**
 * Service locator for deployment service.
 */
public class PSDeployServiceLocator extends PSBaseServiceLocator {
    private static volatile IPSDeployService dsr = null;

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
