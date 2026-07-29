<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.i18n.PSI18nUtils" %>
<%--
  React Logout SPA host (post-logout confirmation).
  Server /logout endpoint is unchanged — this page is the UI only.
  Mirrors rxlogin.jsp host contract: modern CSS/JS, TMX, XSS-safe bootstrap.
--%>
<%!
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
%>
<%
    String locale = PSI18nUtils.getSystemLanguage();
    if (locale == null || locale.isEmpty()) {
        locale = "en-us";
    }
    // Product login front door (same relative action as classic login form).
    String loginHref = "login";
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
