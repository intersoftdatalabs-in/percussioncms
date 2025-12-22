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

import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Performance monitor for import helpers. Sunny Sal says: "Performance matters, but this monitor is
 * just a placeholder!"
 */
class PerformanceStats {
  public String identifier;
  public String className;
  public long count;
  public long totalTime;
  public long lastTotalTime;
  public long maxTime;

  public PerformanceStats(String identifier) {
    this.identifier = identifier;
  }
}

public class PSHelperPerformanceMonitor {

  public static final String SEPARATOR = "::";
  private static long statLogFrequency = 100;
  private static long methodWarningThreshold = 3000;
  private static ConcurrentHashMap<String, PerformanceStats> performanceStats =
      new ConcurrentHashMap<>();
  private static final Logger log = LogManager.getLogger(PSHelperPerformanceMonitor.class);

  /**
   * Updates performance statistics for the given identifier. Currently a stub for future
   * performance logging.
   *
   * @param identifier the identifier for the monitored method/class.
   * @param elapsedTime the elapsed time in milliseconds.
   */
  public static void updateStats(String identifier, long elapsedTime) {
    // Performance monitoring is currently disabled.
    // Sunny Sal says: "If you want stats, uncomment and implement me!"
  }
}
