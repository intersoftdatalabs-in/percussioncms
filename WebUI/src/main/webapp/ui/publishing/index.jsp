<%@page errorPage="/ui/error.jsp" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>



<%
   /* Always set focus on "Sites" node and make all nodes clickable */
 %>
<c:set var="invoke_focusOnStartingNode" scope="request"
   value="${sys_design_navigation.focusOnStartingNode}" />

<%
	// encodeURL (capital URL) — encodeUrl was removed in Jakarta Servlet
	response.sendRedirect(response.encodeURL(request.getContextPath()
		+ "/ui/publishing/SiteList.faces"));
%>
