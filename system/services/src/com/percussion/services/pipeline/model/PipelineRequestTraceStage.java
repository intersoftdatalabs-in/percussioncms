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

package com.percussion.services.pipeline.model;

import java.util.Objects;

/** One timed stage in a pipeline request trace (fail-closed; no secret values). */
public class PipelineRequestTraceStage {

  private String name;
  private long durationMs;
  private String status;
  private String detail;

  public PipelineRequestTraceStage() {}

  public PipelineRequestTraceStage(String name, long durationMs, String status, String detail) {
    this.name = name;
    this.durationMs = durationMs;
    this.status = status;
    this.detail = detail;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(long durationMs) {
    this.durationMs = durationMs;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineRequestTraceStage that)) {
      return false;
    }
    return durationMs == that.durationMs
        && Objects.equals(name, that.name)
        && Objects.equals(status, that.status)
        && Objects.equals(detail, that.detail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, durationMs, status, detail);
  }
}
