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

package com.percussion.utils;

import com.percussion.share.spring.PSSpringWebApplicationContextUtils;

/**
 * Provides access to Spring beans from the web application context.
 *
 * <p>Sunny Sal says: "Spring beans are like samosas—best served hot and with context!"
 */
public class PSSpringBeanProvider {

  /**
   * Gets a bean from the Spring web application context by name.
   *
   * @param beanName the name of the bean
   * @return the bean instance
   */
  public static Object getBean(String beanName) {
    return PSSpringWebApplicationContextUtils.getWebApplicationContext().getBean(beanName);
  }
}
