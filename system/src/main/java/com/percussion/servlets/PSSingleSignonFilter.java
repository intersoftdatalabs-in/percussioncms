/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A filter designed to redirect the requests to the percussion cm server login page if the user is
 * not authenticated otherwise to the requested page.
 *
 * @author bjoginipally
 */
public class PSSingleSignonFilter implements Filter {
  /**
   * Finds the pssessionid cookie from the cookies and calls the {@link
   * #validateSession(String)}method to validate the session, if not validated redirects to
   * percussion cm server login page by passing in the requested url as sys_redirect parameter.
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request == null) {
      throw new IllegalArgumentException("request may not be null");
    }
    if (response == null) {
      throw new IllegalArgumentException("response may not be null");
    }
    if (chain == null) {
      throw new IllegalArgumentException("chain may not be null");
    }

    HttpServletRequest httpReq = null;
    HttpServletResponse httpResp = null;
    if (request instanceof HttpServletRequest) {
      httpReq = (HttpServletRequest) request;
      httpResp = (HttpServletResponse) response;
      Cookie[] cookies = httpReq.getCookies();
      String pssessid = null;
      if (cookies != null) {
        for (Cookie c : cookies) {
          if (c.getName().toLowerCase().equalsIgnoreCase("pssessionid")) pssessid = c.getValue();
        }
      }
      boolean authenticated = false;
      if (pssessid != null) {
        authenticated = validateSession(pssessid, prepareBaseUrl(httpReq));
      }

      if (!authenticated) {
        String forcedRedirectUrl = m_config.getInitParameter("forcedRedirectUrl");
        String redirectUrl = httpReq.getRequestURL().toString();
        if (forcedRedirectUrl != null && forcedRedirectUrl.length() > 0) {
          StringBuilder buff = new StringBuilder();
          URL temp = new URL(redirectUrl);
          buff.append(temp.getProtocol());
          buff.append("://");
          buff.append(temp.getHost());
          if (temp.getPort() != 80) {
            buff.append(":");
            buff.append(temp.getPort());
          }
          buff.append(forcedRedirectUrl);
          redirectUrl = buff.toString();
        }
        String redirect =
            "/"
                + SSOSERVER_APPCONTEXT_ROOT
                + "/login?sys_redirect="
                + URLEncoder.encode(redirectUrl + "?" + httpReq.getQueryString(), "UTF-8");
        httpResp.sendRedirect(redirect);
      }
    }
    chain.doFilter(httpReq, httpResp);
  }

  /**
   * Helper method to create the base url from the request. The returned URL does not include the
   * trailing /. Example http(s)://<Server>:<Port>. The port part is included only if it is not a
   * default port for the protocol.
   *
   * @param httpReq assumed not <code>null</code>.
   * @return The base url fo the request, never <code>null</code>.
   */
  private String prepareBaseUrl(HttpServletRequest httpReq) {

    //  Not sure how this ever worked properly.  Need to refactor how we get local and remove base
    // urls
    //  taking into account proxies;
    String baseUrl = httpReq.isSecure() ? "https://" : "http://";
    baseUrl += PSServer.getHostName();
    // Check whether we need to add port
    boolean addPort =
        httpReq.isSecure() ? httpReq.getServerPort() != 443 : httpReq.getServerPort() != 80;
    if (addPort) {
      baseUrl += ":" + httpReq.getServerPort();
    }
    return baseUrl;
  }

  /**
   * Helper method to make a http client request to Rhythmyx server to validate the passed in
   * session. Incase of an exception validating the session with Rhythmyx, logs the error and
   * returns <code>false</code>.
   *
   * @param pssessid The session id to be validated, assumed not <code>null</code>.
   * @return <code>true</code> if the session is valid otherwise <code>false</code>.
   */
  private boolean validateSession(String pssessid, String baseUrl) {
    // make call to server
    boolean valid = false;
    try {
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(
                      baseUrl
                          + "/"
                          + SSOSERVER_APPCONTEXT_ROOT
                          + "/__validateSession__?pssessionid="
                          + URLEncoder.encode(pssessid, StandardCharsets.UTF_8)))
              .GET()
              .timeout(Duration.ofSeconds(10))
              .build();
      int respCode = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      if (respCode == 200) valid = true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ms_log.error("Interrupted while validating single sign-on session", e);
    } catch (Exception e) {
      ms_log.error(e);
    }
    return valid;
  }

  /*
   * (non-Javadoc)
   *
   * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
   */
  public void init(FilterConfig config) throws ServletException {
    m_config = config;
  }

  /*
   * (non-Javadoc)
   *
   * @see jakarta.servlet.Filter#destroy()
   */
  public void destroy() {
    // There is nothing to destroy at this point.
  }

  /**
   * A reference to the filter configuration. Initialized in {@link #init(FilterConfig)}, never
   * <code>null</code> after that.
   */
  private FilterConfig m_config;

  /** log to use, never <code>null</code>. */
  private static final Logger ms_log = LogManager.getLogger(PSSingleSignonFilter.class);

  /** Context name of Rhythmyx server application context */
  private static final String SSOSERVER_APPCONTEXT_ROOT = "Rhythmyx";

  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
}
