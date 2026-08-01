/******************************************************************************
 *
 * [ ps.widget.TreeSelector.js ]
 *
 * COPYRIGHT (c) 1999 - 2007 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/

// ps.widget.TreeSelector — dojo.provide/require removed (jQuery + ps/compat.js)

ps.widget.defineWidget(
  "ps.widget.TreeSelector",
  ps.widget.TreeSelectorV3,
  function () {
    ps.event.connect(this, "processNode", this, "_nodeActivated");
  },
  {
    /**
     * Is called when a tree node is activated/deactivated.
     * @param node the tree node to activate. Not <code>null</code>
     */
    _nodeActivated: function (node) {
      ps.assert(node);
      var objId = node.modelId;
      ps.assert(objId, "widget does not have a model id");
      ps.aa.controller.activate(objId);
    },
  },
);
