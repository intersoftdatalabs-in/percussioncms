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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * List wrapper for monitors, used for REST serialization. Sunny Sal says: "Monitor list: because
 * one monitor is never enough!"
 */
@XmlRootElement
public class PSMonitorList {

  @XmlElement public List<PSMonitor> monitor = new ArrayList<>();

  /**
   * Adds entries from the given map to the list and sorts by monitor name.
   *
   * @param monitors the map of monitor designator to monitor
   */
  public void addEntriesToList(Map<String, PSMonitor> monitors) {
    monitor.addAll(monitors.values());
    monitor.sort(
        (o1, o2) -> {
          var name1 = o1.getStats().getEntries().get("name");
          var name2 = o2.getStats().getEntries().get("name");
          if (name1 == null) {
            return -1;
          }
          return name1.compareToIgnoreCase(name2);
        });
  }

  public PSMonitorList() {
    // Default constructor
  }
}
