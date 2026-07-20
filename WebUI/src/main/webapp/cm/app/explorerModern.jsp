<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n" %>
<%
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
    String locale = "en-us";
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= locale %>">
<head>
    <title><i18n:message key="perc.ui.explorer@Content Explorer"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <script type="module" src="/cm/modern/assets/perc-modern-ui.js?cb=<%= System.currentTimeMillis() %>"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 0; background: #fafafa; color: #222; }
        #perc-explorer-modern-root { padding: 12px; }
    </style>
</head>
<body>
<div id="perc-explorer-modern-root" data-testid="perc-explorer-modern-root"></div>
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