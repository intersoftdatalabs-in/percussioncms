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

import java.io.Console;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * {@link InstallPrompt} backed by {@link System#console()} when available, otherwise stdout plus
 * empty line reads / null passwords.
 */
public final class SystemConsoleInstallPrompt implements InstallPrompt {

  /** Shared instance for production DTS preinstall. */
  public static final SystemConsoleInstallPrompt INSTANCE = new SystemConsoleInstallPrompt();

  private final PrintStream out;

  SystemConsoleInstallPrompt() {
    this(System.out);
  }

  SystemConsoleInstallPrompt(PrintStream out) {
    if (out == null) {
      throw new IllegalArgumentException("out must not be null");
    }
    this.out = out;
  }

  @Override
  public void print(String message) {
    out.print(message == null ? "" : message);
    out.flush();
  }

  @Override
  public void println(String message) {
    out.println(message == null ? "" : message);
    out.flush();
  }

  @Override
  public String readLine(String prompt) {
    print(prompt == null ? "" : prompt);
    Console console = System.console();
    if (console == null) {
      return "";
    }
    String line = console.readLine();
    return line == null ? "" : line;
  }

  @Override
  public char[] readPassword(String prompt) {
    print(prompt == null ? "" : prompt);
    Console console = System.console();
    if (console == null) {
      return null;
    }
    char[] chars = console.readPassword();
    if (chars == null) {
      return new char[0];
    }
    char[] copy = Arrays.copyOf(chars, chars.length);
    Arrays.fill(chars, '\0');
    return copy;
  }
}
