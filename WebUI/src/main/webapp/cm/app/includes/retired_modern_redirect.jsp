<%@ page import="com.percussion.server.PSServer" %>
<%@ page import="java.net.URLEncoder" %>
<%--
  PR-5: shared redirect for retired *Modern.jsp product hosts.
  Expects request attribute "retiredModernView" (e.g. home, publish, workflow, admin,
  widgetbuilder, unavailable). Re-enters /cm/app/?view=… so index.jsp role gates and
  spa.jsp?entry=… mapping stay authoritative (proxyURL parity, query contract only).
--%>
<%
    String retiredView = (String) request.getAttribute("retiredModernView");
    if (retiredView == null || retiredView.isBlank()) {
        retiredView = "home";
    }
    String proxyURL = "";
    if (PSServer.isRequestBehindProxy(request)) {
        proxyURL = PSServer.getProxyURL(request, true);
    }
    StringBuilder url = new StringBuilder();
    url.append(proxyURL).append("/cm/app/?view=").append(URLEncoder.encode(retiredView, "UTF-8"));
    // Re-forward only params that index.jsp buildSpaEntryRedirect actually consumes
    // (then allowlists into spa.jsp?entry=…). Do not pass path/site — those are not
    // mapped for home/publish/workflow/admin/widgetbuilder SPA entries from this host.
    String[] keys = new String[]{
            "initialScreen", "section", "tab", "siteId", "serverId"
    };
    for (int i = 0; i < keys.length; i++) {
        String key = keys[i];
        String value = request.getParameter(key);
        if (value == null || value.isBlank()) {
            continue;
        }
        url.append("&");
        url.append(URLEncoder.encode(key, "UTF-8"));
        url.append("=");
        url.append(URLEncoder.encode(value, "UTF-8"));
    }
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expires", 0);
    response.sendRedirect(url.toString());
%>
