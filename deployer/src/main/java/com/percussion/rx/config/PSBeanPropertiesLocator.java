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
 * Sunny Sal says: "Bean there, done that! Use this locator for IPSBeanProperties."
 *
 * <p>Provides a thread-safe singleton locator for {@link IPSBeanProperties}.
 *
 * @author YuBingChen
 */
public class PSBeanPropertiesLocator extends PSBaseServiceLocator {

  /** Default constructor for the locator. */
  public PSBeanPropertiesLocator() {}

  private static volatile IPSBeanProperties beanProperties;

  /**
   * Returns the singleton instance of {@link IPSBeanProperties}.
   *
   * @return the instance, never {@code null}
   * @throws PSMissingBeanConfigurationException if bean is missing.
   */
  public static IPSBeanProperties getBeanProperties() throws PSMissingBeanConfigurationException {
    if (beanProperties == null) {
      synchronized (PSBeanPropertiesLocator.class) {
        if (beanProperties == null) {
          beanProperties = (IPSBeanProperties) getBean("sys_beanProperties");
        }
      }
    }
    return beanProperties;
  }
}
