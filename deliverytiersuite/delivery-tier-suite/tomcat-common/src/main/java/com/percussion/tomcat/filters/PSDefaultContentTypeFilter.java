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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Sets the content type to text/html if it hasn't been set.
 *
 * @author natechadwick
 */
public class PSDefaultContentTypeFilter implements Filter {

  /** Default no-argument constructor for the default content type filter. */
  public PSDefaultContentTypeFilter() {
    // Default constructor for the default content type filter.
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (response.getContentType() == null) {
      var servletContext = request.getServletContext();
      if (request instanceof HttpServletRequest) {
        var url = ((HttpServletRequest) request).getRequestURL().toString();
        var mimeType = servletContext.getMimeType(url);
        if (mimeType == null) {
          response.setContentType("text/html; charset=UTF-8");
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // No initialization required
  }

  @Override
  public void destroy() {
    // No resources to clean up
  }
}
