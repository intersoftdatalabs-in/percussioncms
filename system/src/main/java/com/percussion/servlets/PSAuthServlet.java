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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.servlets;

import com.percussion.server.PSServer;
import com.percussion.services.auth.PSAuthServiceLocator;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailVerifyResult;
import com.percussion.services.auth.account.PSForgotUsernameResult;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dedicated public servlet for unauthenticated email verification and account recovery flows under
 * {@code /auth/*}.
 *
 * <p>{@code GET} renders {@code /rxauth.jsp} without mutating state so email link scanners cannot
 * pre-consume single-use tokens. State changes execute only on CSRF- and origin-validated {@code
 * POST} requests.
 */
public class PSAuthServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger(PSAuthServlet.class);

  public static final String ATTR_VIEW = "perc.auth.view";
  public static final String ATTR_TOKEN = "perc.auth.token";
  public static final String ATTR_CSRF_TOKEN = "perc.auth.csrfToken";

  private static final String PATH_VERIFY_EMAIL = "/verify-email";
  private static final String PATH_FORGOT_USERNAME = "/forgot-username";
  private static final String JSP_AUTH_HOST = "/rxauth.jsp";
  private static final int MAX_BODY_BYTES = 8192;

  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final transient IPSAccountSecurityService accountSecurityService;
  private final transient PSAuthCsrfGuard csrfGuard;

  public PSAuthServlet() {
    this.accountSecurityService = null;
    this.csrfGuard = new PSAuthCsrfGuard();
  }

  /** Package-visible constructor for isolated servlet tests. */
  PSAuthServlet(IPSAccountSecurityService accountSecurityService, PSAuthCsrfGuard csrfGuard) {
    this.accountSecurityService =
        Objects.requireNonNull(accountSecurityService, "accountSecurityService");
    this.csrfGuard = Objects.requireNonNull(csrfGuard, "csrfGuard");
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    applySecurityHeaders(response);
    String subPath = normalizeSubPath(request.getPathInfo());
    if (!PATH_VERIFY_EMAIL.equals(subPath) && !PATH_FORGOT_USERNAME.equals(subPath)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String view = PATH_VERIFY_EMAIL.equals(subPath) ? "verify-email" : "forgot-username";
    String token =
        PATH_VERIFY_EMAIL.equals(subPath) ? StringUtils.trimToEmpty(request.getParameter("token")) : "";
    String csrfToken = csrfGuard.ensureSessionToken(request);

    request.setAttribute(ATTR_VIEW, view);
    request.setAttribute(ATTR_TOKEN, token);
    request.setAttribute(ATTR_CSRF_TOKEN, csrfToken);

    RequestDispatcher dispatcher = request.getRequestDispatcher(JSP_AUTH_HOST);
    if (dispatcher == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    dispatcher.forward(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    applySecurityHeaders(response);
    if (!csrfGuard.validatePostRequest(request)) {
      writeJson(
          response,
          HttpServletResponse.SC_FORBIDDEN,
          Map.of(
              "error",
              "CSRF_OR_ORIGIN_INVALID",
              "message",
              "Invalid request origin or security token. Reload the page and try again."));
      return;
    }

    String subPath = normalizeSubPath(request.getPathInfo());
    Map<String, String> params = parseRequestPayload(request);
    if (PATH_VERIFY_EMAIL.equals(subPath)) {
      handleVerifyEmailPost(params.get("token"), response);
      return;
    }
    if (PATH_FORGOT_USERNAME.equals(subPath)) {
      handleForgotUsernamePost(params.get("email"), resolveClientIp(request), response);
      return;
    }
    response.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  private void handleVerifyEmailPost(String rawToken, HttpServletResponse response)
      throws IOException {
    PSEmailVerifyResult result = resolveAccountSecurityService().confirmEmailVerification(rawToken);
    int status = mapVerifyStatus(result.status());
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("verified", result.verified());
    body.put("status", result.status());
    body.put("message", result.message());
    writeJson(response, status, body);
  }

  private void handleForgotUsernamePost(
      String email, String clientIp, HttpServletResponse response) throws IOException {
    try {
      PSForgotUsernameResult result =
          resolveAccountSecurityService().requestForgotUsername(email, clientIp);
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("accepted", result.accepted());
      body.put("message", result.message());
      writeJson(response, HttpServletResponse.SC_OK, body);
    } catch (PSAuthRateLimitException ex) {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("accepted", false);
      body.put("message", ex.getMessage());
      writeJson(response, 429, body);
    }
  }

  private static int mapVerifyStatus(String status) {
    if (PSEmailVerifyResult.STATUS_VERIFIED.equals(status)) {
      return HttpServletResponse.SC_OK;
    }
    if (PSEmailVerifyResult.STATUS_PERSIST_FAILED.equals(status)) {
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
    return HttpServletResponse.SC_BAD_REQUEST;
  }

  private IPSAccountSecurityService resolveAccountSecurityService() {
    return accountSecurityService != null
        ? accountSecurityService
        : PSAuthServiceLocator.getAccountSecurityService();
  }

  private Map<String, String> parseRequestPayload(HttpServletRequest request) throws IOException {
    Map<String, String> out = new LinkedHashMap<>();
    String contentType = StringUtils.trimToEmpty(request.getContentType()).toLowerCase();
    if (contentType.contains("application/json")) {
      parseJsonBody(request, out);
    }
    copyParamIfPresent(request, out, "token");
    copyParamIfPresent(request, out, "email");
    return out;
  }

  private void parseJsonBody(HttpServletRequest request, Map<String, String> out)
      throws IOException {
    String raw = readBoundedBody(request.getInputStream());
    if (StringUtils.isBlank(raw)) {
      return;
    }
    try {
      Map<String, Object> parsed = JSON.readValue(raw, MAP_TYPE);
      if (parsed == null) {
        return;
      }
      for (Map.Entry<String, Object> entry : parsed.entrySet()) {
        if (entry.getValue() instanceof String str) {
          out.put(entry.getKey(), str);
        }
      }
    } catch (RuntimeException ex) {
      log.debug("Ignoring malformed JSON body on /auth request: {}", ex.getMessage());
    }
  }

  private static String readBoundedBody(InputStream in) throws IOException {
    if (in == null) {
      return "";
    }
    byte[] buffer = in.readNBytes(MAX_BODY_BYTES + 1);
    if (buffer.length > MAX_BODY_BYTES) {
      throw new IOException("Request body exceeds maximum size");
    }
    return new String(buffer, StandardCharsets.UTF_8);
  }

  private static void copyParamIfPresent(
      HttpServletRequest request, Map<String, String> out, String name) {
    if (!out.containsKey(name)) {
      String param = request.getParameter(name);
      if (param != null) {
        out.put(name, param);
      }
    }
  }

  private static String normalizeSubPath(String rawPathInfo) {
    String path = StringUtils.trimToEmpty(rawPathInfo);
    while (path.endsWith("/") && path.length() > 1) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  private static String resolveClientIp(HttpServletRequest request) {
    if (PSServer.getServerProps() != null && PSServer.isRequestBehindProxy(request)) {
      String forwarded = StringUtils.trimToNull(request.getHeader("X-Forwarded-For"));
      if (forwarded != null) {
        int comma = forwarded.indexOf(',');
        String first = comma > 0 ? forwarded.substring(0, comma).trim() : forwarded;
        if (!first.isEmpty()) {
          return first;
        }
      }
    }
    return StringUtils.defaultIfBlank(request.getRemoteAddr(), "unknown");
  }

  private static void applySecurityHeaders(HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("X-Content-Type-Options", "nosniff");
  }

  private static void writeJson(
      HttpServletResponse response, int status, Map<String, Object> payload) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String json = JSON.writeValueAsString(payload);
    response.getWriter().write(json);
  }
}
