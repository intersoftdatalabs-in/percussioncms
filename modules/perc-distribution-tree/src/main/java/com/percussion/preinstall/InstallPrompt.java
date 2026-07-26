/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

/**
 * Operator I/O for the interactive installer wizard. Production code uses {@link
 * SystemConsoleInstallPrompt}; unit tests supply scripted answers.
 *
 * <p>Implementations must never log or echo password characters returned from {@link
 * #readPassword(String)}.
 */
public interface InstallPrompt {

  /**
   * Writes a message to the operator (no trailing newline unless included in {@code message}).
   *
   * @param message text to write; may be empty, never {@code null}
   */
  void print(String message);

  /**
   * Writes a message followed by a platform line separator.
   *
   * @param message text to write; may be empty, never {@code null}
   */
  void println(String message);

  /**
   * Prompts and reads a single line of operator input.
   *
   * @param prompt text shown before reading; never {@code null}
   * @return the line without terminator; empty string if EOF / unavailable (never {@code null})
   */
  String readLine(String prompt);

  /**
   * Prompts and reads a password without echoing. Phase 1 may not use this; later DB steps will.
   *
   * @param prompt text shown before reading; never {@code null}
   * @return password characters, or empty string if unavailable (never {@code null})
   */
  String readPassword(String prompt);
}
