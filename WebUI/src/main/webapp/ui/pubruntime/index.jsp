<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String target = "/cm/app/?view=publish&section=runtime";
    response.setStatus(301);
    response.setHeader("Location", target);
%>
<!DOCTYPE html>
<html>
<head><meta http-equiv="refresh" content="0;url=<%= target %>"/><title>Publishing Runtime moved</title></head>
<body>
<p>Publishing Runtime is now in the modern Publishing UI. <a href="<%= target %>">Open Runtime</a>.</p>
</body>
</html>
