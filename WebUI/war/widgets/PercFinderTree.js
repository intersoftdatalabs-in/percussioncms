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
 * A widget on top of tree plugin called Fancytree for creating the finder tree.
 * Loads the tree lazily by making AJAX calls to $.perc_paths.PATH_FOLDER.
 * USAGE:
 * var finderTree = $("Selector").PercFinderTree({rootPath:$.PercFinderTreeConstants.ROOT_PATH_ALL,showFoldersOnly:true});
 *
 * Finder tree comes with the following options.
 * rootPath(String) - If not one of the $.PercFinderTreeConstants.ROOT_PATH_XXX, will be treated as $.PercFinderTreeConstants.ALL
 * height(String) - the height of the tree container default is 200px
 * width(String) - width of the tree container default is 300px
 * showFoldersOnly(boolean) - true will display just folders, false will display folders and pages/assets, default is false.
 * onClick(function) - the callback function to be called on clicking a node.
 * classNames(object) - fancytree classes can be overridden with this option.
 *                      for available class name of fancytree. Example: {container:"my-container-class-name",...}
 * onRenderComplete(function) -- Call back function that gets called after rendering is complete.
 *
 * Apart from the onClick call back function, exposes the following methods.
 * getDynaTree(), returns the underlying fancytree object, use it for readonly and styling purposes.<b>
 * getSelectedNodes() returns the array of selected nodes in the form of PathItem objects, See PSPathItem for more details.
 *
 */
(function($){

    var DYNATREE_UL_LI_PADDING = 13;
    var DYNATREE_UL_LI_PADDING_OFFSET = 5;
    /**
     * Finder tree constants object.
     */
    $.PercFinderTreeConstants = {
        //To Display both Sites and Assets
        ROOT_PATH_ALL:"All",
        //To display just Sites node
        ROOT_PATH_SITES: "Sites",
        //To display just Assets node
        ROOT_PATH_ASSETS: "Assets",
        //To display just Folder nodes
        FOLDERS_ONLY: "Folders",
        //To display just Folder nodes
        SECTIONS_ONLY: "Sections",
        //Max results for a paginated search
        MAX_RESULTS : 200,
        //Folder ID, passed from Click
        FOLDER_ID : null,
        convertFolderPathToPath: function(fPath){
            var path = null;
            if(!fPath)
                return path;
            if(fPath.indexOf("//Sites")!==-1)
                path = fPath.replace("//Sites",$.perc_paths.SITES_ROOT);
            else if(fPath.indexOf("//Folders/$System$/Assets")!==-1)
                path = fPath.replace("//Folders/$System$/Assets",$.perc_paths.ASSETS_ROOT);
            return path;
        }
    };
    
    /**
     * Finder tree widget. See description at the top of the class for the usage.
     */ 
    $.widget("ui.PercFinderTree", 
    {
        settings : {
            rootPath:$.PercFinderTreeConstants.ROOT_PATH_ALL,
            showFoldersOnly:false,
            filter:null,
            height:"200px",
            width:"290px",
            onClick: function(pathItem){},
            onRenderComplete: function(initialPath, initialNode){},
            classNames:{},
            initialPath:null,
            getInitialPathItem:function(initialPathItem){},
            onQueryActivate: function(){return true;},
            onQuerySelect: function(){return true;},
            clickFolderMode: 3,
            acceptableTypes:"Folder",
            acceptableCategories:""
        },
        _init: function()
        {
            
            var self = this;
            $.extend(this.settings, this.options);
            var rp = this.settings.rootPath;
            if(!(rp === $.PercFinderTreeConstants.ROOT_PATH_ALL || rp === $.PercFinderTreeConstants.ROOT_PATH_SITES ||
                rp === $.PercFinderTreeConstants.ROOT_PATH_ASSETS))
            {
                this.settings.rootPath = $.PercFinderTreeConstants.ROOT_PATH_ALL;
            }
            $(this.element).css("width",this.settings.width);
            $(this.element).css("height",this.settings.height);
            $(this.element).css("overflow","auto");
            $(this.element).append($("<div id='perc-finder-tree'></div>"));
            this.intialPathExpanded = true;
            this.initialRenderCompleted = false;
            this.intialPathKey = null;
            if(this.settings.initialPath && this.settings.initialPath.trim().length > 0 && this.settings.initialPath.trim().charAt(0) === '/')
            {
                this.intialPathExpanded = false;
                this.intialPathKey = this._generateKey(this.settings.initialPath);
            }
            $.PercServiceUtils.makeJsonRequest(
                $.perc_paths.PATH_FOLDER + '/',
                $.PercServiceUtils.TYPE_GET,
                false, 
                function(status, result)
                {
                    if(status === $.PercServiceUtils.STATUS_ERROR)
                    {
                        var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                        $.perc_utils.alert_dialog({title: 'Error', content: defaultMsg});
                    }
                    else
                    {
                        self._initTree(result.data);
                    }
                }, 
                null);
        },
        
        /**
         * Initalizes the tree with the root nodes.
         */
        _initTree: function(data)
        {
            var self = this;
            var rootChildren = [];
            var inPath = self._normalizedPath(self.settings.initialPath);
            $.each(self._getPathItems(data), function(){
                var include = self.settings.rootPath === $.PercFinderTreeConstants.ROOT_PATH_ALL || self.settings.rootPath === this.name;
                if(this.name === "Design" || this.name === "Search")
                    include = false;
                if(include )
                {
                    var dtobj = self._makeDtNode(this);
                    $.extend(dtobj,{unselectable:true});
                    var currentPath = self._normalizedPath(this.path);
                    if(inPath === currentPath)
                    {
                        $.extend(dtobj,{active:true});
                        self.intialPathExpanded = true;
                    }
                    // Auto-expand root nodes when initialPath is a child of that root
                    // This handles cases like initialPath="/Assets/uploads" with rootPath="Assets"
                    if(!self.intialPathExpanded && inPath.indexOf(currentPath) === 0 && currentPath !== "/")
                    {
                        $.extend(dtobj,{active:true});
                        // Set a flag to indicate we need to expand this root node after tree init
                        self.rootToExpand = dtobj;
                    }
                    rootChildren.push(dtobj);
                }
            });
            var fancyTree = $(this.element).find("#perc-finder-tree").fancytree({
                selectMode: 1,
                autoFocus: false,
                source: rootChildren,
                clickFolderMode: self.settings.clickFolderMode,
                beforeSelect: function(event, data) {
                    return self.settings.onQuerySelect(data.node);
                },
                beforeActivate: function(event, data) {
                    return self.settings.onQueryActivate(true, data.node);
                },
                classes: self.settings.classNames,
                dblclick: function(event, data) {
                    var node = data.node;
                    if(node.isFolder() && !node.isExpanded()){
                        node.setExpanded(true);
                    }
                    return true;
                },
                init: function(event, data) {
                    if(!self.initialRenderCompleted){
                        self.settings.onRenderComplete(null, null);
                        self.initialRenderCompleted = true;
                    }
                    if(self.rootToExpand && !self.intialPathExpanded){
                        var rootNode = data.tree.getNodeByKey(self.rootToExpand.key);
                        if(rootNode){
                            rootNode.setExpanded(true);
                        }
                    }
                },
                renderNode: function(event, data) {
                    var dtnode = data.node;
                    var span;
                    var level = dtnode.getLevel();
                    if(dtnode.data.nodeType !== "SHOW_MORE"){
                        span = $(dtnode.span);
                        span.css("padding-left", (level * 18) + "px");
                    }
                },
                lazyLoad: function(event, data){
                    var node = data.node;
                    var dtdata = node.data;
                    var url;
                    if(self.settings.showFoldersOnly){
                        url = $.perc_paths.PATH_FOLDER + dtdata.pathItem.path;
                    }
                    else{
                        url = $.perc_paths.PATH_PAGINATED_FOLDER + dtdata.pathItem.path + "?startIndex=1&maxResults=" + $.PercFinderTreeConstants.MAX_RESULTS;
                        if(self.settings.acceptableTypes){
                            url += "&type=" + self.settings.acceptableTypes;
                        }
                        if(self.settings.acceptableCategories){
                            url += "&category=" + self.settings.acceptableCategories;
                        }
                    }
                    var deferred = $.Deferred();
                    data.result = deferred.promise();
                    
                    $.ajax({
                        url: url,
                        type: "GET",
                        dataType: "json",
                        headers: {"perc-version": "1.0"}
                    }).done(function(response){
                        var children = self._buildChildren(node, response);
                        deferred.resolve(children);
                    }).fail(function(xhr, status, error){
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: error});
                        node.setStatus("error");
                        deferred.resolve([]);
                    });
                },
                click: function(event, data){
                    var dtnode = data.node;
                    if(dtnode.data.nodeType === "SHOW_MORE" && !dtnode.data.isShowing){
                        dtnode.data.isShowing = true;
                        dtnode.data.icon = "../images/images/loading.gif";
                        dtnode.render();
                        self._loadMoreChildren(dtnode);
                        return;
                    }
                    self._onClick(dtnode);
                    self.getFolderID(dtnode);
                },
                expand: function(event, data){
                    var dtnode = data.node;
                    if(dtnode.data.nodeType === "SHOW_MORE")
                        return;
                    self._onExpand(data.expand, dtnode);
                }
            });
        },
        
        /**
         * Call back function for dynatree lazy read. dynatree calls this function by passing the selected dtnode object.
         * Makes an ajax call to the server with the path from the supplied dtnode.data and creates the dynatree nodes
         * and appends them as children of the current node.
         */
        _loadChildren: function(dtnode)
        {
            var self = this, url, dtdata = dtnode.data,origDtNode;
            if(self.settings.showFoldersOnly){
                url = $.perc_paths.PATH_FOLDER + dtdata.pathItem.path;
            }
            else{
                url = $.perc_paths.PATH_PAGINATED_FOLDER + dtdata.pathItem.path + "?startIndex=1&maxResults=" + $.PercFinderTreeConstants.MAX_RESULTS;
                if(self.settings.acceptableTypes){
                    url += "&type=" + self.settings.acceptableTypes;
                }
                if(self.settings.acceptableCategories){
                    url += "&category=" + self.settings.acceptableCategories;
                }
            }
            $.PercServiceUtils.makeJsonRequest(
                url,
                $.PercServiceUtils.TYPE_GET,
                false,
                function(status, result){
                    if(status === $.PercServiceUtils.STATUS_ERROR)
                    {
                        var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: defaultMsg});
                        dtnode.setStatus("error");
                    }
                    else
                    {
                        self._addChildren(dtnode, result.data);
                    }
                },
                null);
        },

        /**
         * Helper function that builds an array of child node data objects
         * @param {Object} dtnode assumed to be a fancytree node object.
         * @param {Object} data assumed to be a PathItem or PagedItemList object
         * @returns {Array} array of child node data objects
         */
        _buildChildren:function(dtnode, data){
            var self = this;
            var result = [];
            var temp = self.settings.showFoldersOnly ? self._getPathItems(data) : self._getPagedChildren(data);
            if(!temp){
                return result;
            }
            if(!Array.isArray(temp)){
                temp = [temp];
            }
            $.each(temp, function(){
                var exclude = self.settings.showFoldersOnly && this.leaf;
                if(typeof this.category !== "undefined" && self.settings.filter !== null ) {
                    exclude = exclude || (self.settings.filter === $.PercFinderTreeConstants.FOLDERS_ONLY && this.category !== "FOLDER" && this.category !== "SYSTEM");
                    exclude = exclude || (self.settings.filter === $.PercFinderTreeConstants.SECTIONS_ONLY && this.category !== "SECTION_FOLDER");
                }
                if(!exclude){
                    var dtobj = self._makeDtNode(this);
                    result.push(dtobj);
                }
            });
            return result;
        },

        /**
         * Helper function that loops through the data and adds the children
         * @param {Object} dtnode assumed to be a dynatree node object.
         * @param {Object} data assumed to be a PathItem or PagedItemList object
         */
        _addChildren:function(dtnode, data){
            var expNode = null,self=this;
            var temp = self.settings.showFoldersOnly ? self._getPathItems(data) : self._getPagedChildren(data);
            if(!temp){
                dtnode.setStatus("ok");
                return;
            }
            if(!Array.isArray(temp)){
                temp = [temp];
            }
            
            $.each(temp, function(){
                var exclude = self.settings.showFoldersOnly && this.leaf;

                if(typeof this.category != "undefined" && self.settings.filter != null ) {
                    exclude = exclude || (self.settings.filter === $.PercFinderTreeConstants.FOLDERS_ONLY  && this.category !== "FOLDER" && this.category !== "SYSTEM");
                    exclude = exclude || (self.settings.filter === $.PercFinderTreeConstants.SECTIONS_ONLY && this.category !== "SECTION_FOLDER");
                }
                if(!exclude){
                    dtobj = self._makeDtNode(this);
                    var chNode = dtnode.addChildren(dtobj);
                    if(!self.intialPathExpanded){
                        var currentPath = self._normalizedPath(this.path);
                        var inPath = self._normalizedPath(self.settings.initialPath);
                        if(inPath === currentPath){
                            // Use setActive instead of activateSilently (Fancytree API)
                            if(chNode.setActive){
                                chNode.setActive(true, {noEvents: true});
                            }
                            self.settings.getInitialPathItem(chNode.data.pathItem);                                                
                            self.intialPathExpanded = true;
                        }
                        else if(inPath.indexOf(currentPath) !== -1){
                            expNode = chNode;
                        }
                    }
                }
            });
            
            var pagedList = self._getPagedList(data);
            if(!self.settings.showFoldersOnly && pagedList && pagedList.startIndex + $.PercFinderTreeConstants.MAX_RESULTS - 1 < pagedList.childrenCount){
                var dtobj = self._makeMoreResultsDtNode(pagedList.startIndex + $.PercFinderTreeConstants.MAX_RESULTS, dtnode);
                dtnode.addChildren(dtobj);
            }
            self._adjustScrollWidths();
            dtnode.setStatus("ok");
            if(!self.initialRenderCompleted){
                self.settings.onRenderComplete(dtnode.data.pathItem, dtnode);
                self.initialRenderCompleted = true;
            }
        },

        /**
         * Returns path items for payloads that may use PathItemList or PathItem.
         */
        _getPathItems:function(data){
            if(!data){
                return [];
            }

            var items = data.PathItemList || data.PathItem || [];
            if(!Array.isArray(items)){
                items = [items];
            }
            return items;
        },

        _getPagedList:function(data){
            if(!data){
                return null;
            }
            return data.PagedItemList || data.pagedItemList || null;
        },

        _getPagedChildren:function(data){
            var paged = this._getPagedList(data);
            if(!paged || !paged.childrenInPage){
                return [];
            }
            return Array.isArray(paged.childrenInPage) ? paged.childrenInPage : [paged.childrenInPage];
        },
        _loadMoreChildren:function(dtnode){
            var self = this, url, dtdata = dtnode.data;
            url = $.perc_paths.PATH_PAGINATED_FOLDER + dtdata.parentNode.data.pathItem.path + "?startIndex=" + dtdata.startIndex + "&maxResults=" + $.PercFinderTreeConstants.MAX_RESULTS;
            $.PercServiceUtils.makeJsonRequest(
                url, 
                $.PercServiceUtils.TYPE_GET,
                false, 
                function(status, result){
                    if(status === $.PercServiceUtils.STATUS_ERROR)
                    {
                        var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: defaultMsg});
                        dtnode.setStatus("error");
                    }
                    else
                    {
                        self._addChildren(dtdata.parentNode, result.data);
                        dtnode.remove();
                    }   
                }, 
                null);
        },
        /**
         * Calls the onExpand call back function by passing the PathItem objects of the selected node on the tree.
         */
        _onExpand: function(flag, dtnode)
        {
            this._adjustScrollWidths();
        },
        
        /**
         *Assigns the scroll-width of the perc-folder-selector and assigns it to  perc-finder-tree.
         */
        _adjustScrollWidths: function(dtnode)
        {
        },
        
        /**
         * Calls the onClick call back function by passing the PathItem objects of the selected node on the tree.
         */
        _onClick: function(dtnode)
        {
            this.settings.onClick( dtnode.data.pathItem );
        },
        
        /**
         * Returns the array of the selected nodes PathItem objects.
         */
        getSelectedItems: function()
        {
            var pathItems = [];
            var selNodes = $("#perc-finder-tree").fancytree("getSelectedNodes");
            $.each(selNodes, function(){
                pathItems.push(this.data.pathItem);
            });
            return pathItems;
        },
        /**
         * Returns the Folder ID of the selected Folder.
         */ 
        getFolderID: function(dtnode)
        {
            var pathID = dtnode.data.pathItem.id;
            var folderID;
            if (typeof pathID != 'undefined' )
            {
                var splitID = pathID.split("-");
                folderID = splitID[2];
                $.PercFinderTreeConstants.FOLDER_ID = folderID;
            }
            return folderID;
        },
        
        /**
         * Returns the dynatree object that this widget creates. This object is usefull for getting the selected node
         * info or root node info etc... See the documentation of dynatree for the available methods. 
         * Note: As the nodes are handled by this class, use readonly methods of the dynatree object.
         * If the modifications are needed then add new methods to this plugin.
         */
        getDynaTree: function()
        {
            return $("#perc-finder-tree").fancytree("getTree");
        },
        
        /**
         * Helper function to create a dynatree node from the pathItem.
         * @param pathItem Expects the pathItem to be in the format of PSPathItem.
         */
        _makeDtNode: function(pathItem)
        {
            var self = this;
            var item_path = $.perc_utils.extract_path( pathItem.path );
            var icon = $.perc_utils.choose_icon( pathItem.type, pathItem.icon, item_path );
            var key = this._generateKey(pathItem.path);
            // Convert leaf to boolean - API might return "false" string instead of boolean false
            var isLeaf = pathItem.leaf === true || pathItem.leaf === "true";
            var folder = !isLeaf;
            // For fancytree: make all non-leaf folders lazy so they can be expanded on-demand
            // The lazyLoad event handler will fetch children when user clicks expand
            var lazy = folder; // All folders are expandable/lazy-loadable
            
            // Create node using Fancytree property names (folder, lazy)
            var dtn = {title: pathItem.name, folder: folder, lazy: lazy, tooltip: pathItem.name, pathItem: pathItem, key: key};
            // For lazy nodes, explicitly set children to null so Fancytree knows they haven't been loaded yet
            if (lazy) {
                dtn.children = null;
            }
            // Add icon separately if needed (Fancytree may render icons differently)
            dtn.icon = icon.src;
            if(pathItem.leaf){
                dtn.extraClasses = "perc-hide-node-expander";
            }
            return dtn;
        },
        /**
         * Helper function to create a dynatree node from the show more link.
         * @param {String} startIndex The start index for the next set of results
         * @param {String} parentNode assumed to dynatree node object of parent. 
         */
        _makeMoreResultsDtNode: function(startIndex, parentNode){
            var label = I18N.message("perc.ui.finder.tree@Show More");
            return {title: label, lazy: true, tooltip: label, extraClasses: "perc-hide-node-expander", startIndex: startIndex, nodeType: "SHOW_MORE", isShowing: false, parentNode: parentNode};
        },
        
        /**
         * Helper function to generatea unique key from the path
         * @param {Object} path assumed not blank
         */
        _generateKey: function(path)
        {
             if(!path)
                return null;
             //Create a unique id using the full path for the path item.
             if( path && path.length > 1 && path.substring(path.length-1) === '/')
             {
                 path = path.slice(0, path.length-1);
             }
             return path.replace(/[^a-zA-Z0-9/]/g, '_').replace(/\//g,'-');
        },
        /**
         * Helper function to normalize the path
         * @param {Object} path assumed not blank
         */
        _normalizedPath: function(path)
        {
            if(!path || path.length < 1)
                return path;
            if(path.substring(path.length-1,path.length)!="/")
                return path + "/";
            return path;
        }
    });    
})(jQuery);
