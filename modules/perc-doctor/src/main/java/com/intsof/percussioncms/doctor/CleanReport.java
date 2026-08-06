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

/** Inventory / action report for a doctor clean command. */
public final class CleanReport {

  /** Outcome for a single inventoried path. */
  public enum EntryStatus {
    /** Dry-run: would delete if apply were used. */
    WOULD_DELETE,
    /** Apply: file was deleted. */
    DELETED,
    /** Intentionally not deleted (e.g. already absent). */
    SKIPPED,
    /** Error while sizing or deleting. */
    FAILED
  }

  /** One file considered by a clean command. */
  public static final class Entry {
    private final Path path;
    private final long sizeBytes;
    private final EntryStatus status;
    private final String detail;

    /**
     * @param path absolute or normalized path of the candidate
     * @param sizeBytes size in bytes (0 if unknown / failed early)
     * @param status outcome
     * @param detail optional human-readable detail (may be null)
     */
    public Entry(Path path, long sizeBytes, EntryStatus status, String detail) {
      this.path = Objects.requireNonNull(path, "path");
      this.sizeBytes = sizeBytes;
      this.status = Objects.requireNonNull(status, "status");
      this.detail = detail;
    }

    /** @return candidate path */
    public Path getPath() {
      return path;
    }

    /** @return size in bytes when known */
    public long getSizeBytes() {
      return sizeBytes;
    }

    /** @return outcome status */
    public EntryStatus getStatus() {
      return status;
    }

    /** @return optional detail message, or null */
    public String getDetail() {
      return detail;
    }
  }

  private final String command;
  private final Path installRoot;
  private final boolean dryRun;
  private final List<Entry> entries = new ArrayList<>();

  /**
   * @param command command name (e.g. {@code clean-heap-dumps})
   * @param installRoot resolved install root
   * @param dryRun whether this report is from a dry-run
   */
  public CleanReport(String command, Path installRoot, boolean dryRun) {
    this.command = Objects.requireNonNull(command, "command");
    this.installRoot = Objects.requireNonNull(installRoot, "installRoot");
    this.dryRun = dryRun;
  }

  /** Append an inventory/action entry. */
  public void add(Entry entry) {
    entries.add(Objects.requireNonNull(entry, "entry"));
  }

  /** @return command name */
  public String getCommand() {
    return command;
  }

  /** @return install root used for this run */
  public Path getInstallRoot() {
    return installRoot;
  }

  /** @return true if no deletes were performed */
  public boolean isDryRun() {
    return dryRun;
  }

  /** @return unmodifiable list of entries */
  public List<Entry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /** @return number of entries (including failed/skipped) */
  public int getCandidateCount() {
    return entries.size();
  }

  /** @return sum of sizes for non-failed entries */
  public long getTotalBytes() {
    long total = 0L;
    for (Entry e : entries) {
      if (e.getStatus() == EntryStatus.FAILED) {
        continue;
      }
      total += e.getSizeBytes();
    }
    return total;
  }

  /** @return count of successfully deleted entries */
  public int getDeletedCount() {
    int n = 0;
    for (Entry e : entries) {
      if (e.getStatus() == EntryStatus.DELETED) {
        n++;
      }
    }
    return n;
  }

  /** @return count of failed entries */
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
