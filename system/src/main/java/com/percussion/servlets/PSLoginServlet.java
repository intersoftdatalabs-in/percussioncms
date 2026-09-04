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

import static com.percussion.cms.IPSConstants.SECURITY_LOG;
import static com.percussion.utils.request.PSRequestInfoBase.KEY_PSREQUEST;
import static com.percussion.utils.request.PSRequestInfoBase.getRequestInfo;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.percussion.content.IPSMimeContentTypes;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.i18n.PSI18nUtils;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.utils.PSRedirectValidation;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestParsingException;
import com.percussion.server.PSServer;
import com.percussion.server.PSUserSessionManager;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.tools.PSURIEncoder;
import com.percussion.utils.tools.IPSUtilsConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This servlet will process form based login/out calls. The servlet is mapped to the
 * "/Rhythmyx/login" and "/Rhythmyx/logout" request roots. The servlet will look for pages named
 * "login.jsp", "error.jsp", and "logout.jsp" in the "&lt;webapp root&gt;/user" directory. Any or
 * all of these pages may be defined. For each found, that page will be used by Rhythmyx in place of
 * the default login/error and logout forms. If a custom login page is found, but a custom error
 * page is not found, then the custom login page will be used as the error page.
 */
public class PSLoginServlet extends HttpServlet {
  /** Serial version id */
  private static final long serialVersionUID = 1L;

  /**
   * Handles requests to login and logout. Initial GET requests to "/login" are returned an include
   * of the correct login page (standard or custom if defined). JAAS authentication will be
   * performed for POST request from the login page that provide credentials (the "j_username" and
   * "j_password" request params). Successful authentication will redirect to the originally
   * requested page as specified by the "RX_REDIRECT_URL" session attribute. Authentication failures
   * will return an include of either the custom error page if defined, or else the appropriate
   * login form again. Requests to "/logout" will call {@link
   * jakarta.servlet.http.HttpSession#invalidate()} and redirect the user to the appropriate logout
   * page ((standard or custom if defined).
   *
   * @see HttpServlet#service(HttpServletRequest, HttpServletResponse) for other details.
   */
  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // see if login or logout
    String url = request.getServletPath();
    // if login, logout first, then do login
    if (url.equals("/login")) {
      login(request, response);
    } else if (url.equals("/logout")) {
      logout(request, response);
    }
  }

  /**
   * Calculates the url to use when redirecting after successful form login and appends it to the
   * supplied login page url as a query string parameter.
   *
   * @param request The current request, may not be <code>null</code>.
   * @param loginPage The login page request url to which the result is appended, may not be <code>
   *     null</code> or empty.
   * @return The login page value with the redirect url query string parameter appended, never
   *     <code>null</code> or empty.
   */
  public static String addRedirect(HttpServletRequest request, String loginPage) {
    if (request == null) throw new IllegalArgumentException("request may not be null");

    if (StringUtils.isBlank(loginPage)) {
      throw new IllegalArgumentException("loginPage may not be null or empty");
    }

    String redirect;
    try {
      boolean isBehindProxy = PSServer.isRequestBehindProxy(request);
      if (isBehindProxy) {
        redirect = PSServer.getProxyURL(request, false);
        if (Objects.equals(redirect, "")) {
          redirect = request.getRequestURL().toString();
        }
      } else {
        redirect = request.getRequestURL().toString();
      }
    } catch (NullPointerException ex) {
      // Default
      redirect = CMS_INDEX_PAGE;
    }

    String sep = "?";
    // if the original request was for the login page, redirect to CMS SPA
    if (redirect.endsWith(loginPage)) {
      redirect = CMS_INDEX_PAGE;
    } else if (request.getQueryString() != null) {
      redirect += sep + request.getQueryString();
    }

    loginPage += sep + IPSHtmlParameters.SYS_REDIRECT + "=" + PSURIEncoder.escape(redirect);

    return loginPage;
  }

  /**
   * Handles the logout request.
   *
   * @param request The current request, assumed not <code>null</code>.
   * @param response The current response, assumed not <code>null</code>.
   * @throws IOException If there are any errors redirecting to the logout page.
   * @throws ServletException If there are any other errors
   */
  private void logout(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      PSSystemAuditLogger.logout(request, request.getRemoteUser());
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    // Capture UI locale before the session is invalidated so the logout
    // confirmation page can load TMX + "sign in again" in the same language.
    String logoutLocale = resolveLogoutLocale(request);
    request.setAttribute(LOGOUT_LOCALE_REQUEST_ATTR, logoutLocale);

    HttpSession session = request.getSession(false);
    if (session != null) {
      PSSecurityFilter.logout(
          request, (String) session.getAttribute(IPSHtmlParameters.SYS_SESSIONID));
    }

    // return logout page
    response.setContentType(CONTENT_TYPE_HEADER_VAL);
    request.getRequestDispatcher(getLogoutPage()).include(request, response);
  }

  /**
   * Request attribute holding the BCP-47 locale for the post-logout confirmation page. Set by
   * {@link #logout} before session invalidation so {@code rxlogout.jsp} can still read it.
   */
  public static final String LOGOUT_LOCALE_REQUEST_ATTR = "perc.logout.locale";

  /**
   * Resolves the locale for the post-logout confirmation page.
   *
   * <p>Preference order:
   *
   * <ol>
   *   <li>{@code sys_lang} or {@code j_locale} request parameter (allowlisted BCP-47)
   *   <li>Session private object {@link PSI18nUtils#USER_SESSION_OBJECT_SYS_LANG} (user login
   *       locale), when the session still exists
   *   <li>{@link PSI18nUtils#getSystemLanguage()} as last resort
   * </ol>
   *
   * @param request current request, never {@code null}
   * @return normalized lowercase-hyphen BCP-47 tag, never {@code null} or empty
   */
  public static String resolveLogoutLocale(HttpServletRequest request) {
    if (request == null) {
      return PSI18nUtils.getSystemLanguage();
    }
    String fromParam = firstNonBlankLocale(request.getParameter("sys_lang"));
    if (fromParam == null) {
      fromParam = firstNonBlankLocale(request.getParameter("j_locale"));
    }
    if (fromParam != null) {
      return fromParam;
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      Object fromSession = session.getAttribute(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
      if (fromSession instanceof String sessionLang) {
        String normalized = firstNonBlankLocale(sessionLang);
        if (normalized != null) {
          return normalized;
        }
      }
    }
    return PSI18nUtils.getSystemLanguage();
  }

  /**
   * Builds the relative "sign in again" href for the logout page, carrying the resolved locale as
   * {@code j_locale} so {@code rxlogin.jsp} reopens in the same language.
   *
   * @param locale BCP-47 tag from {@link #resolveLogoutLocale}; may be {@code null}
   * @return relative href such as {@code login?j_locale=fr-fr}, never {@code null}
   */
  public static String buildLogoutLoginHref(String locale) {
    String normalized = firstNonBlankLocale(locale);
    if (normalized == null) {
      return "login";
    }
    return "login?j_locale=" + URLEncoder.encode(normalized, StandardCharsets.UTF_8);
  }

  /**
   * Validates and normalizes a locale tag candidate for use in HTML, TMX, and query strings.
   *
   * @param candidate raw parameter/session value
   * @return normalized tag or {@code null} if empty/invalid
   */
  static String firstNonBlankLocale(String candidate) {
    if (candidate == null) {
      return null;
    }
    // Normalize legacy underscore form (en_US) before allowlist match.
    String trimmed = candidate.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    if (trimmed.isEmpty() || trimmed.length() > 32) {
      return null;
    }
    // BCP-47 language tags used by the product (e.g. en-us, fr-fr, zh-cn). Reject anything that
    // could break HTML attributes or query strings if reflected into the page.
    if (!LOCALE_TAG.matcher(trimmed).matches()) {
      return null;
    }
    return trimmed;
  }

  /** Allowlisted BCP-47 shape: language, optional script/region/variant subtags. */
  private static final Pattern LOCALE_TAG = Pattern.compile("(?i)^[a-z]{2,3}(-[a-z0-9]{2,8})*$");

  /**
   * Handles the login request.
   *
   * @param request The current request, assumed not <code>null</code>.
   * @param response The current response, assumed not <code>null</code>.
   * @throws IOException If there are any errors including the login page or redirecting to the
   *     originally requested page.
   * @throws ServletException If there are any other errors.
   */
  private void login(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // see if initial request for login page, or a post with credentials
    String uid = null;
    String pwd = null;
    String locale;

    // Checking for maximum users allowed in the system, if reached maximum, then don't allow more
    // users
    if (!PSUserSessionManager.checkIfNewUserAllowed()) {
      String errorText = "Maximum number of users are logged in, try again after some time!!";

      // add error param
      request =
          new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String param) {
              if (param.equals("j_error")) return errorText;

              return super.getParameter(param);
            }
          };

      response.setContentType(CONTENT_TYPE_HEADER_VAL);
      request.getRequestDispatcher(getErrorPage()).include(request, response);
      return;
    }

    String formRedirect = null;
    if (request.getMethod().equalsIgnoreCase("POST")) {
      PSRequest psreq = (PSRequest) getRequestInfo(KEY_PSREQUEST);

      if (psreq == null) {
        // this should never happen
        throw new RuntimeException(
            "The request was not properly initialized by the security filter");
      }
      try {
        psreq.parseBody();
        uid = psreq.getParameter("j_username");
        pwd = psreq.getParameter("j_password");
        locale = psreq.getParameter("j_locale");
        // Multipart login form: servlet getParameter() does not see body
        // fields — take sys_redirect from the parsed PSRequest (#3219).
        formRedirect = psreq.getParameter(IPSHtmlParameters.SYS_REDIRECT);

        if (locale != null) {
          request.getSession().setAttribute(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG, locale);
        } else {
          request.getSession().setAttribute(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG, "en-us");
        }

      } catch (PSRequestParsingException e) {
        throw new ServletException(e);
      }
    }

    String redirect =
        PSRedirectValidation.decodeOverEncodedRedirect(
            !StringUtils.isBlank(formRedirect)
                ? formRedirect
                : request.getParameter(IPSHtmlParameters.SYS_REDIRECT));
    if (isValidRedirectUri(request, redirect)) {
      request.getSession().setAttribute(REDIRECT_URL, redirect);
    }
    if (!StringUtils.isBlank(uid)) {
      // handle authentication
      authenticate(request, response, uid, pwd);

    } else {
      // return login page
      response.setContentType(CONTENT_TYPE_HEADER_VAL);
      request.getRequestDispatcher(getLoginPage()).include(request, response);
    }
  }

  /**
   * Normalizes a post-login redirect path for Jetty 12 {@code UriCompliance}.
   *
   * <p>Backslashes and accidental {@code //} segments are treated as ambiguous path separators and
   * cause {@code IllegalArgumentException: Ambiguous URI path separator} on {@code sendRedirect}.
   *
   * <p>This method only normalizes separators — it is <strong>not</strong> an open-redirect
   * barrier. Callers must pass the result through {@link #resolveSafePostLoginRedirect} before
   * {@code sendRedirect}.
   *
   * @param redirect redirect target, may be {@code null}
   * @return sanitized path, or {@code null} if input was {@code null}
   */
  static String sanitizeRedirectPath(String redirect) {
    if (redirect == null) {
      return null;
    }
    String s = redirect.replace('\\', '/');
    // Preserve scheme://host; only collapse // in path-only redirects
    if (!s.contains("://")) {
      while (s.contains("//")) {
        s = s.replace("//", "/");
      }
    }
    return s;
  }

  /**
   * Validates and rebuilds a post-login redirect target so open redirects cannot pivot off
   * session-stored {@code RX_REDIRECT_URL} values (CodeQL {@code
   * java/unvalidated-url-redirection}).
   *
   * <ul>
   *   <li>Path-absolute ({@code /...}): {@link PSRedirectValidation#validateInternalRedirectUrl}
   *   <li>Absolute http(s): allow-list via {@code publicCmsHostname} and request server name
   *   <li>App-relative ({@code index.jsp}, legacy main page): no scheme/host/{@code ..}
   * </ul>
   *
   * <p>Invalid or rejected targets fall back to {@link #CMS_INDEX_PAGE}.
   *
   * @param request current request (used for host allow-list), never {@code null}
   * @param redirect candidate redirect, may be {@code null}
   * @return safe redirect string, never {@code null}
   */
  static String resolveSafePostLoginRedirect(HttpServletRequest request, String redirect) {
    String candidate =
        sanitizeRedirectPath(PSRedirectValidation.decodeOverEncodedRedirect(redirect));
    if (StringUtils.isBlank(candidate)) {
      return CMS_INDEX_PAGE;
    }

    String validated = validatePostLoginRedirectCandidate(request, candidate.trim());
    if (validated == null) {
      log.warn("Rejected post-login redirect; falling back to CMS index");
      return CMS_INDEX_PAGE;
    }

    String rebuilt = rebuildRedirectTarget(validated);
    return rebuilt != null ? rebuilt : CMS_INDEX_PAGE;
  }

  /**
   * Runs {@link PSRedirectValidation} (or relative-path rules) on a separator-normalized candidate.
   * Returns {@code null} when the target is not a safe same-app redirect.
   */
  static String validatePostLoginRedirectCandidate(HttpServletRequest request, String candidate) {
    if (StringUtils.isBlank(candidate)) {
      return null;
    }

    // Path-absolute internal redirects
    if (candidate.startsWith("/") && !candidate.startsWith("//")) {
      return PSRedirectValidation.validateInternalRedirectUrl(candidate);
    }

    // Absolute or protocol-relative — require allow-listed host
    if (candidate.contains("://") || candidate.startsWith("//")) {
      Set<String> allowed = new HashSet<>();
      // defaultValue must be non-null (PSServer.getProperty validates notNull)
      String publicHost = PSServer.getProperty("publicCmsHostname", "");
      if (StringUtils.isNotBlank(publicHost)) {
        allowed.addAll(PSRedirectValidation.createDefaultWhitelist(publicHost));
      }
      if (request != null && StringUtils.isNotBlank(request.getServerName())) {
        allowed.addAll(PSRedirectValidation.createDefaultWhitelist(request.getServerName()));
      }
      return PSRedirectValidation.validateRedirectUrl(candidate, allowed);
    }

    // App-relative UI entry points (index.jsp)
    if (candidate.contains("..") || candidate.indexOf(':') >= 0) {
      return null;
    }
    return candidate;
  }

  /**
   * Rebuilds a validated redirect from URI components so residual CodeQL taint from the original
   * session string does not reach {@code sendRedirect} (same pattern as {@code
   * PSSecurityFilter.sendValidatedRedirect}).
   *
   * <p>Delegates to {@link PSRedirectValidation#rebuildValidatedRedirect(String)} which preserves
   * percent-encoding (must not use multi-arg {@link URI} with raw components — that double-encodes
   * and trips Jetty {@code Ambiguous URI path encoding}).
   */
  static String rebuildRedirectTarget(String safe) {
    return PSRedirectValidation.rebuildValidatedRedirect(safe);
  }

  /**
   * Determines if a redirect URI is valid and safe (XSS). A redirection URI should be to the same
   * host and a valid URI.
   *
   * @param request never null.
   * @param uri maybe null or invalid <code>false</code> will be returned.
   * @return true if a valid redirect uri.
   */
  protected static boolean isValidRedirectUri(HttpServletRequest request, String uri) {
    boolean rvalue = false;
    if (StringUtils.isBlank(uri)) return false;
    try {
      URI targetUri = new URI(uri);
      // See if its just a path
      if (targetUri.getHost() == null
          && targetUri.getAuthority() == null
          && targetUri.getScheme() == null
          && isNotBlank(targetUri.getPath())) {
        rvalue = true;
      } else {
        URI requestUri = new URI(request.getRequestURL().toString());
        rvalue =
            ObjectUtils.equals(requestUri.getHost(), targetUri.getHost())
                && ObjectUtils.equals(requestUri.getPort(), targetUri.getPort())
                && ObjectUtils.equals(requestUri.getScheme(), targetUri.getScheme());
      }
    } catch (URISyntaxException e) {
      log.error("Bad redirect uri: {} , Error : {} ", uri, PSExceptionUtils.getMessageForLog(e));
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    if (PSServer.isRequestBehindProxy(request)) {
      rvalue = true;
    }
    if (!rvalue) log.error("Bad redirect uri: {}", uri);
    return rvalue;
  }

  /**
   * Performs the authentication. If successful, the user is redirected to the originally requested
   * page, if it fails, then the appropriate error page is included.
   *
   * @param request The current request, assumed not <code>null</code>.
   * @param response The current response, assumed not <code>null</code>.
   * @param uid The user id to use, assumed not <code>null</code> or empty.
   * @param pwd The password, may be <code>null</code> or empty.
   * @throws IOException
   * @throws ServletException
   */
  private void authenticate(
      HttpServletRequest request, HttpServletResponse response, String uid, String pwd)
      throws IOException, ServletException {
    try {

      HttpSession sess = request.getSession(true);

      String redirect = (String) sess.getAttribute(REDIRECT_URL);
      if (redirect == null) {
        redirect = CMS_INDEX_PAGE;
      }

      request = PSSecurityFilter.authenticate(request, response, uid, pwd);

      // Audit immediately after successful authentication (before redirect), so a redirect
      // failure cannot drop the security audit record.
      try {
        PSSystemAuditLogger.loginSuccess(request, uid);
      } catch (Exception auditEx) {
        log.error(PSExceptionUtils.getMessageForLog(auditEx));
        log.debug(PSExceptionUtils.getDebugMessageForLog(auditEx));
      }

      // Jetty 12 UriCompliance: normalize separators; then PSRedirectValidation + URI rebuild
      // before sendRedirect (java/unvalidated-url-redirection residual on session redirect).
      String safeRedirect = resolveSafePostLoginRedirect(request, redirect);
      response.sendRedirect(safeRedirect); // codeql[java/unvalidated-url-redirection]

      sess.removeAttribute(REDIRECT_URL);
    } catch (LoginException e) {
      try {
        PSSystemAuditLogger.loginFailure(request, uid, e.getClass().getSimpleName());
      } catch (Exception auditEx) {
        log.error(PSExceptionUtils.getMessageForLog(auditEx));
        log.debug(PSExceptionUtils.getDebugMessageForLog(auditEx));
      }
      Exception ex;

      if (e instanceof PSMissingRoleException) {
        ex = e;
      } else {
        // create error message
        ex =
            new PSAuthenticationFailedException(
                SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED, null);
      }

      final String errorText = ex.getMessage();
      log.debug(errorText, e);

      // add error param
      request =
          new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String param) {
              if (param.equals("j_error")) return errorText;

              return super.getParameter(param);
            }
          };

      response.setContentType(CONTENT_TYPE_HEADER_VAL);
      request.getRequestDispatcher(getErrorPage()).include(request, response);
    }
  }

  /**
   * Gets the appropriate page to include when authentication fails.
   *
   * @return The relative path to the error page, never <code>null</code> or empty.
   */
  private String getErrorPage() {
    File errorPage = new File(getUserDirectory(), ERROR_PAGE);
    if (errorPage.exists()) return "/" + USER_DIR + "/" + ERROR_PAGE;

    return getLoginPage();
  }

  /**
   * Gets the appropriate page to include when returning the login page.
   *
   * @return The relative path to the login page, never <code>null</code> or empty.
   */
  private String getLoginPage() {
    File loginPage = new File(getUserDirectory(), LOGIN_PAGE);
    if (loginPage.exists()) return "/" + USER_DIR + "/" + LOGIN_PAGE;

    return "/rxlogin.jsp";
  }

  /**
   * Gets the appropriate page to include when returning the logout page.
   *
   * @return The relative path to the logout page, never <code>null</code> or empty.
   */
  private String getLogoutPage() {
    File logoutPage = new File(getUserDirectory(), LOGOUT_PAGE);
    if (logoutPage.exists()) return USER_DIR + "/" + LOGOUT_PAGE;

    return "rxlogout.jsp";
  }

  /**
   * Get the absolute path to the user sub-directory of the web application in which this servlet is
   * running.
   *
   * @return The file, never <code>null</code>.
   */
  private File getUserDirectory() {
    return new File(getServletDirectory(), USER_DIR);
  }

  /**
   * Get the path to the directory of the web application in which this servlet is running.
   *
   * @return The path, never <code>null</code>.
   */
  private File getServletDirectory() {
    return new File(getServletContext().getRealPath("/WEB-INF")).getParentFile();
  }

  /**
   * Default post-login landing for the modern CMS UI.
   *
   * <p>Path-absolute SPA entry (query contract). See pure-react-spa design: never use hash
   * fragments on server redirects. Form posts may also supply {@code sys_redirect}.
   */
  /** Dispatcher so {@code getUserHomepage()} can land Navigation / Architecture (#3219). */
  private static final String CMS_INDEX_PAGE = "/cm/app/";

  private static final String USER_DIR = "user";

  /** Name of the user defined login page. */
  private static final String LOGIN_PAGE = "login.jsp";

  /** Name of the user defined logout page. */
  private static final String LOGOUT_PAGE = "logout.jsp";

  /** Name of the user defined error page. */
  private static final String ERROR_PAGE = "error.jsp";

  /**
   * This is used to record the original request that the user was attempting when redirected to the
   * login page while doing form based authentication.
   */
  public static final String REDIRECT_URL = "RX_REDIRECT_URL";

  /** logger */
  private static final Logger log = LogManager.getLogger(SECURITY_LOG);

  /**
   * The Content-Type header value to set when returning included pages, currently text/html with
   * the UTF-8 encoding.
   */
  private static final String CONTENT_TYPE_HEADER_VAL =
      IPSMimeContentTypes.MIME_TYPE_TEXT_HTML + ";charset=" + IPSUtilsConstants.RX_STANDARD_ENC;
}
