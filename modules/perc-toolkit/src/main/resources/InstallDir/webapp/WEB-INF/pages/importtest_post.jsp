<%@ page language="java" contentType="text/html; charset=UTF-8"
		 pageEncoding="UTF-8"
		 import="com.percussion.design.objectstore.PSLocator"
		 import="com.percussion.pso.utils.IPSOItemSummaryFinder"
		 import="com.percussion.security.SecureStringUtils"
		 import="com.percussion.services.contentmgr.IPSContentMgr"
		 import="com.percussion.services.contentmgr.IPSNode"
		 import="com.percussion.services.contentmgr.PSContentMgrConfig"
		 import="com.percussion.services.contentmgr.PSContentMgrLocator, com.percussion.services.guidmgr.IPSGuidManager, com.percussion.services.guidmgr.PSGuidManagerLocator, com.percussion.services.guidmgr.PSGuidUtils, com.percussion.services.legacy.IPSCmsObjectMgr, com.percussion.services.legacy.PSCmsObjectMgrLocator, com.percussion.utils.guid.IPSGuid, com.percussion.webservices.content.IPSContentWs, com.percussion.webservices.content.PSContentWsLocator, com.percussion.webservices.security.IPSSecurityWs, com.percussion.webservices.security.PSSecurityWsLocator"
		 import="jakarta.servlet.jsp.JspWriter"
%>
<%@ page import="java.util.List" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>

	<%!
	//initialize variables used in the JSP page
	
	IPSGuidManager gmgr = PSGuidManagerLocator.getGuidMgr();
	IPSContentMgr mgr = PSContentMgrLocator.getContentMgr(); 
	IPSContentWs contentWs = PSContentWsLocator.getContentWebservice(); 
	IPSCmsObjectMgr objMgr = PSCmsObjectMgrLocator.getObjectManager();
	IPSSecurityWs securityWs = PSSecurityWsLocator.getSecurityWebservice();
	IPSOItemSummaryFinder isFinder = null; 
	List myGuids;
	PSContentMgrConfig myConfig = null;
	
	%>
	
	<%
		//Checking for vulnerability
		String str = request.getQueryString();
		if(str != null && str != ""){
			response.sendError(response.SC_FORBIDDEN, "Invalid QueryString!");
		}
	String cid=request.getParameter("sys_contentid");
		//Checking for vulnerability
		if(!SecureStringUtils.isValidPercId(cid)){
			response.sendError(response.SC_FORBIDDEN, "Invalid cid!");
		}
	PSLocator loc = isFinder.getCurrentOrEditLocator(cid);
	IPSGuid contentGUID=gmgr.makeGuid(loc);
	List myGuid = PSGuidUtils.toGuidList(contentGUID);
	List contentList = mgr.findItemsByGUID(myGuid, myConfig);

	IPSNode contentNode = (IPSNode)contentList.get(0);
	String cmsURL = contentNode.getProperty("feedFormat").toString();
	String feedURL = contentNode.getProperty("feedUrl").toString();
	%>

<html><head><title>REST Import Service</title>
	<script type="text/javascript">
	// Dependency-free vanilla JS (CDN jQuery 1.2.6 removed in #7369, parent
	// audit #5627): this test helper ships no third-party script bytes.
	function htmlEncode(value) {
	    var div = document.createElement("div");
	    div.textContent = value;
	    return div.innerHTML;
	}
	  document.addEventListener("DOMContentLoaded", function(){
	    document.getElementById("form").addEventListener("submit", function(event){
	        event.preventDefault();
	        dataString = request.getParameter("");
			posturl = document.getElementById("post_url").value;

	        fetch(posturl, {
	        method: "POST",
	        // url: "/Rhythmyx/services/Content/import/cdcSyndicationImport",
	        body: dataString,
			headers: {"Content-Type": "text/xml"}
	        }).then(function(response){
	            return response.text().then(function(text){
	                document.getElementById("message_ajax").innerHTML = htmlEncode(text);
	            });
	        });

	        return false;

	    });
	});
	</script>
  </head>
<body>

<table width="100%" height="66" background="/Rhythmyx/sys_resources/images/banner_bkgd.jpg" style="background-attachment: fixed; background-repeat: no-repeat;">
<tbody><tr>
<td>
</td>
</tr>
</tbody></table>
<h1>REST Import Service</h1>
	<csrf:form id="form" action="/Rhythmyx/services/Content/import/cvaSyndicationImport" method="post">
		<!--  CMS IMPORT URL:<br/><textarea rows="2" cols="80" NAME="post_url" id="post_url" ></textarea><br>  -->
		FEED URL:<br/>
		<textarea rows="2" cols="80" name="body" id="body"></textarea><br/>
		<input type="submit"></input>
	</csrf:form>
</body>
</html>
