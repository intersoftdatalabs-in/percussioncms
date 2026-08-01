/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities;

import java.util.Locale;
import java.util.Set;

/**
 * Validates single path-segment names used for application folders and config files under {@code
 * ~/.intsof}.
 */
final class ConfigNames {

  private static final Set<String> WINDOWS_RESERVED =
      Set.of(
          "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7",
          "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

  private ConfigNames() {}

  /**
   * Validate and return a trimmed single-segment name safe for use under a config directory.
   *
   * @param raw name supplied by the caller
   * @param label human-readable label for error messages ({@code "application name"} / {@code "file
   *     name"})
   * @return trimmed name
   * @throws IllegalArgumentException when the name is blank, contains separators, or is reserved
   */
  static String requireValidSegment(String raw, String label) {
    if (raw == null) {
      throw new IllegalArgumentException(label + " must not be null");
    }
    String name = raw.trim();
    if (name.isEmpty()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    if (".".equals(name) || "..".equals(name)) {
      throw new IllegalArgumentException(label + " must not be '.' or '..'");
    }
    if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
      throw new IllegalArgumentException(label + " must not contain path separators: " + name);
    }
    // Reject absolute-looking or drive-relative Windows forms that resolve outside a parent
    if (name.indexOf(':') >= 0) {
      throw new IllegalArgumentException(label + " must not contain ':': " + name);
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c < 0x20 || c == 0x7f) {
        throw new IllegalArgumentException(label + " must not contain control characters");
      }
      if (c == '<' || c == '>' || c == '"' || c == '|' || c == '?' || c == '*') {
        throw new IllegalArgumentException(label + " contains illegal character: " + c);
      }
    }
    String upper = name.toUpperCase(Locale.ROOT);
    int dot = upper.indexOf('.');
    String base = dot >= 0 ? upper.substring(0, dot) : upper;
    if (WINDOWS_RESERVED.contains(base)) {
      throw new IllegalArgumentException(label + " is a reserved name: " + name);
    }
    return name;
  }
}
