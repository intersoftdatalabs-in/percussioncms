<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.i18n.PSI18nUtils" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.regex.Pattern" %>
<%--
  React Logout SPA host (post-logout confirmation).
  Server /logout endpoint is unchanged — this page is the UI only.
  Mirrors rxlogin.jsp host contract: modern CSS/JS, TMX, XSS-safe bootstrap.

  Locale preference (mirrors PSLoginServlet.resolveLogoutLocale):
    1) request attribute perc.logout.locale (set before session invalidate)
    2) sys_lang / j_locale query params (direct hits / bookmarks)
    3) system language
  "Sign in again" carries j_locale so login reopens in the same language.
--%>
<%!
    /** Must match PSLoginServlet.LOGOUT_LOCALE_REQUEST_ATTR */
    private static final String LOGOUT_LOCALE_REQUEST_ATTR = "perc.logout.locale";

    private static final Pattern LOCALE_TAG =
            Pattern.compile("(?i)^[a-z]{2,3}(-[a-z0-9]{2,8})*$");

    private static String jsonString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<': sb.append("\\u003c"); break;
                case '>': sb.append("\\u003e"); break;
                case '&': sb.append("\\u0026"); break;
                case '\u2028': sb.append("\\u2028"); break;
                case '\u2029': sb.append("\\u2029"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Allowlist + normalize BCP-47; null if empty/invalid. */
    private static String normalizeLocaleCandidate(String candidate) {
        if (candidate == null) {
            return null;
        }
        // Normalize legacy underscore form (en_US) before allowlist match.
        String trimmed = candidate.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (trimmed.isEmpty() || trimmed.length() > 32) {
            return null;
        }
        if (!LOCALE_TAG.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }

    /** Relative login front door with j_locale (mirrors PSLoginServlet.buildLogoutLoginHref). */
    private static String buildLoginHref(String locale) {
        if (locale == null || locale.isEmpty()) {
            return "login";
        }
        return "login?j_locale=" + URLEncoder.encode(locale, StandardCharsets.UTF_8);
    }
%>
<%
    // 1) Attribute set by PSLoginServlet.logout before session invalidate
    String locale = null;
    Object attr = request.getAttribute(LOGOUT_LOCALE_REQUEST_ATTR);
    if (attr instanceof String) {
        locale = normalizeLocaleCandidate((String) attr);
    }
    // 2) Query params (direct /rxlogout.jsp?sys_lang=… or j_locale=…)
    if (locale == null) {
        locale = normalizeLocaleCandidate(request.getParameter("sys_lang"));
    }
    if (locale == null) {
        locale = normalizeLocaleCandidate(request.getParameter("j_locale"));
    }
    // 3) System default
    if (locale == null || locale.isEmpty()) {
        locale = PSI18nUtils.getSystemLanguage();
        if (locale == null || locale.isEmpty()) {
            locale = "en-us";
        }
    }

    // Product login front door with locale so sign-in reopens in the same language.
    String loginHref = buildLoginHref(locale);
%>
<!DOCTYPE html>
<html lang="<%= locale %>">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Percussion CMS — Signed out</title>
    <%-- TMX catalog for React message() (required for logout chrome) --%>
    <script src="<%= request.getContextPath() %>/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <%-- Stable CSS entry (Vite cssCodeSplit:false). JS also injects if this is missing. --%>
    <link rel="stylesheet" href="/cm/modern/assets/perc-modern-ui.css"/>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
    <style>
        html, body { margin: 0; padding: 0; }
        /* Defensive: if CSS fails to load, logo must not paint at intrinsic 1477×720 */
        img[data-testid="perc-logout-logo"],
        img[data-testid="perc-brand-logo"] {
            max-height: 48px;
            max-width: 220px;
            width: auto;
            height: auto;
            object-fit: contain;
        }
        img[data-testid="perc-brand-logo"] {
            max-height: 32px;
            max-width: 160px;
        }
    </style>
</head>
<body>
<script type="application/json" id="perc-logout-bootstrap">{
  "locale":<%= jsonString(locale) %>,
  "loginHref":<%= jsonString(loginHref) %>
}</script>
<div id="perc-logout-root" data-testid="perc-logout-root"></div>
</body>
</html>
