<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:f="http://java.sun.com/jsf/core"
	xmlns:h="http://java.sun.com/jsf/html"
	xmlns:t="http://myfaces.apache.org/tomahawk"
	xmlns:tr="http://myfaces.apache.org/trinidad"
	xmlns:trh="http://myfaces.apache.org/trinidad/html"
	xmlns:rxb="urn:jsptagdir:/WEB-INF/tags/banner"
	version="1.2">
	<f:view>
		<tr:document styleClass="backgroundcolor" 
			title="#{page_title}" onload="#{page_onload}" >
			<f:facet name="metaContainer">
				<f:verbatim>
					<meta http-equiv="pragma" content="no-cache" />
					<meta http-equiv="cache-control" content="no-cache" />
					<meta http-equiv="expires" content="0" />
					<link rel="stylesheet" type="text/css"
						href="/Rhythmyx/sys_resources/css/menu.css" />
					<script type="text/javascript" src="/Rhythmyx/sys_resources/js/jsf/Utils.js">;</script>
					<jsp:directive.include file="/ui/header.jsp" />
					${page_script}
					<script type="text/javascript">
					
					   /*
					    * This is a hack to get around an IE7 bug where the table
					    * was not resizing on tree collapse
					    */
					   function forcePanelCollapse()
					   {
						var tree = document.getElementById("rxNavTree");
						var theTd = tree.parentNode.parentNode.previousSibling;
						try
						{
						   if(theTd.style.height == "2px")
						   {
						      theTd.style.height = "1px";
						   }
						   else
						   {
                                                      theTd.style.height = "2px";
						   }
						}
						catch(ignore){}
					   }

					   function forcePanelCollapseDelayed()
					   {
                                              if(psJsfUtil.isExplorer6())
					         setTimeout("psJsfUtil.navTreeNodeIE6Fix()", 120);
					      setTimeout("forcePanelCollapse()", 250);
					   }

					   function openHelpWindow(helpUrl, isTool)
					   {
					      var prefix = '../..';
					      if (isTool == true)
					      	prefix = '../../..';
					      	
					      var hwin = window.open(prefix + '/Docs/Rhythmyx/Rhythmyx_Administration_Tab_Help/index.htm?toc.htm?' + helpUrl,"HelpWindow");
					      hwin.focus();
					   }

					</script>

				</f:verbatim>
			</f:facet>

			<tr:form rendered="#{sys_admin_navigation.isLocked}" defaultCommand="submitForm">
				<f:verbatim>
					<rxb:banner/>
				</f:verbatim>
				<h:panelGrid styleClass="pub-page-layout" columns="2" rowClasses="pub-design-layout" 
					columnClasses="pub-design-nav,pub-design-content">
					<t:div />
					<t:div style="admin-content">
						<jsp:doBody />
					</t:div>
				</h:panelGrid>
			</tr:form>

			<tr:form rendered="#{!sys_admin_navigation.isLocked}" id="admin" defaultCommand="submitForm">
				<f:verbatim>
					<rxb:banner/>
				</f:verbatim>
				<h:panelGrid style="width: 100%" columns="2" rowClasses="admin-layout" 
					columnClasses="admin-nav,admin-content">
					<tr:panelBox styleClass="admin-nav" >
						<tr:navigationTree var="node" id="rxNavTree" onclick="forcePanelCollapseDelayed();"
							disclosedRowKeys="#{sys_admin_navigation.disclosedRows}"
							value="#{sys_admin_navigation.tree}">
							<f:facet name="nodeStamp">
								<tr:commandNavigationItem 
									action="#{node.perform}" styleClass="#{node.navLinkClass}"
									disabled="#{!node.enabled}" text="#{node.label}"
									shortDesc="#{node.title}" selected="#{node.selected}" />
							</f:facet>
						</tr:navigationTree>
					</tr:panelBox>
					<t:div style="admin-content">
						<jsp:doBody />
					</t:div>
				</h:panelGrid>
			</tr:form>
		</tr:document>
	</f:view>
</jsp:root>
