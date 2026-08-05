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

package com.percussion.servlet_utils.servlet;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Production-safe mock {@link HttpServletRequest} for internal request synthesis. Replaces Spring
 * {@code MockHttpServletRequest} so {@code spring-test} stays test-only.
 */
public class PSMockHttpServletRequest implements HttpServletRequest {

  private final Map<String, Object> attributes = new HashMap<>();
  private final Map<String, String[]> parameters = new LinkedHashMap<>();
  private final Map<String, List<String>> headers = new LinkedHashMap<>();
  private final List<Cookie> cookies = new ArrayList<>();

  private String method = "GET";
  private String scheme = "http";
  private String serverName = "localhost";
  private int serverPort = 80;
  private String contextPath = "";
  private String servletPath = "";
  private String pathInfo;
  private String queryString;
  private String requestURI = "";
  private String protocol = "HTTP/1.1";
  private String remoteAddr = "127.0.0.1";
  private String remoteHost = "localhost";
  private String remoteUser;
  private String authType;
  private String characterEncoding = "UTF-8";
  private String contentType;
  private byte[] content = new byte[0];
  private boolean secure;
  private Principal userPrincipal;
  private HttpSession session;
  private ServletContext servletContext;
  private Locale locale = Locale.getDefault();

  /** No-op constructor. */
  public PSMockHttpServletRequest() {}

  /**
   * Convenience constructor that seeds the request method and URI.
   *
   * @param method the HTTP method, defaults to {@code GET} when {@code null}
   * @param requestURI the request URI, defaults to empty when {@code null}
   */
  @SuppressWarnings("this-escape")
  public PSMockHttpServletRequest(String method, String requestURI) {
    this.method = method != null ? method : "GET";
    setRequestURI(requestURI != null ? requestURI : "");
  }

  /**
   * Sets the HTTP method.
   *
   * @param method the method
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * Sets the URL scheme.
   *
   * @param scheme the scheme
   */
  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  /**
   * Sets the server name.
   *
   * @param serverName the server name
   */
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  /**
   * Sets the server port.
   *
   * @param serverPort the server port
   */
  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * Sets the context path.
   *
   * @param contextPath the context path, never {@code null}
   */
  public void setContextPath(String contextPath) {
    this.contextPath = contextPath != null ? contextPath : "";
  }

  /**
   * Sets the servlet path.
   *
   * @param servletPath the servlet path, never {@code null}
   */
  public void setServletPath(String servletPath) {
    this.servletPath = servletPath != null ? servletPath : "";
  }

  /**
   * Sets the path info.
   *
   * @param pathInfo the path info
   */
  public void setPathInfo(String pathInfo) {
    this.pathInfo = pathInfo;
  }

  /**
   * Sets the query string.
   *
   * @param queryString the query string
   */
  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  /**
   * Sets the request URI.
   *
   * @param requestURI the request URI, never {@code null}
   */
  public void setRequestURI(String requestURI) {
    this.requestURI = requestURI != null ? requestURI : "";
  }

  /**
   * Sets the request protocol.
   *
   * @param protocol the protocol
   */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /**
   * Sets the remote client address.
   *
   * @param remoteAddr the remote address
   */
  public void setRemoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
  }

  /**
   * Sets the remote client host.
   *
   * @param remoteHost the remote host
   */
  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  /**
   * Sets the remote authenticated user.
   *
   * @param remoteUser the remote user
   */
  public void setRemoteUser(String remoteUser) {
    this.remoteUser = remoteUser;
  }

  /**
   * Sets the authentication scheme.
   *
   * @param authType the auth type
   */
  public void setAuthType(String authType) {
    this.authType = authType;
  }

  /**
   * Sets whether the request was made over a secure transport.
   *
   * @param secure {@code true} for HTTPS-like requests
   */
  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  /**
   * Sets the authenticated user principal.
   *
   * @param userPrincipal the principal, may be {@code null}
   */
  public void setUserPrincipal(Principal userPrincipal) {
    this.userPrincipal = userPrincipal;
  }

  /**
   * Sets the request content type.
   *
   * @param contentType the content type
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Sets the request body content.
   *
   * @param content the body bytes, never {@code null}
   */
  public void setContent(byte[] content) {
    this.content = content != null ? content : new byte[0];
  }

  /**
   * Sets a single-valued parameter, removing any existing values if {@code value} is {@code null}.
   *
   * @param name the parameter name
   * @param value the parameter value, or {@code null} to remove
   */
  public void setParameter(String name, String value) {
    if (name == null) {
      return;
    }
    if (value == null) {
      parameters.remove(name);
    } else {
      parameters.put(name, new String[] {value});
    }
  }

  /**
   * Sets a multi-valued parameter, removing any existing values if {@code values} is {@code null}.
   *
   * @param name the parameter name
   * @param values the parameter values
   */
  public void setParameter(String name, String... values) {
    if (name == null) {
      return;
    }
    if (values == null) {
      parameters.remove(name);
    } else {
      parameters.put(name, values);
    }
  }

  /**
   * Replaces all parameters with the supplied map.
   *
   * @param params the new parameter map, may be {@code null}
   */
  public void setParameters(Map<String, String[]> params) {
    parameters.clear();
    if (params != null) {
      parameters.putAll(params);
    }
  }

  /**
   * Appends a header value to the list of headers for {@code name}.
   *
   * @param name the header name
   * @param value the header value
   */
  public void addHeader(String name, String value) {
    headers.computeIfAbsent(normalize(name), k -> new ArrayList<>()).add(value);
  }

  /**
   * Sets the active HTTP session.
   *
   * @param session the session, may be {@code null}
   */
  public void setSession(HttpSession session) {
    this.session = session;
  }

  /**
   * Sets the active servlet context.
   *
   * @param servletContext the servlet context
   */
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  public String getAuthType() {
    return authType;
  }

  @Override
  public Cookie[] getCookies() {
    return cookies.isEmpty() ? null : cookies.toArray(Cookie[]::new);
  }

  @Override
  public long getDateHeader(String name) {
    String value = getHeader(name);
    if (value == null) {
      return -1L;
    }
    return Long.parseLong(value);
  }

  @Override
  public String getHeader(String name) {
    List<String> values = headers.get(normalize(name));
    return values == null || values.isEmpty() ? null : values.get(0);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    List<String> values = headers.get(normalize(name));
    return Collections.enumeration(values == null ? List.of() : values);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(headers.keySet());
  }

  @Override
  public int getIntHeader(String name) {
    String value = getHeader(name);
    return value == null ? -1 : Integer.parseInt(value);
  }

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public String getPathInfo() {
    return pathInfo;
  }

  @Override
  public String getPathTranslated() {
    return pathInfo;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public String getQueryString() {
    return queryString;
  }

  @Override
  public String getRemoteUser() {
    return remoteUser;
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    return userPrincipal;
  }

  @Override
  public String getRequestedSessionId() {
    return session != null ? session.getId() : null;
  }

  @Override
  public String getRequestURI() {
    return requestURI;
  }

  @Override
  public StringBuffer getRequestURL() {
    StringBuffer url = new StringBuffer();
    url.append(scheme).append("://").append(serverName);
    if (serverPort > 0
        && !(("http".equalsIgnoreCase(scheme) && serverPort == 80)
            || ("https".equalsIgnoreCase(scheme) && serverPort == 443))) {
      url.append(':').append(serverPort);
    }
    url.append(requestURI != null ? requestURI : "");
    return url;
  }

  @Override
  public String getServletPath() {
    return servletPath;
  }

  @Override
  public HttpSession getSession(boolean create) {
    if (session == null && create) {
      session = new PSMockHttpSession();
    }
    return session;
  }

  @Override
  public HttpSession getSession() {
    return getSession(true);
  }

  @Override
  public String changeSessionId() {
    session = new PSMockHttpSession();
    return session.getId();
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return session != null;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return true;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return false;
  }

  @Override
  public void login(String username, String password) throws ServletException {
    // no-op
  }

  @Override
  public void logout() throws ServletException {
    // no-op
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return List.of();
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
      throws IOException, ServletException {
    throw new UnsupportedOperationException("upgrade not supported on mock request");
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public void setCharacterEncoding(String env) {
    this.characterEncoding = env;
  }

  @Override
  public int getContentLength() {
    return content.length;
  }

  @Override
  public long getContentLengthLong() {
    return content.length;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream in = new ByteArrayInputStream(content);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return in.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(jakarta.servlet.ReadListener readListener) {
        // no-op
      }

      @Override
      public int read() {
        return in.read();
      }
    };
  }

  @Override
  public String getParameter(String name) {
    String[] values = parameters.get(name);
    return values == null || values.length == 0 ? null : values[0];
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(parameters.keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return parameters.get(name);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return Collections.unmodifiableMap(parameters);
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public int getServerPort() {
    return serverPort;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding));
  }

  @Override
  public String getRemoteAddr() {
    return remoteAddr;
  }

  @Override
  public String getRemoteHost() {
    return remoteHost;
  }

  @Override
  public void setAttribute(String name, Object o) {
    if (o == null) {
      attributes.remove(name);
    } else {
      attributes.put(name, o);
    }
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return Collections.enumeration(List.of(locale));
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return servletContext != null ? servletContext.getRequestDispatcher(path) : null;
  }

  @Override
  public int getRemotePort() {
    return 0;
  }

  @Override
  public String getLocalName() {
    return serverName;
  }

  @Override
  public String getLocalAddr() {
    return "127.0.0.1";
  }

  @Override
  public int getLocalPort() {
    return serverPort;
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    throw new IllegalStateException("async not supported on mock request");
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException {
    throw new IllegalStateException("async not supported on mock request");
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new IllegalStateException("async not supported on mock request");
  }

  @Override
  public DispatcherType getDispatcherType() {
    return DispatcherType.REQUEST;
  }

  @Override
  public String getRequestId() {
    return "mock-request";
  }

  @Override
  public String getProtocolRequestId() {
    return "";
  }

  @Override
  public ServletConnection getServletConnection() {
    throw new UnsupportedOperationException("getServletConnection not supported on mock request");
  }

  private static String normalize(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT);
  }

  /** Minimal session used when {@link #getSession(boolean)} creates one. */
  private static final class PSMockHttpSession implements HttpSession {
    private final Map<String, Object> values = new HashMap<>();
    private final long creationTime = System.currentTimeMillis();
    private final String id = "mock-session-" + creationTime;
    private int maxInactiveInterval = 1800;

    @Override
    public long getCreationTime() {
      return creationTime;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public long getLastAccessedTime() {
      return creationTime;
    }

    @Override
    public ServletContext getServletContext() {
      return null;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
      this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
      return maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
      return values.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
      return Collections.enumeration(values.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
      if (value == null) {
        values.remove(name);
      } else {
        values.put(name, value);
      }
    }

    @Override
    public void removeAttribute(String name) {
      values.remove(name);
    }

    @Override
    public void invalidate() {
      values.clear();
    }

    @Override
    public boolean isNew() {
      return true;
    }
  }
}
