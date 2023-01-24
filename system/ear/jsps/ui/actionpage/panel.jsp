<%@page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://rhythmyx.percussion.com/components"
	prefix="rxcomp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
	


<%
	
String path = request.getContextPath();
String basePath = request.getScheme() + "://"
               + request.getServerName() + ":" + request.getServerPort() + path
               + "/";               
%>
<html>
<head>
	<base href="<%=basePath%>ui/actionpage">
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<title>${rxcomp:i18ntext('psx.sys_ActionPage.ActionPage@Rhythmyx Action Panel for',
		param.sys_lang)}&#160;${title}</title>
	<%@include file="../header.jsp"%>
	<link href="../sys_resources/css/actionpagefonts.css" rel="stylesheet" type="text/css"/>
	<link href="../rx_resources/css/actionpagefonts.css" rel="stylesheet" type="text/css"/>
	<link href="../sys_resources/css/actionpagelayout.css" rel="stylesheet" type="text/css"/>
	<script language="javascript" src="../sys_resources/js/globalErrorMessages.js">;</script>
	<script language="javascript" src="../sys_resources/js/browser.js">;</script>
	<script language="javascript" src="../sys_resources/js/href.js">;</script>
	<script language="javascript" src="../sys_resources/js/popmenu.js">;</script>
	<script>
      			var actionPageRefresh = "true";
      			         			
      			function doroll(el)
      			{
      				el.style.textDecoration = "underline";
      			}

      			function dorollout(el)
      			{
      				el.style.textDecoration = "none";
      			}
   </script>	
</head>
<body>
	<div id="RhythmyxBanner" style="border-bottom:none;">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="action-panel-header">${rxcomp:i18ntext('jsp_actionpage@Action Panel', param.sys_lang)}</td>
				<td align="right"  valign="bottom"><%@ include file="../userstatus-inner.jsp" %></td>
			</tr>
		</table>
	</div>
	${rxcomp:getInternal(url)}
</body>
</html>
