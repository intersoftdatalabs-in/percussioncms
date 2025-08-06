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

package com.percussion.servlets;

import com.percussion.server.PSServer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * CORS filter that configures Cross-Origin Resource Sharing settings
 * based on server properties.
 */
@Component(value = "corsFilter")
public final class PSCorsFilter implements Filter {

    private CorsFilter filter;

    /**
     * Called by the web container to indicate to a filter that it is
     * being placed into service.
     *
     * <p>The servlet container calls the init method exactly once after
     * instantiating the filter. The init method must complete successfully
     * before the filter is asked to do any filtering work.
     *
     * <p>The web container cannot place the filter into service if the init
     * method either:
     * <ol>
     * <li>Throws a ServletException
     * <li>Does not return within a time period defined by the web container
     * </ol>
     *
     * @param filterConfig the filter configuration object
     * @throws ServletException if an error occurs during initialization
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        init();
    }

    private void init() {
        var config = new CorsConfiguration();

        // Configure allowed origins
        var allowed = PSServer.getProperty("allowedOrigins", "*").trim();
        if (StringUtils.isNotBlank(allowed)) {
            if (!"*".equalsIgnoreCase(allowed)) {
                config.setAllowedOrigins(Arrays.asList(allowed.split(",", -1)));
            }
        }

        // Configure allowed methods
        var allowedMethods = PSServer.getProperty("allowedMethods",
            "GET,POST,OPTIONS,HEAD,PUT,DELETE,PATCH").trim();
        if (StringUtils.isNotBlank(allowedMethods)) {
            Arrays.stream(allowedMethods.split(",", -1))
                  .map(String::trim)
                  .forEach(config::addAllowedMethod);
        }

        // Configure allowed headers
        var allowedHeaders = PSServer.getProperty("allowedHeaders",
            "Content-Type, Access-Control-Allow-Origin, Access-Control-Allow-Headers, " +
            "Authorization, X-Requested-With, X-UA-Compatible, OWASP-CSRFTOKEN, User-Agent").trim();
        if (StringUtils.isNotBlank(allowedHeaders)) {
            Arrays.stream(allowedHeaders.split(",", -1))
                  .map(String::trim)
                  .forEach(config::addAllowedHeader);
        }

        // Configure allow credentials
        var allowCredentials = PSServer.getProperty("allowCredentials", "false").trim();
        if (StringUtils.isNotBlank(allowCredentials)) {
            config.setAllowCredentials(Boolean.parseBoolean(allowCredentials));
        }

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        filter = new CorsFilter(source);
    }

    /**
     * The {@code doFilter} method of the Filter is called by the
     * container each time a request/response pair is passed through the
     * chain due to a client request for a resource at the end of the chain.
     * The FilterChain passed in to this method allows the Filter to pass
     * on the request and response to the next entity in the chain.
     *
     * <p>A typical implementation of this method would follow the following
     * pattern:
     * <ol>
     * <li>Examine the request
     * <li>Optionally wrap the request object with a custom implementation to
     * filter content or headers for input filtering
     * <li>Optionally wrap the response object with a custom implementation to
     * filter content or headers for output filtering
     * <li>a) <strong>Either</strong> invoke the next entity in the chain
     * using the FilterChain object ({@code chain.doFilter()}),
     * <li>b) <strong>or</strong> not pass on the request/response pair to
     * the next entity in the filter chain to block the request processing
     * </ol>
     *
     * @param request  the ServletRequest object contains the client's request
     * @param response the ServletResponse object contains the filter's response
     * @param chain    the FilterChain for invoking the next filter or the resource
     * @throws IOException      if an I/O related error has occurred during the processing
     * @throws ServletException if an exception has occurred that interferes with the
     *                          filter's normal operation
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(response, "response cannot be null");
        Objects.requireNonNull(chain, "chain cannot be null");

        if (filter != null) {
            filter.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Called by the web container to indicate to a filter that it is being
     * taken out of service.
     *
     * <p>This method is only called once all threads within the filter's
     * doFilter method have exited or after a timeout period has passed.
     * After the web container calls this method, it will not call the
     * doFilter method again on this instance of the filter.
     *
     * <p>This method gives the filter an opportunity to clean up any
     * resources that are being held (for example, memory, file handles,
     * threads) and make sure that any persistent state is synchronized
     * with the filter's current state in memory.
     */
    @Override
    public void destroy() {
        if (filter != null) {
            filter.destroy();
        }
    }
}
