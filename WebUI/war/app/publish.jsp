<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  Classic Minuet publish entry retired (US8 / Erlang S1). Preserve bookmarks by
  redirecting to the modern Publishing shell under /cm/app/.
  Query string is preserved for section/siteId deep links.
--%>
<%
    String qs = request.getQueryString();
    String target = "/cm/app/?view=publish";
    if (qs != null && !qs.isEmpty()) {
        target = "/cm/app/?" + qs;
        if (!qs.contains("view=")) {
            target = "/cm/app/?view=publish&" + qs;
        }
    }
    response.setStatus(301);
    response.setHeader("Location", target);
%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="refresh" content="0;url=<%= target %>"/>
    <title>Publishing moved</title>
</head>
<body>
<p>Publishing has moved to the modern UI. <a href="<%= target %>">Continue</a>.</p>
</body>
</html>
