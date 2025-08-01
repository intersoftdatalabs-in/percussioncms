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

import com.percussion.conn.PSServerException;
import com.percussion.data.PSXslStyleSheetMerger;
import com.percussion.error.PSExceptionUtils;
import com.percussion.server.*;
import com.percussion.tools.Base64;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class to handle agent HTTP requests from clients.
 * Standard clients include:
 * <ul>
 * <li>Agent Manager Web client</li>
 * <li>Command line runner, potentially from a scheduler</li>
 * <li>Workflow action that does ad hoc publishing</li>
 * </ul>
 *
 * <p>The HTTP request format:</p>
 * {@code http://server:port/Rhythmyx/AgentManager/agentmanager.htm?rxagent=publish&rxagentaction=execute}
 *
 * @since Java 11
 */
public class PSAgentRequestHandler implements IPSLoadableRequestHandler {

   /** The handler name for logging and identification */
   public static final String HANDLER = "Agent Manager";

   /** HTML parameter name for CMS user ID */
   private static final String HTML_PARAM_CMS_USERID = "cmsuserid";

   /** HTML parameter name for CMS password */
   private static final String HTML_PARAM_CMS_PASSWORD = "cmspassword";

   /** Server address for URL generation */
   private static String ms_Server;

   /** Server port for URL generation */
   private static String ms_Port = "9992";

   /** Product version description */
   private static String ms_ProductVersion = "Percussion CM System ";

   /** Resource bundle for internationalization */
   private static ResourceBundle ms_Res;

   /** Request roots collection */
   private static Collection<String> ms_requestRoots;

   /** Agent manager instance */
   private PSAgentManager m_AgentManager;

   /** Configuration document */
   private Document m_ConfigDoc;

   /** Stylesheet path for response formatting */
   private String m_StyleSheetPath;

   @Override
   public void init(Collection<String> requestRoots, InputStream cfgFileIn) throws PSServerException {
      PSConsole.printMsg(HANDLER, "Initializing request handler...");

      ms_requestRoots = requestRoots;

      if (ms_Server == null) {
         ms_Server = PSServer.getHostAddress();
         try {
            ms_Port = Integer.toString(PSServer.getListenerPort());
         } catch (NumberFormatException e) {
            ms_Port = "0";
         }
      }

      // Load resource bundle
      try {
         ms_Res = PSUtils.getRes();
      } catch (MissingResourceException e) {
         PSConsole.printMsg(HANDLER, "Failed to load agent request handler resources");
         throw new PSServerException(e);
      }

      // Initialize product version
      try {
         var resourceBundle = ResourceBundle.getBundle(
            "com.percussion.server.agent.Version", Locale.getDefault());

         ms_ProductVersion += resourceBundle.getString("majorVersion") + "." +
               resourceBundle.getString("minorVersion") +
               "; Build:" + resourceBundle.getString("buildNumber");
      } catch (MissingResourceException e) {
         PSConsole.printMsg(HANDLER, "Failed to load Version.properties");
         throw new PSServerException(e);
      }

      // Read configuration and initialize agent manager
      try {
         readConfigDoc(cfgFileIn);
         m_AgentManager = new PSAgentManager(m_ConfigDoc);
      } catch (Exception e) {
         PSConsole.printMsg(HANDLER, "Failed to initialize agent manager request handler");
         throw new PSServerException(e);
      } finally {
         if (cfgFileIn != null) {
            try {
               cfgFileIn.close();
            } catch (IOException e) {
               PSConsole.printMsg(HANDLER, "Error closing configuration input stream: " +
                  PSExceptionUtils.getMessageForLog(e));
            }
         }
      }
      PSConsole.printMsg(HANDLER, "Request handler initialization completed");
   }

   /**
    * Reads the configuration XML document from the input stream.
    *
    * @param is the input stream, must not be {@code null}
    * @throws IOException if I/O error occurs or XML parsing fails
    * @throws IllegalArgumentException if input stream is {@code null}
    */
   private void readConfigDoc(InputStream is) throws IOException {
      if (is == null) {
         throw new IllegalArgumentException("Configuration input stream must not be null");
      }

      try {
         var db = PSUtils.getDocumentBuilder();
         var isource = new InputSource(is);
         m_ConfigDoc = db.parse(isource);
      } catch (Exception e) {
         throw new IOException("Failed to parse configuration document", e);
      }
   }

   @Override
   public void processRequest(PSRequest request) {
      var page = request.getRequestPage(false);

      try {
         var handlerResponse = new PSAgentHandlerResponse();

         if (!page.equalsIgnoreCase(IPSDTDAgentHandlerResponse.HANDLER_PAGE)) {
            handlerResponse.setResponse(
               IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR,
               "Invalid page requested: " + page);
            send(request, handlerResponse);
            return;
         }

         var paramMap = request.getParameters();
         if (paramMap == null || paramMap.isEmpty()) {
            handlerResponse.setResponse(
               IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR,
               "No parameters provided in request");
            send(request, handlerResponse);
            return;
         }

         if (!paramMap.containsKey(IPSDTDAgentHandlerResponse.HANDLER_PARAM_ACTION)) {
            handlerResponse.setResponse(
               IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR,
               "Action parameter is required");
            send(request, handlerResponse);
            return;
         }

         var action = getParameterAsString(paramMap, IPSDTDAgentHandlerResponse.HANDLER_PARAM_ACTION)
            .orElse("");

         if (page.equalsIgnoreCase(IPSDTDAgentHandlerResponse.HANDLER_PAGE)) {
            handleAction(action, paramMap, handlerResponse);
         } else {
            m_AgentManager.handleAction(paramMap, handlerResponse);
         }

         send(request, handlerResponse);

      } catch (Exception e) {
         PSConsole.printMsg(HANDLER, "Error processing request: " +
            PSExceptionUtils.getMessageForLog(e));
      }
   }

   /**
    * Safely extracts a string parameter from the parameter map.
    *
    * @param params the parameter map
    * @param key the parameter key
    * @return Optional containing the parameter value, or empty if not found
    */
   private Optional<String> getParameterAsString(Map<String, Object> params, String key) {
      return Optional.ofNullable(params.get(key))
         .map(Object::toString)
         .filter(StringUtils::isNotBlank);
   }

   /**
    * Handles actions meant for the Request Handler itself.
    * Most actions are delegated to the Agent Manager, but some are handled here
    * (e.g., configuration reload, status queries).
    *
    * @param action the action to handle
    * @param params the request parameters (currently unused but kept for future extensibility)
    * @param handlerResponse the response object
    */
   protected void handleAction(String action, Map<String, Object> params,
                              PSAgentHandlerResponse handlerResponse) {
      // Currently no actions are handled by the request handler
      var msg = String.format("Action '%s' for %s is not valid",
         action, IPSDTDAgentHandlerResponse.HANDLER_PAGE);
      handlerResponse.setResponse(IPSAgentHandlerResponse.RESPONSE_TYPE_ERROR, msg);
   }

   @Override
   public String getName() {
      return HANDLER;
   }

   @Override
   public Iterator<String> getRequestRoots() {
      return ms_requestRoots.iterator();
   }

   /**
    * Sends the response back to the client.
    * If the requested page extension starts with "htm", merges the response with
    * the stylesheet, otherwise sends the XML document directly.
    *
    * @param request the PSRequest object, must not be {@code null}
    * @param handlerResponse the response object, must not be {@code null}
    * @throws IOException if the response could not be sent
    */
   protected void send(PSRequest request, PSAgentHandlerResponse handlerResponse)
         throws IOException {
      var ext = request.getRequestPageExtension();

      if (ext != null &&
          ext.toLowerCase().startsWith(".htm") &&
          handlerResponse.getStyleSheet() != null) {

         try {
            var styleSheetURL = new URL("file:" + handlerResponse.getStyleSheet());

            try (var out = new ByteArrayOutputStream()) {
               new PSXslStyleSheetMerger().merge(
                  request,
                  createResponseDocument(handlerResponse),
                  out,
                  styleSheetURL,
                  null);

               try (var in = new ByteArrayInputStream(out.toByteArray())) {
                  request.getResponse().setContent(in, out.size(), "text/html");
               }
            }

         } catch (Exception e) {
            PSConsole.printMsg(HANDLER,
               "Error merging response document and stylesheet: " +
               PSExceptionUtils.getMessageForLog(e));

            // Fall back to sending raw XML
            sendXmlResponse(request, handlerResponse);
         }
      } else {
         sendXmlResponse(request, handlerResponse);
      }
   }

   /**
    * Creates an XML document from the handler response.
    *
    * @param handlerResponse the response object
    * @return the XML document
    */
   private Document createResponseDocument(PSAgentHandlerResponse handlerResponse) {
      try {
         var db = PSUtils.getDocumentBuilder();
         var doc = db.newDocument();

         var root = doc.createElement("Response");
         root.setAttribute("type", handlerResponse.isSuccess() ? "success" : "error");
         root.setAttribute("timestamp", handlerResponse.getTimestamp());

         if (handlerResponse.hasContent()) {
            root.setTextContent(handlerResponse.getResponseContent());
         }

         doc.appendChild(root);
         return doc;

      } catch (Exception e) {
         PSConsole.printMsg(HANDLER, "Error creating response document: " +
            PSExceptionUtils.getMessageForLog(e));
         return null;
      }
   }

   /**
    * Sends XML response directly to the client.
    *
    * @param request the request object
    * @param handlerResponse the response object
    * @throws IOException if sending fails
    */
   private void sendXmlResponse(PSRequest request, PSAgentHandlerResponse handlerResponse)
         throws IOException {
      var responseContent = Optional.ofNullable(handlerResponse.getResponseContent())
         .orElse("No content");

      var xmlResponse = String.format(
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
         "<Response type=\"%s\" timestamp=\"%s\">%s</Response>",
         handlerResponse.isSuccess() ? "success" : "error",
         handlerResponse.getTimestamp(),
         responseContent);

      try (var in = new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8))) {
         request.getResponse().setContent(in, xmlResponse.length(), "text/xml");
      }
   }

   @Override
   public void shutdown() {
      if (m_AgentManager != null) {
         m_AgentManager.close();
      }
      PSConsole.printMsg(HANDLER, "Agent request handler shutdown completed");
   }

   /**
    * Gets the resource bundle for internationalization.
    *
    * @return the resource bundle
    */
   protected static ResourceBundle getRes() {
      return ms_Res;
   }

   /**
    * Gets the server address.
    *
    * @return the server address
    */
   public static String getServer() {
      return ms_Server;
   }

   /**
    * Gets the server port.
    *
    * @return the server port
    */
   public static String getPort() {
      return ms_Port;
   }

   /**
    * Gets the product version.
    *
    * @return the product version string
    */
   public static String getProductVersion() {
      return ms_ProductVersion;
   }

}
