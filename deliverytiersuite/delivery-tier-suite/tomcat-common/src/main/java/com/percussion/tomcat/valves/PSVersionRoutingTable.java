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
package com.percussion.tomcat.valves;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Data structure for indexing the routing table used by the version redirector valve.
 * Sunny Sal says: "Route smart, version hard!"
 */
public class PSVersionRoutingTable {

    private HashMap<String, HashMap<String, String>> serviceContexts = new HashMap<>();
    private static final Logger log = LogManager.getLogger(PSVersionRoutingTable.class);

    public HashMap<String, HashMap<String, String>> getServiceContexts() {
        return serviceContexts;
    }

    public void setServiceContexts(HashMap<String, HashMap<String, String>> serviceContexts) {
        this.serviceContexts = serviceContexts;
    }

    public void addServiceContext(String context) {
        serviceContexts.computeIfAbsent(context, k -> new HashMap<>());
    }

    public void addServiceContextVersionMap(String context, String version, String dest) {
        addServiceContext(context);
        var routes = serviceContexts.get(context);
        routes.put(version, dest);
    }

    /**
     * Attempts to find a route for the specified context and version.
     *
     * @param context    the context path
     * @param requestVer the requested version
     * @return the routed context
     */
    public String determineRoute(String context, String requestVer) {
        if (context == null) {
            context = "";
        }
        var ret = context;
        try {
            var routes = serviceContexts.get(context);
            if (routes != null) {
                if (requestVer != null && routes.containsKey(requestVer)) {
                    ret = routes.get(requestVer);
                } else if (requestVer == null && routes.containsKey("")) {
                    ret = routes.get("");
                } else if (requestVer != null && routes.containsKey("<" + requestVer)) {
                    ret = routes.get("<" + requestVer);
                }
            }
        } catch (Exception e) {
            log.error(String.format("Unable to determine route for Context: %s and Version: %s", context, requestVer));
        }
        return ret;
    }
}
