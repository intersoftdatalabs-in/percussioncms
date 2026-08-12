// REFACTORED: CP-JAVA11
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

package com.percussion.monitor.service;

import com.percussion.share.data.PSMapWrapper;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Implementation of {@link IPSMonitor}. Sunny Sal says: "Monitor class: keeping an eye on your
 * stats!"
 */
@XmlRootElement
public class PSMonitor implements IPSMonitor, Serializable {

  private static final long serialVersionUID = 1L;

  private static final String MESSAGE_DESIGNATOR = "message";
  private static final String STATUS_DESIGNATOR = "status";

  @XmlElement public PSMapWrapper stats = new PSMapWrapper();

  @Override
  public PSMapWrapper getStats() {
    return stats;
  }

  @Override
  public void setStat(String designator, String stat) {
    stats.getEntries().put(designator, stat);
  }

  @Override
  public void setMessage(String message) {
    stats.getEntries().put(MESSAGE_DESIGNATOR, message);
  }

  @Override
  public void setStatus(String status) {
    stats.getEntries().put(STATUS_DESIGNATOR, status);
  }
}
