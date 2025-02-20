<%@page errorPage="/ui/error.jsp"  pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib tagdir="/WEB-INF/tags/layout" prefix="layout"%>
<%@ taglib uri="http://myfaces.apache.org/trinidad" prefix="tr"%>
<%@ taglib uri="http://rhythmyx.percussion.com/components"
   prefix="rxcomp"%>
<%@ taglib tagdir="/WEB-INF/tags/nav" prefix="rxnav"%>



<c:set var="page_title" scope="request" value="Rhythmyx - Add Context Variable Page" />
<layout:publishing>
   <jsp:body>
   <rxcomp:menubar>
      <rxcomp:menuitem value="Add" 
         action="#{sys_design_navigation.currentNode.addContextVariableCompletion}" />
      <rxcomp:menuitem value="Cancel" immediate="true"
         action="#{sys_design_navigation.currentNode.cancelVariableAction}"/>
			<rxcomp:menuitem value="Help" 
				onclick="openHelpWindow('#{sys_design_navigation.currentNode.currContextVariable.helpFile}')"/>

   </rxcomp:menubar>
   <rxnav:listbreadcrumbs />
      <tr:panelFormLayout inlineStyle="width: 50%">
         <tr:inputText label="Context Variable Name"
               value="#{sys_design_navigation.currentNode.currContextVariable.name}"
               validator="#{sys_design_navigation.currentNode.validateContextVariableName}"
               maximumLength="50"
               required="true">
               <f:validator validatorId="com.percussion.jsf.name"/>
         </tr:inputText>      
         <tr:inputText label="Context Variable Value" columns="80"
               value="#{sys_design_navigation.currentNode.currContextVariable.value}"
               maximumLength="255"
               required="true"/>
         <tr:selectOneChoice label="Context" required="true"
            value="#{sys_design_navigation.currentNode.currContextVariable.contextName}"
            validator="#{sys_design_navigation.currentNode.validateContextName}">
            <f:selectItems value="#{sys_design_navigation.currentNode.contexts}"/>
         </tr:selectOneChoice>
      </tr:panelFormLayout>
		<tr:separator/>
		<tr:panelHeader styleClass="rxPanelHeader" text="All Context Variables">
			<tr:table var="prop" 
				value="#{sys_design_navigation.currentNode.allSiteProperties}"
				rows="25" width="100%" rowBandingInterval="1" >

				<tr:column headerText="Copy">
		   	   <tr:commandLink action="#{prop.copy}">
				      <tr:image source="../../sys_resources/images/copy.gif" shortDesc="Copy to above fields"/>
               </tr:commandLink>
				</tr:column>
				<tr:column sortable="true" sortProperty="name" headerText="Name (Id)">
					<tr:outputText value="#{prop.nameWithId}"/>
				</tr:column>
				<tr:column sortable="true" sortProperty="value" headerText="Value">
					<tr:outputText value="#{prop.value}"/>
				</tr:column>
				<tr:column sortable="true" sortProperty="contextName" headerText="Context (Id)">
					<tr:outputText value="#{prop.contextNameWithId}"/>
				</tr:column>
				<tr:column sortable="true" sortProperty="siteName" headerText="Site (Id)">
					<tr:outputText value="#{prop.siteNameWithId}"/>
				</tr:column>
			</tr:table>
		</tr:panelHeader>		
  </jsp:body>
</layout:publishing>
