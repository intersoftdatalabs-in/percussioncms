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
// REFACTORED: CP-JAVA11

package com.percussion.tomcat.valves;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a very simple redirector, based on the same technique used by the JBoss URL Rewrite
 * valve. The intent is to forward requests from each cm1 host to the host that contains all of the
 * delivery tier services, such as the form processor. It does this by checking the list of supplied
 * service names against the path. If the path begins with any registered service name, the request
 * is redirected to the registered targetHost. It does this by making a very low-level call to the
 * adapter's service method, which re-assigns the new host because we reset the server name.
 *
 * <p>The valve supports a couple of attributes:
 *
 * <pre>
 *      &lt;Valve className=&quot;...&quot; targetHost=&quot;...&quot; serviceNames=&quot;...&quot; /&gt;
 * </pre>
 *
 * <ol>
 *   <li>targetHost - this should be the name of the &lt;Host&gt; entry in the server.xml file that
 *       contains the delivery side apps. If not provided, defaults to localhost.
 *   <li>serviceNames - a comma separated list of all servlet names that provide delivery side
 *       services
 * </ol>
 */
public class PSSimpleRedirectorValve extends ValveBase {
  private static final Logger log = LogManager.getLogger(PSSimpleRedirectorValve.class);

  /** See class description. */
  private String targetHost = "localhost";

  /**
   * A temporary storage place for data used to generate the {@link #servletUrls} member. Set to
   * <code>null</code> when finished.
   */
  private String serviceNames;

  /**
   * All the services that we need to redirect for, with leading and trailing slashes. Never <code>
   * null</code> after {@link #start()} has been called.
   */
  private String[] servletUrls;

  private boolean started;

  public boolean isStarted() {
    return started;
  }

  @Override
  public void invoke(Request request, Response response) throws IOException {
    var matched = false;
    if (started && !targetHost.equals(request.getServerName())) {
      var path = request.getRequestPathMB();
      for (var servletUrl : servletUrls) {
        if (path.startsWithIgnoreCase(servletUrl, 0)) {
          matched = true;
          var chunk = request.getCoyoteRequest().serverName().getCharChunk();
          chunk.recycle();
          chunk.append(targetHost);
          request.getMappingData().recycle();
          try {
            request
                .getConnector()
                .getProtocolHandler()
                .getAdapter()
                .service(request.getCoyoteRequest(), response.getCoyoteResponse());
          } catch (ServletException e) {
            throw new RuntimeException("ServletException during redirect", e);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          break;
        }
      }
    }
    if (!matched) {
      try {
        getNext().invoke(request, response);
      } catch (Exception e) {
        log.error("Error invoking next valve", e);
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Provided to allow Tomcat to set this property from the attribute in server.xml.
   *
   * @param targetHost If <code>null</code> or empty, the property is not set.
   */
  public void setTargetHost(String targetHost) {
    if (targetHost != null && targetHost.trim().length() > 0) {
      this.targetHost = targetHost;
    }
  }

  /**
   * Provided to allow Tomcat to set this property from the attribute in server.xml.
   *
   * @param serviceNames If <code>null</code> or empty, the property is not set.
   */
  public void setServiceNames(String serviceNames) {
    if (serviceNames != null && serviceNames.trim().length() > 0) {
      this.serviceNames = serviceNames;
    }
  }

  /**
   * Performs some initialization.
   *
   * @throws LifecycleException if an error occurs during initialization
   */
  @Override
  public void startInternal() throws LifecycleException {
    log.info("Starting Simple Redirector valve");
    if (serviceNames == null) {
      servletUrls = new String[0];
      return;
    }

    var toker = new StringTokenizer(serviceNames, ",");
    var urls = new ArrayList<String>();
    while (toker.hasMoreTokens()) {
      var s = toker.nextToken().trim();
      if (!s.startsWith("/")) {
        s = "/" + s;
      }
      if (!s.endsWith("/")) {
        s = s + "/";
      }
      urls.add(s);
    }
    servletUrls = urls.toArray(new String[0]);
    log.info(
        "   Redirecting to {} for the following paths: {}",
        targetHost,
        Arrays.toString(servletUrls));
    serviceNames = null;

    started = true;
    if (getContainer() != null) {
      setState(LifecycleState.STARTING);
    }
  }

  @Override
  public void stopInternal() throws LifecycleException {
    started = false;
    if (getContainer() != null) {
      setState(LifecycleState.STOPPING);
    }
  }
}
