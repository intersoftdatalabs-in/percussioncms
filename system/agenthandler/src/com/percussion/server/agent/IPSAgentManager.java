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

package com.percussion.server.agent;

import java.util.Collection;
import java.util.Map;

/**
 * Interface defining the contract for managing server agents.
 * Provides operations for handling agent actions and lifecycle management.
 *
 * @since Java 11
 */
public interface IPSAgentManager {

   /**
    * Handles an action request by delegating to the appropriate agent.
    *
    * @param params request parameters containing agent name and action details,
    *               must not be {@code null}
    * @param response response object for setting results, must not be {@code null}
    * @throws IllegalArgumentException if any required parameter is {@code null}
    */
   void handleAction(Map<String, Object> params, IPSAgentHandlerResponse response);

   /**
    * Closes all registered agents and cleans up resources.
    * This method should be called during shutdown to ensure proper cleanup.
    */
   void close();

   /**
    * Gets all registered agents.
    *
    * @return collection of all agents, never {@code null}
    */
   Collection<IPSAgent> getAllAgents();

   /**
    * Gets the number of registered agents.
    *
    * @return the agent count, always >= 0
    */
   int getAgentCount();
}
