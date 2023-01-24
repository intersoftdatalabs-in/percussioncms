<%@ page import="org.apache.commons.lang.StringUtils" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>


<%
   String mode = request.getParameter("mode");
   String idPrefix = null;
   if(StringUtils.defaultString(mode, "sites").equals("sites"))
   {
      idPrefix = "ps.content.sitespanel.";
   }
   else
   {
      idPrefix = "ps.content.folderspanel.";
   }   

   

%>
<div dojoType="ps:PSSplitContainer" layoutAlign="client" orientation="vertical" sizerWidth="1"
           activeSizing="true" sizerEnabled="false" sizerVisible="true" persist="false" style="border: 0px solid black; width: 100%; height: 100%;"
           id="<%= idPrefix %>mainsplitpane">
   <jsp:include page="addressbarpanel.jsp" flush="true">
      <jsp:param name="idPrefix" value="<%= idPrefix %>" />
   </jsp:include>   
   <div dojoType="ContentPane" sizeMin="200" sizeShare="90" id="<%= idPrefix %>clientpanel">
      <div dojoType="ps:PSSplitContainer" layoutAlign="client" orientation="vertical" sizerWidth="1"
           activeSizing="true" persist="false" style="border: 0px solid black; width: 100%; height: 100%;"
           id="<%= idPrefix %>contentsplitpane">
           <div dojoType="ContentPane" style="background: #ffffff;overflow:auto;" id="<%= idPrefix %>tablepanel" sizeMin="100" sizeShare="65">
		      <jsp:include page="filteringtable.jsp" flush="true">
		        <jsp:param name="idPrefix" value="<%= idPrefix %>" />
		      </jsp:include>
           </div>
           <div dojoType="ContentPane" style="padding: 10px; overflow: hidden;" sizeMin="30" sizeShare="10" id="<%= idPrefix %>filterpanel">
		      <jsp:include page="filterpanel.jsp" flush="true">
		         <jsp:param name="idPrefix" value="<%= idPrefix %>" />
		      </jsp:include>
          </div>
     </div>
  </div>
  <div dojoType="ContentPane" style="padding:10px; overflow: hidden;" sizeMin="35" sizeShare="6" id="<%= idPrefix %>commandpanel">
	<jsp:include page="commandpanel.jsp" flush="true">
	   <jsp:param name="idPrefix" value="<%= idPrefix %>" />
	</jsp:include>
  </div>   
</div>
