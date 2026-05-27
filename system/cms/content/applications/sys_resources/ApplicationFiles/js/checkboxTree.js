/**
 *  Renders a checkbox tree control.
 *  This is a JQuery plugin.
 * 
 * See sys_Templates for the checkboxTree control. It is a replacement
 * for the applet checkbox tree control.
 * Options:
 * @url is an XML resource that dictates the structure of the tree. See sys_CheckboxTreeControl.dtd
 * @selected The values in comma separated list where the separator is ';' thus the data (node ids) cannot have semicolons in.
 * @paramName The paramter name used to update the hidden input field after values are selected.
 * @readonly Read only mode.
 * @debug turns debugging on if true.
 *  Usage:
     var opts = {url : treeSrcUrl, selected : selectedValues, paramName : paramName, readonly=false};
     $('#my-tree').perc_checkboxTree(opts);
 * 
 * Migration: Converted from dynatree to fancytree (2024)
 */
(function($) {
$.fn.perc_checkboxTree = function(opts){
debug("In plugin");

var defaults = {
        separator: ";",
        readonly: false,
        inputId: opts.paramName
};

var options = $.extend(defaults, opts); 

debug(opts);
function debug(o) {
    if (opts.debug)
        console.log(o);
}

return this.each(function() {
    debug("In selected element");
    var $this = $(this);
    var paramName = options.paramName;
    var treeSrcUrl = options.url;
    var selectedValues = options.selected;
    var readonly = options.readonly;
    var inputId = options.inputId;
    var separator = options.separator;
    function getTreeXml(url, callback) {
        $.ajax({
            type: "GET",
            url: url,
            dataType: "xml",
            success: callback
        });
    }

    /*
     * Recursively builds the JSON tree from an XML tree.
     */
    function doNode(node, parentItem, selected) {
        var id = node.attr('id');
        var title = node.attr('label');
        var key = parentItem.key + "/" + id;
        var hideCheckbox = node.attr('selectable') == 'no' ? true : false;
        var select = $.inArray(key,selected) != -1;
        var children = node.children();
        var resultItem = { id : id, title: title, select: select, hideCheckbox: hideCheckbox, expand:true, key: key};
        if (readonly)
            resultItem.unselectable = true;
        if (children.length) {
            var subNodes = $.map(children, function (c) {
                return doNode($(c), resultItem, selected);
            });
            resultItem.children = subNodes;
        }
        return resultItem;
    }
    
   function  updateNodeAria(node) {
	   		/*
     	  // Assign the checkbox role "on the fly"
     	  $(node.span).children('a')
     	    .attr('role', 'checkbox');

     	  // Show the user that it the checkbox is controlling the matching
     	  // packages list
     	  $(node.span).children('a');

     	  // Update the checkbox state
     	  var bSelected = node.isSelected();
     	  $(node.span).children('a')
     	    .attr('aria-checked', bSelected);

     	  // Call the whole li the tree-item, noting that each li has:
     	  //   a <span> for the connector
     	  //   a span for the connector
     	  //   an <a> for the text label
     	  $(node.span).parent('li')
     	    .attr('role', 'treeitem');

     	  // Update the expanded state
     	  // If it has no children, then no need to set the aria-expanded attribute
     	  var bExpanded = node.isExpanded();
     	  if (node.hasChildren() === false) {
     	    $(node.span).parent('li').removeAttr('aria-expanded');
     	  } else {
     	    $(node.span).parent('li')
     	      .attr('aria-expanded', bExpanded);
     	  }

     	  // Set the id of the label "on the fly"
     	  var idHandle = _.str.slugify(node.title);
     	  $(node.span).children('a')
     	    .attr('id', idHandle);

     	  // Now we can point to it as the label
     	  $(node.span).parent('li')
     	    .attr('aria-labelledby', idHandle);
		*/
     	}
    	
    function displayTree(data) {
        if($this.data("ui-fancytree")){
            $this.fancytree("destroy");
        }
        $this.fancytree({
          checkbox: true,
          selectMode: 2,
          source: data,
          select: function(event, data) {
            // Display list of selected nodes
            var selNodes = data.tree.getSelectedNodes();
            // convert to title/key array
            var selKeys = $.map(selNodes, function(node){
                 return node.key;
            });
            $('#' + inputId)[0].value = selKeys.join(separator);
            updateNodeAria(data.node);
          },
          click: function(event, data) {
            // We should not toggle, if target was "checkbox", because this
            // would result in double-toggle (i.e. no toggle)
            if(event.target.classList.contains("fancytree-title"))
              data.node.toggleSelect();
            updateNodeAria(data.node);
          },
          focus: function(event, data) {
              updateNodeAria(data.node);
          },
          keydown: function(event, data) {
        	  // Use arrows to navigate within tree, rather than tabs
              // 2012-11-07 on expand/collapse, need to reset tabindexes
              // Performance TODO: scope selection to items that have changed
              $('.fancytree-title').attr('tabindex', -1);

              // Only first item in tree should have a tab stop
              // so we can quickly navigate out of it
              $('.fancytree-title:first').attr('tabindex', 0);
              
              // Focus should return the last tree node visited
              event.target.setAttribute('tabindex', '0');
              
        	  // 13 is enter, 32 is spacebar
              if (event.keyCode === 32) {
              data.node.toggleSelect();
              updateNodeAria(data.node);
              return false;
            }
          },
          // The following options are only required, if we have more than one tree on one page:
          cookie: { key: "ui-fancytree-Cb2-" + paramName },
          idPrefix: "ui-fancytree-Cb2-" + paramName + "-"
        });
        resizeTreeWidthToFitContent();
    }

    function resizeTreeWidthToFitContent() {
        var container = $(".fancytree-container");
        var parentWidth = container.parent().width();
        var biggestWidth = parentWidth;
        $(".fancytree-title").each(function(index,title){
            var width = $(title).width();
            var leftOffset = $(title).offset().left;
            width += leftOffset;
            if(width > biggestWidth)
                biggestWidth = width;
        });
        if(biggestWidth > parentWidth)
            container.width(biggestWidth);
    }

    getTreeXml(treeSrcUrl, function(xml) {
        _testXml = xml;
        var rootItem = {};
        var selected = selectedValues.split(separator);
        var rootNode = $(xml).children('tree');
        rootItem.title = rootNode.attr('label');
        rootItem.hideCheckbox = true;
        rootItem.select = false;
        rootItem.expand = true;
                rootItem.key = "/" + rootItem.title;
        var children = $.map(rootNode.children(), function (c) {
            return doNode($(c), rootItem, selected);
        });
        if (children)
            rootItem.children = children;
        var tree = [rootItem];
        _testTree = tree;
        if(tree[0].children.length > 0){
            displayTree(tree);
        }else{
            $("#page_categories_tree-tree").html("").css({"text-align" : "center", "font-size" : "large"});
        }
    });

});};

/*
 * Below is for testing and debuging.
 */
 
var _testXml;
var _testTree;
var _treeData = [
{title: "item1 with key and tooltip", tooltip: "Look, a tool tip!" },
{title: "item2: selected on init", select: true },
{title: "Folder", isFolder: true, key: "id3",
  children: [
    {title: "Sub-item 3.1",
      children: [
        {title: "Sub-item 3.1.1", key: "id3.1.1" },
        {title: "Sub-item 3.1.2", key: "id3.1.2" }
      ]
    },
    {title: "Sub-item 3.2",
      children: [
        {title: "Sub-item 3.2.1", key: "id3.2.1" },
        {title: "Sub-item 3.2.2", key: "id3.2.2" }
      ]
    }
  ]
},
{title: "Document with some children (expanded on init)", key: "id4", expand: true,
  children: [
    {title: "Sub-item 4.1 (active on init)", activate: true,
      children: [
        {title: "Sub-item 4.1.1", key: "id4.1.1" },
        {title: "Sub-item 4.1.2", key: "id4.1.2" }
      ]
    },
    {title: "Sub-item 4.2 (selected on init)", select: true,
      children: [
        {title: "Sub-item 4.2.1", key: "id4.2.1" },
        {title: "Sub-item 4.2.2", key: "id4.2.2" }
      ]
    },
    {title: "Sub-item 4.3 (hideCheckbox)", hideCheckbox: true },
    {title: "Sub-item 4.4 (unselectable)", unselectable: true }
  ]
}
];

})(jQuery);
