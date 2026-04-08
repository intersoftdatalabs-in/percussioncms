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
 * Section tree Dialog, displays the sections as a tree.
 */
(function($){
    //Public API for the section tree dialog.
    $.PercSectionTreeDialog = {
        open: openDialog
    };
    /**
     * Opens the section tree dialog and shows all the sections in expanded state.
     * @param siteName(String), assumed to be a valid name of a site.
     * @param excludeId(String) the string format of the guid of the section that needs to be excluded from the tree,
     * this section and all subsections below it are not rendered.
     * @param treeLabel (String), the label for the tree section.
     * @param dlgTitle (String), the dialog title.
     * @param okButton, A string representing what button needs to be rendered for positive action. Currently supported
     * Strings are Move and Select.
     * @param okCallback (function), function to call when user clicks on OK button. Called like okCallback(sectionId,sectionPath),
     * where sectionId(String) is the string format of the guid of the selected section and sectionPath(String) is
     * the path of the section from the root.
     *
     */
    function openDialog(siteName, excludeId, treeLabel, dlgTitle, okButton, okCallback)
    {

        var self = this;
        var $dialog = null;
        // Get section tree
        $.Perc_SectionServiceClient.getTree(siteName, function(status, result){
            if(status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                var rootNode = normalizeSectionNode(result);
                $dialog =   $("<div/>")
                    .append(
                        $("<div style='float:left;'/>").html(treeLabel)
                            .append(
                                $("<div id='perc-movesection-tree' />")
                                    .append(
                                        $("<ul/>")
                                            .append(buildSectionTreeList(rootNode))
                                    )
                            )
                    )
                    .append(
                        $("<div class='ui-layout-south'/>")
                            .append(
                                $("<div id='perc_buttons' style='z-index: 100;'>")
                            )
                    )

                    .perc_dialog({
                        title: dlgTitle,
                        resizable: true,
                        modal: true,
                        percButtons:    {
                            "Move": {
                                click: function(){
                                    onOk();
                                },
                                id: "perc-movesection-move"
                            },
                            "Select": {
                                click: function(){
                                    onOk();
                                },
                                id: "perc-select-section-button"
                            },
                            "Cancel":{
                                click: function(){
                                    $dialog.remove();
                                },
                                id: "perc-movesection-cancel"
                            }
                        },
                        open: function(){
                            if(okButton === "Select")
                            {
                                $("#perc-movesection-move").hide();
                                $("#perc-select-section-button").show();
                            }
                            else
                            {
                                $("#perc-movesection-move").show();
                                $("#perc-select-section-button").hide();
                            }
                        },
                        id: "perc-move-section-dialog",
                        width: 600
                    });
                $("#perc-movesection-tree").fancytree({
                    imagePath: "/cm/images/images/"
                });

            }
            else
            {
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result.data});
            }
        });

        /**
         * Helper function to handle OK button(Move/Select) click.
         * Calls the supplied OK callback with selected item id and the path from root.
         */
        function onOk()
        {
            var tree = $.ui.fancytree.getTree("#perc-movesection-tree");
            var sel = tree.getActiveNode();
            var nodePath = "";
            nodePath = getSelectedNodePath(sel, nodePath);
            if(nodePath.startsWith("/root/"))
                nodePath = nodePath.replace("/root/","/");

            if(typeof(sel) != "undefined" && sel != null)
            {
                $dialog.remove();
                var temp = sel.key.split(/\|/);
                okCallback(temp[1],nodePath);
            }

        }

        /**
         * Helper function to create the section tree HTML list for the section target
         * selection dialog.
         * @param sectionNode {Object} the SectionNode object as returned from the
         * server. Cannot be <code>null</code>.
         * @return the HTML list that will represent the tree.
         * @type String
         */
        function buildSectionTreeList(sectionNode){
            sectionNode = normalizeSectionNode(sectionNode);
            if(!sectionNode || typeof sectionNode === "undefined")
                return "";

            var results;
            if(sectionNode.sectionType && sectionNode.sectionType !== $.Perc_SectionServiceClient.PERC_SECTION_TYPE.SECTION)
                return "";
            if(sectionNode.id === excludeId)
                return "";
            var menuTitle = getSectionDisplayName(sectionNode);
            results = $("<li/>")
                .attr("id", "perc_section_tree|" + sectionNode.id)
                .attr("class","folder expanded")
                .attr("data", "icon:'section.png',sectionName:'" + menuTitle.replace(/'/g, "\\'").replace(/"/g, "\\\"") + "'")
                .append(
                    $("<a/>").attr("href", "#").text(menuTitle)
                    .attr("data", "icon:'section.png',sectionName:'" + menuTitle.replace(/'/g, "\\'").replace(/"/g, "\\\"") + "'")
                );

            var childNodes = sectionNode.childNodes;
            if(childNodes && childNodes !== "")
            {
                var children = childNodes.SectionNode || childNodes.sectionNode || childNodes;
                if(!children)
                    return results;

                var ulItem = $("<ul/>");
                var childList = Array.isArray(children) ? children : [children];
                for(var i = 0; i < childList.length; i++)
                {
                    var childNode = buildSectionTreeList(normalizeSectionNode(childList[i]));
                    if(childNode !== "")
                        ulItem.append(childNode);
                }
                if(ulItem.children().length > 0)
                    results.append(ulItem);
            }
            return results;
        }

        /**
         * Some responses wrap nodes as {SectionNode:{...}} at varying levels.
         * Normalize to the actual section node shape.
         */
        function normalizeSectionNode(node)
        {
            if(!node)
                return node;

            if(node.SectionNode)
                return node.SectionNode;

            if(node.sectionNode)
                return node.sectionNode;

            return node;
        }

        /**
         * Resolve the section display name from the payload.
         * We use the section title/name (not folder path) to populate tree labels and returned path.
         */
        function getSectionDisplayName(sectionNode)
        {
            if(!sectionNode)
                return "";

            var displayName = sectionNode.title || sectionNode.name || sectionNode.sectionName;
            if(displayName == null || displayName === "")
                displayName = I18N.message("perc.ui.section.tree.dialog@Untitled Section");

            return displayName + "";
        }

        /**
         * Returns the display path of the selected node from the root.
         * @param selectedNode, dynatree node object assumed not null.
         * @param nodePath(String) this can be an empty string to start with, then the function recursively builds
         * the path by prepending /name to the path.
         * @return the path from the root to the selected node in the form of /Home/Section1/Section2...
         * @type String.
         */
        function getSelectedNodePath(selectedNode, nodePath)
        {
            if(selectedNode == null || typeof selectedNode == 'undefined')
                return nodePath;

            var nodeName = selectedNode.data && selectedNode.data.sectionName
                ? selectedNode.data.sectionName
                : selectedNode.title;

            if(nodeName == null || nodeName === "")
                return nodePath;

            nodePath = "/" + nodeName + nodePath;
            nodePath = getSelectedNodePath(selectedNode.parent, nodePath);
            return nodePath;
        }
    }// End open dialog

})(jQuery);
