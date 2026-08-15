<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.server.PSServer" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ page import="com.percussion.user.data.PSCurrentUser" %>
<%@ page import="com.percussion.user.service.impl.PSUserService" %>
<%@ page import="com.percussion.utils.PSSpringBeanProvider" %>
<%@ page import="com.percussion.widgetbuilder.service.PSWidgetBuilderService" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
<%--
  Authenticated SPA document (PR-2 app shell; PR-9 BrowserRouter path URLs).
  Server entry / login return: /cm/app/spa.jsp?entry=home (query contract — no hash).
  Client routes: /cm/app/home, /cm/app/publish/… (refresh via PSWebUiSpaFallbackFilter).
  No header.jsp / mainnav.jsp — React AppLayout + TopNav.
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
        if ("widgetbuilder".equals(n)) {
            return "widget-builder";
        }
        if ("arch".equals(n) || "navigation".equals(n)) {
            return "architecture";
        }
        if ("home".equals(n) || "publish".equals(n) || "workflow".equals(n)
                || "admin".equals(n) || "widget-builder".equals(n)
                || "explorer".equals(n) || "developer".equals(n)
                || "design".equals(n) || "architecture".equals(n)
                || "profile".equals(n) || "assembly".equals(n)
                || "unavailable".equals(n)) {
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
    boolean isAdmin = false;
    boolean isDesigner = false;
    boolean isWdgActive = false;
    // server.properties allowExternalAvatarFetch (default true). When false, SPA
    // shows initials only and does not fetch Gravatar (enterprise privacy).
    boolean allowExternalAvatarFetch = true;
    try {
        if (PSServer.getServerProps() != null) {
            String prop = PSServer.getServerProps().getProperty(
                    "allowExternalAvatarFetch", "true");
            allowExternalAvatarFetch = !"false".equalsIgnoreCase(
                    prop == null ? "true" : prop.trim());
        }
    } catch (Exception e) {
        allowExternalAvatarFetch = true;
    }
    try {
        PSUserService userService =
            (PSUserService) PSSpringBeanProvider.getBean("userService");
        PSCurrentUser user = userService.getCurrentUser();
        if (user != null) {
            if (user.getName() != null) {
                userName = user.getName();
            }
            isAdmin = user.isAdminUser();
            isDesigner = user.isDesignerUser();
        }
        PSWidgetBuilderService wb =
            (PSWidgetBuilderService) PSSpringBeanProvider.getBean("widgetBuilderService");
        if (wb != null) {
            isWdgActive = wb.isWidgetBuilderEnabled();
        }
    } catch (Exception e) {
        // Fall back to remote user only; nav still works with reduced chrome
    }

    // Role-based entry gate (server UX; REST remains authoritative)
    if (("workflow".equals(entry) || "admin".equals(entry)) && !isAdmin) {
        entry = "home";
    }
    if (("publish".equals(entry) || "widget-builder".equals(entry)
            || "developer".equals(entry) || "design".equals(entry)
            || "architecture".equals(entry))
            && !(isAdmin || isDesigner)) {
        entry = "home";
    }
    if ("widget-builder".equals(entry) && !isWdgActive) {
        entry = "home";
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
    <%-- TMX catalog for React message() (required for Home and all SPA chrome) --%>
    <script src="<%= request.getContextPath() %>/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <link rel="stylesheet" href="/cm/modern/assets/perc-modern-ui.css"/>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
    <style>html, body { margin: 0; padding: 0; }</style>
</head>
<body>
<script type="application/json" id="perc-bootstrap">{
    "userName":<%= jsonString(userName) %>,
    "locale":<%= jsonString(locale) %>,
    "entry":<%= jsonString(entry) %>,
    "isAdmin":<%= isAdmin %>,
    "isDesigner":<%= isDesigner %>,
    "isWidgetBuilderActive":<%= isWdgActive %>,
    "allowExternalAvatarFetch":<%= allowExternalAvatarFetch %>
}</script>
<div id="perc-spa-root" data-testid="perc-spa-root"></div>
</body>
</html>
