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
 * Read-only value / misconfig checklist report for {@code check-config}. Never implies filesystem
 * mutations.
 */
public final class CheckConfigReport {

  /** Outcome severity for a single config check. */
  public enum CheckStatus {
    /** Expected condition met. */
    PASS,
    /** Non-fatal concern (weak defaults, production hygiene). */
    WARN,
    /** Critical misconfig or unreadable required config. */
    FAIL,
    /** Pure information (documented default, not necessarily wrong). */
    INFO
  }

  /** One checklist row. */
  public static final class Check {
    private final String id;
    private final CheckStatus status;
    private final String message;
    private final Path path;

    /**
     * @param id stable machine-oriented check id (e.g. {@code server.enableDebugTools})
     * @param status outcome severity
     * @param message human-readable detail
     * @param path optional related path under install root (may be null)
     */
    public Check(String id, CheckStatus status, String message, Path path) {
      this.id = Objects.requireNonNull(id, "id");
      this.status = Objects.requireNonNull(status, "status");
      this.message = Objects.requireNonNull(message, "message");
      this.path = path;
    }

    /**
     * @return stable check id
     */
    public String getId() {
      return id;
    }

    /**
     * @return outcome severity
     */
    public CheckStatus getStatus() {
      return status;
    }

    /**
     * @return human-readable detail
     */
    public String getMessage() {
      return message;
    }

    /**
     * @return related path, or null
     */
    public Path getPath() {
      return path;
    }
  }

  private final String command;
  private final Path installRoot;
  private final boolean dryRun;
  private final List<Check> checks = new ArrayList<>();

  /**
   * @param command command token ({@code check-config})
   * @param installRoot resolved install root
   * @param dryRun echoed global flag (check-config never mutates regardless)
   */
  public CheckConfigReport(String command, Path installRoot, boolean dryRun) {
    this.command = Objects.requireNonNull(command, "command");
    this.installRoot = Objects.requireNonNull(installRoot, "installRoot");
    this.dryRun = dryRun;
  }

  /** Append a checklist entry. */
  public void add(Check check) {
    checks.add(Objects.requireNonNull(check, "check"));
  }

  /**
   * @return command token
   */
  public String getCommand() {
    return command;
  }

  /**
   * @return install root used for this run
   */
  public Path getInstallRoot() {
    return installRoot;
  }

  /**
   * Echo of the global {@code --dry-run} flag. check-config is always read-only; this value is
   * reported for flag parity and never gates writes (there are none).
   *
   * @return whether {@code --dry-run} was set on the CLI
   */
  public boolean isDryRun() {
    return dryRun;
  }

  /**
   * @return unmodifiable checklist
   */
  public List<Check> getChecks() {
    return Collections.unmodifiableList(checks);
  }

  /**
   * @return number of checks
   */
  public int getCheckCount() {
    return checks.size();
  }

  /**
   * @return count of {@link CheckStatus#PASS}
   */
  public int getPassCount() {
    return count(CheckStatus.PASS);
  }

  /**
   * @return count of {@link CheckStatus#WARN}
   */
  public int getWarnCount() {
    return count(CheckStatus.WARN);
  }

  /**
   * @return count of {@link CheckStatus#FAIL}
   */
  public int getFailCount() {
    return count(CheckStatus.FAIL);
  }

  /**
   * @return count of {@link CheckStatus#INFO}
   */
  public int getInfoCount() {
    return count(CheckStatus.INFO);
  }

  /**
   * @return true when no {@link CheckStatus#FAIL} entries (WARN/INFO allowed)
   */
  public boolean isHealthy() {
    return getFailCount() == 0;
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
