<%@page errorPage="/ui/error.jsp" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib tagdir="/WEB-INF/tags/layout" prefix="layout"%>
<%@ taglib uri="http://myfaces.apache.org/trinidad" prefix="tr"%>
<%@ taglib uri="http://rhythmyx.percussion.com/components"
	prefix="rxcomp"%>
<%@ taglib tagdir="/WEB-INF/tags/nav" prefix="rxnav"%>


<c:set var="page_title" scope="request" value="Rhythmyx - Site Publishing Logs" />
<layout:pubruntime>
	<jsp:body>	
		<rxcomp:menubar>
			<rxcomp:menu label="Action">
				<rxcomp:menuitem value="Archive Selected Logs" 
					action="#{sys_runtime_navigation.currentNode.archiveSelected}" />	
            <rxcomp:menuitem value="Delete Selected Logs" 
               action="#{sys_runtime_navigation.currentNode.purgeSelected}" /> 
            <rxcomp:menuitem value="Delete Site Item Entries" 
               action="pub-runtime-delete-site-item-warn" />                
			</rxcomp:menu>		
			<rxcomp:menuitem value="Help" 
				onclick="openHelpWindow('#{sys_runtime_navigation.currentNode.helpFile}')"/>
			
		</rxcomp:menubar>	
		<rxnav:runtimebreadcrumbs/>
		<tr:panelBox
			rendered="#{! empty sys_runtime_navigation.currentNode.archiveLocation}">
         <tr:panelHorizontalLayout>
				<tr:inputText readOnly="true" value="The archived log is in the following location:  " />
				<tr:goLink text="#{sys_runtime_navigation.currentNode.archiveLocation}"
				   rendered="#{sys_runtime_navigation.currentNode.remoteViewableArchive}" 
					targetFrame="_blank"
					destination="#{sys_runtime_navigation.currentNode.archiveLink}" />
				<tr:inputText readOnly="true"  
				   rendered="#{! sys_runtime_navigation.currentNode.remoteViewableArchive}"
				   value="#{sys_runtime_navigation.currentNode.archiveLink}" />
         </tr:panelHorizontalLayout>
		</tr:panelBox>
		<tr:panelHeader styleClass="rxPanelHeader" text="Logs">
			<tr:table rowSelection="multiple" var="entry" width="100%" rowBandingInterval="1"
			   selectedRowKeys="#{sys_runtime_navigation.currentNode.selectedRowKeys}"
				rows="#{sys_runtime_navigation.currentNode.pageRows}" 
				value="#{sys_runtime_navigation.currentNode.processedStatusLogs}">
				<tr:column headerText="Job ID" sortable="true" sortProperty="statusid">
					<tr:commandLink text="#{entry.statusid}" 
						action="#{entry.statusentry.perform}"/>
				</tr:column>
            <tr:column headerText="Edition" sortable="true" sortProperty="edition">
					<tr:commandLink text="#{entry.edition}" 
						action="#{entry.statusentry.perform}"/>
            </tr:column>
            <tr:column headerText="Site"  sortable="true" sortProperty="site" 
               rendered="#{sys_runtime_navigation.currentNode.showSiteColumn}">
               <tr:outputText value="#{entry.site}"/>
            </tr:column>
				<tr:column headerText="Start Time" sortable="true" sortProperty="startTime">
					<tr:outputText value="#{entry.start}"/>
				</tr:column>
				<tr:column headerText="Elapsed (HH:mm:ss)" sortable="true" sortProperty="elapsed">
					<tr:outputText value="#{entry.elapsed}"/>
				</tr:column>
				<tr:column headerText="Status" width="23px" sortable="true" sortProperty="statusDesc">
			      <tr:image source="#{entry.statusImage}" shortDesc="#{entry.statusDesc}"/>
				</tr:column>
				<tr:column headerText="Delivered" sortable="true" sortProperty="delivered">
					<tr:outputText value="#{entry.delivered}"/>
				</tr:column>
				<tr:column headerText="Removed" sortable="true" sortProperty="removed">
					<tr:outputText value="#{entry.removed}"/>
				</tr:column>
				<tr:column headerText="Failures" sortable="true" sortProperty="failures">
					<tr:outputText value="#{entry.failures}"/>
				</tr:column>
			</tr:table>
		</tr:panelHeader>
	</jsp:body>
</layout:pubruntime>
