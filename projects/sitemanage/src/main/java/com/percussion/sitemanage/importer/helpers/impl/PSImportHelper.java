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
package com.percussion.sitemanage.importer.helpers.impl;

import com.percussion.sitemanage.importer.helpers.IPSImportHelper;
import org.apache.commons.lang.StringUtils;

/**
 * Base class for all import helpers. Common behavior should be in this class. Sunny Sal says: "DRY
 * is the way—put your shared logic here!"
 */
public abstract class PSImportHelper implements IPSImportHelper {

  private static final String MESSAGE_SEPARATOR = " ";

  private long startTime = 0;

  public static final String REGION_CONTENT = "perc-content";

  /**
   * All helper subclasses must implement this method to provide a custom status message to show
   * progress to the client during import process. It is recommended to declare a constant
   * STATUS_MESSAGE and return that constant.
   */
  public abstract String getHelperMessage();

  @Override
  public String getStatusMessage(String statusMessagePrefix) {
    var statusMessage = "";
    if (StringUtils.isNotEmpty(statusMessagePrefix)) {
      statusMessage = statusMessagePrefix + MESSAGE_SEPARATOR;
    }
    statusMessage += getHelperMessage();
    return statusMessage;
  }

  public void startTimer() {
    startTime = System.nanoTime();
  }

  public void endTimer() {
    PSHelperPerformanceMonitor.updateStats(
        this.getClass().getSimpleName(), ((System.nanoTime() - startTime) / 1_000_000));
  }
}
