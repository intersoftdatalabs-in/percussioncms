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
    String folderId = request.getParameter("folderId");
    if (folderId == null) folderId = "";
%>
<i18n:settings lang="<%= locale %>" prefixes="perc.ui." debug="<%= debug %>"/>
<!DOCTYPE html>
<html lang="<%= lang %>">
<head>
    <title><i18n:message key="perc.ui.folderSecurity@Folder Security (US4 P-ACL pilot)"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; background: #fafafa; color: #222; }
        #perc-folder-security-host { max-width: 1024px; margin: 0 auto; }
        h1 { font-size: 1.1rem; margin: 0 0 16px 0; }
    </style>
</head>
<body>
<div id="perc-folder-security-host">
    <h1>Folder Security (US4 P-ACL pilot)</h1>
    <p style="color:#555; font-size:0.9rem; margin:0 0 12px 0;">
        US4 P-ACL: the modern <code>FolderSecurityPanel</code> mounts on
        this page. The query parameter <code>?folderId=&lt;id&gt;</code>
        selects which folder's permission to view / edit. The current
        session user (Admin) is passed as the identity set so the
        self-lockout warning can be exercised end-to-end.
    </p>
    <div id="perc-folder-security-root"
         data-testid="perc-folder-security-root"
         style="min-height: 320px;"><%
        /* First paint before perc-modern-ui.js: folder-id hosts must expose a
           folder-security-* surface (#3268 / #2749) without miller-column chrome. */
        if (folderId.isEmpty()) {
    %>
        <p data-testid="perc-folder-security-no-folder">No folderId supplied. Append ?folderId=&lt;id&gt; to this URL.</p><%
        } else {
    %>
        <div role="status" data-testid="folder-security-loading">Loading permissions</div><%
        }
    %></div>
    <pre id="perc-folder-security-result"
         data-testid="perc-folder-security-result"
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
        function paintMountError(detail) {
            var el = document.getElementById("perc-folder-security-root");
            if (!el) return;
            if (el.querySelector("[data-testid='folder-security-panel'], [data-testid='folder-security-error']")) {
                return;
            }
            var msg = detail ? String(detail) : "Failed to mount folder security";
            el.innerHTML = "<div role=\"alert\" data-testid=\"folder-security-error\"><p></p></div>";
            var p = el.querySelector("p");
            if (p) p.textContent = msg;
        }
        function mountSecurity() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountSecurity, 50);
                return;
            }
            var url = new URL(window.location.href);
            var folderId = (url.searchParams.get("folderId") || "").trim();
            if (!folderId) {
                /* Keep the server-rendered no-folder placeholder. A
                   failed mount would overwrite it with an error. */
                return;
            }
            try {
                window.PercModernUI.mount("perc-folder-security-root", "FolderSecurityHost", {
                    folderId: folderId,
                    currentUserIdentities: ["Admin"],
                    onSaved: function (props) {
                        var out = document.getElementById("perc-folder-security-result");
                        if (out) {
                            out.textContent = "Saved: " + JSON.stringify({ id: props.id, name: props.name }, null, 2);
                        }
                    },
                });
            } catch (err) {
                paintMountError(err && err.message ? err.message : err);
            }
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountSecurity);
        } else {
            mountSecurity();
        }
    })();
</script>
</body>
</html>
