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
    // initialPath query parameter is forwarded to the modern shell so tests
    // and deep links can land on a specific folder. Must be a plain relative
    // path beginning with "/" — strip everything else to avoid reflected XSS
    // (the value is echoed as a string literal into the inline mount call).
    String initialPath = "";
    String rawInitialPath = request.getParameter("initialPath");
    if (rawInitialPath != null && rawInitialPath.startsWith("/")
            && rawInitialPath.length() < 2048
            && rawInitialPath.matches("[/A-Za-z0-9._-]+")) {
        initialPath = rawInitialPath;
    }
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.explorer@Content Explorer"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@include file="includes/common_meta.jsp" %>
    <%@include file="includes/modern_shell_head.jsp" %>
    <script src="/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
</head>
<body>
<div class="perc-main">
    <jsp:include page="includes/header.jsp" flush="true">
        <jsp:param name="mainNavTab" value="home"/>
    </jsp:include>
</div>
<div id="perc-explorer-modern-root" data-testid="perc-explorer-modern-root" style="margin-top: 48px;"></div>
<script>
    (function () {
        function mountExplorer() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountExplorer, 50);
                return;
            }
            window.PercModernUI.mount("perc-explorer-modern-root", "ContentExplorerShell", {
                initialPath: "<%= initialPath %>"
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountExplorer);
        } else {
            mountExplorer();
        }
    })();
</script>
</body>
</html>