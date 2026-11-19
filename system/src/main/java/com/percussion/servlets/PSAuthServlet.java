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

import com.percussion.security.PSRoleEntry;
import com.percussion.security.PSUserEntry;
import com.percussion.server.PSServer;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.auth.PSAuthServiceLocator;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailVerifyResult;
import com.percussion.services.auth.account.PSForgotUsernameResult;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSMfaChallengeOutcome;
import com.percussion.services.auth.mfa.PSMfaLockoutException;
import com.percussion.services.auth.mfa.PSMfaTxStatus;
import com.percussion.services.auth.mfa.PSPasskeyOptions;
import com.percussion.services.auth.mfa.PSTotpConfirmOutcome;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.services.security.PSJaasUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dedicated public servlet for unauthenticated email verification, MFA challenge/setup, and account
 * recovery flows under {@code /auth/*}.
 *
 * <p>{@code GET} renders {@code /rxauth.jsp} without mutating state. State changes execute only on
 * CSRF- and origin-validated {@code POST} requests.
 */
public class PSAuthServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger(PSAuthServlet.class);

  public static final String ATTR_VIEW = "perc.auth.view";
  public static final String ATTR_TOKEN = "perc.auth.token";
  public static final String ATTR_CSRF_TOKEN = "perc.auth.csrfToken";
  public static final String ATTR_BOOTSTRAP_JSON = "perc.auth.bootstrapJson";

  private static final String PATH_VERIFY_EMAIL = "/verify-email";
  private static final String PATH_FORGOT_USERNAME = "/forgot-username";
  private static final String PATH_CHALLENGE = "/challenge";
  private static final String PATH_CHALLENGE_TOTP = "/challenge/totp";
  private static final String PATH_CHALLENGE_RECOVERY = "/challenge/recovery-code";
  private static final String PATH_CHALLENGE_PASSKEY_OPTIONS = "/challenge/passkey/options";
  private static final String PATH_CHALLENGE_PASSKEY_VERIFY = "/challenge/passkey/verify";
  private static final String PATH_SETUP = "/setup";
  private static final String PATH_SETUP_TOTP_START = "/setup/totp/start";
  private static final String PATH_SETUP_TOTP_CONFIRM = "/setup/totp/confirm";
  private static final String PATH_SETUP_PASSKEY_START = "/setup/passkey/start";
  private static final String PATH_SETUP_PASSKEY_FINISH = "/setup/passkey/finish";
  private static final String PATH_CANCEL = "/cancel";

  private static final String JSP_AUTH_HOST = "/rxauth.jsp";
  private static final int MAX_BODY_BYTES = 65536;

  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  @FunctionalInterface
  interface SessionFinalizer {
    HttpServletRequest finalizeSession(
        HttpServletRequest req, HttpServletResponse res, Subject sub, String locale)
        throws LoginException;
  }

  private final transient IPSAccountSecurityService accountSecurityService;
  private final transient IPSMfaService mfaService;
  private final transient PSAuthCsrfGuard csrfGuard;
  private final transient SessionFinalizer sessionFinalizer;

  public PSAuthServlet() {
    this.accountSecurityService = null;
    this.mfaService = null;
    this.csrfGuard = new PSAuthCsrfGuard();
    this.sessionFinalizer = PSSecurityFilter::finalizeAuthenticatedSession;
  }

  /** Package-visible constructor for isolated M2 tests. */
  PSAuthServlet(IPSAccountSecurityService accountSecurityService, PSAuthCsrfGuard csrfGuard) {
    this.accountSecurityService =
        Objects.requireNonNull(accountSecurityService, "accountSecurityService");
    this.mfaService = null;
    this.csrfGuard = Objects.requireNonNull(csrfGuard, "csrfGuard");
    this.sessionFinalizer = PSSecurityFilter::finalizeAuthenticatedSession;
  }

  /** Package-visible constructor for isolated M3 tests. */
  PSAuthServlet(
      IPSAccountSecurityService accountSecurityService,
      IPSMfaService mfaService,
      PSAuthCsrfGuard csrfGuard,
      SessionFinalizer sessionFinalizer) {
    this.accountSecurityService = accountSecurityService;
    this.mfaService = Objects.requireNonNull(mfaService, "mfaService");
    this.csrfGuard = Objects.requireNonNull(csrfGuard, "csrfGuard");
    this.sessionFinalizer = Objects.requireNonNull(sessionFinalizer, "sessionFinalizer");
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    applySecurityHeaders(response);
    String subPath = normalizeSubPath(request.getPathInfo());
    String view = resolveGetView(subPath);
    if (view == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String token =
        PATH_VERIFY_EMAIL.equals(subPath) ? StringUtils.trimToEmpty(request.getParameter("token")) : "";
    String bootstrapJson = buildGetBootstrapJson(request, subPath);
    String csrfToken = csrfGuard.ensureSessionToken(request);

    request.setAttribute(ATTR_VIEW, view);
    request.setAttribute(ATTR_TOKEN, token);
    request.setAttribute(ATTR_CSRF_TOKEN, csrfToken);
    request.setAttribute(ATTR_BOOTSTRAP_JSON, bootstrapJson);

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
    dispatchPostRoute(request, response, subPath, params);
  }

  private void dispatchPostRoute(
      HttpServletRequest request,
      HttpServletResponse response,
      String subPath,
      Map<String, String> params)
      throws IOException {
    switch (subPath) {
      case PATH_VERIFY_EMAIL -> handleVerifyEmailPost(params.get("token"), response);
      case PATH_FORGOT_USERNAME ->
          handleForgotUsernamePost(params.get("email"), resolveClientIp(request), response);
      case PATH_CHALLENGE_TOTP ->
          handleChallengePost(request, response, params.get("code"), false);
      case PATH_CHALLENGE_RECOVERY ->
          handleChallengePost(request, response, params.get("code"), true);
      case PATH_CHALLENGE_PASSKEY_OPTIONS -> handleChallengePasskeyOptionsPost(request, response);
      case PATH_CHALLENGE_PASSKEY_VERIFY ->
          handleChallengePasskeyVerifyPost(request, response, params);
      case PATH_SETUP_TOTP_START ->
          handleSetupTotpStartPost(request, response, params.get("label"));
      case PATH_SETUP_TOTP_CONFIRM ->
          handleSetupTotpConfirmPost(request, response, params.get("totpId"), params.get("code"));
      case PATH_SETUP_PASSKEY_START -> handleSetupPasskeyStartPost(request, response, params);
      case PATH_SETUP_PASSKEY_FINISH -> handleSetupPasskeyFinishPost(request, response, params);
      case PATH_CANCEL -> handleCancelPost(request, response);
      default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private static String resolveGetView(String subPath) {
    return switch (subPath) {
      case PATH_VERIFY_EMAIL -> "verify-email";
      case PATH_FORGOT_USERNAME -> "forgot-username";
      case PATH_CHALLENGE -> "mfa-challenge";
      case PATH_SETUP -> "mfa-setup";
      default -> null;
    };
  }

  private String buildGetBootstrapJson(HttpServletRequest request, String subPath) {
    if (!PATH_CHALLENGE.equals(subPath) && !PATH_SETUP.equals(subPath)) {
      return "{}";
    }
    String txId = extractAuthTxCookie(request);
    PSMfaTxStatus tx = resolveMfaService().getTransactionStatus(txId);
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("hasValidTx", tx.valid());
    meta.put("txType", tx.txType());
    meta.put("totpAvailable", tx.hasTotp());
    meta.put("passkeyAvailable", tx.hasPasskey());
    meta.put("recoveryCodeAvailable", tx.recoveryCodesRemaining() > 0);
    return JSON.writeValueAsString(meta);
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
      writeErrorJson(response, 429, "RATE_LIMITED", ex.getMessage());
    }
  }

  private void handleChallengePost(
      HttpServletRequest request, HttpServletResponse response, String code, boolean recoveryCode)
      throws IOException {
    String txId = extractAuthTxCookie(request);
    if (StringUtils.isBlank(txId)) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session has expired. Please sign in again.");
      return;
    }
    try {
      String clientIp = resolveClientIp(request);
      PSMfaChallengeOutcome outcome =
          recoveryCode
              ? resolveMfaService().verifyChallengeRecoveryCode(txId, code, clientIp)
              : resolveMfaService().verifyChallengeTotp(txId, code, clientIp);
      if (!outcome.verified()) {
        int codeStatus =
            PSMfaChallengeOutcome.STATUS_TX_INVALID.equals(outcome.status())
                ? HttpServletResponse.SC_BAD_REQUEST
                : HttpServletResponse.SC_UNAUTHORIZED;
        writeErrorJson(response, codeStatus, outcome.status(), outcome.message());
        return;
      }

      String redirectUrl =
          completeMfaSession(
              request,
              response,
              txId,
              outcome.userName(),
              outcome.roles(),
              outcome.redirectUrl(),
              outcome.locale());
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("authenticated", true);
      body.put("redirectUrl", redirectUrl);
      body.put("recoveryCodesRemaining", outcome.recoveryCodesRemaining());
      body.put("message", outcome.message());
      writeJson(response, HttpServletResponse.SC_OK, body);
    } catch (PSMfaLockoutException ex) {
      writeErrorJson(response, 423, "MFA_LOCKED", ex.getMessage());
    } catch (PSAuthRateLimitException ex) {
      writeErrorJson(response, 429, "RATE_LIMITED", ex.getMessage());
    } catch (LoginException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "SESSION_FINALIZE_FAILED",
          "Unable to complete sign-in. Please try again.");
    }
  }

  private void handleChallengePasskeyOptionsPost(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    String txId = extractAuthTxCookie(request);
    if (StringUtils.isBlank(txId)) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session has expired. Please sign in again.");
      return;
    }
    try {
      PSPasskeyOptions opts =
          resolveMfaService().startChallengePasskey(txId, resolveRequestOrigin(request));
      writeJson(
          response,
          HttpServletResponse.SC_OK,
          Map.of("challengeTxId", opts.challengeTxId(), "optionsJson", opts.optionsJson()));
    } catch (IllegalArgumentException | IllegalStateException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          ex.getMessage());
    }
  }

  private void handleChallengePasskeyVerifyPost(
      HttpServletRequest request, HttpServletResponse response, Map<String, String> params)
      throws IOException {
    String txId = extractAuthTxCookie(request);
    if (StringUtils.isBlank(txId)) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session has expired. Please sign in again.");
      return;
    }
    try {
      PSMfaChallengeOutcome outcome =
          resolveMfaService()
              .verifyChallengePasskey(
                  txId,
                  params.get("challengeTxId"),
                  params.get("credentialJson"),
                  resolveRequestOrigin(request),
                  resolveClientIp(request));
      if (!outcome.verified()) {
        int codeStatus =
            PSMfaChallengeOutcome.STATUS_TX_INVALID.equals(outcome.status())
                ? HttpServletResponse.SC_BAD_REQUEST
                : HttpServletResponse.SC_UNAUTHORIZED;
        writeErrorJson(response, codeStatus, outcome.status(), outcome.message());
        return;
      }
      String redirectUrl =
          completeMfaSession(
              request,
              response,
              txId,
              outcome.userName(),
              outcome.roles(),
              outcome.redirectUrl(),
              outcome.locale());
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("authenticated", true);
      body.put("redirectUrl", redirectUrl);
      body.put("recoveryCodesRemaining", outcome.recoveryCodesRemaining());
      body.put("message", outcome.message());
      writeJson(response, HttpServletResponse.SC_OK, body);
    } catch (PSMfaLockoutException ex) {
      writeErrorJson(response, 423, "MFA_LOCKED", ex.getMessage());
    } catch (PSAuthRateLimitException ex) {
      writeErrorJson(response, 429, "RATE_LIMITED", ex.getMessage());
    } catch (LoginException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "SESSION_FINALIZE_FAILED",
          "Unable to complete sign-in. Please try again.");
    }
  }

  private void handleSetupPasskeyStartPost(
      HttpServletRequest request, HttpServletResponse response, Map<String, String> params)
      throws IOException {
    String txId = extractAuthTxCookie(request);
    try {
      PSPasskeyOptions opts =
          resolveMfaService()
              .startEnrollOnlyPasskey(txId, params.get("label"), resolveRequestOrigin(request));
      writeJson(
          response,
          HttpServletResponse.SC_OK,
          Map.of("challengeTxId", opts.challengeTxId(), "optionsJson", opts.optionsJson()));
    } catch (IllegalArgumentException | IllegalStateException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          ex.getMessage());
    }
  }

  private void handleSetupPasskeyFinishPost(
      HttpServletRequest request, HttpServletResponse response, Map<String, String> params)
      throws IOException {
    String txId = extractAuthTxCookie(request);
    if (StringUtils.isBlank(txId)) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "MFA setup session has expired. Sign in again to continue.");
      return;
    }
    try {
      PSTotpConfirmOutcome outcome =
          resolveMfaService()
              .completeEnrollOnlyPasskey(
                  txId,
                  params.get("challengeTxId"),
                  params.get("label"),
                  params.get("credentialJson"),
                  resolveRequestOrigin(request),
                  resolveClientIp(request));
      if (!outcome.enrolled()) {
        writeErrorJson(
            response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_CREDENTIAL", outcome.message());
        return;
      }
      String redirectUrl =
          completeMfaSession(
              request,
              response,
              txId,
              outcome.userName(),
              outcome.roles(),
              outcome.redirectUrl(),
              outcome.locale());
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("enrolled", true);
      body.put("recoveryCodes", outcome.recoveryCodes());
      body.put("redirectUrl", redirectUrl);
      body.put("message", outcome.message());
      writeJson(response, HttpServletResponse.SC_OK, body);
    } catch (PSMfaLockoutException ex) {
      writeErrorJson(response, 423, "MFA_LOCKED", ex.getMessage());
    } catch (PSAuthRateLimitException ex) {
      writeErrorJson(response, 429, "RATE_LIMITED", ex.getMessage());
    } catch (IllegalArgumentException | IllegalStateException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          ex.getMessage());
    } catch (LoginException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "SESSION_FINALIZE_FAILED",
          "Unable to complete sign-in. Please try again.");
    }
  }

  private void handleSetupTotpStartPost(
      HttpServletRequest request, HttpServletResponse response, String label) throws IOException {
    String txId = extractAuthTxCookie(request);
    try {
      PSTotpEnrollmentStart start = resolveMfaService().startEnrollOnlyTotp(txId, label);
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("totpId", start.totpId());
      body.put("otpauthUri", start.otpauthUri());
      body.put("manualSecret", start.manualSecret());
      body.put("issuer", start.issuer());
      body.put("accountName", start.accountName());
      body.put("digits", start.digits());
      body.put("periodSeconds", start.periodSeconds());
      writeJson(response, HttpServletResponse.SC_OK, body);
    } catch (IllegalArgumentException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          ex.getMessage());
    }
  }

  private void handleSetupTotpConfirmPost(
      HttpServletRequest request, HttpServletResponse response, String totpId, String code)
      throws IOException {
    String txId = extractAuthTxCookie(request);
    if (StringUtils.isBlank(txId)) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "MFA setup session has expired. Sign in again to continue.");
      return;
    }
    try {
      PSTotpConfirmOutcome outcome =
          resolveMfaService()
              .completeEnrollOnlyTotp(txId, totpId, code, resolveClientIp(request));
      if (!outcome.enrolled()) {
        writeErrorJson(
            response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_CODE", outcome.message());
        return;
      }

      String redirectUrl =
          completeMfaSession(
              request,
              response,
              txId,
              outcome.userName(),
              outcome.roles(),
              outcome.redirectUrl(),
              outcome.locale());
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("enrolled", true);
      body.put("recoveryCodes", outcome.recoveryCodes());
      body.put("redirectUrl", redirectUrl);
      body.put("message", outcome.message());
      writeJson(response, HttpServletResponse.SC_OK, body);
    } catch (PSMfaLockoutException ex) {
      writeErrorJson(response, 423, "MFA_LOCKED", ex.getMessage());
    } catch (PSAuthRateLimitException ex) {
      writeErrorJson(response, 429, "RATE_LIMITED", ex.getMessage());
    } catch (IllegalArgumentException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          ex.getMessage());
    } catch (LoginException ex) {
      writeErrorJson(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "SESSION_FINALIZE_FAILED",
          "Unable to complete sign-in. Please try again.");
    }
  }

  private void handleCancelPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String txId = extractAuthTxCookie(request);
    resolveMfaService().cancelTransaction(txId);
    HttpSession sess = request.getSession(false);
    if (sess != null) {
      sess.removeAttribute(PSSecurityFilter.ATTR_PENDING_MFA_TX);
    }
    PSSecurityFilter.setAuthTxCookie(request, response, "", 0);
    String ctxPath = StringUtils.trimToEmpty(request.getContextPath());
    String loginUrl = ctxPath.isEmpty() ? "/login" : ctxPath + "/login";
    writeJson(response, HttpServletResponse.SC_OK, Map.of("cancelled", true, "loginUrl", loginUrl));
  }

  private String completeMfaSession(
      HttpServletRequest request,
      HttpServletResponse response,
      String txId,
      String userName,
      List<String> roles,
      String rawRedirect,
      String locale)
      throws LoginException {
    Subject subject = resolveSubjectForTx(request, txId, userName, roles);
    HttpServletRequest authReq =
        sessionFinalizer.finalizeSession(request, response, subject, locale);
    PSSecurityFilter.setAuthTxCookie(request, response, "", 0);
    try {
      PSSystemAuditLogger.loginSuccess(authReq, userName);
    } catch (RuntimeException ex) {
      log.debug("Login success audit skipped after MFA: {}", ex.getMessage());
    }
    String safeRedirect = PSLoginServlet.resolveSafePostLoginRedirect(request, rawRedirect);
    String ctxPath = StringUtils.trimToEmpty(request.getContextPath());
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #50, same as PSLoginServlet) — "/" is HTTP URL context path delimiter, not an OS path.
    if (!ctxPath.isEmpty()
        && safeRedirect.startsWith("/")
        // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #50, same as PSLoginServlet) — "/" is HTTP URL context path delimiter, not an OS path.
        && !safeRedirect.startsWith(ctxPath + "/")
        && !safeRedirect.equals(ctxPath)) {
      return ctxPath + safeRedirect;
    }
    return safeRedirect;
  }

  private static Subject resolveSubjectForTx(
      HttpServletRequest request, String txId, String userName, List<String> roles) {
    HttpSession sess = request.getSession(false);
    if (sess != null
        && sess.getAttribute(PSSecurityFilter.ATTR_PENDING_MFA_TX)
            instanceof PSSecurityFilter.PendingMfaContext pending
        && Objects.equals(txId, pending.txId())
        && userName.equalsIgnoreCase(pending.userName())
        && pending.subject() != null) {
      return pending.subject();
    }
    PSRoleEntry[] roleEntries =
        roles == null
            ? new PSRoleEntry[0]
            : roles.stream().map(role -> new PSRoleEntry(role, 0)).toArray(PSRoleEntry[]::new);
    PSUserEntry entry = new PSUserEntry(userName, 0, null, roleEntries, null, null);
    return PSJaasUtils.userEntryToSubject(entry, null);
  }

  private static String extractAuthTxCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return "";
    }
    for (Cookie cookie : cookies) {
      if (PSSecurityFilter.AUTH_TX_COOKIE_NAME.equals(cookie.getName())) {
        return StringUtils.trimToEmpty(cookie.getValue());
      }
    }
    return "";
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

  private IPSMfaService resolveMfaService() {
    return mfaService != null ? mfaService : PSAuthServiceLocator.getMfaService();
  }

  private Map<String, String> parseRequestPayload(HttpServletRequest request) throws IOException {
    Map<String, String> out = new LinkedHashMap<>();
    String contentType = StringUtils.trimToEmpty(request.getContentType()).toLowerCase();
    if (contentType.contains("application/json")) {
      parseJsonBody(request, out);
    }
    copyParamIfPresent(request, out, "token");
    copyParamIfPresent(request, out, "email");
    copyParamIfPresent(request, out, "code");
    copyParamIfPresent(request, out, "totpId");
    copyParamIfPresent(request, out, "label");
    copyParamIfPresent(request, out, "challengeTxId");
    copyParamIfPresent(request, out, "credentialJson");
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
        Object val = entry.getValue();
        if (val instanceof String str) {
          out.put(entry.getKey(), str);
        } else if (val instanceof Map || val instanceof List) {
          out.put(entry.getKey(), JSON.writeValueAsString(val));
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

  private static String resolveRequestOrigin(HttpServletRequest request) {
    String origin = StringUtils.trimToNull(request.getHeader("Origin"));
    if (origin != null) {
      return origin;
    }
    String scheme = StringUtils.defaultIfBlank(request.getScheme(), "http");
    String host = request.getServerName();
    int port = request.getServerPort();
    if (StringUtils.isBlank(host)) {
      return "";
    }
    boolean defaultPort =
        ("http".equalsIgnoreCase(scheme) && port == 80)
            || ("https".equalsIgnoreCase(scheme) && port == 443)
            || port <= 0;
    return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
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

  private static void writeErrorJson(
      HttpServletResponse response, int status, String errorCode, String message)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("authenticated", false);
    body.put("error", errorCode);
    body.put("status", errorCode);
    body.put("message", StringUtils.defaultString(message));
    writeJson(response, status, body);
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
