/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * classNames(object) - Fancytree classes can be overridden with this option. see https://github.com/mar10/fancytree/wiki/
 *                      for available class name of Fancytree. Example: {container:"my-container-class-name",...}
 * onRenderComplete(function) -- Call back function that gets called after rendering is complete.
 *
 * Apart from the onClick call back function, exposes the following methods.
 * getFancyTree(), returns the underlying Fancytree object, use it for readonly and styling purposes.
 * getSelectedNodes() returns the array of selected nodes in the form of PathItem objects, See PSPathItem for more details.
 *
 */
(function ($) {
  var FANCYTREE_UL_LI_PADDING = 13;
  var FANCYTREE_UL_LI_PADDING_OFFSET = 5;
  /**
   * Finder tree constants object.
   */
  $.PercFinderTreeConstants = {
    //To Display both Sites and Assets
    ROOT_PATH_ALL: "All",
    //To display just Sites node
    ROOT_PATH_SITES: "Sites",
    //To display just Assets node
    ROOT_PATH_ASSETS: "Assets",
    //To display just Folder nodes
    FOLDERS_ONLY: "Folders",
    //To display just Folder nodes
    SECTIONS_ONLY: "Sections",
    //Max results for a paginated search
    MAX_RESULTS: 200,
    //Folder ID, passed from Click
    FOLDER_ID: null,
    convertFolderPathToPath: function (fPath) {
      var path = null;
      if (!fPath) return path;
      if (fPath.indexOf("//Sites") !== -1)
        path = fPath.replace("//Sites", $.perc_paths.SITES_ROOT);
      else if (fPath.indexOf("//Folders/$System$/Assets") !== -1)
        path = fPath.replace(
          "//Folders/$System$/Assets",
          $.perc_paths.ASSETS_ROOT,
        );
      return path;
    },
  };

  /**
   * Finder tree widget. See description at the top of the class for the usage.
   */
  $.widget("ui.PercFinderTree", {
    settings: {
      rootPath: $.PercFinderTreeConstants.ROOT_PATH_ALL,
      showFoldersOnly: false,
      filter: null,
      height: "200px",
      width: "290px",
      onClick: function (pathItem) {},
      onRenderComplete: function (initialPath, initialNode) {},
      classNames: {},
      initialPath: null,
      getInitialPathItem: function (initialPathItem) {},
      onQueryActivate: function () {
        return true;
      },
      onQuerySelect: function () {
        return true;
      },
      clickFolderMode: 3,
      acceptableTypes: "Folder",
      acceptableCategories: "",
    },
    _init: function () {
      var self = this;
      $.extend(this.settings, this.options);
      var rp = this.settings.rootPath;
      if (!(
        rp === $.PercFinderTreeConstants.ROOT_PATH_ALL ||
        rp === $.PercFinderTreeConstants.ROOT_PATH_SITES ||
        rp === $.PercFinderTreeConstants.ROOT_PATH_ASSETS
      )) {
        this.settings.rootPath = $.PercFinderTreeConstants.ROOT_PATH_ALL;
      }
      $(this.element).css("width", this.settings.width);
      $(this.element).css("height", this.settings.height);
      $(this.element).css("overflow", "auto");
      $(this.element).append($("<div id='perc-finder-tree'></div>"));
      this.intialPathExpanded = true;
      this.initialRenderCompleted = false;
      this.intialPathKey = null;
      if (
        this.settings.initialPath &&
        this.settings.initialPath.trim().length > 0
      ) {
        this.intialPathExpanded = false;
        this.intialPathKey = this._generateKey(this.settings.initialPath);
      }
      $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.PATH_FOLDER + "/",
        $.PercServiceUtils.TYPE_GET,
        false,
        function (status, result) {
          if (status === $.PercServiceUtils.STATUS_ERROR) {
            var defaultMsg =
              $.PercServiceUtils.extractDefaultErrorMessage(request);
            $.perc_utils.alert_dialog({ title: "Error", content: defaultMsg });
            dtnode.setLazyNodeStatus(DTNodeStatus_Error);
          } else {
            self._initTree(result.data);
          }
        },
        null,
      );
    },

    /**
     * Initalizes the tree with the root nodes.
     */
    _initTree: function (data) {
      var self = this;
      var rootChildren = [];
      var inPath = self._normalizedPath(self.settings.initialPath);
      $.each(data.PathItem, function () {
        var include =
          self.settings.rootPath === $.PercFinderTreeConstants.ROOT_PATH_ALL ||
          self.settings.rootPath === this.name;
        if (this.name === "Design" || this.name === "Search") include = false;
        if (include) {
          var dtobj = self._makeDtNode(this);
          $.extend(dtobj, { unselectable: true });
          var currentPath = self._normalizedPath(this.path);
          if (inPath === currentPath) {
            $.extend(dtobj, { activate: true });
            self.intialPathExpanded = true;
          }
          rootChildren.push(dtobj);
        }
      });
      var fancyTree = $(this.element)
        .find("#perc-finder-tree")
        .fancytree({
          selectMode: 1,
          autoFocus: false,
          source: rootChildren,
          clickFolderMode: self.settings.clickFolderMode,
          classNames: self.settings.classNames,
          renderNode: function (event, data) {
            var node = data.node;
            var span;
            var uls;
            if (node.data.type !== "SHOW_MORE") {
              span = $(node.span);
              uls = span.parents("ul").length - 1;
              span.css(
                "padding-left",
                uls * FANCYTREE_UL_LI_PADDING + FANCYTREE_UL_LI_PADDING_OFFSET,
              );
            } else {
              span = $(node.span);
              uls = span.parents("ul").length - 3;
              span.css(
                "padding-left",
                uls * FANCYTREE_UL_LI_PADDING + FANCYTREE_UL_LI_PADDING_OFFSET,
              );
            }
          },
          lazyLoad: function (event, data) {
            self._loadChildren(data.node);
          },
          click: function (event, data) {
            if (
              data.node.data.type === "SHOW_MORE" &&
              !data.node.data.isShowing
            ) {
              data.node.data.isShowing = true;
              data.node.data.icon = "/cm/css/fancytree/skin/loading.gif";
              data.node.render();
              self._loadMoreChildren(data.node);
              return;
            }
            self._onClick(data.node);
            self.getFolderID(data.node);
          },
          expand: function (event, data) {
            if (data.node.type === "SHOW_MORE") return;
            self._onExpand(data.flag, data.node);
          },
        });
      fancyTree.fancytree("getRoot").visit(function (node) {
        node.setExpanded(true);
      });
    },

    /**
     * Call back function for fancytree lazy read. fancytree calls this function by passing the selected node object.
     * Makes an ajax call to the server with the path from the supplied node.data and creates the fancytree nodes
     * and appends them as children of the current node.
     */
    _loadChildren: function (node) {
      var self = this,
        url,
        nodeData = node.data;
      if (self.settings.showFoldersOnly) {
        url = $.perc_paths.PATH_FOLDER + nodeData.pathItem.path;
      } else {
        url =
          $.perc_paths.PATH_PAGINATED_FOLDER +
          nodeData.pathItem.path +
          "?startIndex=1&maxResults=" +
          $.PercFinderTreeConstants.MAX_RESULTS;
        if (self.settings.acceptableTypes) {
          url += "&type=" + self.settings.acceptableTypes;
        }
        if (self.settings.acceptableCategories) {
          url += "&category=" + self.settings.acceptableCategories;
        }
      }
      $.PercServiceUtils.makeJsonRequest(
        url,
        $.PercServiceUtils.TYPE_GET,
        false,
        function (status, result) {
          if (status === $.PercServiceUtils.STATUS_ERROR) {
            var defaultMsg =
              $.PercServiceUtils.extractDefaultErrorMessage(request);
            $.perc_utils.alert_dialog({
              title: I18N.message("perc.ui.publish.title@Error"),
              content: defaultMsg,
            });
          } else {
            self._addChildren(node, result.data);
          }
        },
        null,
      );
    },

    /**
     * Helper function that loops through the data and adds the children
     * @param {Object} node assumed to be a Fancytree node object.
     * @param {Object} data assumed to be a PathItem or PagedItemList object
     */
    _addChildren: function (node, data) {
      var expNode = null,
        self = this;
      var temp = self.settings.showFoldersOnly
        ? data.PathItem
        : data.PagedItemList.childrenInPage;
      if (!temp) {
        return;
      }
      if (!Array.isArray(temp)) {
        temp = [temp];
      }

      $.each(temp, function () {
        var exclude = self.settings.showFoldersOnly && this.leaf;

        if (
          typeof this.category != "undefined" &&
          self.settings.filter != null
        ) {
          exclude =
            exclude ||
            (self.settings.filter === $.PercFinderTreeConstants.FOLDERS_ONLY &&
              this.category !== "FOLDER" &&
              this.category !== "SYSTEM");
          exclude =
            exclude ||
            (self.settings.filter === $.PercFinderTreeConstants.SECTIONS_ONLY &&
              this.category !== "SECTION_FOLDER");
        }
        if (!exclude) {
          var nodeObj = self._makeDtNode(this);
          var chNode = node.addChild(nodeObj);
          if (!self.intialPathExpanded) {
            var currentPath = self._normalizedPath(this.path);
            var inPath = self._normalizedPath(self.settings.initialPath);
            if (inPath === currentPath) {
              chNode.setActive(true);
              self.settings.getInitialPathItem(chNode.data.pathItem);
              self.intialPathExpanded = true;
            } else if (inPath.indexOf(currentPath) !== -1) {
              expNode = chNode;
            }
          }
        }
      });

      if (
        !self.settings.showFoldersOnly &&
        data.PagedItemList.startIndex +
          $.PercFinderTreeConstants.MAX_RESULTS -
          1 <
          data.PagedItemList.childrenCount
      ) {
        var nodeObj = self._makeMoreResultsDtNode(
          data.PagedItemList.startIndex + $.PercFinderTreeConstants.MAX_RESULTS,
          node,
        );
        node.addChild(nodeObj);
      }
      self._adjustScrollWidths();
      if (expNode) {
        expNode.setExpanded(true);
      } else if (!self.initialRenderCompleted) {
        self.settings.onRenderComplete(node.data.pathItem, node);
        self.initialRenderCompleted = true;
      }
    },
    _loadMoreChildren: function (node) {
      var self = this,
        url,
        nodeData = node.data;
      url =
        $.perc_paths.PATH_PAGINATED_FOLDER +
        nodeData.parentNode.data.pathItem.path +
        "?startIndex=" +
        nodeData.startIndex +
        "&maxResults=" +
        $.PercFinderTreeConstants.MAX_RESULTS;
      $.PercServiceUtils.makeJsonRequest(
        url,
        $.PercServiceUtils.TYPE_GET,
        false,
        function (status, result) {
          if (status === $.PercServiceUtils.STATUS_ERROR) {
            var defaultMsg =
              $.PercServiceUtils.extractDefaultErrorMessage(request);
            $.perc_utils.alert_dialog({
              title: I18N.message("perc.ui.publish.title@Error"),
              content: defaultMsg,
            });
          } else {
            self._addChildren(nodeData.parentNode, result.data);
            node.remove();
          }
        },
        null,
      );
    },
    /**
     * Calls the onExpand call back function by passing the PathItem objects of the selected node on the tree.
     */
    _onExpand: function (flag, node) {
      this._adjustScrollWidths();
    },

    /**
     *Assigns the scroll-width of the perc-folder-selector and assigns it to  perc-finder-tree.
     */
    _adjustScrollWidths: function (node) {},

    /**
     * Calls the onClick call back function by passing the PathItem objects of the selected node on the tree.
     */
    _onClick: function (node) {
      this.settings.onClick(node.data.pathItem);
    },

    /**
     * Returns the array of the selected nodes PathItem objects.
     */
    getSelectedItems: function () {
      var pathItems = [];
      var selNodes = $("#perc-finder-tree").fancytree("getSelectedNodes");
      $.each(selNodes, function () {
        pathItems.push(this.data.pathItem);
      });
      return pathItems;
    },
    /**
     * Returns the Folder ID of the selected Folder.
     */
    getFolderID: function (node) {
      var pathID = node.data.pathItem.id;
      var folderID;
      if (typeof pathID != "undefined") {
        var splitID = pathID.split("-");
        folderID = splitID[2];
        $.PercFinderTreeConstants.FOLDER_ID = folderID;
      }
      return folderID;
    },

    /**
     * Returns the Fancytree object that this widget creates. This object is useful for getting the selected node
     * info or root node info etc ... See the documentation of Fancytree for the available methods.
     * Note: As the nodes are handled by this class, use readonly methods of the Fancytree object.
     * If the modifications are needed then add new methods to this plugin.
     */
    getFancyTree: function () {
      return $("#perc-finder-tree").fancytree("getTree");
    },

    /**
     * Helper function to create a Fancytree node from the pathItem.
     * @param pathItem Expects the pathItem to be in the format of PSPathItem.
     */
    _makeDtNode: function (pathItem) {
      var self = this;
      var item_path = $.perc_utils.extract_path(pathItem.path);
      var icon = $.perc_utils.choose_icon(
        pathItem.type,
        pathItem.icon,
        item_path,
      );
      var key = this._generateKey(pathItem.path);
      var node = {
        title: pathItem.name,
        isFolder: !pathItem.leaf,
        isLazy: true,
        icon: icon.src,
        tooltip: pathItem.name,
        pathItem: pathItem,
        key: key,
      };
      if (pathItem.leaf) {
        $.extend(node, { addClass: "perc-hide-node-expander" });
      }
      return node;
    },
    /**
     * Helper function to create a Fancytree node from the show more link.
     * @param {String} startIndex The start index for the next set of results
     * @param {String} parentNode assumed to Fancytree node object of parent.
     */
    _makeMoreResultsDtNode: function (startIndex, parentNode) {
      var label = I18N.message("perc.ui.finder.tree@Show More");
      return {
        title: label,
        isLazy: true,
        tooltip: label,
        addClass: "perc-hide-node-expander",
        startIndex: startIndex,
        type: "SHOW_MORE",
        isShowing: false,
        parentNode: parentNode,
      };
    },

    /**
     * Helper function to generatea unique key from the path
     * @param {Object} path assumed not blank
     */
    _generateKey: function (path) {
      if (!path) return null;
      //Create a unique id using the full path for the path item.
      if (path && path.length > 1 && path.substring(path.length - 1) === "/") {
        path = path.slice(0, path.length - 1);
      }
      return path.replace(/[^a-zA-Z0-9/]/g, "_").replace(/\//g, "-");
    },
    /**
     * Helper function to normalize the path
     * @param {Object} path assumed not blank
     */
    _normalizedPath: function (path) {
      if (!path || path.length < 1) return path;
      if (path.substring(path.length - 1, path.length) != "/")
        return path + "/";
      return path;
    },
  });
})(jQuery);
