<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
<%
    String locale = PSRoleUtilities.getUserCurrentLocale();
    if (locale == null) locale = "en-us";
    String lang = "en";
    if (locale.contains("-")) lang = locale.split("-")[0];
    String debug = request.getParameter("debug");
    if (debug == null) debug = "false";
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.search@Search (US5 P-Search pilot)"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; background: #fafafa; color: #222; }
        #perc-search-host { max-width: 720px; margin: 0 auto; }
        h1 { font-size: 1.1rem; margin: 0 0 16px 0; }
    </style>
</head>
<body>
<div id="perc-search-host">
    <h1>Search (US5 P-Search pilot)</h1>
    <p style="color:#555; font-size:0.9rem; margin:0 0 12px 0;">
        US5 P-Search: the modern <code>SearchPanel</code> mounts on
        this page. Submits to <code>/Rhythmyx/services/searchmanagement/search/get/extendedresults</code>;
        "Open" writes the selected row to the result block;
        "Reveal in folder" writes the parent path.
    </p>
    <div id="perc-search-root"
         data-testid="perc-search-root"
         style="margin-bottom: 24px;"></div>
    <pre id="perc-search-result"
         data-testid="perc-search-result"
         style="margin-top:16px; padding:12px; background:#fff; border:1px solid #ddd; font-size:0.85rem; white-space:pre-wrap;"></pre>
</div>
<script>
    (function () {
        if (!document.querySelector('script[src*="perc-modern-ui.js"]')) {
            var s = document.createElement("script");
            s.type = "module";
            s.src = "/cm/modern/assets/perc-modern-ui.js?cb=" + Date.now();
            document.head.appendChild(s);
        }
        function mountSearch() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountSearch, 50);
                return;
            }
            window.PercModernUI.mount("perc-search-root", "SearchPanel", {
                onOpen: function (result) {
                    var out = document.getElementById("perc-search-result");
                    if (out) {
                        out.textContent = "Open: " + JSON.stringify({
                            id: result.id, title: result.title, folderPath: result.folderPath,
                        }, null, 2);
                    }
                },
                onReveal: function (result) {
                    var out = document.getElementById("perc-search-result");
                    if (out) {
                        out.textContent = "Reveal in: " + (result.folderPath || "(unknown)");
                    }
                },
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountSearch);
        } else {
            mountSearch();
        }
    })();
</script>
</body>
</html>
