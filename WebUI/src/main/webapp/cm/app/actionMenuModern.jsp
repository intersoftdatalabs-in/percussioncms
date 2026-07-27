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
    <title><i18n:message key="perc.ui.actionMenu@Action Menu (US3 P-Menu pilot)"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; background: #fafafa; color: #222; }
        #perc-action-menu-host { max-width: 1024px; margin: 0 auto; }
        h1 { font-size: 1.1rem; margin: 0 0 16px 0; }
    </style>
</head>
<body>
<div id="perc-action-menu-host">
    <h1>Action Menu (US3 P-Menu pilot)</h1>
    <p style="color:#555; font-size:0.9rem; margin:0 0 12px 0;">
        US3 P-Menu: the modern ContextMenu and ActionToolbar surfaces mount on
        this page. The toolbar at the top renders the configured actions
        returned by <code>/actions/find</code>; the menu below surfaces
        the same actions when the user selects an item.
    </p>
    <div id="perc-action-toolbar-root"
         data-testid="perc-action-toolbar-root"
         style="margin-bottom: 24px;"></div>
    <div id="perc-context-menu-root"
         data-testid="perc-context-menu-root"></div>
    <pre id="perc-action-menu-result"
         data-testid="perc-action-menu-result"
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
        function mountActionMenu() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountActionMenu, 50);
                return;
            }
            // ActionToolbar: empty actions will render the empty-state
            // placeholder on dev CMS (which has no installed action menus).
            // For demonstrative purposes, also mount a ContextMenu with
            // two synthetic sample actions so the keyboard navigation and
            // click activation can be exercised.
            window.PercModernUI.mount("perc-action-toolbar-root", "ActionToolbar", {
                actions: [],
                emptyMessage: "perc.ui.explorer@No actions available",
            });
            window.PercModernUI.mount("perc-context-menu-root", "ContextMenu", {
                actions: [
                    { name: "open", label: "Open", url: "/cm/app/spa.jsp?entry=explorer", handler: undefined, sortRank: 10, menuType: "MENUITEM" },
                    { name: "preview", label: "Preview", url: undefined, handler: "client", sortRank: 20, menuType: "MENUITEM" }
                ],
                ariaLabel: "Demo context menu",
                onInvoke: function (actionName) {
                    var out = document.getElementById("perc-action-menu-result");
                    if (out) {
                        out.textContent = "Invoked: " + actionName;
                    }
                },
                onClose: function () {
                    var out = document.getElementById("perc-action-menu-result");
                    if (out) {
                        out.textContent = "Closed";
                    }
                }
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountActionMenu);
        } else {
            mountActionMenu();
        }
    })();
</script>
</body>
</html>
