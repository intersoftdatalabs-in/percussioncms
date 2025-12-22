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
 * Interface defining DTD constants for agent handler responses.
 * Contains element and attribute names used in response XML documents.
 *
 * @since Java 11
 */
public interface IPSDTDAgentHandlerResponse {

   /** Root element name for agent handler response */
   String ELEM_RESPONSE = "Response";

   /** Element name for response type */
   String ELEM_RESPONSE_TYPE = "ResponseType";

   /** Element name for response content */
   String ELEM_RESPONSE_CONTENT = "ResponseContent";

   /** Element name for error message */
   String ELEM_ERROR_MESSAGE = "ErrorMessage";

   /** Element name for success data */
   String ELEM_SUCCESS_DATA = "SuccessData";

   /** Request parameter name for the agent name */
   String HANDLER_PARAM_AGENT_NAME = "rxagent";

   /** Request parameter name for the action to execute */
   String HANDLER_PARAM_ACTION = "rxagentaction";

   /** The default handler page name */
   String HANDLER_PAGE = "agentmanager.htm";

   /** Response type value for success */
   String RESPONSE_TYPE_SUCCESS_VALUE = "success";

   /** Response type value for error */
   String RESPONSE_TYPE_ERROR_VALUE = "error";

   /** Attribute name for response timestamp */
   String ATTRIB_TIMESTAMP = "timestamp";

   /** Attribute name for agent name in response */
   String ATTRIB_AGENT = "agent";

   /** Attribute name for action name in response */
   String ATTRIB_ACTION = "action";
}
