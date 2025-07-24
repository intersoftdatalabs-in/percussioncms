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

package com.percussion.tomcat.filters;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.CacheControl;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class PSAddResponseHeaderFilter implements Filter {

    private static final Logger log = LogManager.getLogger(PSAddResponseHeaderFilter.class);

    private static String PERC_SECURITY_PROPS_ROOT = "/conf/perc/perc-security.properties";
    private static String CATALINA_BASE = "catalina.base";
    private Long cachingAgeTimeValue = null;
    private TimeUnit cachingAgeTimeUnit = null;
    private String CACHING_MAX_AGE_VALUE_PROPERTY_KEY="cacheControlMaxAgeValue";
    private String CACHING_MAX_AGE_UNIT_PROPERTY_KEY="cacheControlMaxAgeUnit";
    private static final String PERC_SECURITY_PROPERTIES = "/WEB-INF/perc-security.properties";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            var httpResp = (HttpServletResponse) response;
            if (cachingAgeTimeValue == null || cachingAgeTimeUnit == null) {
                var contextProps = new Properties();
                var contextPath = request.getServletContext();
                try (var in = contextPath.getResourceAsStream(PERC_SECURITY_PROPERTIES)) {
                    if (in != null) {
                        contextProps.load(in);
                    }
                }
                var contextCachingAgeTimeVal = contextProps.getProperty(CACHING_MAX_AGE_VALUE_PROPERTY_KEY);
                if (contextCachingAgeTimeVal != null && !contextCachingAgeTimeVal.isBlank()) {
                    cachingAgeTimeValue = Long.parseLong(contextCachingAgeTimeVal);
                } else {
                    cachingAgeTimeValue = 60L;
                }
                var contextCachingAgeUnitVal = contextProps.getProperty(CACHING_MAX_AGE_UNIT_PROPERTY_KEY);
                if (contextCachingAgeUnitVal != null && !contextCachingAgeUnitVal.isBlank()) {
                    cachingAgeTimeUnit = TimeUnit.valueOf(contextCachingAgeUnitVal);
                } else {
                    cachingAgeTimeUnit = TimeUnit.SECONDS;
                }
            }
            httpResp.setHeader("Cache-Control", CacheControl.maxAge(cachingAgeTimeValue, cachingAgeTimeUnit).getHeaderValue());
            chain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        var props = new Properties();
        var tomcatBase = System.getProperty(CATALINA_BASE);
        if (tomcatBase != null) {
            try (var in = new FileInputStream(tomcatBase + PERC_SECURITY_PROPS_ROOT)) {
                props.load(in);
            } catch (IOException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
        }
        var cachingAgeTimeVal = props.getProperty(CACHING_MAX_AGE_VALUE_PROPERTY_KEY);
        if (cachingAgeTimeVal != null && !cachingAgeTimeVal.isBlank()) {
            cachingAgeTimeValue = Long.parseLong(cachingAgeTimeVal);
        }
        var cachingAgeUnitVal = props.getProperty(CACHING_MAX_AGE_UNIT_PROPERTY_KEY);
        if (cachingAgeUnitVal != null && !cachingAgeUnitVal.isBlank()) {
            cachingAgeTimeUnit = TimeUnit.valueOf(cachingAgeUnitVal);
        }
    }

    @Override
    public void destroy() {

    }
}
