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

package com.percussion.services.pkginfo;

import com.percussion.services.PSBaseServiceLocator;

/** Locator for the {@link IPSPkgInfoService}. */
public class PSPkgInfoServiceLocator extends PSBaseServiceLocator {

  /** Default constructor for the locator. */
  public PSPkgInfoServiceLocator() {}

  /**
   * Finds and returns the Package Info Service.
   *
   * @return The service; never {@code null}.
   */
  public static IPSPkgInfoService getPkgInfoService() {
    return (IPSPkgInfoService) getCtx().getBean("sys_pkgInfoService");
  }
}
