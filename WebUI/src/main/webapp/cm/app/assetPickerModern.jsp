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
    <title><i18n:message key="perc.ui.contentBrowser@Content Browser (Asset Picker)"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; background: #fafafa; color: #222; }
        #perc-content-browser-host { max-width: 1024px; margin: 0 auto; }
        h1 { font-size: 1.1rem; margin: 0 0 16px 0; }
    </style>
</head>
<body>
<div id="perc-content-browser-host">
    <h1>Asset Picker (US2 pilot — modern ContentBrowser)</h1>
    <p style="color:#555; font-size:0.9rem; margin:0 0 12px 0;">
        Replaces the legacy miller-column Finder asset picker (FR-008a).
        The modern ContentBrowser mounts via the PercModernUI bridge; on
        confirm, the host receives a <code>SelectionResult</code> with the
        chosen asset's id + path.
    </p>
    <div id="perc-content-browser-root"
         data-testid="perc-content-browser-root"
         style="min-height: 480px;"></div>
    <pre id="perc-content-browser-result"
         data-testid="perc-content-browser-result"
         style="margin-top:16px; padding:12px; background:#fff; border:1px solid #ddd; font-size:0.85rem; white-space:pre-wrap;"></pre>
</div>
<script>
    (function () {
        // Self-load the modern bridge (same pattern as the US6 hard-cut
        // pages — idempotent, with cache-buster).
        if (!document.querySelector('script[src*="perc-modern-ui.js"]')) {
            var s = document.createElement("script");
            s.type = "module";
            s.src = "/cm/modern/assets/perc-modern-ui.js?cb=" + Date.now();
            document.head.appendChild(s);
        }
        function mountBrowser() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountBrowser, 50);
                return;
            }
            window.PercModernUI.mount("perc-content-browser-root", "ContentBrowser", {
                initialPath: "/Sites",
                mode: "select",
                multiSelect: false,
                allowFolderSelect: false,
                allowItemSelect: true,
                allowedTypes: ["page", "asset"],
                // #2793: mount shared SearchPanel (catalog + execute) in this host.
                enableSearch: true,
                enablePreview: true,
                title: "Pick an asset",
                onConfirm: function (selection) {
                    var out = document.getElementById("perc-content-browser-result");
                    if (out) {
                        out.textContent = "Confirmed: " + JSON.stringify(selection, null, 2);
                    }
                },
                onCancel: function () {
                    var out = document.getElementById("perc-content-browser-result");
                    if (out) {
                        out.textContent = "Cancelled";
                    }
                },
                onError: function (message) {
                    var out = document.getElementById("perc-content-browser-result");
                    if (out) {
                        out.textContent = "Error: " + message;
                    }
                }
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountBrowser);
        } else {
            mountBrowser();
        }
    })();
</script>
</body>
</html>