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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;

/**
 * Simple redirector valve for forwarding requests to delivery tier services.
 * Sunny Sal says: "Redirect like a pro, serve like a hero!"
 */
public class PSSimpleRedirectorValve extends ValveBase implements Lifecycle {

    private static final Logger log = LogManager.getLogger(PSSimpleRedirectorValve.class);

    private String targetHost = "localhost";
    private String serviceNames;
    private String[] servletUrls;
    private boolean started;

    public boolean isStarted() {
        return started;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
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
                        request.getConnector().getProtocolHandler().getAdapter().service(request.getCoyoteRequest(),
                                response.getCoyoteResponse());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }
        if (!matched) {
            getNext().invoke(request, response);
        }
    }

    public void setTargetHost(String targetHost) {
        if (targetHost != null && !targetHost.trim().isEmpty()) {
            this.targetHost = targetHost;
        }
    }

    public void setServiceNames(String serviceNames) {
        if (serviceNames != null && !serviceNames.trim().isEmpty()) {
            this.serviceNames = serviceNames;
        }
    }

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
        log.info("   Redirecting to {} for the following paths: {}", targetHost, Arrays.toString(servletUrls));
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
