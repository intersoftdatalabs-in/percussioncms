<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ page import="org.apache.commons.lang3.StringEscapeUtils" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n" %>
<%
    String locale = PSRoleUtilities.getUserCurrentLocale();
    if (locale == null) {
        locale = "en-us";
    }
    String lang = locale.contains("-") ? locale.split("-")[0] : locale;
    // Optional legacy view name for UnavailableView messaging only — never trust for HTML/JS.
    String detail = request.getParameter("from");
    if (detail == null) {
        detail = "";
    }
    // Escape for a double-quoted JavaScript string literal (blocks </script> breakout etc.).
    String detailJs = StringEscapeUtils.escapeEcmaScript(detail);
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="false"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.home.modern@Unavailable"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@include file="includes/common_meta.jsp" %>
    <script src="/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
</head>
<body>
<div id="perc-unavailable-root" style="margin: 24px;"></div>
<script>
    (function () {
        function mount() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mount, 50);
                return;
            }
            window.PercModernUI.mount("perc-unavailable-root", "UnavailableView", {
                detail: "<%= detailJs %>"
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mount);
        } else {
            mount();
        }
    })();
</script>
</body>
</html>
