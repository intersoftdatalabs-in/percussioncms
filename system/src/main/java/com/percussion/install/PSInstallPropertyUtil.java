/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.install;

/** Shared helpers for installer / migrator system and install properties (#548). */
public final class PSInstallPropertyUtil {

  private PSInstallPropertyUtil() {}

  /**
   * True for common affirmative property values: {@code true}, {@code yes}, {@code 1}
   * (case-insensitive). Null/blank/other values are false.
   *
   * @param value raw property value; may be null
   * @return true if the value is an affirmative flag
   */
  public static boolean isTruthy(String value) {
    if (value == null) {
      return false;
    }
    String v = value.trim();
    return "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v) || "1".equals(v);
  }
}
