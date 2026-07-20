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
    <title><i18n:message key="perc.ui.contentBrowser@Content Browser (Page Picker)"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; background: #fafafa; color: #222; }
        #perc-page-picker-host { max-width: 1024px; margin: 0 auto; }
        h1 { font-size: 1.1rem; margin: 0 0 16px 0; }
    </style>
</head>
<body>
<div id="perc-page-picker-host">
    <h1>Page Picker (US2 T045b — modern ContentBrowser)</h1>
    <p style="color:#555; font-size:0.9rem; margin:0 0 12px 0;">
        Migrates the legacy <code>$.perc_finder().launchPagePreview(...)</code> call
        sites in <code>PercPageView.js</code>, <code>PercSiteImpactView.js</code>,
        <code>PercRevisionDialog.js</code>, and
        <code>PercContributorUiAdaptor.js</code> (per cutover-inventory §C).
        Modern ContentBrowser with <code>allowedTypes: ['page']</code> and
        <code>multiSelect: true</code> to demonstrate the multi-select path.
    </p>
    <div id="perc-page-picker-root"
         data-testid="perc-page-picker-root"
         style="min-height: 480px;"></div>
    <pre id="perc-page-picker-result"
         data-testid="perc-page-picker-result"
         style="margin-top:16px; padding:12px; background:#fff; border:1px solid #ddd; font-size:0.85rem; white-space:pre-wrap;"></pre>
</div>
<script>
    (function () {
        // US2 (T045b): self-load the modern bridge (idempotent).
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
            window.PercModernUI.mount("perc-page-picker-root", "ContentBrowser", {
                initialPath: "/Sites",
                mode: "select",
                multiSelect: true,
                allowFolderSelect: false,
                allowItemSelect: true,
                allowedTypes: ["page"],
                enableSearch: false,
                enablePreview: true,
                title: "Pick one or more pages",
                onConfirm: function (selection) {
                    var out = document.getElementById("perc-page-picker-result");
                    if (out) {
                        out.textContent = "Confirmed (multi): " + JSON.stringify(selection, null, 2);
                    }
                },
                onCancel: function () {
                    var out = document.getElementById("perc-page-picker-result");
                    if (out) {
                        out.textContent = "Cancelled";
                    }
                },
                onError: function (message) {
                    var out = document.getElementById("perc-page-picker-result");
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