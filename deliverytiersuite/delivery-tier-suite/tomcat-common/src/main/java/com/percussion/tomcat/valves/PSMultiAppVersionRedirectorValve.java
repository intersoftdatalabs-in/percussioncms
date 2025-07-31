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

import com.percussion.error.PSExceptionUtils;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;

/**
 * Valve that redirects requests for a specified context to a different application context based on version.
 * Sunny Sal says: "Redirect like a ninja, version like a boss!"
 */
public class PSMultiAppVersionRedirectorValve extends ValveBase implements Lifecycle {

    public static final String PERC_VERSION_HEADER = "perc-version";
    private static final Logger log = LogManager.getLogger(PSMultiAppVersionRedirectorValve.class);

    private String mappingFile;
    private final Properties properties = new Properties();
    protected final ThreadLocal<Boolean> pipelining = new ThreadLocal<>();
    private final PSVersionRoutingTable routingTable = new PSVersionRoutingTable();
    private boolean started;

    public boolean isStarted() {
        return started;
    }

    public String getMappingFile() {
        return this.mappingFile;
    }

    public void setMappingFile(String mappingFile) {
        this.mappingFile = mappingFile;
    }

    @Override
    public synchronized void startInternal() throws LifecycleException {
        started = false;
        log.debug("start");
        log.info("Starting Multi App Version Redirector Valve");

        if (mappingFile != null) {
            try (var fis = new FileInputStream(new File(mappingFile))) {
                properties.load(fis);
            } catch (IOException e) {
                log.warn("Could not access the version Mapping file specified: {}. Error: {}. Multi Version Routing is disabled.",
                        mappingFile, PSExceptionUtils.getMessageForLog(e));
            }

            // Parse the property file.
            try {
                properties.stringPropertyNames().forEach(context -> {
                    var map = properties.getProperty(context).split(",");
                    if (map.length >= 2) {
                        routingTable.addServiceContextVersionMap(context, map[0], map[1]);
                    }
                });
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
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.debug("invoke");

        if (Boolean.TRUE.equals(pipelining.get())) {
            getNext().invoke(request, response);
            pipelining.remove();
            return;
        }

        // Only apply routing logic if the valve is properly initialized.
        if (started) {
            pipelining.set(Boolean.TRUE);
            var context = routingTable.determineRoute(request.getContextPath(), request.getHeader(PERC_VERSION_HEADER));

            if (!context.startsWith("/")) {
                context = "/" + context;
            }

            // Make sure we don't re-route if the context is the same as the target.
            if (!context.equals(request.getContextPath())) {
                var sbUrl = new StringBuilder(request.getRequestURL());
                var sQueryString = request.getQueryString();

                if (sQueryString != null) {
                    sbUrl.append("?").append(sQueryString);
                }

                var sUrl = sbUrl.toString().replace(request.getContextPath(), context);

                response.setStatus(SC_MOVED_PERMANENTLY);
                response.setHeader("Location", response.encodeRedirectURL(sUrl));
                pipelining.remove();
                return;
            }

            Valve nextValve = getNext();
            if (nextValve != null) {
                nextValve.invoke(request, response);
            }
        }

        // Make sure thread local is cleared.
        pipelining.remove();
    }

    @Override
    public synchronized void stopInternal() throws LifecycleException {
        started = false;
        if (getContainer() != null) {
            setState(LifecycleState.STOPPING);
        }
    }
}
