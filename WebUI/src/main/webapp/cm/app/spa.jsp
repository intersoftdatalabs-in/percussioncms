<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
<%--
  Minimal authenticated SPA landing (PR-1).
  Full router + feature shells land in subsequent PRs.
  Entry: /cm/app/spa.jsp?entry=home (query contract — no hash redirects).
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

    private static String allowEntry(String raw) {
        if (raw == null) {
            return "home";
        }
        String n = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if ("home".equals(n) || "publish".equals(n) || "workflow".equals(n)
                || "admin".equals(n) || "widget-builder".equals(n)
                || "explorer".equals(n) || "unavailable".equals(n)) {
            return n;
        }
        return "home";
    }
%>
<%
    String locale = PSRoleUtilities.getUserCurrentLocale();
    String lang = "en";
    if (locale == null) {
        locale = "en-us";
    } else if (locale.contains("-")) {
        lang = locale.split("-")[0];
    } else {
        lang = locale;
    }
    String entry = allowEntry(request.getParameter("entry"));
    String userName = request.getRemoteUser();
    if (userName == null) {
        userName = "";
    }
%>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Percussion CMS</title>
    <meta name="_csrf_header" content="<csrf:tokenname/>"/>
    <meta name="_csrf" content="<csrf:tokenvalue/>"/>
    <script src="/JavaScriptServlet"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
    <style>html, body { margin: 0; padding: 0; }</style>
</head>
<body>
<script type="application/json" id="perc-bootstrap">{
    "userName":<%= jsonString(userName) %>,
    "locale":<%= jsonString(locale) %>,
    "entry":<%= jsonString(entry) %>
}</script>
<div id="perc-spa-root" data-testid="perc-spa-root"></div>
</body>
</html>
