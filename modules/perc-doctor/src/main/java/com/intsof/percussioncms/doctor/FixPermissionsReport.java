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

/** Inventory / action report for {@code fix-permissions}. */
public final class FixPermissionsReport {

  /** Outcome for a single path / permission item. */
  public enum EntryStatus {
    /** Already correct; no change needed. */
    OK,
    /** Dry-run: would apply a documented mode fix. */
    WOULD_FIX,
    /** Apply: mode fix was applied. */
    FIXED,
    /** Intentionally not changed (unsupported platform, missing path, etc.). */
    SKIPPED,
    /** Error while inspecting or applying. */
    FAILED
  }

  /** One path considered by fix-permissions. */
  public static final class Entry {
    private final Path path;
    private final EntryStatus status;
    private final String detail;

    /**
     * @param path absolute or normalized path of the candidate
     * @param status outcome
     * @param detail human-readable detail (may be null)
     */
    public Entry(Path path, EntryStatus status, String detail) {
      this.path = Objects.requireNonNull(path, "path");
      this.status = Objects.requireNonNull(status, "status");
      this.detail = detail;
    }

    /**
     * @return candidate path
     */
    public Path getPath() {
      return path;
    }

    /**
     * @return outcome status
     */
    public EntryStatus getStatus() {
      return status;
    }

    /**
     * @return optional detail message, or null
     */
    public String getDetail() {
      return detail;
    }
  }

  private final String command;
  private final Path installRoot;
  private final boolean dryRun;
  private final List<Entry> entries = new ArrayList<>();

  /**
   * @param command command name ({@code fix-permissions})
   * @param installRoot resolved install root
   * @param dryRun whether this report is from a dry-run
   */
  public FixPermissionsReport(String command, Path installRoot, boolean dryRun) {
    this.command = Objects.requireNonNull(command, "command");
    this.installRoot = Objects.requireNonNull(installRoot, "installRoot");
    this.dryRun = dryRun;
  }

  /** Append an inventory/action entry. */
  public void add(Entry entry) {
    entries.add(Objects.requireNonNull(entry, "entry"));
  }

  /**
   * @return command name
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
   * @return true if no mode fixes were applied
   */
  public boolean isDryRun() {
    return dryRun;
  }

  /**
   * @return unmodifiable list of entries
   */
  public List<Entry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /**
   * @return number of entries
   */
  public int getCandidateCount() {
    return entries.size();
  }

  /**
   * @return count of successfully fixed entries
   */
  public int getFixedCount() {
    int n = 0;
    for (Entry e : entries) {
      if (e.getStatus() == EntryStatus.FIXED) {
        n++;
      }
    }
    return n;
  }

  /**
   * @return count of dry-run would-fix entries
   */
  public int getWouldFixCount() {
    int n = 0;
    for (Entry e : entries) {
      if (e.getStatus() == EntryStatus.WOULD_FIX) {
        n++;
      }
    }
    return n;
  }

  /**
   * @return count of failed entries
   */
  public int getFailedCount() {
    int n = 0;
    for (Entry e : entries) {
      if (e.getStatus() == EntryStatus.FAILED) {
        n++;
      }
    }
    return n;
  }
}
