/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.cms.objectstore.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Pure parser for authtype properties files used by {@link PSAuthTypes}.
 *
 * <p>Separated from {@link PSAuthTypes} so unit tests can exercise key/value handling without
 * triggering the singleton's static config-file initialization (issue #2624).
 */
final class PSAuthTypePropertiesParser {

  private PSAuthTypePropertiesParser() {}

  /**
   * Parse an authtype properties file into the authtype → resource map.
   *
   * <p>Preserves historical key handling: any property whose name is longer than the {@code
   * authtype.} prefix contributes a map entry whose key is {@code name.substring("authtype."
   * .length())} (only non-empty property values are kept).
   *
   * @param props never {@code null}
   * @return never {@code null}; may be empty
   */
  static Map<String, String> parse(Properties props) {
    if (props == null) {
      throw new IllegalArgumentException("props may not be null");
    }
    Map<String, String> result = new HashMap<>();
    final String AUTHTYPE_PREFIX = "authtype.";
    for (String name : props.stringPropertyNames()) {
      String value = props.getProperty(name);
      if (value != null && !value.isEmpty()) {
        if (name.length() > AUTHTYPE_PREFIX.length()) {
          String key = name.substring(AUTHTYPE_PREFIX.length());
          result.put(key, value);
        }
      }
    }
    return result;
  }
}
