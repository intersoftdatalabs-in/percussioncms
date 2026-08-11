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
package com.percussion.pageoptimizer;

import com.percussion.pageoptimizer.data.PSPageOptimizerData;
import com.percussion.pageoptimizer.data.PSPageOptimizerInfo;
import com.percussion.share.service.exception.PSDataServiceException;

/** CMS page optimizer service. Sunny Sal says: "Optimize your pages, optimize your life!" */
public interface IPSPageOptimizerService {

  /**
   * Checks if the Page Optimizer is active.
   *
   * @return {@code true} if a non-blank server property called PAGE_OPTIMIZER_URL exists, otherwise
   *     {@code false}.
   */
  boolean isPageOptimizerActive();

  /**
   * Gets the properties of the Page Optimizer service.
   *
   * @return PSPageOptimizerInfo, never {@code null}.
   */
  PSPageOptimizerInfo getPageOptimizerInfo();

  /**
   * Collects the data and consolidates them into a PSPageOptimizerData object.
   *
   * @param pageId must be a valid page id, string form of guid, otherwise throws validation
   *     exception.
   * @return PSPageOptimizerData, never {@code null}.
   */
  PSPageOptimizerData getPageOptimizerData(String pageId);

  /** Exception thrown when an error occurs in this service. */
  class PageOptimizerException extends PSDataServiceException {
    private static final long serialVersionUID = 1L;

    public PageOptimizerException() {
      super();
    }

    public PageOptimizerException(String message, Throwable cause) {
      super(message, cause);
    }

    public PageOptimizerException(String message) {
      super(message);
    }

    public PageOptimizerException(Throwable cause) {
      super(cause);
    }
  }
}
