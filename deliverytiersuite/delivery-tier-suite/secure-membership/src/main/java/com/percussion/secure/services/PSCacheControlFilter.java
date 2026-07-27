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
package com.percussion.secure.services;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
public class PSCacheControlFilter implements Filter {
  // REFACTORED: CP-JAVA11
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var resp = (HttpServletResponse) response;
    resp.setHeader("Expires", "Tue, 03 Jul 2001 06:00:00 GMT");
    resp.setHeader("Last-Modified", new Date().toString());
    resp.setHeader(
        "Cache-Control",
        "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
    resp.setHeader("Pragma", "no-cache");

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}

  @Override
  public void init(FilterConfig arg0) {}
}
