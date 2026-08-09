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
package com.intsof.percussioncms.doctor;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared pure rules for CMS install/startup log content scans ({@code check-logs}).
 *
 * <p>Aligned with {@code docker/scripts/rhythmyx_ready.py}: Rhythmyx context-death markers and
 * Log4j/JUL-style ERROR / FATAL / SEVERE lines. Keep allowlists empty by default so startup stays
 * clean.
 */
public final class LogScanRules {

  /** Max characters of a matched line returned to operators / RESULT MATCH. */
  public static final int MATCH_LINE_MAX = 240;

  /**
   * Substrings from Jetty / Spring logs when ROOT/Rhythmyx context fails (same set as docker
   * readiness probes).
   */
  public static final String[] RHYTHMYX_CONTEXT_FAIL_MARKERS = {
    "Failed startup of context",
    "BeanCurrentlyInCreationException",
    "Requested bean is currently in creation",
    "Is there an unresolvable circular reference"
  };

  /**
   * Log4j2 / JUL-style severity tokens on a line. Matches {@code ERROR}, {@code FATAL}, {@code
   * SEVERE} as whole tokens (case-sensitive log levels so prose like "error manager" does not
   * match). Not inside identifiers like {@code ERROR_CODE}.
   */
  private static final Pattern SEVERITY_RE =
      Pattern.compile("(?:^|[\\s\\[])(?:ERROR|FATAL|SEVERE)(?:[\\s\\]:]|$)");

  private LogScanRules() {}

  /**
   * Return the first Rhythmyx context-failure marker found in {@code logText}, or null.
   *
   * @param logText log content (may be null/empty)
   * @return first matching marker, or null
   */
  public static String findContextFailureMarker(String logText) {
    if (logText == null || logText.isEmpty()) {
      return null;
    }
    for (String marker : RHYTHMYX_CONTEXT_FAIL_MARKERS) {
      if (marker != null && !marker.isEmpty() && logText.contains(marker)) {
        return marker;
      }
    }
    return null;
  }

  /**
   * Return a short description of the first startup / install error in {@code logText}.
   *
   * <p>Order: context markers first, then the first ERROR/FATAL/SEVERE line (truncated).
   *
   * @param logText log content (may be null/empty — treated as clean / no evidence yet)
   * @return match description, or null when clean
   */
  public static String findStartupError(String logText) {
    return findStartupError(logText, true);
  }

  /**
   * @param logText log content
   * @param alsoContextMarkers when true, also scan for Rhythmyx context markers
   * @return match description, or null when clean
   */
  public static String findStartupError(String logText, boolean alsoContextMarkers) {
    if (logText == null || logText.isEmpty()) {
      return null;
    }
    if (alsoContextMarkers) {
      String ctx = findContextFailureMarker(logText);
      if (ctx != null) {
        return ctx;
      }
    }
    String[] lines = logText.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    for (String raw : lines) {
      if (raw == null) {
        continue;
      }
      String line = raw.strip();
      if (line.isEmpty()) {
        continue;
      }
      if (SEVERITY_RE.matcher(line).find()) {
        return clip(line);
      }
    }
    return null;
  }

  /**
   * Whether {@code logText} is free of context markers and ERROR/FATAL/SEVERE lines.
   *
   * @param logText log content
   * @return true when no error evidence
   */
  public static boolean isClean(String logText) {
    return findStartupError(logText) == null;
  }

  static String clip(String line) {
    Objects.requireNonNull(line, "line");
    if (line.length() <= MATCH_LINE_MAX) {
      return line;
    }
    return line.substring(0, MATCH_LINE_MAX - 3) + "...";
  }
}
