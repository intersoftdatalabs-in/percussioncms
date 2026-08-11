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
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import java.util.ArrayList;
/** A request object used for getting the content traffic data from the rest service. */
@JsonRootName(value = "ContentTrafficRequest")
public class PSContentTrafficRequest extends PSTrafficDetailsRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String granularity;
  private ArrayList<String> trafficRequested;

  /**
   * Get granularity of date list.
   *
   * @return Option returned is DAYS,WEEKS,MONTHS,or YEARS.
   */
  public Optional<String> getGranularity() {
    return Optional.ofNullable(granularity);
  }

  /**
   * @return trafficRequested list of types of date that is getting requested. Options are:
   *     livePages,pageUpdates,newPages,takeDowns,visits
   */
  public Optional<List<String>> getTrafficRequested() {
    return Optional.ofNullable(trafficRequested);
  }

  /**
   * Sets granularity of date list returned. Options are DAYS,WEEKS,MONTHS,YEARS.
   *
   * @param granularity
   */
  public void setGranularity(String granularity) {
    this.granularity = granularity;
  }

  /**
   * List of traffic data types that is getting requested. Options are:
   * livePages,pageUpdates,newPages,takeDowns,visits
   *
   * @param trafficRequested
   */
  @SuppressWarnings("unchecked")
  public void setTrafficRequested(List<String> trafficRequested) {
    if (trafficRequested == null) {
      this.trafficRequested = null;
    } else if (trafficRequested instanceof ArrayList) {
      this.trafficRequested = (ArrayList<String>) trafficRequested;
    } else {
      this.trafficRequested = new ArrayList<>(trafficRequested);
    }
  }
}
