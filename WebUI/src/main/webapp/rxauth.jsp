<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.i18n.PSI18nUtils" %>
<%@ page import="com.percussion.i18n.PSLocaleFormat" %>
<%@ page import="com.percussion.i18n.PSLocaleFormatCatalog" %>
<%@ page import="com.percussion.servlets.PSAuthCsrfGuard" %>
<%@ page import="com.percussion.servlets.PSAuthServlet" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
<%--
  Public React auth host for unauthenticated email verification and account recovery (/auth/*).
  GET renders confirmation/form views only — never mutates tokens on GET.
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
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");

    Object viewAttr = request.getAttribute(PSAuthServlet.ATTR_VIEW);
    Object tokenAttr = request.getAttribute(PSAuthServlet.ATTR_TOKEN);
    Object authCsrfAttr = request.getAttribute(PSAuthServlet.ATTR_CSRF_TOKEN);
    Object bootstrapJsonAttr = request.getAttribute(PSAuthServlet.ATTR_BOOTSTRAP_JSON);

    String view = viewAttr instanceof String && !((String) viewAttr).isBlank()
            ? (String) viewAttr
            : "forgot-username";
    String token = tokenAttr instanceof String ? (String) tokenAttr : "";
    String authCsrfToken = authCsrfAttr instanceof String && !((String) authCsrfAttr).isBlank()
            ? (String) authCsrfAttr
            : new PSAuthCsrfGuard().ensureSessionToken(request);
    String mfaBootstrapJson = bootstrapJsonAttr instanceof String
            ? (String) bootstrapJsonAttr
            : "";

    String pageTitle = switch (view) {
        case "mfa-challenge" -> "Percussion CMS — Two-Step Verification";
        case "mfa-setup" -> "Percussion CMS — Set Up Two-Step Verification";
        case "verify-email" -> "Percussion CMS — Confirm Email";
        default -> "Percussion CMS — Account";
    };

    String locale = PSI18nUtils.getSystemLanguage();
    if (locale == null || locale.isBlank()) {
        locale = "en-us";
    }
    PSLocaleFormat localeFormat = PSLocaleFormatCatalog.resolve(locale);
    String textDir = localeFormat.getTextDir() != null ? localeFormat.getTextDir() : "ltr";
    String ctxPath = request.getContextPath() != null ? request.getContextPath() : "";
%>
<!DOCTYPE html>
<html lang="<%= locale %>" dir="<%= textDir %>">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title><%= pageTitle %></title>
    <meta name="_csrf_header" content="<csrf:tokenname/>"/>
    <meta name="_csrf" content="<csrf:tokenvalue/>"/>
    <script src="<%= ctxPath %>/JavaScriptServlet"></script>
    <script src="<%= ctxPath %>/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <link rel="stylesheet" href="<%= ctxPath %>/cm/modern/assets/perc-modern-ui.css"/>
    <script type="module" src="<%= ctxPath %>/cm/modern/assets/perc-modern-ui.js"></script>
    <style>
        html, body { margin: 0; padding: 0; }
        img[data-testid="perc-auth-logo"],
        img[data-testid="perc-brand-logo"] {
            max-height: 48px;
            max-width: 220px;
            width: auto;
            height: auto;
            object-fit: contain;
        }
    </style>
</head>
<body>
<div id="perc-csrf-holder"
     data-csrf-name="<csrf:tokenname/>"
     data-csrf-value="<csrf:tokenvalue/>"
     hidden
     aria-hidden="true"></div>
<script type="application/json" id="perc-auth-bootstrap">{
  "view":<%= jsonString(view) %>,
  "token":<%= jsonString(token) %>,
  "authCsrfToken":<%= jsonString(authCsrfToken) %>,
  "mfaStateJson":<%= jsonString(mfaBootstrapJson) %>,
  "csrfTokenName":"OWASP_CSRFTOKEN",
  "csrfTokenValue":"",
  "contextPath":<%= jsonString(ctxPath) %>,
  "locale":<%= jsonString(locale) %>,
  "textDir":<%= jsonString(textDir) %>
}</script>
<div id="perc-auth-root" data-testid="perc-auth-root"></div>
<script>
    (function () {
        try {
            var holder = document.getElementById("perc-csrf-holder");
            var bootEl = document.getElementById("perc-auth-bootstrap");
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
            console.error("[rxauth] CSRF bootstrap merge failed", e);
        }
    })();
</script>
</body>
</html>
