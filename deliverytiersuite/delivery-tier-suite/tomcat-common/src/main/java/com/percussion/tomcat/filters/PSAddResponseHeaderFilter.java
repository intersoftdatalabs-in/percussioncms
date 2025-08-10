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
package com.percussion.tomcat.filters;

import com.percussion.security.error.PSExceptionUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.CacheControl;

public class PSAddResponseHeaderFilter implements Filter {

  private static final Logger log = LogManager.getLogger(PSAddResponseHeaderFilter.class);

  private static final String PERC_SECURITY_PROPS_ROOT = "/conf/perc/perc-security.properties";
  private static final String CATALINA_BASE = "catalina.base";
  private Long cachingAgeTimeValue = null;
  private TimeUnit cachingAgeTimeUnit = null;
  private static final String CACHING_MAX_AGE_VALUE_PROPERTY_KEY = "cacheControlMaxAgeValue";
  private static final String CACHING_MAX_AGE_UNIT_PROPERTY_KEY = "cacheControlMaxAgeUnit";
  private static final String PERC_SECURITY_PROPERTIES = "/WEB-INF/perc-security.properties";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
      var httpResp = (HttpServletResponse) response;

      // If global perc-security.properties does not have cache-control properties then look into
      // service level perc-security.properties.
      // This will help service level configuration by removing the global value and configuring
      // each service level values. If service level values do not exist set default value.
      if (Objects.isNull(cachingAgeTimeValue) || Objects.isNull(cachingAgeTimeUnit)) {
        var contextProps = new Properties();
        var contextPath = request.getServletContext();
        try (var in = contextPath.getResourceAsStream(PERC_SECURITY_PROPERTIES)) {
          if (Objects.nonNull(in)) {
            contextProps.load(in);
          }
        }

        var contextCachingAgeTimeVal = contextProps.getProperty(CACHING_MAX_AGE_VALUE_PROPERTY_KEY);
        if (Objects.nonNull(contextCachingAgeTimeVal) && !contextCachingAgeTimeVal.isBlank()) {
          cachingAgeTimeValue = Long.parseLong(contextCachingAgeTimeVal);
        } else {
          cachingAgeTimeValue = 60L;
        }

        var contextCachingAgeUnitVal = contextProps.getProperty(CACHING_MAX_AGE_UNIT_PROPERTY_KEY);
        if (Objects.nonNull(contextCachingAgeUnitVal) && !contextCachingAgeUnitVal.isBlank()) {
          cachingAgeTimeUnit = TimeUnit.valueOf(contextCachingAgeUnitVal);
        } else {
          cachingAgeTimeUnit = TimeUnit.SECONDS;
        }
      }

      httpResp.setHeader(
          "Cache-Control",
          CacheControl.maxAge(cachingAgeTimeValue, cachingAgeTimeUnit).getHeaderValue());
      chain.doFilter(request, response);
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    var props = new Properties();
    // Find in local Webapp,
    var tomcatBase = System.getProperty(CATALINA_BASE);
    if (Objects.nonNull(tomcatBase)) {
      try (var in = new FileInputStream(tomcatBase + PERC_SECURITY_PROPS_ROOT)) {
        props.load(in);
      } catch (IOException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }

    var cachingAgeTimeVal = props.getProperty(CACHING_MAX_AGE_VALUE_PROPERTY_KEY);
    if (Objects.nonNull(cachingAgeTimeVal) && !cachingAgeTimeVal.isBlank()) {
      cachingAgeTimeValue = Long.parseLong(cachingAgeTimeVal);
    }

    var cachingAgeUnitVal = props.getProperty(CACHING_MAX_AGE_UNIT_PROPERTY_KEY);
    if (Objects.nonNull(cachingAgeUnitVal) && !cachingAgeUnitVal.isBlank()) {
      cachingAgeTimeUnit = TimeUnit.valueOf(cachingAgeUnitVal);
    }
  }

  @Override
  public void destroy() {
    // No resources to clean up
  }
}
