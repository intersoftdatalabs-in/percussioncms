<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
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
    String debug = request.getParameter("debug");
    if (debug == null) {
        debug = "false";
    }
    // Allowlist only — never echo arbitrary query text into inline JS (reflected XSS).
    // Values must stay in sync with WebUI/src/main/ts/home/deepLinkMap.ts.
    String initialScreen = "";
    String rawInitialScreen = request.getParameter("initialScreen");
    if (rawInitialScreen != null) {
        String n = rawInitialScreen.trim().toLowerCase(java.util.Locale.ROOT);
        if ("library".equals(n) || "list".equals(n) || "search".equals(n)
                || "newitem".equals(n) || "bookmarks".equals(n) || "bookmark".equals(n)
                || "recent".equals(n) || "create".equals(n)) {
            initialScreen = n;
        }
    }
    Boolean isAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.navMenu.home@Home"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@include file="includes/common_meta.jsp" %>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
</head>
<body>
<div class="perc-main">
    <jsp:include page="includes/header.jsp" flush="true">
        <jsp:param name="mainNavTab" value="home"/>
    </jsp:include>
</div>
<div id="perc-home-modern-root" style="margin-top: 48px;"></div>
<script>
    (function () {
        function mountHome() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountHome, 50);
                return;
            }
            window.PercModernUI.mount("perc-home-modern-root", "HomeShell", {
                initialSection: "<%= initialScreen %>",
                isAdmin: <%= isAdmin %>
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountHome);
        } else {
            mountHome();
        }
    })();
</script>
</body>
</html>
