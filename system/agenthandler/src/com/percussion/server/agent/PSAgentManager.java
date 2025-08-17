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

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSConsole;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the interface {@code IPSAgentManager} and provides:
 * <ul>
 * <li>Instantiation and initialization of all agents from the configuration XML
 * document. If instantiation fails for any agent, that agent will be ignored
 * and initialization proceeds with the next in the document. Requests for
 * services from uninitialized agents are handled as though no such agent exists.
 * <li>The request handler forwards actions from a specified agent
 * to the Agent Manager via the method {@code handleAction()}. The Agent Manager
 * then requests the action from the appropriate agent.
 * <li>Can close all agents initialized by calling the {@code terminate}
 * method of the agents.
 * </ul>
 *
 * @since Java 11
 */
public class PSAgentManager implements IPSAgentManager {

   /**
    * Thread-safe map to store initialized agents.
    */
   private final Map<String, IPSAgent> agents = new ConcurrentHashMap<>();

   /**
    * Constructor. All agents registered in the configuration document are
    * created, initialized and stored in a hashmap. Any agent that fails to be
    * initialized is ignored.
    *
    * @param configDoc the configuration XML document, must not be {@code null}.
    * @throws IllegalArgumentException if configDoc is {@code null}
    */
   PSAgentManager(Document configDoc) {
      if (configDoc == null) {
         throw new IllegalArgumentException(
            "Configuration document for the agents must not be null");
      }

      var nodeList = configDoc.getElementsByTagName(IPSDTDAgentManagerConfig.ELEM_AGENT);

      // No agents are configured. That is fine!
      if (nodeList == null || nodeList.getLength() < 1) {
         return;
      }

      for (int i = 0; i < nodeList.getLength(); i++) {
         var elemAgent = (Element) nodeList.item(i);
         try {
            createAgent(elemAgent);
         } catch (Exception e) {
            PSConsole.printMsg(PSAgentRequestHandler.HANDLER, PSExceptionUtils.getMessageForLog(e));
         }
      }
   }

   /**
    * Helper function that creates the agent object and puts it in the map.
    *
    * @param elemAgent the configuration data element for the agent to be created
    * @throws ClassNotFoundException if the agent implementation class is not found in the classpath
    * @throws InstantiationException if the object is not instantiated from the class
    * @throws IllegalAccessException if the object fails to be instantiated for security reasons
    * @throws PSAgentException if Agent creation fails for any other reason
    * @throws IllegalArgumentException if the argument is {@code null}
    * @throws InvocationTargetException if the underlying constructor throws an exception
    * @throws NoSuchMethodException if a matching method is not found
    */
   private void createAgent(Element elemAgent)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException,
             PSAgentException, InvocationTargetException, NoSuchMethodException {

      if (elemAgent == null) {
         throw new IllegalArgumentException("elemAgent object must not be null");
      }

      var name = elemAgent.getAttribute(IPSDTDAgentManagerConfig.ATTRIB_NAME);
      if (StringUtils.isBlank(name)) {
         throw new PSAgentException(
            "Agent name attribute in its configuration data element must have a valid value");
      }

      var className = PSUtils.getElemValue(elemAgent, IPSDTDAgentManagerConfig.ELEM_CLASS);
      if (StringUtils.isBlank(className)) {
         throw new PSAgentException(
            "Implementing class name for the agent '" + name + "' must not be empty");
      }

      // Use modern reflection with proper exception handling
      var agentClass = Class.forName(className);
      var constructor = agentClass.getDeclaredConstructor();
      var agent = (IPSAgent) constructor.newInstance();

      agent.init(elemAgent);
      agents.put(name, agent);
   }

   @Override
   public void handleAction(Map<String, Object> params, IPSAgentHandlerResponse response) {
      if (response == null) {
         throw new IllegalArgumentException("response object must not be null");
      }

      if (params == null) {
         var msg = "Agent name must not be empty for Agent Manager to request an action from an agent";
         response.setResponse(IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR, msg);
         return;
      }

      var agentName = getParameterAsString(params, IPSDTDAgentHandlerResponse.HANDLER_PARAM_AGENT_NAME);
      if (StringUtils.isBlank(agentName)) {
         var msg = "Agent name must not be empty for Agent Manager to request an action from an agent";
         response.setResponse(IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR, msg);
         return;
      }

      var agent = getAgentByName(agentName);
      if (agent == null) {
         var msg = "Agent '" + agentName + "' is not registered by the Agent Manager";
         response.setResponse(IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR, msg);
         return;
      }

      var agentAction = getParameterAsString(params, IPSDTDAgentHandlerResponse.HANDLER_PARAM_ACTION);
      if (StringUtils.isBlank(agentAction)) {
         var msg = "Agent action name must not be empty for Agent Manager to request an action from an agent";
         response.setResponse(IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR, msg);
         return;
      }

      // Execute the agent action
      agent.executeAction(agentAction, params, response);
   }

   /**
    * Helper method to safely extract string parameters from the parameter map.
    *
    * @param params the parameter map
    * @param key the parameter key
    * @return the parameter value as string, or {@code null} if not found
    */
   private String getParameterAsString(Map<String, Object> params, String key) {
      var value = params.get(key);
      return value != null ? value.toString() : null;
   }

   /**
    * Gets an agent by its name.
    *
    * @param agentName the name of the agent
    * @return the agent instance, or {@code null} if not found
    */
   private IPSAgent getAgentByName(String agentName) {
      return agents.get(agentName);
   }

   @Override
   public void close() {
      var agentCollection = agents.values();
      for (var agent : agentCollection) {
         try {
            agent.terminate();
         } catch (Exception e) {
            PSConsole.printMsg(PSAgentRequestHandler.HANDLER,
               "Error terminating agent: " + PSExceptionUtils.getMessageForLog(e));
         }
      }
      agents.clear();
   }

   @Override
   public Collection<IPSAgent> getAllAgents() {
      return agents.values();
   }

   @Override
   public int getAgentCount() {
      return agents.size();
   }
}
