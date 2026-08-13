<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.percussion.webui.util.PSLegacyViewRedirect" %>
<%--
  Classic CM1 Design template-list entry retired (#3306 / parent #2631).
  Preserve bookmarks by hard-redirecting to SPA Design (template library).
  Incoming view= is always overridden to design (not preserved).
  index.jsp maps view=design → spa.jsp?entry=design (optional section preserved).
  editTemplate.jsp remains for the unmigrated visual template editor.
--%>
<%
    String target = PSLegacyViewRedirect.buildLocation("design", request.getQueryString());
    String htmlTarget = PSLegacyViewRedirect.escapeHtmlAttribute(target);
    response.setStatus(301);
    response.setHeader("Location", target);
%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="refresh" content="0;url=<%= htmlTarget %>"/>
    <title>Design moved</title>
</head>
<body>
<p>Design has moved to the modern UI. <a href="<%= htmlTarget %>">Continue</a>.</p>
</body>
</html>
