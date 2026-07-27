<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String target = "/cm/app/?view=publish&section=design";
    response.setStatus(301);
    response.setHeader("Location", target);
%>
<!DOCTYPE html>
<html>
<head><meta http-equiv="refresh" content="0;url=<%= target %>"/><title>Publishing Design moved</title></head>
<body>
<p>Publishing Design is now in the modern Publishing UI. <a href="<%= target %>">Open Design</a>.</p>
</body>
</html>
