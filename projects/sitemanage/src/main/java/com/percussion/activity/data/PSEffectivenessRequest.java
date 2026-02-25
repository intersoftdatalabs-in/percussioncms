/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11
package com.percussion.activity.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.activity.service.IPSContentActivityService.PSUsageEnum;

/**
 * A request object used for getting the effectiveness data from the rest service. Extends the
 * content activity request by adding additional fields for usage and threshold.
 */
@JsonRootName(value = "EffectivenessRequest")
public class PSEffectivenessRequest extends PSContentActivityRequest {

  private PSUsageEnum usage;
  private int threshold;

  /**
   * @return the usage metric to use when calculating effectiveness.
   */
  public PSUsageEnum getUsage() {
    return usage;
  }

  /**
   * @param usage the usage to set.
   */
  public void setUsage(PSUsageEnum usage) {
    this.usage = usage;
  }

  /**
   * @return the acceptable threshold (views/changes) to use when calculating effectiveness.
   */
  public int getThreshold() {
    return threshold;
  }

  /**
   * @param threshold the threshold to set.
   */
  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
