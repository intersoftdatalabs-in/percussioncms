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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Read-only log content scan report for {@code check-logs}. Never implies filesystem mutations.
 */
public final class CheckLogsReport {

  /** Outcome severity for a single log file scan. */
  public enum CheckStatus {
    /** File present and free of ERROR/FATAL/SEVERE / context markers. */
    PASS,
    /** File missing and not required for the selected phase. */
    SKIP,
    /** Non-fatal concern (e.g. unreadable with soft policy — reserved). */
    WARN,
    /** Errors found, required file missing, or unreadable when required. */
    FAIL,
    /** Pure information. */
    INFO
  }

  /** One log-file scan row. */
  public static final class Check {
    private final String id;
    private final CheckStatus status;
    private final String message;
    private final Path path;
    private final String match;

    /**
     * @param id stable id (e.g. {@code log.server})
     * @param status outcome
     * @param message human-readable detail
     * @param path optional path under install root
     * @param match optional first error line / marker (may be null)
     */
    public Check(String id, CheckStatus status, String message, Path path, String match) {
      this.id = Objects.requireNonNull(id, "id");
      this.status = Objects.requireNonNull(status, "status");
      this.message = Objects.requireNonNull(message, "message");
      this.path = path;
      this.match = match;
    }

    public String getId() {
      return id;
    }

    public CheckStatus getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public Path getPath() {
      return path;
    }

    public String getMatch() {
      return match;
    }
  }

  private final String command;
  private final Path installRoot;
  private final boolean dryRun;
  private final String phase;
  private final List<Check> checks = new ArrayList<>();

  /**
   * @param command command token
   * @param installRoot resolved install root
   * @param dryRun echoed global flag
   * @param phase scan phase ({@code all}, {@code startup}, {@code install})
   */
  public CheckLogsReport(String command, Path installRoot, boolean dryRun, String phase) {
    this.command = Objects.requireNonNull(command, "command");
    this.installRoot = Objects.requireNonNull(installRoot, "installRoot");
    this.dryRun = dryRun;
    this.phase = Objects.requireNonNull(phase, "phase");
  }

  public void add(Check check) {
    checks.add(Objects.requireNonNull(check, "check"));
  }

  public String getCommand() {
    return command;
  }

  public Path getInstallRoot() {
    return installRoot;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public String getPhase() {
    return phase;
  }

  public List<Check> getChecks() {
    return Collections.unmodifiableList(checks);
  }

  public int getCheckCount() {
    return checks.size();
  }

  public int getPassCount() {
    return count(CheckStatus.PASS);
  }

  public int getSkipCount() {
    return count(CheckStatus.SKIP);
  }

  public int getWarnCount() {
    return count(CheckStatus.WARN);
  }

  public int getFailCount() {
    return count(CheckStatus.FAIL);
  }

  public int getInfoCount() {
    return count(CheckStatus.INFO);
  }

  /** @return true when no FAIL entries */
  public boolean isHealthy() {
    return getFailCount() == 0;
  }

  /** First FAIL match string, if any (for RESULT MATCH). */
  public String firstFailMatch() {
    for (Check c : checks) {
      if (c.getStatus() == CheckStatus.FAIL && c.getMatch() != null && !c.getMatch().isEmpty()) {
        return c.getMatch();
      }
    }
    for (Check c : checks) {
      if (c.getStatus() == CheckStatus.FAIL) {
        return c.getMessage();
      }
    }
    return null;
  }

  private int count(CheckStatus status) {
    int n = 0;
    for (Check c : checks) {
      if (c.getStatus() == status) {
        n++;
      }
    }
    return n;
  }
}
