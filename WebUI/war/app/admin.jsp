<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  Classic CM1 Design template-list entry retired (#3306 / parent #2631).
  Preserve bookmarks by hard-redirecting to SPA Design (template library).
  index.jsp maps view=design → spa.jsp?entry=design (optional section preserved).
  editTemplate.jsp remains for the unmigrated visual template editor.
--%>
<%
    String qs = request.getQueryString();
    String target = "/cm/app/?view=design";
    if (qs != null && !qs.isEmpty()) {
        target = "/cm/app/?" + qs;
        if (!qs.contains("view=")) {
            target = "/cm/app/?view=design&" + qs;
        }
    }
    response.setStatus(301);
    response.setHeader("Location", target);
%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="refresh" content="0;url=<%= target %>"/>
    <title>Design moved</title>
</head>
<body>
<p>Design has moved to the modern UI. <a href="<%= target %>">Continue</a>.</p>
</body>
</html>
