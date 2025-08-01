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

/**
 * Interface defining DTD constants for agent manager configuration.
 * Contains element and attribute names used in XML configuration documents.
 *
 * @since Java 11
 */
public interface IPSDTDAgentManagerConfig {

   /** Root element name for agent manager configuration */
   String ELEM_AGENT_MANAGER_CONFIG = "AgentManagerConfig";

   /** Element name for individual agent configuration */
   String ELEM_AGENT = "Agent";

   /** Element name for agent implementation class */
   String ELEM_CLASS = "Class";

   /** Element name for agent description */
   String ELEM_DESCRIPTION = "Description";

   /** Element name for agent parameters */
   String ELEM_PARAMETERS = "Parameters";

   /** Element name for individual parameter */
   String ELEM_PARAMETER = "Parameter";

   /** Attribute name for agent name */
   String ATTRIB_NAME = "name";

   /** Attribute name for parameter value */
   String ATTRIB_VALUE = "value";

   /** Attribute name for agent enabled status */
   String ATTRIB_ENABLED = "enabled";

   /** Attribute name for agent version */
   String ATTRIB_VERSION = "version";
}
