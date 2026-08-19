/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.webui.servlet;

import com.percussion.webui.filter.PSWebUiSpaFallbackFilter;
import com.percussion.webui.util.PSLegacyViewRedirect;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Exact-URL replacement for retired classic JSP hosts (#3587).
 *
 * <p>Jetty's {@code *.jsp} mapping still owns missing JSP files and 404s them. An exact servlet
 * mapping wins over the extension mapping so bookmarks such as {@code
 * /cm/app/siteArchitecture.jsp} 301 to SPA {@code ?view=} without shipping the JSP.
 */
public class PSRetiredJspRedirectServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  /** Init-param name for the SPA {@code view} key. */
  public static final String PARAM_VIEW = "view";

  /** SPA {@code view} key from {@code view} init-param; defaults to home if missing. */
  private String view = "home";

  /** Default constructor for servlet container instantiation. */
  public PSRetiredJspRedirectServlet() {}

  @Override
  public void init() {
    String configured = getInitParameter(PARAM_VIEW);
    if (configured != null && !configured.isBlank()) {
      view = configured.trim();
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    sendMoved(req, resp);
  }

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    sendMoved(req, resp);
  }

  private void sendMoved(HttpServletRequest req, HttpServletResponse resp) {
    String location = PSLegacyViewRedirect.buildLocation(view, req.getQueryString());
    location = PSWebUiSpaFallbackFilter.withContextPath(req.getContextPath(), location);
    resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    resp.setHeader("Location", location);
  }
}
