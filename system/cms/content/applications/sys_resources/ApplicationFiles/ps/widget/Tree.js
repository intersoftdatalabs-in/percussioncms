/******************************************************************************
 *
 * [ ps.widget.Tree.js ]
 *
 * COPYRIGHT (c) 1999 - 2007 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/

/**
 * Comments from Dojo (ps.widget.TreeV3):
 * Tree model does all the drawing, visual node management etc.
 * Throws events about clicks on it, so someone may catch them and process
 */

// ps.widget.Tree — dojo.provide/require removed (jQuery + ps/compat.js)

ps.widget.defineWidget("ps.widget.Tree", ps.widget.TreeV3, {
  /**
   * Selector helps activate the nodes (selects) them.
   */
  selector: "",

  /**
   * Tree Model.
   * @see ps.aa.Tree
   */
  model: null,

  /**
   * Flag to see if the tree is already loaded or not.
   */
  loaded: false,

  /**
   * Tree controller.
   */
  treeController: null,

  /**
   * Load custom css
   */
  templateCssPath: ps.uri.moduleUri("ps", "widget/Tree.css"),

  /**
   * Slot nodes, for which initialization is not completed yet.
   */
  delayedInitSlotNodes: [],

  /**
   * Item nodes, for which initialization is not completed yet.
   */
  delayedInitItemNodes: [],

  /**
   * Indicates whether browser supports Array.indexOf.
   */
  indexOfSupported: Array.indexOf,

  /** Creates a tree node from a model node.
   *
   * @param {ps.aa.TreeNode} modelNode
   *
   * @return {ps.widget.TreeNodeV3}
   */
  createWidgetFromModelNode: function (modelNode) {
    // console.debug("Creating widget node from model " + modelNode.toString());
    ps.assertType(modelNode, ps.aa.TreeNode);
    var title = modelNode.getLabel();
    var widgetId = modelNode.objId.getTreeNodeWidgetId();
    var widgetNode = ps.widget.createWidget("TreeNodeV3", {
      title: title,
      tree: this.widgetId,
      id: widgetId,
      modelId: modelNode.objId,
      tryLazyInit: true,
      isFolder: !modelNode.isLeafNode(),
    });
    var _this = this;
    widgetNode.setChildren = function () {
      // get model node again, because it could be changed already
      var n = _this.model.getNodeById(widgetNode.modelId);
      if (!n.isLeafNode()) {
        for (var i = 0; i < n.childNodes.count; i++) {
          var childModel = n.childNodes.item(i);
          widgetNode.addChild(_this.createWidgetFromModelNode(childModel));
        }
      }
    };

    var oid = modelNode.objId;
    var noMove = !oid.isSnippetNode();
    var hasParentCanCheckout =
      modelNode.parentNode && modelNode.parentNode.objId.isCheckoutByMe();
    var noAddChild =
      !(oid.isSlotNode() && hasParentCanCheckout) || oid.isPageNode();
    var actionsDisabled = [];
    if (noAddChild) actionsDisabled.push("ADDCHILD");
    if (noMove) actionsDisabled.push("MOVE");
    widgetNode.actionsDisabled = actionsDisabled;

    //For some reason dojo does not set html id's to tree nodes
    widgetNode.domNode.setAttribute("id", widgetId);
    this._bindContextMenu(widgetNode);
    return widgetNode;
  },

  /**
   * Binds the context menu to a node widget.
   *
   * @param {ps.widget.TreeNodeV3}
   */
  _bindContextMenu: function (widget) {
    var objId = widget.modelId;
    // it's possible the menus are not created yet during initialization
    if (objId.isSlotNode()) {
      if (ps.aa.Menu.slotCtxMenu) {
        ps.aa.Menu.slotCtxMenu.bindTargetNodes([widget.domNode]);
      } else {
        ps.assert(
          !this.indexOfSupported ||
            this.delayedInitSlotNodes.indexOf(widget.domNode) === -1,
          "Slot node is registered more than once: " + widget.domNode,
        );
        this.delayedInitSlotNodes.push(widget.domNode);
      }
    } else {
      if (ps.aa.Menu.itemCtxMenu) {
        ps.aa.Menu.itemCtxMenu.bindTargetNodes([widget.domNode]);
      } else {
        ps.assert(
          !this.indexOfSupported ||
            this.delayedInitItemNodes.indexOf(widget.domNode) === -1,
          "Snippet node is registered more than once: " + widget.domNode,
        );
        this.delayedInitItemNodes.push(widget.domNode);
      }
    }
  },

  /**
   * Unbinds the context menu from the node widget.
   *
   * @param {ps.widget.TreeNodeV3}
   */
  _unBindContextMenu: function (widget) {
    var objId = widget.modelId;
    if (objId.isSlotNode()) {
      ps.aa.Menu.slotCtxMenu.unBindTargetNodes([widget.domNode]);
    } else {
      ps.aa.Menu.itemCtxMenu.unBindTargetNodes([widget.domNode]);
    }
  },

  /**
   * Loads the tree by creating node widgets based on the model.
   *
   * @param {ps.aa.Tree} model
   */
  loadFromModel: function (model) {
    this.treeController = ps.widget.manager.getWidgetById("treeController");
    this.actionsDisabled.push("ADDCHILD");
    this.model = model;

    this._loadModel();
  },

  /**
   * Finishes functionality of {@link loadModel}, which can be executed later
   * and requires other UI subsystems to be initialized.
   * @param {ps.aa.Tree} model the tree model to use. Not null.
   */
  loadFromModelAsynch: function (model) {
    ps.assertType(model, ps.aa.Tree);
    if (this.delayedInitSlotNodes.length) {
      ps.aa.Menu.slotCtxMenu.bindTargetNodes(this.delayedInitSlotNodes);
    }
    if (this.delayedInitItemNodes.length) {
      ps.aa.Menu.itemCtxMenu.bindTargetNodes(this.delayedInitItemNodes);
    }
  },

  /**
   * Reloads the model.
   */
  _loadModel: function () {
    if (this.loaded) {
      ps.assert(this.children);
      ps.assertType(this.children, Array);
      if (this.children.length > 0) {
        var child = this.children[0];
        ps.assertType(child, ps.widget.TreeNodeV3);
        this.removeChild(child);
        child.destroy();
      }
      var treeDnd = ps.widget.manager.getWidgetById("treeDndController");
      if (treeDnd) {
        treeDnd.reset();
      }
    }

    var rootModelNode = this.model.getRootNode();
    var rootWidgetNode = this.createWidgetFromModelNode(rootModelNode);
    this.addChild(rootWidgetNode);

    this.treeController.expandToLevel(this, this.expandLevel);
    this.loaded = true;
  },

  /**
   * Initializes the tree Drag-and-Drop functionality.
   */
  dndInit: function () {
    if (ps.widget.manager.getWidgetById("treeDndController")) {
      return;
    }

    var dndController = ps.widget.createWidget("ps:TreeDndController", {
      id: "treeDndController",
      controller: "treeController",
    });
    dndController.listenTree(this);
  },

  /**
   * Updates a tree node from a model node.
   *
   * @param {ps.aa.TreeNode} modelNode
   * @param {ps.widget.TreeNodeV3} treeNodeWidget
   *
   * @return {ps.widget.TreeNodeV3}
   */
  _updateWidgetFromModelNode: function (modelNode, parentWidget) {
    // console.debug("Trying to update an existing tree " +
    //      "node widget with modelNode: " + modelNode.toString());

    var childWidget = this.getWidgetFromModelNode(modelNode);
    if (childWidget) {
      // console.debug("sync: Found an existing widget that " +
      //      "matchs this modelNode: " + modelNode.toString());
      if (!parentWidget) {
        // console.debug("parentWidget is null so this must be the root node.");
      } else if (parentWidget == childWidget.parent) {
        // console.debug("This child node had the same parent as before (OK).");
        childWidget.doDetach();
      } else {
        /*
         * detach the child from the old parent and attach it to the
         * new parent.
         */
        // console.debug("Detaching child widget from old parent.");
        childWidget.doDetach();
        // The caller of this method will add the node to the parent.
      }

      if (childWidget.title != modelNode.getLabel()) {
        // console.debug("Title changed from " + childWidget.title
        // + " to " + modelNode.getLabel());
        childWidget.setTitle(modelNode.getLabel());
      }
      childWidget.modelId = modelNode.objId;
    } /* did not find widget for model */ else {
      // console.debug("Did not find widget corresponding to model node");
      childWidget = this.createWidgetFromModelNode(modelNode);
    }
    return childWidget;
  },

  /**
   * Removes the tree nodes that are no longer exist in the tree model.
   */
  _cleanTree: function () {
    // console.debug("Tree - Cleaning Tree - Start");

    var deadNodes = [];
    var root = this.children[0];
    var stack = [root];
    while ((wNode = stack.pop())) {
      var mNode = this.model.getNodeById(wNode.modelId);
      if (mNode) {
        // console.debug("Tree has node " + mNode.toString());
        for (var i = 0; i < wNode.children.length; i++) {
          stack.push(wNode.children[i]);
        }
      } else {
        // console.debug("Node is dead: " + wNode.toString());
        deadNodes.push(wNode);
      }
    }

    // removes all invalid nodes and its decendents
    for (var i = 0; i < deadNodes.length; i++) {
      this._removeNodes(deadNodes[i]);
    }
    // console.debug("Tree - Cleaning Tree - End");
  },

  /**
   * Removes a node and its decendent nodes from the tree.
   *
   * @param {ps.widget.TreeNodeV3} node The to be removed node, which may
   *    contain child node.
   */
  _removeNodes: function (node) {
    // console.debug("Tree - Removing NODES: " + node.modelId.toString() + ", len=" + node.children.length);
    ps.assert(node, "Can't remove null node.");

    // removes the child nodes first if any
    while (node.children.length > 0) {
      this._removeNodes(node.children[0]);
    }

    this._removeNode(node);
  },

  /**
   * Removes a node from the tree.
   *
   * @param {ojo.widget.TreeNodeV3} node The to be removed node, which may not
   *    contain any child node.
   */
  _removeNode: function (node) {
    // console.debug("Tree - Removing node: " + node.modelId.toString());

    ps.assert(node, "Can't remove null node.");
    ps.assert(!node.children.length, "Can't remove a node with children.");

    this._unBindContextMenu(node);
    node.destroy();
  },

  /**
   * Reloads the model.
   */
  _synchModel: function () {
    // console.debug("Tree - Synchronizing Tree to Model - Start");

    // Clean Widgets that are no longer in the tree.
    this._cleanTree();
    if (this.children.length == 0) {
      this._loadModel();
      return;
    }

    var rootModelNode = this.model.getRootNode();
    var rootWidgetNode = this._updateWidgetFromModelNode(rootModelNode, null);
    var child = this.children[0];

    // console.debug("original root = " + child);
    // console.debug("new root = " + rootWidgetNode);

    ps.assert(
      child == rootWidgetNode,
      "The root widget node should not have changed.",
    );

    var modelAndWidget = { model: rootModelNode, widget: rootWidgetNode };
    var stack = [modelAndWidget];
    while ((mw = stack.pop())) {
      var w = mw.widget;
      var m = mw.model;
      if (!m.isLeafNode() && !w.tryLazyInit) {
        // console.debug("Number of children: " + m.childNodes.count);
        for (var i = 0; i < m.childNodes.count; i++) {
          var childModel = m.childNodes.item(i);
          var childWidget = this._updateWidgetFromModelNode(childModel, w);
          stack.push({ model: childModel, widget: childWidget });
          w.addChild(childWidget, i, false);
        }
      } else {
        // console.debug("Tree - leaf node, no children");
      }
      this._updateIsFolderFromModel(w, m);
    }
    this.loaded = true;
    var treeDnd = ps.widget.manager.getWidgetById("treeDndController");
    if (treeDnd) {
      treeDnd.reset();
    }

    // console.debug("Tree - Synchronizing Tree to Model - End");
  },

  /**
   * Insures that tree node widget folder indicator value corresponds to
   * the model.
   *
   * @param {ps.widget.TreeNodeV3} nodeWidget the node widget to set folder
   * status value for.
   * Assumed not null.
   * @param {ps.aa.TreeNode} modelNode the corresponding model.
   * Assumed not null.
   */
  _updateIsFolderFromModel: function (nodeWidget, modelNode) {
    if (nodeWidget.tryLazyInit) {
      if (!modelNode.isLeafNode() !== nodeWidget.isFolder) {
        // model and lazy node are out of sync
        if (modelNode.isLeafNode()) {
          nodeWidget.unsetFolder();
        } else {
          nodeWidget.setFolder();
        }
      }
    } else {
      // ignore non-lazy nodes
    }
  },

  /**
   * Get tree node widget from a tree node model id.
   *
   * @param {ps.aa.ObjectId} objId
   */
  getWidgetFromModelId: function (objId) {
    var widgetId = objId.getTreeNodeWidgetId();
    var widget = ps.widget.manager.getWidgetById(widgetId);
    return widget;
  },

  /**
   * Get widget from model node.
   * @param {ps.aa.TreeNode} treeNode
   */
  getWidgetFromModelNode: function (treeNode) {
    return this.getWidgetFromModelId(treeNode.objId);
  },

  /**
   * Selects the widget node give the corresponding model node.
   * @param {ps.aa.ObjectId} treeNodeId
   */
  activate: function (treeNodeId) {
    var _this = this;
    // expand from top to trigger lazy node loading
    function expandTo(n) {
      ps.assert(n, "Tree model node is expected to be not null.");
      if (n.parentNode) {
        expandTo(n.parentNode);

        var w = _this.getWidgetFromModelId(n.parentNode.objId);
        ps.assert(w, "Can't find a widget for  " + n.parentNode);
        w.expand();
      }
    }

    var treeNode = this.model.getNodeById(treeNodeId);
    expandTo(treeNode);
    var widget = this.getWidgetFromModelId(treeNode.objId);
    var selector = ps.widget.manager.getWidgetById(this.selector);
    selector.deselectAll();
    selector.select(widget);
  },

  /**
   * doMove is used to move snippets within a slot or to a new slot.
   * @see {ps.widget.TreeV3#doMove}
   * @Override ps.widget.TreeV3#doMove
   */
  doMove: function (child, newParent, index) {
    // console.debug("Tree move: "+child+" to "+newParent+" at "+index);
    //ps.aa.SnippetMove = function (snippetId, slotId, targetSlotId, targetIndex,
    // dontUpdatePage)

    ps.assert(child.modelId, "Node being moved does not have a model");
    ps.assert(newParent.modelId, "Node being moved does not have a model");
    var pid = newParent.modelId;
    var cid = child.modelId;
    ps.assert(cid.isSnippetNode(), "child is not a snippet.");
    var childModelNode = this.model.getNodeById(child.modelId);
    var parentModelNode = childModelNode.parentNode;
    ps.assert(parentModelNode, "Unable to get the parent model node.");
    var sid = parentModelNode.objId;
    ps.assert(sid.isSlotNode(), "Original parent of child is not a slot.");
    ps.assert(pid.isSlotNode(), "New parent is not a slot.");
    var targetSlotId = pid;
    var snippetId = cid;
    var slotId = sid;

    var move = new ps.aa.SnippetMove(
      snippetId,
      slotId,
      targetSlotId,
      index + 1,
      false,
    );
    var success = ps.aa.controller.moveToSlot(move);

    //var parent = child.parent;
    if (success == true) {
      // console.debug("Tree - successful move.");
      var snipid = move.getTargetSnippetId();
      try {
        // could fail if the target slot requires a template change
        // and the template selection dialog is called
        ps.aa.controller.activate(snipid);
      } catch (e) {
        console.debug("Ignore on a template change request");
        console.debug(e);
      }
      //We don't need resync the tree because the controller will.
    } else {
      console.debug("Tree - move failed.");
    }
  },

  /**
   * A listener for {@link ps.aa.Tree#onModelChanged}.
   * @see ps.aa.Tree
   */
  onModelChanged: function () {
    // console.debug("tree on model change called.");

    if (this.loaded) {
      this._synchModel();
    } else {
      this._loadModel();
    }

    // console.debug("tree on model change SUCCESSFUL");
  },
});
