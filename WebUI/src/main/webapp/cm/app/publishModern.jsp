<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ page import="com.percussion.user.data.PSCurrentUser" %>
<%@ page import="com.percussion.user.service.impl.PSUserService" %>
<%@ page import="com.percussion.utils.PSSpringBeanProvider" %>
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
    // Progressive disclosure (US7 / Erlang S5): Design for Admin/Designer only.
    // Fail open to true if role lookup fails so admins are not locked out.
    boolean showDesign = true;
    try {
        PSUserService userService = (PSUserService) PSSpringBeanProvider.getBean("userService");
        PSCurrentUser user = userService.getCurrentUser();
        if (user != null) {
            showDesign = user.isAdminUser() || user.isDesignerUser();
        }
    } catch (Exception e) {
        showDesign = true;
    }
    // Allowlist only — never echo arbitrary query text into inline JS (reflected XSS).
    // Values must stay in sync with WebUI/src/main/ts/publishing/deepLinkMap.ts.
    String section = "";
    String rawSection = request.getParameter("section");
    if (rawSection != null) {
        String n = rawSection.trim().toLowerCase(java.util.Locale.ROOT);
        if ("sites".equals(n) || "site".equals(n) || "servers".equals(n)
                || "status".equals(n) || "logs".equals(n) || "log".equals(n)
                || "design".equals(n) || "runtime".equals(n)
                || "editions".equals(n) || "edition".equals(n)) {
            // Non-designers cannot land on Design via deep link
            if ("design".equals(n) && !showDesign) {
                section = "sites";
            } else {
                section = n;
            }
        }
    }
    String siteId = "";
    String rawSiteId = request.getParameter("siteId");
    if (rawSiteId != null && rawSiteId.matches("^[A-Za-z0-9_-]{1,128}$")) {
        siteId = rawSiteId;
    }
    String serverId = "";
    String rawServerId = request.getParameter("serverId");
    if (rawServerId != null && rawServerId.matches("^[A-Za-z0-9_-]{1,128}$")) {
        serverId = rawServerId;
    }
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.navMenu.publish@Publish"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@include file="includes/common_meta.jsp" %>
    <%@include file="includes/modern_shell_head.jsp" %>
    <script src="/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js"></script>
</head>
<body>
<div class="perc-main">
    <jsp:include page="includes/header.jsp" flush="true">
        <jsp:param name="mainNavTab" value="publish"/>
    </jsp:include>
</div>
<div id="perc-publishing-root" style="margin-top: 48px;"></div>
<script>
    (function () {
        function mountPublishing() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountPublishing, 50);
                return;
            }
            window.PercModernUI.mount("perc-publishing-root", "PublishingShell", {
                section: "<%= section %>",
                siteId: "<%= siteId %>",
                serverId: "<%= serverId %>",
                showDesign: <%= showDesign %>
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountPublishing);
        } else {
            mountPublishing();
        }
    })();
</script>
</body>
</html>
