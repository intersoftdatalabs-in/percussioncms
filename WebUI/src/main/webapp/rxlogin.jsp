<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.percussion.server.PSServer" %>
<%@ page import="com.percussion.i18n.PSI18nUtils" %>
<%@ page import="com.percussion.i18n.PSLocaleManager" %>
<%@ page import="com.percussion.i18n.PSLocale" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
<%--
  React Login SPA host (product front door).
  Auth remains POST /login — this page hosts the React UI + XSS-safe bootstrap.
  Classic markup (rxlogin-classic.jsp) was removed in PR-8.
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
    String username = request.getParameter("j_username");
    String locale = request.getParameter("j_locale");
    String error = request.getParameter("j_error");
    String lang = "en";

    if (username == null) {
        username = "";
    }
    if (locale == null) {
        locale = PSI18nUtils.getSystemLanguage();
    }
    if (locale != null && locale.contains("-")) {
        lang = locale.split("-")[0];
    } else if (locale != null) {
        lang = locale;
    }

    String loginComplete = PSServer.getServerProps().getProperty("loginAutoComplete");
    String autocomplete = "on";
    if (loginComplete != null && loginComplete.equalsIgnoreCase("off")) {
        autocomplete = "off";
    }

    // Default post-login SPA entry (query contract — never use # fragments).
    String defaultRedirect = "/cm/app/spa.jsp?entry=home";
    String returnParam = request.getParameter("return");
    if (returnParam != null
            && returnParam.startsWith("/cm/app/spa.jsp")
            && !returnParam.contains("..")
            && !returnParam.contains("://")
            && !returnParam.contains("#")
            && returnParam.length() < 2048) {
        defaultRedirect = returnParam;
    }

    PSLocaleManager locManager = PSLocaleManager.getInstance();
    StringBuilder localesJson = new StringBuilder();
    localesJson.append('[');
    boolean first = true;
    Iterator<PSLocale> locales = locManager.getLocales();
    while (locales.hasNext()) {
        PSLocale loc = locales.next();
        if (!first) {
            localesJson.append(',');
        }
        first = false;
        localesJson.append('{')
                .append("\"name\":").append(jsonString(loc.getName())).append(',')
                .append("\"displayName\":").append(jsonString(loc.getDisplayName()))
                .append('}');
    }
    localesJson.append(']');
%>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Percussion CMS — Sign in</title>
    <meta name="_csrf_header" content="<csrf:tokenname/>"/>
    <meta name="_csrf" content="<csrf:tokenvalue/>"/>
    <script src="/JavaScriptServlet"></script>
    <%-- Stable CSS entry (Vite cssCodeSplit:false). JS also injects if this is missing. --%>
    <link rel="stylesheet" href="/cm/modern/assets/perc-modern-ui.css"/>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
    <style>
        html, body { margin: 0; padding: 0; }
        /* Defensive: if CSS fails to load, logo must not paint at intrinsic 1477×720 */
        img[data-testid="perc-login-logo"],
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
<%-- Hidden holder for CSRF tags (merged into bootstrap before React mounts). --%>
<div id="perc-csrf-holder"
     data-csrf-name="<csrf:tokenname/>"
     data-csrf-value="<csrf:tokenvalue/>"
     hidden
     aria-hidden="true"></div>
<script type="application/json" id="perc-login-bootstrap">{
  "locales":<%= localesJson.toString() %>,
  "selectedLocale":<%= jsonString(locale) %>,
  "username":<%= jsonString(username) %>,
  "error":<%= error == null || error.isEmpty() ? "null" : jsonString(error) %>,
  "autocomplete":<%= jsonString(autocomplete) %>,
  "defaultRedirect":<%= jsonString(defaultRedirect) %>,
  "csrfTokenName":"OWASP_CSRFTOKEN",
  "csrfTokenValue":"",
  "formAction":"login"
}</script>
<div id="perc-login-root" data-testid="perc-login-root"></div>
<script>
    (function () {
        try {
            var holder = document.getElementById("perc-csrf-holder");
            var bootEl = document.getElementById("perc-login-bootstrap");
            if (!holder || !bootEl || !bootEl.textContent) return;
            var data = JSON.parse(bootEl.textContent);
            var name = holder.getAttribute("data-csrf-name") || "OWASP_CSRFTOKEN";
            var value = holder.getAttribute("data-csrf-value") || "";
            data.csrfTokenName = name;
            if (value) {
                data.csrfTokenValue = value;
            } else if (window.OWASP_CSRFTOKEN && window.OWASP_CSRFTOKEN.token) {
                data.csrfTokenValue = window.OWASP_CSRFTOKEN.token;
            }
            bootEl.textContent = JSON.stringify(data);
        } catch (e) {
            console.error("[rxlogin] CSRF bootstrap merge failed", e);
        }
    })();
</script>
</body>
</html>
