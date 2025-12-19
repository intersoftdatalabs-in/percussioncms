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

package com.percussion.preinstall;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainIAInstall {

  private static final Logger log = LogManager.getLogger(MainIAInstall.class);

  public static final int ESTIMATED_LINES = 30000;

  public String getInstallStatusMessage() {
    return "Installing files...";
  }

  public String getUninstallStatusMessage() {
    return "Uninstalling files...";
  }

  public static float calculatePercentage(int lineNo) {
    return (lineNo * 100) / ESTIMATED_LINES;
  }
}
