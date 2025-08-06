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

package com.percussion.server.agent;

/**
 * Interface defining response types and methods for agent handler responses.
 * Provides constants and methods for setting response data and types.
 *
 * @since Java 11
 */
public interface IPSAgentHandlerResponse {

   /** Response type indicating successful operation */
   int RESPONSE_TYPE_SUCCESS = 0;

   /** Response type indicating an error occurred */
   int RESPONSE_TYPE_ERROR = 1;

   /** Request parameter name for the agent name */
   String HANDLER_PARAM_AGENT_NAME = "rxagent";

   /** Request parameter name for the action to execute */
   String HANDLER_PARAM_ACTION = "rxagentaction";

   /** The default handler page name */
   String HANDLER_PAGE = "agentmanager.htm";

   /**
    * Sets the response with the specified type and content.
    *
    * @param responseType the response type (success or error)
    * @param responseContent the response content, may be {@code null}
    */
   void setResponse(int responseType, String responseContent);

   /**
    * Gets the current response type.
    *
    * @return the response type
    */
   int getResponseType();

   /**
    * Gets the current response content.
    *
    * @return the response content, may be {@code null}
    */
   String getResponseContent();

   /**
    * Sets the stylesheet path for response formatting.
    *
    * @param stylesheetPath the stylesheet path, may be {@code null}
    */
   void setStyleSheet(String stylesheetPath);

   /**
    * Gets the stylesheet path.
    *
    * @return the stylesheet path, may be {@code null}
    */
   String getStyleSheet();
}
