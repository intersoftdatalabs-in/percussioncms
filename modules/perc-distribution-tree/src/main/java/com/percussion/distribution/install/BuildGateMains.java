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
package com.percussion.distribution.install;

/**
 * Shared completion policy for build-gate {@code main} classes invoked via {@code
 * exec-maven-plugin:java}.
 *
 * <p>That goal runs the main <strong>in the Maven JVM</strong>. Calling {@link System#exit(int)}
 * therefore terminates Maven itself — including {@code System.exit(0)} after a successful check,
 * which aborts the reactor mid-{@code verify} (no {@code install}, no later modules). Observed
 * symptom: log ends at {@code OK: N JDBC driver JAR(s) verified…} with no Reactor Summary.
 *
 * <p>Default: success returns to the mojo; failure throws so the mojo fails the build. Opt into
 * process exit codes for forked CLI only: {@code -Dperc.build.gate.systemExit=true}.
 */
final class BuildGateMains {

  private static final String SYSTEM_EXIT_PROP = "perc.build.gate.systemExit";

  private BuildGateMains() {}

  /**
   * Completes a gate main with logical exit {@code code}.
   *
   * @param code {@code 0} success; non-zero failure (script-style codes)
   * @param toolName short name for the exception message
   */
  static void complete(int code, String toolName) {
    if (Boolean.parseBoolean(System.getProperty(SYSTEM_EXIT_PROP, "false"))) {
      System.exit(code);
    }
    if (code != 0) {
      throw new RuntimeException(toolName + " failed with exit code " + code);
    }
  }
}
