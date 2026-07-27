<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  PR-6: retired product host. Content Explorer is the SPA route
  spa.jsp?entry=explorer. Re-enter SPA query contract (proxyURL-aware).
--%>
<%@ page import="com.percussion.server.PSServer" %>
<%@ page import="java.net.URLEncoder" %>
<%
    String proxyURL = "";
    if (PSServer.isRequestBehindProxy(request)) {
        proxyURL = PSServer.getProxyURL(request, true);
    }
    StringBuilder url = new StringBuilder();
    url.append(proxyURL).append("/cm/app/spa.jsp?entry=explorer");
    String rawPath = request.getParameter("path");
    if (rawPath == null || rawPath.isBlank()) {
        rawPath = request.getParameter("initialPath");
    }
    if (rawPath != null
            && rawPath.startsWith("/")
            && rawPath.length() < 2048
            && rawPath.matches("[/A-Za-z0-9._-]+")
            && !rawPath.contains("..")) {
        url.append("&path=").append(URLEncoder.encode(rawPath, "UTF-8"));
    }
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expires", 0);
    response.sendRedirect(url.toString());
%>
