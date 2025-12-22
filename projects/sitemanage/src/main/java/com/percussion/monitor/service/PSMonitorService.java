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

import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * REST service for monitor management. Sunny Sal says: "Monitor service: keeping your system in
 * check, one stat at a time!"
 */
@Path("/monitor")
@Component("monitorService")
@Lazy
public class PSMonitorService {

  private static final Map<String, PSMonitor> monitors =
      new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private static final PSMonitorDesignators designatorNames = new PSMonitorDesignators();

  /**
   * Registers a monitor with the given designation and name.
   *
   * @param monitorDesignation the unique designation
   * @param name the monitor name
   * @return the registered monitor
   */
  public static IPSMonitor registerMonitor(String monitorDesignation, String name) {
    if (monitorDesignation == null || monitorDesignation.equalsIgnoreCase("all")) {
      throw new IllegalArgumentException("Illegal monitor designator");
    }
    if (monitors.containsKey(monitorDesignation)) {
      return monitors.get(monitorDesignation);
    }
    var monitor = new PSMonitor();
    monitor.setStat("name", name);
    monitor.setStat("designator", monitorDesignation);
    monitors.put(monitorDesignation, monitor);
    designatorNames.designator.add(monitorDesignation);
    monitor.setMessage("No information available");
    return monitor;
  }

  @GET
  @Path("/list")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public static PSMonitorDesignators getMonitorDesignators() {
    return designatorNames;
  }

  @GET
  @Path("/{monitorDesignator}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public static IPSMonitor getMonitor(@PathParam("monitorDesignator") String monitorDesignator) {
    if (!monitors.containsKey(monitorDesignator)) {
      registerMonitor(monitorDesignator, "No Name Available");
    }
    return monitors.get(monitorDesignator);
  }

  @GET
  @Path("/all")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public static PSMonitorList getMonitors() {
    var monitorList = new PSMonitorList();
    monitorList.addEntriesToList(monitors);
    return monitorList;
  }
}
