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

package com.percussion.rest.workflows;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Create body for one absolute aging transition between existing steps (slice 57).
 *
 * <p>{@code from}, {@code to}, and a positive {@code intervalMinutes} are required. The interval
 * is minutes, the unit on {@code IPSAgingTransition.setInterval}. Does not create steps, change an
 * existing interval, or set the comment-required flag. Jackson root wrap is {@code
 * WorkflowAgingTransitionWrite}.
 */
@XmlRootElement(name = "WorkflowAgingTransitionWrite")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Absolute aging transition create body between existing steps")
public class WorkflowAgingTransitionWrite {

  @Schema(required = true, description = "Source step name. Must already exist on the workflow.")
  private String from;

  @Schema(required = true, description = "Destination step name. Must already exist on the workflow.")
  private String to;

  @Schema(
      required = true,
      description = "Aging interval in minutes. Must be a positive whole number. Absolute type only.")
  private long intervalMinutes;

  public WorkflowAgingTransitionWrite() {}

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public long getIntervalMinutes() {
    return intervalMinutes;
  }

  public void setIntervalMinutes(long intervalMinutes) {
    this.intervalMinutes = intervalMinutes;
  }
}
