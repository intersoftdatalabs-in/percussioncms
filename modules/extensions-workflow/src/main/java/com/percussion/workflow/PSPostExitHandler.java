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

package com.percussion.workflow;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.tools.PrintNode;
import java.io.File;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Base class for result document post-exit handlers used by the workflow debugging extension. Logs
 * details of the request, its input XML document and the result document for debugging purposes.
 */
public class PSPostExitHandler implements IPSResultDocumentProcessor {
  private static final Logger log = LogManager.getLogger(PSPostExitHandler.class);

  /** Constructor */
  public PSPostExitHandler() {
    super();
  }

  public boolean canModifyStyleSheet() {
    return true;
  }

  public void init(IPSExtensionDef extensionDef, File file) throws PSExtensionException {
    // nothing to initialize
  }

  /**
   * This is the main request processing handler. It logs details of the request, its input XML
   * document and the result document to the workflow log for debugging purposes, and returns the
   * supplied result document unchanged.
   *
   * @param params the parameters for this extension, unused.
   * @param request the current request context, may be <code>null</code>.
   * @param resDoc the result XML document, may be <code>null</code>.
   * @return the supplied result document, possibly <code>null</code>.
   * @throws PSParameterMismatchException never thrown.
   * @throws PSExtensionProcessingException never thrown.
   */
  public Document processResultDocument(Object[] params, IPSRequestContext request, Document resDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    log.info("");
    log.info("             *** Beginning of Post-Document Exit Debugger ***");
    log.info("");

    if (null == request) {
      log.info("Request context is null!");
    } else {
      printRequestContext(request);

      log.info("");
      log.info("Input XML Document:");
      log.info("");
      log.info("*** Starts Here ***");

      try {
        if (null == request.getInputDocument()) log.info("   Document is empty");
        else {
          StringWriter writer = new StringWriter();
          PrintNode.printNode(request.getInputDocument(), " ", writer);
          log.info(writer.toString());
        }
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      log.info("*** Ends Here ***");
    }

    if (null != resDoc) {
      log.info("");
      log.info("Result XML Document:");
      log.info("");
      log.info("*** Starts Here ***");
      try {
        StringWriter writer = new StringWriter();
        PrintNode.printNode(resDoc, " ", writer);
        log.info(writer.toString());
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      log.info("*** Ends Here ***");
    } else log.info("   Document is empty");

    log.info("");
    log.info("             *** End of Post-Document Exit Debugger ***");
    log.info("");

    return resDoc;
  }

  /**
   * Logs the contents of the supplied request context (application name, request URL, CGI
   * variables, HTML parameters and response cookies) to the workflow log for debugging.
   *
   * @param request the request context to log, may be <code>null</code>.
   */
  public static void printRequestContext(IPSRequestContext request) {
    log.info("");
    log.info("Contents of the Request Context...");
    log.info("");

    printString(request.getCurrentApplicationName(), "Application Name");
    printString(request.getRequestFileURL(), "Request File URL");
    printString(request.getRequestPage(), "Request Page ");
    printString(request.getRequestRoot(), "Request Root");

    log.info("");
    log.info("List of CGI variables and values:");
    Enumeration headers = request.getHeaders();
    while (headers.hasMoreElements()) {
      String header = (String) headers.nextElement();
      String value = request.getCgiVariable(header);
      printString(header, value);
    }

    log.info("");
    log.info("List HTML parameters and values:");
    printMap(request.getParameters());

    log.info("");
    log.info("List Response cookies and values:");
    printMap(request.getResponseCookies());
  }

  /**
   * Logs the contents of the supplied map to the workflow log for debugging.
   *
   * @param map the map to log, may be <code>null</code>.
   */
  public static void printMap(Map map) {
    if (null == map) {
      log.info("Map containing the list is null");
      return;
    }
    Set keyset = map.keySet();
    if (null == keyset || keyset.isEmpty()) {
      log.info("List is empty");
    }

    if (keyset != null) {

      Object[] obArray = keyset.toArray();
      for (int i = 0; i < obArray.length; i++) {
        log.info("{}  {}={}", i + 1, obArray[i], map.get(obArray[i].toString()));
      }
    }
  }

  /**
   * Logs a single name/value pair to the workflow log for debugging.
   *
   * @param value the value to log, may be <code>null</code>.
   * @param name the name to associate with the value, may be <code>null</code>.
   */
  public static void printString(String value, String name) {
    log.info("{} = {}", name, value);
    log.info("");
  }
}
