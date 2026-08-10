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

/**
 * JSON body for {@code POST .../doctor/{command}}.
 *
 * <p><strong>Dry-run default:</strong> when {@link #dryRun} is {@code null} or omitted, the API
 * treats the request as a dry-run ({@code true}). Explicit apply requires {@code "dryRun": false}.
 * This is the safer default for an admin HTTP surface that can delete install-tree files.
 */
public class DoctorRequest {

  /**
   * When {@code true} or omitted/null, inventory only (no deletes). When {@code false}, perform
   * deletes after inventory.
   */
  private Boolean dryRun;

  /**
   * Optional CMS install root string. When blank/null, the host supplies the server install root
   * ({@code rxdeploydir} / resolved RX dir). When non-blank, the HTTP API accepts it only if it
   * normalizes to the same path as the host default; filesystem I/O always uses the host-provided
   * path (never a client-constructed path) to prevent path injection.
   */
  private String installRoot;

  /**
   * Optional age filter for {@code clean-logs} only (e.g. {@code 7d}, {@code 24h}). Ignored by
   * other commands.
   */
  private String olderThan;

  /**
   * Optional keep-current flag for {@code clean-logs}. When null, defaults to {@code true} (same as
   * CLI).
   */
  private Boolean keepCurrent;

  /**
   * @return dry-run flag as sent by client; may be null
   */
  public Boolean getDryRun() {
    return dryRun;
  }

  /**
   * @param dryRun dry-run flag; null means default true
   */
  public void setDryRun(Boolean dryRun) {
    this.dryRun = dryRun;
  }

  /**
   * Effective dry-run: {@code true} when the field is null or true.
   *
   * @return whether this request must not delete
   */
  public boolean isEffectiveDryRun() {
    return dryRun == null || Boolean.TRUE.equals(dryRun);
  }

  /**
   * @return optional install root path string
   */
  public String getInstallRoot() {
    return installRoot;
  }

  /**
   * @param installRoot optional install root
   */
  public void setInstallRoot(String installRoot) {
    this.installRoot = installRoot;
  }

  /**
   * @return optional older-than duration token for clean-logs
   */
  public String getOlderThan() {
    return olderThan;
  }

  /**
   * @param olderThan optional duration token
   */
  public void setOlderThan(String olderThan) {
    this.olderThan = olderThan;
  }

  /**
   * @return keep-current flag; may be null
   */
  public Boolean getKeepCurrent() {
    return keepCurrent;
  }

  /**
   * @param keepCurrent keep-current flag for clean-logs
   */
  public void setKeepCurrent(Boolean keepCurrent) {
    this.keepCurrent = keepCurrent;
  }

  /**
   * Effective keep-current for clean-logs: defaults to {@code true} when null.
   *
   * @return whether active current logs are retained
   */
  public boolean isEffectiveKeepCurrent() {
    return keepCurrent == null || Boolean.TRUE.equals(keepCurrent);
  }
}
