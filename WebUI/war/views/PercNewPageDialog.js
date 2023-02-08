/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * New page dialog, see API for the available methods and behavior.
 */

(function($){

    $.PercNewPageDialog = function()
    {
        var newPageDialogApi = {
            /**
             * Opens the new page dialog and creates a new page on clicking save button.
             * Validates the input and provides the inline validation errors. Shows an alert dialog if there is an
             * error creating the page on server side.
             * @param finderPath,
             * @param templateId, the string representation of the template guid (EG: 16777215-101-705), if not blank, then uses this
             * template id to create the page. If blank shows the template picker.
             */
            openDialog : _openDialog
        };
        //See API for doc.
        function _openDialog(finderPath, templateId)
        {
            var siteName = "";
            if(finderPath) {
                finderPath = finderPath.split("/");
                if (finderPath[1] === $.perc_paths.SITES_ROOT_NO_SLASH)
                {
                    siteName = finderPath[2];
                }
            }
            var taborder = 30;
            var dialogHtml = "<div>" +
                "<p class='perc-field-error' id='perc-save-error'></p><br/>" +
                "<span style='position: relative; float: right; margin-top: -44px; margin-right: -2px;'><label>* - denotes required field</label></span>" +
                "<form action='' method='GET'> ";

            dialogHtml = dialogHtml + "<div style='float:left;'>" +
                "<fieldset>" +
                "<label for='perc-page-linktext' class='perc-required-field'>" + (!templateId ? I18N.message( "perc.ui.newpagedialog.label@Page link text" ) : I18N.message( "perc.ui.newblogpostdialog.label@Post title" )) + ":</label> <br/> " +
                "<input type='text' required class='required' id='perc-page-linktext' name='page_linktext' maxlength='512' autofocus /> <br/> ";

            if(!templateId)
            {
                dialogHtml = dialogHtml +
                    "<input type='text' required style = 'display:none' id='perc-page-title' class='required' name='page_title' maxlength='512'/> ";
            }
            else
            {
                /*
                 * if the template id is set, we are creating a dialog for the blog post gadget
                 * so, for story 353, we do not show the page title field
                 */
                dialogHtml = dialogHtml +
                    "<label for='perc-page-title' class='perc-required-field' style='display: none;'>" + I18N.message( "perc.ui.newblogpostdialog.label@Hidden Post title" ) + ":</label> <br style='display: none;'/> " +
                    "<input type='hidden' required id='perc-page-title' class='required' name='page_title' maxlength='512'/> <br style='display: none;'/>";
            }

            // render the rest of the dialog
            dialogHtml = dialogHtml +
                "<label for='perc-page-name' class='perc-required-field'>" + (!templateId ? I18N.message( "perc.ui.newpagedialog.label@Page name" ) : I18N.message( "perc.ui.newblogpostdialog.label@Post name" )) + ":</label> <br/> " +
                "<input type='text' required  class='required' id='perc-page-name' name='page_name' maxlength='255'/><br/> " +
                "</fieldset>" +
                "</div><br/>";

            if(!templateId)
            {
                dialogHtml = dialogHtml + "<div style='float:left;'><label for='perc-select-template'>" +I18N.message("perc.ui.new.page.dialog@Select A Template") + "</label><br/>" +
                    "  <input list='perc-page-items-datalist' id='perc-page-item-filter' />" +
                    "  <datalist id='perc-page-items-datalist'></datalist><br/>" +
                    "<a class='prevPage browse left'></a>" +
                    "<div class='perc-scrollable'><input type='hidden' id='perc-select-template' name='template'/>" +
                    "<div class='perc-items'>" +
                    "</div></div>" +
                    "<a class='nextPage browse right' ></a></div>   ";
            }
            else
            {
                dialogHtml = dialogHtml + "<input type='hidden' id='perc-select-template' name='template' value='" + templateId + "'/>";

            }

            dialogHtml = dialogHtml +
                "<div class='ui-layout-south'>" +
                "<div id='perc_buttons' style='z-index: 100;'></div>" +
                "</div>" +
                "</form> </div>";

            // if we are in the new blog post dialog, the width is
            var dialogWidth = !templateId ?  800 : 420;
            var dialog = $(dialogHtml).perc_dialog( {
                title: (!templateId ? I18N.message( "perc.ui.newpagedialog.title@New Page" ) : I18N.message( "perc.ui.newblogpostdialog.title@New Post" )),
                buttons: {},
                percButtons:   {
                    "Save": {
                        click: function()   {
                            $.PercBlockUI();
                            _submit(siteName);
                            $.unblockUI();
                        },
                        id:"perc-page-save"
                    },
                    "Cancel":   {
                        click: function()   {_remove();},
                        id:"perc-page-cancel"
                    }
                },
                id: "perc-new-page-dialog",
                width: dialogWidth,
                resizable: false,
                modal: true
            });
            //add the template selector if the template id is not defined.
            if(!templateId)
            {
                scrollableTemplateSelector();
            }
            /**
             * The call back used when recieved validation or internal errors.
             * It will set the focus on the page name input entry if received
             * an validation error and the error code is "page.alreadyExists".
             *
             * @param request the request object contains the error message in the
             * response.
             */
            function errorHandler( request ) {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(request);
                var code = $.PercServiceUtils.extractFieldErrorCode(request);
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), id:'perc-error-dialog-confirm', content: defaultMsg, okCallBack:function(){
                        if (code === 'page.alreadyExists') {
                            $('#perc-page-name').focus();
                        }
                    }
                });
            }

            function _remove()  {
                dialog.remove();
            }

            function _submit(site)  {
                $.PercSiteService.getSiteProperties(site, function(status, result) {
                    if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                        var fileName = $(dialog.find('#perc-page-name')[0]).val();
                        var fileExt = result.SiteProperties.defaultFileExtention;
                        if (fileExt && fileName.lastIndexOf(".") < 0) {
                            if (fileName.length + fileExt.length < 255) { //consider a dot as one more char
                                fileName += "." + fileExt;
                            } else {
                                fileName = fileName.substring(0, 254 - fileExt.length) + "." + fileExt; //consider a dot as one more char
                            }
                        }
                        $(dialog.find('#perc-page-name')[0]).val(fileName);
                        //below checking for the special characters should not be entered in file name.
                        var regex = /[\\\/~`|<>?":*\[\]{}#;%]/;
                        if (regex.test(fileName)) {
                            $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: '<span style="color:red" > The FileName cannot be empty and must not exceed 255 characters, must be unique within the folder and cannot contain any of the following characters: \\ / | &lt; &gt; ? " : \[ \] { } * # ; % </span>'});
                            return;
                        }

                    } else {
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result});
                    }

                    dialog.find('form').trigger("submit");
                });
            }

            var validation = dialog.find('form').validate({
                errorClass: "perc_field_error",
                validClass: "perc_field_success",
                wrapper: "p",
                validateHiddenFields: false,
                messages: _getValidationMessages(),
                debug: false,
                submitHandler: function(form) {
                    var page_name = $(form).find('[name=page_name]').val( );
                    page_name = page_name.trim();
                    page_name = $.perc_textFilters.WINDOWS_FILE_NAME(page_name);
                    $(form).find('[name=page_name]').val(page_name);
                    $.perc_pathmanager.open_containing_folder( finderPath,
                        function( fspec, path ) {
                            $.perc_pagemanager.create_new_page( path, $(form).serializeArray(), function(page) {
                                dialog.remove();
                                loadPage(path.join("/"), page_name, page.Page.id);}, errorHandler );
                        } );
                }
            });


            /**
             * Builds the scrollable template selector, this needs to be replaced by PercScrollingTemplateBrowser.
             */
            function scrollableTemplateSelector()
            {
                var itemContainer = dialog.find('div.perc-scrollable div.perc-items');
                var datalistContainer = dialog.find('#perc-page-items-datalist');
                datalistContainer.empty();

                var selectLocalStyle = "height: 160px; width: 410px; overflow-x: scroll; overflow-y: hidden;";
                $('#perc-select-template_perc_is').attr("style", selectLocalStyle);

                var queryPath;
                if (finderPath[1] === $.perc_paths.SITES_ROOT_NO_SLASH)
                {
                    queryPath = $.perc_paths.TEMPLATES_BY_SITE + '/' + finderPath[2];
                }
                else
                {
                    queryPath = $.perc_paths.TEMPLATES_USER;
                }

                $.getJSON( queryPath, function( spec ) {
                    //Load template selector
                    $.each( spec.TemplateSummary, function() {

                        itemContainer.append(createTemplateEntry(this));
                        datalistContainer.append(createTemplateListEntry(this));

                        $("div.perc-scrollable").scrollable({
                            items: ".perc-items",
                            size: 4,
                            keyboard: true
                        });

                        $(".perc-items .item .item-id").hide();

                        //Wire the keydown event
                        $(".perc-items .item").on("keydown", function(event){
                            event.stopPropagation();
                            event.stopImmediatePropagation();

                            if(event.code == "Enter" || event.code == "Space"){
                                $(this).dblclick();
                            }

                        });

                        // bind click event to each item to handle selection
                        $(".perc-items .item").on('click', function(event){
                            event.stopPropagation();
                            event.stopImmediatePropagation();

                            var itemId = $(this).find(".item-id").text();
                            $("#perc-select-template").val(itemId);
                            $(".perc-items .item").removeClass("perc-selected-item");
                            $(this).addClass("perc-selected-item");
                        });

                        $(".perc-items .item").on('dblclick', function(event)
                        {
                            event.stopPropagation();
                            event.stopImmediatePropagation();

                            var editorUrl = $(this).find(".item-editor-url").text();
                            var workflowId = $(this).find(".item-workflow-id").text();
                            $("#perc-select-template").val($(this).find(".item-id").text());
                            $("#perc-editor-url").val(editorUrl);
                            $("#perc-workflow-id").val(workflowId);
                            $(".perc-items .item").removeClass("perc-selected-item");
                            $(this).addClass("perc-selected-item");
                            $("#perc-page-save").click();

                        });

                        // select first item by default
                        $firstItem = $(".perc-items .item:first");
                        $("#perc-select-template").val($firstItem.find(".item-id").text());
                        $firstItem.addClass("perc-selected-item");

                    });

                    // after adding all the template entries, truncate the labels if they dont fit
                    $.PercTextOverflow($("div.perc-text-overflow"), 122);
                });

                $("#perc-page-item-filter").on("keydown",function(event){
                    event.stopPropagation();
                    event.stopImmediatePropagation();

                    if(event.key==="Escape"){
                        $(this).val("");
                    }

                });

                $("#perc-page-item-filter").on("change",function(event){
                    let scroll = $("div.perc-scrollable").scrollable();
                    let idx = 0;
                    $('#perc-page-items-datalist').children('option').each(function () {
                        if(this.value === event.target.value){
                            $(".perc-items .item").eq(idx).click();
                            $(this).blur();
                            $(".perc-items .item").eq(idx).focus();
                            return;
                        }
                        idx++;
                    });

                });


                $( "#perc-new-page-dialog" ).keyup(function( event ) {

                    switch(event.code){
                        case "ArrowRight": {
                            $("a.nextPage.browse.right").click();
                            break;
                        }
                        case "ArrowLeft":
                            $("a.prevPage.browse.left").click();
                            break;
                    }
                });

                /**
                 * Generates the html for a new datalist entry for the current asset type
                 * @param {*} template
                 */
                function createTemplateListEntry(template){
                    return "<option value='" + template.name  + "' />";
                }

                /**
                 * Creates and returns an entry for the template selection field.
                 */
                function createTemplateEntry(data)
                {
                    var temp = "<button type='button' class='item'>" +
                        "<div class=\"item-id\">@ITEM_ID@</div>" +
                        "    <table>" +
                        "        <tr><td align='left'>" +
                        "            <img style='border:1px solid #E6E6E9' height = '86px' width = '122px' src=\"@IMG_SRC@\"/>" +
                        "        </td></tr>" +
                        "        <tr><td>" +
                        "            <div class='perc-text-overflow-container' style='text-overflow:ellipsis;width:122px;overflow:hidden;white-space:nowrap'>" +
                        "                <div class='perc-text-overflow' style='float:none' title='@ITEM_TT@' alt='@ITEM_TT@'>@ITEM_LABEL@</div>" +
                        "        </td></tr>" +
                        "    </table>" +
                        "</button>";
                    return temp.replace(/@IMG_SRC@/, data.imageThumbPath)
                        .replace(/@ITEM_ID@/, data.id)
                        .replace(/@ITEM_LABEL@/, data.name)
                        .replace(/@ITEM_TT@/g, data.name);
                }

            }

            //Text auto fill and filter settings for form fields
            {
                var linkTextField = $('#perc-page-linktext');
                var titleField = $('#perc-page-title');
                var pageNameField = $('#perc-page-name');
                $.perc_textAutoFill(linkTextField, titleField);
                $.perc_textAutoFill(linkTextField, pageNameField, $.perc_autoFillTextFilters.URL, null, 255);
                $.perc_filterField(pageNameField, $.perc_textFilters.URL);
            }

            /**
             * Builds and returns an object that has the validation messages for each field.
             */
            function _getValidationMessages()
            {
                var messages = {
                    "page_linktext": {
                        required: (!templateId ? "Page link text" : "Post Title") + "  is a required field."
                    },
                    "page_title": {
                        required: (!templateId ? "Page" : "Hidden Post") + " title is a required field."
                    },
                    "page_name": {
                        required: (!templateId ? "Page" : "Post") + " name is a required field."
                    }
                };
                return messages;
            }

            /**
             * Helper method to load the page with the given parameters. This is a browser reload.
             * @param folderPath assumed not null.
             * @param pageName assumed not null.
             * @param pageId assumed not null.
             *
             */
            function loadPage(folderPath, pageName, pageId)
            {
                $.PercNavigationManager.goToLocation(
                    $.PercNavigationManager.VIEW_EDITOR,
                    $.PercNavigationManager.parseSiteFromPath(folderPath),
                    $.PercNavigationManager.MODE_EDIT,
                    pageId,pageName,folderPath + "/" + pageName, $.PercNavigationManager.PATH_TYPE_PAGE);
            }


        }// End open dialog
        return newPageDialogApi;
    };

})(jQuery);
