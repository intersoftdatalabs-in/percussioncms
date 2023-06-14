<%@ page import="com.percussion.services.utils.jspel.PSRoleUtilities" %>
<%@ taglib uri="/WEB-INF/tmxtags.tld" prefix="i18n"%>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>
<%
	String locale= PSRoleUtilities.getUserCurrentLocale();
	String lang="en";
	if(locale==null){
		locale="en-us";
	}else{
		if(locale.contains("-"))
			lang=locale.split("-")[0];
		else
			lang=locale;
	}
    String debug = request.getParameter("debug");
    String status = request.getParameter("status");
    String msgClass = null;
    if(status != null && status.equals("PERC_SUCCESS"))
        msgClass = "perc-success";
    else if(status != null && status.equals("PERC_ERROR"))
        msgClass = "perc-error";
    String message = request.getParameter("message");
%>
<i18n:settings lang="<%=locale %>" prefixes="perc.ui." debug="<%= debug %>"/>

<div id="perc-category-menu" style="height:54px;"> 

<div class="dropdown"  id="perc-categories-publish">
    <button tabindex="0" title='<i18n:message key = "perc.ui.perc.categories@Publish"/>' id="perc-categories-publish-dropdown" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown" style="border-style: outset; border-width:2px;"><i18n:message key = "perc.ui.perc.categories@Publish"/>
    <span class="caret"></span></button>
    <ul id="perc-categories-publish-dropdown-menu" class="dropdown-menu pull-right" role="menu" aria-labelledby="perc-categories-publish-dropdown">
      <li role="presentation"><a tabindex="0" title='<i18n:message key = "perc.ui.perc.categories@Publish Staging DTS"/>' role="menuitem" href="#" id="perc-categories-publish-staging"><i18n:message key = "perc.ui.perc.categories@Publish Staging DTS"/></a></li>
      <li role="presentation"><a tabindex="0" title='<i18n:message key = "perc.ui.perc.categories@Publish Production DTS"/>' role="menuitem" href="#" id="perc-categories-publish-production"><i18n:message key = "perc.ui.perc.categories@Publish Production DTS"/></a></li>
      <li role="presentation"><a tabindex="0" title='<i18n:message key = "perc.ui.perc.categories@Publish to Both"/>' role="menuitem" href="#" id="perc-categories-publish-both"><i18n:message key = "perc.ui.perc.categories@Publish to Both"/></a></li>
    </ul>

</div>

</div>
<div id="perc-pageEditor-toolbar-content" class="ui-helper-clearfix"> </div> 
<div class='perc-whitebg' style="overflow : auto">
    <div id = "perc-category-wrapper" style="width:1024px">
        <div id = "perc-category-title">
            <h1><i18n:message key = "perc.ui.admin.workflow@Categories"/></h1>
        </div>
        <div id = "perc-site-selection">
               <select id = "perc-category-site-dropdown">
                   <option value=""><i18n:message key = "perc.ui.perc.categories@No Sites"/></option>
               </select>
            </div>
            
            <div id = "perc-category-tree">
            </div>
        <div id="perc-category-details">
            <div id="perc-categories-add-category-button" role="button" tabindex="0"  title="<i18n:message key = "perc.ui.categories@Add New Category"/>"></div>
            <div id="perc-categories-add-child-category-button" role="button"  tabindex="0" title="<i18n:message key = "perc.ui.categories@Add New Child Category"/>"></div>
            <div id="perc-categories-delete-category-button" role="button" tabindex="0" title="<i18n:message key = "perc.ui.categories@Remove Category"/>"></div>
            <div id="perc-categories-edit-category-button" role="button" tabindex="0" title="<i18n:message key = "perc.ui.categories@Edit Category Details"/>"></div>
            <div id="perc-categories-moveup-button" role="button" tabindex="0" title="<i18n:message key = "perc.ui.categories@Move Up"/>"><i class="perc-font-icon icon-arrow-up fas fa-arrow-up"></i></div>
            <div id="perc-categories-movedown-button" role="button" tabindex="0" title="<i18n:message key = "perc.ui.categories@Move Down"/>"><i class="perc-font-icon icon-arrow-down fas fa-arrow-down"></i></div>
            <div id="perc-category-info">
                <span class="perc-required-label" style="display:none;"><label><i18n:message key = "perc.ui.general@Denotes Required Field"/></label></span> 
                <div id="perc-category-name-label"><i18n:message key = "perc.ui.perc.categories@Category Name"/>
					<input id="perc-category-name-field" maxlength="255" title='<i18n:message key = "perc.ui.perc.categories@Category Name"/>'/>
                </div><br />
                <div id="perc-category-selectable-label"><i18n:message key = "perc.ui.perc.categories@Is It Selectable"/>
					<input type="checkbox" id="perc-category-selectable-field" title='<i18n:message key = "perc.ui.perc.categories@Is It Selectable"/>'/>
                </div><br />
                <div id="perc-category-show-in-page-label"><i18n:message key = "perc.ui.perc.categories@Show in Page Metadata"/>
					<input type="checkbox" id="perc-category-show-in-page-field" title='<i18n:message key = "perc.ui.perc.categories@Show in Page Metadata"/>'/>
                </div><br />
                <div id="perc-allowedsites-label"><i18n:message key = "perc.ui.perc.categories@Allowed Sites"/> 
					<select id="perc-allowedsites-field" multiple title='<i18n:message key = "perc.ui.perc.categories@Allowed Sites"/>'>
                    </select>
                </div><br />
                <div id="perc-category-save-cancel-block" style="width:100%; height:50px;"> 
                    <button id="perc-category-save" tabindex="0" title='<i18n:message key ="perc.ui.button@Save"/>'  class="btn btn-primary" type="button"  name="perc_wizard_save"   style="float: right; margin:0 27px 27px 0px;"><i18n:message key ="perc.ui.button@Save"/> </button>
                    <button id="perc-category-cancel" tabindex="0" title='<i18n:message key ="perc.ui.common.label@Cancel"/>' class="btn btn-primary" type="button"  name="perc_wizard_cancel" style="float: right; margin: 0 8px 27px 0"><i18n:message key ="perc.ui.common.label@Cancel"/>  </button>
                </div>
                <div id="perc-category-details-label"><i18n:message key = "perc.ui.perc.categories@Details"/></div><br />
                <div id="perc-category-created-by-label"><i18n:message key = "perc.ui.perc.categories@Created By"/> 
					<input id="perc-category-createdby-field" class="perc-category-field-readonly" disabled title='<i18n:message key = "perc.ui.perc.categories@Created By"/>'/>
                </div><br />
                <div id="perc-category-creation-date-label"><i18n:message key = "perc.ui.perc.categories@Creation Date"/> 
					<input id="perc-category-creationdt-field" class="perc-category-field-readonly" disabled title='<i18n:message key = "perc.ui.perc.categories@Creation Date"/>'/>
                </div><br />
                <div id="perc-category-last-modified-by-label"><i18n:message key = "perc.ui.perc.categories@Last Modified By"/> 
					<input id="perc-category-lstmodifiedby-field" class="perc-category-field-readonly" disabled title='<i18n:message key = "perc.ui.perc.categories@Last Modified By"/>'/>
                </div><br />
                <div id="perc-category-last-modified-date-label"><i18n:message key = "perc.ui.perc.categories@Last Modified Date"/>
					<input id="perc-category-lstmodifieddt-field" class="perc-category-field-readonly" disabled title='<i18n:message key = "perc.ui.perc.categories@Last Modified Date"/>'/>
                </div><br />
              
            </div>
        </div>
    </div>    
</div>
