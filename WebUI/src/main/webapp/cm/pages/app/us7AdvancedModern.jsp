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
    <title><i18n:message key="perc.ui.us7advanced@US7 Advanced CE (P-Adv pilot)"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="_csrf_header" content="OWASP-CSRFTOKEN"/>
    <script src="/Rhythmyx/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
    <script src="/JavaScriptServlet"></script>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; background: #fafafa; color: #222; }
        h1 { font-size: 1.1rem; margin: 0 0 12px 0; }
        h2 { font-size: 1rem; margin: 24px 0 12px 0; }
        section { margin-bottom: 16px; }
    </style>
</head>
<body>
<div id="perc-us7-host" data-testid="perc-us7-host">
    <h1>US7 Advanced CE (P-Adv pilot)</h1>
    <p style="color:#555; font-size:0.9rem; margin:0 0 16px 0;">
        US7 P-Adv: the modern advanced-CE surfaces (clipboard +
        wizards + dependency + relationships) mount on this page.
        The ClipboardPanel shows the in-memory state; the
        SiteCopyWizard / SubfolderCopyWizard exercise the
        multi-step state machine; the DependencyViewer and
        RelationshipsView render the 6 capability-matrix dimensions
        (client-side preview until the typed relationships REST
        enhancement lands — see US7 research notes).
    </p>

    <section id="perc-clipboard-section" data-testid="perc-clipboard-section">
        <h2>Clipboard</h2>
        <div id="perc-clipboard-root" data-testid="perc-clipboard-root"></div>
    </section>

    <section id="perc-site-copy-section" data-testid="perc-site-copy-section">
        <h2>Site Copy</h2>
        <div id="perc-site-copy-root" data-testid="perc-site-copy-root"></div>
    </section>

    <section id="perc-subfolder-copy-section" data-testid="perc-subfolder-copy-section">
        <h2>Subfolder Copy</h2>
        <div id="perc-subfolder-copy-root" data-testid="perc-subfolder-copy-root"></div>
    </section>

    <section id="perc-dependency-section" data-testid="perc-dependency-section">
        <h2>Dependency</h2>
        <div id="perc-dependency-root" data-testid="perc-dependency-root"></div>
    </section>

    <section id="perc-relationships-section" data-testid="perc-relationships-section">
        <h2>IA Relationships</h2>
        <div id="perc-relationships-root" data-testid="perc-relationships-root"></div>
    </section>
</div>
<script>
    (function () {
        if (!document.querySelector('script[src*="perc-modern-ui.js"]')) {
            var s = document.createElement("script");
            s.type = "module";
            s.src = "/cm/modern/assets/perc-modern-ui.js?cb=" + Date.now();
            document.head.appendChild(s);
        }
        function mountAll() {
            if (!window.PercModernUI || typeof window.PercModernUI.mount !== "function") {
                window.setTimeout(mountAll, 50);
                return;
            }
            // ClipboardPanel: pre-populated with one entry to exercise
            // the size badge + the items list. The "Add" / "Clear"
            // buttons drive additional state transitions when
            // interacted with.
            window.PercModernUI.mount("perc-clipboard-root", "ClipboardPanel", {
                clipboard: {
                    operation: "copy",
                    items: [
                        { id: "i-1", path: "/Sites/Foo/Bar", kind: "page", sourceAccessLevel: "ADMIN" }
                    ],
                    updatedAt: new Date().toISOString(),
                },
                onClipboardChange: function (next) {
                    window.__us7Clipboard = next;
                },
                mode: "copy",
                onModeChange: function (m) {
                    window.__us7ClipboardMode = m;
                },
                target: { path: "/Sites/Baz", accessLevel: "ADMIN" },
                onPasteSettled: function (summary) {
                    window.__us7PasteSummary = summary;
                },
            });
            // SiteCopyWizard: empty initial sources / target.
            window.PercModernUI.mount("perc-site-copy-root", "SiteCopyWizard", {});
            // SubfolderCopyWizard: empty initial paths.
            window.PercModernUI.mount("perc-subfolder-copy-root", "SubfolderCopyWizard", {});
            // DependencyViewer + RelationshipsView: a single synthetic
            // item so the 6 dimensions render with the AA row
            // populated and the rest marked unknown.
            var item = { id: "u-1", folderPath: "/Sites/Foo/Bar", type: "page" };
            window.PercModernUI.mount("perc-dependency-root", "DependencyViewer", {
                item: item,
                aaLinkCount: 3,
            });
            window.PercModernUI.mount("perc-relationships-root", "RelationshipsView", {
                item: item,
                aaLinkCount: 3,
            });
        }
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", mountAll);
        } else {
            mountAll();
        }
    })();
</script>
</body>
</html>
