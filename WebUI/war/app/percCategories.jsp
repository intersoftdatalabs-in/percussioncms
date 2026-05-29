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
<div class='perc-whitebg' style="overflow : auto; padding: 20px;">
    <div id="perc-category-wrapper" class="container-fluid" style="width: auto; max-width: 1200px;">
        <!-- Title and Dropdowns -->
        <div class="row" style="margin-bottom: 20px;">
            <div class="col-md-6">
                <div id="perc-category-title">
                    <h1 style="margin-top: 0;"><i18n:message key="perc.ui.admin.workflow@Categories"/></h1>
                </div>
            </div>
            <div class="col-md-6 text-right">
                <div id="perc-site-selection" style="display:inline-block; margin-right: 15px;">
                   <select id="perc-category-site-dropdown" class="form-control" style="display:inline-block; width: auto;">
                       <option value=""><i18n:message key="perc.ui.perc.categories@No Sites"/></option>
                   </select>
                </div>
            </div>
        </div>

        <div class="row">
            <!-- Left Column: Tree and Actions -->
            <div class="col-md-5">
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <div class="btn-group" role="group" aria-label="Category Actions">
                            <button id="perc-categories-add-category-button" type="button" class="btn btn-default" title="<i18n:message key='perc.ui.categories@Add New Category'/>"><i class="fas fa-plus"></i></button>
                            <button id="perc-categories-add-child-category-button" type="button" class="btn btn-default" title="<i18n:message key='perc.ui.categories@Add New Child Category'/>"><i class="fas fa-level-down-alt"></i></button>
                            <button id="perc-categories-delete-category-button" type="button" class="btn btn-default" title="<i18n:message key='perc.ui.categories@Remove Category'/>"><i class="fas fa-trash"></i></button>
                            <button id="perc-categories-moveup-button" type="button" class="btn btn-default" title="<i18n:message key='perc.ui.categories@Move Up'/>"><i class="fas fa-arrow-up"></i></button>
                            <button id="perc-categories-movedown-button" type="button" class="btn btn-default" title="<i18n:message key='perc.ui.categories@Move Down'/>"><i class="fas fa-arrow-down"></i></button>
                        </div>
                    </div>
                    <div class="panel-body" style="max-height: 600px; overflow-y: auto;">
                        <div id="perc-category-tree" style="width: 100%; float: none;"></div>
                    </div>
                </div>
            </div>
            
            <!-- Right Column: Details Form -->
            <div class="col-md-7">
                <div id="perc-category-details" class="panel panel-default" style="width: 100%; float: none; margin-left: 0; margin-top: 0;">
                    <div class="panel-heading">
                        <h3 class="panel-title" id="perc-category-details-label" style="display:inline-block; margin-top:6px;">
                            <i18n:message key="perc.ui.perc.categories@Details"/>
                        </h3>
                        <!-- Save/Cancel now at top of the form per UX request -->
                        <div class="pull-right">
                            <button id="perc-categories-edit-category-button" type="button" class="btn btn-primary btn-sm" title="<i18n:message key='perc.ui.categories@Edit Category Details'/>"><i class="fas fa-edit"></i> <i18n:message key="perc.ui.categories@Edit Category Details"/></button>
                        </div>
                        <div id="perc-category-save-cancel-block" class="pull-right" style="display: none; margin-right: 8px;">
                            <button id="perc-category-cancel" tabindex="0" title='<i18n:message key="perc.ui.common.label@Cancel"/>' class="btn btn-default btn-sm" type="button" name="perc_wizard_cancel"><i18n:message key="perc.ui.common.label@Cancel"/></button>
                            <button id="perc-category-save" tabindex="0" title='<i18n:message key="perc.ui.button@Save"/>' class="btn btn-primary btn-sm" type="button" name="perc_wizard_save"><i18n:message key="perc.ui.button@Save"/></button>
                        </div>
                        <div class="clearfix"></div>
                    </div>
                    <div class="panel-body" id="perc-category-info" style="background-color: transparent;">
                        <span class="perc-required-label text-danger" style="display:none; margin-bottom: 10px;"><label><i18n:message key="perc.ui.general@Denotes Required Field"/></label></span> 
                        
                        <form class="form-horizontal">
                            <div class="form-group" id="perc-category-name-label">
                                <label class="col-sm-4 control-label"><i18n:message key="perc.ui.perc.categories@Category Name"/></label>
                                <div class="col-sm-8">
                                    <input type="text" class="form-control" id="perc-category-name-field" maxlength="255" title='<i18n:message key="perc.ui.perc.categories@Category Name"/>'/>
                                </div>
                            </div>
                            
                            <div class="form-group" id="perc-category-selectable-label">
                                <div class="col-sm-offset-4 col-sm-8">
                                    <div class="checkbox">
                                        <label>
                                            <input type="checkbox" id="perc-category-selectable-field" title='<i18n:message key="perc.ui.perc.categories@Is It Selectable"/>'/>
                                            <i18n:message key="perc.ui.perc.categories@Is It Selectable"/>
                                        </label>
                                    </div>
                                </div>
                            </div>

                            <div class="form-group" id="perc-category-show-in-page-label">
                                <div class="col-sm-offset-4 col-sm-8">
                                    <div class="checkbox">
                                        <label>
                                            <input type="checkbox" id="perc-category-show-in-page-field" title='<i18n:message key="perc.ui.perc.categories@Show in Page Metadata"/>'/>
                                            <i18n:message key="perc.ui.perc.categories@Show in Page Metadata"/>
                                        </label>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="form-group" id="perc-allowedsites-label">
                                <label class="col-sm-4 control-label"><i18n:message key="perc.ui.perc.categories@Allowed Sites"/></label>
                                <div class="col-sm-8">
                                    <select class="form-control" id="perc-allowedsites-field" multiple title='<i18n:message key="perc.ui.perc.categories@Allowed Sites"/>'>
                                    </select>
                                </div>
                            </div>

                            <hr/>

                            <div class="form-group" id="perc-category-created-by-label">
                                <label class="col-sm-4 control-label"><i18n:message key="perc.ui.perc.categories@Created By"/></label>
                                <div class="col-sm-8">
                                    <input type="text" class="form-control perc-category-field-readonly" id="perc-category-createdby-field" aria-disabled="true" disabled title='<i18n:message key="perc.ui.perc.categories@Created By"/>'/>
                                </div>
                            </div>
                            
                            <div class="form-group" id="perc-category-creation-date-label">
                                <label class="col-sm-4 control-label"><i18n:message key="perc.ui.perc.categories@Creation Date"/></label>
                                <div class="col-sm-8">
                                    <input type="text" class="form-control perc-category-field-readonly" id="perc-category-creationdt-field" aria-disabled="true" disabled title='<i18n:message key="perc.ui.perc.categories@Creation Date"/>'/>
                                </div>
                            </div>
                            
                            <div class="form-group" id="perc-category-last-modified-by-label">
                                <label class="col-sm-4 control-label"><i18n:message key="perc.ui.perc.categories@Last Modified By"/></label>
                                <div class="col-sm-8">
                                    <input type="text" class="form-control perc-category-field-readonly" id="perc-category-lstmodifiedby-field" aria-disabled="true" disabled title='<i18n:message key="perc.ui.perc.categories@Last Modified By"/>'/>
                                </div>
                            </div>
                            
                            <div class="form-group" id="perc-category-last-modified-date-label">
                                <label class="col-sm-4 control-label"><i18n:message key="perc.ui.perc.categories@Last Modified Date"/></label>
                                <div class="col-sm-8">
                                    <input type="text" class="form-control perc-category-field-readonly" id="perc-category-lstmodifieddt-field" aria-disabled="true" disabled title='<i18n:message key="perc.ui.perc.categories@Last Modified Date"/>'/>
                                </div>
                            </div>
                            
                        </form>
                    </div>
                    <!-- Save/Cancel moved to panel-heading (top of form) -->
                </div>
            </div>
        </div>
    </div>
</div>
