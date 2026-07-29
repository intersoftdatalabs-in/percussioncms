/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.tomcat.valves;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a basic data structure for indexing the routing table used by the version redirector
 * valve.
 *
 * @author natechadwick
 */
public class PSVersionRoutingTable {

  /** Default no-argument constructor for the routing table. */
  public PSVersionRoutingTable() {
    // Default constructor for the routing table.
  }

  private Map<String, Map<String, String>> serviceContexts;
  private static final Logger log = LogManager.getLogger(PSVersionRoutingTable.class);

  /**
   * Returns the service-context map keyed by context path.
   *
   * @return the serviceContexts
   */
  public Map<String, Map<String, String>> getServiceContexts() {
    return serviceContexts;
  }

  /**
   * Replaces the service-context map keyed by context path.
   *
   * @param serviceContexts the serviceContexts to set
   */
  public void setServiceContexts(Map<String, Map<String, String>> serviceContexts) {
    this.serviceContexts = serviceContexts;
  }

  /**
   * Add a service context to the routing table
   *
   * @param context the context to add
   */
  public void addServiceContext(String context) {
    if (serviceContexts == null) {
      serviceContexts = new HashMap<>();
    }
    serviceContexts.computeIfAbsent(context, k -> new HashMap<>());
  }

  /**
   * Add a mapping between a context, version and destination
   *
   * @param context the context path
   * @param version the version identifier
   * @param dest the destination path
   */
  public void addServiceContextVersionMap(String context, String version, String dest) {
    // Make sure the context is added
    addServiceContext(context);

    var routes = serviceContexts.get(context);
    routes.put(version, dest);
    serviceContexts.put(context, routes);
  }

  /**
   * Attempts to find a route for the specified context and version.
   *
   * @param context the context path
   * @param requestVer the requested version
   * @return a context path to route to
   */
  public String determineRoute(String context, String requestVer) {
    // Default to the original request context
    var contextPath = Optional.ofNullable(context).orElse("");

    try {
      var routes = serviceContexts.get(contextPath);
      if (routes != null) {
        if (routes.containsKey(requestVer)) {
          return routes.get(requestVer);
        } else if (requestVer == null) {
          if (routes.containsKey("")) {
            return routes.get("");
          }
        } else if (routes.containsKey("<" + requestVer)) {
          return routes.get("<" + requestVer);
        }
      }
    } catch (Exception e) {
      log.error(
          "Unable to determine route for Context: {} and Version: {}", contextPath, requestVer, e);
    }

    return contextPath;
  }
}
