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

    public EntryView() {}

    public EntryView(String path, long sizeBytes, String status, String detail) {
      this.path = path;
      this.sizeBytes = sizeBytes;
      this.status = status;
      this.detail = detail;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public long getSizeBytes() {
      return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
      this.sizeBytes = sizeBytes;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getDetail() {
      return detail;
    }

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

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getInstallRoot() {
    return installRoot;
  }

  public void setInstallRoot(String installRoot) {
    this.installRoot = installRoot;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public int getCandidateCount() {
    return candidateCount;
  }

  public void setCandidateCount(int candidateCount) {
    this.candidateCount = candidateCount;
  }

  public int getDeletedCount() {
    return deletedCount;
  }

  public void setDeletedCount(int deletedCount) {
    this.deletedCount = deletedCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public void setFailedCount(int failedCount) {
    this.failedCount = failedCount;
  }

  public long getTotalBytes() {
    return totalBytes;
  }

  public void setTotalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
  }

  public List<EntryView> getEntries() {
    return entries == null ? Collections.emptyList() : entries;
  }

  public void setEntries(List<EntryView> entries) {
    this.entries = entries;
  }
}
