<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>

<%
    String locale = PSRoleUtilities.getUserCurrentLocale();
    String lang = "en";
    if (locale == null) {
        locale = "en-us";
    } else {
        if (locale.contains("-"))
            lang = locale.split("-")[0];
        else
            lang = locale;
    }
    String tab = request.getParameter("tab");
    if (tab == null || !tab.matches("^(tasks|logs|notifications)$")) {
        tab = "tasks";
    }
%>
<i18n:settings lang="<%=locale%>" prefixes="perc.ui."/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.admin.title@Administration"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@include file="includes/common_meta.jsp" %>
    <link rel="stylesheet" type="text/css" href="../themes/smoothness/jquery-ui-1.8.9.custom.css"/>
    <link rel="stylesheet" type="text/css" href="/cm/jslib/profiles/3x/libraries/fontawesome/css/all.css"/>
    <link rel="stylesheet" type="text/css" href="../jslib/profiles/3x/libraries/bootstrap/css/bootstrap.min.css"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale%>"></script>
    <script src="/JavaScriptServlet"></script>
</head>
<body class="perc-admin-body">
    <div id="perc-admin-root" data-testid="perc-admin-root"></div>

    <script>
      (function() {
        function mountShell() {
          if (window.PercModernUI && typeof window.PercModernUI.mount === "function") {
            window.PercModernUI.mount("perc-admin-root", "AdminShell", {
              initialTab: "<%= tab %>"
            });
          } else {
            setTimeout(mountShell, 50);
          }
        }
        if (!document.querySelector('script[src*="perc-modern-ui.js"]')) {
          var s = document.createElement("script");
          s.type = "module";
          s.src = "/cm/modern/assets/perc-modern-ui.js?cb=" + Date.now();
          document.head.appendChild(s);
        }
        mountShell();
      })();
    </script>
</body>
</html>
