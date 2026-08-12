<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  Classic CM1 Architecture entry retired (#3099 / parent #3092).
  Preserve bookmarks by hard-redirecting to SPA Architecture.
  index.jsp maps view=arch → spa.jsp?entry=architecture (optional site preserved).
--%>
<%
    String qs = request.getQueryString();
    String target = "/cm/app/?view=arch";
    if (qs != null && !qs.isEmpty()) {
        // Avoid double view=arch; allowlist-style append of original params
        target = "/cm/app/?" + qs;
        if (!qs.contains("view=")) {
            target = "/cm/app/?view=arch&" + qs;
        }
    }
    response.setStatus(301);
    response.setHeader("Location", target);
%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="refresh" content="0;url=<%= target %>"/>
    <title>Architecture moved</title>
</head>
<body>
<p>Architecture has moved to the modern UI. <a href="<%= target %>">Continue</a>.</p>
</body>
</html>
