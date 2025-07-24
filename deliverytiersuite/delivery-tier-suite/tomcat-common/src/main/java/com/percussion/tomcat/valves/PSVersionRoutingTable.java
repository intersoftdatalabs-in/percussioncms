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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a basic data structure for indexing the routine table 
 * used by the version redirector valve. 
 * 
 * @author natechadwick
 *
 */
public class PSVersionRoutingTable {
    private Map<String, Map<String, String>> serviceContexts = new HashMap<>();
    private static final Logger log = LogManager.getLogger(PSVersionRoutingTable.class);
    public Map<String, Map<String, String>> getServiceContexts() {
        return serviceContexts;
    }
    public void setServiceContexts(Map<String, Map<String, String>> serviceContexts) {
        this.serviceContexts = serviceContexts;
    }
    public void addServiceContextVersionMap(String context, String version, String route) {
        serviceContexts.computeIfAbsent(context, k -> new HashMap<>()).put(version, route);
    }
    public String determineRoute(String context, String version) {
        var contextMap = serviceContexts.get(context);
        if (contextMap != null && version != null && contextMap.containsKey(version)) {
            return contextMap.get(version);
        }
        return context;
    }
}
