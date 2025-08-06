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

package com.percussion.services;

import com.percussion.server.PSServer;
import com.percussion.servlets.PSContextLoaderListener;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.jndi.PSJndiObjectLocator;
import com.percussion.utils.servlet.PSServletUtils;
import com.percussion.utils.xml.PSEntityResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import java.util.Objects;
import java.util.Optional;

/**
 * The context loader loads the root spring context which is
 * first setup by {@link PSBaseServiceLocator#init(ServletContext)}.
 * <p>
 * The loader is kicked off by {@link PSContextLoaderListener}.
 * 
 * @author adamgent
 */
// REFACTORED: CP-JAVA11
public class PSContextLoader extends ContextLoader {

    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    private static final Logger log = LogManager.getLogger(PSContextLoader.class);

    /**
     * Shuts down the server and the spring context. See spring doc.
     *
     * @param servletContext The base servlet context, must not be null
     * @throws IllegalArgumentException if servletContext is null
     */
    @Override
    public void closeWebApplicationContext(ServletContext servletContext) {
        Objects.requireNonNull(servletContext, "ServletContext must not be null");

        log.info("Shutting down Web Application Context");
        PSServer.shutdown();
        super.closeWebApplicationContext(servletContext);
        log.info("Web Application Context shutdown complete");
    }

    /**
     * Initializes part of the server and then initializes spring.
     * See spring doc.
     *
     * @param servletContext The base servlet context, must not be null
     * @return the root web application context, never null
     * @throws IllegalStateException if context initialization fails
     * @throws BeansException if bean creation fails
     * @throws IllegalArgumentException if servletContext is null
     */
    @Override
    public WebApplicationContext initWebApplicationContext(ServletContext servletContext)
            throws IllegalStateException, BeansException {
        Objects.requireNonNull(servletContext, "ServletContext must not be null");

        log.info("Initializing Root Web Application Context");

        java.io.File rxDir = PathUtils.getRxDir(null);
        PSServer.setRxDir(rxDir);
        PSEntityResolver.setResolutionHome(rxDir);

        // Initialize JNDI prefix from servlet context
        Optional.ofNullable(servletContext.getInitParameter("jndiPrefix"))
            .ifPresent(PSJndiObjectLocator::setPrefix);

        PSServletUtils.initialize(servletContext);

        WebApplicationContext context = super.initWebApplicationContext(servletContext);
        log.info("Finished loading spring");
        return context;
    }

    /**
     * Delegates to PSBaseServiceLocator to load the parent application context.
     *
     * @param servletContext Base servlet context, must not be null
     * @return the parent application context, never null
     * @throws BeansException if bean loading fails
     * @throws IllegalArgumentException if servletContext is null
     */
    @Override
    protected ApplicationContext loadParentContext(ServletContext servletContext)
            throws BeansException {
        Objects.requireNonNull(servletContext, "ServletContext must not be null");

        log.info("Loading Service locators");
        PSBaseServiceLocator.init(servletContext);
        log.info("Finished loading service locators");
        return PSBaseServiceLocator.getCtx();
    }
}
