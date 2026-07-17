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
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.widget.builder@Widget Builder"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@include file="includes/common_meta.jsp" %>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
</head>
<body>
<div class="perc-main">
    <jsp:include page="includes/header.jsp" flush="true">
        <jsp:param name="mainNavTab" value="widgetbuilder"/>
    </jsp:include>
</div>
<div id="perc-wb-modern-root" style="margin-top: 48px;"></div>
<script>
    (function () {
        function mountWb() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountWb, 50);
                return;
            }
            window.PercModernUI.mount("perc-wb-modern-root", "WidgetBuilderApp", {});
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountWb);
        } else {
            mountWb();
        }
    })();
</script>
</body>
</html>
