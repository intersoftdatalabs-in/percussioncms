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
 * PercCategoryView.js
 * 
 */
(function($) {

    var dirtyController = $.PercDirtyController;
    var sitesList = [];
        
    $.PercCategoryView = function() {
        
        var viewApi = {
            init                            : init,
            getCategories                   : getCategories,
            showSelectedCategoryEditor      : showSelectedCategoryEditor,
            alertDialog                     : alertDialog,
            errorDialog                     : errorDialog,
            confirmDialog                   : confirmDialog,
            editCategories                  : editCategories,
            displayCategoryDetails          : displayCategoryDetails,
            deleteCategory                  : deleteCategory,
            handleDelete                    : handleDelete,
            getCurrentDate                  : getCurrentDate,
            visitTreeForBaseProperties      : visitTreeForBaseProperties,
            manageDynaProps                 : manageDynaProps,
            getUpdatedCategoryArray         : getUpdatedCategoryArray,
            updateCategoryXML               : updateCategoryXML,
            save                            : save,
            findUpTargetNode                : findUpTargetNode,
            moveNodeUp                      : moveNodeUp,
            findDownTargetNode              : findDownTargetNode,
            moveNodeDown                    : moveNodeDown,
            publishToDTS                    : publishToDTS
            
        };

        // A snippet to adjust the frame size on resizing the window.
        $(window).on("resize",function() {
            fixIframeHeight();
            fixTemplateHeight();
        });

        var container = $("#perc-category-tree");
        var controller = $.PercCategoryController;
        var sitename="";
        var categories;
        var editing = false;
        var isMoved = false;
        var isDelete = false;
        var isPublished = false;
        var siteSelection;
        var originalTitle = null;
        var generateUid = function () {
            var delim = "-";

            function S4() {
                return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
            }

            return (S4() + S4() + delim + S4() + delim + S4() + delim + S4() + delim + S4() + S4() + S4());
        };

        function getTree() {
            return $.ui.fancytree.getTree(container);
        }
        
        controller.init(viewApi);
        
        function addSitesToDropdown(selectionid, siteArray, selectedArray, allSitesOption) {
             var optionsAsString = "";  
             if (allSitesOption)
             {
                optionsAsString += '<option value="">All Sites</option>';
             }
             for(i = 0; i < siteArray.length; i++)
             {
                 selectedString="";
                 if (selectedArray != null && $.inArray(siteArray[i], selectedArray) > -1)
                 {
                     selectedString=" selected='selected'";
                 }
                 optionsAsString += '<option value="' + siteArray[i] + '"' +selectedString+ '>' + siteArray[i] + '</option>';
             }
             $( selectionid ).html( optionsAsString );
        }
        
        function stringToList(string)
        {
            if (string != null )
                return string.split(',');
            else
                return [];
        }
        

        function getSelectedSites(node) {
            if (node == null || typeof node == "undefined")
            {
                return sitesList;
            }
            if ( node.data.allowedSites == null || typeof node.data.allowedSites == "undefined")
            {
                return getAllowedSites(node);

            }
                return node.data.allowedSites.split(",");
        }
        
        function getAllowedSites(node) {
            if (node == null || typeof node == "undefined")
            {
                return sitesList;
            }
            return getSelectedSites(node.getParent());
        }
        
        function init() {
            
             $.PercSiteService.getSites(function(status, result){
                 var optionsAsString = "";      
                 for(i = 0; i < result.SiteSummary.length; i++)
                 {
                     sitesList.push(result.SiteSummary[i].name);
                 }
                 addSitesToDropdown("#perc-category-site-dropdown",sitesList,null, true);
                 sitename = $('#perc-category-site-dropdown').find(":selected").val();
                 controller.getCategories(sitename);
             });

            $( "#perc-category-site-dropdown" ).on("click",function() {

                siteSelection = $('#perc-category-site-dropdown').find(":selected").val();
            });

            $( "#perc-category-site-dropdown" ).on("change",function() {

                if (editing)
                {
                        currentlyEditing();
                    if(typeof siteSelection !== 'undefined'){
                        $('#perc-category-site-dropdown').val(siteSelection).trigger('chosen:updated');
                    }
                        return;
                }
                sitename = $('#perc-category-site-dropdown').find(":selected").val();

                container.fancytree("destroy");
                controller.getCategories(this.value);
            });
            
			$("#perc-categories-add-category-button").off("keydown").on("keydown",function(event) {
				if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-add-category-button").off("click").on("click", function(){
                
                if (!$.PercNavigationManager.isAdmin()) {
                    alertDialog(I18N.message("perc.ui.category.view@User Admin"), I18N.message("perc.ui.category.view@User Admin Delete"));
                    return;
                }
                
                if (editing)
                {
                        currentlyEditing();
                        return;
                }


                var node = newNode(false);
                displayCategoryDetails(node);
                showSelectedCategoryEditor(node);

            });

			$("#perc-categories-add-child-category-button").off("keydown").on("keydown",function(event) {
				if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-add-child-category-button").off("click").on("click", function(){
                
                if (!$.PercNavigationManager.isAdmin()) {
                    alertDialog(I18N.message("perc.ui.category.view@User Admin"), I18N.message("perc.ui.category.view@User Admin Delete"));
                    return;
                }
                
                if (editing)
                {
                        currentlyEditing();
                        return;
                }

                
                var node = newNode(true);
                displayCategoryDetails(node);
                showSelectedCategoryEditor(node);

            });
            $("#perc-categories-delete-category-button").off("keydown").on("keydown",function(event) {
				if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-delete-category-button").off("click").on("click", function(){
                
                if (!$.PercNavigationManager.isAdmin()) {
                    alertDialog(I18N.message("perc.ui.category.view@User Admin"), I18N.message("perc.ui.category.view@User Admin Delete"));
                    return;
                }
                if (editing)
                {
                        currentlyEditing();
                        return;
                }

                var tree = getTree();
                if(tree.count() === 1)
                    alertDialog(I18N.message("perc.ui.category.view@Delete Category"), I18N.message("perc.ui.category.view@Cannot Delete Node"));
                else {
                    isDelete = true;
                    
                    deleteCategory();
                    editing = false;
                }

            });
            
			$("#perc-categories-edit-category-button").off("keydown").on("keydown",function(event) {
				if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-edit-category-button").off("click").on("click", function(){
                
                if (!$.PercNavigationManager.isAdmin()) {
                    alertDialog(I18N.message("perc.ui.category.view@User Admin"), I18N.message("perc.ui.category.view@User Admin Edit"));
                    return;
                }
                
                if (editing)
                {
                        currentlyEditing();
                        return;
                }

                var node = getTree().getActiveNode();
                displayCategoryDetails(node);
                showSelectedCategoryEditor(node);
                    
            });
            
			$("#perc-categories-moveup-button").off("keydown").on("keydown",function(event) {
				if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-moveup-button").off("click").on("click", function(){
                
                if (editing)
                {
                        currentlyEditing();
                        return;
                }


                var node = getTree().getActiveNode();

                var targetNode = findUpTargetNode(node);
                if(targetNode != null)
                    moveNodeUp(node, targetNode);
                
                displayCategoryDetails(getTree().getActiveNode());
            });
            
			$("#perc-categories-movedown-button").off("keydown").on("keydown",function(event) {
				if(event.code == "Enter" || event.code == "Space"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-movedown-button").off("click").on("click", function(){
                
                if (editing)
                {
                        currentlyEditing();
                        return;
                }


                var node = getTree().getActiveNode();
                var targetNode = findDownTargetNode(node);
                
                if(targetNode != null)
                    moveNodeDown(node, targetNode);
                
                displayCategoryDetails(getTree().getActiveNode());
            });
            
            //Bind Save event
            $("#perc-category-save").off("click").on("click", function(){
                var node = getTree().getActiveNode();
                if (node != null && node.title === "New Category")
                {
                    alertDialog("Error", "You must change the category name.");
                    return;
                }
                save();
            });
            //Bind Cancel event
            $("#perc-category-cancel").off("click").on("click", function(){
                var node = getTree().getActiveNode();
                editing = false;
                if (!node.data.saved)
                {
                    parent = node.parent;
                    parent.activate();
                    if (node.parent.children && node.parent.children.length !== 0)
                        node.remove();
                    
                    node = parent;
                }
                else
                {
                    node.title = originalTitle;
                    node.renderTitle();
                
                }
                displayCategoryDetails(node);
                try {
                    if (node.children && node.children.length > 0) {
                        node.children[0].activate();
                    }
                }catch(err) {}
            });
            
			 $("#perc-categories-publish-staging").off("click").on("click", function(event){
                if(event.code == "Enter"){
						document.activeElement.click();
				}
            });
            $("#perc-categories-publish-staging").off("click").on("click", function(){
                if (editing)
                {
                        currentlyEditing();
                        return;
                }

                var node = getTree().getActiveNode();
                publishToDTS(node, "Staging");
            });

			 $("#perc-categories-publish-production").off("click").on("click", function(event){
                if(event.code == "Enter"){
						document.activeElement.click();
				}
            });

            $("#perc-categories-publish-production").off("click").on("click", function(){
                if (editing)
                {
                        currentlyEditing();
                        return;
                }

                var node = getTree().getActiveNode();
                publishToDTS(node, "Production");
            });
            
			$("#perc-categories-publish-both").off("click").on("click", function(event){
                if(event.code == "Enter"){
						document.activeElement.click();
				}
            });

            $("#perc-categories-publish-both").off("click").on("click", function(){
                if (editing)
                {
                        currentlyEditing();
                        return;
                }

                var node = getTree().getActiveNode();
                publishToDTS(node, "Both");
            });
            
        }
        
        function getCategories(categoryJson) {

            var treedata = categoryJson;
            
            var categorytree = treedata.topLevelNodes;
            
            if (categorytree == null || typeof categorytree == "undefined" || categorytree.length === 0)
            {
                var uid = generateUid();
                categorytree = [ // Pass an array of nodes.
                {
                                    id : uid,
                                    key : uid,
                                    title : "New Category",
                                    selectable : true,
                                    showInPgMetaData : true,
                                    createdBy : "system",
                                    creationDate : getCurrentDate(),
                                    deleted : false,
                                    activate: true,
                                    saved: false,
                                    initialViewCollapsed : true
                                }
                ];
            }
            // Always destroy any existing tree before re-initializing.
            // This is critical for the post-save reload path (getCategories after edit/add).
            // Without destroy, re-calling .fancytree() on the same element can leave
            // stale tree instances, causing new/updated categories (especially children)
            // to not appear until a full browser refresh.
            var existingTree = $.ui.fancytree.getTree(container);
            if (existingTree) {
                existingTree.destroy();
            }

container.fancytree({
                 selectMode: 3,
                 keyboard: true,
                 autoCollapse: true,
                 source: categorytree,
                 init: function(event, data) {
                     visitTreeForBaseProperties();

                     // Ensure something is active on initial load/rebuild so move buttons get correct initial state
                     if (!data.tree.getActiveNode() && data.tree.rootNode.children && data.tree.rootNode.children.length > 0) {
                         data.tree.rootNode.children[0].setActive();
                     }

                     $("span.fancytree-title").each(function(){
                         this.title=this.innerHTML;
                         this.tabIndex="0";
                         this.setAttribute("role", "button");
                     });

                     updateMoveButtonsState();
                 },
                 beforeActivate: function(event, data) {
                     if (editing)
                     {
                         currentlyEditing();
                         return false;
                     }
                 },
                 activate: function(event, data) {
                     var node = data.node;
                     displayCategoryDetails(node);
                 },
                 extensions: ["dnd5"],
                 dnd5: {
                     preventVoidMoves: true, // Prevent dropping nodes 'before self', etc.
                     dragStart: function(node, data) {
                       return true;
                     },
                     dragEnter: function(node, data) {
                       var sourceNode = data.otherNode;
                       // Prevent dropping a parent below another parent (only sort
                       // nodes under the same parent)
                       if(node.parent !== sourceNode.parent){
                         return false;
                       }
                       // Don't allow dropping *over* a node (would create a child)
                       return ["before", "after"];
                     },
                     drop: function(node, data) {
                       /** This function MUST be defined to enable dropping of items on
                        *  the tree.
                        */
                       var sourceNode = data.otherNode;
                       var hitMode = data.hitMode;
                       sourceNode.moveTo(node, hitMode);
                       isMoved = true;
                       save();
                       // Update button states after drag-and-drop reorder
                       setTimeout(updateMoveButtonsState, 50);
                     }
                   }
               });
            }
        
         function visitTreeForBaseProperties() {

             var tree = getTree();
             var treeRoot = tree.getRootNode();

             // NOTE: node.expanded is a native Fancytree source property. It is
             // preserved by manageDynaProps()'s toDict() call on save (dict.expanded
             // is never deleted) and is round-tripped back from the server, so
             // Fancytree already restores each node's correct expanded/collapsed
             // state when the tree is rebuilt from source here. We must NOT force
             // every node with children back open on each rebuild (previously done
             // via a `hasChildren` check below) - doing so overrides/undoes any
             // manual collapsing the user did before saving, making child nodes
             // pop open again on every save. New child categories are made visible
             // at creation time instead, via newNode()'s visitParents() expand.
             //
             // autoCollapse causes each setExpanded(true) call below to try to
             // collapse sibling nodes. Since this bulk restore can expand several
             // siblings in the same pass, that sibling-collapse can be attempted
             // while a prior (still-settling) expand/collapse animation is in
             // progress, which Fancytree logs as "setExpanded(false) while
             // animating: ignored." Temporarily disable autoCollapse for this
             // programmatic restore so we don't fight our own expand calls.
             var autoCollapse = tree.options.autoCollapse;
             tree.options.autoCollapse = false;

             try {
                 treeRoot.visit(function(node){
                     node.data.saved=true;
                     // Only force-expand nodes explicitly marked as not collapsed.
                     // Do not expand purely because a node has children - that would
                     // undo the user's manual collapse state on every save/reload.
                     if (node.data.initialViewCollapsed === "false" && !node.expanded) {
                         // Fancytree's setExpanded() option to skip the expand/collapse
                         // animation is `noAnimation`, not `animation: false` (which
                         // Fancytree does not recognize and silently ignores, leaving
                         // the node in an animating state). Using the correct option
                         // avoids spurious "while animating: ignored" warnings from
                         // any setExpanded()/makeVisible() calls that follow.
                         node.setExpanded(true, {noAnimation: true});
                     }
                 });
             } finally {
                 tree.options.autoCollapse = autoCollapse;
             }
         }
         
         function updateMoveButtonsState() {
            var $up = $("#perc-categories-moveup-button");
            var $down = $("#perc-categories-movedown-button");

            if (editing) {
                $up.prop("disabled", true);
                $down.prop("disabled", true);
                return;
            }

            var tree = getTree();
            if (!tree) {
                $up.prop("disabled", true);
                $down.prop("disabled", true);
                return;
            }

            var node = tree.getActiveNode();
            if (!node || !node.parent || !node.parent.children) {
                $up.prop("disabled", true);
                $down.prop("disabled", true);
                return;
            }

            var siblings = node.parent.children;
            if (siblings.length <= 1) {
                $up.prop("disabled", true);
                $down.prop("disabled", true);
                return;
            }

            var index = siblings.indexOf(node);
            if (index === -1) {
                $up.prop("disabled", true);
                $down.prop("disabled", true);
                return;
            }

            $up.prop("disabled", index === 0);
            $down.prop("disabled", index === siblings.length - 1);
        }

        function displayCategoryDetails(node) {
            if (node == null)
                return;
            originalTitle = node.title;
            $("#perc-category-save-cancel-block").hide();
            
            $("#perc-category-name-field").prop("disabled", true);
			$("#perc-category-name-field").attr("aria-disabled","true");

            $("#perc-category-name-field").val(node.title);
            
            $("#perc-category-selectable-field").prop("disabled", true);
			$("#perc-category-selectable-field").attr("aria-disabled","true");
            var selectable = node.data.selectable;
            if(selectable === true || selectable === "true") {
                $("#perc-category-selectable-field").prop("checked", true);
            }
            else {
                $("#perc-category-selectable-field").prop("checked", false);
            }
            
            $("#perc-category-show-in-page-field").prop("disabled", true);
			$("#perc-category-show-in-page-field").attr("aria-disabled","true");
            var sinpmd = node.data.showInPgMetaData;
            if(sinpmd === "true" || sinpmd === true) {
                $("#perc-category-show-in-page-field").prop("checked", true);
            }
            else {
                $("#perc-category-show-in-page-field").prop("checked", false);
            }
     
            $("#perc-allowedsites-field").prop("disabled", true);
			$("#perc-allowedsites-field").attr("aria-disabled","true");

            addSitesToDropdown("#perc-allowedsites-field",getAllowedSites(node),getSelectedSites(node));
            

            $("#perc-category-createdby-field").val(node.data.createdBy);
            $("#perc-category-creationdt-field").val(node.data.creationDate);
            $("#perc-category-lstmodifiedby-field").val(node.data.lastModifiedBy);
            $("#perc-category-lstmodifieddt-field").val(node.data.lastModifiedDate);

            updateMoveButtonsState();
        }
        
        function showSelectedCategoryEditor(node) {
            editing = true;
            originalTitle = node.title;

            $("#perc-category-name-field").prop("disabled", false);
			$("#perc-category-name-field").attr("aria-disabled","false");

            $("#perc-category-name-field").on('keyup', function() {
                 var node = getTree().getActiveNode();
                 node.setTitle($( this ).val() === "" ? "[empty]" : $( this ).val());
            });

            $("#perc-allowedsites-field").prop("disabled", false);
			$("#perc-allowedsites-field").attr("aria-disabled","false");

            $("#perc-category-selectable-field").prop("disabled", false);
			$("#perc-category-selectable-field").attr("aria-disabled","false");

            $('#perc-category-selectable-field option[value="'+sitename+'"]').prop('disabled', true);
    
            $("#perc-category-selectable-field option").on('click',function() {
                $('#perc-category-selectable-field option[value="'+sitename+'"]').prop('selected',true);
            });

            $("#perc-category-show-in-page-field").prop("disabled", false);
			$("#perc-category-show-in-page-field").attr("aria-disabled","false");

            $("#perc-category-save-cancel-block").show();

            // Disable move buttons while editing
            $("#perc-categories-moveup-button").prop("disabled", true);
            $("#perc-categories-movedown-button").prop("disabled", true);

            var $nameField = $("#perc-category-name-field");
            $nameField.trigger("focus");

            // UX improvement for new categories: if the field still contains the
            // default placeholder "New Category", select the text so the user can
            // immediately start typing to overwrite it.
            if (!node.data.saved && $nameField.val() === "New Category") {
                $nameField.select();
            }

            // Hitting Enter in the category name field should trigger Save (issue request)
            $nameField.off("keydown.enterSave").on("keydown.enterSave", function(e) {
                if (e.which === 13) { // Enter key
                    e.preventDefault();
                    $("#perc-category-save").trigger("click");
                }
            });
        }
        
       
        
        function getCurrentDate() {
            
            var d = new Date();
            var output = d.toISOString();
            //As output will be '2022-02-22T13:18:21.942Z', so remove 'Z'
            output = output.replace('Z','');
            
            return output;
        }
        
        function currentlyEditing() {
            var parentNode;
            
                w = 400;
                $.perc_utils.alert_dialog({
                    title: I18N.message("perc.ui.category.view@Editing Category"),
                    content: I18N.message("perc.ui.category.view@Editing Category Dialog"),
                    width: w,
                    okCallBack: function()
                    {
                        
                    }
                });
        }
        
         function confirmDialog(title, message, w) {
           
            $.perc_utils.confirm_dialog({
                title: title,
                question: message,
                success: function()
                {
                    if(isDelete) {
                        
                        handleDelete();
                    }
                    
                    controller.getCategories();
                },
                cancel: function () 
                {
                    
                }
            });
        }

        function alertDialog(title, message, w) {
            var parentNode;
            
            if(w == null || w === undefined || w === "" || w < 1)
                w = 400;
            $.perc_utils.alert_dialog({
                title: title,
                content: message,
                width: w,
                okCallBack: function()
                {
                    if(isDelete) {
                        
                        handleDelete();
                    }
                    
                    controller.getCategories();
                }
            });
        }

        function errorDialog(title, message, w, useCallback) {
            var parentNode;
            
            if(w == null || w === undefined || w === "" || w < 1)
                w = 400;
            $.perc_utils.alert_dialog({
                title: title,
                content: message,
                width: w,
                okCallBack: function()
                {
                }
            });
        }

        function handleDelete() {

            isDelete = false;
            var node = getTree().getActiveNode();
            parentNode = node.getParent();
            var upTarget = findUpTargetNode(node);
                

            if(node.hasChildren()) {
                node.visit(function(node){
                    node.data.deleted = true;
                });
            } 
            node.data.deleted = true;
            node.data.lastModifiedBy = $.PercNavigationManager.getUserName();
            node.data.lastModifiedDate = getCurrentDate();

            updateCategoryXML();
            
            node.remove();
            
            var switchtoNode = null;
            if(upTarget != null)
                switchtoNode = upTarget;
            else if (parentNode!=null)
            {
                switchtoNode = parentNode;
            }
            getTree().activateKey(switchtoNode.key);
            displayCategoryDetails(switchtoNode);
            controller.getCategories();
        }
        
        function newNode(child)
        {
            
            var root = getTree().getRootNode();
            
            var destinationNode = null;
            var children =  root.children;
            if ( children == null || typeof children == "undefined" || children.length===0)
            {
                destinationNode = root;
                child=true;
            } else if (children.length===1 && children[0].title === "New Category")
            {
                return children[0];
            } else {
                destinationNode = getTree().getActiveNode();
                if (destinationNode == null || typeof destinationNode == "undefined" || !destinationNode.hasOwnProperty('parent'))
                {
                    destinationNode = root;
                    child=true;
                } 
            }

            if (child===true)
            {
                addTo = destinationNode;
            } 
            else
                addTo = destinationNode.getParent();
                
var uid = generateUid();
             var newChild = addTo.addChildren({
                                 id : uid,
                                 key : uid,   // Explicit key helps fancytree track the node across rebuilds
                                 title : "New Category",
                                 selectable : true,
                                 showInPgMetaData : true,
                                 createdBy : $.PercNavigationManager.getUserName(),
                                 creationDate : getCurrentDate(),
                                 deleted : false,
                                 activate: true,
                                 saved: false,
                                 initialViewCollapsed : true
                             });

                 newChild.visitParents(function (childnode) {
                     // Use `noAnimation` (the option Fancytree actually recognizes),
                     // not `animation: false`. With the wrong key Fancytree still
                     // animates the expand, and the node stays in an "animating"
                     // state, so the setActive()/makeVisible() call right below
                     // (which also tries to expand this node) gets ignored with
                     // "setExpanded(true) while animating: ignored." in the console.
                     childnode.setExpanded(true, {noAnimation: true});
                 }, true); 

                 newChild.setActive(true);

                 return newChild;
     
         }
         
         function editCategories(node) {
            
            var nodeKey = node.key;
            var childNode;
            
            if ($('#perc-allowedsites-field option:not(:checked)').length === 0)
            {
                allowedSites=null;
            } else {
                allowedSites = $("#perc-allowedsites-field option:selected").map(function () {
                    return $(this).text();
                }).get().join(',');
            }
            
        
            var categoryname  = $("#perc-category-name-field").val().trim();
           
            if (originalTitle !== categoryname)
            	node.data.previousCategoryName = originalTitle;
            
            node.data.lastModifiedBy = $.PercNavigationManager.getUserName();
            node.data.lastModifiedDate = getCurrentDate();

            if(isPublished) {
                node.data.publishDate = node.data.lastModifiedDate;
                isPublished = false;
            }
            

            node.setTitle(categoryname);

            var selectable = $("#perc-category-selectable-field").prop("checked");

            if(selectable === true) {
                node.data.selectable = "true";
            } else {
                node.data.selectable = "false";
            }
            var showInPage = $("#perc-category-show-in-page-field").prop("checked");

            if(showInPage === true) {
                node.data.showInPgMetaData = "true";
            } else {
                node.data.showInPgMetaData = "false";
            }

            if(node.data.createdBy == null) {
                node.data.createdBy = $.PercNavigationManager.getUserName();
            }

            if(node.data.creationDate == null) {
                node.data.creationDate = getCurrentDate();
            }

            node.data.allowedSites = allowedSites;
            //  Add site save   
      
            return node;
        }
        
        
        function deleteCategory() {
            
            var node = getTree().getActiveNode();
            
            if(node.hasChildren() === false) {
                confirmDialog(I18N.message("perc.ui.category.view@Delete Category"), I18N.message("perc.ui.category.view@Are You Sure"));
            } else {
                confirmDialog(I18N.message("perc.ui.category.view@Delete Category"), I18N.message("perc.ui.category.view@Category And Children Deleted"));
            }
        }
        
        function manageDynaProps() {
            
            var treeRoot = getTree().getRootNode();
            var children = [];
            treeRoot.visit(function(node){
                var parent = node.getParent();

                if(parent.isRootNode()) {
                    children.push(node.toDict(true, function(dict) {
                        if (dict.data) {
                            $.extend(dict, dict.data);
                            delete dict.data;
                        }
                        delete dict.activate;
                        delete dict.addClass;
                        delete dict.expand;
                        delete dict.focus;
                        delete dict.hideCheckbox;
                        delete dict.icon;
                        delete dict.isFolder;
                        delete dict.isLazy;
                        // Intentionally NOT deleting dict.key so that client-generated UIDs
                        // can potentially be used for stable identification after server roundtrip.
                        delete dict.noLink;
                        delete dict.select;
                        delete dict.tooltip;
                        delete dict.saved;
                        delete dict.unselectable;
                    }));
                }
            });
            
            return children;
        }
        
        function getUpdatedCategoryArray(tempChildList) {
            var children = [];
            
            for(i = 0; i < tempChildList.length; i++) {
                children.push(tempChildList[i].data);
            }
            
            return children;
        }
        
        function updateCategoryXML() {
            var catArray = manageDynaProps();
            controller.editCategories(catArray, sitename,
            function(){
                // Defensive: after save the tree may have been fully rebuilt by the
                // controller's getCategories call. getActiveNode() may be null or a
                // different node reference, so guard it.
                var node = getTree().getActiveNode();
                if (node) {
                    displayCategoryDetails(node);
                    node.data.saved = true;
                }
                editing = false;
            },
            function(){
           
            }
            );
        }
        
        function save() {
        
            var node = getTree().getActiveNode();
            
            
            if(!isMoved) {
                var thisnode = editCategories(node);
                node = thisnode;
            }
            
            
            isMoved = false;
            updateCategoryXML();
        }
        
        function findUpTargetNode(sourceNode) {
            
            var parentNode = sourceNode.getParent();
            var tempNode = null;
            var targetNode = null;
            var i = 0;
            // if the souceNode is a top level parent node, 
            // traverse the tree for only top level parent nodes.
            if(parentNode.isRootNode()) {
                var treeRoot = getTree().getRootNode();
                
                treeRoot.visit(function(node){
                    var parent = node.getParent();
                    
                    if(parent.isRootNode()) {
                        i++;
                        if(sourceNode.data.id !== node.data.id)
                            tempNode = node;
                        else {
                            if(i > 1) {
                                targetNode = tempNode;
                                return false;
                            }
                            return false;
                        }
                    }
                });
                
                return targetNode;
            } else {
                parentNode.visit(function(node) {
                    i++;
                    var p = node.getParent();
                    if(p.data.id === parentNode.data.id) {
                        if(sourceNode.data.id !== node.data.id)
                            tempNode = node;
                        else {
                            if(i > 1) {
                                targetNode = tempNode;
                                return false;
                            }
                            return false;
                        }
                        
                    }
                }); 
                
                return targetNode;
            }
        }
        
        function moveNodeUp(node, targetNode) {
            node.moveTo(targetNode, "before");
            
            isMoved = true;
            save();
        }
        
        function findDownTargetNode(sourceNode) {
            
            var parentNode = sourceNode.getParent();
            var tempNode = null;
            var targetNode = null;
            var i = 0;
            // if the souceNode is a top level parent node, 
            // traverse the tree for only top level parent nodes.
            if(parentNode.isRootNode()) {
                var treeRoot = getTree().getRootNode();
                
                treeRoot.visit(function(node){
                    var parent = node.getParent();
                    
                    if(parent.isRootNode()) {
                        if(sourceNode.data.id !== node.data.id) {
                            if(i > 0) {
                                targetNode = node;
                                return false;
                            }
                        }
                        else {
                            i++;

                        }
                    }
                });
                
                return targetNode;
            } else {
                parentNode.visit(function(node) {
                    var p = node.getParent();
                    if(p.data.id === parentNode.data.id) {
                        if(sourceNode.data.id !== node.data.id) {
                            if(i > 0) {
                                targetNode = node;
                                return false;
                            }
                        }
                        else {
                            i++;

                        }
                    }
                }); 
                
                return targetNode;
            }
        }
        
        function moveNodeDown(node, targetNode) {
            node.moveTo(targetNode, "after");
            
            isMoved = true;
            save();
        }
        
        function publishToDTS(node, deliveryServer) {
            var catArray = manageDynaProps();
            if (sitename == null || typeof sitename == "undefined" || sitename==="")
            {
                alertDialog(I18N.message("perc.ui.category.view@Select A Site"), I18N.message("perc.ui.category.view@Select A Site Content"));
                return;
            }
            controller.publishToDTS(catArray, deliveryServer, sitename);
            
            isPublished = true;
            save();
        }
    };
})(jQuery);
