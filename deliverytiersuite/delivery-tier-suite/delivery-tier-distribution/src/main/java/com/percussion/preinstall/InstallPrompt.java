/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * Operator I/O for the DTS interactive installer wizard (issue #1513). Production code uses {@link
 * SystemConsoleInstallPrompt}; unit tests supply scripted answers.
 *
 * <p>Callers that receive a non-null {@code char[]} from {@link #readPassword(String)} should zero
 * the array after converting to the durable form they need.
 */
public interface InstallPrompt {

  /**
   * Writes a message to the operator (no trailing newline unless included).
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
   * @return the line without terminator; empty string if EOF (never {@code null})
   */
  String readLine(String prompt);

  /**
   * Prompts and reads a password without echoing.
   *
   * @param prompt text shown before reading; never {@code null}
   * @return password characters (caller should zero after use); empty array if the operator entered
   *     an empty password; {@code null} if no console is available
   */
  char[] readPassword(String prompt);
}
