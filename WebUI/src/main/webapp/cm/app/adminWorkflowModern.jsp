<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  PR-5: retired product host. Redirects via index.jsp to spa.jsp?entry=workflow.
  File retained as reference until PR-8 deletes obsolete *Modern.jsp shells.
--%>
<%
    request.setAttribute("retiredModernView", "workflow");
%>
<%@ include file="includes/retired_modern_redirect.jsp" %>
