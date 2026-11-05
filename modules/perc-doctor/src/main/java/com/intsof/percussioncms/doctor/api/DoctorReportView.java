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
package com.intsof.percussioncms.doctor.api;

import com.intsof.percussioncms.doctor.CleanReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JSON-serializable structured report matching CLI clean command output ({@link CleanReport}).
 *
 * <p>Paths are absolute path strings for portable JSON (no {@link java.nio.file.Path} type).
 */
public class DoctorReportView {

  /** One inventoried / acted-on file. */
  public static class EntryView {
    private String path;
    private long sizeBytes;
    private String status;
    private String detail;

    /**
     * Performs the operation.
     *
     */
    public EntryView() {}

    /**
     * The value.
     *
     */
    /**
     * Constructs a new instance.
     *
     * @param path the value
     * @param sizeBytes the value
     * @param status the value
     * @param detail the value
     */
    public EntryView(String path, long sizeBytes, String status, String detail) {
      this.path = path;
      this.sizeBytes = sizeBytes;
      this.status = status;
      this.detail = detail;
    }

    /**
     * The value.
     *
     */
    /**
     * Returns the value.
     *
     * @return the result
     */
    public String getPath() {
      return path;
    }

    /**
     * The value.
     *
     */
    /**
     * Sets the value.
     *
     * @param path the value
     */
    public void setPath(String path) {
      this.path = path;
    }

    /**
     * The value.
     *
     */
    /**
     * Returns the value.
     *
     * @return the result
     */
    public long getSizeBytes() {
      return sizeBytes;
    }

    /**
     * The value.
     *
     */
    /**
     * Sets the value.
     *
     * @param sizeBytes the value
     */
    public void setSizeBytes(long sizeBytes) {
      this.sizeBytes = sizeBytes;
    }

    /**
     * The value.
     *
     */
    /**
     * Returns the value.
     *
     * @return the result
     */
    public String getStatus() {
      return status;
    }

    /**
     * The value.
     *
     */
    /**
     * Sets the value.
     *
     * @param status the value
     */
    public void setStatus(String status) {
      this.status = status;
    }

    /**
     * The value.
     *
     */
    /**
     * Returns the value.
     *
     * @return the result
     */
    public String getDetail() {
      return detail;
    }

    /**
     * The value.
     *
     */
    /**
     * Sets the value.
     *
     * @param detail the value
     */
    public void setDetail(String detail) {
      this.detail = detail;
    }
  }

  private String command;
  private String installRoot;
  private boolean dryRun;
  private int candidateCount;
  private int deletedCount;
  private int failedCount;
  private long totalBytes;
  private List<EntryView> entries = new ArrayList<>();

  /**
   * Constructs a new instance.
   *
   */
  public DoctorReportView() {}

  /**
   * Convert a CLI {@link CleanReport} into a JSON view.
   *
   * @param report command report; never null
   * @return view for REST response
   */
  public static DoctorReportView from(CleanReport report) {
    Objects.requireNonNull(report, "report");
    DoctorReportView view = new DoctorReportView();
    view.command = report.getCommand();
    view.installRoot = report.getInstallRoot().toString();
    view.dryRun = report.isDryRun();
    view.candidateCount = report.getCandidateCount();
    view.deletedCount = report.getDeletedCount();
    view.failedCount = report.getFailedCount();
    view.totalBytes = report.getTotalBytes();
    List<EntryView> list = new ArrayList<>();
    for (CleanReport.Entry e : report.getEntries()) {
      list.add(
          new EntryView(
              e.getPath().toString(),
              e.getSizeBytes(),
              e.getStatus().name(),
              e.getDetail()));
    }
    view.entries = list;
    return view;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public String getCommand() {
    return command;
  }

  /**
   * Sets the value.
   *
   * @param command the value
   */
  public void setCommand(String command) {
    this.command = command;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public String getInstallRoot() {
    return installRoot;
  }

  /**
   * Sets the value.
   *
   * @param installRoot the value
   */
  public void setInstallRoot(String installRoot) {
    this.installRoot = installRoot;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public boolean isDryRun() {
    return dryRun;
  }

  /**
   * Sets the value.
   *
   * @param dryRun the value
   */
  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public int getCandidateCount() {
    return candidateCount;
  }

  /**
   * Sets the value.
   *
   * @param candidateCount the value
   */
  public void setCandidateCount(int candidateCount) {
    this.candidateCount = candidateCount;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public int getDeletedCount() {
    return deletedCount;
  }

  /**
   * Sets the value.
   *
   * @param deletedCount the value
   */
  public void setDeletedCount(int deletedCount) {
    this.deletedCount = deletedCount;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public int getFailedCount() {
    return failedCount;
  }

  /**
   * Sets the value.
   *
   * @param failedCount the value
   */
  public void setFailedCount(int failedCount) {
    this.failedCount = failedCount;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public long getTotalBytes() {
    return totalBytes;
  }

  /**
   * Sets the value.
   *
   * @param totalBytes the value
   */
  public void setTotalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public List<EntryView> getEntries() {
    return entries == null ? Collections.emptyList() : entries;
  }

  /**
   * Sets the value.
   *
   * @param entries the value
   */
  public void setEntries(List<EntryView> entries) {
    this.entries = entries;
  }
}
