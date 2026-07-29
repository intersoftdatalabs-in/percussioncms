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

import static jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;

import com.percussion.security.error.PSExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A valve that performs redirection of requests for a specified context to a different application
 * context based upon the version of the application.
 *
 * <p>The mappings are controlled by the contents of the version-map.properties file specified by
 * the mappingFile attribute of the Valve.
 *
 * @author natechadwick
 */
public class PSMultiAppVersionRedirectorValve extends ValveBase {

  /** Default no-argument constructor for the redirector valve. */
  public PSMultiAppVersionRedirectorValve() {
    // Default constructor for the redirector valve.
  }

  /**
   * HTTP request header carrying the requested application version used by the routing table.
   * Expected to contain a version identifier such as {@code "8.2"}.
   */
  public static final String PERC_VERSION_HEADER = "perc-version";

  private static final Logger log = LogManager.getLogger(PSMultiAppVersionRedirectorValve.class);

  // Contains a file system pointer to the mapping configuration file
  private String mappingFile;

  // Contains the mapping properties.
  private final Properties properties = new Properties();

  /**
   * Per-thread re-entrancy guard used to avoid recursing into {@link #invoke(Request, Response)}
   * while this valve is in the middle of forwarding a request. When {@code Boolean.TRUE}, routing
   * logic is skipped and the next valve in the pipeline is invoked; the flag is cleared before
   * returning.
   */
  protected final ThreadLocal<Boolean> pipelining = ThreadLocal.withInitial(() -> Boolean.FALSE);

  private final PSVersionRoutingTable routingTable = new PSVersionRoutingTable();

  private boolean started;

  /**
   * Indicates whether the valve has been started successfully and the routing table loaded.
   *
   * @return {@code true} when {@link #startInternal()} has finished initializing the valve.
   */
  public boolean isStarted() {
    return started;
  }

  /**
   * Returns the mapping file for this release.
   *
   * @return the mappingFile
   */
  public String getMappingFile() {
    return this.mappingFile;
  }

  /**
   * Specifies the mapping file for this release. The expected server.xml entry is {@code <Valve
   * className="com.percussion.tomcat.valves.PSMultiAppVersionRedirectorValve"
   * mappingFile="${catalina.base}/conf/perc/version-mappings.properties"/>}.
   *
   * @param mappingFile the mappingFile to set
   */
  public void setMappingFile(String mappingFile) {
    this.mappingFile = mappingFile;
  }

  @Override
  public synchronized void startInternal() throws LifecycleException {

    started = false;

    log.debug("start");

    log.info("Starting Multi App Version Redirector Valve");

    if (mappingFile != null) {
      try {
        var file = new File(mappingFile);
        try (var fis = Files.newInputStream(file.toPath())) {
          properties.load(fis);
        }
      } catch (IOException e) {
        log.warn(
            "Could not access the version Mapping file specified: {}. Error: {}. Multi Version"
                + " Routing is disabled.",
            mappingFile,
            PSExceptionUtils.getMessageForLog(e));
      }

      // Try to parse out the property file.
      try {
        var e = properties.propertyNames();

        while (e.hasMoreElements()) {
          var context = (String) e.nextElement();
          var map = properties.getProperty(context).split(",");

          routingTable.addServiceContextVersionMap(context, map[0], map[1]);
        }

        // if we got this far then we have a valid routine table.
        started = true;
        log.info("Routing Table initialized");

      } catch (Exception e) {
        log.error("Unable to initialize routing tables.", e);
      }
    }

    started = true;
    if (getContainer() != null) {
      setState(LifecycleState.STARTING);
    }
  }

  @Override
  public void invoke(Request request, Response response) throws IOException {
    log.debug("invoke");

    try {
      if (Boolean.TRUE.equals(pipelining.get())) {
        getNext().invoke(request, response);
        pipelining.remove();
        return;
      }

      // Only apply routing logic if the valve is properly initialized.
      if (started) {
        pipelining.set(Boolean.TRUE);
        var context =
            routingTable.determineRoute(
                request.getContextPath(), request.getHeader(PERC_VERSION_HEADER));

        if (!context.startsWith("/")) {
          context = "/" + context;
        }

        // Make sure we don't re-route if the context is the same as the target.
        if (!context.equals(request.getContextPath())) {
          var sbUrl = request.getRequestURL();
          var sQueryString = request.getQueryString();

          if (sQueryString != null) {
            sbUrl.append("?");
            sbUrl.append(sQueryString);
          }

          var sUrl = sbUrl.toString().replace(request.getContextPath(), context);

          response.setStatus(SC_MOVED_PERMANENTLY);
          response.setHeader("Location", response.encodeRedirectURL(sUrl));
          return;
        }

        var nextValve = getNext();
        if (nextValve != null) {
          nextValve.invoke(request, response);
        }
      }

      // Make sure thread local is cleared.
      pipelining.remove();
    } catch (Exception e) {
      log.error("Error invoking next valve", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void stopInternal() throws LifecycleException {
    started = false;
    if (getContainer() != null) {
      setState(LifecycleState.STOPPING);
    }
  }
}
