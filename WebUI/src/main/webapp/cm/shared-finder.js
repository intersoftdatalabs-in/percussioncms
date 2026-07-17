/*! jQuery Fancytree Plugin - 2.38.3 - 2023-02-01T20:52:50Z
  * https://github.com/mar10/fancytree
  * Copyright (c) 2023 Martin Wendt; Licensed MIT
 */
(function( factory ) {
	if ( typeof define === "function" && define.amd ) {
		// AMD. Register as an anonymous module.
		define( [
			"jquery",
			"jquery-ui/ui/widgets/mouse",
			"jquery-ui/ui/widgets/draggable",
			"jquery-ui/ui/widgets/droppable",
			"jquery-ui/ui/effects/effect-blind",
			"jquery-ui/ui/data",
			"jquery-ui/ui/effect",
			"jquery-ui/ui/focusable",
			"jquery-ui/ui/keycode",
			"jquery-ui/ui/position",
			"jquery-ui/ui/scroll-parent",
			"jquery-ui/ui/tabbable",
			"jquery-ui/ui/unique-id",
			"jquery-ui/ui/widget"
		], factory );
	} else if ( typeof module === "object" && module.exports ) {
		// Node/CommonJS
		module.exports = factory(require("jquery"));
	} else {
		// Browser globals
		factory( jQuery );
	}
}(function( $ ) {

!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree.ui-deps"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree.ui-deps"),module.exports=e(require("jquery"))):e(jQuery)}(function(k){"use strict";if(!k.ui||!k.ui.fancytree){for(var e,h=null,c=new RegExp(/\.|\//),t=/[&<>"'/]/g,n=/[<>"'/]/g,f="$recursive_request",p="$request_target_invalid",i={"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;","/":"&#x2F;"},r={16:!0,17:!0,18:!0},u={8:"backspace",9:"tab",10:"return",13:"return",19:"pause",20:"capslock",27:"esc",32:"space",33:"pageup",34:"pagedown",35:"end",36:"home",37:"left",38:"up",39:"right",40:"down",45:"insert",46:"del",59:";",61:"=",96:"0",97:"1",98:"2",99:"3",100:"4",101:"5",102:"6",103:"7",104:"8",105:"9",106:"*",107:"+",109:"-",110:".",111:"/",112:"f1",113:"f2",114:"f3",115:"f4",116:"f5",117:"f6",118:"f7",119:"f8",120:"f9",121:"f10",122:"f11",123:"f12",144:"numlock",145:"scroll",173:"-",186:";",187:"=",188:",",189:"-",190:".",191:"/",192:"`",219:"[",220:"\\",221:"]",222:"'"},g={16:"shift",17:"ctrl",18:"alt",91:"meta",93:"meta"},o={0:"",1:"left",2:"middle",3:"right"},v="active expanded focus folder lazy radiogroup selected unselectable unselectableIgnore".split(" "),y={},b="columns types".split(" "),m="checkbox expanded extraClasses folder icon iconTooltip key lazy partsel radiogroup refKey selected statusNodeType title tooltip type unselectable unselectableIgnore unselectableStatus".split(" "),s={},x={},a={active:!0,children:!0,data:!0,focus:!0},l=0;l<v.length;l++)y[v[l]]=!0;for(l=0;l<m.length;l++)e=m[l],s[e]=!0,e!==e.toLowerCase()&&(x[e.toLowerCase()]=e);var N=Array.isArray;return _(k.ui,"Fancytree requires jQuery UI (http://jqueryui.com)"),Date.now||(Date.now=function(){return(new Date).getTime()}),I.prototype={_findDirectChild:function(e){var t,n,i=this.children;if(i)if("string"==typeof e){for(t=0,n=i.length;t<n;t++)if(i[t].key===e)return i[t]}else{if("number"==typeof e)return this.children[e];if(e.parent===this)return e}return null},_setChildren:function(e){_(e&&(!this.children||0===this.children.length),"only init supported"),this.children=[];for(var t=0,n=e.length;t<n;t++)this.children.push(new I(this,e[t]));this.tree._callHook("treeStructureChanged",this.tree,"setChildren")},addChildren:function(e,t){var n,i,r,o,s=this.getFirstChild(),a=this.getLastChild(),l=[];for(k.isPlainObject(e)&&(e=[e]),this.children||(this.children=[]),n=0,i=e.length;n<i;n++)l.push(new I(this,e[n]));if(o=l[0],null==t?this.children=this.children.concat(l):(t=this._findDirectChild(t),_(0<=(r=k.inArray(t,this.children)),"insertBefore must be an existing child"),this.children.splice.apply(this.children,[r,0].concat(l))),s&&!t){for(n=0,i=l.length;n<i;n++)l[n].render();s!==this.getFirstChild()&&s.renderStatus(),a!==this.getLastChild()&&a.renderStatus()}else this.parent&&!this.parent.ul&&!this.tr||this.render();return 3===this.tree.options.selectMode&&this.fixSelection3FromEndNodes(),this.triggerModifyChild("add",1===l.length?l[0]:null),o},addClass:function(e){return this.toggleClass(e,!0)},addNode:function(e,t){switch(t=void 0===t||"over"===t?"child":t){case"after":return this.getParent().addChildren(e,this.getNextSibling());case"before":return this.getParent().addChildren(e,this);case"firstChild":var n=this.children?this.children[0]:null;return this.addChildren(e,n);case"child":case"over":return this.addChildren(e)}_(!1,"Invalid mode: "+t)},addPagingNode:function(e,t){var n,i;if(t=t||"child",!1!==e)return e=k.extend({title:this.tree.options.strings.moreData,statusNodeType:"paging",icon:!1},e),this.partload=!0,this.addNode(e,t);for(n=this.children.length-1;0<=n;n--)"paging"===(i=this.children[n]).statusNodeType&&this.removeChild(i);this.partload=!1},appendSibling:function(e){return this.addNode(e,"after")},applyCommand:function(e,t){return this.tree.applyCommand(e,this,t)},applyPatch:function(e){if(null===e)return this.remove(),T(this);var t,n,i={children:!0,expanded:!0,parent:!0};for(t in e)C(e,t)&&(n=e[t],i[t]||w(n)||(s[t]?this[t]=n:this.data[t]=n));return C(e,"children")&&(this.removeChildren(),e.children&&this._setChildren(e.children)),this.isVisible()&&(this.renderTitle(),this.renderStatus()),C(e,"expanded")?this.setExpanded(e.expanded):T(this)},collapseSiblings:function(){return this.tree._callHook("nodeCollapseSiblings",this)},copyTo:function(e,t,n){return e.addNode(this.toDict(!0,n),t)},countChildren:function(e){var t,n,i,r=this.children;if(!r)return 0;if(i=r.length,!1!==e)for(t=0,n=i;t<n;t++)i+=r[t].countChildren();return i},debug:function(e){4<=this.tree.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("log",arguments))},discard:function(){return this.warn("FancytreeNode.discard() is deprecated since 2014-02-16. Use .resetLazy() instead."),this.resetLazy()},discardMarkup:function(e){this.tree._callHook(e?"nodeRemoveMarkup":"nodeRemoveChildMarkup",this)},error:function(e){1<=this.tree.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("error",arguments))},findAll:function(t){t=w(t)?t:R(t);var n=[];return this.visit(function(e){t(e)&&n.push(e)}),n},findFirst:function(t){t=w(t)?t:R(t);var n=null;return this.visit(function(e){if(t(e))return n=e,!1}),n},findRelatedNode:function(e,t){return this.tree.findRelatedNode(this,e,t)},_changeSelectStatusAttrs:function(e){var t=!1,n=this.tree.options,i=h.evalOption("unselectable",this,this,n,!1),n=h.evalOption("unselectableStatus",this,this,n,void 0);switch(e=i&&null!=n?n:e){case!1:t=this.selected||this.partsel,this.selected=!1,this.partsel=!1;break;case!0:t=!this.selected||!this.partsel,this.selected=!0,this.partsel=!0;break;case void 0:t=this.selected||!this.partsel,this.selected=!1,this.partsel=!0;break;default:_(!1,"invalid state: "+e)}return t&&this.renderStatus(),t},fixSelection3AfterClick:function(e){var t=this.isSelected();this.visit(function(e){if(e._changeSelectStatusAttrs(t),e.radiogroup)return"skip"}),this.fixSelection3FromEndNodes(e)},fixSelection3FromEndNodes:function(e){var u=this.tree.options;_(3===u.selectMode,"expected selectMode 3"),function e(t){var n,i,r,o,s,a,l,d,c=t.children;if(c&&c.length){for(l=!(a=!0),n=0,i=c.length;n<i;n++)o=e(r=c[n]),h.evalOption("unselectableIgnore",r,r,u,!1)||(!1!==o&&(l=!0),!0!==o&&(a=!1));s=!!a||!!l&&void 0}else s=null==(d=h.evalOption("unselectableStatus",t,t,u,void 0))?!!t.selected:!!d;return t.partsel&&!t.selected&&t.lazy&&null==t.children&&(s=void 0),t._changeSelectStatusAttrs(s),s}(this),this.visitParents(function(e){for(var t,n,i,r=e.children,o=!0,s=!1,a=0,l=r.length;a<l;a++)t=r[a],h.evalOption("unselectableIgnore",t,t,u,!1)||(((n=null==(i=h.evalOption("unselectableStatus",t,t,u,void 0))?!!t.selected:!!i)||t.partsel)&&(s=!0),n||(o=!1));e._changeSelectStatusAttrs(n=!!o||!!s&&void 0)})},fromDict:function(e){for(var t in e)s[t]?this[t]=e[t]:"data"===t?k.extend(this.data,e.data):w(e[t])||a[t]||(this.data[t]=e[t]);e.children&&(this.removeChildren(),this.addChildren(e.children)),this.renderTitle()},getChildren:function(){if(void 0!==this.hasChildren())return this.children},getFirstChild:function(){return this.children?this.children[0]:null},getIndex:function(){return k.inArray(this,this.parent.children)},getIndexHier:function(e,n){e=e||".";var i,r=[];return k.each(this.getParentList(!1,!0),function(e,t){i=""+(t.getIndex()+1),n&&(i=("0000000"+i).substr(-n)),r.push(i)}),r.join(e)},getKeyPath:function(e){var t=this.tree.options.keyPathSeparator;return t+this.getPath(!e,"key",t)},getLastChild:function(){return this.children?this.children[this.children.length-1]:null},getLevel:function(){for(var e=0,t=this.parent;t;)e++,t=t.parent;return e},getNextSibling:function(){if(this.parent)for(var e=this.parent.children,t=0,n=e.length-1;t<n;t++)if(e[t]===this)return e[t+1];return null},getParent:function(){return this.parent},getParentList:function(e,t){for(var n=[],i=t?this:this.parent;i;)(e||i.parent)&&n.unshift(i),i=i.parent;return n},getPath:function(e,t,n){n=n||"/";var i,r=[],o=w(t=t||"title");return this.visitParents(function(e){e.parent&&(i=o?t(e):e[t],r.unshift(i))},e=!1!==e),r.join(n)},getPrevSibling:function(){if(this.parent)for(var e=this.parent.children,t=1,n=e.length;t<n;t++)if(e[t]===this)return e[t-1];return null},getSelectedNodes:function(t){var n=[];return this.visit(function(e){if(e.selected&&(n.push(e),!0===t))return"skip"}),n},hasChildren:function(){return this.lazy?null==this.children?void 0:0!==this.children.length&&(1!==this.children.length||!this.children[0].isStatusNode()||void 0):!(!this.children||!this.children.length)},hasClass:function(e){return 0<=(" "+(this.extraClasses||"")+" ").indexOf(" "+e+" ")},hasFocus:function(){return this.tree.hasFocus()&&this.tree.focusNode===this},info:function(e){3<=this.tree.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("info",arguments))},isActive:function(){return this.tree.activeNode===this},isBelowOf:function(e){return this.getIndexHier(".",5)>e.getIndexHier(".",5)},isChildOf:function(e){return this.parent&&this.parent===e},isDescendantOf:function(e){if(!e||e.tree!==this.tree)return!1;for(var t=this.parent;t;){if(t===e)return!0;t===t.parent&&k.error("Recursive parent link: "+t),t=t.parent}return!1},isExpanded:function(){return!!this.expanded},isFirstSibling:function(){var e=this.parent;return!e||e.children[0]===this},isFolder:function(){return!!this.folder},isLastSibling:function(){var e=this.parent;return!e||e.children[e.children.length-1]===this},isLazy:function(){return!!this.lazy},isLoaded:function(){return!this.lazy||void 0!==this.hasChildren()},isLoading:function(){return!!this._isLoading},isRoot:function(){return this.isRootNode()},isPartsel:function(){return!this.selected&&!!this.partsel},isPartload:function(){return!!this.partload},isRootNode:function(){return this.tree.rootNode===this},isSelected:function(){return!!this.selected},isStatusNode:function(){return!!this.statusNodeType},isPagingNode:function(){return"paging"===this.statusNodeType},isTopLevel:function(){return this.tree.rootNode===this.parent},isUndefined:function(){return void 0===this.hasChildren()},isVisible:function(){var e,t,n=this.tree.enableFilter,i=this.getParentList(!1,!1);if(n&&!this.match&&!this.subMatchCount)return!1;for(e=0,t=i.length;e<t;e++)if(!i[e].expanded)return!1;return!0},lazyLoad:function(e){k.error("FancytreeNode.lazyLoad() is deprecated since 2014-02-16. Use .load() instead.")},load:function(e){var t=this,n=this.isExpanded();return _(this.isLazy(),"load() requires a lazy node"),e||this.isUndefined()?(this.isLoaded()&&this.resetLazy(),!1===(e=this.tree._triggerNodeEvent("lazyLoad",this))?T(this):(_("boolean"!=typeof e,"lazyLoad event must return source in data.result"),e=this.tree._callHook("nodeLoadChildren",this,e),n?(this.expanded=!0,e.always(function(){t.render()})):e.always(function(){t.renderStatus()}),e)):T(this)},makeVisible:function(e){for(var t=this,n=[],i=new k.Deferred,r=this.getParentList(!1,!1),o=r.length,s=!(e&&!0===e.noAnimation),a=!(e&&!1===e.scrollIntoView),l=o-1;0<=l;l--)n.push(r[l].setExpanded(!0,e));return k.when.apply(k,n).done(function(){a?t.scrollIntoView(s).done(function(){i.resolve()}):i.resolve()}),i.promise()},moveTo:function(t,e,n){void 0===e||"over"===e?e="child":"firstChild"===e&&(t.children&&t.children.length?(e="before",t=t.children[0]):e="child");var i,r=this.tree,o=this.parent,s="child"===e?t:t.parent;if(this!==t){if(this.parent?s.isDescendantOf(this)&&k.error("Cannot move a node to its own descendant"):k.error("Cannot move system root"),s!==o&&o.triggerModifyChild("remove",this),1===this.parent.children.length){if(this.parent===s)return;this.parent.children=this.parent.lazy?[]:null,this.parent.expanded=!1}else _(0<=(i=k.inArray(this,this.parent.children)),"invalid source parent"),this.parent.children.splice(i,1);if((this.parent=s).hasChildren())switch(e){case"child":s.children.push(this);break;case"before":_(0<=(i=k.inArray(t,s.children)),"invalid target parent"),s.children.splice(i,0,this);break;case"after":_(0<=(i=k.inArray(t,s.children)),"invalid target parent"),s.children.splice(i+1,0,this);break;default:k.error("Invalid mode "+e)}else s.children=[this];n&&t.visit(n,!0),s===o?s.triggerModifyChild("move",this):s.triggerModifyChild("add",this),r!==t.tree&&(this.warn("Cross-tree moveTo is experimental!"),this.visit(function(e){e.tree=t.tree},!0)),r._callHook("treeStructureChanged",r,"moveTo"),o.isDescendantOf(s)||o.render(),s.isDescendantOf(o)||s===o||s.render()}},navigate:function(e,t){var n=k.ui.keyCode;switch(e){case"left":case n.LEFT:if(this.expanded)return this.setExpanded(!1);break;case"right":case n.RIGHT:if(!this.expanded&&(this.children||this.lazy))return this.setExpanded()}if(n=this.findRelatedNode(e)){try{n.makeVisible({scrollIntoView:!1})}catch(e){}return!1===t?(n.setFocus(),T()):n.setActive()}return this.warn("Could not find related node '"+e+"'."),T()},remove:function(){return this.parent.removeChild(this)},removeChild:function(e){return this.tree._callHook("nodeRemoveChild",this,e)},removeChildren:function(){return this.tree._callHook("nodeRemoveChildren",this)},removeClass:function(e){return this.toggleClass(e,!1)},render:function(e,t){return this.tree._callHook("nodeRender",this,e,t)},renderTitle:function(){return this.tree._callHook("nodeRenderTitle",this)},renderStatus:function(){return this.tree._callHook("nodeRenderStatus",this)},replaceWith:function(e){var n=this.parent,i=k.inArray(this,n.children),r=this;return _(this.isPagingNode(),"replaceWith() currently requires a paging status node"),(e=this.tree._callHook("nodeLoadChildren",this,e)).done(function(e){var t=r.children;for(l=0;l<t.length;l++)t[l].parent=n;n.children.splice.apply(n.children,[i+1,0].concat(t)),r.children=null,r.remove(),n.render()}).fail(function(){r.setExpanded()}),e},resetLazy:function(){this.removeChildren(),this.expanded=!1,this.lazy=!0,this.children=void 0,this.renderStatus()},scheduleAction:function(e,t){this.tree.timer&&(clearTimeout(this.tree.timer),this.tree.debug("clearTimeout(%o)",this.tree.timer)),this.tree.timer=null;var n=this;switch(e){case"cancel":break;case"expand":this.tree.timer=setTimeout(function(){n.tree.debug("setTimeout: trigger expand"),n.setExpanded(!0)},t);break;case"activate":this.tree.timer=setTimeout(function(){n.tree.debug("setTimeout: trigger activate"),n.setActive(!0)},t);break;default:k.error("Invalid mode "+e)}},scrollIntoView:function(e,t){if(void 0!==t&&((p=t).tree&&void 0!==p.statusNodeType))throw Error("scrollIntoView() with 'topNode' option is deprecated since 2014-05-08. Use 'options.topNode' instead.");var n=k.extend({effects:!0===e?{duration:200,queue:!1}:e,scrollOfs:this.tree.options.scrollOfs,scrollParent:this.tree.options.scrollParent,topNode:null},t),i=n.scrollParent,r=this.tree.$container,o=r.css("overflow-y");i?i.jquery||(i=k(i)):i=!this.tree.tbody&&("scroll"===o||"auto"===o)?r:r.scrollParent(),i[0]!==document&&i[0]!==document.body||(this.debug("scrollIntoView(): normalizing scrollParent to 'window':",i[0]),i=k(window));var s,a,l=new k.Deferred,d=this,c=k(this.span).height(),u=n.scrollOfs.top||0,h=n.scrollOfs.bottom||0,f=i.height(),p=i.scrollTop(),e=i,t=i[0]===window,o=n.topNode||null,r=null;return this.isRootNode()||!this.isVisible()?(this.info("scrollIntoView(): node is invisible."),T()):(t?(a=k(this.span).offset().top,s=o&&o.span?k(o.span).offset().top:0,e=k("html,body")):(_(i[0]!==document&&i[0]!==document.body,"scrollParent should be a simple element or `window`, not document or body."),t=i.offset().top,a=k(this.span).offset().top-t+p,s=o?k(o.span).offset().top-t+p:0,f-=Math.max(0,i.innerHeight()-i[0].clientHeight)),a<p+u?r=a-u:p+f-h<a+c&&(r=a+c-f+h,o&&(_(o.isRootNode()||o.isVisible(),"topNode must be visible"),s<r&&(r=s-u))),null===r?l.resolveWith(this):n.effects?(n.effects.complete=function(){l.resolveWith(d)},e.stop(!0).animate({scrollTop:r},n.effects)):(e[0].scrollTop=r,l.resolveWith(this)),l.promise())},setActive:function(e,t){return this.tree._callHook("nodeSetActive",this,e,t)},setExpanded:function(e,t){return this.tree._callHook("nodeSetExpanded",this,e,t)},setFocus:function(e){return this.tree._callHook("nodeSetFocus",this,e)},setSelected:function(e,t){return this.tree._callHook("nodeSetSelected",this,e,t)},setStatus:function(e,t,n){return this.tree._callHook("nodeSetStatus",this,e,t,n)},setTitle:function(e){this.title=e,this.renderTitle(),this.triggerModify("rename")},sortChildren:function(e,t){var n,i,r=this.children;if(r){if(r.sort(e=e||function(e,t){e=e.title.toLowerCase(),t=t.title.toLowerCase();return e===t?0:t<e?1:-1}),t)for(n=0,i=r.length;n<i;n++)r[n].children&&r[n].sortChildren(e,"$norender$");"$norender$"!==t&&this.render(),this.triggerModifyChild("sort")}},toDict:function(e,t){var n,i,r,o,s={},a=this;if(k.each(m,function(e,t){!a[t]&&!1!==a[t]||(s[t]=a[t])}),k.isEmptyObject(this.data)||(s.data=k.extend({},this.data),k.isEmptyObject(s.data)&&delete s.data),t){if(!1===(o=t(s,a)))return!1;"skip"===o&&(e=!1)}if(e&&N(this.children))for(s.children=[],n=0,i=this.children.length;n<i;n++)(r=this.children[n]).isStatusNode()||!1!==(o=r.toDict(!0,t))&&s.children.push(o);return s},toggleClass:function(e,t){var n,i,r=e.match(/\S+/g)||[],o=0,s=!1,a=this[this.tree.statusClassPropName],l=" "+(this.extraClasses||"")+" ";for(a&&k(a).toggleClass(e,t);n=r[o++];)if(i=0<=l.indexOf(" "+n+" "),t=void 0===t?!i:!!t)i||(l+=n+" ",s=!0);else for(;-1<l.indexOf(" "+n+" ");)l=l.replace(" "+n+" "," ");return this.extraClasses=S(l),s},toggleExpanded:function(){return this.tree._callHook("nodeToggleExpanded",this)},toggleSelected:function(){return this.tree._callHook("nodeToggleSelected",this)},toString:function(){return"FancytreeNode@"+this.key+"[title='"+this.title+"']"},triggerModifyChild:function(e,t,n){var i=this.tree.options.modifyChild;i&&(t&&t.parent!==this&&k.error("childNode "+t+" is not a child of "+this),t={node:this,tree:this.tree,operation:e,childNode:t||null},n&&k.extend(t,n),i({type:"modifyChild"},t))},triggerModify:function(e,t){this.parent.triggerModifyChild(e,this,t)},visit:function(e,t){var n,i,r=!0,o=this.children;if(!0===t&&(!1===(r=e(this))||"skip"===r))return r;if(o)for(n=0,i=o.length;n<i&&!1!==(r=o[n].visit(e,!0));n++);return r},visitAndLoad:function(n,e,t){var i,r,o,s=this;return!n||!0!==e||!1!==(r=n(s))&&"skip"!==r?s.children||s.lazy?(i=new k.Deferred,o=[],s.load().done(function(){for(var e=0,t=s.children.length;e<t;e++){if(!1===(r=s.children[e].visitAndLoad(n,!0,!0))){i.reject();break}"skip"!==r&&o.push(r)}k.when.apply(this,o).then(function(){i.resolve()})}),i.promise()):T():t?r:T()},visitParents:function(e,t){if(t&&!1===e(this))return!1;for(var n=this.parent;n;){if(!1===e(n))return!1;n=n.parent}return!0},visitSiblings:function(e,t){for(var n,i=this.parent.children,r=0,o=i.length;r<o;r++)if(n=i[r],(t||n!==this)&&!1===e(n))return!1;return!0},warn:function(e){2<=this.tree.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("warn",arguments))}},F.prototype={_makeHookContext:function(e,t,n){var i,r;return void 0!==e.node?(t&&e.originalEvent!==t&&k.error("invalid args"),i=e):e.tree?i={node:e,tree:r=e.tree,widget:r.widget,options:r.widget.options,originalEvent:t,typeInfo:r.types[e.type]||{}}:e.widget?i={node:null,tree:e,widget:e.widget,options:e.widget.options,originalEvent:t}:k.error("invalid args"),n&&k.extend(i,n),i},_callHook:function(e,t,n){var i=this._makeHookContext(t),r=this[e],t=Array.prototype.slice.call(arguments,2);return w(r)||k.error("_callHook('"+e+"') is not a function"),t.unshift(i),r.apply(this,t)},_setExpiringValue:function(e,t,n){this._tempCache[e]={value:t,expire:Date.now()+(+n||50)}},_getExpiringValue:function(e){var t=this._tempCache[e];return t&&t.expire>Date.now()?t.value:(delete this._tempCache[e],null)},_usesExtension:function(e){return 0<=k.inArray(e,this.options.extensions)},_requireExtension:function(e,t,n,i){null!=n&&(n=!!n);var r=this._local.name,o=this.options.extensions,s=k.inArray(e,o)<k.inArray(r,o),o=t&&null==this.ext[e],s=!o&&null!=n&&n!==s;return _(r&&r!==e,"invalid or same name '"+r+"' (require yourself?)"),!o&&!s||(i||(o||t?(i="'"+r+"' extension requires '"+e+"'",s&&(i+=" to be registered "+(n?"before":"after")+" itself")):i="If used together, `"+e+"` must be registered "+(n?"before":"after")+" `"+r+"`"),k.error(i),!1)},activateKey:function(e,t){e=this.getNodeByKey(e);return e?e.setActive(!0,t):this.activeNode&&this.activeNode.setActive(!1,t),e},addPagingNode:function(e,t){return this.rootNode.addPagingNode(e,t)},applyCommand:function(e,t,n){var i;switch(t=t||this.getActiveNode(),e){case"moveUp":(i=t.getPrevSibling())&&(t.moveTo(i,"before"),t.setActive());break;case"moveDown":(i=t.getNextSibling())&&(t.moveTo(i,"after"),t.setActive());break;case"indent":(i=t.getPrevSibling())&&(t.moveTo(i,"child"),i.setExpanded(),t.setActive());break;case"outdent":t.isTopLevel()||(t.moveTo(t.getParent(),"after"),t.setActive());break;case"remove":i=t.getPrevSibling()||t.getParent(),t.remove(),i&&i.setActive();break;case"addChild":t.editCreateNode("child","");break;case"addSibling":t.editCreateNode("after","");break;case"rename":t.editStart();break;case"down":case"first":case"last":case"left":case"parent":case"right":case"up":return t.navigate(e);default:k.error("Unhandled command: '"+e+"'")}},applyPatch:function(e){for(var t,n,i,r,o=e.length,s=[],a=0;a<o;a++)_(2===(t=e[a]).length,"patchList must be an array of length-2-arrays"),n=t[0],i=t[1],(r=null===n?this.rootNode:this.getNodeByKey(n))?(t=new k.Deferred,s.push(t),r.applyPatch(i).always(A(t,r))):this.warn("could not find node with key '"+n+"'");return k.when.apply(k,s).promise()},clear:function(e){this._callHook("treeClear",this)},count:function(){return this.rootNode.countChildren()},debug:function(e){4<=this.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("log",arguments))},destroy:function(){this.widget.destroy()},enable:function(e){!1===e?this.widget.disable():this.widget.enable()},enableUpdate:function(e){return!!this._enableUpdate==!!(e=!1!==e)?e:((this._enableUpdate=e)?(this.debug("enableUpdate(true): redraw "),this._callHook("treeStructureChanged",this,"enableUpdate"),this.render()):this.debug("enableUpdate(false)..."),!e)},error:function(e){1<=this.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("error",arguments))},expandAll:function(t,n){var e=this.enableUpdate(!1);t=!1!==t,this.visit(function(e){!1!==e.hasChildren()&&e.isExpanded()!==t&&e.setExpanded(t,n)}),this.enableUpdate(e)},findAll:function(e){return this.rootNode.findAll(e)},findFirst:function(e){return this.rootNode.findFirst(e)},findNextNode:function(t,n){var i,r=null,e=this.getFirstChild();function o(e){if((r=t(e)?e:r)||e===n)return!1}return t="string"==typeof t?(i=new RegExp("^"+t,"i"),function(e){return i.test(e.title)}):t,this.visitRows(o,{start:n=n||e,includeSelf:!1}),r||n===e||this.visitRows(o,{start:e,includeSelf:!0}),r},findRelatedNode:function(e,t,n){var i=null,r=k.ui.keyCode;switch(t){case"parent":case r.BACKSPACE:e.parent&&e.parent.parent&&(i=e.parent);break;case"first":case r.HOME:this.visit(function(e){if(e.isVisible())return i=e,!1});break;case"last":case r.END:this.visit(function(e){e.isVisible()&&(i=e)});break;case"left":case r.LEFT:e.expanded?e.setExpanded(!1):e.parent&&e.parent.parent&&(i=e.parent);break;case"right":case r.RIGHT:e.expanded||!e.children&&!e.lazy?e.children&&e.children.length&&(i=e.children[0]):(e.setExpanded(),i=e);break;case"up":case r.UP:this.visitRows(function(e){return i=e,!1},{start:e,reverse:!0,includeSelf:!1});break;case"down":case r.DOWN:this.visitRows(function(e){return i=e,!1},{start:e,includeSelf:!1});break;default:this.tree.warn("Unknown relation '"+t+"'.")}return i},generateFormElements:function(e,t,n){n=n||{};var i="string"==typeof e?e:"ft_"+this._id+"[]",r="string"==typeof t?t:"ft_"+this._id+"_active",o="fancytree_result_"+this._id,s=k("#"+o),a=3===this.options.selectMode&&!1!==n.stopOnParents;function l(e){s.append(k("<input>",{type:"checkbox",name:i,value:e.key,checked:!0}))}s.length?s.empty():s=k("<div>",{id:o}).hide().insertAfter(this.$container),!1!==t&&this.activeNode&&s.append(k("<input>",{type:"radio",name:r,value:this.activeNode.key,checked:!0})),n.filter?this.visit(function(e){var t=n.filter(e);if("skip"===t)return t;!1!==t&&l(e)}):!1!==e&&(a=this.getSelectedNodes(a),k.each(a,function(e,t){l(t)}))},getActiveNode:function(){return this.activeNode},getFirstChild:function(){return this.rootNode.getFirstChild()},getFocusNode:function(){return this.focusNode},getOption:function(e){return this.widget.option(e)},getNodeByKey:function(t,e){var n,i;return!e&&(n=document.getElementById(this.options.idPrefix+t))?n.ftnode||null:(e=e||this.rootNode,t=""+t,e.visit(function(e){if(e.key===t)return i=e,!1},!(i=null)),i)},getRootNode:function(){return this.rootNode},getSelectedNodes:function(e){return this.rootNode.getSelectedNodes(e)},hasFocus:function(){return!!this._hasFocus},info:function(e){3<=this.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("info",arguments))},isLoading:function(){var t=!1;return this.rootNode.visit(function(e){if(e._isLoading||e._requestId)return!(t=!0)},!0),t},loadKeyPath:function(e,t){var i,n,r,o=this,s=new k.Deferred,a=this.getRootNode(),l=this.options.keyPathSeparator,d=[],c=k.extend({},t);for("function"==typeof t?i=t:t&&t.callback&&(i=t.callback),c.callback=function(e,t,n){i&&i.call(e,t,n),s.notifyWith(e,[{node:t,status:n}])},null==c.matchKey&&(c.matchKey=function(e,t){return e.key===t}),N(e)||(e=[e]),n=0;n<e.length;n++)(r=e[n]).charAt(0)===l&&(r=r.substr(1)),d.push(r.split(l));return setTimeout(function(){o._loadKeyPathImpl(s,c,a,d).done(function(){s.resolve()})},0),s.promise()},_loadKeyPathImpl:function(e,o,t,n){var i,r,s,a,l,d,c,u,h,f,p=this;for(c={},r=0;r<n.length;r++)for(h=n[r],u=t;h.length;){if(s=h.shift(),!(a=function(e,t){var n,i,r=e.children;if(r)for(n=0,i=r.length;n<i;n++)if(o.matchKey(r[n],t))return r[n];return null}(u,s))){this.warn("loadKeyPath: key not found: "+s+" (parent: "+u+")"),o.callback(this,s,"error");break}if(0===h.length){o.callback(this,a,"ok");break}if(a.lazy&&void 0===a.hasChildren()){o.callback(this,a,"loaded"),c[s=a.key]?c[s].pathSegList.push(h):c[s]={parent:a,pathSegList:[h]};break}o.callback(this,a,"loaded"),u=a}for(l in i=[],c)C(c,l)&&(d=c[l],f=new k.Deferred,i.push(f),function(t,n,e){o.callback(p,n,"loading"),n.load().done(function(){p._loadKeyPathImpl.call(p,t,o,n,e).always(A(t,p))}).fail(function(e){p.warn("loadKeyPath: error loading lazy "+n),o.callback(p,a,"error"),t.rejectWith(p)})}(f,d.parent,d.pathSegList));return k.when.apply(k,i).promise()},reactivate:function(e){var t,n=this.activeNode;return n?(this.activeNode=null,t=n.setActive(!0,{noFocus:!0}),e&&n.setFocus(),t):T()},reload:function(e){return this._callHook("treeClear",this),this._callHook("treeLoad",this,e)},render:function(e,t){return this.rootNode.render(e,t)},selectAll:function(t){this.visit(function(e){e.setSelected(t)})},setFocus:function(e){return this._callHook("treeSetFocus",this,e)},setOption:function(e,t){return this.widget.option(e,t)},debugTime:function(e){4<=this.options.debugLevel&&window.console.time(this+" - "+e)},debugTimeEnd:function(e){4<=this.options.debugLevel&&window.console.timeEnd(this+" - "+e)},toDict:function(e,t){t=this.rootNode.toDict(!0,t);return e?t:t.children},toString:function(){return"Fancytree@"+this._id},_triggerNodeEvent:function(e,t,n,i){i=this._makeHookContext(t,n,i),n=this.widget._trigger(e,n,i);return!1!==n&&void 0!==i.result?i.result:n},_triggerTreeEvent:function(e,t,n){n=this._makeHookContext(this,t,n),t=this.widget._trigger(e,t,n);return!1!==t&&void 0!==n.result?n.result:t},visit:function(e){return this.rootNode.visit(e,!1)},visitRows:function(t,e){if(!this.rootNode.hasChildren())return!1;if(e&&e.reverse)return delete e.reverse,this._visitRowsUp(t,e);for(var n,i,r,o=0,s=!1===(e=e||{}).includeSelf,a=!!e.includeHidden,l=!a&&this.enableFilter,d=e.start||this.rootNode.children[0],c=d.parent;c;){for(_(0<=(i=(r=c.children).indexOf(d)+o),"Could not find "+d+" in parent's children: "+c),n=i;n<r.length;n++)if(d=r[n],!l||d.match||d.subMatchCount){if(!s&&!1===t(d))return!1;if(s=!1,d.children&&d.children.length&&(a||d.expanded)&&!1===d.visit(function(e){return!l||e.match||e.subMatchCount?!1!==t(e)&&(a||!e.children||e.expanded?void 0:"skip"):"skip"},!1))return!1}c=(d=c).parent,o=1}return!0},_visitRowsUp:function(e,t){for(var n,i,r,o=!!t.includeHidden,s=t.start||this.rootNode.children[0];;){if((n=(r=s.parent).children)[0]===s){if(!(s=r).parent)break;n=r.children}else for(i=n.indexOf(s),s=n[i-1];(o||s.expanded)&&s.children&&s.children.length;)s=(n=(r=s).children)[n.length-1];if((o||s.isVisible())&&!1===e(s))return!1}},warn:function(e){2<=this.options.debugLevel&&(Array.prototype.unshift.call(arguments,this.toString()),d("warn",arguments))}},k.extend(F.prototype,{nodeClick:function(e){var t,n,i=e.targetType,r=e.node;if("expander"===i)r.isLoading()?r.debug("Got 2nd click while loading: ignored"):this._callHook("nodeToggleExpanded",e);else if("checkbox"===i)this._callHook("nodeToggleSelected",e),e.options.focusOnSelect&&this._callHook("nodeSetFocus",e,!0);else{if(t=!(n=!1),r.folder)switch(e.options.clickFolderMode){case 2:t=!(n=!0);break;case 3:n=t=!0}t&&(this.nodeSetFocus(e),this._callHook("nodeSetActive",e,!0)),n&&this._callHook("nodeToggleExpanded",e)}},nodeCollapseSiblings:function(e,t){var n,i,r,o=e.node;if(o.parent)for(i=0,r=(n=o.parent.children).length;i<r;i++)n[i]!==o&&n[i].expanded&&this._callHook("nodeSetExpanded",n[i],!1,t)},nodeDblclick:function(e){"title"===e.targetType&&4===e.options.clickFolderMode&&this._callHook("nodeToggleExpanded",e),"title"===e.targetType&&e.originalEvent.preventDefault()},nodeKeydown:function(e){var t=e.originalEvent,n=e.node,i=e.tree,r=e.options,o=t.which,s=t.key||String.fromCharCode(o),a=!!(t.altKey||t.ctrlKey||t.metaKey),l=!g[o]&&!u[o]&&!a,o=k(t.target),d=!0,c=!(t.ctrlKey||!r.autoActivate);if(n||(a=this.getActiveNode()||this.getFirstChild())&&(a.setFocus(),(n=e.node=this.focusNode).debug("Keydown force focus on active node")),r.quicksearch&&l&&!o.is(":input:enabled"))return 500<(o=Date.now())-i.lastQuicksearchTime&&(i.lastQuicksearchTerm=""),i.lastQuicksearchTime=o,i.lastQuicksearchTerm+=s,(s=i.findNextNode(i.lastQuicksearchTerm,i.getActiveNode()))&&s.setActive(),void t.preventDefault();switch(h.eventToString(t)){case"+":case"=":i.nodeSetExpanded(e,!0);break;case"-":i.nodeSetExpanded(e,!1);break;case"space":n.isPagingNode()?i._triggerNodeEvent("clickPaging",e,t):h.evalOption("checkbox",n,n,r,!1)?i.nodeToggleSelected(e):i.nodeSetActive(e,!0);break;case"return":i.nodeSetActive(e,!0);break;case"home":case"end":case"backspace":case"left":case"right":case"up":case"down":n.navigate(t.which,c);break;default:d=!1}d&&t.preventDefault()},nodeLoadChildren:function(o,s){var t,n,a,e=null,i=!0,l=o.tree,d=o.node,c=d.parent,r="nodeLoadChildren",u=Date.now();return w(s)&&_(!w(s=s.call(l,{type:"source"},o)),"source callback must not return another function"),w(s.then)?e=s:s.url?e=(t=k.extend({},o.options.ajax,s)).debugDelay?(n=t.debugDelay,delete t.debugDelay,N(n)&&(n=n[0]+Math.random()*(n[1]-n[0])),d.warn("nodeLoadChildren waiting debugDelay "+Math.round(n)+" ms ..."),k.Deferred(function(e){setTimeout(function(){k.ajax(t).done(function(){e.resolveWith(this,arguments)}).fail(function(){e.rejectWith(this,arguments)})},n)})):k.ajax(t):k.isPlainObject(s)||N(s)?i=!(e={then:function(e,t){e(s,null,null)}}):k.error("Invalid source type: "+s),d._requestId&&(d.warn("Recursive load request #"+u+" while #"+d._requestId+" is pending."),d._requestId=u),i&&(l.debugTime(r),l.nodeSetStatus(o,"loading")),a=new k.Deferred,e.then(function(e,t,n){var i,r;if("json"!==s.dataType&&"jsonp"!==s.dataType||"string"!=typeof e||k.error("Ajax request returned a string (did you get the JSON dataType wrong?)."),d._requestId&&d._requestId>u)a.rejectWith(this,[f]);else if(null!==d.parent||null===c){if(o.options.postProcess){try{(r=l._triggerNodeEvent("postProcess",o,o.originalEvent,{response:e,error:null,dataType:s.dataType})).error&&l.warn("postProcess returned error:",r)}catch(e){r={error:e,message:""+e,details:"postProcess failed"}}if(r.error)return i=k.isPlainObject(r.error)?r.error:{message:r.error},i=l._makeHookContext(d,null,i),void a.rejectWith(this,[i]);(N(r)||k.isPlainObject(r)&&N(r.children))&&(e=r)}else e&&C(e,"d")&&o.options.enableAspx&&(42===o.options.enableAspx&&l.warn("The default for enableAspx will change to `false` in the fututure. Pass `enableAspx: true` or implement postProcess to silence this warning."),e="string"==typeof e.d?k.parseJSON(e.d):e.d);a.resolveWith(this,[e])}else a.rejectWith(this,[p])},function(e,t,n){n=l._makeHookContext(d,null,{error:e,args:Array.prototype.slice.call(arguments),message:n,details:e.status+": "+n});a.rejectWith(this,[n])}),a.done(function(e){var t,n,i;l.nodeSetStatus(o,"ok"),k.isPlainObject(e)?(_(d.isRootNode(),"source may only be an object for root nodes (expecting an array of child objects otherwise)"),_(N(e.children),"if an object is passed as source, it must contain a 'children' array (all other properties are added to 'tree.data')"),t=(n=e).children,delete n.children,k.each(b,function(e,t){void 0!==n[t]&&(l[t]=n[t],delete n[t])}),k.extend(l.data,n)):t=e,_(N(t),"expected array of children"),d._setChildren(t),l.options.nodata&&0===t.length&&(w(l.options.nodata)?i=l.options.nodata.call(l,{type:"nodata"},o):!0===l.options.nodata&&d.isRootNode()?i=l.options.strings.noData:"string"==typeof l.options.nodata&&d.isRootNode()&&(i=l.options.nodata),i&&d.setStatus("nodata",i)),l._triggerNodeEvent("loadChildren",d)}).fail(function(e){var t;e!==f?e!==p?(e.node&&e.error&&e.message?t=e:"[object Object]"===(t=l._makeHookContext(d,null,{error:e,args:Array.prototype.slice.call(arguments),message:e?e.message||e.toString():""})).message&&(t.message=""),d.warn("Load children failed ("+t.message+")",t),!1!==l._triggerNodeEvent("loadError",t,null)&&l.nodeSetStatus(o,"error",t.message,t.details)):d.warn("Lazy parent node was removed while loading: discarding response."):d.warn("Ignored response for obsolete load request #"+u+" (expected #"+d._requestId+")")}).always(function(){d._requestId=null,i&&l.debugTimeEnd(r)}),a.promise()},nodeLoadKeyPath:function(e,t){},nodeRemoveChild:function(e,t){var n=e.node,i=k.extend({},e,{node:t}),r=n.children;if(1===r.length)return _(t===r[0],"invalid single child"),this.nodeRemoveChildren(e);this.activeNode&&(t===this.activeNode||this.activeNode.isDescendantOf(t))&&this.activeNode.setActive(!1),this.focusNode&&(t===this.focusNode||this.focusNode.isDescendantOf(t))&&(this.focusNode=null),this.nodeRemoveMarkup(i),this.nodeRemoveChildren(i),_(0<=(i=k.inArray(t,r)),"invalid child"),n.triggerModifyChild("remove",t),t.visit(function(e){e.parent=null},!0),this._callHook("treeRegisterNode",this,!1,t),r.splice(i,1)},nodeRemoveChildMarkup:function(e){e=e.node;e.ul&&(e.isRootNode()?k(e.ul).empty():(k(e.ul).remove(),e.ul=null),e.visit(function(e){e.li=e.ul=null}))},nodeRemoveChildren:function(e){var t=e.tree,n=e.node;n.children&&(this.activeNode&&this.activeNode.isDescendantOf(n)&&this.activeNode.setActive(!1),this.focusNode&&this.focusNode.isDescendantOf(n)&&(this.focusNode=null),this.nodeRemoveChildMarkup(e),n.triggerModifyChild("remove",null),n.visit(function(e){e.parent=null,t._callHook("treeRegisterNode",t,!1,e)}),n.lazy?n.children=[]:n.children=null,n.isRootNode()||(n.expanded=!1),this.nodeRenderStatus(e))},nodeRemoveMarkup:function(e){var t=e.node;t.li&&(k(t.li).remove(),t.li=null),this.nodeRemoveChildMarkup(e)},nodeRender:function(e,t,n,i,r){var o,s,a,l,d,c,u,h=e.node,f=e.tree,p=e.options,g=p.aria,v=!1,y=h.parent,b=!y,m=h.children,x=null;if(!1!==f._enableUpdate&&(b||y.ul)){if(_(b||y.ul,"parent UL must exist"),b||(h.li&&(t||h.li.parentNode!==h.parent.ul)&&(h.li.parentNode===h.parent.ul?x=h.li.nextSibling:this.debug("Unlinking "+h+" (must be child of "+h.parent+")"),this.nodeRemoveMarkup(e)),h.li?this.nodeRenderStatus(e):(v=!0,h.li=document.createElement("li"),(h.li.ftnode=h).key&&p.generateIds&&(h.li.id=p.idPrefix+h.key),h.span=document.createElement("span"),h.span.className="fancytree-node",g&&!h.tr&&k(h.li).attr("role","treeitem"),h.li.appendChild(h.span),this.nodeRenderTitle(e),p.createNode&&p.createNode.call(f,{type:"createNode"},e)),p.renderNode&&p.renderNode.call(f,{type:"renderNode"},e)),m){if(b||h.expanded||!0===n){for(h.ul||(h.ul=document.createElement("ul"),(!0!==i||r)&&h.expanded||(h.ul.style.display="none"),g&&k(h.ul).attr("role","group"),h.li?h.li.appendChild(h.ul):h.tree.$div.append(h.ul)),l=0,d=m.length;l<d;l++)u=k.extend({},e,{node:m[l]}),this.nodeRender(u,t,n,!1,!0);for(o=h.ul.firstChild;o;)o=(a=o.ftnode)&&a.parent!==h?(h.debug("_fixParent: remove missing "+a,o),c=o.nextSibling,o.parentNode.removeChild(o),c):o.nextSibling;for(o=h.ul.firstChild,l=0,d=m.length-1;l<d;l++)(s=m[l])===(a=o.ftnode)?o=o.nextSibling:h.ul.insertBefore(s.li,a.li)}}else h.ul&&(this.warn("remove child markup for "+h),this.nodeRemoveChildMarkup(e));b||v&&y.ul.insertBefore(h.li,x)}},nodeRenderTitle:function(e,t){var n,i,r=e.node,o=e.tree,s=e.options,a=s.aria,l=r.getLevel(),d=[];void 0!==t&&(r.title=t),r.span&&!1!==o._enableUpdate&&(t=a&&!1!==r.hasChildren()?" role='button'":"",l<s.minExpandLevel?(r.lazy||(r.expanded=!0),1<l&&d.push("<span "+t+" class='fancytree-expander fancytree-expander-fixed'></span>")):d.push("<span "+t+" class='fancytree-expander'></span>"),(l=h.evalOption("checkbox",r,r,s,!1))&&!r.isStatusNode()&&(n="fancytree-checkbox",("radio"===l||r.parent&&r.parent.radiogroup)&&(n+=" fancytree-radio"),d.push("<span "+(t=a?" role='checkbox'":"")+" class='"+n+"'></span>")),void 0!==r.data.iconClass&&(r.icon?k.error("'iconClass' node option is deprecated since v2.14.0: use 'icon' only instead"):(r.warn("'iconClass' node option is deprecated since v2.14.0: use 'icon' instead"),r.icon=r.data.iconClass)),!1!==(n=h.evalOption("icon",r,r,s,!0))&&(t=a?" role='presentation'":"",i=(i=h.evalOption("iconTooltip",r,r,s,null))?" title='"+P(i)+"'":"","string"==typeof n?c.test(n)?(n="/"===n.charAt(0)?n:(s.imagePath||"")+n,d.push("<img src='"+n+"' class='fancytree-icon'"+i+" alt='' />")):d.push("<span "+t+" class='fancytree-custom-icon "+n+"'"+i+"></span>"):n.text?d.push("<span "+t+" class='fancytree-custom-icon "+(n.addClass||"")+"'"+i+">"+h.escapeHtml(n.text)+"</span>"):n.html?d.push("<span "+t+" class='fancytree-custom-icon "+(n.addClass||"")+"'"+i+">"+n.html+"</span>"):d.push("<span "+t+" class='fancytree-icon'"+i+"></span>")),t="",t=(t=s.renderTitle?s.renderTitle.call(o,{type:"renderTitle"},e)||"":t)||"<span class='fancytree-title'"+(i=(i=!0===(i=h.evalOption("tooltip",r,r,s,null))?r.title:i)?" title='"+P(i)+"'":"")+(s.titlesTabbable?" tabindex='0'":"")+">"+(s.escapeTitles?h.escapeHtml(r.title):r.title)+"</span>",d.push(t),r.span.innerHTML=d.join(""),this.nodeRenderStatus(e),s.enhanceTitle&&(e.$title=k(">span.fancytree-title",r.span),t=s.enhanceTitle.call(o,{type:"enhanceTitle"},e)||""))},nodeRenderStatus:function(e){var t,n=e.node,i=e.tree,r=e.options,o=n.hasChildren(),s=n.isLastSibling(),a=r.aria,l=r._classNames,d=[],e=n[i.statusClassPropName];e&&!1!==i._enableUpdate&&(a&&(t=k(n.tr||n.li)),d.push(l.node),i.activeNode===n&&d.push(l.active),i.focusNode===n&&d.push(l.focused),n.expanded&&d.push(l.expanded),a&&(!1===o?t.removeAttr("aria-expanded"):t.attr("aria-expanded",Boolean(n.expanded))),n.folder&&d.push(l.folder),!1!==o&&d.push(l.hasChildren),s&&d.push(l.lastsib),n.lazy&&null==n.children&&d.push(l.lazy),n.partload&&d.push(l.partload),n.partsel&&d.push(l.partsel),h.evalOption("unselectable",n,n,r,!1)&&d.push(l.unselectable),n._isLoading&&d.push(l.loading),n._error&&d.push(l.error),n.statusNodeType&&d.push(l.statusNodePrefix+n.statusNodeType),n.selected?(d.push(l.selected),a&&t.attr("aria-selected",!0)):a&&t.attr("aria-selected",!1),n.extraClasses&&d.push(n.extraClasses),!1===o?d.push(l.combinedExpanderPrefix+"n"+(s?"l":"")):d.push(l.combinedExpanderPrefix+(n.expanded?"e":"c")+(n.lazy&&null==n.children?"d":"")+(s?"l":"")),d.push(l.combinedIconPrefix+(n.expanded?"e":"c")+(n.folder?"f":"")),e.className=d.join(" "),n.li&&k(n.li).toggleClass(l.lastsib,s))},nodeSetActive:function(e,t,n){var i=e.node,r=e.tree,o=e.options,s=!0===(n=n||{}).noEvents,a=!0===n.noFocus,n=!1!==n.scrollIntoView;return i===r.activeNode===(t=!1!==t)?T(i):(n&&e.originalEvent&&k(e.originalEvent.target).is("a,:checkbox")&&(i.info("Not scrolling while clicking an embedded link."),n=!1),t&&!s&&!1===this._triggerNodeEvent("beforeActivate",i,e.originalEvent)?L(i,["rejected"]):(t?(r.activeNode&&(_(r.activeNode!==i,"node was active (inconsistency)"),t=k.extend({},e,{node:r.activeNode}),r.nodeSetActive(t,!1),_(null===r.activeNode,"deactivate was out of sync?")),o.activeVisible&&i.makeVisible({scrollIntoView:n}),r.activeNode=i,r.nodeRenderStatus(e),a||r.nodeSetFocus(e),s||r._triggerNodeEvent("activate",i,e.originalEvent)):(_(r.activeNode===i,"node was not active (inconsistency)"),r.activeNode=null,this.nodeRenderStatus(e),s||e.tree._triggerNodeEvent("deactivate",i,e.originalEvent)),T(i)))},nodeSetExpanded:function(i,r,e){var t,n,o,s,a,l,d=i.node,c=i.tree,u=i.options,h=!0===(e=e||{}).noAnimation,f=!0===e.noEvents;if(r=!1!==r,k(d.li).hasClass(u._classNames.animating))return d.warn("setExpanded("+r+") while animating: ignored."),L(d,["recursion"]);if(d.expanded&&r||!d.expanded&&!r)return T(d);if(r&&!d.lazy&&!d.hasChildren())return T(d);if(!r&&d.getLevel()<u.minExpandLevel)return L(d,["locked"]);if(!f&&!1===this._triggerNodeEvent("beforeExpand",d,i.originalEvent))return L(d,["rejected"]);if(h||d.isVisible()||(h=e.noAnimation=!0),n=new k.Deferred,r&&!d.expanded&&u.autoCollapse){a=d.getParentList(!1,!0),l=u.autoCollapse;try{for(u.autoCollapse=!1,o=0,s=a.length;o<s;o++)this._callHook("nodeCollapseSiblings",a[o],e)}finally{u.autoCollapse=l}}return n.done(function(){var e=d.getLastChild();r&&u.autoScroll&&!h&&e&&c._enableUpdate?e.scrollIntoView(!0,{topNode:d}).always(function(){f||i.tree._triggerNodeEvent(r?"expand":"collapse",i)}):f||i.tree._triggerNodeEvent(r?"expand":"collapse",i)}),t=function(e){var t=u._classNames,n=u.toggleEffect;if(d.expanded=r,c._callHook("treeStructureChanged",i,r?"expand":"collapse"),c._callHook("nodeRender",i,!1,!1,!0),d.ul)if("none"!==d.ul.style.display==!!d.expanded)d.warn("nodeSetExpanded: UL.style.display already set");else{if(n&&!h)return k(d.li).addClass(t.animating),void(w(k(d.ul)[n.effect])?k(d.ul)[n.effect]({duration:n.duration,always:function(){k(this).removeClass(t.animating),k(d.li).removeClass(t.animating),e()}}):(k(d.ul).stop(!0,!0),k(d.ul).parent().find(".ui-effects-placeholder").remove(),k(d.ul).toggle(n.effect,n.options,n.duration,function(){k(this).removeClass(t.animating),k(d.li).removeClass(t.animating),e()})));d.ul.style.display=d.expanded||!parent?"":"none"}e()},r&&d.lazy&&void 0===d.hasChildren()?d.load().done(function(){n.notifyWith&&n.notifyWith(d,["loaded"]),t(function(){n.resolveWith(d)})}).fail(function(e){t(function(){n.rejectWith(d,["load failed ("+e+")"])})}):t(function(){n.resolveWith(d)}),n.promise()},nodeSetFocus:function(e,t){var n,i=e.tree,r=e.node,o=i.options,s=!!e.originalEvent&&k(e.originalEvent.target).is(":input");if(t=!1!==t,i.focusNode){if(i.focusNode===r&&t)return;n=k.extend({},e,{node:i.focusNode}),i.focusNode=null,this._triggerNodeEvent("blur",n),this._callHook("nodeRenderStatus",n)}t&&(this.hasFocus()||(r.debug("nodeSetFocus: forcing container focus"),this._callHook("treeSetFocus",e,!0,{calledByNode:!0})),r.makeVisible({scrollIntoView:!1}),i.focusNode=r,o.titlesTabbable&&(s||k(r.span).find(".fancytree-title").focus()),o.aria&&k(i.$container).attr("aria-activedescendant",k(r.tr||r.li).uniqueId().attr("id")),this._triggerNodeEvent("focus",e),document.activeElement===i.$container.get(0)||1<=k(document.activeElement,i.$container).length||k(i.$container).focus(),o.autoScroll&&r.scrollIntoView(),this._callHook("nodeRenderStatus",e))},nodeSetSelected:function(e,t,n){var i=e.node,r=e.tree,o=e.options,s=!0===(n=n||{}).noEvents,a=i.parent;if(t=!1!==t,!h.evalOption("unselectable",i,i,o,!1))return i._lastSelectIntent=t,!!i.selected!==t||3===o.selectMode&&i.partsel&&!t?s||!1!==this._triggerNodeEvent("beforeSelect",i,e.originalEvent)?(t&&1===o.selectMode?(r.lastSelectedNode&&r.lastSelectedNode.setSelected(!1),i.selected=t):3!==o.selectMode||!a||a.radiogroup||i.radiogroup?a&&a.radiogroup?i.visitSiblings(function(e){e._changeSelectStatusAttrs(t&&e===i)},!0):i.selected=t:(i.selected=t,i.fixSelection3AfterClick(n)),this.nodeRenderStatus(e),r.lastSelectedNode=t?i:null,void(s||r._triggerNodeEvent("select",e))):!!i.selected:t},nodeSetStatus:function(i,e,t,n){var r=i.node,o=i.tree;function s(e,t){var n=r.children?r.children[0]:null;return n&&n.isStatusNode()?(k.extend(n,e),n.statusNodeType=t,o._callHook("nodeRenderTitle",n)):(r._setChildren([e]),o._callHook("treeStructureChanged",i,"setStatusNode"),r.children[0].statusNodeType=t,o.render()),r.children[0]}switch(e){case"ok":!function(){var e=r.children?r.children[0]:null;if(e&&e.isStatusNode()){try{r.ul&&(r.ul.removeChild(e.li),e.li=null)}catch(e){}1===r.children.length?r.children=[]:r.children.shift(),o._callHook("treeStructureChanged",i,"clearStatusNode")}}(),r._isLoading=!1,r._error=null,r.renderStatus();break;case"loading":r.parent||s({title:o.options.strings.loading+(t?" ("+t+")":""),checkbox:!1,tooltip:n},e),r._isLoading=!0,r._error=null,r.renderStatus();break;case"error":s({title:o.options.strings.loadError+(t?" ("+t+")":""),checkbox:!1,tooltip:n},e),r._isLoading=!1,r._error={message:t,details:n},r.renderStatus();break;case"nodata":s({title:t||o.options.strings.noData,checkbox:!1,tooltip:n},e),r._isLoading=!1,r._error=null,r.renderStatus();break;default:k.error("invalid node status "+e)}},nodeToggleExpanded:function(e){return this.nodeSetExpanded(e,!e.node.expanded)},nodeToggleSelected:function(e){var t=e.node,n=!t.selected;return t.partsel&&!t.selected&&!0===t._lastSelectIntent&&(t.selected=!(n=!1)),t._lastSelectIntent=n,this.nodeSetSelected(e,n)},treeClear:function(e){var t=e.tree;t.activeNode=null,t.focusNode=null,t.$div.find(">ul.fancytree-container").empty(),t.rootNode.children=null,t._callHook("treeStructureChanged",e,"clear")},treeCreate:function(e){},treeDestroy:function(e){this.$div.find(">ul.fancytree-container").remove(),this.$source&&this.$source.removeClass("fancytree-helper-hidden")},treeInit:function(e){var n=e.tree,i=n.options;n.$container.attr("tabindex",i.tabindex),k.each(b,function(e,t){void 0!==i[t]&&(n.info("Move option "+t+" to tree"),n[t]=i[t],delete i[t])}),i.checkboxAutoHide&&n.$container.addClass("fancytree-checkbox-auto-hide"),i.rtl?n.$container.attr("DIR","RTL").addClass("fancytree-rtl"):n.$container.removeAttr("DIR").removeClass("fancytree-rtl"),i.aria&&(n.$container.attr("role","tree"),1!==i.selectMode&&n.$container.attr("aria-multiselectable",!0)),this.treeLoad(e)},treeLoad:function(e,t){var n,i,r,o=e.tree,s=e.widget.element,a=k.extend({},e,{node:this.rootNode});if(o.rootNode.children&&this.treeClear(e),t=t||this.options.source)"string"==typeof t&&k.error("Not implemented");else switch(i=s.data("type")||"html"){case"html":(r=s.find(">ul").not(".fancytree-container").first()).length?(r.addClass("ui-fancytree-source fancytree-helper-hidden"),t=k.ui.fancytree.parseHtml(r),this.data=k.extend(this.data,H(r))):(h.warn("No `source` option was passed and container does not contain `<ul>`: assuming `source: []`."),t=[]);break;case"json":t=k.parseJSON(s.text()),s.contents().filter(function(){return 3===this.nodeType}).remove(),k.isPlainObject(t)&&(_(N(t.children),"if an object is passed as source, it must contain a 'children' array (all other properties are added to 'tree.data')"),t=(n=t).children,delete n.children,k.each(b,function(e,t){void 0!==n[t]&&(o[t]=n[t],delete n[t])}),k.extend(o.data,n));break;default:k.error("Invalid data-type: "+i)}return o._triggerTreeEvent("preInit",null),this.nodeLoadChildren(a,t).done(function(){o._callHook("treeStructureChanged",e,"loadChildren"),o.render(),3===e.options.selectMode&&o.rootNode.fixSelection3FromEndNodes(),o.activeNode&&o.options.activeVisible&&o.activeNode.makeVisible(),o._triggerTreeEvent("init",null,{status:!0})}).fail(function(){o.render(),o._triggerTreeEvent("init",null,{status:!1})})},treeRegisterNode:function(e,t,n){e.tree._callHook("treeStructureChanged",e,t?"addNode":"removeNode")},treeSetFocus:function(e,t,n){var i;(t=!1!==t)!==this.hasFocus()&&(!(this._hasFocus=t)&&this.focusNode?this.focusNode.setFocus(!1):!t||n&&n.calledByNode||k(this.$container).focus(),this.$container.toggleClass("fancytree-treefocus",t),this._triggerTreeEvent(t?"focusTree":"blurTree"),t&&!this.activeNode&&(i=this._lastMousedownNode||this.getFirstChild())&&i.setFocus())},treeSetOption:function(e,t,n){var i=e.tree,r=!0,o=!1,s=!1;switch(t){case"aria":case"checkbox":case"icon":case"minExpandLevel":case"tabindex":s=o=!0;break;case"checkboxAutoHide":i.$container.toggleClass("fancytree-checkbox-auto-hide",!!n);break;case"escapeTitles":case"tooltip":s=!0;break;case"rtl":!1===n?i.$container.removeAttr("DIR").removeClass("fancytree-rtl"):i.$container.attr("DIR","RTL").addClass("fancytree-rtl"),s=!0;break;case"source":r=!1,i._callHook("treeLoad",i,n),s=!0}i.debug("set option "+t+"="+n+" <"+typeof n+">"),r&&(this.widget._super||k.Widget.prototype._setOption).call(this.widget,t,n),o&&i._callHook("treeCreate",i),s&&i.render(!0,!1)},treeStructureChanged:function(e,t){}}),k.widget("ui.fancytree",{options:{activeVisible:!0,ajax:{type:"GET",cache:!1,dataType:"json"},aria:!0,autoActivate:!0,autoCollapse:!1,autoScroll:!1,checkbox:!1,clickFolderMode:4,copyFunctionsToData:!1,debugLevel:null,disabled:!1,enableAspx:42,escapeTitles:!1,extensions:[],focusOnSelect:!1,generateIds:!1,icon:!0,idPrefix:"ft_",keyboard:!0,keyPathSeparator:"/",minExpandLevel:1,nodata:!0,quicksearch:!1,rtl:!1,scrollOfs:{top:0,bottom:0},scrollParent:null,selectMode:2,strings:{loading:"Loading...",loadError:"Load error!",moreData:"More...",noData:"No data."},tabindex:"0",titlesTabbable:!1,toggleEffect:{effect:"slideToggle",duration:200},tooltip:!1,treeId:null,_classNames:{active:"fancytree-active",animating:"fancytree-animating",combinedExpanderPrefix:"fancytree-exp-",combinedIconPrefix:"fancytree-ico-",error:"fancytree-error",expanded:"fancytree-expanded",focused:"fancytree-focused",folder:"fancytree-folder",hasChildren:"fancytree-has-children",lastsib:"fancytree-lastsib",lazy:"fancytree-lazy",loading:"fancytree-loading",node:"fancytree-node",partload:"fancytree-partload",partsel:"fancytree-partsel",radio:"fancytree-radio",selected:"fancytree-selected",statusNodePrefix:"fancytree-statusnode-",unselectable:"fancytree-unselectable"},lazyLoad:null,postProcess:null},_deprecationWarning:function(e){var t=this.tree;t&&3<=t.options.debugLevel&&t.warn("$().fancytree('"+e+"') is deprecated (see https://wwwendt.de/tech/fancytree/doc/jsdoc/Fancytree_Widget.html")},_create:function(){this.tree=new F(this),this.$source=this.source||"json"===this.element.data("type")?this.element:this.element.find(">ul").first();for(var e,t,n=this.options,i=n.extensions,r=(this.tree,0);r<i.length;r++)t=i[r],(e=k.ui.fancytree._extensions[t])||k.error("Could not apply extension '"+t+"' (it is not registered, did you forget to include it?)"),this.tree.options[t]=function e(t){var n,i,r,o,s=t||{},a=1,l=arguments.length;if("object"==typeof s||w(s)||(s={}),a===l)throw Error("need at least two args");for(;a<l;a++)if(null!=(n=arguments[a]))for(i in n)C(n,i)&&(o=s[i],s!==(r=n[i])&&(r&&k.isPlainObject(r)?(o=o&&k.isPlainObject(o)?o:{},s[i]=e(o,r)):void 0!==r&&(s[i]=r)));return s}({},e.options,this.tree.options[t]),_(void 0===this.tree.ext[t],"Extension name must not exist as Fancytree.ext attribute: '"+t+"'"),this.tree.ext[t]={},function(e,t,n){for(var i in t)"function"==typeof t[i]?"function"==typeof e[i]?e[i]=E(i,e,0,t,n):"_"===i.charAt(0)?e.ext[n][i]=E(i,e,0,t,n):k.error("Could not override tree."+i+". Use prefix '_' to create tree."+n+"._"+i):"options"!==i&&(e.ext[n][i]=t[i])}(this.tree,e,t),0;void 0!==n.icons&&(!0===n.icon?(this.tree.warn("'icons' tree option is deprecated since v2.14.0: use 'icon' instead"),n.icon=n.icons):k.error("'icons' tree option is deprecated since v2.14.0: use 'icon' only instead")),void 0!==n.iconClass&&(n.icon?k.error("'iconClass' tree option is deprecated since v2.14.0: use 'icon' only instead"):(this.tree.warn("'iconClass' tree option is deprecated since v2.14.0: use 'icon' instead"),n.icon=n.iconClass)),void 0!==n.tabbable&&(n.tabindex=n.tabbable?"0":"-1",this.tree.warn("'tabbable' tree option is deprecated since v2.17.0: use 'tabindex='"+n.tabindex+"' instead")),this.tree._callHook("treeCreate",this.tree)},_init:function(){this.tree._callHook("treeInit",this.tree),this._bind()},_setOption:function(e,t){return this.tree._callHook("treeSetOption",this.tree,e,t)},_destroy:function(){this._unbind(),this.tree._callHook("treeDestroy",this.tree)},_unbind:function(){var e=this.tree._ns;this.element.off(e),this.tree.$container.off(e),k(document).off(e)},_bind:function(){var s=this,a=this.options,o=this.tree,e=o._ns;this._unbind(),o.$container.on("focusin"+e+" focusout"+e,function(e){var t=h.getNode(e),n="focusin"===e.type;if(!n&&t&&k(e.target).is("a"))t.debug("Ignored focusout on embedded <a> element.");else{if(n){if(o._getExpiringValue("focusin"))return void o.debug("Ignored double focusin.");o._setExpiringValue("focusin",!0,50),t||(t=o._getExpiringValue("mouseDownNode"))&&o.debug("Reconstruct mouse target for focusin from recent event.")}t?o._callHook("nodeSetFocus",o._makeHookContext(t,e),n):o.tbody&&k(e.target).parents("table.fancytree-container > thead").length?o.debug("Ignore focus event outside table body.",e):o._callHook("treeSetFocus",o,n)}}).on("selectstart"+e,"span.fancytree-title",function(e){e.preventDefault()}).on("keydown"+e,function(e){if(a.disabled||!1===a.keyboard)return!0;var t,n=o.focusNode,i=o._makeHookContext(n||o,e),r=o.phase;try{return o.phase="userEvent","preventNav"===(t=n?o._triggerNodeEvent("keydown",n,e):o._triggerTreeEvent("keydown",e))?t=!0:!1!==t&&(t=o._callHook("nodeKeydown",i)),t}finally{o.phase=r}}).on("mousedown"+e,function(e){e=h.getEventTarget(e);o._lastMousedownNode=e?e.node:null,o._setExpiringValue("mouseDownNode",o._lastMousedownNode)}).on("click"+e+" dblclick"+e,function(e){if(a.disabled)return!0;var t,n=h.getEventTarget(e),i=n.node,r=s.tree,o=r.phase;if(!i)return!0;t=r._makeHookContext(i,e);try{switch(r.phase="userEvent",e.type){case"click":return t.targetType=n.type,i.isPagingNode()?!0===r._triggerNodeEvent("clickPaging",t,e):!1!==r._triggerNodeEvent("click",t,e)&&r._callHook("nodeClick",t);case"dblclick":return t.targetType=n.type,!1!==r._triggerNodeEvent("dblclick",t,e)&&r._callHook("nodeDblclick",t)}}finally{r.phase=o}})},getActiveNode:function(){return this._deprecationWarning("getActiveNode"),this.tree.activeNode},getNodeByKey:function(e){return this._deprecationWarning("getNodeByKey"),this.tree.getNodeByKey(e)},getRootNode:function(){return this._deprecationWarning("getRootNode"),this.tree.rootNode},getTree:function(){return this._deprecationWarning("getTree"),this.tree}}),h=k.ui.fancytree,k.extend(k.ui.fancytree,{version:"2.38.3",buildType: "production",debugLevel: 3,_nextId:1,_nextNodeKey:1,_extensions:{},_FancytreeClass:F,_FancytreeNodeClass:I,jquerySupports:{positionMyOfs:function(e){for(var t,n,i=k.map(S(e).split("."),function(e){return parseInt(e,10)}),r=k.map(Array.prototype.slice.call(arguments,1),function(e){return parseInt(e,10)}),o=0;o<r.length;o++)if((t=i[o]||0)!==(n=r[o]||0))return n<t;return!0}(k.ui.version,1,9)},assert:_,createTree:function(e,t){t=k(e).fancytree(t);return h.getTree(t)},debounce:function(t,n,i,r){var o;return 3===arguments.length&&"boolean"!=typeof i&&(r=i,i=!1),function(){var e=arguments;r=r||this,i&&!o&&n.apply(r,e),clearTimeout(o),o=setTimeout(function(){i||n.apply(r,e),o=null},t)}},debug:function(e){4<=k.ui.fancytree.debugLevel&&d("log",arguments)},error:function(e){1<=k.ui.fancytree.debugLevel&&d("error",arguments)},escapeHtml:function(e){return(""+e).replace(t,function(e){return i[e]})},fixPositionOptions:function(e){var t,n,i,r;return(e.offset||0<=(""+e.my+e.at).indexOf("%"))&&k.error("expected new position syntax (but '%' is not supported)"),k.ui.fancytree.jquerySupports.positionMyOfs||(t=/(\w+)([+-]?\d+)?\s+(\w+)([+-]?\d+)?/.exec(e.my),n=/(\w+)([+-]?\d+)?\s+(\w+)([+-]?\d+)?/.exec(e.at),i=(t[2]?+t[2]:0)+(n[2]?+n[2]:0),r=(t[4]?+t[4]:0)+(n[4]?+n[4]:0),e=k.extend({},e,{my:t[1]+" "+t[3],at:n[1]+" "+n[3]}),(i||r)&&(e.offset=i+" "+r)),e},getEventTarget:function(e){var t=e&&e.target?e.target.className:"",n={node:this.getNode(e.target),type:void 0};return/\bfancytree-title\b/.test(t)?n.type="title":/\bfancytree-expander\b/.test(t)?n.type=!1===n.node.hasChildren()?"prefix":"expander":/\bfancytree-checkbox\b/.test(t)?n.type="checkbox":/\bfancytree(-custom)?-icon\b/.test(t)?n.type="icon":/\bfancytree-node\b/.test(t)?n.type="title":e&&e.target&&((e=k(e.target)).is("ul[role=group]")?((n.node&&n.node.tree||h).debug("Ignoring click on outer UL."),n.node=null):e.closest(".fancytree-title").length?n.type="title":e.closest(".fancytree-checkbox").length?n.type="checkbox":e.closest(".fancytree-expander").length&&(n.type="expander")),n},getEventTargetType:function(e){return this.getEventTarget(e).type},getNode:function(e){if(e instanceof I)return e;for(e instanceof k?e=e[0]:void 0!==e.originalEvent&&(e=e.target);e;){if(e.ftnode)return e.ftnode;e=e.parentNode}return null},getTree:function(e){var t=e;return e instanceof F?e:("number"==typeof(e=void 0===e?0:e)?e=k(".fancytree-container").eq(e):"string"==typeof e?(e=k("#ft-id-"+t).eq(0)).length||(e=k(t).eq(0)):e instanceof Element||e instanceof HTMLDocument?e=k(e):e instanceof k?e=e.eq(0):void 0!==e.originalEvent&&(e=k(e.target)),(e=(e=e.closest(":ui-fancytree")).data("ui-fancytree")||e.data("fancytree"))?e.tree:null)},evalOption:function(e,t,n,i,r){var o,s=t.tree,i=i[e],n=n[e];return w(i)?(o={node:t,tree:s,widget:s.widget,options:s.widget.options,typeInfo:s.types[t.type]||{}},null==(o=i.call(s,{type:e},o))&&(o=n)):o=null==n?i:n,o=null==o?r:o},setSpanIcon:function(e,t,n){var i=k(e);"string"==typeof n?i.attr("class",t+" "+n):(n.text?i.text(""+n.text):n.html&&(e.innerHTML=n.html),i.attr("class",t+" "+(n.addClass||"")))},eventToString:function(e){var t=e.which,n=e.type,i=[];return e.altKey&&i.push("alt"),e.ctrlKey&&i.push("ctrl"),e.metaKey&&i.push("meta"),e.shiftKey&&i.push("shift"),"click"===n||"dblclick"===n?i.push(o[e.button]+n):"wheel"===n?i.push(n):r[t]||i.push(u[t]||String.fromCharCode(t).toLowerCase()),i.join("+")},info:function(e){3<=k.ui.fancytree.debugLevel&&d("info",arguments)},keyEventToString:function(e){return this.warn("keyEventToString() is deprecated: use eventToString()"),this.eventToString(e)},overrideMethod:function(e,t,n,i){var r,o=e[t]||k.noop;e[t]=function(){var e=i||this;try{return r=e._super,e._super=o,n.apply(e,arguments)}finally{e._super=r}}},parseHtml:function(s){var a,l,d,c,u,h,f,p,e=s.find(">li"),g=[];return e.each(function(){var e,t,n=k(this),i=n.find(">span",this).first(),r=i.length?null:n.find(">a").first(),o={tooltip:null,data:{}};for(i.length?o.title=i.html():r&&r.length?(o.title=r.html(),o.data.href=r.attr("href"),o.data.target=r.attr("target"),o.tooltip=r.attr("title")):(o.title=n.html(),0<=(u=o.title.search(/<ul/i))&&(o.title=o.title.substring(0,u))),o.title=S(o.title),c=0,h=v.length;c<h;c++)o[v[c]]=void 0;for(a=this.className.split(" "),d=[],c=0,h=a.length;c<h;c++)l=a[c],y[l]?o[l]=!0:d.push(l);if(o.extraClasses=d.join(" "),(f=n.attr("title"))&&(o.tooltip=f),(f=n.attr("id"))&&(o.key=f),n.attr("hideCheckbox")&&(o.checkbox=!1),(e=H(n))&&!k.isEmptyObject(e)){for(t in x)C(e,t)&&(e[x[t]]=e[t],delete e[t]);for(c=0,h=m.length;c<h;c++)f=m[c],null!=(p=e[f])&&(delete e[f],o[f]=p);k.extend(o.data,e)}(s=n.find(">ul").first()).length?o.children=k.ui.fancytree.parseHtml(s):o.children=o.lazy?void 0:null,g.push(o)}),g},registerExtension:function(e){_(null!=e.name,"extensions must have a `name` property."),_(null!=e.version,"extensions must have a `version` property."),k.ui.fancytree._extensions[e.name]=e},trim:S,unescapeHtml:function(e){var t=document.createElement("div");return t.innerHTML=e,0===t.childNodes.length?"":t.childNodes[0].nodeValue},warn:function(e){2<=k.ui.fancytree.debugLevel&&d("warn",arguments)}}),k.ui.fancytree}function _(e,t){e||(k.ui.fancytree.error(t="Fancytree assertion failed"+(t=t?": "+t:"")),k.error(t))}function C(e,t){return Object.prototype.hasOwnProperty.call(e,t)}function w(e){return"function"==typeof e}function S(e){return null==e?"":e.trim()}function d(t,n){var i,r,t=window.console?window.console[t]:null;if(t)try{t.apply(window.console,n)}catch(e){for(r="",i=0;i<n.length;i++)r+=n[i];t(r)}}function E(e,i,t,n,r){var o,s,a;function l(){return o.apply(i,arguments)}function d(e){return o.apply(i,e)}return o=i[e],s=n[e],a=i.ext[r],function(){var e=i._local,t=i._super,n=i._superApply;try{return i._local=a,i._super=l,i._superApply=d,s.apply(i,arguments)}finally{i._local=e,i._super=t,i._superApply=n}}}function T(e,t){return(void 0===e?k.Deferred(function(){this.resolve()}):k.Deferred(function(){this.resolveWith(e,t)})).promise()}function L(e,t){return(void 0===e?k.Deferred(function(){this.reject()}):k.Deferred(function(){this.rejectWith(e,t)})).promise()}function A(e,t){return function(){e.resolveWith(t)}}function H(e){var t=k.extend({},e.data()),e=t.json;return delete t.fancytree,delete t.uiFancytree,e&&(delete t.json,t=k.extend(t,e)),t}function P(e){return(""+e).replace(n,function(e){return i[e]})}function R(t){return t=t.toLowerCase(),function(e){return 0<=e.title.toLowerCase().indexOf(t)}}function I(e,t){var n,i,r;for(this.parent=e,this.tree=e.tree,this.ul=null,this.li=null,this.statusNodeType=null,this._isLoading=!1,this._error=null,this.data={},n=0,i=m.length;n<i;n++)this[r=m[n]]=t[r];for(r in null==this.unselectableIgnore&&null==this.unselectableStatus||(this.unselectable=!0),t.hideCheckbox&&k.error("'hideCheckbox' node option was removed in v2.23.0: use 'checkbox: false'"),t.data&&k.extend(this.data,t.data),t)s[r]||!this.tree.options.copyFunctionsToData&&w(t[r])||a[r]||(this.data[r]=t[r]);null==this.key?this.tree.options.defaultKey?(this.key=""+this.tree.options.defaultKey(this),_(this.key,"defaultKey() must return a unique key")):this.key="_"+h._nextNodeKey++:this.key=""+this.key,t.active&&(_(null===this.tree.activeNode,"only one active node allowed"),this.tree.activeNode=this),t.selected&&(this.tree.lastSelectedNode=this),(e=t.children)?e.length?this._setChildren(e):this.children=this.lazy?[]:null:this.children=null,this.tree._callHook("treeRegisterNode",this.tree,!0,this)}function F(e){this.widget=e,this.$div=e.element,this.options=e.options,this.options&&(void 0!==this.options.lazyload&&k.error("The 'lazyload' event is deprecated since 2014-02-25. Use 'lazyLoad' (with uppercase L) instead."),void 0!==this.options.loaderror&&k.error("The 'loaderror' event was renamed since 2014-07-03. Use 'loadError' (with uppercase E) instead."),void 0!==this.options.fx&&k.error("The 'fx' option was replaced by 'toggleEffect' since 2014-11-30."),void 0!==this.options.removeNode&&k.error("The 'removeNode' event was replaced by 'modifyChild' since 2.20 (2016-09-10).")),this.ext={},this.types={},this.columns={},this.data=H(this.$div),this._id=""+(this.options.treeId||k.ui.fancytree._nextId++),this._ns=".fancytree-"+this._id,this.activeNode=null,this.focusNode=null,this._hasFocus=null,this._tempCache={},this._lastMousedownNode=null,this._enableUpdate=!0,this.lastSelectedNode=null,this.systemFocusElement=null,this.lastQuicksearchTerm="",this.lastQuicksearchTime=0,this.viewport=null,this.statusClassPropName="span",this.ariaPropName="li",this.nodeContainerAttrName="li",this.$div.find(">ul.fancytree-container").remove(),this.rootNode=new I({tree:this},{title:"root",key:"root_"+this._id,children:null,expanded:!0}),this.rootNode.parent=null,e=k("<ul>",{id:"ft-id-"+this._id,class:"ui-fancytree fancytree-container fancytree-plain"}).appendTo(this.$div),this.$container=e,this.rootNode.ul=e[0],null==this.options.debugLevel&&(this.options.debugLevel=h.debugLevel)}k.ui.fancytree.warn("Fancytree: ignored duplicate include")});

/*! Extension 'jquery.fancytree.childcounter.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(i){"use strict";return i.ui.fancytree._FancytreeClass.prototype.countSelected=function(e){this.options;return this.getSelectedNodes(e).length},i.ui.fancytree._FancytreeNodeClass.prototype.updateCounters=function(){var e=this,n=i("span.fancytree-childcounter",e.span),t=e.tree.options.childcounter,o=e.countChildren(t.deep);!(e.data.childCounter=o)&&t.hideZeros||e.isExpanded()&&t.hideExpanded?n.remove():(n=!n.length?i("<span class='fancytree-childcounter'/>").appendTo(i("span.fancytree-icon,span.fancytree-custom-icon",e.span)):n).text(o),!t.deep||e.isTopLevel()||e.isRootNode()||e.parent.updateCounters()},i.ui.fancytree.prototype.widgetMethod1=function(e){this.tree;return e},i.ui.fancytree.registerExtension({name:"childcounter",version:"2.38.3",options:{deep:!0,hideZeros:!0,hideExpanded:!1},foo:42,_appendCounter:function(e){},treeInit:function(e){e.options,e.options.childcounter;this._superApply(arguments),this.$container.addClass("fancytree-ext-childcounter")},treeDestroy:function(e){this._superApply(arguments)},nodeRenderTitle:function(e,n){var t=e.node,o=e.options.childcounter,r=null==t.data.childCounter?t.countChildren(o.deep):+t.data.childCounter;this._super(e,n),!r&&o.hideZeros||t.isExpanded()&&o.hideExpanded||i("span.fancytree-icon,span.fancytree-custom-icon",t.span).append(i("<span class='fancytree-childcounter'/>").text(r))},nodeSetExpanded:function(e,n,t){var o=e.tree;e.node;return this._superApply(arguments).always(function(){o.nodeRenderTitle(e)})}}),i.ui.fancytree});

/*! Extension 'jquery.fancytree.clones.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(c){"use strict";var f=c.ui.fancytree.assert;function n(e,t,n){for(var r,s,i=3&e.length,o=e.length-i,l=n,a=3432918353,u=461845907,c=0;c<o;)s=255&e.charCodeAt(c)|(255&e.charCodeAt(++c))<<8|(255&e.charCodeAt(++c))<<16|(255&e.charCodeAt(++c))<<24,++c,l=27492+(65535&(r=5*(65535&(l=(l^=s=(65535&(s=(s=(65535&s)*a+(((s>>>16)*a&65535)<<16)&4294967295)<<15|s>>>17))*u+(((s>>>16)*u&65535)<<16)&4294967295)<<13|l>>>19))+((5*(l>>>16)&65535)<<16)&4294967295))+((58964+(r>>>16)&65535)<<16);switch(s=0,i){case 3:s^=(255&e.charCodeAt(c+2))<<16;case 2:s^=(255&e.charCodeAt(c+1))<<8;case 1:l^=s=(65535&(s=(s=(65535&(s^=255&e.charCodeAt(c)))*a+(((s>>>16)*a&65535)<<16)&4294967295)<<15|s>>>17))*u+(((s>>>16)*u&65535)<<16)&4294967295}return l^=e.length,l=2246822507*(65535&(l^=l>>>16))+((2246822507*(l>>>16)&65535)<<16)&4294967295,l=3266489909*(65535&(l^=l>>>13))+((3266489909*(l>>>16)&65535)<<16)&4294967295,l^=l>>>16,t?("0000000"+(l>>>0).toString(16)).substr(-8):l>>>0}return c.ui.fancytree._FancytreeNodeClass.prototype.getCloneList=function(e){var t,n=this.tree,r=n.refMap[this.refKey]||null,s=n.keyMap;return r&&(t=this.key,e?r=c.map(r,function(e){return s[e]}):(r=c.map(r,function(e){return e===t?null:s[e]})).length<1&&(r=null)),r},c.ui.fancytree._FancytreeNodeClass.prototype.isClone=function(){var e=this.refKey||null,e=e&&this.tree.refMap[e]||null;return!!(e&&1<e.length)},c.ui.fancytree._FancytreeNodeClass.prototype.reRegister=function(t,e){e=null==e?null:""+e;var n=this.tree,r=this.key,s=this.refKey,i=n.keyMap,o=n.refMap,l=o[s]||null,n=!1;return null!=(t=null==t?null:""+t)&&t!==this.key&&(i[t]&&c.error("[ext-clones] reRegister("+t+"): already exists: "+this),delete i[r],i[t]=this,l&&(o[s]=c.map(l,function(e){return e===r?t:e})),this.key=t,n=!0),null!=e&&e!==this.refKey&&(l&&(1===l.length?delete o[s]:o[s]=c.map(l,function(e){return e===r?null:e})),o[e]?o[e].append(t):o[e]=[this.key],this.refKey=e,n=!0),n},c.ui.fancytree._FancytreeNodeClass.prototype.setRefKey=function(e){return this.reRegister(null,e)},c.ui.fancytree._FancytreeClass.prototype.getNodesByRef=function(e,t){var n=this.keyMap,e=this.refMap[e]||null;return e=e&&(e=t?c.map(e,function(e){e=n[e];return e.isDescendantOf(t)?e:null}):c.map(e,function(e){return n[e]})).length<1?null:e},c.ui.fancytree._FancytreeClass.prototype.changeRefKey=function(e,t){var n,r=this.keyMap,s=this.refMap[e]||null;if(s){for(n=0;n<s.length;n++)r[s[n]].refKey=t;delete this.refMap[e],this.refMap[t]=s}},c.ui.fancytree.registerExtension({name:"clones",version:"2.38.3",options:{highlightActiveClones:!0,highlightClones:!1},treeCreate:function(e){this._superApply(arguments),e.tree.refMap={},e.tree.keyMap={}},treeInit:function(e){this.$container.addClass("fancytree-ext-clones"),f(null==e.options.defaultKey),e.options.defaultKey=function(e){return t=e,"id_"+(t=n(e=(e=c.map(e.getParentList(!1,!0),function(e){return e.refKey||e.key})).join("/"),!0))+n(t+e,!0);var t},this._superApply(arguments)},treeClear:function(e){return e.tree.refMap={},e.tree.keyMap={},this._superApply(arguments)},treeRegisterNode:function(e,t,n){var r,s,i=e.tree,o=i.keyMap,l=i.refMap,a=n.key,u=n&&null!=n.refKey?""+n.refKey:null;return n.isStatusNode()||(t?(null!=o[n.key]&&(s=o[n.key],s="clones.treeRegisterNode: duplicate key '"+n.key+"': /"+n.getPath(!0)+" => "+s.getPath(!0),i.error(s),c.error(s)),o[a]=n,u&&((r=l[u])?(r.push(a),2===r.length&&e.options.clones.highlightClones&&o[r[0]].renderStatus()):l[u]=[a])):(null==o[a]&&c.error("clones.treeRegisterNode: node.key not registered: "+n.key),delete o[a],u&&(r=l[u])&&((s=r.length)<=1?(f(1===s),f(r[0]===a),delete l[u]):(function(e,t){for(var n=e.length-1;0<=n;n--)if(e[n]===t)return e.splice(n,1)}(r,a),2===s&&e.options.clones.highlightClones&&o[r[0]].renderStatus())))),this._super(e,t,n)},nodeRenderStatus:function(e){var t,n=e.node,r=this._super(e);return e.options.clones.highlightClones&&(t=c(n[e.tree.statusClassPropName])).length&&n.isClone()&&t.addClass("fancytree-clone"),r},nodeSetActive:function(e,n,t){var r=e.tree.statusClassPropName,s=e.node,i=this._superApply(arguments);return e.options.clones.highlightActiveClones&&s.isClone()&&c.each(s.getCloneList(!0),function(e,t){c(t[r]).toggleClass("fancytree-active-clone",!1!==n)}),i}}),c.ui.fancytree});

/*! Extension 'jquery.fancytree.dnd.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","jquery-ui/ui/widgets/draggable","jquery-ui/ui/widgets/droppable","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(v){"use strict";var t=!1,g="fancytree-drop-accept",u="fancytree-drop-after",c="fancytree-drop-before",f="fancytree-drop-reject";function h(e){return 0===e?"":0<e?"+"+e:""+e}function r(e){var r=e.options.dnd||null,n=e.options.glyph||null;r&&(t||(v.ui.plugin.add("draggable","connectToFancytree",{start:function(e,r){var t=v(this).data("ui-draggable")||v(this).data("draggable"),a=r.helper.data("ftSourceNode")||null;if(a)return t.offset.click.top=-2,t.offset.click.left=16,a.tree.ext.dnd._onDragEvent("start",a,null,e,r,t)},drag:function(e,r){var t,a=v(this).data("ui-draggable")||v(this).data("draggable"),n=r.helper.data("ftSourceNode")||null,o=r.helper.data("ftTargetNode")||null,d=v.ui.fancytree.getNode(e.target),l=n&&n.tree.options.dnd;e.target&&!d&&0<v(e.target).closest("div.fancytree-drag-helper,#fancytree-drop-marker").length?(n||o||v.ui.fancytree).debug("Drag event over helper: ignored."):(r.helper.data("ftTargetNode",d),l&&l.updateHelper&&(t=n.tree._makeHookContext(n,e,{otherNode:d,ui:r,draggable:a,dropMarker:v("#fancytree-drop-marker")}),l.updateHelper.call(n.tree,n,t)),o&&o!==d&&o.tree.ext.dnd._onDragEvent("leave",o,n,e,r,a),d&&d.tree.options.dnd.dragDrop&&(d===o||d.tree.ext.dnd._onDragEvent("enter",d,n,e,r,a),d.tree.ext.dnd._onDragEvent("over",d,n,e,r,a)))},stop:function(e,r){var t=v(this).data("ui-draggable")||v(this).data("draggable"),a=r.helper.data("ftSourceNode")||null,n=r.helper.data("ftTargetNode")||null,o="mouseup"===e.type&&1===e.which;o||(a||n||v.ui.fancytree).debug("Drag was cancelled"),n&&(o&&n.tree.ext.dnd._onDragEvent("drop",n,a,e,r,t),n.tree.ext.dnd._onDragEvent("leave",n,a,e,r,t)),a&&a.tree.ext.dnd._onDragEvent("stop",a,null,e,r,t)}}),t=!0)),r&&r.dragStart&&e.widget.element.draggable(v.extend({addClasses:!1,appendTo:e.$container,containment:!1,delay:0,distance:4,revert:!1,scroll:!0,scrollSpeed:7,scrollSensitivity:10,connectToFancytree:!0,helper:function(e){var r,t,a=v.ui.fancytree.getNode(e.target);return a?(t=a.tree.options.dnd,r=v(a.span),(r=v("<div class='fancytree-drag-helper'><span class='fancytree-drag-helper-img' /></div>").css({zIndex:3,position:"relative"}).append(r.find("span.fancytree-title").clone())).data("ftSourceNode",a),n&&r.find(".fancytree-drag-helper-img").addClass(n.map._addClass+" "+n.map.dragHelper),t.initHelper&&t.initHelper.call(a.tree,a,{node:a,tree:a.tree,originalEvent:e,ui:{helper:r}}),r):"<div>ERROR?: helper requested but sourceNode not found</div>"},start:function(e,r){return!!r.helper.data("ftSourceNode")}},e.options.dnd.draggable)),r&&r.dragDrop&&e.widget.element.droppable(v.extend({addClasses:!1,tolerance:"intersect",greedy:!1},e.options.dnd.droppable))}return v.ui.fancytree.registerExtension({name:"dnd",version:"2.38.3",options:{autoExpandMS:1e3,draggable:null,droppable:null,focusOnClick:!1,preventVoidMoves:!0,preventRecursiveMoves:!0,smartRevert:!0,dropMarkerOffsetX:-24,dropMarkerInsertOffsetX:-16,dragStart:null,dragStop:null,initHelper:null,updateHelper:null,dragEnter:null,dragOver:null,dragExpand:null,dragDrop:null,dragLeave:null},treeInit:function(t){var e=t.tree;this._superApply(arguments),e.options.dnd.dragStart&&e.$container.on("mousedown",function(e){var r;t.options.dnd.focusOnClick&&((r=v.ui.fancytree.getNode(e))&&r.debug("Re-enable focus that was prevented by jQuery UI draggable."),setTimeout(function(){v(e.target).closest(":tabbable").focus()},10))}),r(e)},_setDndStatus:function(e,r,t,a,n){var o,d="center",l=this._local,s=this.options.dnd,i=this.options.glyph,p=e?v(e.span):null,e=v(r.span),r=e.find("span.fancytree-title");if(l.$dropMarker||(l.$dropMarker=v("<div id='fancytree-drop-marker'></div>").hide().css({"z-index":1e3}).prependTo(v(this.$div).parent()),i&&l.$dropMarker.addClass(i.map._addClass+" "+i.map.dropMarker)),"after"===a||"before"===a||"over"===a){switch(o=s.dropMarkerOffsetX||0,a){case"before":d="top",o+=s.dropMarkerInsertOffsetX||0;break;case"after":d="bottom",o+=s.dropMarkerInsertOffsetX||0}r={my:"left"+h(o)+" center",at:"left "+d,of:r},this.options.rtl&&(r.my="right"+h(-o)+" center",r.at="right "+d),l.$dropMarker.toggleClass(u,"after"===a).toggleClass("fancytree-drop-over","over"===a).toggleClass(c,"before"===a).toggleClass("fancytree-rtl",!!this.options.rtl).show().position(v.ui.fancytree.fixPositionOptions(r))}else l.$dropMarker.hide();p&&p.toggleClass(g,!0===n).toggleClass(f,!1===n),e.toggleClass("fancytree-drop-target","after"===a||"before"===a||"over"===a).toggleClass(u,"after"===a).toggleClass(c,"before"===a).toggleClass(g,!0===n).toggleClass(f,!1===n),t.toggleClass(g,!0===n).toggleClass(f,!1===n)},_onDragEvent:function(e,r,t,a,n,o){var d,l,s,i,p=this.options.dnd,g=this._makeHookContext(r,a,{otherNode:t,ui:n,draggable:o}),u=null,c=this,f=v(r.span);switch(p.smartRevert&&(o.options.revert="invalid"),e){case"start":r.isStatusNode()?u=!1:p.dragStart&&(u=p.dragStart(r,g)),!1===u?(this.debug("tree.dragStart() cancelled"),n.helper.trigger("mouseup").hide()):(p.smartRevert&&(d=r[g.tree.nodeContainerAttrName].getBoundingClientRect(),l=v(o.options.appendTo)[0].getBoundingClientRect(),o.originalPosition.left=Math.max(0,d.left-l.left),o.originalPosition.top=Math.max(0,d.top-l.top)),f.addClass("fancytree-drag-source"),v(document).on("keydown.fancytree-dnd,mousedown.fancytree-dnd",function(e){("keydown"===e.type&&e.which===v.ui.keyCode.ESCAPE||"mousedown"===e.type)&&c.ext.dnd._cancelDrag()}));break;case"enter":u=!!(i=(!p.preventRecursiveMoves||!r.isDescendantOf(t))&&(p.dragEnter?p.dragEnter(r,g):null))&&(Array.isArray(i)?{over:0<=v.inArray("over",i),before:0<=v.inArray("before",i),after:0<=v.inArray("after",i)}:{over:!0===i||"over"===i,before:!0===i||"before"===i,after:!0===i||"after"===i}),n.helper.data("enterResponse",u);break;case"over":s=null,!1===(l=n.helper.data("enterResponse"))||("string"==typeof l?s=l:(i=f.offset(),i={x:(i={x:a.pageX-i.left,y:a.pageY-i.top}).x/f.width(),y:i.y/f.height()},l.after&&.75<i.y||!l.over&&l.after&&.5<i.y?s="after":l.before&&i.y<=.25||!l.over&&l.before&&i.y<=.5?s="before":l.over&&(s="over"),p.preventVoidMoves&&(r===t?(this.debug("    drop over source node prevented"),s=null):"before"===s&&t&&r===t.getNextSibling()?(this.debug("    drop after source node prevented"),s=null):"after"===s&&t&&r===t.getPrevSibling()?(this.debug("    drop before source node prevented"),s=null):"over"===s&&t&&t.parent===r&&t.isLastSibling()&&(this.debug("    drop last child over own parent prevented"),s=null)),n.helper.data("hitMode",s))),"before"===s||"after"===s||!p.autoExpandMS||!1===r.hasChildren()||r.expanded||p.dragExpand&&!1===p.dragExpand(r,g)||r.scheduleAction("expand",p.autoExpandMS),s&&p.dragOver&&(g.hitMode=s,u=p.dragOver(r,g)),l=!1!==u&&null!==s,p.smartRevert&&(o.options.revert=!l),this._local._setDndStatus(t,r,n.helper,s,l);break;case"drop":(s=n.helper.data("hitMode"))&&p.dragDrop&&(g.hitMode=s,p.dragDrop(r,g));break;case"leave":r.scheduleAction("cancel"),n.helper.data("enterResponse",null),n.helper.data("hitMode",null),this._local._setDndStatus(t,r,n.helper,"out",void 0),p.dragLeave&&p.dragLeave(r,g);break;case"stop":f.removeClass("fancytree-drag-source"),v(document).off(".fancytree-dnd"),p.dragStop&&p.dragStop(r,g);break;default:v.error("Unsupported drag event: "+e)}return u},_cancelDrag:function(){var e=v.ui.ddmanager.current;e&&e.cancel()}}),v.ui.fancytree});

/*! Extension 'jquery.fancytree.dnd5.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(c){"use strict";var s,l,u=c.ui.fancytree,n=/Mac/.test(navigator.platform),i="fancytree-drag-source",f="fancytree-drag-remove",v="fancytree-drop-accept",y="fancytree-drop-after",b="fancytree-drop-before",h="fancytree-drop-over",m="fancytree-drop-reject",E="fancytree-drop-target",p="application/x-fancytree-node",D=null,g=null,w=null,N=null,x=null,d=null,S=null,k=null,C=null,M=null;function A(){w=g=d=k=S=M=x=null,N&&N.removeClass(i+" "+f),N=null,D&&D.hide(),l&&(l.remove(),l=null)}function T(e){return 0===e?"":0<e?"+"+e:""+e}function I(e,r){var t,o=r.tree,a=r.dataTransfer;"dragstart"===e.type?(r.effectAllowed=o.options.dnd5.effectAllowed,r.dropEffect=o.options.dnd5.dropEffectDefault):(r.effectAllowed=k,r.dropEffect=S),r.dropEffectSuggested=(t=e,o=(e=o).options.dnd5.dropEffectDefault,n?t.metaKey&&t.altKey||t.ctrlKey?o="link":t.metaKey?o="move":t.altKey&&(o="copy"):t.ctrlKey?o="copy":t.shiftKey?o="move":t.altKey&&(o="link"),o!==d&&e.info("evalEffectModifiers: "+t.type+" - evalEffectModifiers(): "+d+" -> "+o),d=o),r.isMove="move"===r.dropEffect,r.files=a.files||[]}function O(e,r,t){var o=r.tree,a=r.dataTransfer;return"dragstart"!==e.type&&k!==r.effectAllowed&&o.warn("effectAllowed should only be changed in dragstart event: "+e.type+": data.effectAllowed changed from "+k+" -> "+r.effectAllowed),!1===t&&(o.info("applyDropEffectCallback: allowDrop === false"),r.effectAllowed="none",r.dropEffect="none"),r.isMove="move"===r.dropEffect,"dragstart"===e.type&&(k=r.effectAllowed,S=r.dropEffect),a.effectAllowed=k,a.dropEffect=S}function P(e,r){if(r.options.dnd5.scroll&&(g=r.tree,d=e,a=g.options.dnd5,n=g.$scrollParent[0],l=a.scrollSensitivity,p=a.scrollSpeed,o=0,n!==document&&"HTML"!==n.tagName?(a=g.$scrollParent.offset(),i=n.scrollTop,a.top+n.offsetHeight-d.pageY<l?0<n.scrollHeight-g.$scrollParent.innerHeight()-i&&(n.scrollTop=o=i+p):0<i&&d.pageY-a.top<l&&(n.scrollTop=o=i-p)):0<(i=c(document).scrollTop())&&d.pageY-i<l?(o=i-p,c(document).scrollTop(o)):c(window).height()-(d.pageY-i)<l&&(o=i+p,c(document).scrollTop(o)),o&&g.debug("autoScroll: "+o+"px")),!r.node)return r.tree.warn("Ignored dragover for non-node"),C;var t,o,a=null,n=r.tree,d=n.options,s=d.dnd5,l=r.node,i=r.otherNode,f="center",p=c(l.span),g=p.find("span.fancytree-title");if(!1===x)return n.debug("Ignored dragover, since dragenter returned false."),!1;if("string"==typeof x&&c.error("assert failed: dragenter returned string"),o=p.offset(),p=(e.pageY-o.top)/p.height(),void 0===e.pageY&&n.warn("event.pageY is undefined: see issue #1013."),x.after&&.75<p||!x.over&&x.after&&.5<p?a="after":x.before&&p<=.25||!x.over&&x.before&&p<=.5?a="before":x.over&&(a="over"),s.preventVoidMoves&&"move"===r.dropEffect&&(l===i?(l.debug("Drop over source node prevented."),a=null):"before"===a&&i&&l===i.getNextSibling()?(l.debug("Drop after source node prevented."),a=null):"after"===a&&i&&l===i.getPrevSibling()?(l.debug("Drop before source node prevented."),a=null):"over"===a&&i&&i.parent===l&&i.isLastSibling()&&(l.debug("Drop last child over own parent prevented."),a=null)),(r.hitMode=a)&&s.dragOver&&(I(e,r),s.dragOver(l,r),O(e,r,!!a),a=r.hitMode),"after"===(C=a)||"before"===a||"over"===a){switch(t=s.dropMarkerOffsetX||0,a){case"before":f="top",t+=s.dropMarkerInsertOffsetX||0;break;case"after":f="bottom",t+=s.dropMarkerInsertOffsetX||0}g={my:"left"+T(t)+" center",at:"left "+f,of:g},d.rtl&&(g.my="right"+T(-t)+" center",g.at="right "+f),D.toggleClass(y,"after"===a).toggleClass(h,"over"===a).toggleClass(b,"before"===a).show().position(u.fixPositionOptions(g))}else D.hide();return c(l.span).toggleClass(E,"after"===a||"before"===a||"over"===a).toggleClass(y,"after"===a).toggleClass(b,"before"===a).toggleClass(v,"over"===a).toggleClass(m,!1===a),a}function j(e){var r,t=this,o=t.options.dnd5,a=null,n=u.getNode(e),d=e.dataTransfer||e.originalEvent.dataTransfer,s={tree:t,node:n,options:t.options,originalEvent:e.originalEvent,widget:t.widget,hitMode:x,dataTransfer:d,otherNode:g||null,otherNodeList:w||null,otherNodeData:null,useDefaultImage:!0,dropEffect:void 0,dropEffectSuggested:void 0,effectAllowed:void 0,files:null,isCancelled:void 0,isMove:void 0};switch(e.type){case"dragenter":if(M=null,!n){t.debug("Ignore non-node "+e.type+": "+e.target.tagName+"."+e.target.className),x=!1;break}if(c(n.span).addClass(h).removeClass(v+" "+m),r=0<=c.inArray(p,d.types),o.preventNonNodes&&!r){n.debug("Reject dropping a non-node."),x=!1;break}if(o.preventForeignNodes&&(!g||g.tree!==n.tree)){n.debug("Reject dropping a foreign node."),x=!1;break}if(o.preventSameParent&&s.otherNode&&s.otherNode.tree===n.tree&&n.parent===s.otherNode.parent){n.debug("Reject dropping as sibling (same parent)."),x=!1;break}if(o.preventRecursion&&s.otherNode&&s.otherNode.tree===n.tree&&n.isDescendantOf(s.otherNode)){n.debug("Reject dropping below own ancestor."),x=!1;break}if(o.preventLazyParents&&!n.isLoaded()){n.warn("Drop over unloaded target node prevented."),x=!1;break}D.show(),I(e,s),r=o.dragEnter(n,s),r=!!(r=r)&&(r=c.isPlainObject(r)?{over:!!r.over,before:!!r.before,after:!!r.after}:Array.isArray(r)?{over:0<=c.inArray("over",r),before:0<=c.inArray("before",r),after:0<=c.inArray("after",r)}:{over:!0===r||"over"===r,before:!0===r||"before"===r,after:!0===r||"after"===r},0!==Object.keys(r).length&&r),O(e,s,a=(x=r)&&(r.over||r.before||r.after));break;case"dragover":if(!n){t.debug("Ignore non-node "+e.type+": "+e.target.tagName+"."+e.target.className);break}I(e,s),a=!!(C=P(e,s)),("over"===C||!1===C)&&!n.expanded&&!1!==n.hasChildren()?M?!(o.autoExpandMS&&Date.now()-M>o.autoExpandMS)||n.isLoading()||o.dragExpand&&!1===o.dragExpand(n,s)||n.setExpanded():M=Date.now():M=null;break;case"dragleave":if(!n){t.debug("Ignore non-node "+e.type+": "+e.target.tagName+"."+e.target.className);break}if(!c(n.span).hasClass(h)){n.debug("Ignore dragleave (multi).");break}c(n.span).removeClass(h+" "+v+" "+m),n.scheduleAction("cancel"),o.dragLeave(n,s),D.hide();break;case"drop":if(0<=c.inArray(p,d.types)&&(i=d.getData(p),t.info(e.type+": getData('application/x-fancytree-node'): '"+i+"'")),i||(i=d.getData("text"),t.info(e.type+": getData('text'): '"+i+"'")),i)try{void 0!==(l=JSON.parse(i)).title&&(s.otherNodeData=l)}catch(e){}t.debug(e.type+": nodeData: '"+i+"', otherNodeData: ",s.otherNodeData),c(n.span).removeClass(h+" "+v+" "+m),s.hitMode=C,I(e,s),s.isCancelled=!C;var l=g&&g.span,i=g&&g.tree;o.dragDrop(n,s),e.preventDefault(),l&&!document.body.contains(l)&&(i===t?(t.debug("Drop handler removed source element: generating dragEnd."),o.dragEnd(g,s)):t.warn("Drop handler removed source element: dragend event may be lost.")),A()}if(a)return e.preventDefault(),!1}return c.ui.fancytree.getDragNodeList=function(){return w||[]},c.ui.fancytree.getDragNode=function(){return g},c.ui.fancytree.registerExtension({name:"dnd5",version:"2.38.3",options:{autoExpandMS:1500,dropMarkerInsertOffsetX:-16,dropMarkerOffsetX:-24,dropMarkerParent:"body",multiSource:!1,effectAllowed:"all",dropEffectDefault:"move",preventForeignNodes:!1,preventLazyParents:!0,preventNonNodes:!1,preventRecursion:!0,preventSameParent:!1,preventVoidMoves:!0,scroll:!0,scrollSensitivity:20,scrollSpeed:5,setTextTypeJson:!1,sourceCopyHook:null,dragStart:null,dragDrag:c.noop,dragEnd:c.noop,dragEnter:null,dragOver:c.noop,dragExpand:c.noop,dragDrop:c.noop,dragLeave:c.noop},treeInit:function(e){var r=e.tree,t=e.options,o=t.glyph||null,a=t.dnd5;0<=c.inArray("dnd",t.extensions)&&c.error("Extensions 'dnd' and 'dnd5' are mutually exclusive."),a.dragStop&&c.error("dragStop is not used by ext-dnd5. Use dragEnd instead."),null!=a.preventRecursiveMoves&&c.error("preventRecursiveMoves was renamed to preventRecursion."),a.dragStart&&u.overrideMethod(e.options,"createNode",function(e,r){this._super.apply(this,arguments),r.node.span?r.node.span.draggable=!0:r.node.warn("Cannot add `draggable`: no span tag")}),this._superApply(arguments),this.$container.addClass("fancytree-ext-dnd5"),e=c("<span>").appendTo(this.$container),this.$scrollParent=e.scrollParent(),e.remove(),(D=c("#fancytree-drop-marker")).length||(D=c("<div id='fancytree-drop-marker'></div>").hide().css({"z-index":1e3,"pointer-events":"none"}).prependTo(a.dropMarkerParent),o&&u.setSpanIcon(D[0],o.map._addClass,o.map.dropMarker)),D.toggleClass("fancytree-rtl",!!t.rtl),a.dragStart&&r.$container.on("dragstart drag dragend",function(e){var r=this,t=r.options.dnd5,o=u.getNode(e),a=e.dataTransfer||e.originalEvent.dataTransfer,n={tree:r,node:o,options:r.options,originalEvent:e.originalEvent,widget:r.widget,dataTransfer:a,useDefaultImage:!0,dropEffect:void 0,dropEffectSuggested:void 0,effectAllowed:void 0,files:void 0,isCancelled:void 0,isMove:void 0};switch(e.type){case"dragstart":if(!o)return r.info("Ignored dragstart on a non-node."),!1;g=o,w=!1===t.multiSource?[o]:!0===t.multiSource?o.isSelected()?r.getSelectedNodes():[o]:t.multiSource(o,n),(N=c(c.map(w,function(e){return e.span}))).addClass(i);var d=o.toDict(!0,t.sourceCopyHook);d.treeId=o.tree._id,d=JSON.stringify(d);try{a.setData(p,d),a.setData("text/html",c(o.span).html()),a.setData("text/plain",o.title)}catch(e){r.warn("Could not set data (IE only accepts 'text') - "+e)}return(t.setTextTypeJson?a.setData("text",d):a.setData("text",o.title),I(e,n),!1===t.dragStart(o,n))?(A(),!1):(O(e,n),l=null,n.useDefaultImage&&(s=c(o.span).find(".fancytree-title"),w&&1<w.length&&(l=c("<span class='fancytree-childcounter'/>").text("+"+(w.length-1)).appendTo(s)),a.setDragImage&&a.setDragImage(s[0],-10,-10)),!0);case"drag":I(e,n),t.dragDrag(o,n),O(e,n),N.toggleClass(f,n.isMove);break;case"dragend":I(e,n),A(),n.isCancelled=!C,t.dragEnd(o,n,!C)}}.bind(r)),a.dragEnter&&r.$container.on("dragenter dragover dragleave drop",j.bind(r))}}),c.ui.fancytree});

/*! Extension 'jquery.fancytree.edit.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(l){"use strict";var t=/Mac/.test(navigator.platform),c=l.ui.fancytree.escapeHtml,u=l.ui.fancytree.trim,o=l.ui.fancytree.unescapeHtml;return l.ui.fancytree._FancytreeNodeClass.prototype.editStart=function(){var t,i=this,e=this.tree,n=e.ext.edit,r=e.options.edit,a=l(".fancytree-title",i.span),s={node:i,tree:e,options:e.options,isNew:l(i[e.statusClassPropName]).hasClass("fancytree-edit-new"),orgTitle:i.title,input:null,dirty:!1};if(!1===r.beforeEdit.call(i,{type:"beforeEdit"},s))return!1;l.ui.fancytree.assert(!n.currentNode,"recursive edit"),n.currentNode=this,n.eventData=s,e.widget._unbind(),n.lastDraggableAttrValue=i.span.draggable,n.lastDraggableAttrValue&&(i.span.draggable=!1),l(document).on("mousedown.fancytree-edit",function(e){l(e.target).hasClass("fancytree-edit-input")||i.editEnd(!0,e)}),t=l("<input />",{class:"fancytree-edit-input",type:"text",value:e.options.escapeTitles?s.orgTitle:o(s.orgTitle)}),n.eventData.input=t,null!=r.adjustWidthOfs&&t.width(a.width()+r.adjustWidthOfs),null!=r.inputCss&&t.css(r.inputCss),a.html(t),t.focus().change(function(e){t.addClass("fancytree-edit-dirty")}).on("keydown",function(e){switch(e.which){case l.ui.keyCode.ESCAPE:i.editEnd(!1,e);break;case l.ui.keyCode.ENTER:return i.editEnd(!0,e),!1}e.stopPropagation()}).blur(function(e){return i.editEnd(!0,e)}),r.edit.call(i,{type:"edit"},s)},l.ui.fancytree._FancytreeNodeClass.prototype.editEnd=function(e,t){var i,n=this,r=this.tree,a=r.ext.edit,s=a.eventData,o=r.options.edit,d=l(".fancytree-title",n.span).find("input.fancytree-edit-input");return o.trim&&d.val(u(d.val())),i=d.val(),s.dirty=i!==n.title,s.originalEvent=t,!1===e?s.save=!1:s.isNew?s.save=""!==i:s.save=s.dirty&&""!==i,!1!==o.beforeClose.call(n,{type:"beforeClose"},s)&&((!s.save||!1!==o.save.call(n,{type:"save"},s))&&(d.removeClass("fancytree-edit-dirty").off(),l(document).off(".fancytree-edit"),s.save?(n.setTitle(r.options.escapeTitles?i:c(i)),n.setFocus()):s.isNew?(n.remove(),n=s.node=null,a.relatedNode.setFocus()):(n.renderTitle(),n.setFocus()),a.eventData=null,a.currentNode=null,a.relatedNode=null,r.widget._bind(),n&&a.lastDraggableAttrValue&&(n.span.draggable=!0),r.$container.get(0).focus({preventScroll:!0}),s.input=null,o.close.call(n,{type:"close"},s),!0))},l.ui.fancytree._FancytreeNodeClass.prototype.editCreateNode=function(e,t){var i,n=this.tree,r=this;e=e||"child",null==t?t={title:""}:"string"==typeof t?t={title:t}:l.ui.fancytree.assert(l.isPlainObject(t)),"child"!==e||this.isExpanded()||!1===this.hasChildren()?((i=this.addNode(t,e)).match=!0,l(i[n.statusClassPropName]).removeClass("fancytree-hide").addClass("fancytree-match"),i.makeVisible().done(function(){l(i[n.statusClassPropName]).addClass("fancytree-edit-new"),r.tree.ext.edit.relatedNode=r,i.editStart()})):this.setExpanded().done(function(){r.editCreateNode(e,t)})},l.ui.fancytree._FancytreeClass.prototype.isEditing=function(){return this.ext.edit?this.ext.edit.currentNode:null},l.ui.fancytree._FancytreeNodeClass.prototype.isEditing=function(){return!!this.tree.ext.edit&&this.tree.ext.edit.currentNode===this},l.ui.fancytree.registerExtension({name:"edit",version:"2.38.3",options:{adjustWidthOfs:4,allowEmpty:!1,inputCss:{minWidth:"3em"},triggerStart:["f2","mac+enter","shift+click"],trim:!0,beforeClose:l.noop,beforeEdit:l.noop,close:l.noop,edit:l.noop,save:l.noop},currentNode:null,treeInit:function(e){var n=e.tree;this._superApply(arguments),this.$container.addClass("fancytree-ext-edit").on("fancytreebeforeupdateviewport",function(e,t){var i=n.isEditing();i&&(i.info("Cancel edit due to scroll event."),i.editEnd(!1,e))})},nodeClick:function(e){var t=l.ui.fancytree.eventToString(e.originalEvent),i=e.options.edit.triggerStart;return"shift+click"===t&&0<=l.inArray("shift+click",i)&&e.originalEvent.shiftKey||"click"===t&&0<=l.inArray("clickActive",i)&&e.node.isActive()&&!e.node.isEditing()&&l(e.originalEvent.target).hasClass("fancytree-title")?(e.node.editStart(),!1):this._superApply(arguments)},nodeDblclick:function(e){return 0<=l.inArray("dblclick",e.options.edit.triggerStart)?(e.node.editStart(),!1):this._superApply(arguments)},nodeKeydown:function(e){switch(e.originalEvent.which){case 113:if(0<=l.inArray("f2",e.options.edit.triggerStart))return e.node.editStart(),!1;break;case l.ui.keyCode.ENTER:if(0<=l.inArray("mac+enter",e.options.edit.triggerStart)&&t)return e.node.editStart(),!1}return this._superApply(arguments)}}),l.ui.fancytree});

/*! Extension 'jquery.fancytree.filter.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(g){"use strict";var m="__not_found__",x=g.ui.fancytree.escapeHtml;function v(e){return(e+"").replace(/([.?*+^$[\]\\(){}|-])/g,"\\$1")}function C(e,t,i){for(var n=[],a=1;a<t.length;a++){var r=t[a].length+(1===a?0:1)+(n[n.length-1]||0);n.push(r)}var s=e.split("");return i?n.forEach(function(e){s[e]="\ufff7"+s[e]+"\ufff8"}):n.forEach(function(e){s[e]="<mark>"+s[e]+"</mark>"}),s.join("")}return g.ui.fancytree._FancytreeClass.prototype._applyFilterImpl=function(n,a,e){var t,r,s,l,o,h,d=0,i=this.options,c=i.escapeTitles,u=i.autoCollapse,p=g.extend({},i.filter,e),f="hide"===p.mode,y=!!p.leavesOnly&&!a;if("string"==typeof n){if(""===n)return this.warn("Fancytree passing an empty string as a filter is handled as clearFilter()."),void this.clearFilter();t=p.fuzzy?n.split("").map(v).reduce(function(e,t){return e+"([^"+t+"]*)"+t},""):v(n),r=new RegExp(t,"i"),s=new RegExp(v(n),"gi"),c&&(l=new RegExp(v("\ufff7"),"g"),o=new RegExp(v("\ufff8"),"g")),n=function(e){if(!e.title)return!1;var t,i=c?e.title:0<=(t=e.title).indexOf(">")?g("<div/>").html(t).text():t,t=i.match(r);return t&&p.highlight&&(c?(h=p.fuzzy?C(i,t,c):i.replace(s,function(e){return"\ufff7"+e+"\ufff8"}),e.titleWithHighlight=x(h).replace(l,"<mark>").replace(o,"</mark>")):p.fuzzy?e.titleWithHighlight=C(i,t):e.titleWithHighlight=i.replace(s,function(e){return"<mark>"+e+"</mark>"})),!!t}}return this.enableFilter=!0,this.lastFilterArgs=arguments,e=this.enableUpdate(!1),this.$div.addClass("fancytree-ext-filter"),f?this.$div.addClass("fancytree-ext-filter-hide"):this.$div.addClass("fancytree-ext-filter-dimm"),this.$div.toggleClass("fancytree-ext-filter-hide-expanders",!!p.hideExpanders),this.rootNode.subMatchCount=0,this.visit(function(e){delete e.match,delete e.titleWithHighlight,e.subMatchCount=0}),(t=this.getRootNode()._findDirectChild(m))&&t.remove(),i.autoCollapse=!1,this.visit(function(t){if(!y||null==t.children){var e=n(t),i=!1;if("skip"===e)return t.visit(function(e){e.match=!1},!0),"skip";e||!a&&"branch"!==e||!t.parent.match||(i=e=!0),e&&(d++,t.match=!0,t.visitParents(function(e){e!==t&&(e.subMatchCount+=1),!p.autoExpand||i||e.expanded||(e.setExpanded(!0,{noAnimation:!0,noEvents:!0,scrollIntoView:!1}),e._filterAutoExpanded=!0)},!0))}}),i.autoCollapse=u,0===d&&p.nodata&&f&&(!0===(t="function"==typeof(t=p.nodata)?t():t)?t={}:"string"==typeof t&&(t={title:t}),t=g.extend({statusNodeType:"nodata",key:m,title:this.options.strings.noData},t),this.getRootNode().addNode(t).match=!0),this._callHook("treeStructureChanged",this,"applyFilter"),this.enableUpdate(e),d},g.ui.fancytree._FancytreeClass.prototype.filterNodes=function(e,t){return"boolean"==typeof t&&(t={leavesOnly:t},this.warn("Fancytree.filterNodes() leavesOnly option is deprecated since 2.9.0 / 2015-04-19. Use opts.leavesOnly instead.")),this._applyFilterImpl(e,!1,t)},g.ui.fancytree._FancytreeClass.prototype.filterBranches=function(e,t){return this._applyFilterImpl(e,!0,t)},g.ui.fancytree._FancytreeClass.prototype.updateFilter=function(){this.enableFilter&&this.lastFilterArgs&&this.options.filter.autoApply?this._applyFilterImpl.apply(this,this.lastFilterArgs):this.warn("updateFilter(): no filter active.")},g.ui.fancytree._FancytreeClass.prototype.clearFilter=function(){var t,e=this.getRootNode()._findDirectChild(m),i=this.options.escapeTitles,n=this.options.enhanceTitle,a=this.enableUpdate(!1);e&&e.remove(),delete this.rootNode.match,delete this.rootNode.subMatchCount,this.visit(function(e){e.match&&e.span&&(t=g(e.span).find(">span.fancytree-title"),i?t.text(e.title):t.html(e.title),n&&n({type:"enhanceTitle"},{node:e,$title:t})),delete e.match,delete e.subMatchCount,delete e.titleWithHighlight,e.$subMatchBadge&&(e.$subMatchBadge.remove(),delete e.$subMatchBadge),e._filterAutoExpanded&&e.expanded&&e.setExpanded(!1,{noAnimation:!0,noEvents:!0,scrollIntoView:!1}),delete e._filterAutoExpanded}),this.enableFilter=!1,this.lastFilterArgs=null,this.$div.removeClass("fancytree-ext-filter fancytree-ext-filter-dimm fancytree-ext-filter-hide"),this._callHook("treeStructureChanged",this,"clearFilter"),this.enableUpdate(a)},g.ui.fancytree._FancytreeClass.prototype.isFilterActive=function(){return!!this.enableFilter},g.ui.fancytree._FancytreeNodeClass.prototype.isMatched=function(){return!(this.tree.enableFilter&&!this.match)},g.ui.fancytree.registerExtension({name:"filter",version:"2.38.3",options:{autoApply:!0,autoExpand:!1,counter:!0,fuzzy:!1,hideExpandedCounter:!0,hideExpanders:!1,highlight:!0,leavesOnly:!1,nodata:!0,mode:"dimm"},nodeLoadChildren:function(e,t){var i=e.tree;return this._superApply(arguments).done(function(){i.enableFilter&&i.lastFilterArgs&&e.options.filter.autoApply&&i._applyFilterImpl.apply(i,i.lastFilterArgs)})},nodeSetExpanded:function(e,t,i){var n=e.node;return delete n._filterAutoExpanded,!t&&e.options.filter.hideExpandedCounter&&n.$subMatchBadge&&n.$subMatchBadge.show(),this._superApply(arguments)},nodeRenderStatus:function(e){var t=e.node,i=e.tree,n=e.options.filter,a=g(t.span).find("span.fancytree-title"),r=g(t[i.statusClassPropName]),s=e.options.enhanceTitle,l=e.options.escapeTitles,e=this._super(e);return r.length&&i.enableFilter&&(r.toggleClass("fancytree-match",!!t.match).toggleClass("fancytree-submatch",!!t.subMatchCount).toggleClass("fancytree-hide",!(t.match||t.subMatchCount)),!n.counter||!t.subMatchCount||t.isExpanded()&&n.hideExpandedCounter?t.$subMatchBadge&&t.$subMatchBadge.hide():(t.$subMatchBadge||(t.$subMatchBadge=g("<span class='fancytree-childcounter'/>"),g("span.fancytree-icon, span.fancytree-custom-icon",t.span).append(t.$subMatchBadge)),t.$subMatchBadge.show().text(t.subMatchCount)),!t.span||t.isEditing&&t.isEditing.call(t)||(t.titleWithHighlight?a.html(t.titleWithHighlight):l?a.text(t.title):a.html(t.title),s&&s({type:"enhanceTitle"},{node:t,$title:a}))),e}}),g.ui.fancytree});

/*! Extension 'jquery.fancytree.glyph.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(i){"use strict";var d=i.ui.fancytree,n={awesome3:{_addClass:"",checkbox:"icon-check-empty",checkboxSelected:"icon-check",checkboxUnknown:"icon-check icon-muted",dragHelper:"icon-caret-right",dropMarker:"icon-caret-right",error:"icon-exclamation-sign",expanderClosed:"icon-caret-right",expanderLazy:"icon-angle-right",expanderOpen:"icon-caret-down",loading:"icon-refresh icon-spin",nodata:"icon-meh",noExpander:"",radio:"icon-circle-blank",radioSelected:"icon-circle",doc:"icon-file-alt",docOpen:"icon-file-alt",folder:"icon-folder-close-alt",folderOpen:"icon-folder-open-alt"},awesome4:{_addClass:"fa",checkbox:"fa-square-o",checkboxSelected:"fa-check-square-o",checkboxUnknown:"fa-square fancytree-helper-indeterminate-cb",dragHelper:"fa-arrow-right",dropMarker:"fa-long-arrow-right",error:"fa-warning",expanderClosed:"fa-caret-right",expanderLazy:"fa-angle-right",expanderOpen:"fa-caret-down",loading:{html:"<span class='fa fa-spinner fa-pulse' />"},nodata:"fa-meh-o",noExpander:"",radio:"fa-circle-thin",radioSelected:"fa-circle",doc:"fa-file-o",docOpen:"fa-file-o",folder:"fa-folder-o",folderOpen:"fa-folder-open-o"},awesome5:{_addClass:"",checkbox:"far fa-square",checkboxSelected:"far fa-check-square",checkboxUnknown:"fas fa-square fancytree-helper-indeterminate-cb",radio:"far fa-circle",radioSelected:"fas fa-circle",radioUnknown:"far fa-dot-circle",dragHelper:"fas fa-arrow-right",dropMarker:"fas fa-long-arrow-alt-right",error:"fas fa-exclamation-triangle",expanderClosed:"fas fa-caret-right",expanderLazy:"fas fa-angle-right",expanderOpen:"fas fa-caret-down",loading:"fas fa-spinner fa-pulse",nodata:"far fa-meh",noExpander:"",doc:"far fa-file",docOpen:"far fa-file",folder:"far fa-folder",folderOpen:"far fa-folder-open"},bootstrap3:{_addClass:"glyphicon",checkbox:"glyphicon-unchecked",checkboxSelected:"glyphicon-check",checkboxUnknown:"glyphicon-expand fancytree-helper-indeterminate-cb",dragHelper:"glyphicon-play",dropMarker:"glyphicon-arrow-right",error:"glyphicon-warning-sign",expanderClosed:"glyphicon-menu-right",expanderLazy:"glyphicon-menu-right",expanderOpen:"glyphicon-menu-down",loading:"glyphicon-refresh fancytree-helper-spin",nodata:"glyphicon-info-sign",noExpander:"",radio:"glyphicon-remove-circle",radioSelected:"glyphicon-ok-circle",doc:"glyphicon-file",docOpen:"glyphicon-file",folder:"glyphicon-folder-close",folderOpen:"glyphicon-folder-open"},material:{_addClass:"material-icons",checkbox:{text:"check_box_outline_blank"},checkboxSelected:{text:"check_box"},checkboxUnknown:{text:"indeterminate_check_box"},dragHelper:{text:"play_arrow"},dropMarker:{text:"arrow-forward"},error:{text:"warning"},expanderClosed:{text:"chevron_right"},expanderLazy:{text:"last_page"},expanderOpen:{text:"expand_more"},loading:{text:"autorenew",addClass:"fancytree-helper-spin"},nodata:{text:"info"},noExpander:{text:""},radio:{text:"radio_button_unchecked"},radioSelected:{text:"radio_button_checked"},doc:{text:"insert_drive_file"},docOpen:{text:"insert_drive_file"},folder:{text:"folder"},folderOpen:{text:"folder_open"}}};function l(e,r,n,a,o){var t=a.map,c=t[o],d=i(r),a=d.find(".fancytree-childcounter"),t=n+" "+(t._addClass||"");"string"==typeof(c="function"==typeof c?c.call(this,e,r,o):c)?(r.innerHTML="",d.attr("class",t+" "+c).append(a)):c&&(c.text?r.textContent=""+c.text:c.html?r.innerHTML=c.html:r.innerHTML="",d.attr("class",t+" "+(c.addClass||"")).append(a))}return i.ui.fancytree.registerExtension({name:"glyph",version:"2.38.3",options:{preset:null,map:{}},treeInit:function(e){var r=e.tree,e=e.options.glyph;e.preset?(d.assert(!!n[e.preset],"Invalid value for `options.glyph.preset`: "+e.preset),e.map=i.extend({},n[e.preset],e.map)):r.warn("ext-glyph: missing `preset` option."),this._superApply(arguments),r.$container.addClass("fancytree-ext-glyph")},nodeRenderStatus:function(e){var r,n,a=e.node,o=i(a.span),t=e.options.glyph,c=this._super(e);return a.isRootNode()||((n=o.children(".fancytree-expander").get(0))&&(r=a.expanded&&a.hasChildren()?"expanderOpen":a.isUndefined()?"expanderLazy":a.hasChildren()?"expanderClosed":"noExpander",l(a,n,"fancytree-expander",t,r)),(n=(a.tr?i("td",a.tr).find(".fancytree-checkbox"):o.children(".fancytree-checkbox")).get(0))&&(e=d.evalOption("checkbox",a,a,t,!1),a.parent&&a.parent.radiogroup||"radio"===e?l(a,n,"fancytree-checkbox fancytree-radio",t,r=a.selected?"radioSelected":"radio"):l(a,n,"fancytree-checkbox",t,r=a.selected?"checkboxSelected":a.partsel?"checkboxUnknown":"checkbox")),(n=o.children(".fancytree-icon").get(0))&&(r=a.statusNodeType||(a.folder?a.expanded&&a.hasChildren()?"folderOpen":"folder":a.expanded?"docOpen":"doc"),l(a,n,"fancytree-icon",t,r))),c},nodeSetStatus:function(e,r,n,a){var o,t=e.options.glyph,c=e.node,e=this._superApply(arguments);return"error"!==r&&"loading"!==r&&"nodata"!==r||(c.parent?(o=i(".fancytree-expander",c.span).get(0))&&l(c,o,"fancytree-expander",t,r):(o=i(".fancytree-statusnode-"+r,c[this.nodeContainerAttrName]).find(".fancytree-icon").get(0))&&l(c,o,"fancytree-icon",t,r)),e}}),i.ui.fancytree});

/*! Extension 'jquery.fancytree.gridnav.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree","./jquery.fancytree.table"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree.table"),module.exports=e(require("jquery"))):e(jQuery)}(function(l){"use strict";var p=l.ui.keyCode,o={text:[p.UP,p.DOWN],checkbox:[p.UP,p.DOWN,p.LEFT,p.RIGHT],link:[p.UP,p.DOWN,p.LEFT,p.RIGHT],radiobutton:[p.UP,p.DOWN,p.LEFT,p.RIGHT],"select-one":[p.LEFT,p.RIGHT],"select-multiple":[p.LEFT,p.RIGHT]};function a(e,t){var n,i,r,o,a,s,u=e.closest("td"),c=null;switch(t){case p.LEFT:c=u.prev();break;case p.RIGHT:c=u.next();break;case p.UP:case p.DOWN:for(n=u.parent(),r=n,a=u.get(0),s=0,r.children().each(function(){return this!==a&&(o=l(this).prop("colspan"),void(s+=o||1))}),i=s;(n=t===p.UP?n.prev():n.next()).length&&(n.is(":hidden")||!(c=function(e,t){var n,i=null,r=0;return e.children().each(function(){return t<=r?(i=l(this),!1):(n=l(this).prop("colspan"),void(r+=n||1))}),i}(n,i))||!c.find(":input,a").length););}return c}return l.ui.fancytree.registerExtension({name:"gridnav",version:"2.38.3",options:{autofocusInput:!1,handleCursorKeys:!0},treeInit:function(n){this._requireExtension("table",!0,!0),this._superApply(arguments),this.$container.addClass("fancytree-ext-gridnav"),this.$container.on("focusin",function(e){var t=l.ui.fancytree.getNode(e.target);t&&!t.isActive()&&(e=n.tree._makeHookContext(t,e),n.tree._callHook("nodeSetActive",e,!0))})},nodeSetActive:function(e,t,n){var i=e.options.gridnav,r=e.node,o=e.originalEvent||{},o=l(o.target).is(":input");t=!1!==t,this._superApply(arguments),t&&(e.options.titlesTabbable?(o||(l(r.span).find("span.fancytree-title").focus(),r.setFocus()),e.tree.$container.attr("tabindex","-1")):i.autofocusInput&&!o&&l(r.tr||r.span).find(":input:enabled").first().focus())},nodeKeydown:function(e){var t,n,i=e.options.gridnav,r=e.originalEvent,e=l(r.target);return e.is(":input:enabled")?t=e.prop("type"):e.is("a")&&(t="link"),t&&i.handleCursorKeys?!((t=o[t])&&0<=l.inArray(r.which,t)&&(n=a(e,r.which))&&n.length)||(n.find(":input:enabled,a").focus(),!1):this._superApply(arguments)}}),l.ui.fancytree});

/*! Extension 'jquery.fancytree.multi.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(o){"use strict";return o.ui.fancytree.registerExtension({name:"multi",version:"2.38.3",options:{allowNoSelect:!1,mode:"sameParent"},treeInit:function(e){this._superApply(arguments),this.$container.addClass("fancytree-ext-multi"),1===e.options.selectMode&&o.error("Fancytree ext-multi: selectMode: 1 (single) is not compatible.")},nodeClick:function(e){var t=e.tree,i=e.node,r=t.getActiveNode()||t.getFirstChild(),n="checkbox"===e.targetType,c="expander"===e.targetType;switch(o.ui.fancytree.eventToString(e.originalEvent)){case"click":if(c)break;n||(t.selectAll(!1),i.setSelected());break;case"shift+click":t.visitRows(function(e){if(e.setSelected(),e===i)return!1},{start:r,reverse:r.isBelowOf(i)});break;case"ctrl+click":case"meta+click":return void i.toggleSelected()}return this._superApply(arguments)},nodeKeydown:function(e){var t=e.tree,i=e.node,r=e.originalEvent;switch(o.ui.fancytree.eventToString(r)){case"up":case"down":t.selectAll(!1),i.navigate(r.which,!0),t.getActiveNode().setSelected();break;case"shift+up":case"shift+down":i.navigate(r.which,!0),t.getActiveNode().setSelected()}return this._superApply(arguments)}}),o.ui.fancytree});

/*! Extension 'jquery.fancytree.persist.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(y){"use strict";var t=null,o=null,i=null,s=y.ui.fancytree.assert,u="active",v="expanded",p="focus",f="selected";try{s(window.localStorage&&window.localStorage.getItem),o={get:function(e){return window.localStorage.getItem(e)},set:function(e,t){window.localStorage.setItem(e,t)},remove:function(e){window.localStorage.removeItem(e)}}}catch(e){y.ui.fancytree.warn("Could not access window.localStorage",e)}try{s(window.sessionStorage&&window.sessionStorage.getItem),i={get:function(e){return window.sessionStorage.getItem(e)},set:function(e,t){window.sessionStorage.setItem(e,t)},remove:function(e){window.sessionStorage.removeItem(e)}}}catch(e){y.ui.fancytree.warn("Could not access window.sessionStorage",e)}return"function"==typeof Cookies?t={get:Cookies.get,set:function(e,t){Cookies.set(e,t,this.options.persist.cookie)},remove:Cookies.remove}:y&&"function"==typeof y.cookie&&(t={get:y.cookie,set:function(e,t){y.cookie(e,t,this.options.persist.cookie)},remove:y.removeCookie}),y.ui.fancytree._FancytreeClass.prototype.clearPersistData=function(e){var t=this.ext.persist,o=t.cookiePrefix;0<=(e=e||"active expanded focus selected").indexOf(u)&&t._data(o+u,null),0<=e.indexOf(v)&&t._data(o+v,null),0<=e.indexOf(p)&&t._data(o+p,null),0<=e.indexOf(f)&&t._data(o+f,null)},y.ui.fancytree._FancytreeClass.prototype.clearCookies=function(e){return this.warn("'tree.clearCookies()' is deprecated since v2.27.0: use 'clearPersistData()' instead."),this.clearPersistData(e)},y.ui.fancytree._FancytreeClass.prototype.getPersistData=function(){var e=this.ext.persist,t=e.cookiePrefix,o=e.cookieDelimiter,i={};return i[u]=e._data(t+u),i[v]=(e._data(t+v)||"").split(o),i[f]=(e._data(t+f)||"").split(o),i[p]=e._data(t+p),i},y.ui.fancytree.registerExtension({name:"persist",version:"2.38.3",options:{cookieDelimiter:"~",cookiePrefix:void 0,cookie:{raw:!1,expires:"",path:"",domain:"",secure:!1},expandLazy:!1,expandOpts:void 0,fireActivate:!0,overrideSource:!0,store:"auto",types:"active expanded focus selected"},_data:function(e,t){var o=this._local.store;if(void 0===t)return o.get.call(this,e);null===t?o.remove.call(this,e):o.set.call(this,e,t)},_appendKey:function(e,t,o){t=""+t;var i=this._local,s=this.options.persist.cookieDelimiter,r=i.cookiePrefix+e,n=i._data(r),e=n?n.split(s):[],n=y.inArray(t,e);0<=n&&e.splice(n,1),o&&e.push(t),i._data(r,e.join(s))},treeInit:function(e){var a=e.tree,c=e.options,d=this._local,l=this.options.persist;return d.cookiePrefix=l.cookiePrefix||"fancytree-"+a._id+"-",d.storeActive=0<=l.types.indexOf(u),d.storeExpanded=0<=l.types.indexOf(v),d.storeSelected=0<=l.types.indexOf(f),d.storeFocus=0<=l.types.indexOf(p),d.store=null,"auto"===l.store&&(l.store=o?"local":"cookie"),y.isPlainObject(l.store)?d.store=l.store:"cookie"===l.store?d.store=t:"local"!==l.store&&"session"!==l.store||(d.store="local"===l.store?o:i),s(d.store,"Need a valid store."),a.$div.on("fancytreeinit",function(e){var t,o,i,s,r,n;!1!==a._triggerTreeEvent("beforeRestore",null,{})&&(i=d._data(d.cookiePrefix+p),s=!1===l.fireActivate,r=d._data(d.cookiePrefix+v),n=r&&r.split(l.cookieDelimiter),(d.storeExpanded?function e(t,o,i,s,r){var n,a,c,d,l=!1,u=t.options.persist.expandOpts,p=[],f=[];for(i=i||[],r=r||y.Deferred(),n=0,c=i.length;n<c;n++)a=i[n],(d=t.getNodeByKey(a))?s&&d.isUndefined()?(l=!0,t.debug("_loadLazyNodes: "+d+" is lazy: loading..."),"expand"===s?p.push(d.setExpanded(!0,u)):p.push(d.load())):(t.debug("_loadLazyNodes: "+d+" already loaded."),d.setExpanded(!0,u)):(f.push(a),t.debug("_loadLazyNodes: "+d+" was not yet found."));return y.when.apply(y,p).always(function(){if(l&&0<f.length)e(t,o,f,s,r);else{if(f.length)for(t.warn("_loadLazyNodes: could not load those keys: ",f),n=0,c=f.length;n<c;n++)a=i[n],o._appendKey(v,i[n],!1);r.resolve()}}),r}(a,d,n,!!l.expandLazy&&"expand",null):(new y.Deferred).resolve()).done(function(){if(d.storeSelected){if(r=d._data(d.cookiePrefix+f))for(n=r.split(l.cookieDelimiter),t=0;t<n.length;t++)(o=a.getNodeByKey(n[t]))?(void 0===o.selected||l.overrideSource&&!1===o.selected)&&(o.selected=!0,o.renderStatus()):d._appendKey(f,n[t],!1);3===a.options.selectMode&&a.visit(function(e){if(e.selected)return e.fixSelection3AfterClick(),"skip"})}d.storeActive&&(!(r=d._data(d.cookiePrefix+u))||!c.persist.overrideSource&&a.activeNode||(o=a.getNodeByKey(r))&&(o.debug("persist: set active",r),o.setActive(!0,{noFocus:!0,noEvents:s}))),d.storeFocus&&i&&(o=a.getNodeByKey(i))&&(a.options.titlesTabbable?y(o.span).find(".fancytree-title"):y(a.$container)).focus(),a._triggerTreeEvent("restore",null,{})}))}),this._superApply(arguments)},nodeSetActive:function(e,t,o){var i=this._local;return t=!1!==t,t=this._superApply(arguments),i.storeActive&&i._data(i.cookiePrefix+u,this.activeNode?this.activeNode.key:null),t},nodeSetExpanded:function(e,t,o){var i=e.node,s=this._local;return t=!1!==t,e=this._superApply(arguments),s.storeExpanded&&s._appendKey(v,i.key,t),e},nodeSetFocus:function(e,t){var o=this._local;return t=!1!==t,t=this._superApply(arguments),o.storeFocus&&o._data(o.cookiePrefix+p,this.focusNode?this.focusNode.key:null),t},nodeSetSelected:function(e,t,o){var i=e.tree,s=e.node,r=this._local;return t=!1!==t,t=this._superApply(arguments),r.storeSelected&&(3===i.options.selectMode?(i=(i=y.map(i.getSelectedNodes(!0),function(e){return e.key})).join(e.options.persist.cookieDelimiter),r._data(r.cookiePrefix+f,i)):r._appendKey(f,s.key,s.selected)),t}}),y.ui.fancytree});

/*! Extension 'jquery.fancytree.table.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(v){"use strict";var g=v.ui.fancytree.assert;function x(e,n){e.visit(function(e){var t=e.tr;if(t&&(t.style.display=e.hide||!n?"none":""),!e.expanded)return"skip"})}return v.ui.fancytree.registerExtension({name:"table",version:"2.38.3",options:{checkboxColumnIdx:null,indentation:16,mergeStatusColumns:!0,nodeColumnIdx:0},treeInit:function(e){var t,n,r,o=e.tree,d=e.options,s=d.table,a=o.widget.element;if(null!=s.customStatus&&(null==d.renderStatusColumns?(o.warn("The 'customStatus' option is deprecated since v2.15.0. Use 'renderStatusColumns' instead."),d.renderStatusColumns=s.customStatus):v.error("The 'customStatus' option is deprecated since v2.15.0. Use 'renderStatusColumns' only instead.")),d.renderStatusColumns&&!0===d.renderStatusColumns&&(d.renderStatusColumns=d.renderColumns),a.addClass("fancytree-container fancytree-ext-table"),(r=a.find(">tbody")).length||(a.find(">tr").length&&v.error("Expected table > tbody > tr. If you see this please open an issue."),r=v("<tbody>").appendTo(a)),o.tbody=r[0],o.columnCount=v("thead >tr",a).last().find(">th",a).length,(n=r.children("tr").first()).length)e=n.children("td").length,o.columnCount&&e!==o.columnCount&&(o.warn("Column count mismatch between thead ("+o.columnCount+") and tbody ("+e+"): using tbody."),o.columnCount=e),n=n.clone();else for(g(1<=o.columnCount,"Need either <thead> or <tbody> with <td> elements to determine column count."),n=v("<tr />"),t=0;t<o.columnCount;t++)n.append("<td />");n.find(">td").eq(s.nodeColumnIdx).html("<span class='fancytree-node' />"),d.aria&&(n.attr("role","row"),n.find("td").attr("role","gridcell")),o.rowFragment=document.createDocumentFragment(),o.rowFragment.appendChild(n.get(0)),r.empty(),o.statusClassPropName="tr",o.ariaPropName="tr",this.nodeContainerAttrName="tr",o.$container=a,this._superApply(arguments),v(o.rootNode.ul).remove(),o.rootNode.ul=null,this.$container.attr("tabindex",d.tabindex),d.aria&&o.$container.attr("role","treegrid").attr("aria-readonly",!0)},nodeRemoveChildMarkup:function(e){e.node.visit(function(e){e.tr&&(v(e.tr).remove(),e.tr=null)})},nodeRemoveMarkup:function(e){var t=e.node;t.tr&&(v(t.tr).remove(),t.tr=null),this.nodeRemoveChildMarkup(e)},nodeRender:function(e,t,n,r,o){var d,s,a,i,l,u,c,p,h,m=e.tree,f=e.node,y=e.options,C=!f.parent;if(!1!==m._enableUpdate){if(o||(e.hasCollapsedParents=f.parent&&!f.parent.expanded),!C)if(f.tr&&t&&this.nodeRemoveMarkup(e),f.tr)t?this.nodeRenderTitle(e):this.nodeRenderStatus(e);else{if(e.hasCollapsedParents&&!n)return;l=m.rowFragment.firstChild.cloneNode(!0),p=function(e){var t,n,r=e.parent,o=r?r.children:null;if(o&&1<o.length&&o[0]!==e)for(n=o[v.inArray(e,o)-1],g(n.tr);n.children&&n.children.length&&(t=n.children[n.children.length-1]).tr;)n=t;else n=r;return n}(f),g(p),(!0===r&&o||n&&e.hasCollapsedParents)&&(l.style.display="none"),p.tr?(h=p.tr).parentNode.insertBefore(l,h.nextSibling):(g(!p.parent,"prev. row must have a tr, or be system root"),(p=m.tbody).insertBefore(l,p.firstChild)),f.tr=l,f.key&&y.generateIds&&(f.tr.id=y.idPrefix+f.key),(f.tr.ftnode=f).span=v("span.fancytree-node",f.tr).get(0),this.nodeRenderTitle(e),y.createNode&&y.createNode.call(m,{type:"createNode"},e)}if(y.renderNode&&y.renderNode.call(m,{type:"renderNode"},e),(d=f.children)&&(C||n||f.expanded))for(a=0,i=d.length;a<i;a++)(c=v.extend({},e,{node:d[a]})).hasCollapsedParents=c.hasCollapsedParents||!f.expanded,this.nodeRender(c,t,n,r,!0);d&&!o&&(u=f.tr||null,s=m.tbody.firstChild,f.visit(function(e){var t;e.tr&&(e.parent.expanded||"none"===e.tr.style.display||(e.tr.style.display="none",x(e,!1)),e.tr.previousSibling!==u&&(f.debug("_fixOrder: mismatch at node: "+e),t=u?u.nextSibling:s,m.tbody.insertBefore(e.tr,t)),u=e.tr)}))}},nodeRenderTitle:function(e,t){var n=e.tree,r=e.node,o=e.options,d=r.isStatusNode(),s=this._super(e,t);return r.isRootNode()||(o.checkbox&&!d&&null!=o.table.checkboxColumnIdx&&(t=v("span.fancytree-checkbox",r.span),v(r.tr).find("td").eq(+o.table.checkboxColumnIdx).html(t)),this.nodeRenderStatus(e),d?o.renderStatusColumns?o.renderStatusColumns.call(n,{type:"renderStatusColumns"},e):o.table.mergeStatusColumns&&r.isTopLevel()&&v(r.tr).find(">td").eq(0).prop("colspan",n.columnCount).text(r.title).addClass("fancytree-status-merged").nextAll().remove():o.renderColumns&&o.renderColumns.call(n,{type:"renderColumns"},e)),s},nodeRenderStatus:function(e){var t=e.node,n=e.options;this._super(e),v(t.tr).removeClass("fancytree-node"),e=(t.getLevel()-1)*n.table.indentation,n.rtl?v(t.span).css({paddingRight:e+"px"}):v(t.span).css({paddingLeft:e+"px"})},nodeSetExpanded:function(t,n,r){if(n=!1!==n,t.node.expanded&&n||!t.node.expanded&&!n)return this._superApply(arguments);var o=new v.Deferred,e=v.extend({},r,{noEvents:!0,noAnimation:!0});function d(e){e?(x(t.node,n),n&&t.options.autoScroll&&!r.noAnimation&&t.node.hasChildren()?t.node.getLastChild().scrollIntoView(!0,{topNode:t.node}).always(function(){r.noEvents||t.tree._triggerNodeEvent(n?"expand":"collapse",t),o.resolveWith(t.node)}):(r.noEvents||t.tree._triggerNodeEvent(n?"expand":"collapse",t),o.resolveWith(t.node))):(r.noEvents||t.tree._triggerNodeEvent(n?"expand":"collapse",t),o.rejectWith(t.node))}return r=r||{},this._super(t,n,e).done(function(){d(!0)}).fail(function(){d(!1)}),o.promise()},nodeSetStatus:function(e,t,n,r){return"ok"!==t||(e=(e=e.node).children?e.children[0]:null)&&e.isStatusNode()&&v(e.tr).remove(),this._superApply(arguments)},treeClear:function(e){return this.nodeRemoveChildMarkup(this._makeHookContext(this.rootNode)),this._superApply(arguments)},treeDestroy:function(e){return this.$container.find("tbody").empty(),this.$source&&this.$source.removeClass("fancytree-helper-hidden"),this._superApply(arguments)}}),v.ui.fancytree});

/*! Extension 'jquery.fancytree.themeroller.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(l){"use strict";return l.ui.fancytree.registerExtension({name:"themeroller",version:"2.38.3",options:{activeClass:"ui-state-active",addClass:"ui-corner-all",focusClass:"ui-state-focus",hoverClass:"ui-state-hover",selectedClass:"ui-state-highlight"},treeInit:function(e){var s=e.widget.element,t=e.options.themeroller;this._superApply(arguments),"TABLE"===s[0].nodeName?(s.addClass("ui-widget ui-corner-all"),s.find(">thead tr").addClass("ui-widget-header"),s.find(">tbody").addClass("ui-widget-conent")):s.addClass("ui-widget ui-widget-content ui-corner-all"),s.on("mouseenter mouseleave",".fancytree-node",function(e){var s=l.ui.fancytree.getNode(e.target),e="mouseenter"===e.type;l(s.tr||s.span).toggleClass(t.hoverClass+" "+t.addClass,e)})},treeDestroy:function(e){this._superApply(arguments),e.widget.element.removeClass("ui-widget ui-widget-content ui-corner-all")},nodeRenderStatus:function(e){var s={},t=e.node,a=l(t.tr||t.span),i=e.options.themeroller;this._super(e),s[i.activeClass]=!1,s[i.focusClass]=!1,s[i.selectedClass]=!1,t.isActive()&&(s[i.activeClass]=!0),t.hasFocus()&&(s[i.focusClass]=!0),t.isSelected()&&!t.isActive()&&(s[i.selectedClass]=!0),a.toggleClass(i.activeClass,s[i.activeClass]),a.toggleClass(i.focusClass,s[i.focusClass]),a.toggleClass(i.selectedClass,s[i.selectedClass]),a.addClass(i.addClass)}}),l.ui.fancytree});

/*! Extension 'jquery.fancytree.wide.min.js' */!function(e){"function"==typeof define&&define.amd?define(["jquery","./jquery.fancytree"],e):"object"==typeof module&&module.exports?(require("./jquery.fancytree"),module.exports=e(require("jquery"))):e(jQuery)}(function(o){"use strict";var p=/^([+-]?(?:\d+|\d*\.\d+))([a-z]*|%)$/;function f(e,t){var a=o("#"+(e="fancytree-style-"+e));if(t){a.length||(a=o("<style />").attr("id",e).addClass("fancytree-style").prop("type","text/css").appendTo("head"));try{a.html(t)}catch(e){a[0].styleSheet.cssText=t}return a}a.remove()}function u(e,t,a,n,l,i){for(var s="#"+e+" span.fancytree-level-",c=[],r=0;r<t;r++)c.push(s+(r+1)+" span.fancytree-title { padding-left: "+(r*a+n)+i+"; }");return c.push("#"+e+" div.ui-effects-wrapper ul li span.fancytree-title, #"+e+" li.fancytree-animating span.fancytree-title { padding-left: "+l+i+"; position: static; width: auto; }"),c.join("\n")}return o.ui.fancytree.registerExtension({name:"wide",version:"2.38.3",options:{iconWidth:null,iconSpacing:null,labelSpacing:null,levelOfs:null},treeCreate:function(e){this._superApply(arguments),this.$container.addClass("fancytree-ext-wide");var t=e.options.wide,a=o("<li id='fancytreeTemp'><span class='fancytree-node'><span class='fancytree-icon' /><span class='fancytree-title' /></span><ul />").appendTo(e.tree.$container),n=a.find(".fancytree-icon"),l=a.find("ul"),i=t.iconSpacing||n.css("margin-left"),s=t.iconWidth||n.css("width"),c=t.labelSpacing||"3px",r=t.levelOfs||l.css("padding-left");a.remove(),n=i.match(p)[2],i=parseFloat(i,10),t=c.match(p)[2],c=parseFloat(c,10),l=s.match(p)[2],s=parseFloat(s,10),a=r.match(p)[2],n===l&&a===l&&t===l||o.error("iconWidth, iconSpacing, and levelOfs must have the same css measure unit"),this._local.measureUnit=l,this._local.levelOfs=parseFloat(r),this._local.lineOfs=(1+(e.options.checkbox?1:0)+(!1===e.options.icon?0:1))*(s+i)+i,this._local.labelOfs=c,this._local.maxDepth=10,f(c=this.$container.uniqueId().attr("id"),u(c,this._local.maxDepth,this._local.levelOfs,this._local.lineOfs,this._local.labelOfs,this._local.measureUnit))},treeDestroy:function(e){return f(this.$container.attr("id"),null),this._superApply(arguments)},nodeRenderStatus:function(e){var t=e.node,a=t.getLevel(),n=this._super(e);return a>this._local.maxDepth&&(e=this.$container.attr("id"),this._local.maxDepth*=2,t.debug("Define global ext-wide css up to level "+this._local.maxDepth),f(e,u(e,this._local.maxDepth,this._local.levelOfs,this._local.lineOfs,this._local.labelSpacing,this._local.measureUnit))),o(t.span).addClass("fancytree-level-"+a),n}}),o.ui.fancytree});
// Value returned by `require('jquery.fancytree')`
return $.ui.fancytree;
}));  // End of closure

// Note: We currently allow eval() to parse the 'data' attribtes, when initializing from HTML.
/*jslint laxbreak: true, browser: true, evil: true, indent: 0, white: false, onevar: false */

/*************************************************************************
 *	Debug functions
 */

var _canLog = true;

function _log(mode, msg) {
	/**
	 * Usage: logMsg("%o was toggled", this);
	 */
	if( !_canLog ){
		return;
	}
	// Remove first argument
	var args = Array.prototype.slice.apply(arguments, [1]);
	// Prepend timestamp
	var dt = new Date();
	var tag = dt.getHours()+":"+dt.getMinutes()+":"+dt.getSeconds()+"."+dt.getMilliseconds();
	args[0] = tag + " - " + args[0];

	try {
		switch( mode ) {
		case "info":
			window.console.info.apply(window.console, args);
			break;
		case "warn":
			window.console.warn.apply(window.console, args);
			break;
		default:
			window.console.log.apply(window.console, args);
			break;
		}
	} catch(e) {
		if( !window.console ){
			_canLog = false; // Permanently disable, when logging is not supported by the browser
		}
	}
}

function logMsg(msg) {
	Array.prototype.unshift.apply(arguments, ["debug"]);
	_log.apply(this, arguments);
}


// Forward declaration
var getDynaTreePersistData = null;



/*************************************************************************
 *	Constants
 */
var DTNodeStatus_Error   = -1;
var DTNodeStatus_Loading = 1;
var DTNodeStatus_Ok      = 0;


// Start of local namespace
(function($) {

/*************************************************************************
 *	Common tool functions.
 */

var Class = {
	create: function() {
		return function() {
			this.initialize.apply(this, arguments);
		};
	}
};

// Tool function to get dtnode from the event target:
function getDtNodeFromElement(el) {
	var iMax = 5;
	while( el && iMax-- ) {
		if(el.dtnode) { return el.dtnode; }
		el = el.parentNode;
	}
	return null;
}

function noop() {
}

/*************************************************************************
 *	Class DynaTreeNode
 */
var DynaTreeNode = Class.create();

DynaTreeNode.prototype = {
	initialize: function(parent, tree, data) {
		/**
		 * @constructor
		 */
		this.parent = parent;
		this.tree = tree;
		if ( typeof data === "string" ){
			data = { title: data };
		}
		if( data.key === undefined ){
			data.key = "_" + tree._nodeCount++;
		}
		this.data = $.extend({}, $.ui.dynatree.nodedatadefaults, data);
		this.li = null; // not yet created
		this.span = null; // not yet created
		this.ul = null; // not yet created
		this.childList = null; // no subnodes yet
		this.isLoading = false; // Lazy content is being loaded
		this.hasSubSel = false;
		this.bExpanded = false;
		this.bSelected = false;

	},

	toString: function() {
		return "DynaTreeNode<" + this.data.key + ">: '" + this.data.title + "'";
	},

	toDict: function(recursive, callback) {
		var dict = $.extend({}, this.data);
		dict.activate = ( this.tree.activeNode === this );
		dict.focus = ( this.tree.focusNode === this );
		dict.expand = this.bExpanded;
		dict.select = this.bSelected;
		if( callback ){
			callback(dict);
		}
		if( recursive && this.childList ) {
			dict.children = [];
			for(var i=0, l=this.childList.length; i<l; i++ ){
				dict.children.push(this.childList[i].toDict(true, callback));
			}
		} else {
			delete dict.children;
		}
		return dict;
	},

	fromDict: function(dict) {
		/**
		 * Update node data. If dict contains 'children', then also replace
		 * the hole sub tree.
		 */
		var children = dict.children;
		if(children === undefined){
			this.data = $.extend(this.data, dict);
			this.render();
			return;
		}
		dict = $.extend({}, dict);
		dict.children = undefined;
		this.data = $.extend(this.data, dict);
		this.removeChildren();
		this.addChild(children);
	},

	_getInnerHtml: function() {
		var tree = this.tree,
			opts = tree.options,
			cache = tree.cache,
			level = this.getLevel(),
			data = this.data,
			res = "";
		// connector (expanded, expandable or simple)
		if( level < opts.minExpandLevel ) {
			if(level > 1){
				res += cache.tagConnector;
			}
			// .. else (i.e. for root level) skip expander/connector altogether
		} else if( this.hasChildren() !== false ) {
			res += cache.tagExpander;
		} else {
			res += cache.tagConnector;
		}
		// Checkbox mode
		if( opts.checkbox && data.hideCheckbox !== true && !data.isStatusNode ) {
			res += cache.tagCheckbox;
		}
		// folder or doctype icon
		if ( data.icon ) {
			res += "<img src='" + opts.imagePath + data.icon + "' alt='' />";
		} else if ( data.icon === false ) {
			// icon == false means 'no icon'
			noop(); // keep JSLint happy
		} else {
			// icon == null means 'default icon'
			res += cache.tagNodeIcon;
		}
		// node title
		var nodeTitle = "";
		if ( opts.onCustomRender ){
			nodeTitle = opts.onCustomRender.call(tree, this) || "";
		}
		if(!nodeTitle){
			var tooltip = data.tooltip ? " title='" + data.tooltip + "'" : "";
			if( opts.noLink || data.noLink ) {
				nodeTitle = "<span style='display: inline-block;' class='" + opts.classNames.title + "'" + tooltip + ">" + data.title + "</span>";
			}else{
				nodeTitle = "<a href='#' class='" + opts.classNames.title + "'" + tooltip + ">" + data.title + "</a>";
			}
		}
		res += nodeTitle;
		return res;
	},


	_fixOrder: function() {
		/**
		 * Make sure, that <li> order matches childList order.
		 */
		var cl = this.childList;
		if( !cl || !this.ul ){
			return;
		}
		var childLI = this.ul.firstChild;
		for(var i=0, l=cl.length-1; i<l; i++) {
			var childNode1 = cl[i];
			var childNode2 = childLI.dtnode;
			if( childNode1 !== childNode2 ) {
				this.tree.logDebug("_fixOrder: mismatch at index " + i + ": " + childNode1 + " != " + childNode2);
				this.ul.insertBefore(childNode1.li, childNode2.li);
			} else {
				childLI = childLI.nextSibling;
			}
		}
	},


	render: function(useEffects, includeInvisible) {
		/**
		 * Create <li><span>..</span> .. </li> tags for this node.
		 *
		 * <li id='key'> // This div contains the node's span and list of child div's.
		 *   <span class='title'>S S S A</span> // Span contains graphic spans and title <a> tag
		 *   <ul> // only present, when node has children
		 *       <li>child1</li>
		 *       <li>child2</li>
		 *   </ul>
		 * </li>
		 */
//		this.tree.logDebug("%s.render(%s)", this, useEffects);
		// ---
		var tree = this.tree,
			parent = this.parent,
			data = this.data,
			opts = tree.options,
			cn = opts.classNames,
			isLastSib = this.isLastSibling();

		if( !parent && !this.ul ) {
			// Root node has only a <ul>
			this.li = this.span = null;
			this.ul = document.createElement("ul");
			if( opts.minExpandLevel > 1 ){
				this.ul.className = cn.container + " " + cn.noConnector;
			}else{
				this.ul.className = cn.container;
			}
		} else if( parent ) {
			// Create <li><span /> </li>
			if( ! this.li ) {
				this.li = document.createElement("li");
				this.li.dtnode = this;
				if( data.key && opts.generateIds ){
					this.li.id = opts.idPrefix + data.key;
				}
				this.span = document.createElement("span");
				this.span.className = cn.title;
				this.li.appendChild(this.span);

				if( !parent.ul ) {
					// This is the parent's first child: create UL tag
					// (Hidden, because it will be
					parent.ul = document.createElement("ul");
					parent.ul.style.display = "none";
					parent.li.appendChild(parent.ul);
//					if( opts.minExpandLevel > this.getLevel() ){
//						parent.ul.className = cn.noConnector;
//					}
				}
				parent.ul.appendChild(this.li);
			}
			// set node connector images, links and text
			this.span.innerHTML = this._getInnerHtml();
			// Set classes for current status
			var cnList = [];
			cnList.push(cn.node);
			if( data.isFolder ){
				cnList.push(cn.folder);
			}
			if( this.bExpanded ){
				cnList.push(cn.expanded);
			}
			if( this.hasChildren() !== false ){
				cnList.push(cn.hasChildren);
			}
			if( data.isLazy && this.childList === null ){
				cnList.push(cn.lazy);
			}
			if( isLastSib ){
				cnList.push(cn.lastsib);
			}
			if( this.bSelected ){
				cnList.push(cn.selected);
			}
			if( this.hasSubSel ){
				cnList.push(cn.partsel);
			}
			if( tree.activeNode === this ){
				cnList.push(cn.active);
			}
			if( data.addClass ){
				cnList.push(data.addClass);
			}
			// IE6 doesn't correctly evaluate multiple class names,
			// so we create combined class names that can be used in the CSS
			cnList.push(cn.combinedExpanderPrefix
					+ (this.bExpanded ? "e" : "c")
					+ (data.isLazy && this.childList === null ? "d" : "")
					+ (isLastSib ? "l" : "")
					);
			cnList.push(cn.combinedIconPrefix
					+ (this.bExpanded ? "e" : "c")
					+ (data.isFolder ? "f" : "")
					);
			this.span.className = cnList.join(" ");

			// TODO: we should not set this in the <span> tag also, if we set it here:
			this.li.className = isLastSib ? cn.lastsib : "";

			// Hide children, if node is collapsed
//			this.ul.style.display = ( this.bExpanded || !parent ) ? "" : "none";
			// Allow tweaking, binding, ...
			if(opts.onRender){
				opts.onRender.call(tree, this, this.span);
			}
		}
		// Visit child nodes
		if( (this.bExpanded || includeInvisible === true) && this.childList ) {
			for(var i=0, l=this.childList.length; i<l; i++) {
				this.childList[i].render(false, includeInvisible);
			}
			// Make sure the tag order matches the child array
			this._fixOrder();
		}
		// Hide children, if node is collapsed
		if( this.ul ) {
			var isHidden = (this.ul.style.display === "none");
			var isExpanded = !!this.bExpanded;
//			logMsg("isHidden:%s", isHidden);
			if( useEffects && opts.fx && (isHidden === isExpanded) ) {
				var duration = opts.fx.duration || 200;
				$(this.ul).animate(opts.fx, duration);
			} else {
				this.ul.style.display = ( this.bExpanded || !parent ) ? "" : "none";
			}
		}
	},
	/** Return '/id1/id2/id3'. */
	getKeyPath: function(excludeSelf) {
		var path = [];
		this.visitParents(function(node){
			if(node.parent){
				path.unshift(node.data.key);
			}
		}, !excludeSelf);
		return "/" + path.join(this.tree.options.keyPathSeparator);
	},

	getParent: function() {
		return this.parent;
	},

	getChildren: function() {
		return this.childList;
	},

	/** Check if node has children (returns undefined, if not sure). */
	hasChildren: function() {
		if(this.data.isLazy){
			if(this.childList === null || this.childList === undefined){
				// Not yet loaded
				return undefined;
			}else if(this.childList.length === 0){
				// Loaded, but response was empty
				return false;
			}else if(this.childList.length === 1 && this.childList[0].isStatusNode()){
				// Currently loading or load error
				return undefined;
			}
			return true;
		}
		return !!this.childList;
	},

	isFirstSibling: function() {
		var p = this.parent;
		return !p || p.childList[0] === this;
	},

	isLastSibling: function() {
		var p = this.parent;
		return !p || p.childList[p.childList.length-1] === this;
	},

	getPrevSibling: function() {
		if( !this.parent ){
			return null;
		}
		var ac = this.parent.childList;
		for(var i=1, l=ac.length; i<l; i++){ // start with 1, so prev(first) = null
			if( ac[i] === this ){
				return ac[i-1];
			}
		}
		return null;
	},

	getNextSibling: function() {
		if( !this.parent ){
			return null;
		}
		var ac = this.parent.childList;
		for(var i=0, l=ac.length-1; i<l; i++){ // up to length-2, so next(last) = null
			if( ac[i] === this ){
				return ac[i+1];
			}
		}
		return null;
	},

	isStatusNode: function() {
		return (this.data.isStatusNode === true);
	},

	isChildOf: function(otherNode) {
		return (this.parent && this.parent === otherNode);
	},

	isDescendantOf: function(otherNode) {
		if(!otherNode){
			return false;
		}
		var p = this.parent;
		while( p ) {
			if( p === otherNode ){
				return true;
			}
			p = p.parent;
		}
		return false;
	},

	countChildren: function() {
		var cl = this.childList;
		if( !cl ){
			return 0;
		}
		var n = cl.length;
		for(var i=0, l=n; i<l; i++){
			var child = cl[i];
			n += child.countChildren();
		}
		return n;
	},

	/**Sort child list by title.
	 * cmd: optional compare function.
	 * deep: optional: pass true to sort all descendant nodes.
	 */
	sortChildren: function(cmp, deep) {
		var cl = this.childList;
		if( !cl ){
			return;
		}
		cmp = cmp || function(a, b) {
			return a.data.title === b.data.title ? 0 : a.data.title > b.data.title;
			};
		cl.sort(cmp);
		if( deep ){
			for(var i=0, l=cl.length; i<l; i++){
				if( cl[i].childList ){
					cl[i].sortChildren(cmp, "$norender$");
				}
			}
		}
		if( deep !== "$norender$" ){
			this.render();
		}
	},

	_setStatusNode: function(data) {
		// Create, modify or remove the status child node (pass 'null', to remove it).
		var firstChild = ( this.childList ? this.childList[0] : null );
		if( !data ) {
			if ( firstChild && firstChild.isStatusNode()) {
				try{
					// I've seen exceptions here with loadKeyPath...
					if(this.ul){
						this.ul.removeChild(firstChild.li);
					}
				}catch(e){}
				if( this.childList.length === 1 ){
					this.childList = [];
				}else{
					this.childList.shift();
				}
			}
		} else if ( firstChild ) {
			data.isStatusNode = true;
			data.key = "_statusNode";
			firstChild.data = data;
			firstChild.render();
		} else {
			data.isStatusNode = true;
			data.key = "_statusNode";
			firstChild = this.addChild(data);
		}
	},

	setLazyNodeStatus: function(lts, opts) {
		var tooltip = (opts && opts.tooltip) ? opts.tooltip : null;
		var info = (opts && opts.info) ? " (" + opts.info + ")" : "";
		switch( lts ) {
			case DTNodeStatus_Ok:
				this._setStatusNode(null);
				$(this.span).removeClass(this.tree.options.classNames.nodeLoading);
				this.isLoading = false;
				this.render();
				if( this.tree.options.autoFocus ) {
					if( this === this.tree.tnRoot && this.childList && this.childList.length > 0) {
						// special case: using ajaxInit
						this.childList[0].focus();
					} else {
						this.focus();
					}
				}
				break;
			case DTNodeStatus_Loading:
				this.isLoading = true;
				$(this.span).addClass(this.tree.options.classNames.nodeLoading);
				// The root is hidden, so we set a temporary status child
				if(!this.parent){
					this._setStatusNode({
						title: this.tree.options.strings.loading + info,
						tooltip: tooltip,
						addClass: this.tree.options.classNames.nodeWait
					});
				}
				break;
			case DTNodeStatus_Error:
				this.isLoading = false;
//				$(this.span).addClass(this.tree.options.classNames.nodeError);
				this._setStatusNode({
					title: this.tree.options.strings.loadError + info,
					tooltip: tooltip,
					addClass: this.tree.options.classNames.nodeError
				});
				break;
			default:
				throw "Bad LazyNodeStatus: '" + lts + "'.";
		}
	},

	_parentList: function(includeRoot, includeSelf) {
		var l = [];
		var dtn = includeSelf ? this : this.parent;
		while( dtn ) {
			if( includeRoot || dtn.parent ){
				l.unshift(dtn);
			}
			dtn = dtn.parent;
		}
		return l;
	},
	getLevel: function() {
		/**
		 * Return node depth. 0: System root node, 1: visible top-level node.
		 */
		var level = 0;
		var dtn = this.parent;
		while( dtn ) {
			level++;
			dtn = dtn.parent;
		}
		return level;
	},

	_getTypeForOuterNodeEvent: function(event) {
		/** Return the inner node span (title, checkbox or expander) if
		 *  event.target points to the outer span.
		 *  This function should fix issue #93:
		 *  FF2 ignores empty spans, when generating events (returning the parent instead).
		 */
		var cns = this.tree.options.classNames;
		var target = event.target;
		// Only process clicks on an outer node span (probably due to a FF2 event handling bug)
		if( target.className.indexOf(cns.node) < 0 ) {
			return null;
		}
		// Event coordinates, relative to outer node span:
		var eventX = event.pageX - target.offsetLeft;
		var eventY = event.pageY - target.offsetTop;

		for(var i=0, l=target.childNodes.length; i<l; i++) {
			var cn = target.childNodes[i];
			var x = cn.offsetLeft - target.offsetLeft;
			var y = cn.offsetTop - target.offsetTop;
			var nx = cn.clientWidth, ny = cn.clientHeight;
//	        alert (cn.className + ": " + x + ", " + y + ", s:" + nx + ", " + ny);
			if( eventX >= x && eventX <= (x+nx) && eventY >= y && eventY <= (y+ny) ) {
//	            alert("HIT "+ cn.className);
				if( cn.className==cns.title ){
					return "title";
				}else if( cn.className==cns.expander ){
					return "expander";
				}else if( cn.className==cns.checkbox ){
					return "checkbox";
				}else if( cn.className==cns.nodeIcon ){
					return "icon";
				}
			}
		}
		return "prefix";
	},

	getEventTargetType: function(event) {
		// Return the part of a node, that a click event occured on.
		// Note: there is no check, if the event was fired on TIHS node.
		var tcn = event && event.target ? event.target.className : "";
		var cns = this.tree.options.classNames;

		if( tcn === cns.title ){
			return "title";
		}else if( tcn === cns.expander ){
			return "expander";
		}else if( tcn === cns.checkbox ){
			return "checkbox";
		}else if( tcn === cns.nodeIcon ){
			return "icon";
		}else if( tcn.indexOf("dynatree-custom-checkbox") != -1 ){
			return "dynatree-custom-checkbox";            
		}else if( tcn === cns.empty || tcn === cns.vline || tcn === cns.connector ){
			return "prefix";
		}else if( tcn.indexOf(cns.node) >= 0 ){
			// FIX issue #93
			return this._getTypeForOuterNodeEvent(event);
		}
		return null;
	},

	isVisible: function() {
		// Return true, if all parents are expanded.
		var parents = this._parentList(true, false);
		for(var i=0, l=parents.length; i<l; i++){
			if( ! parents[i].bExpanded ){ return false; }
		}
		return true;
	},

	makeVisible: function() {
		// Make sure, all parents are expanded
		var parents = this._parentList(true, false);
		for(var i=0, l=parents.length; i<l; i++){
			parents[i]._expand(true);
		}
	},

	focus: function() {
		this.makeVisible();
		try {
			$(this.span).find(">a").trigger("focus");
		} catch(e) { }
	},

	isFocused: function() {
		return (this.tree.tnFocused === this);
	},

	_activate: function(flag, fireEvents) {
		// (De)Activate - but not focus - this node.
		this.tree.logDebug("dtnode._activate(%o, fireEvents=%o) - %o", flag, fireEvents, this);
		var opts = this.tree.options;
		if( this.data.isStatusNode ){
			return;
		}
		if ( fireEvents && opts.onQueryActivate && opts.onQueryActivate.call(this.tree, flag, this) === false ){
			return; // Callback returned false
		}
		if( flag ) {
			// Activate
			if( this.tree.activeNode ) {
				if( this.tree.activeNode === this ){
					return;
				}
				this.tree.activeNode.deactivate();
			}
			if( opts.activeVisible ){
				this.makeVisible();
			}
			this.tree.activeNode = this;
			if( opts.persist ){
				$.cookie(opts.cookieId+"-active", this.data.key, opts.cookie);
			}
			this.tree.persistence.activeKey = this.data.key;
			$(this.span).addClass(opts.classNames.active);
			if ( fireEvents && opts.onActivate ){
				opts.onActivate.call(this.tree, this);
			}
			if ( fireEvents && opts.onClick ){
				opts.onClick.call(this.tree, this);
			}
		} else {
			// Deactivate
			if( this.tree.activeNode === this ) {
				if ( opts.onQueryActivate && opts.onQueryActivate.call(this.tree, false, this) === false ){
					return; // Callback returned false
				}
				$(this.span).removeClass(opts.classNames.active);
				if( opts.persist ) {
					// Note: we don't pass null, but ''. So the cookie is not deleted.
					// If we pass null, we also have to pass a COPY of opts, because $cookie will override opts.expires (issue 84)
					$.cookie(opts.cookieId+"-active", "", opts.cookie);
				}
				this.tree.persistence.activeKey = null;
				this.tree.activeNode = null;
				if ( fireEvents && opts.onDeactivate ){
					opts.onDeactivate.call(this.tree, this);
				}
			}
		}
	},

	activate: function() {
		// Select - but not focus - this node.
//		this.tree.logDebug("dtnode.activate(): %o", this);
		this._activate(true, true);
	},

	activateSilently: function() {
		this._activate(true, false);
	},

	deactivate: function() {
//		this.tree.logDebug("dtnode.deactivate(): %o", this);
		this._activate(false, true);
	},

	isActive: function() {
		return (this.tree.activeNode === this);
	},

	_userActivate: function() {
		// Handle user click / [space] / [enter], according to clickFolderMode.
		var activate = true;
		var expand = false;
		if ( this.data.isFolder ) {
			switch( this.tree.options.clickFolderMode ) {
			case 2:
				activate = false;
				expand = true;
				break;
			case 3:
				activate = expand = true;
				break;
			}
		}
		if( this.parent === null ) {
			expand = false;
		}
		if( expand ) {
			this.toggleExpand();
			this.focus();
		}
		if( activate ) {
			this.activate();
		}
	},

	_setSubSel: function(hasSubSel) {
		if( hasSubSel ) {
			this.hasSubSel = true;
			$(this.span).addClass(this.tree.options.classNames.partsel);
		} else {
			this.hasSubSel = false;
			$(this.span).removeClass(this.tree.options.classNames.partsel);
		}
	},

	_fixSelectionState: function() {
		// fix selection status, for multi-hier mode
//		this.tree.logDebug("_fixSelectionState(%o) - %o", this.bSelected, this);
		var p, i, l;
		if( this.bSelected ) {
			// Select all children
			this.visit(function(node){
				node.parent._setSubSel(true);
				node._select(true, false, false);
			});
			// Select parents, if all children are selected
			p = this.parent;
			while( p ) {
				p._setSubSel(true);
				var allChildsSelected = true;
				for(i=0, l=p.childList.length; i<l;  i++) {
					var n = p.childList[i];
					if( !n.bSelected && !n.data.isStatusNode ) {
						allChildsSelected = false;
						break;
					}
				}
				if( allChildsSelected ){
					p._select(true, false, false);
				}
				p = p.parent;
			}
		} else {
			// Deselect all children
			this._setSubSel(false);
			this.visit(function(node){
				node._setSubSel(false);
				node._select(false, false, false);
			});
			// Deselect parents, and recalc hasSubSel
			p = this.parent;
			while( p ) {
				p._select(false, false, false);
				var isPartSel = false;
				for(i=0, l=p.childList.length; i<l;  i++) {
					if( p.childList[i].bSelected || p.childList[i].hasSubSel ) {
						isPartSel = true;
						break;
					}
				}
				p._setSubSel(isPartSel);
				p = p.parent;
			}
		}
	},

	_select: function(sel, fireEvents, deep) {
		// Select - but not focus - this node.
//		this.tree.logDebug("dtnode._select(%o) - %o", sel, this);
		var opts = this.tree.options;
		if( this.data.isStatusNode ){
			return;
		}
		//
		if( this.bSelected === sel ) {
//			this.tree.logDebug("dtnode._select(%o) IGNORED - %o", sel, this);
			return;
		}
		// Allow event listener to abort selection
		if ( fireEvents && opts.onQuerySelect && opts.onQuerySelect.call(this.tree, sel, this) === false ){
			return; // Callback returned false
		}
		// Force single-selection
		if( opts.selectMode==1 && sel ) {
			this.tree.visit(function(node){
				if( node.bSelected ) {
					// Deselect; assuming that in selectMode:1 there's max. one other selected node
					node._select(false, false, false);
					return false;
				}
			});
		}

		this.bSelected = sel;
//        this.tree._changeNodeList("select", this, sel);

		if( sel ) {
			if( opts.persist ){
				this.tree.persistence.addSelect(this.data.key);
			}
			$(this.span).addClass(opts.classNames.selected);

			if( deep && opts.selectMode === 3 ){
				this._fixSelectionState();
			}
			if ( fireEvents && opts.onSelect ){
				opts.onSelect.call(this.tree, true, this);
			}
		} else {
			if( opts.persist ){
				this.tree.persistence.clearSelect(this.data.key);
			}
			$(this.span).removeClass(opts.classNames.selected);

			if( deep && opts.selectMode === 3 ){
				this._fixSelectionState();
			}
			if ( fireEvents && opts.onSelect ){
				opts.onSelect.call(this.tree, false, this);
			}
		}
	},

	select: function(sel) {
		// Select - but not focus - this node.
//		this.tree.logDebug("dtnode.select(%o) - %o", sel, this);
		if( this.data.unselectable ){
			return this.bSelected;
		}
		return this._select(sel!==false, true, true);
	},

	toggleSelect: function() {
//		this.tree.logDebug("dtnode.toggleSelect() - %o", this);
		return this.select(!this.bSelected);
	},

	isSelected: function() {
		return this.bSelected;
	},

	_loadContent: function() {
		try {
			var opts = this.tree.options;
			this.tree.logDebug("_loadContent: start - %o", this);
			this.setLazyNodeStatus(DTNodeStatus_Loading);
			if( true === opts.onLazyRead.call(this.tree, this) ) {
				// If function returns 'true', we assume that the loading is done:
				this.setLazyNodeStatus(DTNodeStatus_Ok);
				// Otherwise (i.e. if the loading was started as an asynchronous process)
				// the onLazyRead(dtnode) handler is expected to call dtnode.setLazyNodeStatus(DTNodeStatus_Ok/_Error) when done.
				this.tree.logDebug("_loadContent: succeeded - %o", this);
			}
		} catch(e) {
			this.tree.logWarning("_loadContent: failed - %o", e);
			this.setLazyNodeStatus(DTNodeStatus_Error, {tooltip: ""+e});
		}
	},

	_expand: function(bExpand, forceSync) {
		if( this.bExpanded === bExpand ) {
			this.tree.logDebug("dtnode._expand(%o) IGNORED - %o", bExpand, this);
			return;
		}
		this.tree.logDebug("dtnode._expand(%o) - %o", bExpand, this);
		var opts = this.tree.options;
		if( !bExpand && this.getLevel() < opts.minExpandLevel ) {
			this.tree.logDebug("dtnode._expand(%o) prevented collapse - %o", bExpand, this);
			return;
		}
		if ( opts.onQueryExpand && opts.onQueryExpand.call(this.tree, bExpand, this) === false ){
			return; // Callback returned false
		}
		this.bExpanded = bExpand;

		// Persist expand state
		if( opts.persist ) {
			if( bExpand ){
				this.tree.persistence.addExpand(this.data.key);
			}else{
				this.tree.persistence.clearExpand(this.data.key);
			}
		}
		// Do not apply animations in init phase, or before lazy-loading
		var allowEffects = !(this.data.isLazy && this.childList === null)
			&& !this.isLoading
			&& !forceSync;
		this.render(allowEffects);

		// Auto-collapse mode: collapse all siblings
		if( this.bExpanded && this.parent && opts.autoCollapse ) {
			var parents = this._parentList(false, true);
			for(var i=0, l=parents.length; i<l; i++){
				parents[i].collapseSiblings();
			}
		}
		// If the currently active node is now hidden, deactivate it
		if( opts.activeVisible && this.tree.activeNode && ! this.tree.activeNode.isVisible() ) {
			this.tree.activeNode.deactivate();
		}
		// Expanding a lazy node: set 'loading...' and call callback
		if( bExpand && this.data.isLazy && this.childList === null && !this.isLoading ) {
			this._loadContent();
			return;
		}
		if ( opts.onExpand ){
			opts.onExpand.call(this.tree, bExpand, this);
		}
	},

	expand: function(flag) {
		flag = (flag !== false);
		if( !this.childList && !this.data.isLazy && flag ){
			return; // Prevent expanding empty nodes
		} else if( this.parent === null && !flag ){
			return; // Prevent collapsing the root
		}
		this._expand(flag);
	},

	scheduleAction: function(mode, ms) {
		/** Schedule activity for delayed execution (cancel any pending request).
		 *  scheduleAction('cancel') will cancel the request.
		 */
		if( this.tree.timer ) {
			clearTimeout(this.tree.timer);
			this.tree.logDebug("clearTimeout(%o)", this.tree.timer);
		}
		var self = this; // required for closures
		switch (mode) {
		case "cancel":
			// Simply made sure that timer was cleared
			break;
		case "expand":
			this.tree.timer = setTimeout(function(){
				self.tree.logDebug("setTimeout: trigger expand");
				self.expand(true);
			}, ms);
			break;
		case "activate":
			this.tree.timer = setTimeout(function(){
				self.tree.logDebug("setTimeout: trigger activate");
				self.activate();
			}, ms);
			break;
		default:
			throw "Invalid mode " + mode;
		}
		this.tree.logDebug("setTimeout(%s, %s): %s", mode, ms, this.tree.timer);
	},

	toggleExpand: function() {
		this.expand(!this.bExpanded);
	},

	collapseSiblings: function() {
		if( this.parent === null ){
			return;
		}
		var ac = this.parent.childList;
		for (var i=0, l=ac.length; i<l; i++) {
			if ( ac[i] !== this && ac[i].bExpanded ){
				ac[i]._expand(false);
			}
		}
	},

	_onClick: function(event) {
//		this.tree.logDebug("dtnode.onClick(" + event.type + "): dtnode:" + this + ", button:" + event.button + ", which: " + event.which);
		var targetType = this.getEventTargetType(event);
		if( targetType === "expander" ) {
			// Clicking the expander icon always expands/collapses
			this.toggleExpand();
			this.focus(); // issue 95
		} else if( targetType === "checkbox" ) {
			// Clicking the checkbox always (de)selects
			this.toggleSelect();
			this.focus(); // issue 95
		} else if (targetType != "dynatree-custom-checkbox"){
			this._userActivate();
			var aTag = this.span.getElementsByTagName("a");
			if(aTag[0]){
				// issue 154
				// TODO: check if still required on IE 9:
				// Chrome and Safari don't focus the a-tag on click,
				// but calling focus() seem to have problems on IE:
				// http://code.google.com/p/dynatree/issues/detail?id=154
				if(!$.browser.msie){
					aTag[0].focus();
				}
			}else{
				// 'noLink' option was set
				return true;
			}
		}
		// Make sure that clicks stop, otherwise <a href='#'> jumps to the top
		return false;
	},

	_onDblClick: function(event) {
//		this.tree.logDebug("dtnode.onDblClick(" + event.type + "): dtnode:" + this + ", button:" + event.button + ", which: " + event.which);
	},

	_onKeydown: function(event) {
//		this.tree.logDebug("dtnode.onKeydown(" + event.type + "): dtnode:" + this + ", charCode:" + event.charCode + ", keyCode: " + event.keyCode + ", which: " + event.which);
		var handled = true,
			sib;
//		alert("keyDown" + event.which);

		switch( event.which ) {
			// charCodes:
//			case 43: // '+'
			case 107: // '+'
			case 187: // '+' @ Chrome, Safari
				if( !this.bExpanded ){ this.toggleExpand(); }
				break;
//			case 45: // '-'
			case 109: // '-'
			case 189: // '+' @ Chrome, Safari
				if( this.bExpanded ){ this.toggleExpand(); }
				break;
			//~ case 42: // '*'
				//~ break;
			//~ case 47: // '/'
				//~ break;
			// case 13: // <enter>
				// <enter> on a focused <a> tag seems to generate a click-event.
				// this._userActivate();
				// break;
			case 32: // <space>
				this._userActivate();
				break;
			case 8: // <backspace>
				if( this.parent ){
					this.parent.focus();
				}
				break;
			case 37: // <left>
				if( this.bExpanded ) {
					this.toggleExpand();
					this.focus();
//				} else if( this.parent && (this.tree.options.rootVisible || this.parent.parent) ) {
				} else if( this.parent && this.parent.parent ) {
					this.parent.focus();
				}
				break;
			case 39: // <right>
				if( !this.bExpanded && (this.childList || this.data.isLazy) ) {
					this.toggleExpand();
					this.focus();
				} else if( this.childList ) {
					this.childList[0].focus();
				}
				break;
			case 38: // <up>
				sib = this.getPrevSibling();
				while( sib && sib.bExpanded && sib.childList ){
					sib = sib.childList[sib.childList.length-1];
				}
//				if( !sib && this.parent && (this.tree.options.rootVisible || this.parent.parent) )
				if( !sib && this.parent && this.parent.parent ){
					sib = this.parent;
				}
				if( sib ){
					sib.focus();
				}
				break;
			case 40: // <down>
				if( this.bExpanded && this.childList ) {
					sib = this.childList[0];
				} else {
					var parents = this._parentList(false, true);
					for(var i=parents.length-1; i>=0; i--) {
						sib = parents[i].getNextSibling();
						if( sib ){ break; }
					}
				}
				if( sib ){
					sib.focus();
				}
				break;
			default:
				handled = false;
		}
		// Return false, if handled, to prevent default processing
		return !handled;
	},

	_onKeypress: function(event) {
		// onKeypress is only hooked to allow user callbacks.
		// We don't process it, because IE and Safari don't fire keypress for cursor keys.
//		this.tree.logDebug("dtnode.onKeypress(" + event.type + "): dtnode:" + this + ", charCode:" + event.charCode + ", keyCode: " + event.keyCode + ", which: " + event.which);
	},

	_onFocus: function(event) {
		// Handles blur and focus events.
//		this.tree.logDebug("dtnode.onFocus(%o): %o", event, this);
		var opts = this.tree.options;
		if ( event.type == "blur" || event.type == "focusout" ) {
			if ( opts.onBlur ){
				opts.onBlur.call(this.tree, this);
			}
			if( this.tree.tnFocused ){
				$(this.tree.tnFocused.span).removeClass(opts.classNames.focused);
			}
			this.tree.tnFocused = null;
			if( opts.persist ){
				$.cookie(opts.cookieId+"-focus", "", opts.cookie);
			}
		} else if ( event.type=="focus" || event.type=="focusin") {
			// Fix: sometimes the blur event is not generated
			if( this.tree.tnFocused && this.tree.tnFocused !== this ) {
				this.tree.logDebug("dtnode.onFocus: out of sync: curFocus: %o", this.tree.tnFocused);
				$(this.tree.tnFocused.span).removeClass(opts.classNames.focused);
			}
			this.tree.tnFocused = this;
			if ( opts.onFocus ){
				opts.onFocus.call(this.tree, this);
			}
			$(this.tree.tnFocused.span).addClass(opts.classNames.focused);
			if( opts.persist ){
				$.cookie(opts.cookieId+"-focus", this.data.key, opts.cookie);
			}
		}
		// TODO: return anything?
//		return false;
	},

	visit: function(fn, includeSelf) {
		// Call fn(node) for all child nodes. Stop iteration, if fn() returns false.
		var res = true;
		if( includeSelf === true ) {
			res = fn(this);
			if( res === false || res == "skip" ){
				return res;
			}
		}
		if(this.childList){
			for(var i=0, l=this.childList.length; i<l; i++){
				res = this.childList[i].visit(fn, true);
				if( res === false ){
					break;
				}
			}
		}
		return res;
	},

	visitParents: function(fn, includeSelf) {
		// Visit parent nodes (bottom up)
		if(includeSelf && fn(this) === false){
			return false;
		}
		var p = this.parent;
		while( p ) {
			if(fn(p) === false){
				return false;
			}
			p = p.parent;
		}
		return true;
	},

	remove: function() {
		// Remove this node
//		this.tree.logDebug ("%s.remove()", this);
		if ( this === this.tree.root ){
			throw "Cannot remove system root";
		}
		return this.parent.removeChild(this);
	},

	removeChild: function(tn) {
		// Remove tn from list of direct children.
		var ac = this.childList;
		if( ac.length == 1 ) {
			if( tn !== ac[0] ){
				throw "removeChild: invalid child";
			}
			return this.removeChildren();
		}
		if( tn === this.tree.activeNode ){
			tn.deactivate();
		}
		if( this.tree.options.persist ) {
			if( tn.bSelected ){
				this.tree.persistence.clearSelect(tn.data.key);
			}
			if ( tn.bExpanded ){
				this.tree.persistence.clearExpand(tn.data.key);
			}
		}
		tn.removeChildren(true);
//		this.div.removeChild(tn.div);
		this.ul.removeChild(tn.li);
		for(var i=0, l=ac.length; i<l; i++) {
			if( ac[i] === tn ) {
				this.childList.splice(i, 1);
//				delete tn;  // JSLint complained
				break;
			}
		}
	},

	removeChildren: function(isRecursiveCall, retainPersistence) {
		// Remove all child nodes (more efficiently than recursive remove())
		this.tree.logDebug("%s.removeChildren(%o)", this, isRecursiveCall);
		var tree = this.tree;
		var ac = this.childList;
		if( ac ) {
			for(var i=0, l=ac.length; i<l; i++) {
				var tn = ac[i];
				if ( tn === tree.activeNode && !retainPersistence ){
					tn.deactivate();
				}
				if( this.tree.options.persist && !retainPersistence ) {
					if( tn.bSelected ){
						this.tree.persistence.clearSelect(tn.data.key);
					}
					if ( tn.bExpanded ){
						this.tree.persistence.clearExpand(tn.data.key);
					}
				}
				tn.removeChildren(true, retainPersistence);
				if(this.ul){
					this.ul.removeChild(tn.li);
				}
/*
				try{
					this.ul.removeChild(tn.li);
				}catch(e){
					this.tree.logDebug("%s.removeChildren: couldnt remove LI", this, e);
				}
*/
//				delete tn;  JSLint complained
			}
			// Set to 'null' which is interpreted as 'not yet loaded' for lazy
			// nodes
			this.childList = null;
		}
		if( ! isRecursiveCall ) {
//			this._expand(false);
//			this.isRead = false;
			this.isLoading = false;
			this.render();
		}
	},

	setTitle: function(title) {
		this.fromDict({title: title});
	},

	reload: function(force) {
		throw "Use reloadChildren() instead";
	},

	reloadChildren: function(callback) {
		// Reload lazy content (expansion state is maintained).
		if( this.parent === null ){
			throw "Use tree.reload() instead";
		}else if( ! this.data.isLazy ){
			throw "node.reloadChildren() requires lazy nodes.";
		}
		// appendAjax triggers 'nodeLoaded' event.
		// We listen to this, if a callback was passed to reloadChildren
		if(callback){
			var self = this;
			var eventType = "nodeLoaded.dynatree." + this.tree.$tree.attr("id")
				+ "." + this.data.key;
			this.tree.$tree.on(eventType, function(e, node, isOk){
				self.tree.$tree.off(eventType);
				self.tree.logInfo("loaded %o, %o, %o", e, node, isOk);
				if(node !== self){
					throw "got invalid load event";
				}
				callback.call(self.tree, node, isOk);
			});
		}
		// The expansion state is maintained
		this.removeChildren();
		this._loadContent();
//		if( this.bExpanded ) {
//			// Remove children first, to prevent effects being applied
//			this.removeChildren();
//			// then force re-expand to trigger lazy loading
////			this.expand(false);
////			this.expand(true);
//			this._loadContent();
//		} else {
//			this.removeChildren();
//			this._loadContent();
//		}
	},

	/**
	 * Make sure the node with a given key path is available in the tree.
	 */
	_loadKeyPath: function(keyPath, callback) {
		var tree = this.tree;
		tree.logDebug("%s._loadKeyPath(%s)", this, keyPath);
		if(keyPath === ""){
			throw "Key path must not be empty";
		}
		var segList = keyPath.split(tree.options.keyPathSeparator);
		if(segList[0] === ""){
			throw "Key path must be relative (don't start with '/')";
		}
		var seg = segList.shift();

		for(var i=0, l=this.childList.length; i < l; i++){
			var child = this.childList[i];
			if( child.data.key === seg ){
				if(segList.length === 0) {
					// Found the end node
					callback.call(tree, child, "ok");

				}else if(child.data.isLazy && (child.childList === null || child.childList === undefined)){
					tree.logDebug("%s._loadKeyPath(%s) -> reloading %s...", this, keyPath, child);
					var self = this;
					child.reloadChildren(function(node, isOk){
						// After loading, look for direct child with that key
						if(isOk){
							tree.logDebug("%s._loadKeyPath(%s) -> reloaded %s.", node, keyPath, node);
							callback.call(tree, child, "loaded");
							node._loadKeyPath(segList.join(tree.options.keyPathSeparator), callback);
						}else{
							tree.logWarning("%s._loadKeyPath(%s) -> reloadChildren() failed.", self, keyPath);
							callback.call(tree, child, "error");
						}
					}); // Note: this line gives a JSLint warning (Don't make functions within a loop)
					// we can ignore it, since it will only be exectuted once, the the loop is ended
					// See also http://stackoverflow.com/questions/3037598/how-to-get-around-the-jslint-error-dont-make-functions-within-a-loop
				} else {
					callback.call(tree, child, "loaded");
					// Look for direct child with that key
					child._loadKeyPath(segList.join(tree.options.keyPathSeparator), callback);
				}
				return;
			}
		}
		// Could not find key
		tree.logWarning("Node not found: " + seg);
		return;
	},

	resetLazy: function() {
		// Discard lazy content.
		if( this.parent === null ){
			throw "Use tree.reload() instead";
		}else if( ! this.data.isLazy ){
			throw "node.resetLazy() requires lazy nodes.";
		}
		this.expand(false);
		this.removeChildren();
	},

	_addChildNode: function(dtnode, beforeNode) {
		/**
		 * Internal function to add one single DynatreeNode as a child.
		 *
		 */
		var tree = this.tree;
		var opts = tree.options;
		var pers = tree.persistence;

//		tree.logDebug("%s._addChildNode(%o)", this, dtnode);

		// --- Update and fix dtnode attributes if necessary
		dtnode.parent = this;
//		if( beforeNode && (beforeNode.parent !== this || beforeNode === dtnode ) )
//			throw "<beforeNode> must be another child of <this>";

		// --- Add dtnode as a child
		if ( this.childList === null ) {
			this.childList = [];
		} else if( ! beforeNode ) {
			// Fix 'lastsib'
			$(this.childList[this.childList.length-1].span).removeClass(opts.classNames.lastsib);
		}
		if( beforeNode ) {
			var iBefore = $.inArray(beforeNode, this.childList);
			if( iBefore < 0 ){
				throw "<beforeNode> must be a child of <this>";
			}
			this.childList.splice(iBefore, 0, dtnode);
//			alert(this.childList);
		} else {
			// Append node
			this.childList.push(dtnode);
		}

		// --- Handle persistence
		// Initial status is read from cookies, if persistence is active and
		// cookies are already present.
		// Otherwise the status is read from the data attributes and then persisted.
		var isInitializing = tree.isInitializing();
		if( opts.persist && pers.cookiesFound && isInitializing ) {
			// Init status from cookies
//			tree.logDebug("init from cookie, pa=%o, dk=%o", pers.activeKey, dtnode.data.key);
			if( pers.activeKey == dtnode.data.key ){
				tree.activeNode = dtnode;
			}
			if( pers.focusedKey == dtnode.data.key ){
				tree.focusNode = dtnode;
			}
			dtnode.bExpanded = ($.inArray(dtnode.data.key, pers.expandedKeyList) >= 0);
			dtnode.bSelected = ($.inArray(dtnode.data.key, pers.selectedKeyList) >= 0);
//			tree.logDebug("    key=%o, bSelected=%o", dtnode.data.key, dtnode.bSelected);
		} else {
			// Init status from data (Note: we write the cookies after the init phase)
//			tree.logDebug("init from data");
			if( dtnode.data.activate ) {
				tree.activeNode = dtnode;
				if( opts.persist ){
					pers.activeKey = dtnode.data.key;
				}
			}
			if( dtnode.data.focus ) {
				tree.focusNode = dtnode;
				if( opts.persist ){
					pers.focusedKey = dtnode.data.key;
				}
			}
			dtnode.bExpanded = ( dtnode.data.expand === true ); // Collapsed by default
			if( dtnode.bExpanded && opts.persist ){
				pers.addExpand(dtnode.data.key);
			}
			dtnode.bSelected = ( dtnode.data.select === true ); // Deselected by default
/*
			Doesn't work, cause pers.selectedKeyList may be null
			if( dtnode.bSelected && opts.selectMode==1
				&& pers.selectedKeyList && pers.selectedKeyList.length>0 ) {
				tree.logWarning("Ignored multi-selection in single-mode for %o", dtnode);
				dtnode.bSelected = false; // Fixing bad input data (multi selection for mode:1)
			}
*/
			if( dtnode.bSelected && opts.persist ){
				pers.addSelect(dtnode.data.key);
			}
		}

		// Always expand, if it's below minExpandLevel
//		tree.logDebug ("%s._addChildNode(%o), l=%o", this, dtnode, dtnode.getLevel());
		if ( opts.minExpandLevel >= dtnode.getLevel() ) {
//			tree.logDebug ("Force expand for %o", dtnode);
			this.bExpanded = true;
		}

		// In multi-hier mode, update the parents selection state
		// issue #82: only if not initializing, because the children may not exist yet
//		if( !dtnode.data.isStatusNode && opts.selectMode==3 && !isInitializing )
//			dtnode._fixSelectionState();

		// In multi-hier mode, update the parents selection state
		if( dtnode.bSelected && opts.selectMode==3 ) {
			var p = this;
			while( p ) {
				if( !p.hasSubSel ){
					p._setSubSel(true);
				}
				p = p.parent;
			}
		}
		// render this node and the new child
		if ( tree.bEnableUpdate ){
			this.render();
		}
		return dtnode;
	},

	addChild: function(obj, beforeNode) {
		/**
		 * Add a node object as child.
		 *
		 * This should be the only place, where a DynaTreeNode is constructed!
		 * (Except for the root node creation in the tree constructor)
		 *
		 * @param obj A JS object (may be recursive) or an array of those.
		 * @param {DynaTreeNode} beforeNode (optional) sibling node.
		 *
		 * Data format: array of node objects, with optional 'children' attributes.
		 * [
		 *	{ title: "t1", isFolder: true, ... }
		 *	{ title: "t2", isFolder: true, ...,
		 *		children: [
		 *			{title: "t2.1", ..},
		 *			{..}
		 *			]
		 *	}
		 * ]
		 * A simple object is also accepted instead of an array.
		 *
		 */
//		this.tree.logDebug("%s.addChild(%o, %o)", this, obj, beforeNode);
		if(typeof(obj) == "string"){
			throw "Invalid data type for " + obj;
		}else if( !obj || obj.length === 0 ){ // Passed null or undefined or empty array
			return;
		}else if( obj instanceof DynaTreeNode ){
			return this._addChildNode(obj, beforeNode);
		}

		if( !obj.length ){ // Passed a single data object
			obj = [ obj ];
		}
		var prevFlag = this.tree.enableUpdate(false);

		var tnFirst = null;
		for (var i=0, l=obj.length; i<l; i++) {
			var data = obj[i];
			var dtnode = this._addChildNode(new DynaTreeNode(this, this.tree, data), beforeNode);
			if( !tnFirst ){
				tnFirst = dtnode;
			}
			// Add child nodes recursively
			if( data.children ){
				dtnode.addChild(data.children, null);
			}
		}
		this.tree.enableUpdate(prevFlag);
		return tnFirst;
	},

	append: function(obj) {
		this.tree.logWarning("node.append() is deprecated (use node.addChild() instead).");
		return this.addChild(obj, null);
	},

	appendAjax: function(ajaxOptions) {
		var self = this;
		this.removeChildren(false, true);
		this.setLazyNodeStatus(DTNodeStatus_Loading);
		// Debug feature: force a delay, to simulate slow loading...
		if(ajaxOptions.debugLazyDelay){
			var ms = ajaxOptions.debugLazyDelay;
			ajaxOptions.debugLazyDelay = 0;
			this.tree.logInfo("appendAjax: waiting for debugLazyDelay " + ms);
			setTimeout(function(){self.appendAjax(ajaxOptions);}, ms);
			return;
		}
		// Ajax option inheritance: $.ajaxSetup < $.ui.dynatree.prototype.options.ajaxDefaults < tree.options.ajaxDefaults < ajaxOptions
		var orgSuccess = ajaxOptions.success;
		var orgError = ajaxOptions.error;
		var eventType = "nodeLoaded.dynatree." + this.tree.$tree.attr("id")
			+ "." + this.data.key;
		var options = $.extend({}, this.tree.options.ajaxDefaults, ajaxOptions, {
			success: function(data, textStatus){
				// <this> is the request options
//				self.tree.logDebug("appendAjax().success");
				var prevPhase = self.tree.phase;
				self.tree.phase = "init";
				// postProcess is similar to the standard dataFilter hook,
				// but it is also called for JSONP
				if( options.postProcess ){
					data = options.postProcess.call(this, data, this.dataType);
				}
				if(!Array.isArray(data) || data.length !== 0){
					self.addChild(data, null);
				}
				self.tree.phase = "postInit";
				if( orgSuccess ){
					orgSuccess.call(options, self);
				}
				self.tree.logInfo("trigger " + eventType);
				self.tree.$tree.trigger(eventType, [self, true]);
				self.tree.phase = prevPhase;
				// This should be the last command, so node.isLoading is true
				// while the callbacks run
				self.setLazyNodeStatus(DTNodeStatus_Ok);
				if(Array.isArray(data) && data.length === 0){
					// Set to [] which is interpreted as 'no children' for lazy
					// nodes
					self.childList = [];
					self.render();
				}
				},
			error: function(XMLHttpRequest, textStatus, errorThrown){
				// <this> is the request options
				self.tree.logWarning("appendAjax failed:", textStatus, ":\n", XMLHttpRequest, "\n", errorThrown);
				if( orgError ){
					orgError.call(options, self, XMLHttpRequest, textStatus, errorThrown);
				}
				self.tree.$tree.trigger(eventType, [self, false]);
				self.setLazyNodeStatus(DTNodeStatus_Error, {info: textStatus, tooltip: ""+errorThrown});
				}
		});
		$.ajax(options);
	},

	move: function(targetNode, mode) {
		/**Move this node to targetNode.
		 *  mode 'child': append this node as last child of targetNode.
		 *                This is the default. To be compatble with the D'n'd
		 *                hitMode, we also accept 'over'.
		 *  mode 'before': add this node as sibling before targetNode.
		 *  mode 'after': add this node as sibling after targetNode.
		 */
		var pos;
		if(this === targetNode){
			return;
		}
		if( !this.parent  ){
			throw "Cannot move system root";
		}
		if(mode === undefined || mode == "over"){
			mode = "child";
		}
		var prevParent = this.parent;
		var targetParent = (mode === "child") ? targetNode : targetNode.parent;
		if( targetParent.isDescendantOf(this) ){
			throw "Cannot move a node to it's own descendant";
		}
		// Unlink this node from current parent
		if( this.parent.childList.length == 1 ) {
			this.parent.childList = null;
			this.parent.bExpanded = false;
		} else {
			pos = $.inArray(this, this.parent.childList);
			if( pos < 0 ){
				throw "Internal error";
			}
			this.parent.childList.splice(pos, 1);
		}
		// Remove from source DOM parent
		this.parent.ul.removeChild(this.li);

		// Insert this node to target parent's child list
		this.parent = targetParent;
		if( targetParent.hasChildren() ) {
			switch(mode) {
			case "child":
				// Append to existing target children
				targetParent.childList.push(this);
				break;
			case "before":
				// Insert this node before target node
				pos = $.inArray(targetNode, targetParent.childList);
				if( pos < 0 ){
					throw "Internal error";
				}
				targetParent.childList.splice(pos, 0, this);
				break;
			case "after":
				// Insert this node after target node
				pos = $.inArray(targetNode, targetParent.childList);
				if( pos < 0 ){
					throw "Internal error";
				}
				targetParent.childList.splice(pos+1, 0, this);
				break;
			default:
				throw "Invalid mode " + mode;
			}
		} else {
			targetParent.childList = [ this ];
			// Parent has no <ul> tag yet:
			if( !targetParent.ul ) {
				// This is the parent's first child: create UL tag
				// (Hidden, because it will be
				targetParent.ul = document.createElement("ul");
				targetParent.ul.style.display = "none";
				targetParent.li.appendChild(targetParent.ul);
			}
		}
		// Add to target DOM parent
		targetParent.ul.appendChild(this.li);

		if( this.tree !== targetNode.tree ) {
			// Fix node.tree for all source nodes
			this.visit(function(node){
				node.tree = targetNode.tree;
			}, null, true);
			throw "Not yet implemented.";
		}
		// TODO: fix selection state
		// TODO: fix active state
		if( !prevParent.isDescendantOf(targetParent)) {
			prevParent.render();
		}
		if( !targetParent.isDescendantOf(prevParent) ) {
			targetParent.render();
		}
//		this.tree.redraw();
/*
		var tree = this.tree;
		var opts = tree.options;
		var pers = tree.persistence;


		// Always expand, if it's below minExpandLevel
//		tree.logDebug ("%s._addChildNode(%o), l=%o", this, dtnode, dtnode.getLevel());
		if ( opts.minExpandLevel >= dtnode.getLevel() ) {
//			tree.logDebug ("Force expand for %o", dtnode);
			this.bExpanded = true;
		}

		// In multi-hier mode, update the parents selection state
		// issue #82: only if not initializing, because the children may not exist yet
//		if( !dtnode.data.isStatusNode && opts.selectMode==3 && !isInitializing )
//			dtnode._fixSelectionState();

		// In multi-hier mode, update the parents selection state
		if( dtnode.bSelected && opts.selectMode==3 ) {
			var p = this;
			while( p ) {
				if( !p.hasSubSel )
					p._setSubSel(true);
				p = p.parent;
			}
		}
		// render this node and the new child
		if ( tree.bEnableUpdate )
			this.render();

		return dtnode;

*/
	},

	// --- end of class
	lastentry: undefined
};

/*************************************************************************
 * class DynaTreeStatus
 */

var DynaTreeStatus = Class.create();


DynaTreeStatus._getTreePersistData = function(cookieId, cookieOpts) {
	// Static member: Return persistence information from cookies
	var ts = new DynaTreeStatus(cookieId, cookieOpts);
	ts.read();
	return ts.toDict();
};
// Make available in global scope
getDynaTreePersistData = DynaTreeStatus._getTreePersistData;


DynaTreeStatus.prototype = {
	// Constructor
	initialize: function(cookieId, cookieOpts) {
		this._log("DynaTreeStatus: initialize");
		if( cookieId === undefined ){
			cookieId = $.ui.dynatree.prototype.options.cookieId;
		}
		cookieOpts = $.extend({}, $.ui.dynatree.prototype.options.cookie, cookieOpts);

		this.cookieId = cookieId;
		this.cookieOpts = cookieOpts;
		this.cookiesFound = undefined;
		this.activeKey = null;
		this.focusedKey = null;
		this.expandedKeyList = null;
		this.selectedKeyList = null;
	},
	// member functions
	_log: function(msg) {
		//	this.logDebug("_changeNodeList(%o): nodeList:%o, idx:%o", mode, nodeList, idx);
		Array.prototype.unshift.apply(arguments, ["debug"]);
		_log.apply(this, arguments);
	},
	read: function() {
		this._log("DynaTreeStatus: read");
		// Read or init cookies.
		this.cookiesFound = false;

		var cookie = $.cookie(this.cookieId + "-active");
		this.activeKey = ( cookie === null ) ? "" : cookie;
		if( cookie !== null ){
			this.cookiesFound = true;
		}
		cookie = $.cookie(this.cookieId + "-focus");
		this.focusedKey = ( cookie === null ) ? "" : cookie;
		if( cookie !== null ){
			this.cookiesFound = true;
		}
		cookie = $.cookie(this.cookieId + "-expand");
		this.expandedKeyList = ( cookie === null ) ? [] : cookie.split(",");
		if( cookie !== null ){
			this.cookiesFound = true;
		}
		cookie = $.cookie(this.cookieId + "-select");
		this.selectedKeyList = ( cookie === null ) ? [] : cookie.split(",");
		if( cookie !== null ){
			this.cookiesFound = true;
		}
	},
	write: function() {
		this._log("DynaTreeStatus: write");
		$.cookie(this.cookieId + "-active", ( this.activeKey === null ) ? "" : this.activeKey, this.cookieOpts);
		$.cookie(this.cookieId + "-focus", ( this.focusedKey === null ) ? "" : this.focusedKey, this.cookieOpts);
		$.cookie(this.cookieId + "-expand", ( this.expandedKeyList === null ) ? "" : this.expandedKeyList.join(","), this.cookieOpts);
		$.cookie(this.cookieId + "-select", ( this.selectedKeyList === null ) ? "" : this.selectedKeyList.join(","), this.cookieOpts);
	},
	addExpand: function(key) {
		this._log("addExpand(%o)", key);
		if( $.inArray(key, this.expandedKeyList) < 0 ) {
			this.expandedKeyList.push(key);
			$.cookie(this.cookieId + "-expand", this.expandedKeyList.join(","), this.cookieOpts);
		}
	},
	clearExpand: function(key) {
		this._log("clearExpand(%o)", key);
		var idx = $.inArray(key, this.expandedKeyList);
		if( idx >= 0 ) {
			this.expandedKeyList.splice(idx, 1);
			$.cookie(this.cookieId + "-expand", this.expandedKeyList.join(","), this.cookieOpts);
		}
	},
	addSelect: function(key) {
		this._log("addSelect(%o)", key);
		if( $.inArray(key, this.selectedKeyList) < 0 ) {
			this.selectedKeyList.push(key);
			$.cookie(this.cookieId + "-select", this.selectedKeyList.join(","), this.cookieOpts);
		}
	},
	clearSelect: function(key) {
		this._log("clearSelect(%o)", key);
		var idx = $.inArray(key, this.selectedKeyList);
		if( idx >= 0 ) {
			this.selectedKeyList.splice(idx, 1);
			$.cookie(this.cookieId + "-select", this.selectedKeyList.join(","), this.cookieOpts);
		}
	},
	isReloading: function() {
		return this.cookiesFound === true;
	},
	toDict: function() {
		return {
			cookiesFound: this.cookiesFound,
			activeKey: this.activeKey,
			focusedKey: this.activeKey,
			expandedKeyList: this.expandedKeyList,
			selectedKeyList: this.selectedKeyList
		};
	},
	// --- end of class
	lastentry: undefined
};


/*************************************************************************
 * class DynaTree
 */

var DynaTree = Class.create();

// --- Static members ----------------------------------------------------------

DynaTree.version = "$Version: 1.1.0$";
/*
DynaTree._initTree = function() {
};

DynaTree._bind = function() {
};
*/
//--- Class members ------------------------------------------------------------

DynaTree.prototype = {
	// Constructor
//	initialize: function(divContainer, options) {
	initialize: function($widget) {
		// instance members
		this.phase = "init";
		this.$widget = $widget;
		this.options = $widget.options;
		this.$tree = $widget.element;
		this.timer = null;
		// find container element
		this.divTree = this.$tree.get(0);

//		var parentPos = $(this.divTree).parent().offset();
//		this.parentTop = parentPos.top;
//		this.parentLeft = parentPos.left;

		_initDragAndDrop(this);
	},

	// member functions

	_load: function(callback) {
		var $widget = this.$widget;
		var opts = this.options;
		this.bEnableUpdate = true;
		this._nodeCount = 1;
		this.activeNode = null;
		this.focusNode = null;

		// Some deprecation warnings to help with migration
		if( opts.rootVisible !== undefined ){
			_log("warn", "Option 'rootVisible' is no longer supported.");
		}
//		if( opts.title  !== undefined ){
//			_log("warn", "Option 'title' is no longer supported.");
//		}
		if( opts.minExpandLevel < 1 ) {
			_log("warn", "Option 'minExpandLevel' must be >= 1.");
			opts.minExpandLevel = 1;
		}
//		_log("warn", "jQuery.support.boxModel " + jQuery.support.boxModel);

		// If a 'options.classNames' dictionary was passed, still use defaults
		// for undefined classes:
		if( opts.classNames !== $.ui.dynatree.prototype.options.classNames ) {
			opts.classNames = $.extend({}, $.ui.dynatree.prototype.options.classNames, opts.classNames);
		}
		if( opts.ajaxDefaults !== $.ui.dynatree.prototype.options.ajaxDefaults ) {
			opts.ajaxDefaults = $.extend({}, $.ui.dynatree.prototype.options.ajaxDefaults, opts.ajaxDefaults);
		}
		if( opts.dnd !== $.ui.dynatree.prototype.options.dnd ) {
			opts.dnd = $.extend({}, $.ui.dynatree.prototype.options.dnd, opts.dnd);
		}
		// Guess skin path, if not specified
		if(!opts.imagePath) {
			$("script").each( function () {
				var _rexDtLibName = /.*dynatree[^\/]*\.js$/i;
				if( this.src.search(_rexDtLibName) >= 0 ) {
					if( this.src.indexOf("/")>=0 ){ // issue #47
						opts.imagePath = this.src.slice(0, this.src.lastIndexOf("/")) + "/skin/";
					}else{
						opts.imagePath = "skin/";
					}
					logMsg("Guessing imagePath from '%s': '%s'", this.src, opts.imagePath);
					return false; // first match
				}
			});
		}

		this.persistence = new DynaTreeStatus(opts.cookieId, opts.cookie);
		if( opts.persist ) {
			if( !$.cookie ){
				_log("warn", "Please include jquery.cookie.js to use persistence.");
			}
			this.persistence.read();
		}
		this.logDebug("DynaTree.persistence: %o", this.persistence.toDict());

		// Cached tag strings
		this.cache = {
			tagEmpty: "<span class='" + opts.classNames.empty + "'></span>",
			tagVline: "<span class='" + opts.classNames.vline + "'></span>",
			tagExpander: "<span class='" + opts.classNames.expander + "'></span>",
			tagConnector: "<span class='" + opts.classNames.connector + "'></span>",
			tagNodeIcon: "<span class='" + opts.classNames.nodeIcon + "'></span>",
			tagCheckbox: "<span class='" + opts.classNames.checkbox + "'></span>",
			lastentry: undefined
		};

		// Clear container, in case it contained some 'waiting' or 'error' text
		// for clients that don't support JS.
		// We don't do this however, if we try to load from an embedded UL element.
		if( opts.children || (opts.initAjax && opts.initAjax.url) || opts.initId ){
			$(this.divTree).empty();
		}else if( this.divRoot ){
			$(this.divRoot).remove();
		}
/*
		// create the root element
		this.tnRoot = new DynaTreeNode(null, this, {title: opts.title, key: "root"});
		this.tnRoot.data.isFolder = true;
		this.tnRoot.render(false, false);
		this.divRoot = this.tnRoot.div;
		this.divRoot.className = opts.classNames.container;

		// add root to container
		// TODO: this should be delayed until all children have been created for performance reasons
		this.divTree.appendChild(this.divRoot);
*/
		// Create the root element
		this.tnRoot = new DynaTreeNode(null, this, {});
		this.tnRoot.bExpanded = true;
		this.tnRoot.render();
		this.divTree.appendChild(this.tnRoot.ul);

		var root = this.tnRoot;
		var isReloading = ( opts.persist && this.persistence.isReloading() );
		var isLazy = false;
		var prevFlag = this.enableUpdate(false);

		this.logDebug("Dynatree._load(): read tree structure...");

		// Init tree structure
		if( opts.children ) {
			// Read structure from node array
			root.addChild(opts.children);

		} else if( opts.initAjax && opts.initAjax.url ) {
			// Init tree from AJAX request
			isLazy = true;
			root.data.isLazy = true;
			this._reloadAjax(callback);

		} else if( opts.initId ) {
			// Init tree from another UL element
			this._createFromTag(root, $("#"+opts.initId));

		} else {
			// Init tree from the first UL element inside the container <div>
			var $ul = this.$tree.find(">ul:first").hide();
			this._createFromTag(root, $ul);
			$ul.remove();
		}

		this._checkConsistency();
		// Render html markup
		this.logDebug("Dynatree._load(): render nodes...");
		this.enableUpdate(prevFlag);

		// bind event handlers
		this.logDebug("Dynatree._load(): bind events...");
		this.$widget.bind();

		// --- Post-load processing
		this.logDebug("Dynatree._load(): postInit...");
		this.phase = "postInit";

		// In persist mode, make sure that cookies are written, even if they are empty
		if( opts.persist ) {
			this.persistence.write();
		}
		// Set focus, if possible (this will also fire an event and write a cookie)
		if( this.focusNode && this.focusNode.isVisible() ) {
			this.logDebug("Focus on init: %o", this.focusNode);
			this.focusNode.focus();
		}
		if( !isLazy && opts.onPostInit ) {
			opts.onPostInit.call(this, isReloading, false);
		}
		this.phase = "idle";
	},

//	_setNoUpdate: function(silent) {
//		// TODO: set options to disable and re-enable updates while loading
//		var opts = this.options;
//		var prev = {
//			fx: opts.fx,
//			autoFocus: opts.autoFocus,
//			autoCollapse: opts.autoCollapse };
//		if(silent === true){
//			opts.autoFocus = false;
//			opts.fx = null;
//			opts.autoCollapse = false;
//		} else {
//			opts.autoFocus = silent.autoFocus;
//			opts.fx = silent.fx;
//			opts.autoCollapse = silent.autoCollapse;
//		}
//		return prev;
//	},

	_reloadAjax: function(callback) {
		// Reload
		var opts = this.options;
		if( ! opts.initAjax || ! opts.initAjax.url ){
			throw "tree.reload() requires 'initAjax' mode.";
		}
		var pers = this.persistence;
		var ajaxOpts = $.extend({}, opts.initAjax);
		// Append cookie info to the request
//		this.logDebug("reloadAjax: key=%o, an.key:%o", pers.activeKey, this.activeNode?this.activeNode.data.key:"?");
		if( ajaxOpts.addActiveKey ){
			ajaxOpts.data.activeKey = pers.activeKey;
		}
		if( ajaxOpts.addFocusedKey ){
			ajaxOpts.data.focusedKey = pers.focusedKey;
		}
		if( ajaxOpts.addExpandedKeyList ){
			ajaxOpts.data.expandedKeyList = pers.expandedKeyList.join(",");
		}
		if( ajaxOpts.addSelectedKeyList ){
			ajaxOpts.data.selectedKeyList = pers.selectedKeyList.join(",");
		}
		// Set up onPostInit callback to be called when Ajax returns
		if( opts.onPostInit ) {
			if( ajaxOpts.success ){
				this.logWarning("initAjax: success callback is ignored when onPostInit was specified.");
			}
			if( ajaxOpts.error ){
				this.logWarning("initAjax: error callback is ignored when onPostInit was specified.");
			}
			var isReloading = pers.isReloading();
			ajaxOpts.success = function(dtnode) {
				opts.onPostInit.call(dtnode.tree, isReloading, false);
				if(callback){
					callback.call(dtnode.tree, "ok");
				}
			};
			ajaxOpts.error = function(dtnode) {
				opts.onPostInit.call(dtnode.tree, isReloading, true);
				if(callback){
					callback.call(dtnode.tree, "error");
				}
			};
		}
		this.logDebug("Dynatree._init(): send Ajax request...");
		this.tnRoot.appendAjax(ajaxOpts);
	},

	toString: function() {
//		return "DynaTree '" + this.options.title + "'";
		return "Dynatree '" + this.$tree.attr("id") + "'";
	},

	toDict: function() {
		return this.tnRoot.toDict(true);
	},

	serializeArray: function(stopOnParents) {
		// Return a JavaScript array of objects, ready to be encoded as a JSON
		// string for selected nodes
		var nodeList = this.getSelectedNodes(stopOnParents),
			name = this.$tree.attr("name") || this.$tree.attr("id"),
			arr = [];
		for(var i=0, l=nodeList.length; i<l; i++){
			arr.push({name: name, value: nodeList[i].data.key});
		}
		return arr;
	},

	getPersistData: function() {
		return this.persistence.toDict();
	},

	logDebug: function(msg) {
		if( this.options.debugLevel >= 2 ) {
			Array.prototype.unshift.apply(arguments, ["debug"]);
			_log.apply(this, arguments);
		}
	},

	logInfo: function(msg) {
		if( this.options.debugLevel >= 1 ) {
			Array.prototype.unshift.apply(arguments, ["info"]);
			_log.apply(this, arguments);
		}
	},

	logWarning: function(msg) {
		Array.prototype.unshift.apply(arguments, ["warn"]);
		_log.apply(this, arguments);
	},

	isInitializing: function() {
		return ( this.phase=="init" || this.phase=="postInit" );
	},
	isReloading: function() {
		return ( this.phase=="init" || this.phase=="postInit" ) && this.options.persist && this.persistence.cookiesFound;
	},
	isUserEvent: function() {
		return ( this.phase=="userEvent" );
	},

	redraw: function() {
//		this.logDebug("dynatree.redraw()...");
		this.tnRoot.render(false, false);
//		this.logDebug("dynatree.redraw() done.");
	},
	renderInvisibleNodes: function() {
		this.tnRoot.render(false, true);
	},
	reload: function(callback) {
		this._load(callback);
	},

	getRoot: function() {
		return this.tnRoot;
	},

	enable: function() {
		this.$widget.enable();
	},

	disable: function() {
		this.$widget.disable();
	},

	getNodeByKey: function(key) {
		// Search the DOM by element ID (assuming this is faster than traversing all nodes).
		// $("#...") has problems, if the key contains '.', so we use getElementById()
		var el = document.getElementById(this.options.idPrefix + key);
		if( el ){
			return el.dtnode ? el.dtnode : null;
		}
		// Not found in the DOM, but still may be in an unrendered part of tree
		var match = null;
		this.visit(function(node){
//			window.console.log("%s", node);
			if(node.data.key == key) {
				match = node;
				return false;
			}
		}, true);
		return match;
	},

	getActiveNode: function() {
		return this.activeNode;
	},

	reactivate: function(setFocus) {
		// Re-fire onQueryActivate and onActivate events.
		var node = this.activeNode;
//		this.logDebug("reactivate %o", node);
		if( node ) {
			this.activeNode = null; // Force re-activating
			node.activate();
			if( setFocus ){
				node.focus();
			}
		}
	},

	getSelectedNodes: function(stopOnParents) {
		var nodeList = [];
		this.tnRoot.visit(function(node){
			if( node.bSelected ) {
				nodeList.push(node);
				if( stopOnParents === true ){
					return "skip"; // stop processing this branch
				}
			}
		});
		return nodeList;
	},

	activateKey: function(key) {
		var dtnode = (key === null) ? null : this.getNodeByKey(key);
		if( !dtnode ) {
			if( this.activeNode ){
				this.activeNode.deactivate();
			}
			this.activeNode = null;
			return null;
		}
		dtnode.focus();
		dtnode.activate();
		return dtnode;
	},

	loadKeyPath: function(keyPath, callback) {
		var segList = keyPath.split(this.options.keyPathSeparator);
		// Remove leading '/'
		if(segList[0] === ""){
			segList.shift();
		}
		// Remove leading system root key
		if(segList[0] == this.tnRoot.data.key){
			this.logDebug("Removed leading root key.");
			segList.shift();
		}
		keyPath = segList.join(this.options.keyPathSeparator);
		return this.tnRoot._loadKeyPath(keyPath, callback);
	},

	selectKey: function(key, select) {
		var dtnode = this.getNodeByKey(key);
		if( !dtnode ){
			return null;
		}
		dtnode.select(select);
		return dtnode;
	},

	enableUpdate: function(bEnable) {
		if ( this.bEnableUpdate==bEnable ){
			return bEnable;
		}
		this.bEnableUpdate = bEnable;
		if ( bEnable ){
			this.redraw();
		}
		return !bEnable; // return previous value
	},

	count: function() {
		return this.tnRoot.countChildren();
	},

	visit: function(fn, includeRoot) {
		return this.tnRoot.visit(fn, includeRoot);
	},

	_createFromTag: function(parentTreeNode, $ulParent) {
		// Convert a <UL>...</UL> list into children of the parent tree node.
		var self = this;
/*
TODO: better?
		this.$lis = $("li:has(a[href])", this.element);
		this.$tabs = this.$lis.map(function() { return $("a", this)[0]; });
 */
		$ulParent.find(">li").each(function() {
			var $li = $(this);
			var $liSpan = $li.find(">span:first");
			var title;
			if( $liSpan.length ) {
				// If a <li><span> tag is specified, use it literally.
				title = $liSpan.html();
			} else {
				// If only a <li> tag is specified, use the trimmed string up to the next child <ul> tag.
				title = $li.html();
				var iPos = title.search(/<ul/i);
				if( iPos>=0 ){
					title = title.substring(0, iPos).trim();
				}else{
					title = title.trim();
				}
//				self.logDebug("%o", title);
			}
			// Parse node options from ID, title and class attributes
			var data = {
				title: title,
				isFolder: $li.hasClass("folder"),
				isLazy: $li.hasClass("lazy"),
				expand: $li.hasClass("expanded"),
				select: $li.hasClass("selected"),
				activate: $li.hasClass("active"),
				focus: $li.hasClass("focused"),
				noLink: $li.hasClass("noLink")
			};
			if( $li.attr("title") ){
				data.tooltip = $li.attr("title");
			}
			if( $li.attr("id") ){
				data.key = $li.attr("id");
			}
			// If a data attribute is present, evaluate as a JavaScript object
			if( $li.attr("data") ) {
				var dataAttr = $li.attr("data").trim();
				if( dataAttr ) {
					if( dataAttr.charAt(0) !== "{" ){
						dataAttr = "{" + dataAttr + "}";
					}
					try {
						$.extend(data, eval("(" + dataAttr + ")"));
					} catch(e) {
						throw ("Error parsing node data: " + e + "\ndata:\n'" + dataAttr + "'");
					}
				}
			}
			var childNode = parentTreeNode.addChild(data);
			// Recursive reading of child nodes, if LI tag contains an UL tag
			var $ul = $li.find(">ul:first");
			if( $ul.length ) {
				self._createFromTag(childNode, $ul); // must use 'self', because 'this' is the each() context
			}
		});
	},

	_checkConsistency: function() {
//		this.logDebug("tree._checkConsistency() NOT IMPLEMENTED - %o", this);
	},

	_setDndStatus: function(sourceNode, targetNode, helper, hitMode, accept) {
		// hitMode: 'after', 'before', 'over', 'out', 'start', 'stop'
		var $source = sourceNode ? $(sourceNode.span) : null;
		var $target = $(targetNode.span);
		if( !this.$dndMarker ) {
			this.$dndMarker = $("<div id='dynatree-drop-marker'></div>")
				.hide()
				.prependTo($(this.divTree).parent());
//				.prependTo("body");
//			logMsg("Creating marker: %o", this.$dndMarker);
		}
/*
		if(hitMode === "start"){
		}
		if(hitMode === "stop"){
//			sourceNode.removeClass("dynatree-drop-target");
		}
*/
//		this.$dndMarker.attr("class", hitMode);
		if(hitMode === "after" || hitMode === "before" || hitMode === "over"){
//			$source && $source.addClass("dynatree-drag-source");
			var pos = $target.position();
			switch(hitMode){
			case "before":
				this.$dndMarker.removeClass("dynatree-drop-after dynatree-drop-over");
				this.$dndMarker.addClass("dynatree-drop-before");
				pos.top -= 8;
				break;
			case "after":
				this.$dndMarker.removeClass("dynatree-drop-before dynatree-drop-over");
				this.$dndMarker.addClass("dynatree-drop-after");
				pos.top += 8;
				break;
			default:
				this.$dndMarker.removeClass("dynatree-drop-after dynatree-drop-before");
				this.$dndMarker.addClass("dynatree-drop-over");
				$target.addClass("dynatree-drop-target");
				pos.left += 8;
			}
			this.$dndMarker.css({"left": (pos.left) + "px", "top": (pos.top) + "px" })
				.show();
//			helper.addClass("dynatree-drop-hover");
		} else {
//			$source && $source.removeClass("dynatree-drag-source");
			$target.removeClass("dynatree-drop-target");
			this.$dndMarker.hide();
//			helper.removeClass("dynatree-drop-hover");
		}
		if(hitMode === "after"){
			$target.addClass("dynatree-drop-after");
		} else {
			$target.removeClass("dynatree-drop-after");
		}
		if(hitMode === "before"){
			$target.addClass("dynatree-drop-before");
		} else {
			$target.removeClass("dynatree-drop-before");
		}
		if(accept === true){
			if($source){
				$source.addClass("dynatree-drop-accept");
			}
			$target.addClass("dynatree-drop-accept");
			helper.addClass("dynatree-drop-accept");
		}else{
			if($source){
				$source.removeClass("dynatree-drop-accept");
			}
			$target.removeClass("dynatree-drop-accept");
			helper.removeClass("dynatree-drop-accept");
		}
		if(accept === false){
			if($source){
				$source.addClass("dynatree-drop-reject");
			}
			$target.addClass("dynatree-drop-reject");
			helper.addClass("dynatree-drop-reject");
		}else{
			if($source){
				$source.removeClass("dynatree-drop-reject");
			}
			$target.removeClass("dynatree-drop-reject");
			helper.removeClass("dynatree-drop-reject");
		}
	},

	_onDragEvent: function(eventName, node, otherNode, event, ui, draggable) {
		/**
		 * Handles drag'n'drop functionality.
		 *
		 * A standard jQuery drag-and-drop process may generate these calls:
		 *
		 * draggable helper():
		 *     _onDragEvent("helper", sourceNode, null, event, null, null);
		 * start:
		 *     _onDragEvent("start", sourceNode, null, event, ui, draggable);
		 * drag:
		 *     _onDragEvent("leave", prevTargetNode, sourceNode, event, ui, draggable);
		 *     _onDragEvent("over", targetNode, sourceNode, event, ui, draggable);
		 *     _onDragEvent("enter", targetNode, sourceNode, event, ui, draggable);
		 * stop:
		 *     _onDragEvent("drop", targetNode, sourceNode, event, ui, draggable);
		 *     _onDragEvent("leave", targetNode, sourceNode, event, ui, draggable);
		 *     _onDragEvent("stop", sourceNode, null, event, ui, draggable);
		 */
//		if(eventName !== "over"){
//			this.logDebug("tree._onDragEvent(%s, %o, %o) - %o", eventName, node, otherNode, this);
//		}
		var opts = this.options;
		var dnd = this.options.dnd;
		var res = null;
		var nodeTag = $(node.span);
		var hitMode;

		switch (eventName) {
		case "helper":
			// Only event and node argument is available
			var helper = $("<div class='dynatree-drag-helper'><span class='dynatree-drag-helper-img' /></div>")
				.append($(event.target).closest('a').clone());
			// Attach node reference to helper object
			helper.data("dtSourceNode", node);
//			this.logDebug("helper.sourceNode=%o", helper.data("dtSourceNode"));
			res = helper;
			break;
		case "start":
			if(node.isStatusNode()) {
				res = false;
			} else if(dnd.onDragStart) {
				res = dnd.onDragStart(node);
			}
			if(res === false) {
				this.logDebug("tree.onDragStart() cancelled");
				//draggable._clear();
				// NOTE: the return value seems to be ignored (drag is not canceled, when false is returned)
				ui.helper.trigger("mouseup");
				ui.helper.hide();
			} else {
				nodeTag.addClass("dynatree-drag-source");
			}
			break;
		case "enter":
			res = dnd.onDragEnter ? dnd.onDragEnter(node, otherNode) : null;
			res = {
				over: (res !== false) && ((res === true) || (res === "over") || $.inArray("over", res) >= 0),
				before: (res !== false) && ((res === true) || (res === "before") || $.inArray("before", res) >= 0),
				after: (res !== false) && ((res === true) || (res === "after") || $.inArray("after", res) >= 0)
			};
			ui.helper.data("enterResponse", res);
//			this.logDebug("helper.enterResponse: %o", res);
			break;
		case "over":
			var enterResponse = ui.helper.data("enterResponse");
			hitMode = null;
			if(enterResponse === false){
				// Don't call onDragOver if onEnter returned false.
				break;
			} else if(typeof enterResponse === "string") {
				// Use hitMode from onEnter if provided.
				hitMode = enterResponse;
			} else {
				// Calculate hitMode from relative cursor position.
				var nodeOfs = nodeTag.offset();
//				var relPos = { x: event.clientX - nodeOfs.left,
//							y: event.clientY - nodeOfs.top };
//				nodeOfs.top += this.parentTop;
//				nodeOfs.left += this.parentLeft;
				var relPos = { x: event.pageX - nodeOfs.left,
							   y: event.pageY - nodeOfs.top };
				var relPos2 = { x: relPos.x / nodeTag.width(),
								y: relPos.y / nodeTag.height() };
//				this.logDebug("event.page: %s/%s", event.pageX, event.pageY);
//				this.logDebug("event.client: %s/%s", event.clientX, event.clientY);
//				this.logDebug("nodeOfs: %s/%s", nodeOfs.left, nodeOfs.top);
////				this.logDebug("parent: %s/%s", this.parentLeft, this.parentTop);
//				this.logDebug("relPos: %s/%s", relPos.x, relPos.y);
//				this.logDebug("relPos2: %s/%s", relPos2.x, relPos2.y);
				if( enterResponse.after && relPos2.y > 0.75 ){
					hitMode = "after";
				} else if(!enterResponse.over && enterResponse.after && relPos2.y > 0.5 ){
					hitMode = "after";
				} else if(enterResponse.before && relPos2.y <= 0.25) {
					hitMode = "before";
				} else if(!enterResponse.over && enterResponse.before && relPos2.y <= 0.5) {
					hitMode = "before";
				} else if(enterResponse.over) {
					hitMode = "over";
				}
				// Prevent no-ops like 'before source node'
				// TODO: these are no-ops when moving nodes, but not in copy mode
				if( dnd.preventVoidMoves ){
					if(node === otherNode){
//						this.logDebug("    drop over source node prevented");
						hitMode = null;
					}else if(hitMode === "before" && otherNode && node === otherNode.getNextSibling()){
//						this.logDebug("    drop after source node prevented");
						hitMode = null;
					}else if(hitMode === "after" && otherNode && node === otherNode.getPrevSibling()){
//						this.logDebug("    drop before source node prevented");
						hitMode = null;
					}else if(hitMode === "over" && otherNode
							&& otherNode.parent === node && otherNode.isLastSibling() ){
//						this.logDebug("    drop last child over own parent prevented");
						hitMode = null;
					}
				}
//				this.logDebug("hitMode: %s - %s - %s", hitMode, (node.parent === otherNode), node.isLastSibling());
				ui.helper.data("hitMode", hitMode);
			}
			// Auto-expand node (only when 'over' the node, not 'before', or 'after')
			if(hitMode === "over"
				&& dnd.autoExpandMS && node.hasChildren() !== false && !node.bExpanded) {
				node.scheduleAction("expand", dnd.autoExpandMS);
			}
			if(hitMode && dnd.onDragOver){
				res = dnd.onDragOver(node, otherNode, hitMode);
			}
			this._setDndStatus(otherNode, node, ui.helper, hitMode, res!==false);
			break;
		case "drop":
			hitMode = ui.helper.data("hitMode");
			if(hitMode && dnd.onDrop){
				dnd.onDrop(node, otherNode, hitMode, ui, draggable);
			}
			break;
		case "leave":
			// Cancel pending expand request
			node.scheduleAction("cancel");
			ui.helper.data("enterResponse", null);
			ui.helper.data("hitMode", null);
			this._setDndStatus(otherNode, node, ui.helper, "out", undefined);
			if(dnd.onDragLeave){
				dnd.onDragLeave(node, otherNode);
			}
			break;
		case "stop":
			nodeTag.removeClass("dynatree-drag-source");
			if(dnd.onDragStop){
				dnd.onDragStop(node);
			}
			break;
		default:
			throw "Unsupported drag event: " + eventName;
		}
		return res;
	},

	cancelDrag: function() {
		 var dd = $.ui.ddmanager.current;
		 if(dd){
			 dd.cancel();
		 }
	},

	// --- end of class
	lastentry: undefined
};

/*************************************************************************
 * Widget $(..).dynatree
 */

$.widget("ui.dynatree", {
/*
	init: function() {
		// ui.core 1.6 renamed init() to _init(): this stub assures backward compatibility
		_log("warn", "ui.dynatree.init() was called; you should upgrade to jquery.ui.core.js v1.8 or higher.");
		return this._init();
	},
 */
	_init: function() {
		if( parseFloat($.ui.version) < 1.8 ) {
			// jquery.ui.core 1.8 renamed _init() to _create(): this stub assures backward compatibility
			_log("warn", "ui.dynatree._init() was called; you should upgrade to jquery.ui.core.js v1.8 or higher.");
			return this._create();
		}
		// jquery.ui.core 1.8 still uses _init() to perform "default functionality"
		_log("debug", "ui.dynatree._init() was called; no current default functionality.");
	},

	_create: function() {
		logMsg("Dynatree._create(): version='%s', debugLevel=%o.", DynaTree.version, this.options.debugLevel);

		var opts = this.options;
		// The widget framework supplies this.element and this.options.
		this.options.event += ".dynatree"; // namespace event

		var divTree = this.element.get(0);
/*		// Clear container, in case it contained some 'waiting' or 'error' text
		// for clients that don't support JS
		if( opts.children || (opts.initAjax && opts.initAjax.url) || opts.initId )
			$(divTree).empty();
*/
		// Create the DynaTree object
		this.tree = new DynaTree(this);
		this.tree._load();
		this.tree.logDebug("Dynatree._init(): done.");
	},

	bind: function() {
		// Prevent duplicate binding
		this.unbind();

		var eventNames = "click.dynatree dblclick.dynatree";
		if( this.options.keyboard ){
			// Note: leading ' '!
			eventNames += " keypress.dynatree keydown.dynatree";
		}
		this.element.on(eventNames, function(event){
			var dtnode = getDtNodeFromElement(event.target);
			if( !dtnode ){
				return true;  // Allow bubbling of other events
			}
			var tree = dtnode.tree;
			var o = tree.options;
			tree.logDebug("event(%s): dtnode: %s", event.type, dtnode);
			var prevPhase = tree.phase;
			tree.phase = "userEvent";
			try {
				switch(event.type) {
				case "click":
					return ( o.onClick && o.onClick.call(tree, dtnode, event)===false ) ? false : dtnode._onClick(event);
				case "dblclick":
					return ( o.onDblClick && o.onDblClick.call(tree, dtnode, event)===false ) ? false : dtnode._onDblClick(event);
				case "keydown":
					return ( o.onKeydown && o.onKeydown.call(tree, dtnode, event)===false ) ? false : dtnode._onKeydown(event);
				case "keypress":
					return ( o.onKeypress && o.onKeypress.call(tree, dtnode, event)===false ) ? false : dtnode._onKeypress(event);
				}
			} catch(e) {
				var _ = null; // issue 117
				tree.logWarning("bind(%o): dtnode: %o, error: %o", event, dtnode, e);
			} finally {
				tree.phase = prevPhase;
			}
		});

		// focus/blur don't bubble, i.e. are not delegated to parent <div> tags,
		// so we use the addEventListener capturing phase.
		// See http://www.howtocreate.co.uk/tutorials/javascript/domevents
		function __focusHandler(event) {
			// Handles blur and focus.
			// Fix event for IE:
			// doesn't pass JSLint:
//			event = arguments[0] = $.event.fix( event || window.event );
			// what jQuery does:
//			var args = jQuery.makeArray( arguments );
//			event = args[0] = jQuery.event.fix( event || window.event );
			if(typeof event == "undefined" && window.event == null)
				return false;
			event = $.event.fix( event || window.event );
			var dtnode = getDtNodeFromElement(event.target);
			return dtnode ? dtnode._onFocus(event) : false;
		}
		var div = this.tree.divTree;
		if( div.addEventListener ) {
			div.addEventListener("focus", __focusHandler, true);
			div.addEventListener("blur", __focusHandler, true);
		} else {
			div.onfocusin = div.onfocusout = __focusHandler;
		}
		// EVENTS
		// disable click if event is configured to something else
//		if (!(/^click/).test(o.event))
//			this.$tabs.on("click.tabs", function() { return false; });

	},

	unbind: function() {
		this.element.off(".dynatree");
	},

/* TODO: we could handle option changes during runtime here (maybe to re-render, ...)
	setData: function(key, value) {
		this.tree.logDebug("dynatree.setData('" + key + "', '" + value + "')");
	},
*/
	enable: function() {
		this.on();
		// Call default disable(): remove -disabled from css:
		$.Widget.prototype.enable.apply(this, arguments);
	},

	disable: function() {
		this.off();
		// Call default disable(): add -disabled to css:
		$.Widget.prototype.disable.apply(this, arguments);
	},

	// --- getter methods (i.e. NOT returning a reference to $)
	getTree: function() {
		return this.tree;
	},

	getRoot: function() {
		return this.tree.getRoot();
	},

	getActiveNode: function() {
		return this.tree.getActiveNode();
	},

	getSelectedNodes: function() {
		return this.tree.getSelectedNodes();
	},

	// ------------------------------------------------------------------------
	lastentry: undefined
});


// The following methods return a value (thus breaking the jQuery call chain):
if( parseFloat($.ui.version) < 1.8 ) {
	$.ui.dynatree.getter = "getTree getRoot getActiveNode getSelectedNodes";
}


/*******************************************************************************
 * Plugin default options:
 */
$.ui.dynatree.prototype.options = {
	title: "Dynatree", // Tree's name (only used for debug outpu)
	minExpandLevel: 1, // 1: root node is not collapsible
	imagePath: null, // Path to a folder containing icons. Defaults to 'skin/' subdirectory.
	children: null, // Init tree structure from this object array.
	initId: null, // Init tree structure from a <ul> element with this ID.
	initAjax: null, // Ajax options used to initialize the tree strucuture.
	autoFocus: true, // Set focus to first child, when expanding or lazy-loading.
	keyboard: true, // Support keyboard navigation.
	persist: false, // Persist expand-status to a cookie
	autoCollapse: false, // Automatically collapse all siblings, when a node is expanded.
	clickFolderMode: 3, // 1:activate, 2:expand, 3:activate and expand
	activeVisible: true, // Make sure, active nodes are visible (expanded).
	checkbox: false, // Show checkboxes.
	selectMode: 2, // 1:single, 2:multi, 3:multi-hier
	fx: null, // Animations, e.g. null or { height: "toggle", duration: 200 }
	noLink: false, // Use <span> instead of <a> tags for all nodes
	// Low level event handlers: onEvent(dtnode, event): return false, to stop default processing
	onClick: null, // null: generate focus, expand, activate, select events.
	onDblClick: null, // (No default actions.)
	onKeydown: null, // null: generate keyboard navigation (focus, expand, activate).
	onKeypress: null, // (No default actions.)
	onFocus: null, // null: set focus to node.
	onBlur: null, // null: remove focus from node.

	// Pre-event handlers onQueryEvent(flag, dtnode): return false, to stop processing
	onQueryActivate: null, // Callback(flag, dtnode) before a node is (de)activated.
	onQuerySelect: null, // Callback(flag, dtnode) before a node is (de)selected.
	onQueryExpand: null, // Callback(flag, dtnode) before a node is expanded/collpsed.

	// High level event handlers
	onPostInit: null, // Callback(isReloading, isError) when tree was (re)loaded.
	onActivate: null, // Callback(dtnode) when a node is activated.
	onDeactivate: null, // Callback(dtnode) when a node is deactivated.
	onSelect: null, // Callback(flag, dtnode) when a node is (de)selected.
	onExpand: null, // Callback(dtnode) when a node is expanded/collapsed.
	onLazyRead: null, // Callback(dtnode) when a lazy node is expanded for the first time.
	onCustomRender: null, // Callback(dtnode) before a node is rendered. Return a HTML string to override.
	onRender: null, // Callback(dtnode, nodeSpan) after a node was rendered.

	// Drag'n'drop support
	dnd: {
		// Make tree nodes draggable:
		onDragStart: null, // Callback(sourceNode), return true, to enable dnd
		onDragStop: null, // Callback(sourceNode)
//		helper: null,
		// Make tree nodes accept draggables
		autoExpandMS: 1000, // Expand nodes after n milliseconds of hovering.
		preventVoidMoves: true, // Prevent dropping nodes 'before self', etc.
		onDragEnter: null, // Callback(targetNode, sourceNode)
		onDragOver: null, // Callback(targetNode, sourceNode, hitMode)
		onDrop: null, // Callback(targetNode, sourceNode, hitMode)
		onDragLeave: null // Callback(targetNode, sourceNode)
	},
	ajaxDefaults: { // Used by initAjax option
		cache: false, // false: Append random '_' argument to the request url to prevent caching.
		dataType: "json" // Expect json format and pass json object to callbacks.
	},
	strings: {
		loading: "Loading&#8230;",
		loadError: "Load error!"
	},
	generateIds: false, // Generate id attributes like <span id='dynatree-id-KEY'>
	idPrefix: "dynatree-id-", // Used to generate node id's like <span id="dynatree-id-<key>">.
	keyPathSeparator: "/", // Used by node.getKeyPath() and tree.loadKeyPath().
//    cookieId: "dynatree-cookie", // Choose a more unique name, to allow multiple trees.
	cookieId: "dynatree", // Choose a more unique name, to allow multiple trees.
	cookie: {
		expires: null //7, // Days or Date; null: session cookie
//		path: "/", // Defaults to current page
//		domain: "jquery.com",
//		secure: true
	},
	// Class names used, when rendering the HTML markup.
	// Note: if only single entries are passed for options.classNames, all other
	// values are still set to default.
	classNames: {
		container: "dynatree-container",
		node: "dynatree-node",
		folder: "dynatree-folder",
//		document: "dynatree-document",

		empty: "dynatree-empty",
		vline: "dynatree-vline",
		expander: "dynatree-expander",
		connector: "dynatree-connector",
		checkbox: "dynatree-checkbox",
		nodeIcon: "dynatree-icon",
		title: "dynatree-title",
		noConnector: "dynatree-no-connector",

		nodeError: "dynatree-statusnode-error",
		nodeWait: "dynatree-statusnode-wait",
		hidden: "dynatree-hidden",
		combinedExpanderPrefix: "dynatree-exp-",
		combinedIconPrefix: "dynatree-ico-",
		nodeLoading: "dynatree-loading",
//		disabled: "dynatree-disabled",
		hasChildren: "dynatree-has-children",
		active: "dynatree-active",
		selected: "dynatree-selected",
		expanded: "dynatree-expanded",
		lazy: "dynatree-lazy",
		focused: "dynatree-focused",
		partsel: "dynatree-partsel",
		lastsib: "dynatree-lastsib"
	},
	debugLevel: 1,

	// ------------------------------------------------------------------------
	lastentry: undefined
};
//
if( parseFloat($.ui.version) < 1.8 ) {
	$.ui.dynatree.defaults = $.ui.dynatree.prototype.options;
}

/*******************************************************************************
 * Reserved data attributes for a tree node.
 */
$.ui.dynatree.nodedatadefaults = {
	title: null, // (required) Displayed name of the node (html is allowed here)
	key: null, // May be used with activate(), select(), find(), ...
	isFolder: false, // Use a folder icon. Also the node is expandable but not selectable.
	isLazy: false, // Call onLazyRead(), when the node is expanded for the first time to allow for delayed creation of children.
	tooltip: null, // Show this popup text.
	icon: null, // Use a custom image (filename relative to tree.options.imagePath). 'null' for default icon, 'false' for no icon.
	addClass: null, // Class name added to the node's span tag.
	noLink: false, // Use <span> instead of <a> tag for this node
	activate: false, // Initial active status.
	focus: false, // Initial focused status.
	expand: false, // Initial expanded status.
	select: false, // Initial selected status.
	hideCheckbox: false, // Suppress checkbox display for this node.
	unselectable: false, // Prevent selection.
//  disabled: false,
	// The following attributes are only valid if passed to some functions:
	children: null, // Array of child nodes.
	// NOTE: we can also add custom attributes here.
	// This may then also be used in the onActivate(), onSelect() or onLazyTree() callbacks.
	// ------------------------------------------------------------------------
	lastentry: undefined
};

/*******************************************************************************
 * Drag and drop support
 */
function _initDragAndDrop(tree) {
	var dnd = tree.options.dnd || null;
	// Register 'connectToDynatree' option with ui.draggable
	if(dnd && (dnd.onDragStart || dnd.onDrop)) {
		_registerDnd();
	}
	// Attach ui.draggable to this Dynatree instance
	if(dnd && dnd.onDragStart ) {
		tree.$tree.draggable({
			addClasses: false,
			appendTo: "body",
			containment: false,
			delay: 0,
			distance: 4,
			revert: false,
			// Delegate draggable.start, drag, and stop events to our handler
			connectToDynatree: true,
			// Let source tree create the helper element
			helper: function(event) {
				var sourceNode = getDtNodeFromElement(event.target);
				return sourceNode.tree._onDragEvent("helper", sourceNode, null, event, null, null);
			},
			_last: null
		});
	}
	// Attach ui.droppable to this Dynatree instance
	if(dnd && dnd.onDrop) {
		tree.$tree.droppable({
			addClasses: false,
			tolerance: "intersect",
			greedy: false,
			_last: null
		});
	}
}

//--- Extend ui.draggable event handling --------------------------------------
var didRegisterDnd = false;
var _registerDnd = function() {
	if(didRegisterDnd){
		return;
	}
	$.ui.plugin.add("draggable", "connectToDynatree", {
		start: function(event, ui) {
			var draggable = $(this).data("draggable");
			var sourceNode = ui.helper.data("dtSourceNode") || null;
//			logMsg("draggable-connectToDynatree.start, %s", sourceNode);
//			logMsg("    this: %o", this);
//			logMsg("    event: %o", event);
//			logMsg("    draggable: %o", draggable);
//			logMsg("    ui: %o", ui);
			if(sourceNode) {
				// Adjust helper offset, so cursor is slightly outside top/left corner
//				draggable.offset.click.top -= event.target.offsetTop;
//				draggable.offset.click.left -= event.target.offsetLeft;
				draggable.offset.click.top = -2;
				draggable.offset.click.left = + 16;
//				logMsg("    draggable.offset.click FIXED: %s/%s", draggable.offset.click.left, draggable.offset.click.top);
				// Trigger onDragStart event
				// TODO: when called as connectTo..., the return value is ignored(?)
				return sourceNode.tree._onDragEvent("start", sourceNode, null, event, ui, draggable);
			}
		},
		drag: function(event, ui) {
			var draggable = $(this).data("draggable");
			var sourceNode = ui.helper.data("dtSourceNode") || null;
			var prevTargetNode = ui.helper.data("dtTargetNode") || null;
			var targetNode = getDtNodeFromElement(event.target);
//			logMsg("getDtNodeFromElement(%o): %s", event.target, targetNode);
			if(event.target && !targetNode){
				// We got a drag event, but the targetNode could not be found
				// at the event location. This may happen, if the mouse
				// jumped over the drag helper, in which case we ignore it:
				var isHelper = $(event.target).closest("div.dynatree-drag-helper,#dynatree-drop-marker").length > 0;
				if(isHelper){
//					logMsg("Drag event over helper: ignored.");
					return;
				}
			}
//			logMsg("draggable-connectToDynatree.drag: targetNode(from event): %s, dtTargetNode: %s", targetNode, ui.helper.data("dtTargetNode"));
			ui.helper.data("dtTargetNode", targetNode);
			// Leaving a tree node
			if(prevTargetNode && prevTargetNode !== targetNode ) {
				prevTargetNode.tree._onDragEvent("leave", prevTargetNode, sourceNode, event, ui, draggable);
			}
			if(targetNode){
				if(!targetNode.tree.options.dnd.onDrop) {
					// not enabled as drop target
					noop(); // Keep JSLint happy
				} else if(targetNode === prevTargetNode) {
					// Moving over same node
					targetNode.tree._onDragEvent("over", targetNode, sourceNode, event, ui, draggable);
				}else{
					// Entering this node first time
					targetNode.tree._onDragEvent("enter", targetNode, sourceNode, event, ui, draggable);
				}
			}
			// else go ahead with standard event handling
		},
		stop: function(event, ui) {
			var draggable = $(this).data("draggable");
			var sourceNode = ui.helper.data("dtSourceNode") || null;
			var targetNode = ui.helper.data("dtTargetNode") || null;
//			logMsg("draggable-connectToDynatree.stop: targetNode(from event): %s, dtTargetNode: %s", targetNode, ui.helper.data("dtTargetNode"));
//			logMsg("draggable-connectToDynatree.stop, %s", sourceNode);
			var mouseDownEvent = draggable._mouseDownEvent;
			var eventType = event.type;
//			logMsg("    type: %o, downEvent: %o, upEvent: %o", eventType, mouseDownEvent, event);
//			logMsg("    targetNode: %o", targetNode);
			var dropped = (eventType == "mouseup" && event.which == 1);
			if(!dropped){
				logMsg("Drag was cancelled");
			}
			if(targetNode) {
				if(dropped){
					targetNode.tree._onDragEvent("drop", targetNode, sourceNode, event, ui, draggable);
				}
				targetNode.tree._onDragEvent("leave", targetNode, sourceNode, event, ui, draggable);
			}
			if(sourceNode){
				sourceNode.tree._onDragEvent("stop", sourceNode, null, event, ui, draggable);
			}
		}
	});
	didRegisterDnd = true;
};

// ---------------------------------------------------------------------------
})(jQuery);

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

(function($){
   $.widget("ui.perc_imageselect", {
       // Globals
        _idPostfix: "_perc_is",
        _class: "perc_imageselect",
        _pseudoCtl: null,

      /**
       * Initialize the widget
       */             
      _init: function() {
         var self = this;
         options = this.options;            
         if("select" !== this.element[0].nodeName.toLowerCase())
            window.alert(I18N.message("perc.ui.image.select@Widget Use Error") + 
               I18N.message("perc.ui.image.select@Select HTML Element"));
         self._addPseudoControl();
         self._bindEvents(false);
      
      },
       /**
        * Create the pseudo image select control from the real select
        */               
      _addPseudoControl: function() {
         var self = this;
         var buff = "";
         var id = this.element[0].id;
         if('undefined' === id || null === id || '' === id) {
             window.alert(
                 I18N.message("perc.ui.image.select@Attribute Required Error"));
         }
         buff += "<div id='";
         buff += (id + this._idPostfix);
         buff += "' class='";
         buff += this._class;
         buff += "'>";      
         var children = this.element.children('option');
         
         children.each(function(){
            buff += self._getItemHtml(this.value, this.text);     
         });
         buff += "</div>";
         this.element.hide();
         this.element.after(buff);
         this._pseudoCtl = $("#" + this.element[0].id + this._idPostfix);
         this._addChildIndexes(this._pseudoCtl.children(".perc_imageselect_item"));

      }, _getItemHtml: function(val, imageurl){
         var buff = "";
         buff += "<div class='perc_imageselect_item'>";
         buff += "<span class='perc_imageselect_value'>" + val + "</span>";
         buff += "<img src='";
         buff += imageurl;
         buff += "'/><span>";
	 buff += $.PercTruncateText(val.split(".")[2], 22);
	 buff += "</span></div>";
         return buff;      
      },
      
      _addChildIndexes: function(elem){
         
         var count = 0;
         //Remove any existing index nodes
         elem.each(function(){
            $(this).remove(".perc_imageselect_index");
         });
         //Add index nodes        
         elem.each(function(){
            $(this).append(
               "<span class='perc_imageselect_index'>" + (count++) + "</span>");
         });
         
      },
      
      destroy: function() {
         $.widget.prototype.apply(this, arguments); // default destroy
         this._unbindEvents(false);
       },

      
      /**
       * Bind all necessary events
       */             
      _bindEvents: function(childrenOnly) {
         var self = this;
         // Add events to the pseudo control          
         if(!childrenOnly)
         {
           this._pseudoCtl.on('keydown', function(evt){
              self._handleKeyDown(evt);
           });
           
           this._pseudoCtl.on('click', function(evt){
              $(this).focus();
           });
                              
         }
                  
         this._pseudoCtl.children(".perc_imageselect_item")
         .on('click',
            function(evt){
              var idx = $(this).children(".perc_imageselect_index").text();
              self.selectIndex(idx);   
         });
         
         this._pseudoCtl.children(".perc_imageselect_item").on('dblclick',
            function(evt){
              if(self.options.hardSelect)
                 self._onSelect();    
         });
         
         
         
      },
      /**
       * Unbind events from the controls
       * @param childrenOnly (boolean) if <code>true</code> then only
       * unbind from the child nodes. 
       */                           
      _unbindEvents: function(childrenOnly){
         if(!childrenOnly)
         {
         this._pseudoCtl
            .off('keydown')
            .off('click');
         }
         
         this._pseudoCtl.children(".perc_imageselect_item")
            .off('click')
            .off('dblclick');
                       
      },      
      
      /**
       * Handle the keydown events
       */             
      _handleKeyDown: function(evt){
         if (!this.options.vertical && (37 === evt.keyCode || 39 === evt.keyCode)) {
					this._moveSelection(39 === evt.keyCode);
					return evt.preventDefault();
			}	
					
			if (this.options.vertical && (38 === evt.keyCode || 40 === evt.keyCode)) {
					this._moveSelection(40 === evt.keyCode);
					return evt.preventDefault();
			}
			if (this.options.hardSelect && 13 === evt.keyCode)
			{
			   this._onSelect();
            return evt.preventDefault();
         }
			return true;            
      },
      
      /**
       * Increment or decrement selection if possible.
       * @param increment (boolean).
       */                    
      _moveSelection: function(increment){
         var current = this.getSelectedIndex();
         var childCount = this._pseudoCtl.children(".perc_imageselect_item").length;
         if(current !== -1)
         {
            if(increment && (childCount - 1) > current)
            {
               this.selectIndex(current + 1);   
            }
            else if(!increment && 0 < current)
            {
               this.selectIndex(current - 1);
            }   
         
         }
      },
      /**
       * Fire off the on select callback if one was specified.
       */             
      _onSelect: function(){
         if(null !== this.options.onSelect)
         {
             var selection = 
                this._pseudoCtl.children(".perc_imageselect_selected");                
             var value = selection.children(".perc_imageselect_value").text();
             var imageurl = selection.children("img").attr("src");               
             this.options.onSelect(value, imageurl);
         }
      },
      
      /**
       * Sets items for this image select.
       * @param items (Array) an array of the items the following
       * format. [["value1", "imageurl1"], ["value2", "imageurl2"]]              
       */             
      setItems: function(items){
         if(null === items || 'undefined' === items)
         {
            alert(I18N.message("perc.ui.image.select@Null Or Undefined Items"));
         }
         else
         {
            this.clearSelection();
            this._unbindEvents(true);
            this._pseudoCtl.children(".perc_imageselect_item").remove();
            for(var key in items)
            {
               var current = items[key];
               this._pseudoCtl.append(this._getItemHtml(current[0], current[1]));
            }
            this._addChildIndexes(
               this._pseudoCtl.children(".perc_imageselect_item"));
            this._bindEvents(true);   
         }      
      },
      /**
       * Load from remote server based on passed in url.
       * Expects to receive a JSON object with an items property that contains an 
       * array in the format specified by
       * the {@link #setItems(items)} function.
       * @param url (string) the url of the remote to get the items
       * from.
       */                                        
      loadFromUrl: function(url)
      {
         var self = this;
         if(null !== url && 'undefined' !== url)
         {
            $.getJSON(url, function(data){
               self.setItems(data.items);
            });   
         
         }
      },      
      
      /**
       * Clear all selected items.
       */             
      clearSelection: function(){
         var imageselectId = this.element[0].id + this._idPostfix; 
         $("#" + imageselectId + " .perc_imageselect_selected")
            .removeClass("perc_imageselect_selected");
      },
      
      /**
       * Select the item based on the passed in index.
       */             
      selectIndex: function(idx){
         this.clearSelection();
         var imageselectId = this.element[0].id + this._idPostfix;
         $("#" + imageselectId + 
            " .perc_imageselect_item:eq(" + idx + ")").addClass(
               "perc_imageselect_selected");
         var targetOffset =  
            this._pseudoCtl.children(".perc_imageselect_selected").offset().top - 50; 
         var sourceOffset = this._pseudoCtl.offset().top;
         var oldScrollTop = this._pseudoCtl.scrollTop();
         if(!this.options.hardSelect)
         {
            
            this._onSelect();      
         }
         this._pseudoCtl.animate({scrollTop: oldScrollTop + targetOffset - sourceOffset}, 500);             
         this._pseudoCtl.focus();          
      },
      
      /**
       * Get the index of the selected item.
       * @return index of the selection or -1 if no selection.
       */                    
      getSelectedIndex: function(){
         var selection = this._pseudoCtl.children(".perc_imageselect_selected");
         if(0 < selection.length)
         {
           return parseInt(selection.children(".perc_imageselect_index").text());
         }
         return -1;     
      }      
     
      
      

   });

   $.extend($.ui.perc_imageselect, {
      version: "1.0.0",
      defaults: {
         vertical: true,
         onSelect: null,
         hardSelect: true     
      }
   });
 
})(jQuery);

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

/*
 * Wizard dialog widget
 *
 * This widget can create a multi page wizard dialog based on a set of
 * step defined in a div structure.
 *
 * Example Usage:
 *
 * $(document).ready(function() {
 *
 *      var $wizard = $("<div></div>").perc_wizard({
 *         templateUrl: "../html/dialogs/perc_newSiteDialog.html",
 *         title: "New Site",
 *         height: 400
 *         }
 *
 *      });
 *
 *      $('#someButton').click(function() {
 *           $wizard.perc_wizard('open');
 *      });
 * });
 *
 *  The template structure:
 *
 * <div id="perc_newSiteDialog" class="perc_dialog">
 *    <div id="perc_wizard_step1" class="perc_wizard_step">
 *      <div class="perc_dialog_summary">
 *          Some summary text
 *      </div>
 *        ...
 *        ... step 1 body here
 *     </div>
 *     <div id="perc_wizard_step2" class="perc_wizard_step">
 *      <div class="perc_dialog_summary">
 *          Some summary text
 *      </div>
 *        ...
 *        ... step 2 body here
 *     </div>
 *   </div>
 *
 */
(function($)
{
    $.widget("ui.perc_wizard", {
        // Globals
        steps: null,
        currentStep: 0,
        isCancelled: false,

        _init: function()
        {
            var self = this,
                options = this.options;
            this.element.load(options.templateUrl, function()
            {
                self.element.perc_dialog(
                {
                    autoOpen: false,
                    title: options.title,
                    modal: options.modal,
                    width: options.width,
                    height: options.height,
                    show: options.show,
                    hide: options.hide,
                    open: options.open,
                    resizable: options.resizable
                });
                self.steps = $('.perc_wizard_step');
                self._addButtons();

            });
        },

        open: function()
        {
            this._showStep(0);
            this.element.dialog('open');
        },

        _addButtons: function()
        {
            var self = this;

            var buttons = "<div class='ui-dialog-buttonpane ui-widget-content ui-helper-clearfix'>" +
            	 "<button id='perc_wizard_finish' class='btn btn-primary' name='perc_wizard_finish' style='float:right;'>" +I18N.message("perc.ui.common.label@Finish") + "</button>" +
                "<button id='perc_wizard_next' class='btn btn-primary'  name='perc_wizard_next' style='float:right;'>" + I18N.message("perc.ui.common.label@Next") + "</button>" +
                 "<button id='perc_wizard_cancel' class='btn btn-primary' name='perc_wizard_cancel' style='float:right;'>" +I18N.message("perc.ui.assign.workflow@Cancel") + "</button>" +
                 "<button id='perc_wizard_back' class='btn btn-primary'  name='perc_wizard_back' style='float:right;'>" +I18N.message("perc.ui.common.label@Back") +  "</button>" + "</div>";
            //Appending buttons to the buttonpane
            $(self.element).closest('.ui-dialog').append(buttons);
            $('#perc_wizard_back').on("click", function()
            {
                self._onBack();
            });
            $('#perc_wizard_cancel').on("click", function()
            {
                self._onCancel();
            }).val(this.options.cancelButtonLabel);
            $('#perc_wizard_next').on("click",function()
            {
                self._onNext();
            });
            $('#perc_wizard_finish').on("click",function()
            {
                self._onOk();
            });

        },

        _showStep: function(step)
        {
            if (this.steps == null || step > this.steps.length)
            {
                alert(I18N.message("perc.ui.wizard@Step Does Not Exist"));
                return;
            }
            this.currentStep = step;
            for (i = 0; i < this.steps.length; i++)
            {
                if (i == step) $(this.steps[i]).show();
                else $(this.steps[i]).hide();
            }

            if (step == 0 && this.steps.length > 1)
            {
                //first step of 2 or more
                $('#perc_wizard_back').hide();
                $('#perc_wizard_cancel').show().val(this.options.cancelButtonLabel);
                $('#perc_wizard_next').show().val(this.options.nextButtonLabel);
                $('#perc_wizard_finish').hide();

            }
            else if (step < this.steps.length - 1)
            {
                //mid step
                $('#perc_wizard_back').show().val(this.options.backButtonLabel);
                $('#perc_wizard_cancel').show().val(this.options.cancelButtonLabel);
                $('#perc_wizard_next').show().val(this.options.nextButtonLabel);
                $('#perc_wizard_finish').hide();
            }
            else
            {
                //final step
                $('#perc_wizard_back').show().val(this.options.backButtonLabel);
                $('#perc_wizard_cancel').show().val(this.options.cancelButtonLabel);
                $('#perc_wizard_next').hide();
                $('#perc_wizard_finish').show().val(this.options.finishButtonLabel);
            }


            var elem = this.element;
        },

        _onOk: function(e)
        {
            var self = this;
            if (this.options.onValidate()) this.options.onOk();
        },

        _onNext: function(e)
        {
            if (this.options.onValidate())
            {
                // If a custom onNext function was defined, excecute it before going to the next
                // step (and retrieve its return value)
                if (this.options.onNext !== undefined && this.options.onNext() === false)
                {
                    return;
                }
                this._showStep(this.currentStep + 1);
            }
        },

        _onBack: function()
        {
            if (this.currentStep > 0) this._showStep(this.currentStep - 1);
        },

        _onCancel: function()
        {
            this.isCancelled = true;
            this.element.dialog('close');
        }
    });

    $.extend($.ui.perc_wizard, {
        version: "1.0.0",
        defaults: {
            backButtonLabel: typeof I18N == "undefined" ? "Back" : I18N.message("perc.ui.common.label@Back"),
            cancelButtonLabel: typeof I18N == "undefined" ? "Cancel" : I18N.message("perc.ui.common.label@Cancel"),
            nextButtonLabel: typeof I18N == "undefined" ? "Next" : I18N.message("perc.ui.common.label@Next"),
            finishButtonLabel: typeof I18N == "undefined" ? "Finish" : I18N.message("perc.ui.common.label@Finish"),
            width: "800px",
            height: "700px",
            modal: true,
            title: typeof I18N == "undefined" ? "Wizard" : I18N.message("perc.ui.common.label@Wizard"),
            onOk: function()
            {
                console.log("No onOk callback was set.");
            },
            onValidate: function()
            {
                return true;
            }
        }
    });

})(jQuery);

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

(function($)
{
    $.PercSiteService = {
        getSiteProperties: getSiteProperties,
        updateSiteProperties: updateSiteProperties,
        copySite: copySite,
        copySiteInfo: copySiteInfo,
        getTemplates: getTemplates,
        getBaseTemplates: getBaseTemplates,
        getSites: getSites,
        validateCopySiteFolders: validateCopySiteFolders,
        createSiteFromUrl: createSiteFromUrl,
        createSiteFromUrlAsync: createSiteFromUrlAsync,
        createSiteFromUrlStatus: createSiteFromUrlStatus,
        createSiteFromUrlResult: createSiteFromUrlResult, 
        isSiteBeingImported : isSiteBeingImported,
        getSaaSSiteNames : getSaaSSiteNames
    };

    /**
     * Update site properties. This is a JSON only call and returns json in
     * the callback.
     * @param siteProps {Object} the SiteProperties object. Cannot be <code>null</code>.
     * @param callback the callback function to be called when the request completes.
     */
    function updateSiteProperties(siteProps, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITE_UPDATE_PROPERTIES, $.PercServiceUtils.TYPE_POST, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
            }
        },
        siteProps);
    }

    /**
     * Get the properties for the site specified.
     * @param site the sitename, assumed not <code>null</code> or empty.
     * @param callback function to be called when section is retrieved, the
     * section object will be the sole argument passsed to the callback.
     */
    function getSiteProperties(site, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITE_GET_PROPERTIES + "/" + site, $.PercServiceUtils.TYPE_GET, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
            }
        });
    }

    /**
     * Copy site.
     * @param postObject {object} the json object to be passed to the server, assumed not <code>null</code> or empty.
     * Format: {"SiteCopyRequest":{"srcSite":"Site1","copySite":"Site1-copy15","assetFolder":""}}
     * @param callback the callback function to be called when the request completes.
     */
    function copySite(postObject, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITE_COPY, $.PercServiceUtils.TYPE_POST, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                var defaultCode = $.PercServiceUtils.extractGlobalErrorCode(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg, defaultCode);
            }
        },
        postObject);
    }

    /**
     * Validate Copy Site asset folders
     * @param postObject {object} the json object to be passed to the server, assumed not <code>null</code> or empty.
     * Format: {"SiteCopyRequest":{"srcSite":"Site1","copySite":"Site1-copy15","assetFolder":""}}
     * @param callback the callback function to be called when the request completes.
     */
    function validateCopySiteFolders(postObject, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITE_COPY_VALIDATE_FOLDERS, $.PercServiceUtils.TYPE_POST, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                var defaultCode = $.PercServiceUtils.extractGlobalErrorCode(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg, defaultCode);
            }
        },
        postObject);
    }

    /**
     * Get info of copy site in progress.
     * @param callback the callback function to be called when the request completes.
     */
    function copySiteInfo(callback)
    {
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITE_COPY_INFO, $.PercServiceUtils.TYPE_GET, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
            }
        });
    }

    /**
     * Retrieves a list of templates for the given site.
     * @param siteName the name of the site for which we want a list of templates
     * @param callback the callback function
     * @return array of objects representing templates used in this site:
     * <pre>
     *    {"TemplateSummary":[
     *     {    "id":"16777215-101-1957",
     *         "imageThumbPath":"\/Rhythmyx\/rx_resources\/images\/TemplateImages\/AnySite\/perc.base.cClampBottom_Thumb.png",
     *         "label":"\"C\" Clamp Bottom",
     *         "name":"template name",
     *         "readOnly":false,
     *         "sourceTemplateName":"perc.base.cClampBottom"}]}
     * </pre>
     */
    function getTemplates(siteName, callback, widgetDefId)
    {
        var sendURL;

        if (widgetDefId != null)
        {
            sendURL = $.perc_paths.TEMPLATES_BY_SITE + "/" + siteName + "/" + widgetDefId;
        }
        else
        {
            sendURL = $.perc_paths.TEMPLATES_BY_SITE + "/" + siteName + "";
        }

        $.PercServiceUtils.makeJsonRequest(
        sendURL, $.PercServiceUtils.TYPE_GET, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
            }
        });
    }
    
    function getBaseTemplates(type, callback){
        var templUrl = $.perc_paths.TEMPLATES_READONLY + "?type=" + type;
        //Load regular base templates
        $.PercServiceUtils.makeJsonRequest(templUrl,$.PercServiceUtils.TYPE_GET,false,function(status, result){
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback(true, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback(false, defaultMsg);
                }
        });
     }

    /**
     * Retrieves a list of all sites in the CM sytem.
     * @param callback the callback function that will pass an array
     * of objects with the following properties:
     * <pre>
     *    sitename
     * </pre>
     */
    function getSites(callback)
    {
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITES_ALL + "/", $.PercServiceUtils.TYPE_GET, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
            }
        });
    }

    /**
     * Crete a site, template and page based in a given URL.
     * @param siteProps {Object} The basic properties needed to ceate the site. Cannot be <code>null</code>.
     * The object will have the following structure:
     * <pre>
     *     {
     *         "name" : "a valida site name",
     *         "baseUrl" : "URL"
     *     }
     * </pre>
     * @param callback the callback function to be called when the request completes.
     */
    function createSiteFromUrl(siteProps, callback)
    {
        // Redefine temporarly timeout to be 10 minutes (importing proccess might take too long)
        $.ajaxSetup({timeout: 600000});

        // Before sending the data to the server-side service, we have to adapt it
        var serviceParam = {Site: siteProps};
        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SITE_CREATE_FROM_URL, $.PercServiceUtils.TYPE_POST, false, function(status, result)
        {
            // Re-Set default timeout
            $.ajaxSetup({
                timeout: 60000
            });
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
            }
        },
        serviceParam);
    }

    /**
     * Starts a site creation job (async), given a site properties specification
     * @param Object siteProps
     * @param function callback.
     */
    function createSiteFromUrlAsync(siteProps, callback)
    {
        // Before sending the data to the server-side service, we have to adapt it
        var serviceParam = {SiteImportConfiguration: siteProps};

        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.SITE_CREATE_FROM_URL_ASYNC,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = I18N.message("perc.ui.site.service@Unexpected Error Importing");
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            serviceParam
        );
    }

    /**
     * Retrieves the status of a previously started site creation job.
     * @param String siteCreationJobId returned by a createTemplateFromUrlStatus
     * @param function callback.
     */
    function createSiteFromUrlStatus(siteCreationJobId, callback)
    {
        var jobId  = siteCreationJobId.Long === undefined ? siteCreationJobId : siteCreationJobId.Long;
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.JOB_STATUS + '/' + jobId,
            $.PercServiceUtils.TYPE_GET,
            false,
            function templateCreateFromUrlAsyncCallback(status, result)
            {
                var data,
                    serviceStatus = $.PercServiceUtils.STATUS_SUCCESS;

                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    // NOTE that the progress could be -1, but that is something that the user of
                    // the service must check, the request has been successful
                    data = result.data.asyncJobStatus;
                }
                else
                {
                    // If an unhandled error happened in the server, the answer to the request will
                    // be an error
                    serviceStatus = $.PercServiceUtils.STATUS_ERROR;
                    data = I18N.message("perc.ui.site.service@Unexpected Error Importing");
                }

                callback(serviceStatus, data);
            }
        );
    }

    /**
     * Retrieves the result of a previously started site creation job.
     * @param String siteCreationJobId returned by a createTemplateFromUrlStatus
     * @param function callback.
     */
    function createSiteFromUrlResult(siteCreationJobId, callback)
    {
        var jobId  = siteCreationJobId.Long === undefined ? siteCreationJobId : siteCreationJobId.Long;
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.SITE_CREATE_FROM_URL_RESULT + '/'+ jobId,
            $.PercServiceUtils.TYPE_GET,
            false,
            function templateCreateFromUrlAsyncCallback(status, result)
            {
                var data,
                    serviceStatus = $.PercServiceUtils.STATUS_SUCCESS;

                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    data = result.data;
                }
                else
                {
                    // If an unhandled error happened in the server, the answer to the request will
                    // be an error
                    serviceStatus = $.PercServiceUtils.STATUS_ERROR;
                    data = I18N.message("perc.ui.site.service@Unexpected Error Importing");
                }

                callback(serviceStatus, data);
            }
        );
    }
    
    /**
     * Checks if the given site is being imported.
     * @param String sitename the name of the site.
     */
    function isSiteBeingImported(sitename, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.SITE_IS_BEING_IMPORTED + '/'+ sitename,
            $.PercServiceUtils.TYPE_GET,
            false,
            function (status, result)
            {
                var data, serviceStatus = $.PercServiceUtils.STATUS_SUCCESS;
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                	data = result.data;
                }
                else
                {
                    // If an unhandled error happened in the server, the answer to the request will
                    // be an error
                    serviceStatus = $.PercServiceUtils.STATUS_ERROR;
                    data = I18N.message("perc.ui.site.service@Unexpected Error Importing");
                }
                callback(serviceStatus, data);
            }
        );
    }
    
    /**
     * Retrieves a map of all saas sitenames and associated config file names.
     * @param filterUsedSites if it is true, then returns unused sites only
     * the map may be empty if no valid sites found.
     * Uses jQuery deferred, on success resolves with site name map and on
     * failure rejects with error message.
     * 
     */
    function getSaaSSiteNames(filterUsedSites)
    {
        var deferred = $.Deferred();

        $.PercServiceUtils.makeJsonRequest(
        $.perc_paths.SAAS_SITES_NAMES + "?filterUsedSites=" + filterUsedSites, $.PercServiceUtils.TYPE_GET, false, function(status, result)
        {
            if (status === $.PercServiceUtils.STATUS_SUCCESS)
            {
                deferred.resolve(result.data);
            }
            else
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                deferred.reject(defaultMsg);
            }
        });
        return deferred.promise();
    }
    
    
})(jQuery);

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

(function($)
{
    $.PercAssetService = {
        getAssetEditorForAssetId: getAssetEditorForAssetId,
        getAssetViewForAssetId: getAssetViewForAssetId,
        getAssetEditorLibrary   : getAssetEditorLibrary,
        getAssetEditor          : getAssetEditor,
        getAssetEditorForWidgetAndFolder:getAssetEditorForWidgetAndFolder,
        putAssetInFolder        : putAssetInFolder,
        clear_asset             : clear_asset,
        clear_orphan_assets     : clear_orphan_assets,
        set_relationship        : set_asset_relationship,
        update_relationship     : update_asset_relationship,
        asset_from_local_content : asset_from_local_content,
        deleteAsset             : deleteAsset,
        forceDeleteAsset        : forceDeleteAsset,
        validateDeleteAsset     : validateDeleteAsset,
        updateAsset             : updateAsset,
        getUnusedAssets         : getUnusedAssets,
        promoteAsset            : promoteAsset,
        getAssetTypes           : getAssetTypes
    };

    /**
     * Get an editor for an asset
     * @param assetId of the asset we want to edit with the editor URL we are asking for.
     * The URL accesses a form that already knows about the asset and how to edit it by submitting the form. 
     * @type string
     * 
     */
    function getAssetEditorForAssetId(assetId, callback) {
        $.ajax({
             url: $.perc_paths.ASSET_EDITOR_URL_FOR_ASSET_ID + "/" + assetId,
             success: function(data) {

              callback($.PercServiceUtils.STATUS_SUCCESS, data);
            }, 
              error: function(request, textstatus, error){

               var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(request);
               callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
              
            }
        });
    }
    
    /**
     * Get read only view url for an asset.
     * @param assetId of the asset we want to view with the view URL.
     * @return the URL string 
     * @type string
     * 
     */
    function getAssetViewForAssetId(assetId, callback) {
        $.ajax({
             url: $.perc_paths.ASSET_VIEW_URL_FOR_ASSET_ID + "/" + assetId,
             success: function(data){

              callback($.PercServiceUtils.STATUS_SUCCESS, data);
            }, 
              error: function(request, textstatus, error){

               var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(request);
               callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
              
            }
        });
    }

    /**
     * Get a list of AssetEditors.
     * @param callback function to be called when library is retrieved
     * return JSON object has the following format:
     * 
     * {"AssetEditor":[
     *  { "icon":"/rx_resources/widgets/simpleList/images/theIconImage.png",
     *    "title":"The Editor Label",
     *    "url":"/Rhythmyx/psx_cepercSimpleAutoList/theEditorPage.html?sys_command=edit&sys_view=sys_HiddenFields:"},
     *    "workflowId":4,
     *    "contentType":"AssetContentType"
     *  }
     * ...
     * ]}
     * 
     */
    function getAssetEditorLibrary(currentFolderPath, callback)
    {
        getAssetEditors(currentFolderPath, "", callback);
    }
    function getAssetEditors(currentFolderPath, widgetId, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ASSET_EDITOR_LIBRARY + currentFolderPath + "?filterDisabledWidgets=yes&widgetId=" + widgetId,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }
    /**
     * Get an asset editor for the supplied widgetId
     * @param callback function to be called when library is retrieved
     * return JSON object has the following format:
     * 
     * {"AssetEditor":[
     *  { "icon":"/rx_resources/widgets/simpleList/images/theIconImage.png",
     *    "title":"The Editor Label",
     *    "url":"/Rhythmyx/psx_cepercSimpleAutoList/theEditorPage.html?sys_command=edit&sys_view=sys_HiddenFields:"},
     *    "workflowId":4,
     *    "contentType":"AssetContentType"
     *  }
     * ...
     * ]}
     * 
     */
    function getAssetEditor(widgetId, assetFolderPath, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ASSET_EDITOR + "/" + widgetId + "?parentFolderPath=" + assetFolderPath,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }
    
    /**
     * Get an asset editor for the supplied widgetId and folder path.
     * @param callback function to be called when library is retrieved
     * return JSON object has the following format:
     * 
     * {"AssetEditor":[
     *  { "icon":"/rx_resources/widgets/simpleList/images/theIconImage.png",
     *    "title":"The Editor Label",
     *    "url":"/Rhythmyx/psx_cepercSimpleAutoList/theEditorPage.html?sys_command=edit&sys_view=sys_HiddenFields:"},
     *    "workflowId":4,
     *    "contentType":"AssetContentType"
     *  }
     * ...
     * ]}
     * 
     */
    function getAssetEditorForWidgetAndFolder(folderPath, widgetId, callback)
    {
        getAssetEditors(folderPath, widgetId, callback);
    }

    /**
     * Put an asset in a folder (virtual)
     * @param assetFolderRelationship JSON object containing association of asset id and the folder path.
     * assetFolderRelationship has the following schema:
     * 
     * {"AssetFolderRelationship" : {"assetId" : assetId,    "folderPath" : folderPath}}
     * 
     */
    function putAssetInFolder(assetFolderRelationship, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ASSET_ADD_TO_FOLDER,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            assetFolderRelationship
        );
    }

    /**
     * Deletes an asset.  The asset will be validated to ensure it is safe to delete.
     * @param id of the asset we want to delete.
     * @param callback handles success.
     * @param errorCallBack handles errors and validation warnings.
     */
    function deleteAsset(id, callback, errorCallBack)
    {
        $.ajax({
            url     : $.perc_paths.ASSET_DELETE + "/" + id, 
            type    : 'DELETE',
            success : function() {
                callback();
            },
            error   : errorCallBack
        });
    }
    
    /**
     * Deletes an asset without validation.
     * @param id of the asset we want to delete.
     * @param callback handles success.
     * @param errorCallBack handles errors.
     */
    function forceDeleteAsset(id, callback, errorCallBack)
    {
        $.ajax(
            {
                url: $.perc_paths.ASSET_FORCE_DELETE + "/" + id, 
                type: 'GET',
                success: callback,
                error: errorCallBack
            });
    }

    /**
     * Validates that an asset may be deleted by the current user.
     * @param id of the asset we want to delete.
     * @param callback handles success.
     * @param errorCallBack handles errors and validation warnings.
     */
    function validateDeleteAsset(id, callback, errorCallBack)
    {
       $.ajax(
            {
                url: $.perc_paths.ASSET_VALIDATE_DELETE + "/" + id, 
                type: 'GET',
				dataType: "json",
                contentType: "application/json",
                success: callback,
                error: errorCallBack
            });
    }

    /**
     * set_asset_relationship()
     * 
     * Moved here from perc_asset_manager.js to merge functionality into one file.
     *  
     */
    function set_asset_relationship( assetid, widgetData, pageid, assetOrder, isResource, folderPath, k, err ) {
         var resType = isResource?"shared":"local";
         var relationshipId = (typeof(widgetData.relationshipId)!== "undefined") ? widgetData.relationshipId : -1;
         var awr = {
            "AssetWidgetRelationship":{
                "ownerId":pageid,
                "widgetId":widgetData.widgetid,
                "widgetName":widgetData.widgetdefid,
                "widgetInstanceName":widgetData.widgetName,
                "replacedRelationshipId": relationshipId,
                "assetId":assetid,
                "assetOrder":"0",
                "resourceType":resType
             }
         };
         if(folderPath)
            awr.AssetWidgetRelationship.folderPath = folderPath;
         $.ajax({
                   url: $.perc_paths.ASSET_WIDGET_REL + "/",
                   dataType: "text",
                   contentType: "application/json",
                   type: "POST",
                   data: JSON.stringify(awr),
                   success: k,
                   error: err });
    }
    
    /**
     * Creates an asset from local content.
     * @param assetid
     * @param widgetData
     * @param pageid
     * @param name
     * @param path
     * @param callback
     */
    function asset_from_local_content( assetid, widgetData, pageid, name, path, callback) {
        // path variable already comes with a leading '/'
        var url = $.perc_paths.ASSET_FROM_LOCALCONTENT + '/' + name + path;
        var awr = {
           'AssetWidgetRelationship' : {
               'ownerId' : pageid,
               'widgetId' : widgetData.widgetid,
               'widgetName' : widgetData.widgetdefid,
               'widgetInstanceName' : widgetData.widgetName,
               'assetId' : assetid,
               'assetOrder' : '0',
               'resourceType' : 'local'
            }
        };
        
        $.PercServiceUtils.makeRequest(
            url,
            $.PercServiceUtils.TYPE_POST,
            false,
            function( status, result ) {
                if ( status === $.PercServiceUtils.STATUS_SUCCESS )
                {
                    callback( $.PercServiceUtils.STATUS_SUCCESS, result.data );
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage( result.request );
                    callback( $.PercServiceUtils.STATUS_ERROR, defaultMsg );
                }
            },
            awr,
            "application/json",
            "text"
        );
    }

    function update_asset_relationship( assetid, relationshipId, widgetData, pageid, k, err) {
        var replacedRelationshipId = (typeof(widgetData.relationshipId)!== "undefined") ? widgetData.relationshipId : -1;
        var awr = {
            "AssetWidgetRelationship":{
                "ownerId":pageid,
                "widgetId":widgetData.widgetid,
                "widgetName":widgetData.widgetdefid,
                "widgetInstanceName":widgetData.widgetName,
                "replacedRelationshipId": replacedRelationshipId,
                "assetId":assetid,
                "assetOrder":"0",
                "resourceType":"local",
                "relationshipId":relationshipId
             }
         };
         $.PercServiceUtils.makeJsonRequest($.perc_paths.ASSET_WIDGET_REL_UPDATE + "/", 
                $.PercServiceUtils.TYPE_POST, false, function(status, results)
         {
            if (status === $.PercServiceUtils.STATUS_SUCCESS) 
            {
                // the call returns the id of the relationship that was updated, or -1 if it does not find it
                var relId = results.data;
                if(relId === '-1' && err!=null)
                {
                    var msg = I18N.message("perc.ui.asset.service@Removed Asset");
                    err(false, msg);
                }
                else 
                {
                    k(true, results.data);
                }
            } 
            else 
            {
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                err(false, defMsg);
            }
        }, awr);
    }
    
    /**
     * clear_asset()
     * 
     * Moved here from perc_asset_manager.js to merge functionality into one file.
     *  
     */
    function clear_asset(ownerId, widgetId, widgetDefinitionId, assetId, callback ) {
       callback = callback || function(){};
       var awr = {"AssetWidgetRelationship":{"ownerId":ownerId,"widgetId":widgetId,"widgetName":widgetDefinitionId,"assetId":assetId,"assetOrder":"0"}};
       $.ajax({
             url: $.perc_paths.ASSET_WIDGET_REL_DEL + "/",
             dataType: "json",
             contentType: "application/json",
             type: "POST",
             data: JSON.stringify(awr),
             success: callback,
             error: function(request, textstatus, error){
                   alert("error");
             }  
       });
    }
    
    /**
     * Executes a request to delete selected orphan assets
     * @param ownerId
     * @param widgetId
     * @param widgetDefinitionId
     * @param assetId
     */
    function clear_orphan_assets(ownerId, widgetIds, widgetDefinitionIds, assetIds, callback ) {
	callback = callback || function(){};
	
	var assets = [];
	for (var i = 0; i < assetIds.length; i++)
	{
	    var asset = "";
	    asset = "{\"assetId\":" + "\"" + assetIds[i] + "\"," +
			"\"assetOrder\":" + "0" + "," +
			"\"ownerId\":" + "\"" + ownerId + "\"," +
			"\"widgetId\":" + widgetIds[i] + "," +
			"\"widgetName\":" + "\"" + widgetDefinitionIds[i] + "\"}";
	    assets.push(asset);
	}
	
	var json = "{\"OrphanAssetsSummary\":{\"assetWidgetRelationship\":["+ assets +"]}}";
	var awr = JSON.parse(json);
	
	$.ajax({
             url: $.perc_paths.ASSET_ORPHAN_WIDGET_REL_DEL + "/",
             dataType: "json",
             contentType: "application/json",
             type: "POST",
             data: JSON.stringify(awr),
             success: callback,
             error: function(request, textstatus, error){
                   alert("error");
             }  
       });
    }
    
     /**
     * Executes a request to update the page title value 
     * @param pageId the id of the page
     * @param assetId the id of the asset
     */
    function updateAsset(pageId, assetId, callback )
    {
	   var getUrl = $.perc_paths.ASSET_UPDATE + "/" + pageId + "/" + assetId;
       
       var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,results.data);
            }
        };
        $.PercServiceUtils.makeRequest(getUrl, $.PercServiceUtils.TYPE_POST, false, serviceCallback);

    }
    
    /**
     * Makes a call to the server to promote the asset to the template.
     * @param {Object} assetid guid of the asset that needs to be promoted, assumed not null.
     * @param {Object} widgetData widgetData Expected to be an object that provides widgetId, widgetdefid and widgetName.
     * @param {Object} ownerid only template guid is supported now.
     * @param {Object} assetOrder order of the asset.
     * @param {Object} isResource flag to indicate whether the asset is a shared asset or not.
     * @param {Object} callback function to be called after the ajax call. Calls the method with first argument as 
     * $.PercServiceUtils.STATUS_SUCCESS incase of success or $.PercServiceUtils.STATUS_ERROR in case of error. The second argument is
     * error message in case of error.
     */
    function promoteAsset(assetid, widgetData, ownerid, assetOrder, isResource, callback ) {
         var resType = isResource?"shared":"local";
         var awr = {
            "AssetWidgetRelationship":{
                "ownerId":ownerid,
                "widgetId":widgetData.widgetid,
                "widgetName":widgetData.widgetdefid,
                "widgetInstanceName": widgetData.widgetName,
                "assetId":assetid,
                "assetOrder":assetOrder,
                "resourceType":resType
             }
         };
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ASSET_PROMOTE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            awr
        );      
    }
    
    /**
     * Get all unused Assets that could be used in a specified page
     * @param pageId Id Page of the current edited page.
     */
    function getUnusedAssets(pageId, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ASSET_UNUSED + "/" + pageId,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, $.perc_utils.convertCXFArray(result.data.UnusedAssetSummary));
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }
    
    /**
     * Get all unused Assets that could be used in a specified page
     * @param pageId Id Page of the current edited page.
     */
    function getAssetTypes(filterDisabledWidgets, callback){
        var url = $.perc_paths.ASSET_TYPES;
        if(filterDisabledWidgets && filterDisabledWidgets === "yes"){
            url += url.indexOf("?") === -1?"?filterDisabledWidgets=yes":"&filterDisabledWidgets=yes";
        }
        $.PercServiceUtils.makeJsonRequest(
            url,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, $.perc_utils.convertCXFArray(result.data.WidgetContentType));
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }
        
})(jQuery);

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

(function($)
{
    $.PercRecycleService = {
        restoreItem: restoreItem,
        purgeItem: purgeItem
    };

    function restoreItem(id, path, callback) {
        $.PercServiceUtils.makeJsonRequest(
            path + '/' + id,
            $.PercServiceUtils.TYPE_PUT,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    function purgeItem(id, path, callback) {
        $.PercServiceUtils.makeJsonRequest(
            path + '/' + id,
            $.PercServiceUtils.TYPE_DELETE,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

})(jQuery);

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

/****
 * Service call for getting search results.
 */
(function($)
{
    $.PercSearchService =
    {
        getSearchResult: getSearchResult,
        getAsyncSearchResult: getAsyncSearchResult,
        getAsyncSearchExtendedResult:getAsyncSearchExtendedResult
    };

    /**
     * Executes a request to get the search results based on entered keyword.
     * @param setUrl the url used to get the search results
     * @param serviceCallback (function) callback function to be invoked when ajax call returns
     */
function getSearchResult(searchCriteriaObj, callback)
    {
	   var setUrl = $.perc_paths.FINDER_SEARCH;

       var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                callback(false,defaultMsg);
            }
            else
            {
                callback(true,results.data);
            }
         };

        $.PercServiceUtils.makeJsonRequest(setUrl, $.PercServiceUtils.TYPE_POST, false, serviceCallback, searchCriteriaObj);

    }	

function getAsyncSearchResult(searchCriteriaObj, callback)
{
    var setUrl = $.perc_paths.SEARCH_PAGE_ASSETS_BY_STATUS;
    
    var serviceCallback = function(status, results){
        if(status === $.PercServiceUtils.STATUS_ERROR)
        {
            var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
            callback(false,defaultMsg);
        }
        else if(status === $.PercServiceUtils.STATUS_ABORT)
        {
            callback(false,I18N.message('perc.ui.search.service@Server Taking Too Long'));
        }
        else
        {
            callback(true,results.data);
        }
    };
    $.PercServiceUtils.makeJsonRequest(setUrl, $.PercServiceUtils.TYPE_POST, false, serviceCallback, searchCriteriaObj, serviceCallback);
    
}
function getAsyncSearchExtendedResult(searchCriteriaObj, callback)
{
    var setUrl = $.perc_paths.FINDER_SEARCH + '/extendedresults';
    
    var serviceCallback = function(status, results){
        if(status === $.PercServiceUtils.STATUS_ERROR)
        {
            var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
            callback(false,defaultMsg);
        }
        else if(status === $.PercServiceUtils.STATUS_ABORT)
        {
            callback(false,I18N.message('perc.ui.search.service@Server Taking Too Long'));
        }
        else
        {
            callback(true,results.data);
        }
    };
    $.PercServiceUtils.makeJsonRequest(setUrl, $.PercServiceUtils.TYPE_POST, false, serviceCallback, searchCriteriaObj, serviceCallback);
    
}

})(jQuery);

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

(function($)
{
    // imports
    var service = $.PercServiceUtils;
    
    $.PercPageService = {
        forceDeletePage : forceDeletePage,
        validateDeletePage : validateDeletePage,
        getNonSEOPages : getNonSEOPages,
        savePageMetadata : savePageMetadata,
        copyPage : copyPage,
        getPagesWithTemplate : getPagesWithTemplate,
        getUnassignedPagesBySite : getUnassignedPagesBySite,
        checkForEmptyMigrationWidgets : checkForEmptyMigrationWidgets,
        clearFlagShowMigrationEmptyMessage : clearFlagShowMigrationEmptyMessage,
        addToMyPages : addToMyPages,
        removeFromMyPages : removeFromMyPages,
        isMyPage : isMyPage,
        getMyContent : getMyContent
    };

    /**
     * Determine if page has empty migration widgets for the given content id. Invoke callback only if it does.
     * Note: any returned errors are ignored.
     *
     * @param {Object} contentId
     * @param {Object} callback
     */
    function checkForEmptyMigrationWidgets (contentId, onPageHasEmptyMigrationWidgets) {
        var url = $.perc_paths.MIGRATION_EMPTY_FLAG + "/" + contentId;
        function serviceCallback (status, results)
        {
            if (status === service.STATUS_SUCCESS)
            {
               if (results.data === true) 
               {
                   onPageHasEmptyMigrationWidgets();
               }
            }
            // nothing to do in case of error
        }
        service.makeJsonRequest(url, service.TYPE_GET, false, serviceCallback);
    }
    
    /**
     * Tell server to clear the migration empty flag.
     * Note: any returned errors are ignored.
     * 
     * @param {Object} contentId
     * @param {Object} callback
     */
    function clearFlagShowMigrationEmptyMessage (contentId) {
        var url = $.perc_paths.CLEAR_MIGRATION_EMPTY_FLAG + "/" + contentId;
        function serviceCallback (status, results)
        {
            //if (status === service.STATUS_ERROR)
            //{
                // nothing to do in case of error
            //}
        }
        service.makeJsonRequest(url, service.TYPE_POST, false, serviceCallback);
    }
    
    /**
     * Deletes a page without validation.
     * @param id of the page we want to delete.
     * @param callback handles success.
     * @param errorCallBack handles errors.
     */
    function forceDeletePage(id, callback, errorCallBack)
    {
        $.ajax(
            {
                url: $.perc_paths.PAGE_FORCE_DELETE + "/" + id, 
                type: 'GET',
                success: callback,
                error: errorCallBack
            });
        
        callback();
    }

    /**
     * Validates that a page may be deleted by the current user.
     * @param id of the page we want to delete.
     * @param callback handles success.
     * @param errorCallBack handles errors and validation warnings.
     */
    function validateDeletePage(id, callback, errorCallBack)
    {
        $.ajax(
            {
                url: $.perc_paths.PAGE_VALIDATE_DELETE + "/" + id, 
                type: 'GET',
                success: callback,
                error: errorCallBack
            });
    }

    /**
     * Retrieves page seo statistics based on specified path, workflow, state, and severity.
     * @param path {string} the finder path where the search should be based in,
     *  cannot be <code>null</code>.
     * @param workflow {string} the workflow to be used for search, cannot be <code>null</code>.
     * @param state {string} the workflow state from the specified workflow, may be
     * <code>null</code> in which case items in any workflow state will be returned.
     * @param severity {string} the minimum seo severity level of the pages.  Pages with a
     * severity greater than or equal to this severity will be returned.
     * @param keyword {string} the keyword to search for , may be <code>null</code> or
     * empty.
     * @param callback {function} the callback function to be called when request 
     * is done. First arg is status second is the following object:
     * <pre>
     *
     * {"SEOStatistics":
     *    [{"issues":["DEFAULT_TITLE","MISSING_DESCRIPTION"],
     *      "linkTitle":"Home",
     *      "path":"\/Sites\/Test\/index",
     *      "severity":100,
     *      "title":"Home",
     *      "description":"The home page"}
     *    ]
     * }
     * </pre>         
     */                   
    function getNonSEOPages(path, workflow, state, severity, keyword, callback)
    {
       if(state === null)
          state = "";
       if(keyword === null)
          keyword = "";   
       var obj = {NonSEOPagesRequest: {
          keyword: keyword,
          path: path,
          workflow: workflow,
          state: state,
          severity: severity
       }};
       $.PercServiceUtils.makeJsonRequest(
          $.perc_paths.PAGE_NONSEO,
          $.PercServiceUtils.TYPE_POST,
          false,
          function(status, result)
          {
             if(status === $.PercServiceUtils.STATUS_SUCCESS)
             {
                callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
             }
             else
             {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
             }
          },
          obj
       
       );
    }
    
    /**
     * Executes a request to update the page title value 
     * @param pageId the id of the page
     */
    function savePageMetadata(pageId,callback)
    {
       var getUrl = $.perc_paths.SAVE_PAGE_METADATA + "/" + pageId;
       
       var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,results.data);
            }
        };        
        $.PercServiceUtils.makeRequest(getUrl, $.PercServiceUtils.TYPE_POST, false, serviceCallback);

    }     
    
    /**
     * Executes a request to copy a page 
     * @param pageId the id of the page
     */
    function copyPage(pageId, callback)
    {
       var url = $.perc_paths.PAGE_COPY + "/" + pageId + "?addToRecent=true";
       // url example: http://localhost:9992/Rhythmyx/services/pagemanagement/page/copy/16777215-101-324
       
       $.PercServiceUtils.makeRequest(
            url,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result){
                if(status === $.PercServiceUtils.STATUS_SUCCESS){
                    callback($.PercServiceUtils.STATUS_SUCCESS, result);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
    	            callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },null,null,null,true
        );
    }     
    
    /**
     * Retrieves the pages that use template.
     * @param templateId {string} the template id.
     * @param serviceParams {object}. the parameters for the service
     * The object should look like this
     * serviceParams = {
     *     startIndex : 1,
     *     maxResults : 5,
     *     sortColumn : 'name',
     *     sortOrder : 'asc'
     * };
     * @param callback {function} the success callback function.
     * @param errorCallback {function} the error callback function.
     */                                 
    function getPagesWithTemplate(templateId, serviceParams, callback, errorCallback)
    {
        // If there are missing values in serviceParams, use default ones
        var defaultServiceParams = {
            startIndex : 1,
            maxResults : 5,
            sortColumn : 'name',
            sortOrder : 'asc',
            pageId : null
        };
        $.extend(defaultServiceParams, serviceParams);
        
        var requestURL = $.perc_paths.PAGES_WITH_TEMPLATE + '/' + templateId;
        requestURL += '?startIndex=' + defaultServiceParams.startIndex;
        requestURL += '&maxResults=' + defaultServiceParams.maxResults;
        requestURL += '&sortColumn=' + defaultServiceParams.sortColumn;
        requestURL += '&sortOrder=' + defaultServiceParams.sortOrder;
        if (defaultServiceParams.pageId != null)
            requestURL += '&pageId=' + defaultServiceParams.pageId;
        
        $.PercServiceUtils.makeJsonRequest(
            requestURL,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if (status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
                else
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data.PagedItemList);
                }
            }
        );
    }
    
    function getUnassignedPagesBySite(siteName, startIndex, maxResults, callback){
        var requestURL = $.perc_paths.UNASSIGNED_PAGES_BY_SITE + "/" + siteName + "?startIndex=" + startIndex + "&maxResults=" + maxResults;
        $.PercServiceUtils.makeJsonRequest(
            requestURL,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if (status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
                else
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
            }
        );
    }

    /**
     * Makes a call to the server to add supplied pageId to logged in user pages.
     * @param {Object} pageId assumed to be string format of page guid.
     * @param {Object} callback function that gets called after AJAX request is completed, the first parameter is status and the second 
     * parameter is server supplied message for success case and extracted error message for the error case.
     */
    function addToMyPages(pageId, callback){
        var requestURL = $.perc_paths.ADD_TO_MYPAGES + "/" + pageId;
        $.PercServiceUtils.makeJsonRequest(
            requestURL,
            $.PercServiceUtils.TYPE_PUT,
            false,
            function(status, result)
            {
                if (status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
                else
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
            }
        );
    }
    
    /**
     * Makes a call to the server to remove supplied pageId from logged in user pages.
     * @param {Object} pageId assumed to be string format of page guid.
     * @param {Object} callback function that gets called after AJAX request is completed, the first parameter is status and the second 
     * parameter is server supplied message for success case and extracted error message for the error case.
     */
    function removeFromMyPages(pageId, callback){
        var requestURL = $.perc_paths.REMOVE_FROM_MYPAGES + "/" + pageId;
        $.PercServiceUtils.makeJsonRequest(
            requestURL,
            $.PercServiceUtils.TYPE_DELETE,
            false,
            function(status, result)
            {
                if (status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
                else
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
            }
        );
    }
    
    /**
     * Makes a call to the server to check whether supplied pageId is in logged in user pages.
     * @param {Object} pageId assumed to be string format of page guid.
     * @param {Object} callback function that gets called after AJAX request is completed, the first parameter is status and the second 
     * parameter is true or false success case and extracted error message for the error case.
     */
    function isMyPage(pageId, callback){
        var requestURL = $.perc_paths.IS_MY_PAGE + "/" + pageId;
        $.PercServiceUtils.makeJsonRequest(
            requestURL,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if (status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
                else
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
            }
        );
    }

     /**
     * Makes a call to the server to get my content.
     * @param {Object} callback function that gets called after AJAX request is completed, the first parameter is status and the second 
     * parameter is error message for the error case and for success case it will be a array of PSItemProperties returned by server.
     */
    function getMyContent(callback){
        var requestURL = $.perc_paths.MY_CONTENT;
        $.PercServiceUtils.makeJsonRequest(
            requestURL,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if (status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
                else
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
            }
        );
        
    }
})(jQuery);

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

(function($)
{
    $.PercPathService = {
        getPathItemById: getPathItemById,
        getFolderPathItem: getFolderPathItem,
        getPathItemForPath : getPathItemForPath,
        getItemPropertiesByWorkflowState: getItemPropertiesByWorkflowState,
        deleteFolder : deleteFolder,
        deleteSection : deleteSection,
        deleteFSFolder : deleteFSFolder,
        moveItem: moveItem,
        renameFolder: renameFolder,
        createNewFolder: createNewFolder,
        getLastExistingPath: getLastExistingPath,
        validatePath:validatePath,
        getDisplayFormat : getDisplayFormat,
        getContentForPath :getContentForPath,
        getFolderProperties: getFolderProperties,
        saveFolderProperties: saveFolderProperties,
        deleteFolderSkipValidation : deleteFolderSkipValidation,
        getInlineRenderLink : getInlineRenderLink
    };

    function getDisplayFormat(callback)
    {
        // Retrieve the corresponding displayformat for the current path
        var displayFormatName = $.perc_utils.getDisplayFormat($.PercNavigationManager.getPath());
        // Search View has no path asigned, so it should always be the default one
        if ($.Percussion.getCurrentFinderView() === $.Percussion.PERC_FINDER_SEARCH_RESULTS || $.Percussion.getCurrentFinderView() === $.Percussion.PERC_FINDER_RESULT)
            displayFormatName = $.perc_utils.getDisplayFormat("/");
        //var url = "http://localhost:9982/Rhythmyx/services/pathmanagement/path/item/displayFormat?userid=100";
        var url = $.perc_paths.DISPLAY_FORMAT + displayFormatName;

        $.PercServiceUtils.makeJsonRequest(
            url,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    function getContentForPath(displayFormat, config, callback)
    {

        var str_path = $.perc_utils.encodeURL(config.path) + "/?startIndex=" + config.startIndex +
            "&maxResults=" + config.maxResults + "&displayFormatId=" + displayFormat.id +
            "&sortColumn=" + config.sortColumn + "&sortOrder=" + config.sortOrder;
        var url = $.perc_paths.PATH_PAGINATED_FOLDER + str_path;
        $.PercServiceUtils.makeJsonRequest(
            url,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }
    /**
     * Get detailed information about a path.
     * @param path we want information about
     * @param callback function to be called when path information is retrieved
     * return JSON object has the following format:
     *
     *   {"PathItem":
     *       {
     *           "id":"16777215-101-713",
     *           "folderPaths":"\/\/Folders\/$System$\/Assets\/Folder1",
     *           "icon":"\/Rhythmyx\/sys_resources\/images\/folder.gif",
     *           "name":"Folder2",
     *           "type":"Folder",
     *           "folderPath":"\/\/Folders\/$System$\/Assets\/Folder1\/Folder2\/",
     *           "leaf":false,
     *           "path":"\/Assets\/Folder1\/Folder2\/"
     *       }
     *   }
     */
    function getPathItemForPath(path, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_ITEM + path,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    /**
     * Get detailed information about an item.
     * @param id {string} the object id, cannot be <code>null</code> or empty.
     * @param callback function to be called when information is retrieved
     * return JSON object has the following format:
     *
     *   {"PathItem":
     *       {
     *           "id":"16777215-101-713",
     *           "folderPaths":"\/\/Folders\/$System$\/Assets\/Folder1",
     *           "icon":"\/Rhythmyx\/sys_resources\/images\/folder.gif",
     *           "name":"Folder2",
     *           "type":"Folder",
     *           "folderPath":"\/\/Folders\/$System$\/Assets\/Folder1\/Folder2\/",
     *           "leaf":false,
     *           "path":"\/Assets\/Folder1\/Folder2\/"
     *       }
     *   }
     */
    function getPathItemById(id, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_ITEM_BY_ID + "/" + id,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    var errorCode = $.PercServiceUtils.extractGlobalErrorCode(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg, errorCode);
                }
            }
        );
    }

    /**
     * Retrieves item Properties based on specified path, workflow and state.
     * @param path {string} the finder path where the search should be based in,
     *  cannot be <code>null</code>.
     * @param workflow {string} the workflow to be used for search, cannot be <code>null</code>.
     * @param state {string} the workflow state from the specified workflow, may be
     * <code>null</code> in which case items in any workflow state will be returned.
     * @param callback {function} the callback function to be called when request
     * is done. Fisrt arg is status second is the following object:
     * <pre>
     *
     * {"ItemProperties":
     *    [{"id":"16777215-101-321",
     *      "lastModifiedDate":"Jun 29, 2010 1:18:14 PM",
     *      "lastModifier":"Admin",
     *      "lastPublishedDate":"",
     *      "name":"Home",
     *      "path":"/Sites/test2/index",
     *      "status": "",
     *      "type":"hkjhkh"}
     *    ]
     * }
     * </pre>
     */
    function getItemPropertiesByWorkflowState(path, workflow, state, callback)
    {
        if(state === null)
            state = "";
        var obj = {ItemByWfStateRequest: {
                path: path,
                workflow: workflow,
                state: state
            }};
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_ITEM_SUMMARY_BY_WORKFLOW_STATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            obj

        );
    }

    /**
     * Move item to another location.
     * @param sourcepath {string} the source item path, cannot be <code>null</code>.
     * @param targetpath {string} the target path, cannot be <code>null</code>.
     * @param callback {function} the callback function to be called when request
     * is done.
     */
    function moveItem(sourcepath, targetpath, callback)
    {
        var obj = {MoveFolderItem: {
                itemPath: sourcepath,
                targetFolderPath: targetpath
            }};
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_ITEM_MOVE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            obj

        );
    }
    /**
     * Creates a new folder on the server.
     * @param path {string} the path of the parent folder, cannot be <code>null</code>
     * or empty.
     * @param callback {function} the function that will be called when the server
     * request is complete, successful or not. Cannot be <code>null</code>. The
     * callback will be passed a PathItem if successful or the error string
     * if not.
     */
    function createNewFolder(path, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_ADD_NEW_FOLDER + path,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }
    /**
     * Request the pathItem object for the specified folder path.
     * @param path {string} the folder path string, cannot be <code>null</code>,
     * or empty.
     * @param callback {function} the function to be called when the server request
     * returns. Cannot be <code>null</code> or empty.
     */
    function getFolderPathItem(path, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_FOLDER +  $.perc_utils.encodeURL(path),
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    /**
     * Renames the specified folder.
     * @param path {string} the folder path, cannot be <code>null</code> or empty.
     * @param newName {string} the new name for the folder, cannot be <code>null</code> or
     * empty.
     * @param callback {function} the function to call after rename completes
     * or has error on the server. The
     * callback will be passed a PathItem if successful or the error string
     */
    function renameFolder(path, newName, callback){
        var obj = {"RenameFolderItem":{"path": path,"name": newName}};
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_RENAME_FOLDER,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    var defaultCode = $.PercServiceUtils.extractFieldErrorCode(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg, defaultCode);
                }
            },
            obj
        );
    }

    /**
     * Deletes a folder and its contents.  The folder is first validated to
     * determine if all child items can be deleted.  Appropriate dialogs are
     * displayed.
     *
     * @param path of the folder as it appears in the finder, i.e., /Sites/MySite/MyFolder.
     * @param name of the folder.
     * @param type of folder (asset, site, section).
     * @param callback function to be executed after a successful deletion.
     */
    function deleteFolder(path, name, type, callback){
        $.ajax({
            url: $.perc_paths.PATH_VALIDATE_DELETE_FOLDER + path,
            dataType: "text",
            type: 'GET',
            success: function(data) {
                cbVdfSuccess(data, path, name, type, callback);},
            error: cbVdfErrors });
    }

    /**
     * Deletes a section and all contents.  Calls deleteFolder(path, name, "section", callback).
     */
    function deleteSection(path, name, callback){
        var shouldPurge = path.indexOf($.perc_paths.RECYCLING_ROOT_NO_SLASH) !== -1;
        $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
        //var guid = $('a.perc-listing-category-FOLDER.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
        //var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":skipItems, "shouldPurge": shouldPurge, "guid":guid}};
        //var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":"NO", "shouldPurge": shouldPurge}};
        var dataJson = JSON.parse($('div.perc-site-map-box-selected').attr("data"));
        var guid = dataJson.id;
        var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":"NO", "shouldPurge": shouldPurge, "guid":guid}};
        var timeoutMillis = 3600000;
        $.ajax({
            url: $.perc_paths.PATH_DELETE_FOLDER,
            type: 'POST',
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(delCriteria),
            timeout: timeoutMillis,
            success: callback,
            error: cbDfErrors
        });
        $.unblockUI();
    }
    /**
     * Deletes a Folder in the filesystem and all of it's files and subfolders.
     * Calls deleteFolder(path, name, "fsfolder", callback).
     */
    function deleteFSFolder(path, name, callback)
    {
        deleteFolder(path, name, "fsfolder", callback);
    }

    /**
     * Checks for the existence of a path and finds the last portion of the path which exists.
     * @param path as it appears in the finder, i.e., /Sites/MySite/MyFolder.
     * @param callback {function} the function to be called when the server request
     * returns. Cannot be <code>null</code> or empty.
     * return last existing path as it appears in the finder (no leading/trailing forward slashes)
     * or empty for root paths ("/Assets", "/Sites").
     */
    function getLastExistingPath(path, callback)
    {
        $.PercServiceUtils.makeRequest(
            $.perc_paths.PATH_LAST_EXISTING + path,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    /**
     * Checks for the existence of a path.
     * @param path as it appears in the finder, i.e., /Sites/MySite/MyFolder.
     * @param callback {function} the function to be called when the server request
     * returns. Cannot be <code>null</code> or empty.
     */
    function validatePath(path, callback)
    {
        $.PercServiceUtils.makeRequest(
            $.perc_paths.PATH_VALIDATE_EXIST + path,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            null,
            null,
            null,
            true
        );
    }

    /**
     * Validate folder delete success callback, shows the warning message and makes a call to the server if user clicks ok.
     */
    function cbVdfSuccess(data, path, name, type, callback)
    {
        var shouldPurge = path.indexOf($.perc_paths.RECYCLING_ROOT_NO_SLASH) !== -1;

        var title;
        if (type === "section")
        {
            title = I18N.message( "perc.ui.finder.section.delete@Title" );
        }
        else
        {
            if (shouldPurge) {
                title = I18N.message("perc.ui.finder.folder.purge@Title");
            } else {
                title = I18N.message("perc.ui.finder.folder.delete@Title");
            }
        }

        /**
         * Set the timeout to one hour (3600 seconds) because of possible large folders.
         */
        var timeoutMillis = 3600000;

        $.perc_utils.confirm_dialog({
            id: 'perc-finder-delete-folder',
            title: title,
            question: createDelWarning(data, name, type, shouldPurge),
            success: function(){
                var skipItems = "EMPTY";
                if($("#perc_delete_folder_force").length > 0)
                {
                    skipItems=$("#perc_delete_folder_force").get(0).checked?"NO":"YES";
                }
                var guid;

                if(typeof $('a.perc-listing-category-FOLDER.perc_last_selected').attr("id")!=='undefined'){
                    guid = $('a.perc-listing-category-FOLDER.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
                }else if(typeof $('a.perc-listing-category-SECTION_FOLDER.perc_last_selected').attr("id")!=='undefined'){
                    guid = $('a.perc-listing-category-SECTION_FOLDER.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
                }else if(typeof $('a.perc-listing-category-SYSTEM.perc_last_selected').attr("id")!=='undefined'){
                    guid = $('a.perc-listing-category-SYSTEM.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
                }else{
                    guid = $('a.perc-listing-category-FOLDER.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
                }

                //var guid = $('a.perc-listing-category-FOLDER.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
                var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":skipItems, "shouldPurge": shouldPurge, "guid":guid}};
                //var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":skipItems, "shouldPurge": shouldPurge}};
                $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
                $.ajax({
                    url: $.perc_paths.PATH_DELETE_FOLDER,
                    type: 'POST',
                    dataType:"json",
                    contentType:"application/json",
                    data:JSON.stringify(delCriteria),
                    timeout: timeoutMillis,
                    success: callback,
                    error: cbDfErrors });
                $.unblockUI();
            },
            width:500});
    }

    /**
     * Delete a folder without making first a request to validations.
     */
    function deleteFolderSkipValidation(path, name, type, callback)
    {
        var data = '';
        cbVdfSuccess(data, path, name, type, callback);
    }

    /**
     * Validate folder delete error callback, shows the error message to the user.
     */
    function cbVdfErrors(error)
    {
        var errorMsg = $.PercServiceUtils.extractDefaultErrorMessage(errors);
        var defMessage = I18N.message("perc.ui.path.service@Failed to Delete Folder");
        $.perc_utils.alert_dialog( {
            id: 'perc-finder-delete-error',
            title: I18N.message("perc.ui.path.service@Delete Folder Error"),
            content: (errorMsg !== "")? errorMsg : defMessage});
    }

    /**
     * Delete folder error callback, shows the error message to the user.
     */
    function cbDfErrors(errors)
    {
        var errorMsg = $.PercServiceUtils.extractDefaultErrorMessage(errors);
        var defMessage = I18N.message("perc.ui.path.service@Failed to Delete Folder");
        $.perc_utils.alert_dialog( {
            id: 'perc-finder-delete-error',
            title: I18N.message("perc.ui.path.service@Delete Folder Error"),
            content: (errorMsg !== "")? errorMsg : defMessage});
    }

    /**
     * Creates a custom delete warning message based on the supplied message type, folder name, and folder type.
     */
    function createDelWarning(type, name, folderType, shouldPurge)
    {
        var confirm;
        var warning;
        var middle = "";
        var del = "";
        if (folderType === "Assets")
        {
            if (shouldPurge) {
                warning = I18N.message("perc.ui.finder.folder.purge@WarningAssets",[name]);
                confirm = I18N.message("perc.ui.finder.folder.purge@ConfirmAssets",[name]);
            } else {
                warning = I18N.message("perc.ui.finder.folder.delete@WarningAssets",[name]);
                confirm = I18N.message("perc.ui.finder.folder.delete@ConfirmAssets",[name]);
            }

            if (type.indexOf("AssetsNotAuthorized") > -1)
            {
                middle += I18N.message( "perc.ui.finder.folder.delete@AssetNotAuthorized" ) + "<br/><br/>";
            }

            if (type.indexOf("AssetsInUseTemplates") > -1)
            {
                middle += I18N.message( "perc.ui.finder.folder.delete@AssetInUseTemplates" ) + "<br/><br/>";
            }

            if (type.indexOf("AssetsInUsePages") > -1)
            {
                middle += I18N.message( "perc.ui.finder.folder.delete@AssetInUsePages" ) + "<br/>";
                del = "<br/><input type='checkbox' id='perc_delete_folder_force' style='width:15px'/> <label class='perc_dialog_label'>" +
                    I18N.message( "perc.ui.finder.folder.delete@DeleteLiveAssets" ) + "</label>";
            }
        }
        else if (folderType === "fsfolder")
        {
            confirm = "perc.ui.finder.fsfolder.delete@Confirm";
            warning = "perc.ui.finder.fsfolder.delete@Warning";
        }
        else
        {
            if (folderType === "Sites")
            {
                if (shouldPurge) {
                    confirm = "perc.ui.finder.folder.purge@ConfirmPages";
                    warning = "perc.ui.finder.folder.purge@WarningPages";
                } else {
                    confirm = "perc.ui.finder.folder.delete@ConfirmPages";
                    warning = "perc.ui.finder.folder.delete@WarningPages";
                }
            }
            else
            {
                confirm = "perc.ui.finder.section.delete@Confirm";
                warning = "perc.ui.finder.section.delete@Warning";
            }

            if (type.indexOf("PagesNotAuthorized") > -1)
            {
                middle += I18N.message( "perc.ui.finder.folder.delete@PageNotAuthorized" ) + "<br/><br/>";
            }

            if (type.indexOf("PagesInUseTemplates") > -1)
            {
                middle += I18N.message( "perc.ui.finder.folder.delete@PageInUseTemplates" ) + "<br/><br/>";
            }

            if (type.indexOf("PagesInUsePages") > -1)
            {
                middle += I18N.message( "perc.ui.finder.folder.delete@PageInUsePages" ) + "<br/>";
                del = "<br/><input type='checkbox' id='perc_delete_folder_force' style='width:15px'/> <label class='perc_dialog_label'>" +
                    I18N.message( "perc.ui.finder.folder.delete@DeleteLinkedPages" ) + "</label>";
            }
        }

        var first;
        if (type === "Success")
        {
            first = I18N.message( confirm, [name] );
        }
        else
        {
            first = I18N.message( warning, [name] ) + "<br/><br/>";
        }

        return first + middle + del;
    }

    /**
     * Get detailed information about a folder, given its id.
     * @param id {string} the object id, cannot be <code>null</code> or empty.
     * @param callback {function} the callback function to be called when request is done, cannot
     * be <code>null</code> or empty. Fisrt arg is status second (result) is the following object:
     * <pre>
     * {
     *    "FolderProperties" : {
     *      "id" : "16777215-101-716",
     *      "name" : "mynewfolder",
     *      "permission" : {
     *        "accessLevel" : "WRITE"
     *      },
     *      "workflowId" : -1,
     *      "allowedSites" : "301,302"
     *    }
     * }
     * </pre>
     */
    function getFolderProperties(id, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_GET_FOLDER_PROPERTIES + "/" + id,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                callback(status, result);
            }
        );
    }

    /**
     * Saves folder properties.
     * @param folderProps {object} that contains the folder properties:
     * <pre>
     * {
     *   "FolderProperties" : {
     *     "name": "mynewfolder",
     *     "id": "16777215-101-703",
     *     "permission": {
     *       "accessLevel": "READ",
     *       "writePrincipals": []
     *       },
     *     "allowedSites":"302,307"
     *   }
     * }
     * </pre>
     * @param callback {function} the callback function to be called when request
     * is done, cannot be <code>null</code>. Fisrt arg is status second is the following object:
     * <pre>
     * {
     *    "NoContent" : {
     *      "operation" :"saveFolderProperties"
     *    }
     * }
     * </pre>
     */
    function saveFolderProperties(folderProps, callback)
    {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.PATH_SAVE_FOLDER_PROPERTIES,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                callback(status, result);
            },
            folderProps
        );
    }

    /**
     * Gets item render link details from server for the given item id, see com.percussion.pagemanagement.service.impl.PSRenderLinkService#renderPreviewLink
     * method for details.
     * @param {Object} itemId assumed to be a valid itemId
     * @param {Object} callback The function that will be called with two arguments,
     *     status (first arg) -- boolean true if service call succeeds otherwise false
     *     data (second arg) -- this will be String error message if status is false, otherwise the data object returned by service call.
     *
     */
    function getInlineRenderLink(itemId, callback)
    {
        var svcUrl = $.perc_paths.RENDER_LINK_PREVIEW + "/" + itemId + "/default";

        $.PercServiceUtils.makeJsonRequest(
            svcUrl,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    callback(true, result.data);
                }
                else
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback(false, defaultMsg);
                }
            }
        );
    }

})(jQuery);

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

(function($){

    /**
     * Constant for no permission
     */
    var ACCESS_NONE = "NONE";

    /**
     * Constant for read permission
     */
    var ACCESS_READ = "READER";

    /**
     * Constant for write permission
     */
    var ACCESS_WRITE = "ASSIGNEE";

    /**
     * Constant for admin permission
     */
    var ACCESS_ADMIN = "ADMIN";

    $.PercUserService =  {
        ACCESS_NONE         : ACCESS_NONE,
        ACCESS_READ         : ACCESS_READ,
        ACCESS_WRITE        : ACCESS_WRITE,
        ACCESS_ADMIN        : ACCESS_ADMIN,
        getUsers            : getUsers,
        findUser            : findUser,
        findDirectoryUsers  : findDirectoryUsers,
        importDirectoryUsers: importDirectoryUsers,
        getDirectoryStatus  : getDirectoryStatus,
        getRoles            : getRoles,
        createUser          : createUser,
        deleteUser          : deleteUser,
        updateUser          : updateUser,
        findRole            : findRole,
        deleteRole          : deleteRole,
        updateRole          : updateRole,
        createRole          : createRole,
        getAvailableUsers   : getAvailableUsers,
        getAccessLevel      : getAccessLevel,
        validateDeleteRole  : validateDeleteRole,
        validateDeleteUsers : validateDeleteUsers,
        changePassword      : changePassword
    };

    /**
     * Get list of usernames
     * @param callback function to be called when list of users is retrieved.
     * response status and list of users is passed back to the callback.
     *
     * Response user list JSON:
     *
     * {"UserList":{"users":["Admin","Contributor"]}}
     *
     */
    function getUsers(callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_USERS,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    //Convert all users values to string.
                    result.data.UserList.users = convertToString(result.data.UserList.users);
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    /**
     * Find a user by username
     * @param username username of user to be found.
     * @param callback function to be called when user is retrieved.
     * response status and userObj is passed back to the callback.
     *
     * Response userObj JSON:
     *
     * {"User":{"name":"","roles":["Admin","Contributor"]}}
     *
     */
    function findUser(username, callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_FIND + "/" + username,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    result.data.User.name = result.data.User.name.toString();//CXF return a numeric value if the name contains just numbers
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    /**
     * Change password of current user only
     * @param userObj user object which contains username and new password.
     * @param callback function to be called when user is retrieved.
     * response status and userObj is passed back to the callback.
     *
     * Response userObj JSON:
     *
     * {"User":{"name":""}}
     *
     */
    function changePassword(userObj, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.CHANGEPW,
            $.PercServiceUtils.TYPE_PUT,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            userObj
        );
    }

    /**
     * Find directory users by usernameStartsWith
     *
     * @param usernameStartsWith {string} can be empty string
     * @param callback {function} function to be called when user is retrieved.
     * Response status and usersObj is passed back to the callback.
     *
     * Response usersObj JSON:
     *
     * {"ExternalUsers" : [ {"name":"Alice"}, {"name":"Bob"}, {"name":"Charlie"} ] }
     *
     */
    function findDirectoryUsers(usernameStartsWith, callback){

        if(usernameStartsWith === null) {
            callback($.PercServiceUtils.STATUS_ERROR, I18N.message("perc.ui.user.service@Null String"));
            return;
        }
        usernameStartsWith = usernameStartsWith.replace("%", "*");
        if(!usernameStartsWith.endsWith("*"))
            usernameStartsWith = usernameStartsWith + "*";
        var urlfindExternalUsernamesThatStartwith = $.perc_paths.USER_EXTERNAL_FIND + "/" + encodeURIComponent(usernameStartsWith);

        $.PercServiceUtils.makeJsonRequest(
            urlfindExternalUsernamesThatStartwith,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {

                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);

                } else {

                    try {
                        var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                        callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                    } catch (err) {
                        callback($.PercServiceUtils.STATUS_ERROR, I18N.message("perc.ui.user.service@Unable To Retrieve Users"));
                    }
                }
            }
        );
    }

    /**
     * Imports list of users.
     * @param usersJSON {object} containing list of users to be imported with the following data structure
     *
     * {"ImportUsers":{"externalUsers":[{"name":"a"},{"name":"b"},{"name":"c"}]}}
     *
     * @param callback {function} the callback function to be called when the request completes. Passes back the list of users that
     * have and have not been imported and the reason why they were not imported. The data structure of response
     *
     * {"ImportedUser":[{"name":"a","status":"SUCCESS"},{"name":"b","status":"DUPLICATE"},{"name":"c","status":"ERROR"}]}
     *
     */
    function importDirectoryUsers(usersJSON, callback){

        if(usersJSON === null || usersJSON === undefined || usersJSON.ImportUsers.externalUsers.length === 0) {
            callback($.PercServiceUtils.STATUS_ERROR, I18N.message("perc.ui.user.service@Null or Empty List of Users"));
            return;
        }

        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_EXTERNAL_IMPORT,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {

                if(status === $.PercServiceUtils.STATUS_SUCCESS) {

                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);

                } else {

                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);

                }
            },
            usersJSON);
    }


    /**
     * Get the status of the directory service
     *
     * @param callback {function} to be called when user is retrieved.
     * response status and usersObj is passed back to the callback.
     *
     * Response
     *
     * {"DirectoryServiceStatus":{"status":"ENABLED"}}
     *
     */
    function getDirectoryStatus(callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_EXTERNAL_STATUS,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {

                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);

                } else {

                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);

                }
            }
        );
    }

    /**
     * Get list of roles
     * @param callback function to be called when list of roles is retrieved.
     * response status and list of roles is passed back to the callback.
     *
     * Response looks as follows:
     *
     * {"RoleList":{"roles":["Admin","Contributor","Editor"]}}
     *
     */
    function getRoles(callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_ROLES,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    //Convert all roles values to string.
                    result.data.RoleList.roles = convertToString(result.data.RoleList.roles);
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                }
                else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }


    /**
     * Create a new user. This is a JSON only call and returns json in
     * the callback.
     * @param userObj the user object to be created. Cannot be <code>null</code>.
     * contains username, password and list of roles. Password can be left blank.
     * @param callback the callback function to be called when the request completes.
     *
     * Response userObj JSON:
     *
     * {"User":{"name":"username", "password":"p@$$w0rd", "roles":["Admin","Contributor"]}}
     *
     */
    function createUser(userObj, callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_CREATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {

                if(status === $.PercServiceUtils.STATUS_SUCCESS) {

                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);

                } else {

                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            userObj);
    }


    /**
     * Delete a user by username
     * @param username username of user to be found.
     * @param callback function to be called when user is retrieved.
     * response status and userObj is passed back to the callback.
     */
    function deleteUser(username, callback) {
        $.PercServiceUtils.makeDeleteRequest(
            $.perc_paths.USER_DELETE + "/" + username,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {

                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);

                } else {

                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            }
        );
    }

    /**
     * Update An Existing User. This is a JSON only call and returns json in
     * the callback.
     * @param userObj the user object to be updated. Cannot be <code>null</code>.
     * contains new password and new list of roles. If password is null or blank, password stays unchanged
     * if list of roles is empty, user will be removed from all roles.
     * If password is empty, password will not be changed on the server.
     * Username will be ignored by the server.
     * @param callback the callback function to be called when the request completes.
     *
     * Response userObj JSON:
     *
     * {"User":{"name":"username", "password":"p@$$w0rd", "roles":["Admin","Contributor"]}}
     *
     */
    function updateUser(userObj, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_UPDATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {

                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);

                } else {

                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);

                }
            },
            userObj
        );
    }

    /*ROLE SECTION*/
    /**
     * Find a role using rolename
     * @param rolename name of role to be found.
     * @param callback function to be called when user is retrieved.
     * response status and roleObj is passed back to the callback.
     *
     * Response roleObj JSON:
     *
     * {"Role":{"name":"","description":"","users":["Admin","Admin2"]}}
     *
     */
    function findRole(rolename, callback){
        var strObj = {"psstring":{"value":rolename}};
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_FIND,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    result.data.Role.name = result.data.Role.name.toString(); //CXF return a numeric value if the name contains just numbers
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            strObj
        );
    }

    /**
     * Delete a role by rolename
     * @param rolename name of role to be delete.
     * @param callback function to be called when user is retrieved.
     * response status and roleObj is passed back to the callback.
     */
    function deleteRole(rolename, callback){
        var strObj = {"psstring":{"value":rolename}};
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_DELETE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            strObj
        );
    }

    /**
     * Create a new role. This is a JSON only call and returns json in
     * the callback.
     * @param roleObj the role object to be created. Cannot be <code>null</code>.
     * contains rolename, description and list of users.
     * @param callback the callback function to be called when the request completes.
     *
     * Response roleObj JSON:
     *
     * {"Role":{"name":"rolename","description":"Description text", "users":["user1","user2"]}}
     *
     */
    function createRole(roleObj, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_CREATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            roleObj
        );
    }

    /**
     * Update An Existing Role. This is a JSON only call and returns json in
     * the callback.
     * @param roleObj the role object to be updated. Cannot be <code>null</code>.
     * contains new description and new list of users.
     * if list of users is empty, role will be removed from all users.
     * Rolename will be ignored by the server.
     * @param callback the callback function to be called when the request completes.
     *
     * Response roleObj JSON:
     *
     * {"Role":{"name":"rolename", "description":"new Description Text", "users":["user1","userNew"]}}
     *
     */
    function updateRole(roleObj, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_UPDATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            roleObj
        );
    }

    /**
     * Get the available users by an existing Role. This is a JSON only call and returns json in
     * the callback.
     * @param roleObj the role object. Cannot be <code>null</code>.
     * contains the role name. {"Role":{"name":"RoleName"}}
     * @param callback the callback function to be called when the request completes.
     *
     * Response roleObj JSON:
     *
     * {"Role":{"name":"rolename", "users":["user1","userNew"]}}
     *
     */
    function getAvailableUsers(roleObj, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_AVAILABLE_USERS,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    //Convert all users values to string.
                    result.data.UserList.users = convertToString(result.data.UserList.users);
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            roleObj
        );
    }

    /**
     * Finds the access level for the current user. This returns json in the callback.
     * @param type the default workflow of the specified content type will be used.
     * @param itemId the id of the item selected.
     * @param parentFolderPath the path of the folder to check the workflow id if needed.
     * @param sync Optional param to make the ajax call synchronous, false by default
     * @callback (Function), callback function with status and result. if status is $.PercServiceUtils.STATUS_SUCCESS the
     *           result would be one of ACCESS_XXX value. if the status is $.PercServiceUtils.STATUS_ERROR then result
     *           would be the error message.
     */
    function getAccessLevel(type, itemId, callback, parentFolderPath){
        var reqObj;
        if (type != null)
        {
            reqObj = {"AccessLevelRequest":{"type":type, "itemId":itemId, "parentFolderPath": parentFolderPath}};
        }
        else
        {
            reqObj = {"AccessLevelRequest":{"itemId":itemId, "parentFolderPath": parentFolderPath}};
        }

        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.USER_ACCESS_LEVEL,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data.AccessLevel.accessLevel);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            reqObj
        );
    }

    /**
     * Validate a Role before delete it. This is a JSON only call and returns json in
     * the callback.
     * @param roleObj the role object to be validate. Cannot be <code>null</code>.
     * contains the role name and the assigned users list.
     * @param callback the callback function to be called when the request completes.
     *
     * roleObj:
     *
     * {"Role":{"name":"rolename", "users":["user1","user2"]}}
     *
     */
    function validateDeleteRole(roleObj, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_DELETE_VALIDATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            roleObj
        );
    }

    /**
     * Validate the user list to be removed from a Role. This is a JSON only call and returns json in
     * the callback.
     * @param UserList Users List to be validated. Cannot be <code>null</code>.
     * @param callback the callback function to be called when the request completes.
     *
     * UserList:
     *
     * {"UserList":{"users":["Admin","Contributor"]}}
     *
     */
    function validateDeleteUsers(userList, callback){
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.ROLE_REMOVE_USERS_VALIDATE,
            $.PercServiceUtils.TYPE_POST,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                } else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },
            userList
        );
    }

    /**
     * Convert all array values to string.
     * CXF return a mixed array (numeric and string values if some of the values contains just numbers).
     */
    function convertToString(itemList){
        itemList = $.perc_utils.convertCXFArray(itemList);
        $.each(itemList, function(k,v){
            itemList[k] = v.toString();
        });
        return itemList;
    }

})(jQuery);

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
 * PercCookieConsentService.js
 * Cookie Consent service, makes a call to the server to log and retrieve
 * cookie consent information.  Interfaces with DTS backend via perc-metadata-services
 * end points.
 */
(function($)
{
    $.PercCookieConsentService = {
        getAllCookieConsentEntries           : getAllCookieConsentEntries,
        exportCookieCSV                      : exportCookieCSV,
        getCookieConsentEntriesPerSite       : getCookieConsentEntriesPerSite,
        deleteAllCookieConsentEntries        : deleteAllCookieConsentEntries,
        deleteAllCookieConsentEntriesForSite : deleteAllCookieConsentEntriesForSite
    };

    /**
     * Gets all cookie consent entry information from DB.
     * Response from server should be a Map<String, Integer>.
     * @param {*} callback - returns data/response to calling function.
     */
    function getAllCookieConsentEntries(site,callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.COOKIE_CONSENT_TOTALS+ "/" + site,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data, I18N.message("perc.ui.gadgets.cookieConsent@Success retrieving cookie consent entries"));
                }
                else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg, I18N.message("perc.ui.gadgets.cookieConsent@No cookie consent entries found"));
                }
            }
        );
    }

    /**
     * Gets all services that have been logged for the specified
     * site with totals for each service.
     * 
     * @param {*} siteName - the site in which to get entries for.
     * @param {*} callback - returns data to calling function.
     */
    function getCookieConsentEntriesPerSite(siteName, callback) {
        $.PercServiceUtils.makeJsonRequest(
            $.perc_paths.COOKIE_CONSENT_TOTALS + "/" + siteName,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    callback($.PercServiceUtils.STATUS_SUCCESS, result.data, I18N.message("perc.ui.gadgets.cookieConsent@Success retrieving cookie consent entries"));
                }
                else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg, I18N.message("perc.ui.gadgets.cookieConsent@No cookie consent entries found"));
                }
            }
        );
    }

    /**
     * Gets all cookie consent entries from DTS DB
     * and exports in .CSV format.
     * 
     * @param url - The URL for the Sitemanage endpoint which returns
     * a String in .CSV format.
     */
    function exportCookieCSV(url, callback) {
        $.PercServiceUtils.makeRequest(
            url,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    result.url = url;
                    callback($.PercServiceUtils.STATUS_SUCCESS, result);
                }
                else {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback($.PercServiceUtils.STATUS_ERROR, defaultMsg);
                }
            },"","text/csv","text");
    }

    /**
     * Deletes all cookie consent entries
     * @param {*} url - the url for the sitemanage cookie consent delete service
     * @param {*} callback 
     * @param {*} errorCallBack 
     */
    function deleteAllCookieConsentEntries(url, callback, errorCallBack) {
        $.ajax({
            url     : url, 
            type    : $.PercServiceUtils.TYPE_DELETE,
            success : function() {
                callback();
            },
            error   : errorCallBack
        });
    }

    function deleteAllCookieConsentEntriesForSite(url, callback, errorCallBack) {
        $.ajax({
            url   : url,
            type  : $.PercServiceUtils.TYPE_DELETE,
            success: function() {
                callback();
            },
            error : errorCallBack
        });
    }

})(jQuery);

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

(function($)
{
    $.PercWebResourcesService = {
        	deleteFile : deleteFile,
        	validateFileUpload : validateFileUpload
    };

    /**
     * Validate folder delete success callback, shows the warning message and makes a call to the server if user clicks ok. 
     */
    function cbVdfSuccess(data, path, name, type, callback)
    {
        var shouldPurge = path.indexOf($.perc_paths.RECYCLING_ROOT_NO_SLASH) !== -1;

        var title;
        if (type === "section")
        {
            title = I18N.message( "perc.ui.finder.section.delete@Title" );
        }
        else
        {
            if (shouldPurge) {
                title = I18N.message("perc.ui.finder.folder.purge@Title");
            } else {
                title = I18N.message("perc.ui.finder.folder.delete@Title");
            }
        }

        $.perc_utils.confirm_dialog({ 
              id: 'perc-finder-delete-folder',
              title: title,
              question: createDelWarning(data, name, type),
              success: function(){ 
                  var skipItems = "EMPTY";
                  if($("#perc_delete_folder_force").length > 0)
                  {
                    skipItems=$("#perc_delete_folder_force").get(0).checked?"NO":"YES";
                  }
                  var guid = $('a.perc-listing-category-FOLDER.perc_last_selected').attr("id").split("perc-finder-listing-")[1];
                  var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":skipItems, "shouldPurge": shouldPurge, "guid":guid}};
                  //var delCriteria  = {"DeleteFolderCriteria":{"path":path,"skipItems":skipItems, "shouldPurge": shouldPurge}};
                  $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
                  $.ajax({
                        url: $.perc_paths.PATH_DELETE_FOLDER, 
                        type: 'POST',
                        dataType:"json", 
                        contentType:"application/json", 
                        data:JSON.stringify(delCriteria),
                        success: callback,
                        error: cbDfErrors });
                   $.unblockUI(); 
              },
              width:500});
    }

    /**
	 * Delete file error callback, shows the error message to the user.
	 */
	function cbDfileErrors(errors) {
		var errorMsg = $.PercServiceUtils.extractDefaultErrorMessage(errors);
		var defMessage = I18N.message("perc.ui.web.resources.service@Could Not Delete File");
		$.perc_utils.alert_dialog({
			id : 'perc-finder-delete-error',
			title : I18N.message("perc.ui.web.resources.service@Delete File Error"),
			content : (errorMsg !== "") ? errorMsg : defMessage
		});
	}

	/**
	 * Creates a custom delete warning message based on the supplied file name.
	 */
	function createDelWarning(name) {
		var confirm;
		var middle;

		// type === fsfile
		middle = "perc.ui.finder.fsfile.delete@Filename";
		confirm = "perc.ui.finder.fsfile.delete@Confirm";

		var first;
		first = I18N.message(middle, [ name ]);
		first = first + "<br /><br />" + I18N.message(confirm, [ name ]);

		return first;
	}
    
    function deleteFile(path, name, callback)
    {
        var title = I18N.message( "perc.ui.finder.fsfile.delete@Title" );

        $.perc_utils.confirm_dialog({ 
              id: 'perc-finder-delete-fsfile',
              title: title,
              question: createDelWarning(name),
              success: function(){ 
                  $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
                  $.PercServiceUtils.makeDeleteRequest(
                      $.perc_paths.WEBRESOURCESMGT + path,
                      false,
                      function(status, result) {
                          if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                              callback($.PercServiceUtils.STATUS_SUCCESS, result.data);
                          } else {
                              cbDfileErrors(result.request);
                          }
                      }
                    );
                  $.unblockUI();
              },
              width:500});
    }
    
    /**
     * Checks if file exist given a name and path from the finder, under the Design node.
     * 
     * @param path
     * @param fileName
     * @param callback
     */
    function validateFileUpload(path, fileName, callback)
    {
        // Take the path from the finder, and joint everything (except "Web resources") with '/'. 
        // Append the filename to that.
        path = '/' + path.slice(3).join('/') + '/' + fileName;
                
        $.PercServiceUtils.makeRequest(
            $.perc_paths.WEBRESOURCESMGT_VALIDATE_FILE_UPLOAD + path,
            $.PercServiceUtils.TYPE_GET,
            false,
            function(status, result)
            {
                callback(status, result);
            }
        );
    }
})(jQuery);

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
 * Service to handle the workflow transitions and check in and check out of items.
 */

(function($)
{
    $.PercWorkflowService = function()
    {
        return {
            checkIn : checkIn,
            checkOut : checkOut,
            forceCheckOut : forceCheckOut,
            transition : transition,
            getTransitions : getTransitions,
            isCheckedOutToCurrentUser: isCheckedOutToCurrentUser,
            isApproveAvailableToCurrentUser: isApproveAvailableToCurrentUser,
            getWorkflowObject: getWorkflowObject,
            createNewWorkflowStep : createNewWorkflowStep,
            deleteWorkflowStep : deleteWorkflowStep,
            updateWorkflowStep : updateWorkflowStep,
            getStatusByWorkflow : getStatusByWorkflow,
            getWorkflowList:getWorkflowList,
            getWorkflows: getWorkflows,
            createWorkflow : createWorkflow,
            updateWorkflow : updateWorkflow,
            deleteWorkflow : deleteWorkflow,
            bulkApproveItems: bulkApproveItems,
            getBulkApproveStatus: getBulkApproveStatus,
            getDefaultWorkflow: getDefaultWorkflow
        };
    };

    /**
     * Retrieve the status of a Workflow
     * @param workflow (string) name of the workflow. Note: the Url changes according to the workflow name.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function getStatusByWorkflow(workflow, callback){
        var url = "/Rhythmyx/services/workflowmanagement/workflows/" + $.perc_utils.encodeURL(workflow) + "/states/choices";
        callback = callback === null?function(){}:callback;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }
    /**
     * Makes an Ajax call to server to check out the item, Expects the following object from the server as result.
     * {"ItemUserInfo":
     *      {"checkOutUser":"user1",
     *       "currentUser":"editor1",
     *       "itemName":"Home Page",
     *       "assignmentType":true}}
     * See PSItemUserInfo for more details.
     * @param itemId (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function checkOut(itemId, callback)
    {
        if($.perc_utils.isBlankString(itemId)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank Item ID checkOut"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_CHECKOUT + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }

    /**
     * Makes an ajax call to the server to checkIn the supplied item.
     * @param itemId (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function checkIn(itemId, callback)
    {
        if($.perc_utils.isBlankString(itemId)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank Item ID checkIn"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_CHECKIN + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }

    /**
     * Makes an Ajax call to get the avialable transitions to the logged in user for the supplied item.
     * Expects the results from server in the following format.
     * {"ItemStateTransition":
     *  { "itemId":"2-101-781",
     *    "stateId":"4",
     *    "workflowId":"7",
     *    "transitionTriggers":["Publish","Reject"]
     *  }
     * }
     * @param itemId (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function getTransitions(itemId, callback)
    {
        if($.perc_utils.isBlankString(itemId)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank Item ID getTransitions"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_TRANSITIONS + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }

    /**
     * Makes an Ajax call to the server to transition the supplied item.
     * @param itemId (string), must not be blank.
     * @param transitionName, must not be blank
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function transition(itemId, transitionName, comment, callback)
    {
        if($.perc_utils.isBlankString(itemId)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank Item ID Transition"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_TRANSITION_COMMENT + "/" + itemId + "/" + transitionName + "?comment=" + comment;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }

    /**
     * Call this method to checkIn an item that has been checkedout to someone else. The logged in user must be an admin.
     * Use checkOut method first and then call this method based the results of that call.
     * Makes an Ajax call to server to check in the item first and then check it out, Expects the following object from
     * the server as result.
     * {"ItemUserInfo":
     *      {"checkOutUser":"user1",
     *       "currentUser":"editor1",
     *       "itemName":"Home Page",
     *       "assignmentType":true}}
     * @param itemId (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function forceCheckOut(itemId,callback){
        if($.perc_utils.isBlankString(itemId)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank Item ID"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_FORCE_CHECKOUT + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);

    }

    /**
     * Makes an Ajax call to the server to find whether the supplied item is checked out to the current user or not.
     * @param itemId (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function isCheckedOutToCurrentUser(itemId,callback){
        if($.perc_utils.isBlankString(itemId)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank Item ID"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_CHECKED_OUT_TO_USER + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }

    /**
     * Makes an Ajax call to the server to find whether the currently logged in user has previleges to
     * perform an approve in the specified folder
     * @param folderPath The path to the folder
     * @param callback (function) callback function to be invoked when ajax call returns,
     * @return true if the current user has preveliges to perform approve, otherwise false
     */
    function isApproveAvailableToCurrentUser(folderPath, callback){
        if($.perc_utils.isBlankString(folderPath)){
            $.perc_utils.debug(I18N.message("perc.ui.workflow.service@Blank folderPath"));
            return false;
        }
        callback = callback === null?function(){}:callback;
        var url = $.perc_paths.WORKFLOW_IS_APPROVE_ALLOWED + folderPath;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }


    /**
     * Makes an Ajax call to server to get the workflow object
     * {"Workflow":{
                    "workflowName":"Default Workflow",
                    "workflowSteps":[{
                                      "stepName":"Draft",
                                      "stepRoles":[
                                                    {"roleName":"Admin"},
                                                    {"roleName":"Contributor"},
                                                    {"roleName":"Editor"}
                                                   ]
                                     }]
                    }
        }
     * See PSItemUserInfo for more details.
     * @param workflowName (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function getWorkflowObject(workflowName, callback)
    {
        var Url = $.perc_paths.WORKFLOW_STEPPED + workflowName;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback($.PercServiceUtils.STATUS_ERROR, [results.request,results.textstatus,results.error]);
            }
            else
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, [results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_GET, false, serviceCallback);
    }

    /**
     * Makes an Ajax call to server to update existing step
     *     "NewWorkflowStep": {
                                "workflowName" : workflowName,
                                "previousStepName" : previousStepName,
                                "workflowStep":[{
                                            "stepName":"Reveiw One",
                                            "stepRoles":stepRoles
                                }]
                            }
     * See PSItemUserInfo for more details.
     * @param workflowName (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function updateWorkflowStep(workflowName,stepName, StepObj, callback)
    {
        var Url = $.perc_paths.WORKFLOW_STEPPED +  workflowName + "/steps/" + stepName;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_PUT, false, serviceCallback, StepObj);
    }

    /**
     * Makes an Ajax call to server to update workflow object with new step
     *     "NewWorkflowStep": {
                                "workflowName" : workflowName,
                                "previousStepName" : previousStepName,
                                "workflowStep":[{
                                            "stepName":"Reveiw One",
                                            "stepRoles":stepRoles
                                }]
                            }
     * See PSItemUserInfo for more details.
     * @param workflowName (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function createNewWorkflowStep(workflowName,stepName, newStepObj, callback)
    {
        if(typeof stepName === 'undefined' || stepName === null || stepName.trim() === "" ){
            $.perc_utils.alert_dialog(
                {
                    "title" : I18N.message("perc.ui.workflow.view@Error Creating Workflow"),
                    "content" : "Workflow Step name can't be blank"
                });
            $.unblockUI();
            return;
        }

        stepName = stepName.replace(/[\\\/;%"]/g,'#');

        newStepObj.Workflow.workflowSteps[0].stepName = stepName;

        var Url = $.perc_paths.WORKFLOW_STEPPED +  workflowName + "/steps/" + encodeName(stepName);

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_POST, false, serviceCallback, newStepObj);
    }
    /**
     * Makes an Ajax call to server to update workflow object with deleted step
     * @param workflowName (string), must not be blank.
     * @param stepName (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */

    function deleteWorkflowStep(workflowName,stepName, callback)
    {
        var Url = $.perc_paths.WORKFLOW_STEPPED +  workflowName + "/steps/" + stepName ;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_DELETE, false, serviceCallback);
    }

    /**
     * Makes an Ajax call to server to get list of all workflows
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */

    function getWorkflowList(callback)
    {
        var Url = $.perc_paths.WORKFLOW_STEPPED;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                results.data.EnumVals.entries = $.perc_utils.convertCXFArray(results.data.EnumVals.entries);
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_GET, false, serviceCallback);
    }

    /**
     * Makes an Ajax call to server to get the information of default workflow (name and id)
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function getDefaultWorkflow(callback)
    {
        var Url = $.perc_paths.DEFAULT_WORKFLOW_META;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                results.data.EnumVals.entries = $.perc_utils.convertCXFArray(results.data.EnumVals.entries);
                callback(true,results.data);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_GET, false, serviceCallback);
    }

    /**
     * Makes an Ajax call to server to get list of all availabel workflows plus info on which one is default workflow
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */

    function getWorkflows(callback)
    {
        var Url = $.perc_paths.WORKFLOW_META;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_GET, false, serviceCallback);

    }

    function createWorkflow(workflowName, workflowObj, callback)
    {
        if(typeof workflowName === 'undefined' || workflowName === null || workflowName.trim() === "" ){
            $.perc_utils.alert_dialog(
                {
                    "title" : I18N.message("perc.ui.workflow.view@Error Creating Workflow"),
                    "content" : "Workflow Name can't be blank"
                });
            $.unblockUI();
            return;
        }

        workflowName = workflowName.replace(/[\\\/;%"]/g,'#');

        workflowObj.Workflow.workflowName = workflowName;

        var Url = $.perc_paths.WORKFLOW_STEPPED + encodeName(workflowName);



        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_POST, false, serviceCallback, workflowObj);

    }

    /**
     * Makes an Ajax call to server to update existing workflow name
     *     {"Workflow":
                    {"default":false,
                     "workflowName":"New Workflow Name",
                     "previousStepName":"",
                     "workflowDescription":"",
                     "stagingRoleNames" : publishNowRoles,
                     "previousWorkflowName":"Old Workflow Name"}}"

     * See PSItemUserInfo for more details.
     * @param workflowName (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */
    function updateWorkflow(prevWfName, workflowObj, callback)
    {
        var Url = $.perc_paths.WORKFLOW_STEPPED + prevWfName;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_PUT, false, serviceCallback, workflowObj);
    }

    /**
     * Decodes a given name to be added to a URL
     * @param nameToEncode (function) nameToEncode the name to be encoded, not <code>null</code>
     */
    function encodeName(nameToEncode)
    {
        var encodedName = nameToEncode.toString();
        encodedName = encodedName.replace(/[\\\/;"]/g,'#');
        return encodeURIComponent(JSON.stringify(encodedName));
    }

    /**
     * Makes an Ajax call to server to update workflow object with deleted step
     * @param workflowName (string), must not be blank.
     * @param stepName (string), must not be blank.
     * @param callback (function) callback function to be invoked when ajax call returns, may be <code>null</code>
     * See $.PercServiceUtils.makeJsonRequest for parameter details, when this function is called.
     */

    function deleteWorkflow(workflowName, callback)
    {
        var Url = $.perc_paths.WORKFLOW_STEPPED +  workflowName;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_DELETE, false, serviceCallback);
    }

    function bulkApproveItems(approvalItems, callback)
    {
        var Url = $.perc_paths.WORKFLOW_BULK_APPROVE;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback($.PercServiceUtils.STATUS_ERROR,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, [results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_POST, false, serviceCallback, approvalItems);

    }

    function getBulkApproveStatus(jobId, isFull, callback)
    {
        var Url = $.perc_paths.WORKFLOW_BULK_APPROVE + "/status";
        if(isFull)
            Url += "/full/" + jobId;
        else
            Url += "/processed/" + jobId;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback($.PercServiceUtils.STATUS_ERROR, [results.request,results.textstatus,results.error]);
            }
            else
            {
                callback($.PercServiceUtils.STATUS_SUCCESS, [results.data,results.textstatus]);
            }
        };
        $.PercServiceUtils.makeJsonRequest(Url, $.PercServiceUtils.TYPE_GET, false, serviceCallback);

    }

})(jQuery);

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
 * Service to handle the item revision related actions.
 */

(function($)
{
    //Public API
    $.PercRevisionService = 
    {
            getRevisionDetails : getRevisionDetails,
            restoreRevision : restoreRevision,
            getLastComment : getLastComment
    };
    
    /**
     * Makes a call to the server and calls the supplied callback with status and result. See $.PercServiceUtils.makeJsonRequest
     * for more details.
     */
    function getRevisionDetails(itemId, callback)
    {
        var url = $.perc_paths.ITEM_REVISIONS + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
        /* Test Data****
        var result = [{revId:1,lastModifiedDate:"Jul 21, 2010 1:33:13 PM",lastModifier:"Admin", status:"Live"},
                      {revId:2,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:3,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:4,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:5,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:6,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:7,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:8,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:9,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:10,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:11,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:12,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:13,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:14,lastModifiedDate:"Jul 22, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:15,lastModifiedDate:"Jul 24, 2010 1:33:13 PM",lastModifier:"Editor", status:"Pending"},
                      {revId:16,lastModifiedDate:"Jul 25, 2010 1:33:13 PM",lastModifier:"Admin", status:"Live"}];
        callback($.PercServiceUtils.STATUS_SUCCESS,result);
        */
    }
    
    /**
     * Makes a json request to restore a revision and calls the supplied callback with the results.
     * @param itemId, the guid representation of the id of page or asset. 16777215-101-709
     * @param revId, the id of the revision that needs to be restored.
     * @param callback the callback function that gets called from PercServiceUtils#makeJsonRequest,
     * Please see that method for the description of the arguments with which the function is called.
     */
    function restoreRevision(itemId, revId, callback)
    {
        //Replace the revision in item id
        var ida = itemId.split("-");
        ida[0] = revId;
        itemId = ida.join("-");
        var url = $.perc_paths.ITEM_PROMOTE_REVISION + "/" + itemId;
        $.PercServiceUtils.makeJsonRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }
    
    function getLastComment(itemId, callback){
        var url = $.perc_paths.ITEM_LAST_COMMENT + "/" + itemId;
        $.PercServiceUtils.makeRequest(url,$.PercServiceUtils.TYPE_GET,false,callback);
    }
})(jQuery);
    

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

/****
 * Service calls for the Publish page/assest.
 */
(function($)
{
    $.PercItemPublisherService =
        {
            publishItem: publishItem,
            takeDownItem: takeDownItem,
            publishToStaging: publishToStaging,
            removeFromStaging:removeFromStaging,
            getPublishActions: getPublishActions,
            setScheduleDates: setScheduleDates,
            getScheduleDates: getScheduleDates,
            isDefaultServerModified: isDefaultServerModified,

            PUBLISHER_JOB_STATUS_FORBIDDEN: "FORBIDDEN",
            PUBLISHER_JOB_STATUS_BADCONFIG: "BADCONFIG",
            PUBLISHER_JOB_STATUS_NOSTAGING_SERVERS: "NOSTAGING_SERVERS",
            PUBLISHER_JOB_STATUS_BADCONFIG_MULTIPLE_SITES: "BADCONFIGMULTIPLESITES"

        };

    /**
     * Publishes an item.
     * @param itemId (string) the id of the item to be published
     * @param itemType (string) the type of item (Page/Asset)
     * @param callback (function) callback function to be invoked when ajax call returns
     */
    function publishItem(itemId, itemType, callback)
    {
        var publishUrl = itemType === 'Page' ? $.perc_paths.PAGE_PUBLISH:$.perc_paths.RESOURCE_PUBLISH;
        publishUrl+="/" + itemId;

        _executePublishAction(publishUrl, callback);
    }

    /**
     * Takes down (unpublishes) an item.
     * @param itemId (string) the id of the item to be taken down
     * @param itemType (string) the type of item (Page/Asset)
     * @param callback (function) callback function to be invoked when ajax call returns
     */
    function takeDownItem(itemId, itemType, callback)
    {

        var findLinkedItemsUrl = $.perc_paths.ITEM_LINKED_TO_ITEM + "/" + itemId;
        var takeDownUrl = itemType === 'Page' ? $.perc_paths.PAGE_TAKEDOWN : $.perc_paths.RESOURCE_TAKEDOWN;
        takeDownUrl+="/" + itemId;

        $.PercServiceUtils.makeJsonRequest(findLinkedItemsUrl, $.PercServiceUtils.TYPE_GET, false, function(status, result) {
            if (status === $.PercServiceUtils.STATUS_ERROR) {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result);
                console.error(defaultMsg);
                // if there is an error, we proceed with previous behavior (no confirm display)
                $.PercBlockUI();
                _executePublishAction(takeDownUrl, callback, result.data.ArrayList);
            }
            else {
                if (result.data != null && result.data.ArrayList != null && result.data.ArrayList.length > 0) {
                    takeDownItemConfirm(takeDownUrl, result.data, callback);
                }else {
                    //If no associated/Linked Pages, then just publish this page
                    $.PercBlockUI();
                    _executePublishAction(takeDownUrl, callback, result.data.ArrayList);
                }
            }
        }, null);
    }

    /**
     *
     * @param {*} takeDownUrl
     * @param {*} data
     * @param {*} callback
     */
    function takeDownItemConfirm(takeDownUrl, data, callback)
    {
        var title = I18N.message("perc.ui.publish.title@Remove From Site");
        var options = {
            title: title,
            question: createDialogQuestion(data),
            cancel: function()
            {
            },
            success: function()
            {
                $.PercBlockUI();
                _executePublishAction(takeDownUrl, callback, data);
            }
        };
        $.perc_utils.confirm_dialog(options);
    }

    function createDialogQuestion(data) {
        var dialog = I18N.message("perc.ui.publish.question@Remove From Site") + '<br /><br />';
        $.each(data.ArrayList, function (index, value) {
            if (index > 9) {
                return false;
            }
            dialog += value.pagePath + '<br />';
        });
        return dialog;
    }


    /**
     * Executes a request to the given publish url.
     * @param url the url used to invoke the publish action
     * @param callback (function) callback function to be invoked when ajax call returns
     */
    function _executePublishAction(url, callback, dataObj = null)
    {
        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,[results.data,results.textstatus]);
            }
        };
        if (dataObj === null || dataObj.length <= 0) {
            $.PercServiceUtils.makeJsonRequest(url, $.PercServiceUtils.TYPE_GET, false, serviceCallback);
        } else {
            $.PercServiceUtils.makeJsonRequest(url, $.PercServiceUtils.TYPE_PUT, false, serviceCallback, dataObj);
        }
    }

    /**
     * Executes a request to the given publish url.
     * @param publishUrl the url used to invoke the publish action for an item
     * @param serviceCallback (function) callback function to be invoked when ajax call returns
     */
    function getPublishActions(itemId, callback)
    {
        var publishUrl = $.perc_paths.SITE_ITEM_PUBLISH_ACTIONS + "/" + itemId;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,results.data);
            }
        };
        $.PercServiceUtils.makeRequest(publishUrl, $.PercServiceUtils.TYPE_GET, false, serviceCallback);

    }

    /**
     * Executes a request to get the set Schedule dates for an item.
     * @param getUrl the url used to get the set dates for an item
     * @param serviceCallback (function) callback function to be invoked when ajax call returns
     */
    function getScheduleDates(itemId, callback)
    {
        var getUrl = $.perc_paths.ITEM_GETDATES + "/" + itemId;

        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                callback(false,[results.request,results.textstatus,results.error]);
            }
            else
            {
                callback(true,results.data);
            }
        };
        $.PercServiceUtils.makeRequest(getUrl, $.PercServiceUtils.TYPE_GET, false, serviceCallback);

    }


    /**
     * Executes a request to set the Schedule dates for an item.
     * @param setUrl the url used to save the Schedule dates for an item
     * @param serviceCallback (function) callback function to be invoked when ajax call returns
     */
    function setScheduleDates(sendDates, callback)
    {
        var setUrl = $.perc_paths.ITEM_SETDATES;
        var obj = sendDates;
        var serviceCallback = function(status, results){
            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                callback(false,defaultMsg);
            }
            else
            {
                $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                callback(true,results.data);
            }
        };
        $.PercServiceUtils.makeJsonRequest(setUrl, $.PercServiceUtils.TYPE_POST, false, serviceCallback, obj);

    }

    /**
     * A method to know if the default server is modified and need
     * @param {string} siteName
     * @param {function} 'callback' : Callback function to execute when ajax call returns
     */
    function isDefaultServerModified(siteName, callback)
    {
        var serviceUrl = $.perc_paths.DEFAULT_SERVER_MODIFIED + siteName;
        var serviceCallback = function(status, results)
        {
            if (status === $.PercServiceUtils.STATUS_ERROR)
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                callback(false, defaultMsg);
            }
            else
            {
                callback(true, results.data === "true");
            }
        };
        $.PercServiceUtils.makeRequest(serviceUrl, $.PercServiceUtils.TYPE_GET, false, serviceCallback);
    }

    function publishToStaging(itemId, itemType, callback)
    {
        var publishUrl = itemType === 'Page' ? $.perc_paths.PAGE_PUBLISH:$.perc_paths.RESOURCE_PUBLISH;
        publishUrl+="/staging/" + itemId;

        _executePublishAction(publishUrl, callback);
    }

    function removeFromStaging(itemId, itemType, callback)
    {
        var publishUrl = itemType === 'Page' ? $.perc_paths.PAGE_TAKEDOWN:$.perc_paths.RESOURCE_TAKEDOWN;
        publishUrl+="/staging/" + itemId;

        _executePublishAction(publishUrl, callback);
    }


})(jQuery);

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

(function($){
    $.percCompareServiceInstance = null;
    var params = null;
    $.PercCompareService = function () {
        if($.percCompareServiceInstance == null){
            $.percCompareServiceInstance = PercCompareService();
        }
        return $.percCompareServiceInstance;
    };

    class CompareParams {
        constructor() {
            this.page1 = null;
            this.page2 = null;
            this.comparedPage = null;
            this.revision1 = null;
            this.revision2 = null;
            this.allRevisions = null;
            this.itemId = null;
            this.title = null;
            this.siteId = null;
            this.folderId = null;
            this.itemHref = null;
            this.mobilePreview = null;
            this.compareWindow = null;
            this.assemblerRenderer = false;
            this.templates = null;
            this.selectedTemplate = null;
            this.openNewWindow = true;
            this.refreshFullPage = true;
            this.revisionsPopulated = false;
        }
    }

    function PercCompareService() {

        var params = new CompareParams();

        return {
            openComparisonWindow:openComparisonWindow,
            params:params,
            loadComparePages:loadComparePages
        };


        function getAllTemplates(passedParams){
            var url = "../../../rest/templates/summaries-by-filter";
            var payload = {TemplateFilter: {
                    contentId: passedParams.itemId,
                }};
            $.PercServiceUtils.makeJsonRequest(url,  $.PercServiceUtils.TYPE_POST,false, function callback(status,result){
                if(status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback(false);
                    return;
                }else{
                    var templateList = result.data.TemplateSummaryList;
                    passedParams.templates = templateList;
                    passedParams.selectedTemplate = templateList[0].templateId;
                    getRevisionDetails(passedParams);

                }
            },payload);
        }

        function getRevisionDetails(passedParams){
            $.PercRevisionService.getRevisionDetails(passedParams.itemId,function callback(status,result){
                if(status === $.PercServiceUtils.STATUS_ERROR)
                {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                    callback(false);
                    return;
                }else{
                    convertRevisions(result.data.RevisionsSummary.revisions,passedParams);
                    openComparisonWindow();
                }
            });
        }

        function isComparable(response) {
            const notAllowedCT = ["application/pdf", "image/gif", "application/octet-stream",
                "image/jpeg","image/png","image/svg+xml","audio/mpeg","video/mp4","application/zip",
                "application/x-gzip","application/x-tar"];
            var contentType = response.headers.get("content-type");

            if(notAllowedCT.includes(contentType)){
                return false;
            }else{
                return true;
            }
        }

        function isIteratable(value){
            if(value != null && typeof value[Symbol.iterator] === 'function'){
                return true;
            }
            return false;
        }

        function convertRevisions(revs,passedParams){
            var allRevisions = new Map([]);
            var lastRevision;
            if(isIteratable(revs)){
                for (let item of revs) {
                    allRevisions.set(Number(item.revId), { revId: item.revId, lastModified: item.lastModifiedDate, modifier: item.lastModifier,status : item.status});
                    lastRevision = item.revId;
                }
            }else if (typeof(revs) != 'undefined'){
                allRevisions.set(Number(revs.revId), { revId: revs.revId, lastModified: revs.lastModifiedDate, modifier: revs.lastModifier,status : revs.status});
                lastRevision = revs.revId;
            }
            passedParams.allRevisions = allRevisions;
            passedParams.revision2 = lastRevision;
        }

        function openComparisonWindow(){
            if(params.assemblerRenderer && params.refreshFullPage && params.templates == null){
                getAllTemplates(params);
            }else if(params.assemblerRenderer && !params.refreshFullPage){
                $.percCompareServiceInstance.loadComparePages();
            }else if(!params.assemblerRenderer && params.refreshFullPage && params.allRevisions == null){
                getRevisionDetails(params);
            }else{
                // Retrieve the path for the given page id to build the friendly URL and open hte preview
                $.PercPathService.getPathItemById(params.itemId, function(status, result, errorCode) {
                    if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                        var href = result.PathItem.folderPaths + "/" + result.PathItem.name;
                        var mobilePreview = result.PathItem.mobilePreviewEnabled;
                        if(typeof mobilePreview === "undefined" || mobilePreview === null){
                            mobilePreview = false;
                        }
                        params.title = result.PathItem.name;
                        href = href.substring(1);
                        $.percCompareServiceInstance.params.itemHref = href;
                        $.percCompareServiceInstance.params.mobilePreview = mobilePreview;
                        $.percCompareServiceInstance.loadComparePages();

                    }
                    else {
                        // We failed retrieving the friendly URL. Show the error dialog
                        $.unblockUI();
                        var msg = "";
                        if (errorCode == "cannot.find.item")
                        {
                            msg = I18N.message( 'perc.ui.revisionDialog.failedPageLoad@Failed Page Load' );
                            console.log("Failed to Load Page. Item Id:" + itemId);
                        }
                        else
                        {
                            msg = result;
                        }
                    }
                });
            }
        }

        function createWindow(){
            var url = "/cm/app/compare.jsp?sys_revision1=" + $.percCompareServiceInstance.params.revision1 + "&sys_contentid1=" + $.percCompareServiceInstance.params.itemId;
            if($.percCompareServiceInstance.params.assemblerRenderer){
                url = url +"&sys_siteid=" + $.percCompareServiceInstance.params.siteId + "&sys_folderid=" + $.percCompareServiceInstance.params.folderId;
            }
            compareWindow = window.open(url);
            params.compareWindow = compareWindow;
            compareWindow.onload = function () {
                compareWindow.refreshFullPage($.percCompareServiceInstance.params);
            };
        }

        function refreshCompareWindow(){
            if(params.refreshFullPage){
                if(params.openNewWindow){
                    createWindow();
                }else{
                    params.compareWindow.refreshFullPage(params);
                }
            }else{
                params.compareWindow.refreshRightSide(params);
            }
        }

        function loadComparePages(){
            var href1 = params.itemHref;
            var href2=params.itemHref;
            var mobilePreview = params.mobilePreview;
            if(params.revision1)
            {
                if(params.assemblerRenderer){
                    href1 = "/assembler/render?sys_revision=" + params.revision1 + "&sys_context=0&sys_siteid="+
                        params.siteId+"&sys_contentid="+
                        params.itemId+
                        "&sys_itemfilter=preview&sys_template=" + params.selectedTemplate +
                        "&sys_folderid="+ params.folderId;
                }else{
                    href1 += "?sys_revision=" + params.revision1 + "&percmobilepreview="+params.mobilePreview;
                }
            }

            if(params.revision2)
            {
                if(params.assemblerRenderer){
                    href2 = "/assembler/render?sys_revision=" + params.revision2 +
                        "&sys_context=0&sys_folderid=513&sys_siteid="+
                        params.siteId+"&sys_contentid="+ params.itemId+
                        "&sys_itemfilter=preview&sys_template=" + params.selectedTemplate+
                        "&sys_folderid="+ params.folderId;
                }else{
                    href2 += "?sys_revision=" + params.revision2 + "&percmobilepreview="+ params.mobilePreview;
                }
            }

            fetch(href1)
                .then(function (response) {
                    switch (response.status) {
                        case 200:{
                            if(!isComparable(response)){
                                var contentType = response.headers.get("content-type");
                                // var message = "Content is not comparable. ContentType : " + contentType ;
                                var message =  I18N.message( 'perc.ui.revisionDialog.notComparable@Not compareable' ) + " ContentType: " + contentType;
                                params.page1 = message;
                                params.page2 = message;
                                params.comparedPage = message;
                                refreshCompareWindow(params);
                                return false;
                            }else{
                                return response.text();
                            }
                            break;
                        }
                        case 404:
                            throw response;
                    }
                })
                .then(function (template) {
                    //means page is not comparable, thus no need to load page2
                    if(template == false){
                        return;
                    }
                    params.page1 = template;
                    fetch(href2)
                        .then(function (response) {
                            switch (response.status) {
                                // status "OK"
                                case 200:
                                    return response.text();
                                // status "Not Found"
                                case 404:
                                    throw response;
                            }
                        })
                        .then(function (template) {
                            params.page2 = template;
                            // Diff HTML strings
                            var output = htmldiff(params.page1, params.page2);
                            params.comparedPage = output;
                            refreshCompareWindow();
                        })
                        .catch(function (response) {
                            console.log(response.statusText);
                            throw response;
                        });
                })
                .catch(function (response) {
                    console.log(response.statusText);
                    $.percCompareServiceInstance.comparedPage =  I18N.message("perc.ui.revisionDialog.failedCompare@Failed Compare.")+ " Error: " + response;
                });
        }
    }
})(jQuery);

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
 * "Import progress" dialog. Shows the implementation progress with a progress bar.
 */
(function($)
{
    /**
     * Creates the dialog.
     * @param Object config this object should have the following structure:
     * <pre>
     *    {
     *        showVideo: boolean, true if we want to include the video in the dialog (OPTIONAL)
     *        backgroundRefreshCallback: function() UI callback function to refresh after import
     *        onSuccessCallback: function(importData) callback function to handle the import result
     *        startProgressCallback: function(callbackJobIdHandler) status polling function
     *    }
     * </pre>
     */
    $.PercImportProgressDialog = function PercImportProgressDialog(config)
    {
        var CONSTANTS = {
            IDS: {
                MAIN_DIALOG: 'perc_import_progress_dialog',
                MAIN_DIALOG_CLOSE_BUTTON: 'perc_import_progress_dialog_close',
                MAIN_DIALOG_CLOSE_DISABLED_BUTTON: 'perc_import_progress_dialog_close_disabled',
                MAIN_DIALOG_DONE_BUTTON: 'perc_import_progress_dialog_done',
                MAIN_DIALOG_DONE_DISABLED_BUTTON: 'perc_import_progress_dialog_done_disabled'
            },
            TEXT: {
                MAIN_DIALOG_TITLE: I18N.message("perc.ui.ImportProgressDialog.title@Import"),
                IMPORT_ERROR_MESSAGE: I18N.message('perc.ui.ImportProgressDialog.message@Import failed message'),
                IMPORT_SUCCESS_MESSAGE: I18N.message('perc.ui.ImportProgressDialog.message@Import succeded message')
            },
            OTHER: {
                EVENTS_ENABLE_DONE_BUTTON: 'enableDoneButton',
                POLLING_PERIOD_DURATION: 100,
                IMPORT_LOG_URL: $.perc_paths.VIEW_IMPORT_LOG + '?templateId='
            }
        };

        // Default dialog options
        var dialogButtons = {
            "Done": {
                id: CONSTANTS.IDS.MAIN_DIALOG_DONE_BUTTON,
                click: function(event)
                {
                    // Bind the enabled DONE button to the onSuccess callback function with the
                    // importData returned when the import process finish successfuly
                    config.onSuccessCallback(importData);
                }
            },
            "Done Disabled": {
                id: CONSTANTS.IDS.MAIN_DIALOG_DONE_DISABLED_BUTTON
            },
            "Close Normal": {
                id: CONSTANTS.IDS.MAIN_DIALOG_CLOSE_BUTTON,
                click: function()
                {
                    // If the dialog closes, set to true the UI refresh, to enque the refresh
                    invokeBackgroundRefreshCallback = true;
                    // If the import progress finished, we have to also refrsh in background
                    if (config.backgroundRefreshCallback !== undefined && importFinished === true)
                    {
                        config.backgroundRefreshCallback();
                    }
                    dialog.remove();
                }
            }
        };
        var percDialogOptions = {
            id: CONSTANTS.IDS.MAIN_DIALOG,
            title: CONSTANTS.TEXT.MAIN_DIALOG_TITLE,
            modal: true,
            resizable: false,
            closeOnEscape: true,
            width: 686,
            height: 'auto',
            percButtons: dialogButtons
        };
        // The dialog basic markup
        var dialogMarkup = $('<div/>');
        // Holds the HTML element wrapped with jQuery and the perc_dialog generated
        var dialog;
        // Holds the import result data
        var importJobId;
        // The result data from the import progress
        var importData;
        // Flag needed to ignore unnecesary function calls
        var importFinished = false;
        // Will hold the % of the import progress
        var importPercentage = -1;
        // Will hold the the progress section elements wrapped with jQuery
        var progressSection;
        // Flag that will tell the import progress to perform a refresh in the UI if the import
        // dialog has been closed and an import progress was in progress
        var invokeBackgroundRefreshCallback = false;

        function createImportProgressDialog(config)
        {
            // Initialize the dialog markup and instantiate the perc_dialog plugin
            progressSection = '<div class="progress">';
            progressSection +=     '<p class="progress-message">&nbsp;</p>';
            progressSection +=     '<div class="progress-bar-container">';
            progressSection +=         '<div class="progress-bar"></div>';
            progressSection +=     '</div>';
            progressSection +=     '<div class="error-message">';
            progressSection +=         CONSTANTS.TEXT.IMPORT_ERROR_MESSAGE;
            progressSection +=     '</div>';
            progressSection +=     '<div class="success-message">';
            progressSection +=         CONSTANTS.TEXT.IMPORT_SUCCESS_MESSAGE;
            progressSection +=     '</div>';
            progressSection += '</div>';
            progressSection = $(progressSection);

            dialogMarkup.append($('<div>').append(progressSection));

            if (config.showVideo === true)
            {
                dialogMarkup.append(
                $('<div align="center" style="margin-top:24px; overflow:hidden">')
                    .append(
                        $('<img id="perc_import_image" src="../images/images/ImportVideoNotFound.png" width="560px" height="0px">')
                    )
                );
            }

             dialogMarkup.append(
                $('<div>')
                    .append('<p class="hint">Ask questions. Get answers. Visit the <a target="_blank" rel = "noopener noreferrer" href="https://percussioncmscommunity.intsof.com" title="Percussion Community">Percussion Community</a> to access Video Tutorials, Forums, and more.</p>')
            );

            if (config.showVideo === true)
            {
                //Used a random dummy parameter to avoid cache
                dialogMarkup.append(
                    $('<img height="0px" width="0px" src="https://percussioncmshelp.intsof.com/Assets/Help/header/images/PercussionSwoosh.png?dummy=' + Math.random() + '">')
                        .on("error", handleUnreachableURL)
                        .on("load", showVideoIframe)
                );
            }

            function handleUnreachableURL()
            {
                $("#" + CONSTANTS.IDS.MAIN_DIALOG).find("#perc_import_image").attr("height", "315px");
            }

            function showVideoIframe()
            {
                $("#" + CONSTANTS.IDS.MAIN_DIALOG).find("#perc_import_video").attr("height", "315px");
            }

            dialog = $(dialogMarkup).perc_dialog(percDialogOptions);

            configureDialogUI();
            startImportProgress();
        }

        /**
         * Setup of the dialog UI general configuration and events.
         */
        function configureDialogUI()
        {
            // Fix the disabled DONE button right margin is incorrect
            var doneButtonDisabled = $('#' + dialogButtons['Done Disabled'].id);

            // By default the DONE button is disabled (the enabled version is hidden)
            var doneButton = $('#' + dialogButtons.Done.id);
            doneButton.hide();
            // Set a custom event for enabling/disabling the DONE button
            $('#' + CONSTANTS.IDS.MAIN_DIALOG).on(
                CONSTANTS.OTHER.EVENTS_ENABLE_DONE_BUTTON,
                function(event, flag)
                {
                    enableDoneButton(flag);
                }
            );
        }

        /**
         * Enables or disables the DONE button according a flag parameter.
         * @param boolean flag If true, will enable the button (otherwise disable it)
         */
        function enableDoneButton(flag)
        {
            var doneButton = $('#' + dialogButtons.Done.id),
                doneButtonDisabled = $('#' + dialogButtons['Done Disabled'].id);
            if (flag === true)
            {
                doneButton.show();
                doneButtonDisabled.hide();
            }
            else
            {
                doneButton.hide();
                doneButtonDisabled.show();
            }
        }

        /**
         * Checks the status of the import job.
         * TODO:
         * - the whole process should have a timeout near 10mins
         */
        function startImportProgress()
        {
            config.startProgressCallback(function(status, jobId)
            {
                // If something went wrong during the job creation, show the error status status.
                // Else, invoke the polling of the job status
                if (status !== $.PercServiceUtils.STATUS_SUCCESS)
                {
                    setErrorState();
                }
                else
                {
                    importJobId = jobId;
                    pollStatus();
                }
            });
        }

        /**
         * Checks the status of the import job.
         */
        function pollStatus()
        {
            // We could have one or more pollStatus() invokations pending and, previosly got the
            // completed status. This check prevent from making an unnecesary request if the
            // process finished
            if (importFinished === true)
            {
                return;
            }

            config.pollingProgressCallback(importJobId, function(status, asyncJobStatus) {
                if (status !== $.PercServiceUtils.STATUS_SUCCESS || asyncJobStatus.status < 0)
                {
                    setErrorState();
                }
                else
                {
                    var progressMessage = progressSection.find('.progress-message');
                    var progressBar = progressSection.find('.progress-bar');

                    // Update the progress info only when the job has done some % of advance
                    if (asyncJobStatus.status > importPercentage)
                    {
                        // If there is an import status message, make it look good by making the
                        // first letter of the sentence uppercase and appending "..."
                        if (asyncJobStatus.message !== undefined &&
                            asyncJobStatus.message.length > 0)
                        {
                            asyncJobStatus.message = asyncJobStatus.message.charAt(0).toUpperCase() +
                                asyncJobStatus.message.slice(1) + '...';
                            progressMessage.html(asyncJobStatus.message);
                        }

                        // Update progress representation and store the % of progress
                        progressBar.css('width', asyncJobStatus.status + '%');
                        importPercentage = asyncJobStatus.status;
                    }

                    if (asyncJobStatus.status === 100)
                    {
                        // The job reached its end successfully, set the corresponding flag to true
                        importFinished = true;

                        // If the dialog has been closed before the import finishes, invoke the
                        // background refresh callback
                        if (config.backgroundRefreshCallback !== undefined &&
                            invokeBackgroundRefreshCallback === true)
                        {
                            config.backgroundRefreshCallback();
                            return;
                        }

                        // We put a delay of 1,5 seg after the 100% is reached, so the user can see
                        // it before showing the success state of the dialog
                        setSuccessState();
                        return;
                    }

                    // Reached this point, we are in the middle of the progress, call the
                    // pollStatus function again
                    setTimeout(pollStatus, CONSTANTS.OTHER.POLLING_PERIOD_DURATION);
                }
            });
        }

        /**
         * Makes the progress message and progress bar hidden, so the layout stays umodified.
         */
        function hideProgressMessageAndBar()
        {
            progressSection.find('.progress-message').css('visibility', 'hidden');
            progressSection.find('.progress-bar-container').css('visibility', 'hidden');
        }

        /**
         * Shows the error state tha the dialog shows when something went wrong during the whole
         * process.
         */
        function setErrorState()
        {
            hideProgressMessageAndBar();
            progressSection.find('.error-message').show();
        }

        /**
         * Sets the dialog appearance when the import finished successfuly
         */
        function setSuccessState()
        {
            // We have to retrieve the import result before proceeding
            config.importResultCallback(importJobId, function(status, importResult)
            {
                importData = importResult;

                // Set the corresponding link for the import log
                setTemplateIdForLink(importData);

                hideProgressMessageAndBar();
                progressSection.find('.success-message').show();

                // Enable the DONE button
                enableDoneButton(true);
            });
        }

        /**
         * Retrieves the id of the template needed in the import log link
         */
        function setTemplateIdForLink()
        {
            /**
             * TODO: There is a little delay after completing the href attrib for the import log
             * link when using Chrome as the web browser. Don't know if it can be fixed via JS
             */
            function modifyLink(templateId)
            {
                var importLogLink = progressSection.find('a[title="Download the import log here"]');
                importLogLink.attr('href', CONSTANTS.OTHER.IMPORT_LOG_URL + templateId);
            }

            // We cheat a little here, we are not supposed to know importResult structure
            if (importData.id !== undefined)
            {
                modifyLink(importData.id);
            }
            else
            {
                // We don't have the ID for the template, so we have to make an extra service call
                $.PercSiteService.getTemplates(importData.Site.name, function(status, templatesData)
                {
                    modifyLink(templatesData.TemplateSummary[0].id);
                });
            }
        }

        // //////////////////////////////////////////////////////////
        // PercImportProgressDialog function execution starts from here
        // //////////////////////////////////////////////////////////
        createImportProgressDialog(config);
    };
})(jQuery);

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
 * Site creation wizard.
 */
var $perc_newSiteDialogLayout;

(function($)
{
    var dialog;
    $.perc_createNewSiteDialog = function(cbh)
    {
        var v = null;
        dialog = $("<div></div>").perc_wizard(
            {
                steps: 2,
                modal: true,
                resizable: false,
                templateUrl: "/Rhythmyx/cm/app/dialogs/perc_newSiteDialog.jsp",
                title: I18N.message("perc.ui.newsitedialog.title@Create Site"),
                height: 'auto',
                width: 725,
                open: function()
                {
                    var siteNameField = $("#sitename"),
                        templateNameField = $("#templatename"),
                        urlField = $("#url"),
                        defaultSiteOption = $('input#type_url'),
						queryParamaterField = $("#queryParamaterField");
						queryParamaterFieldRequired = $("#queryParamaterFieldRequired");



                    // This check is performed because the dialog is never removed from the DOM. If the
                    // dialog doesn't exist, initialize it. Else clear its fields.
                    if (!$perc_newSiteDialogLayout)
                    {
                        // Set the "exist dialog" flag
                        $perc_newSiteDialogLayout = true;

                        // Apply filters to fields
                        $.perc_filterField(siteNameField, $.perc_textFilters.HOSTNAME);
                        $.perc_filterField(templateNameField, $.perc_textFilters.URL);

                        // If the URL checkbox is unselected, disable the sitename field
                        $('input[type=radio]').on("change",function()
                        {
                            // If the selected radio button is not the one for the
                            // URL option, make the URL field read only
                            var radioSelected = $(this);
                            if (radioSelected.attr('id') !== "type_url")
                            {
                                urlField
                                    .attr('readonly', 'readonly')
                                    .attr('disabled', 'true');
								queryParamaterField
									.attr('readonly', 'readonly')
                                    .attr('disabled', 'true');
								$('#queryParamaterFieldRequired').prop('checked', false);
								queryParamaterFieldRequired
									.attr('readonly', 'readonly')
                                    .attr('disabled', 'true');
                                clearValidationErrorMessage(urlField);
                            }
                            else
                            {
                                urlField
                                    .removeAttr('readonly')
                                    .removeAttr('disabled');
								queryParamaterFieldRequired
									.removeAttr('readonly')
                                    .removeAttr('disabled');
							}
                        });
						$('#queryParamaterFieldRequired').click(function() {
							if(this.checked){
								queryParamaterField
									.removeAttr('readonly')
                                    .removeAttr('disabled');
							}else{
								$('#queryParamaterField').val("");
								queryParamaterField
									.attr('readonly', 'readonly')
                                    .attr('disabled', 'true');
							}

						});



                        // Create a themplate selector using images: initialize the element
                        var $tempList = $('#perc_templateList').perc_imageselect(
                            {
                                hardSelect: false,
                                onSelect: function(val, url)
                                {
                                    $('#perc_selectedTemplate img').remove();
                                    $('#perc_selectedTemplate').html("<img alt='' src='" + url + "'/>" + "<br/><span>" + val.split(".")[2] + "</span>");
                                    $('#selectedtemplate').val(val);
                                }
                            });
                        // We have to get the images for the perc_imageselect plugin
                        _loadTemplateList();
                        $("#perc-select-template-type").on("change",function(){
                            if($(this).val() === "base"){
                                $("#perc-base-template-lib").show();
                                $("#perc-resp-template-lib").hide();
                            }
                            else{
                                $("#perc-base-template-lib").hide();
                                $("#perc-resp-template-lib").show();
                            }
                        });
                        // Initialize validation for form
                        v = $("#perc_newSiteDialogForm").validate(
                            {
                                errorClass: "perc_field_error",
                                validClass: "perc_field_success",
                                wrapper: "p",
                                validateHiddenFields: false,
                                debug: false,
                                rules: _getValidationRules(),
                                messages: _getValidationMessages(),
                                showErrors: function() {
                                    if (this.pendingRequest < 1)
                                    {
                                        this.defaultShowErrors();
                                        var formDialog = $("#perc_newSiteDialogForm");
                                        formDialog.find('.perc_dialog_field.perc_field_success')
                                            .prevAll(".perc_dialog_label").removeClass("perc_dialog_label_error");
                                        formDialog.find('.perc_dialog_field.perc_field_error')
                                            .prevAll(".perc_dialog_label").addClass("perc_dialog_label_error");
                                        $(this.currentElements).removeClass(this.settings.errorClass);
                                    }
                                }
                            });

                    }
                    else
                    {
                        _clearFieldValues();
                    }

                    /////////////////////////////////////////
                    // Common code after the dialog is opened
                    /////////////////////////////////////////

                    // Clear & reset fields validation errors
                    urlField.prop('readonly', false).prop('disabled', false);
                    clearValidationErrorMessage(siteNameField);
                    clearValidationErrorMessage(urlField);

                    // Initialize the radio button groupt ot its default value (URL)
                    defaultSiteOption.attr('checked', true);

                },
                onOk: function()
                {
                    _onOK(cbh);
                },
                onNext: _onNext,
                // We specify a validation function/method that calls the corresponding method from the
                // validate library
                onValidate: function()
                {
                    return v.form();
                }
            });
        return dialog;
    };

    /**
     * Override default next behavior, because we have to check in the first step the URL
     * option has been selected. If the user selected the URL radio in the first step, call the
     * corresponding ervice to create a site based on the given URL
     * @param function callbackHandlerOnCreate function invoked on successful site creation
     */
    function _onNext(callbackHandlerOnCreate)
    {
        var sitenameDropdown = dialog.find('select[name=sitename-select]');
        if (sitenameDropdown.length > 0){
            dialog.find('input[name=sitename]').val(sitenameDropdown.val());
        }
        var siteNameField = $("#sitename"),
            urlField = $("#url"),
			queryParamaterField = $("#queryParamaterField"),

            // Will hold the information needed to redirect to the Design manager
            memento = {
                templateName: undefined,
                templateId: undefined,
                pageId: undefined
            },

            // Wil hold the new site path (needed to invoke services)
            newSitePath,
            continueToNextStep = true;

        /**
         * Shows an error dialog. Will be set as the error callback (behavior) for each of the
         * service calls.
         * @param data String Message that the error dialog will show.
         */
        function errorCallbackFallback(data)
        {
            $.unblockUI();
            $.perc_utils.alert_dialog(
                {
                    title: I18N.message("perc.ui.publish.title@Error"),
                    content: data
                });
        }

        /**
         * Callback for site import/creation service;
         * @param status String PercServiceUtils
         * @param siteData Object data return by the PercSiteService.createSiteFromUrl service
         */
        function siteImportCallback(status, siteData)
        {
            if (status !== $.PercServiceUtils.STATUS_SUCCESS)
            {
                errorCallbackFallback(siteData);
                return;
            }

            // No unblockUI is needed because we are redirecting to a different page, unless we
            // have an error in any service call

            // Betgin setting the memento file used to redirect to the Design view
            memento.templateName = siteData.Site.templateName;
            memento.tabId = "perc-tab-layout";
            newSitePath = $.perc_paths.SITES_ROOT + '/' + siteData.Site.name;

            // We need to call 2 services to get the pageId and templateId
            $.PercSiteService.getTemplates(siteData.Site.name, getTemplateIdCallback);
        }

        /**
         * Callback for site import/creation service;
         * @param status String PercServiceUtils
         * @param siteData Object data return by the PercSiteService.getTemplates service
         */
        function getTemplateIdCallback(status, teamplatesData)
        {
            if (status !== $.PercServiceUtils.STATUS_SUCCESS)
            {
                errorCallbackFallback(teamplatesData);
                return;
            }

            memento.templateId = teamplatesData.TemplateSummary[0].id;

            $.PercPathService.getFolderPathItem(newSitePath, getPageIdCallback);
        }

        /**
         * Callback for site import/creation service;
         * @param status String PercServiceUtils
         * @param siteData Object data return by the PercPathService.getFolderPathItem service
         */
        function getPageIdCallback(status, foldersData)
        {
            if (status !== $.PercServiceUtils.STATUS_SUCCESS)
            {
                errorCallbackFallback(foldersData);
                return;
            }

            var pathItem;

            for (var i = 0; i < foldersData.PathItem.length; i++) {
                if (foldersData.PathItem[i].category === 'LANDING_PAGE')
                {
                    pathItem = foldersData.PathItem[i];
                    i = foldersData.PathItem.length;
                }
            }

            memento.pageId = pathItem.id;

            // Finally we have all the things we want in the memento, retrieve the path (URL param)
            // and invoke the navigation manager
            var querystring = $.deparam.querystring();
            $.PercNavigationManager.goToLocation(
                $.PercNavigationManager.VIEW_DESIGN,
                newSite.name,
                null,
                null,
                null,
                pathItem.path,
                null,
                memento
            );
        }

        /////////////////////////////////////////
        // _onNext function execution begins here
        /////////////////////////////////////////
        // We use :checked inside a filter to fix an IE compatibility issue
        if (dialog.find('input[type=radio]').filter(':checked').attr('id') === "type_url")
        {
            var newSite = {
                name: siteNameField.val().trim(),
                baseUrl: urlField.val().trim(),
				queryParamater: queryParamaterField.val().trim()
            };

            // If the URL entered lacks 'http(s)://' prefix, append 'http://'
            if(! (/^(https?):\/\//i).test(newSite.baseUrl)) {
                newSite.baseUrl = 'https://' + newSite.baseUrl;
            }

			var importConfig = {
				mapQueryParamToPageName: queryParamaterField.val().trim(),
				site: newSite
			};

            // We don't have to go to the next step, an error dialog could appear
            continueToNextStep = false;

            // Close the dialog to simulate wizard
            $(".ui-dialog-titlebar .ui-icon-closethick").trigger("click");

            // Open the Import Progress dialog
            $.PercImportProgressDialog({
                showVideo: false,
                backgroundRefreshCallback: function()
                {
                    // When the import progress dialog closes and there is an import in progress,
                    // refresh the finder whenever the process finishes
                    $.perc_finderInstance.refresh();
                },
                onSuccessCallback: function(importData)
                {
                    // The new template data will be available in importData, use it to redirect
                    // to the template editor
                    siteImportCallback('success', importData);
                },
                startProgressCallback: function(callbackJobIdHandler)
                {
                    $.PercSiteService.createSiteFromUrlAsync(importConfig, function(status, jobId) {
                        // callbackJobIdHandler is specified by the Import Progress dialog
                        callbackJobIdHandler(status, jobId);
                    });
                },
                pollingProgressCallback: $.PercSiteService.createSiteFromUrlStatus,
                importResultCallback: $.PercSiteService.createSiteFromUrlResult
            });
        }

        return continueToNextStep;
    }

    // Invoked before submitting the form
    function _onOK(cbh)
    {
        var fields = _getFieldValues();
        fields.selectedtemplate = $("#perc-select-template-type").val()==="base"?fields.perc_selected_basetemplate:fields.perc_selected_resptemplate;
        if (fields.selectedtemplate.length === 0)
        {
            $.perc_utils.alert_dialog(
                {
                    title: I18N.message("perc.ui.publish.title@Error"),
                    content: I18N.message("perc.ui.new.site.dialog@Select Template")
                });
            return;
        }

        if (fields.templatename === null || fields.templatename.trim() === '')
        {
            $.perc_utils.alert_dialog(
                {
                    title: I18N.message("perc.ui.publish.title@Error"),
                    content: I18N.message("perc.ui.new.site.dialog@Template Name")
                });
            return;
        }

        // Set the  basic information we are going to send to the site creation service
        var fielddata = {
            Site: {
                name: fields.sitename,
                label: fields.sitename,
                description: fields.description,
                homePageTitle: I18N.message("perc.ui.new.site.dialog@Home Page"),
                navigationTitle: I18N.message("perc.ui.new.site.dialog@Home Page"),
                baseTemplateName: fields.selectedtemplate,
                templateName: fields.templatename.trim()
            }
        };

        $.PercBlockUI();
        // Force the dialog close while blocking the UI
        $(".ui-dialog-titlebar .ui-icon-closethick").trigger("click");

        $.ajax(
            {
                url: $.perc_paths.SITE_CREATE + "/",
                dataType: "json",
                contentType: "application/json",
                type: "POST",
                data: JSON.stringify(fielddata),
                success: function(data, textstatus)
                {
                    // Redirect to architecture tab for the new site
                    // $.perc_redirect($.perc_paths.URL_ARCHITECTURE, {site: fields.sitename});

                    // Invoke the callback handler with the sitename as a parameter and unblock UI
                    cbh(fields.sitename);
                    $.unblockUI();
                },
                error: function(request, textstatus, error)
                {
                    // If something went wrong unblock UI and show an error dialog
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(request);
                    $.unblockUI();
                    $.perc_utils.alert_dialog(
                        {
                            title: I18N.message("perc.ui.publish.title@Error"),
                            //TODO: Find in Java code for I18N
                            content: defaultMsg
                        });
                }
            });
    }

    // Clear/reset each field in the dialog. It has to be applied because the dialog is
    // never removed from the DOM.
    // Invoked during dialog open/initialization.
    function _clearFieldValues()
    {
        $("#perc_newSiteDialog .perc_dialog_field").each(function()
        {
            $(this).val('');
        });
        $('#perc_selectedTemplate').html('');
        $(".perc_imageselect_selected").removeClass("perc_imageselect_selected");
    }

    function _getFieldValues()
    {
        var results = {};
        $("#perc_newSiteDialog .perc_dialog_field").each(function()
        {
            results[this.id] = $(this).val();
        });
        return results;
    }

    function _loadTemplateList()
    {
        var baseTemplates = $("#perc-base-template-lib").PercScrollingTemplateBrowser({isBase:true,width:590, baseType: "base", hiddenFieldId:"perc_selected_basetemplate"});
        var respTemplates  = $("#perc-resp-template-lib").PercScrollingTemplateBrowser({isBase:true,width:590, baseType: "resp", hiddenFieldId:"perc_selected_resptemplate"});
    }
    /**
     * Retrieves the existing sites from the response object and compares the value against
     * each one of those (case-insensitive).
     * @param validator - unused
     * @param element - unused
     * @param value {String} - the name of the site to check, must be a non-emtpy string.
     * @param response {} - object containing an array of existing site summaries
     * @return true if the name does not conflict w/ any existing name, false otherwise
     */
    function _validateUniqueSiteNameHandler(validator, element, value, response)
    {
        //we need to add an assertion 'framework' and check them here
        for (i = 0; i < response.SiteSummary.length; i++)
        {
            if ((response.SiteSummary[i].name + "").toLowerCase() === value.toLowerCase())
            {
                return false;
            }
        }
        return true;
    }

    function _getValidationRules()
    {
        var rules = {
            sitename: {
                required: true,
                perc_remote: {
                    url: $.perc_paths.SITES_ALL,
                    contentType: "application/json",
                    type: "GET",
                    dataType: "json",
                    handler: _validateUniqueSiteNameHandler
                }
            },
            url : {
                // URL field is required only in the (radio) URL type was selected
                required: 'input[type=radio]#type_url:checked',
                maxlength: 2000,
                noBinary : "noBinary"
            },
            description: {
                maxlength: 255
            },
            selectedtemplate: {
                required: true
            }
        };
        return rules;
    }

    function _getValidationMessages()
    {
        var messages = {
            sitename: {
                required: I18N.message("perc.ui.new.site.dialog@Site Name Req"),
                perc_remote: I18N.message("perc.ui.new.site.dialog@Unique Name Req")
            },
            url: {
                required: I18N.message("perc.ui.new.site.dialog@URL Req"),
                noBinary: I18N.message("perc.ui.new.site.dialog@URL Format Req")
            },
            templatename: {
                required: I18N.message("perc.ui.new.site.dialog@Template Req"),
                perc_remote: I18N.message("perc.ui.new.site.dialog@Template Unique Req")
            },
            selectedtemplate: {
                required: I18N.message("perc.ui.new.site.dialog@Template Selected Req")
            }
        };
        return messages;
    }

    /**
     * Removes the validation message of the field, if it has one.
     * @param field Object HTML field element wrapped with jQuery.
     */
    function clearValidationErrorMessage(field)
    {
        field.prevAll(".perc_dialog_label").removeClass("perc_dialog_label_error");
        var errorMsg = field.next();
        if (errorMsg.find('.perc_field_error').length > 0)
        {
            field.removeClass('perc_field_error');
            errorMsg.remove();
        }
    }
})(jQuery);

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

(function($)
{
    $.perc_iframe_fix = function( frame )
    {
        //special scope for droppables which are inside the iframe, so that
        //they do not have spurious interactions with draggables outside the
        //iframe
        $.perc_iframe_scope = 'perc-iframe-scope';
        $.dragDelay = 10;
        var dragging = false;



        //Create an invisible div to put over the iframe
        var overlay = $("<div class=\"perc-iframe-overlay-dnd-container\"/>");
        $('body').append( overlay );

        overlay
            .height( frame.height() )
            .width( frame.width() )
            .addClass('ui-layout-ignore')
            .css(
                {
                    overflow: 'hidden',
                    position: 'absolute',
                    left: '-10000px',
                    top: '0px',
                    zIndex: 1000
                });

        addDragSupportDroppable();

        //Move the div over the iframe
        function addOverlay()
        {
            overlay.css({ left: frame.position().left, top: frame.position().top  });
            overlay.height( frame.height() );
            overlay.width( frame.width() );
        }

        //Move the div back offscreen
        function removeOverlay()
        {
            overlay.css({ left: '-10000px', top: '0px' });
        }

        //Add droppable targets to the overlay div - this allows draggables outside the iframe to communicate with
        //droppables inside the iframe
        function liftDroppables( )
        {
            var droppables = frame.contents().find( ':data(ui-droppable)' );

            droppables.each( function()
            {
                var orig = $(this);
                var orig_drop = $.data( this, 'ui-droppable' );
                orig_drop.options.disabled=true;
                var clone = $("<div/>").addClass("allDroppablesHelpers").addClass("perc-iframe-dnd-overlay-droppable").attr("for", orig.attr("id")).width( orig.outerWidth() ).height( orig.outerHeight() );
                overlay.append( clone );
                var iframeLeft, iframeTop;
                var fr = frame;
                if( $.browser.msie )
                {
                    //Of *course* the scroll offsets would be in frame.contentWindow.document.documentElement - where else???
                    var contentWindow = fr[0].contentWindow;
                    var documentElement = contentWindow.document.documentElement;
                    iframeLeft = documentElement.scrollLeft;
                    iframeTop = documentElement.scrollTop;
                }
                else
                {
                    //Oh, you crazy other browsers, what a pathetically obvious place to put your scroll offsets!
                    iframeLeft = frame[0].contentWindow.scrollX;
                    iframeTop = frame[0].contentWindow.scrollY;
                }
                var left = $(this).offset().left - iframeLeft;
                var top = $(this).offset().top - iframeTop;
                if( $.browser.mozilla || $.browser.safari )
                {
                    //Fix offsets for scrolled window
                    left -= window.scrollX;
                    top -= window.scrollY;
                }
                clone.css( { position: 'absolute', left: left + "px", top: top + "px" } );

                //Make the clone droppable, with event functions which
                //call through to the original droppable's events
                clone.droppable(
                    {
                        greedy: orig_drop.options.greedy,
                        tolerance: orig_drop.options.tolerance,
                        accept: orig_drop.options.accept,
                        iframeFix: true,
                        scope: orig_drop.options.scope,
                        over: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._over.call(orig_drop, [evt,ui]);
                        },
                        activate: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._activate.call(orig_drop, [evt,ui]);
                        },
                        deactivate: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._deactivate.call(orig_drop, [evt,ui]);
                        },
                        out: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._out.call(orig_drop, [evt,ui]);
                        },
                        drop: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._drop.call( orig_drop,[evt,ui]);
                        }
                    });
            });
        }

        //Get rid of the added droppables.
        function removeDroppables()
        {
            overlay.empty();
            var droppables = frame.contents().find( ':data(ui-droppable)' );

            droppables.each( function() {
                var orig = $(this);
                var orig_drop = $.data(this, 'ui-droppable');
                orig_drop.options.disabled = false;
            });
        }

        function onDragStart()
        {
            if( !dragging )
            {
                dragging = true;
                addOverlay();
                liftDroppables();
            }
        }

        function onDragStop()
        {
            removeOverlay();
            removeDroppables();
            dragging = false;
        }

        function addDragSupportDroppable()
        {
            var d = $("<div/>")
                .addClass('ui-layout-ignore')
                .droppable({
                    addClasses: false,
                    scope: $.perc_iframe_scope,
                    tolerance : 'pointer',
                    iframeFix: true,
                    activate: function(event,ui)
                    {
                        onDragStart(ui.draggable);
                    },
                    deactivate: function(event,ui)
                    {
                        setTimeout( onDragStop, 100 );
                    }
                });
            //.css({'position':'absolute', 'left':-1000});
            $('body').append(d);
        }

        //If a draggable needs to drag onto the iframe, it must call
        //startDrag() when dragging starts, end stopDrag() when dragging
        //stops

    };
})(jQuery);

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
 * define the pagemanager functions, to interface with services on the server side.
 */


(function($){

    /**
     * Use the supplied metadata parameters to create a new page.
     */
    function create_new_page( path, params, k, err ) {

        if (!$.perc_fakes.page_service.create_page) {
            $.perc_pathmanager.get_folder_path(path, function(fp){
                createPage(fp, params, k, err);
            }, err);
        }
        else {
            if (false)
                err(I18N.message("perc.ui.page.manager@Create New Page Error"));
            else
                k('16777215-101-733');
        }
    }
    function createPage(folderPath, params, k, err){
        var myObj = {};
        $.each(params, function(){
            myObj[this.name] = this.value;
        });
        var createPath = null;
        var passIn = null;
        var addToRecent = myObj.addToRecent ?true:false;
        if (myObj.landingpage) {
            createPath = $.perc_paths.SECTION_CREATE;
            pass_in = {
                'CreateSiteSection': {
                    'pageName': myObj.page_name,
                    'pageTitle': myObj.page_title,
                    'templateId': myObj.template,
                    'pageUrlIdentifier': myObj.page_name,
                    'pageLinkTitle': myObj.page_linktext,
                    'folderPath': folderPath,
                    'addToRecent':true
                }
            };
        }
        else {
            createPath = $.perc_paths.PAGE_CREATE;
            pass_in = {
                'Page': {
                    'name': myObj.page_name,
                    'title': myObj.page_title,
                    'templateId': myObj.template,
                    'linkTitle': myObj.page_linktext,
                    'folderPath': folderPath,
                    'addToRecent':true
                }
            };
        }
        $.ajax({
            dataType: 'json',
            data: JSON.stringify(pass_in),
            contentType: 'application/json',
            type: 'POST',
            url: createPath,
            success: k,
            error: err
        });
    }
    function render_page( id, k, err ) {
        $.ajax( {
            url: $.perc_paths.PAGE_EDIT + "/" + id,
            type: 'GET',
            success: k,
            error: err,
            dataType: 'text' });
    }

    function delete_page( id, callback, errorCallback )
    {
        $.ajax(
            {
                url: $.perc_paths.PAGE_DELETE + "/" + id,
                type: 'DELETE',
                success: function() {
                    callback();
                },
                error: errorCallback
            });
    }
    /*
    @param id {String} - name of the site to be deleted
    @param callback {function()	{}} - defines action to be performed on success (when return status = 200)
    @param errorCallback {function( data, textStatus, errorThrown)	{}} - defines action to be performed on failure (when return status != 200)
    */
    function delete_site( id, callback, errorCallback )
    {
        $.ajax(
            {
                url: $.perc_paths.SITE_DELETE + "/" + id,
                type: 'DELETE',
                success: callback,
                error: errorCallback
            });
    }


    function new_asset(path, folder_spec, callback, errorCallback)
    {
        if( !$.perc_fakes.page_service.new_asset )
        {
            errorCallback( I18N.message("perc.ui.page.manager@New Asset Not Implemented") );
        }
        else
        {
            //Call callback with asset id
            callback('54321');
        }
    }

    function get_widget_ctypes( page_id, is_page, callback, errorCallback )
    {
        // if we are not using mocked data,
        // get the asset widget drop criteria from the REST service
        if( !$.perc_fakes.page_service.get_widget_ctypes )
        {
            function parseAssetDropCriteria( json )

            {
                var assetDropCriteria = {};
                $.each( json, function()
                {
                    assetDropCriteria[ this.widgetId ] = this.supportedCtypes;
                });
                callback( assetDropCriteria );
            }

            $.ajax(
                {
                    url: $.perc_paths.ASSET_WIDGET_DROP_CRITERIA + page_id + "/" + is_page,
                    type: 'GET',
                    dataType: 'json',
                    success: parseAssetDropCriteria,
                    error: errorCallback
                });
        }

        // otherwise use mocked data
        else
        {
            var testContentTypes = { '12345' : ['percPage', 'article'], '4567': [] };
            callback( testContentTypes );
        }
    }

    function save_page(pageId, pageObject, callback){
        $.ajax( {
            url: $.perc_paths.PAGE_CREATE + "/",
            type: 'POST',
            contentType: 'application/xml',
            data: pageObject,
            dataType: "xml",
            processData: false,
            success: callback,
            error: function(data, textstatus, error){
                $.unblockUI();
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: $.PercServiceUtils.extractDefaultErrorMessage(data)});
            }
        });
    }

    function load_page(pageId, callback) {
        // show an hour glass cursor when page is still loading
        $.ajax({
            url:      $.perc_paths.PAGE_CREATE + "/" + pageId,
            type:     'GET',
            dataType: 'text',
            accepts: {
                text: "application/xml"
            },
            success:  callback,
            error:    function(data, textstatus, error) {
                // remove the hour glass if there was an error loading the page
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: $.PercServiceUtils.extractDefaultErrorMessage(data)});
            }
        });
    }

    /**
     * Renders an individual region of a page. Called by PercPageModel.render()
     * @argument regionId is the id of the outermost region to be rendered. Result HTML includes the HTML
     * of all enclosing regions within regionId
     * @argument pageObject is an instance of PercPageModel which contains the current state of the page
     * including all new widgets, regions, etc. The state is maintained in the client so that we can cancel
     * all changes before persisting.
     */
    function render_region(regionId, pageObject, callback ) {

        $.ajax({
            url          : $.perc_paths.PAGE_PREVIEW + regionId,
            type         : "POST",
            contentType  : "application/xml",
            dataType     : "xml",
            data         : pageObject,
            processData  : false,
            success      : callback,
            error        : function(data, textstatus, error) {

                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: $.PercServiceUtils.extractDefaultErrorMessage(data)});
            }
        });
    }

    $.perc_pagemanager = {
        save_page : save_page,
        render_region : render_region,
        load_page : load_page,
        create_new_page : create_new_page,
        render_page: render_page,
        new_asset : new_asset,
        delete_page : delete_page,
        delete_site : delete_site,
        get_widget_ctypes : get_widget_ctypes,
        createPage : createPage
    };

})(jQuery);

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
 * Define the pathmanager functions.
 */
(function ($) {

    var ut = $.perc_utils;

    /**
     * If 'path' is a leaf, run if_leaf on its summary JSON; if it is a
     * folder, run if_folder; if any call fails, run if_error.
     *
     * This version does some pre-processing on the returned contents.
     */
    function is_leaf(path, if_leaf, if_folder, if_error) {

        _is_leaf(path, function (spec) { if_leaf(spec.pathItem); }, prepare_folder_items(if_folder), if_error);

        function prepare_folder_items(callback) {
            return function (pathitems) {
                var specs = $.map(pathitems.PathItem, function (s) {
                    var paths = s.path.split('/');
                    if (paths[paths.length - 1] === "") {
                        paths.pop();
                    }
                    s.path = $.map(paths, function (p) { return p; });
                    s.path_component = paths[paths.length - 1];
                    return s;
                });
                specs = $.grep(specs, function (s) { return s.type !== 'percNavTree' && s.name != '.system'; });
                callback(specs);
            };
        }
    }

    function _is_leaf(path, if_leaf, if_folder, if_error) {
        if (path.length <= 2) {
            open_path(path, true, if_folder, if_error);
        }
        else {
            var pathclone = $.map(path, function (x) { return x; });
            var path_end = pathclone.pop();
            function check_results(specs) {
                console.log("check_results received:", specs);

                // Handle different response formats from the API
                var pathItems;
                if (specs && specs.PathItemList) {
                    // New format: {PathItemList: [...]}
                    pathItems = specs.PathItemList;
                } else if (specs && specs.PathItem) {
                    // Old format: {PathItem: {...}} or {PathItem: [...]}
                    pathItems = Array.isArray(specs.PathItem) ? specs.PathItem : [specs.PathItem];
                } else {
                    console.error("Missing PathItem or PathItemList in response:", specs);
                    if (typeof if_error === 'function') {
                        if_error(I18N.message("perc.ui.path.manager@Item Not Found")); //I18N
                    }
                    return;
                }

                var it = $.grep(pathItems, function (p) {
                    var path_components = p.path.split('/');
                    var e = path_components.pop();
                    if (e == "")
                        e = path_components.pop();
                    return e == path_end;
                });
                if (it.length == 0) {
                    if_error(I18N.message("perc.ui.path.manager@Item Not Found")); //I18N
                }
                else {
                    if (it[0].leaf == true) {
                        open_path(path, false, if_leaf, if_error);
                    }
                    else {
                        open_path(path, true, if_folder, if_error);
                    }
                }
            }

            open_path(pathclone, true, check_results, if_error);
        }
    }


    function get_site_id(path, k, err) {
        //XXX get this from id.
        k(path[1]);
    }

    function get_folder_path(path, k, err) {
        open_path(path, false, function (pathItem) {
            k(pathItem.PathItem.folderPath);
        }, err);
    }


    /**
     * Decide if the path is a leaf; if it is, open its parent folder; otherwise, open the folder itself.
     */
    function open_containing_folder(path, k, err) {
        var pathclone = $.map(path, function (x) { return x; });
        _is_leaf(path,
            function () {
                pathclone.pop();
                open_path(pathclone, true, function (spec) { k(spec, pathclone); }, err);
            },
            function (spec) {
                k(spec, pathclone);
            },
            err);
    }

    function add_folder(path, k, err) {
        var path_str = path.join('/') + '/';
        var parent = $.perc_utils.acop(path);
        var nm = parent.pop();
        open_path(parent, true, function (folder_spec) {
            var matches = $.grep(folder_spec.PathItem, function (fs) {
                return fs.name === nm;
            });
            if (matches.length > 0) {
                err(I18N.message("perc.ui.saveasdialog.error@Duplicate folder"));
            }
            else {
                path_str = $.perc_utils.encodeURL(path_str);
                $.ajax({
                    type: 'GET',
                    success: k,
                    error: err,
                    url: $.perc_paths.PATH_ADD_FOLDER + path_str,
                    dataType: 'json'
                });
            }
        }, err);

    }

    /**
     * Open path, sending the response directly to callback; if the call fails
     * call err. folder determines if it is opened as a folder or an item.
     * Added the last param to retrieve paged results
     */
    function open_path(path, folder, callback, err, paging) {
        var path_str;
        var serviceUrl;
        //Check if we need paged results and change the service URL.
        if (paging) {
            path_str = path;
            serviceUrl = $.perc_paths.PATH_PAGINATED_FOLDER + path_str;
        }
        else {
            path_str = $.perc_utils.encodeURL(path.join("/") + "/");
            serviceUrl = $.perc_paths.PATH_FOLDER + path_str;
        }

        if (!$.perc_fakes.path_service) {
            var maxRetries = 6;
            var retryDelay = 300;
            var retryCount = 0;

            function doOpen() {
                if (folder) {
                    $.ajax({
                        type: 'GET',
                        success: callback,
                        error: function(xhr, status, error) {
                            // Retry on server errors (500) AND path-not-found (404) since folder may not be indexed yet
                            if ((xhr.status === 500 || xhr.status === 404) && retryCount < maxRetries) {
                                retryCount++;
                                console.log("open_path retry " + retryCount + " of " + maxRetries + " for path: " + path_str);
                                setTimeout(doOpen, retryDelay);
                            } else {
                                err(xhr, status, error);
                            }
                        },
                        url: serviceUrl,
                        dataType: 'json',
                        cache: false
                    });
                }
                else {
                    $.ajax({
                        type: 'GET',
                        success: callback,
                        error: function(xhr, status, error) {
                            // Retry on server errors (500) AND path-not-found (404) since folder may not be indexed yet
                            if ((xhr.status === 500 || xhr.status === 404) && retryCount < maxRetries) {
                                retryCount++;
                                console.log("open_path retry " + retryCount + " of " + maxRetries + " for path: " + path_str);
                                setTimeout(doOpen, retryDelay);
                            } else {
                                err(xhr, status, error);
                            }
                        },
                        url: $.perc_paths.PATH_ITEM + path_str,
                        dataType: 'json',
                        cache: false
                    });
                }
            }

            doOpen();
        }
        else {
            var fakes = {
                '/': { "PathItem": [{ "name": "Sites", "leaf": "false", "path": "\/Sites\/" }] },
                '/Sites/': {
                    "PathItem": [{ "name": "Test", "leaf": "false", "path": "\/Sites\/Test\/" },
                    { "name": "TestTwo", "leaf": "false", "path": "\/Sites\/TestTwo\/" }]
                },
                '/Sites/Test/': { "PathItem": [{ "name": "Test", "leaf": "true", "path": "\/Sites\/Test\/Test\/" }] },
                '/Sites/Test/Test/': { "PathItem": { "name": "Test", "leaf": "true", "path": "\/Sites\/Test\/Test\/" } },
                '/Sites/TestTwo/': { "PathItem": [{ "name": "TestTwo", "leaf": "true", "path": "\/Sites\/TestTwo\/TestTwo\/" }] },
                '/Sites/TestTwo/TestTwo/': { "PathItem": { "name": "TestTwo", "leaf": "true", "path": "\/Sites\/TestTwo\/TestTwo\/" } }
            };

            callback(fakes[path_str]);
        }
    }

    /**
     * Makes an AJAX call to the server and gets the item properties and calls the supplied call back function with this object.
     * {"ItemProperties":{"name":"Home Page","status":"Live", "lastAccessedBy":"Some User", "lastAccessedDate": "2010/01/22:10:30AM"}}
     * @param path The full path of the item for which the properties are needed.
     * @param callback The callback function that gets called with true and ItemProperties object if succeeds, otherwise falls.
     * Shows the error message in case of error.
     */
    function getItemProperties(path, callback) {
        /** Testing code
         var itemProps = {"ItemProperties":{"name":"Home Page", "status":"Live", "lastAccessedBy":"Some User", "lastAccessedDate": "2010/01/22:10:30AM"}};
         callback(true, itemProps);
         */
        var successCallback = function (data) {
            callback(true, data.ItemProperties);
        };
        var errorCallback = function (request, status, error) {
            callback(false, $.PercServiceUtils.extractDefaultErrorMessage(request));
        };
        var path_str = $.perc_utils.encodeURL(path);

        $.ajax({
            type: 'GET',
            success: successCallback,
            error: errorCallback,
            url: $.perc_paths.PATH_ITEM_PROPERTIES + path_str,
            dataType: 'json',
            cache: false
        });
    }

    $.perc_pathmanager = {
        'is_leaf': is_leaf,
        'open_path': open_path,
        'get_folder_path': get_folder_path,
        'get_site_id': get_site_id,
        'open_containing_folder': open_containing_folder,
        'add_folder': add_folder,
        'getItemProperties': getItemProperties
    };

})(jQuery);

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
 * Handles the new folder action.
 */
(function ($) {
    $.perc_build_new_folder_button = function (finder, contentViewer) {
        var ut = $.perc_utils;
        var finder_path = ["", $.perc_paths.SITES_ROOT_NO_SLASH];
        var pitem = {};
        var btn = $("<a id='perc-finder-new-folder' class='perc-font-icon ui-disabled' href='#' title='"+I18N.message("perc.ui.new.folder.button@Click New Folder") + "'><span class='icon-plus fas fa-plus'></span><span class='icon-folder-close fas fa-folder'></span></a>")
            .perc_button().on("click",function (evt) {
                createNewFolder(evt);
            });


        /**
         * Makes an ajax request to create the new folder. Passes the finder path.
         */
        function createNewFolder(evt){
            //Check user access
            $.PercFolderHelper().getAccessLevelByPath(finder_path.join('/'),false,function(status, result){
                if(status === $.PercFolderHelper().PERMISSION_ERROR || result === $.PercFolderHelper().PERMISSION_READ)
                {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.page.general@Warning"), content: I18N.message("perc.ui.new.folder.button@Permissions to Create Folder")});
                    return;
                }
                else
                {
                    $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
                    $.PercPathService.createNewFolder(finder_path.join('/'),
                        function(status, result){
                            if(status === $.PercServiceUtils.STATUS_SUCCESS)
                            {
                                pitem = result;
                                finder.refresh(function() {
                                    var expanded = $(".perc-finder").css("visibility") === "visible";
                                    if(expanded){
                                        ut.makeFolderEditable(pitem.PathItem);
                                    }
                                });
                                $.unblockUI();
                            }
                            else
                            {
                                $.unblockUI();
                                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result});
                            }
                        });
                }
            });
        }

        /**
         * Path changed listener function. Updates the finder_path variable with the current path.
         */
        function pathChangedListener(path) {
            finder_path = path;
            btn.show();
            enableButton(true);

            // If current view is Search then keep the button disabled (since no path to create is defined in Finder)
            if ($.Percussion.getCurrentFinderView() == $.Percussion.PERC_FINDER_SEARCH_RESULTS || $.Percussion.getCurrentFinderView() == $.Percussion.PERC_FINDER_RESULT)
            {
                enableButton(false);
            }
            else if (path[1] == $.perc_paths.RECYCLING_ROOT_NO_SLASH) {
                btn.hide();
            }
            else if(path[1] == $.perc_paths.DESIGN_ROOT_NO_SLASH && path.length < 4)
            {
                enableButton(false);
            }
            else if(path.length == 2 && path[1] == $.perc_paths.SITES_ROOT_NO_SLASH)
            {
                enableButton(false);
            }
            else
            {
                $.PercFolderHelper().getAccessLevelByPath(path.join('/'),true,function(status, result){
                    if(status == $.PercFolderHelper().PERMISSION_ERROR || result == $.PercFolderHelper().PERMISSION_READ)
                    {
                        enableButton(false);
                    }
                });
            }
        }

        /**
         * Helper function to enable or disable the new folder button on finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            if(flag){
                $( ".perc-finder-menu #perc-finder-new-folder" ).removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        createNewFolder(evt);
                    } );
            }
            else{
                $( ".perc-finder-menu #perc-finder-new-folder" ).addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
        }

        finder.addPathChangedListener(pathChangedListener);
        return btn;
    };
})(jQuery);



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

(function($){

//Add custom validation method for the URL name.
//$.validator.addMethod( 'url_name',
//        function(x) { return x.match( /^[a-zA-Z0-9\-]*$/ ); },
//       I18N.message( "perc.ui.newpagedialog.error@Url name validation error" ));

    $.perc_build_new_page_button = function(finderRef, contentViewer)
    {
        var finderPath;


        var newPageButton = $('<a id="mcol-new-page" class="perc-font-icon" href="#" title="' +I18N.message("perc.ui.new.page.button@Click New Page") + '"class="ui-disabled"><span class="icon-plus fas fa-plus"></span><span class="icon-file fas fa-file"></span></a>').perc_button();

        /**
         * Listener function that is added to the finder listeners, this method gets called whenever there is a path change
         * happens on the finder.
         * @param path, an array of the path entries. For a page Page1 under Foo site Bar folder will be
         * ["Sites","Foo","Bar","Page1"].
         * Based on the supplied path decides whether the button needs to be enabled or disabled.
         * Enables only when the root node is a site or site folder and user has at least write access to the folder.
         */
        function newPageButtonListener(path) {
            finderPath = path;

            // If current view is Search then keep the button disabled (since no path to create is defined in Finder)
            if ($.Percussion.getCurrentFinderView() == $.Percussion.PERC_FINDER_SEARCH_RESULTS || $.Percussion.getCurrentFinderView() == $.Percussion.PERC_FINDER_RESULT)
            {
                enableButton(false);
            }
            else if(path[1] == $.perc_paths.SITES_ROOT_NO_SLASH)
            {
                newPageButton.show();
                enableButton(true);
                if(path.length < 3)
                {
                    enableButton(false);
                }
                else
                {
                    $.PercFolderHelper().getAccessLevelByPath(path.join('/'),true,function(status, result){
                        if(status == $.PercFolderHelper().PERMISSION_ERROR || result == $.PercFolderHelper().PERMISSION_READ)
                        {
                            enableButton(false);
                        }
                    });
                }
            }
            else
            {
                if(path[1] == $.perc_paths.DESIGN_ROOT_NO_SLASH || path[1] == $.perc_paths.ASSETS_ROOT_NO_SLASH || path[1] == $.perc_paths.RECYCLING_ROOT_NO_SLASH )
                {
                    newPageButton.hide();
                }
            }

        }

        /**
         * Helper function to enable or disable the new page button on finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            if(flag){
                newPageButton.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        checkAndOpenNewPageDialog(evt);
                    } );
            }
            else{
                newPageButton.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
        }


        /**
         * Checks whether the current user has permission to create a new page, if yes, then calls the contentViewer
         */
        function checkAndOpenNewPageDialog(evt)
        {
            var currentItem = finderRef.getCurrentItem();
            var folderPath = "";
            if (currentItem != null){
                if (typeof(currentItem.folderPaths) === 'object') {
                    folderPath = currentItem.folderPaths[0];
                } else {
                    folderPath = currentItem.folderPaths;
                }
                //if the current item is a Folder select the current path.
                if (currentItem.type == "Folder"){
                    folderPath = currentItem.folderPath;
                }
            }
            //Check user access
            $.PercUserService.getAccessLevel("percPage", -1, function(status, result){
                if(status == $.PercServiceUtils.STATUS_ERROR || result == $.PercUserService.ACCESS_READ || result == $.PercUserService.ACCESS_NONE)
                {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.new.page.button@New Page"), content: I18N.message("perc.ui.new.page.button@New Page Authorization")});
                    return;
                }
                else if(contentViewer)
                {
                    //contentViewer.confirm_if_dirty( $.PercNewPageDialog().openDialog(finderPath.join('/')));
                    contentViewer.confirm_if_dirty( function () {$.PercNewPageDialog().openDialog(finderPath.join('/'));});
                }
                else
                {
                    open_new_page_dialog($.PercNewPageDialog().openDialog(finderPath.join('/')),null);
                }
            }, folderPath);
        }

        finderRef.addPathChangedListener( newPageButtonListener );
        return newPageButton;
    };

})(jQuery);

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
 * Handles the copy page action. 
 */
(function ($) {
    var pageName;
    $.perc_build_copy_page_button = function (finderRef, content) {
        var btn = $("<a id='perc-finder-copy-page' href='#' title='Click to copy selected page'>Copy</a>")
        	.on("click",function (event) {
        		copyPageValidate(event);
        });        
      
        function copyPageValidate(evt){
            $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
            var selectedPage = $(".mcol-opened.perc-listing-type-percPage.perc-listing-category-PAGE");
            var selectedPage1 = $(".mcol-opened.perc-listing-type-percPage.perc-listing-category-LANDING_PAGE");
            var selectedItemList = $("#perc-finder-listview .perc-datatable-row-highlighted");
            
            if (selectedPage[0] === undefined && selectedPage1[0] === undefined && selectedItemList.length() === 0)
                 return;
            
            pageName = selectedPage.text();
            if(!pageName || pageName === "")
                pageName = selectedPage1.text();
            
            var id;
            if (selectedItemList.length > 0)
            {
                listSelectedRowData = selectedItemList.data("percRowData");
                if (listSelectedRowData.category === "LANDING_PAGE" || listSelectedRowData.category === "PAGE")
                {
                    id = listSelectedRowData.id;
                    pageName = listSelectedRowData.name;
                }
            }
            else if (selectedPage[0] !== undefined)
            {
                id = selectedPage.attr("id");
            }
            else
            {
                id = selectedPage1.attr("id");
            }

            var currentItem = finderRef.getCurrentItem();
            var itemId = "";
            if (currentItem != null){
                itemId = currentItem.id;
            }
            
            $.PercUserService.getAccessLevel("percPage", itemId,function(status, result){
                if(status === $.PercServiceUtils.STATUS_ERROR || result === $.PercUserService.ACCESS_READ || result === $.PercUserService.ACCESS_NONE)
                {
                   $.perc_utils.alert_dialog({title: I18N.message("perc.ui.copy.page.button@Copy Page"), content: I18N.message("perc.ui.copy.page.button@Copy Page Authorization") + pageName + ".'"});
                   $.unblockUI();
                   return;
                }
                else
                {
                   id = id.replace("perc-finder-listing-", "");
                   copyPage(id);
                }
            });
        }
        
        function copyPage(pageId){
            var currentPath = finderRef.getCurrentPath();
            var pathItem = finderRef.getPathItemById(pageId);
            $.PercPageService.copyPage(pageId, function(status, result){
                if(status=="error") {
                     showErrorMessage(result);
                     finderRef.refresh();
                } else {
                    finderRef.refresh(function(){
                        var pathItems = result.data.split("/");
                        var pageName = pathItems[pathItems.length-1];
                        currentPath[currentPath.length-1] = pageName;
                        finderRef.open(currentPath);
                    });
                }
                $.unblockUI();
            });
        }
        function showErrorMessage(message) {
            message = message.replace("PAGE_NAME", pageName);
            $.perc_utils.alert_dialog({id: 'perc-finder-copy-page-error', title: I18N.message("perc.ui.publish.title@Error"), content: message});
        }
    
        /**
         * Logic in charge of enabling/disablig the link, according to a given path
         * @param String path Path of the folder that contains the page
         */
        function update_copy_btn(path) {
            var last_page = path[path.length - 1];
            if(path[1] == $.perc_paths.SITES_ROOT_NO_SLASH)
            {
                if(path.length < 4)
                {
                    enableButtonCopy(false);
                }
                else
                {
                	var selectedPage = $(".mcol-opened.perc-listing-type-percPage.perc-listing-category-PAGE.perc_last_selected");
                    var selectedPage1 = $(".mcol-opened.perc-listing-type-percPage.perc-listing-category-LANDING_PAGE.perc_last_selected");
                    var selectedFolderId = $(".mcol-listing.perc-listing-type-Folder.perc-listing-category-FOLDER.ui-draggable.perc_last_selected.ui-droppable[title='" + last_page + "']").eq(0).attr("id");
                    var selectedSectionFolderId = $(".mcol-listing.perc-listing-type-Folder.perc-listing-category-SECTION_FOLDER.perc_last_selected.ui-droppable[title='" + last_page + "']").eq(0).attr("id");
                    
                    var selectedItemList = $("#perc-finder-listview .perc-datatable-row-highlighted");
                    
                    if (selectedItemList.length > 0)
                    {
                        listSelectedRowData = selectedItemList.data("percRowData");

                        // First we must check if the containingFolder is writable
                        isFolderWritable(path, function(isWritable) {
                            if (isWritable && (listSelectedRowData.category == "LANDING_PAGE" || listSelectedRowData.category == "PAGE"))
                            {
                                enableButtonCopy(true);                 
                            }
                            else
                            {
                                enableButtonCopy(false);
                            }
                        });
                        return;
                    }
                    else if (selectedPage[0] != undefined || selectedPage1[0] != undefined)
                    {
                        // First we must check if the containingFolder is writable
                        isFolderWritable(path, function(isWritable) {
                            // This if is to correct an error of the finder refresh regarding this enablement.
                            // When selecting a page and going back to it's folder due to addPathChangedListener executing order, the enablement is not well performed
                            // If the target is a folder then turn the button to false
                            if (!isWritable || selectedFolderId != undefined || selectedSectionFolderId != undefined)
                            {
                                enableButtonCopy(false);
                            }
                            else
                            {
                                enableButtonCopy(true);
                            }                        
                        });
                    }
                    else
                    {
                        enableButtonCopy(false);
                    }
                }
            }
            else
            {
                enableButtonCopy(false);
            }
        }
        
        /**
         * Helper function that ask if the containing folder of the file to be copied is writable.
         * @param String path Path of the folder that contains the page
         * @param function callback Function that will get evaluated, with a boolean param
         */
        function isFolderWritable(path, callback)
        {
            var folderPath = path.slice(0, path.length - 1).join('/');
            $.PercFolderHelper().getAccessLevelByPath(folderPath, false, function(status, result) {
                // If error requesting folder properties show a dialog, else continue with the logic
                if (status == $.PercFolderHelper().PERMISSION_ERROR)
                {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result});
                    return;
                }
                else
                {
                    // We evaluate the callback with the result of the comparission of permission
                    //debugger;
                    callback( (result !== $.PercFolderHelper().PERMISSION_READ));
                    return;
                }
            });
        }
        
        /**
         * Helper function to enable or disable the new folder button on finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButtonCopy(flag)
        {
            if (flag)
            {
                btn.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        copyPageValidate(evt);
                    } );
            }
            else
            {
                btn.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
            // We trigger a custom event using jQuery, the actions button will act acordingly
            btn.trigger('actions-change-enabled-state');
        }
        
        finderRef.addPathChangedListener( update_copy_btn );
        return btn;
    };
})(jQuery);


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
 * Handles the restoration of a deleted item/folder from the recycle bin.
 */
(function ($) {
    var itemName;
    $.perc_build_restore_button = function (finderRef, content) {
        var btn = $("<a id='perc-finder-restore-item' href='#' title='Click to restore the selected item'>Restore Item</a>")
            .on("click",function (event) {
                restorePageValidate(event);
            });

        function restorePageValidate(evt) {
            $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
            var selectedItem = $('.mcol-opened.perc_last_selected');
            var selectedItemList = $("#perc-finder-listview .perc-datatable-row-highlighted");

            if (!(isItem() || isFolder()) && selectedItemList.length === 0)
                return;
            //Don't allow user restore landing page of Navigation.Let them use folder to restore.
            if(isLandingPage()){
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.restore.title@Restore"),content:I18N.message("perc.ui.restore.messsage@Restore Not Allowed")});
                $.unblockUI();
                return;
            }

            itemName = selectedItem.text();

            // TODO: make sure ID is set here under all use cases

            var currentItem = finderRef.getCurrentItem();
            var itemId = "";
            if (currentItem != null) {
                itemId = currentItem.id;
            }

            $.PercUserService.getAccessLevel("percPage", itemId, function (status, result) {
                if (status == $.PercServiceUtils.STATUS_ERROR || result == $.PercUserService.ACCESS_READ || result == $.PercUserService.ACCESS_NONE) {
                    $.perc_utils.alert_dialog({ title: I18N.message("perc.ui.copy.page.button@Copy Page"), content: I18N.message("perc.ui.copy.page.button@Copy Page Authorization") + itemName + ".'" });
                    $.unblockUI();
                    return;
                }
                else {
                    restoreSelection(itemId);
                }
            });
        }

        function restoreSelection(id) {
            var path = '';

            if (isFolder()) {
                path = $.perc_paths.PATH_RESTORE_FOLDER;
            } else if (isPage()) {
                path = $.perc_paths.PAGE_RESTORE;
            } else if (isAsset()) {
                path = $.perc_paths.ASSET_RESTORE;
            } else {
                console.warn('The seleted item for restore is not an asset, page, or folder.', id);
                return;
            }

            $.PercRecycleService.restoreItem(id, path, function(status, data) {
                if (status === $.PercServiceUtils.STATUS_ERROR) {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: data});
                } else {
                    finderRef.refresh();
                }
            });

            $.unblockUI();
        }

        function showErrorMessage(message) {
            message = message.replace("PAGE_NAME", itemName);
            $.perc_utils.alert_dialog({ id: 'perc-finder-copy-page-error', title: I18N.message("perc.ui.publish.title@Error"), content: message });
        }

        /**
         * Logic in charge of enabling/disablig the link, according to a given path
         * @param String path Path of the folder that contains the page
         */
        function update_restore_btn(path) {
            if (path[1] == $.perc_paths.RECYCLING_ROOT_NO_SLASH) {
                if (path.length < 4) {
                    enableButtonRestore(false);
                }
                else {
                    var selectedItemList = $("#perc-finder-listview .perc-datatable-row-highlighted");
                    // This is present to select items under List mode in CM1 UI.
                    if (selectedItemList.length > 0) {
                        listSelectedRowData = selectedItemList.data("percRowData");

                        // First we must check if the containingFolder is writable
                        isFolderWritable(path, function (isWritable) {
                            if (isWritable && (isRestorableCategory(listSelectedRowData))) {
                                enableButtonRestore(true);
                            }
                            else {
                                enableButtonRestore(false);
                            }
                        });
                        return;
                    }
                    else if (isItem()) {
                        // we have a page selected for restore
                        // First we must check if the containingFolder is writable
                        isFolderWritable(path, function (isWritable) {
                            // This if is to correct an error of the finder refresh regarding this enablement.
                            // When selecting a page and going back to it's folder due to addPathChangedListener executing order, the enablement is not well performed
                            // If the target is a folder then turn the button to false
                            if (!isWritable) {
                                enableButtonRestore(false);
                            }
                            else {
                                enableButtonRestore(true);
                            }
                        });
                    } else if(isFolder()) {
                        // we have a folder selected for restore
                        enableButtonRestore(true);
                    }
                    else {
                        enableButtonRestore(false);
                    }
                }
            }
            else {
                enableButtonRestore(false);
            }
        }

        /**
         * Checks if the currently selected element in Finder list view is valid for restore.
         * @param {*} listSelectedRowData the currently selected html element in the Finder List VIew.
         */
        function isRestorableCategory(listSelectedRowData) {
            return listSelectedRowData.category === 'LANDING_PAGE' ||
                listSelectedRowData.category === 'PAGE' ||
                listSelectedRowData.category === 'SECTION_FOLDER' ||
                listSelectedRowData.category === 'FOLDER' ||
                listSelectedRowData.category === 'ASSET';
        }

        /**
         * Checks if the currently selected item is an item (asset or page or w/e). :)
         */
        function isItem() {
            return $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-ASSET') ||
                $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-PAGE') ||
                $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-LANDING_PAGE');
        }

        /**
         * Checks if the currently selected item is a page :)
         */
        function isPage() {
            return $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-PAGE') ||
                $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-LANDING_PAGE');
        }

        /**
         * Checks if the currently selected item is a page :)
         */
        function isLandingPage() {
            return $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-LANDING_PAGE');
        }


        /**
         * Checks if the currently selected item is an asset :)
         *
         */
        function isAsset() {
            return $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-ASSET');
        }

        /**
         * Checks if the currently selected item is a folder.
         */
        function isFolder() {
            return $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-FOLDER') ||
                $('.mcol-opened.perc_last_selected').hasClass('perc-listing-category-SECTION_FOLDER');
        }

        /**
         * Helper function that ask if the containing folder of the file to be copied is writable.
         * @param String path Path of the folder that contains the page
         * @param function callback Function that will get evaluated, with a boolean param
         */
        function isFolderWritable(path, callback) {
            var folderPath = path.slice(0, path.length - 1).join('/');
            $.PercFolderHelper().getAccessLevelByPath(folderPath, false, function (status, result) {
                // If error requesting folder properties show a dialog, else continue with the logic
                if (status == $.PercFolderHelper().PERMISSION_ERROR) {
                    $.perc_utils.alert_dialog({ title: I18N.message("perc.ui.publish.title@Error"), content: result });
                    return;
                }
                else {
                    // We evaluate the callback with the result of the comparission of permission
                    //debugger;
                    callback((result !== $.PercFolderHelper().PERMISSION_READ));
                    return;
                }
            });
        }

        /**
         * Helper function to enable or disable the new folder button on finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButtonRestore(flag) {
            if (flag) {
                btn.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        restorePageValidate(evt);
                    });
            }
            else {
                btn.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
            // We trigger a custom event using jQuery, the actions button will act acordingly
            btn.trigger('actions-change-enabled-state');
        }

        finderRef.addPathChangedListener(update_restore_btn);
        return btn;
    };
})(jQuery);

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
 * Download button 
 */
(function ($)
{
    /**
     * Constructs the download button.
     *
     * @param finder
     * @param contentViewer
     */
    $.perc_build_download_button = function (finder, contentViewer)
    {
        //TODO: I18N With correct Formatting on btn below.
    	var btn = $('<a id="perc-finder-download" href="#" title="Click to download the selected file">Download File</a>');

        /**
         * Listener function that is added to the finder listeners, this method gets called whenever a path change
         * happens on the finder.
         * @param path, an array of the path entries. For a page Page1 under Foo site Bar folder will be
         * ["Sites","Foo","Bar","Page1"].
         * Based on the supplied path decides whether the button needs to be enabled or disabled.
         */
        function downloadButtonListener(path)
        {
            // Enable de button only on 
            //     1. "Design" view. We figure out this by checking the Path
            //     2. Add to the previous condition(s): file should be under path "/Design/Web Resources/themes"
            //        {0:"", 1:"Design", 2:"Web Resources", 3:"themes", 4:"THEMENAME", 5:"THEMEFILE"}
            //     3. The element selected is a file (not a folder)
            if(path[1] === $.perc_paths.DESIGN_ROOT_NO_SLASH && path.length > 4)
            {
                // Get the selected item from Column or List mode with the class FSFile
            	var selectedItemSpec = $("#perc-finder-listview .perc-datatable-row-highlighted").data("percRowData");
                if (typeof selectedItemSpec === 'undefined')
                {
                	 selectedItemSpec = $(".mcol-listing.perc-listing-type-FSFile.mcol-opened.perc_last_selected").data("spec");
                }
                // Now check the 3rd condition, that the element selected is a file under Design
                if (typeof selectedItemSpec !== 'undefined' &&
                    selectedItemSpec.type === 'FSFile' &&
                    selectedItemSpec.leaf)
                {
                    updateButtonUrl(true);
                    enableButton(true);
                    return;
                }
            }
            // Any other option disables the button
            updateButtonUrl(false);
            enableButton(false);
        }

        /**
         * Helper function to enable or disable the button in the finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            if (flag)
            {
                btn.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt) {
                        launchDownload(evt);
                    });
            }
            else
            {
                btn.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
            btn.trigger('actions-change-enabled-state');
        }

        /**
         * Launches the download functionality specific to the browser on the selected item.
         */
        function launchDownload(evt)
        {
        }
        
        /**
         * Updates the href attribute of the button according to the path and the corresponding
         * server side URL to download the file
         */
        function updateButtonUrl(flag)
        {
            var downloadUrl;
            if (flag)
            {
                downloadUrl = $.perc_paths.WEBRESOURCESMGT + '/' + finder.getCurrentPath().slice(3).join("/");
            }
            else
            {
                downloadUrl = "#";
            }
            btn.attr('href', downloadUrl);
        }
        
        // Finally, return the button element
        finder.addPathChangedListener( downloadButtonListener );
        return btn;
    };

})(jQuery);

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
 * Upload button 
 */
(function ($)
{
    /**
     * Constructs the upload button.
     *
     * @param finder
     * @param contentViewer
     */
    $.perc_build_upload_button = function (finder, contentViewer)
    {
        var btn = $('<a id="perc-finder-upload" href="#" title="' +I18N.message("perc.ui.upload.button@Click Upload File") + '">Upload File...</a>')
            .on("click",function(evt){
                lauchClickHandler(evt);
            });
        
        /**
         * Listener function that is added to the finder listeners, this method gets called whenever a path change
         * happens on the finder.
         * @param path, an array of the path entries. For a page Page1 under Foo site Bar folder will be
         * ["Sites","Foo","Bar","Page1"].
         * Based on the supplied path decides whether the button needs to be enabled or disabled.
         */
        function uploadButtonChangePathListener(path)
        {
            if(path.length > 3 && $.perc_paths.DESIGN_THEMES === path[0] + '/' + path[1] + '/' + path[2] + '/' + path[3])
            {
                enableButton(true);
                return;
            }
            // Any other option disables the button
            enableButton(false);
        }

        /**
         * Helper function to enable or disable the button in the finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            if (flag)
            {
                btn.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        lauchClickHandler(evt);
                    } );
            }
            else
            {
                btn.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
            $(document).trigger('actions-change-enabled-state');
        }

        /**
         * Launches the download functionality specific to the browser on the selected item.
         */
        function lauchClickHandler(evt)
        {
            // Open the dialog and pass it the current finder path
            $.perc_upload_theme_file_dialog.open(finder);
        }

        
        // Finally, return the button element
        finder.addPathChangedListener( uploadButtonChangePathListener );
        return btn;
    };
})(jQuery);

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
 * Opens the Folder properties dialog, by creating.
 */
(function ($)
{
    /**
     * Constructs the menu entry.
     *
     * @param finder
     * @param contentViewer
     */
    $.perc_build_folderproperties_button = function (finder, contentViewer)
    {
        var btn = $('<a id="perc-finder-folderproperties" href="#" title="Folder properties">Folder properties</a>');
        var selectedItem;
        var pathItemSpec;

        /**
         * Listener function that is added to the finder listeners, this method gets called whenever a path change
         * happens on the finder.
         * @param path, an array of the path entries. For a page Page1 under Foo site Bar folder will be
         * ["Sites","Foo","Bar","Page1"].
         * Based on the supplied path decides whether the button needs to be enabled or disabled.
         */
        function finderPathChangedListener(path)
        {
            // Disable the menu entry under under //Design
            if (path.length > 1 && path[1] == $.perc_paths.DESIGN_ROOT_NO_SLASH) {
                enableButton(false);
                pathItemSpec = undefined;
                return;
            }

            if (path[1] == $.perc_paths.RECYCLING_ROOT_NO_SLASH) {
                enableButton(false);
                pathItemSpec = undefined;
                return;
            }

            // Reached this point, enable the button only on Folder selection
            selectedItem = $("#perc-finder-listview .perc-datatable-row-highlighted");
            if (selectedItem.length > 0) {
                // Element selected in list mode: if the selectedItem is not a Folder, make
                // selectedItem undefined, so the menu entry will not be enabled
                pathItemSpec = selectedItem.data("percRowData");
                if (pathItemSpec.type !== 'Folder' && pathItemSpec.type !== 'FSFolder') {
                    selectedItem = undefined;
                }
            }
            else {
                // If the jQuery selected collection is empty, it means that we are in column mode
                // In column mode is difficult to select the selected folder with jQuery, so we
                // will have to use the current path in the finder
                var folderSelector;
                folderSelector = '.mcol-listing.perc-listing-type-FSFolder.mcol-opened';
                folderSelector += ', .mcol-listing.perc-listing-type-Folder.mcol-opened';
                var highlightedElems = $(folderSelector);

                var i;
                for (i = 0; i < highlightedElems.length; i++) {
                    var self = $(highlightedElems[i]);
                    var elemPath;
                    if(typeof self.data('spec') !== 'undefined'){
                        elemPath = self.data('spec').path;
                    }
                    //var elemPath = self.data('spec').path;
                    // Now make the path comparisson
                    if (elemPath == path.join('/') + '/') {
                        selectedItem = self;
                        pathItemSpec = self.data('spec');
                    }
                }
            }

            // Enable the menu entry if the corresponding selectedItem is not undefined
            if (selectedItem != undefined && selectedItem.length > 0) {
                enableButton(true);
            }
            else {
                enableButton(false);
                pathItemSpec = undefined;
            }
        }

        /**
         * Helper function to enable or disable the button in the finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            if (flag) {
                btn.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        clickHandler(evt);
                    } );
            }
            else {
                btn.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
            btn.trigger('actions-change-enabled-state');
        }

        /**
         * Handler function invoked when clicking the Folder properties option in the actions menu.
         */
        function clickHandler(evt)
        {
            // We must perform some checks before opening the dialog
            if(pathItemSpec.category === "SYSTEM") {
                $.perc_utils.alert_dialog({
                    title: I18N.message("perc.ui.page.general@Warning"),
                    content: I18N.message("perc.ui.folder.properties.button@Path Nonvalid String")
                });
                return;
            }
            else if(pathItemSpec.accessLevel !== $.PercFolderHelper().PERMISSION_ADMIN) {
                var type = pathItemSpec.category === "SECTION_FOLDER" ? "section" : "folder";
                $.perc_utils.alert_dialog({
                    title: I18N.message("perc.ui.page.general@Warning"),
                    content: I18N.message("perc.ui.folder.properties.button@Permissions Error") + type + "."
                });
                return;
            }
            else if(pathItemSpec.category === "SECTION_FOLDER") {
                $.perc_utils.alert_dialog({
                    title: I18N.message("perc.ui.page.general@Warning"),
                    content: I18N.message("perc.ui.folder.properties.button@Use Navigation Editor")
                });
                return;
            }

            // Reached this point, we can open the dialog safely
            $.PercFolderPropertiesDialog().open(pathItemSpec, function(newName,status) {
                if (status === undefined) {
                    if (newName !== "" && newName !== pathItemSpec.name)
                    {
                        var newPath = pathItemSpec.folderPaths + "/" + newName;
                        newPath = newPath.substring(1);
                        newPath = newPath.replace('/Folders/$System$', '');
                        $.perc_finder().open(newPath.split("/"));
                    }
                    else
                    {
                        $.perc_finder().refresh();
                    }
                }
            });
        }

        /**
         * Updates the href attribute of the button according to the path and the corresponding
         * server side URL to download the file
         */
        function updateButtonUrl(flag)
        {
            var downloadUrl;
            if (flag) {
                downloadUrl = $.perc_paths.WEBRESOURCESMGT + '/' + finder.getCurrentPath().slice(3).join("/");
            }
            else {
                downloadUrl = "#";
            }
            btn.attr('href', downloadUrl);
        }

        // Finally, return the button element
        finder.addPathChangedListener( finderPathChangedListener );
        return btn;
    };

})(jQuery);

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
 * Actions split button
 */
(function ($)
{
    /**
     * Constructs the actions button.
     *
     * @param finder
     * @param contentViewer
     */
    $.perc_build_actions_button = function (finder, contentViewer)
    {
        // Create the action elements
        var cp = $.perc_build_copy_page_button( finder, contentViewer );
        var fp = $.perc_build_folderproperties_button( finder, contentViewer );
        var db = $.perc_build_download_button( finder, contentViewer );
        var ub = $.perc_build_upload_button( finder, contentViewer );
        var ri = $.perc_build_restore_button( finder, contentViewer );

        var menuEntries = [cp, fp, db, ub, ri];
        // Create the menu and the button
        var menu = createMenuHTML(menuEntries)
            .on("mouseenter",function(e){
                preventHide(e);
            })
            .on("mouseleave",function(e){
                hideOnMouseOut(e);
            });

        var btnHtml ='<div id="perc-finder-actions" >' +
            '<a id="perc-finder-actions-button" class="perc-font-icon" title="' +I18N.message("perc.ui.actions.button@Select An Action") +
            '" href="#"><span class="icon-cog fas fa-cog"></span><span class="icon-caret-down fas fa-caret-down"></span></a>' +
            '</div>';
        var btn = $(btnHtml)
            //.perc_button()
            .append(menu)
            .on("mouseenter",function(e){
                preventHide(e);
            })
            .on("mouseleave",function(e){
                hideOnMouseOut(e);
            });

        // This flag, hideOnMouseOut and preventHide prevent an unnatural hiding of the menu
        var flag_show = false;

        /**
         * Binds the hiding behavior to the menu once the cursor left it.
         */
        function hideOnMouseOut(e)
        {
            flag_show = false;
            setTimeout(function() {
                if (!flag_show) {
                    showMenu(false);
                }
            },500);
        }

        /**
         * Prevents the menu hiding if the cursor returns to the hover the menu or the button.
         */
        function preventHide(event)
        {
            var target = $(event.target);

            if (target.attr('id') === btn.attr('id'))
            {
                flag_show = true;
                return;
            }

            if (target.is("#perc-finder-actions *"))
            {
                flag_show = true;
            }
        }

        /**
         * Handler that get called when the button is clicked
         */
        function clickHandler(evt)
        {
            if ($('#perc-finder-actions-button').hasClass('ui-disabled'))
            {
                return false;
            }
            else
            {
                if (menu.css('display') === 'none')
                {
                    showMenu(true,event.pageX,event.pageY);
                }
                else
                {
                    showMenu(false);
                }
                return false;
            }
        }

        /**
         * Makes the menu visible/invisible.
         * @param boolean flag If true, makes the menu visible
         */
        function showMenu(flag,X,Y)
        {
            if (flag)
            {
                var menuX = X  - menu.outerWidth(true);
                var menuY = Y + 10;
                menu
                    .css("top", menuY)
                    .css("left", menuX)
                    .css("display", "block");
            }
            else
            {
                menu.hide();
            }
        }

        /**
         * Helper function to enable or disable the button in the finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            var anchor = $('#perc-finder-actions-button');
            if (flag)
            {
                // We perform an "unbind" first, in case clickHandler has been bound several times by error
                // (same thing in the else)
                anchor.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        clickHandler(evt);
                    });
            }
            else
            {
                anchor.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
        }

        /**
         * Creates the base HTML and adds the menu entries.
         * @param array of menuentries (former button elements)
         */
        function createMenuHTML(menuentries)
        {
            var dropdown = $("<ul class=\"perc-actions-menu box_shadow_with_padding\">");
            var option = $("<li class=\"perc-actions-menu-item\">");

            for(let l of menuentries){
                option.clone().append(l).appendTo(dropdown);
            }

            return dropdown;
        }

        var entriesListenedLeft = menuEntries.length;
        var entriesDisabled = 0;

        /**
         * Callback function that is called whenever an 'actions-change-enabled-state' event
         * is triggered. It uses closure to take advantage of storing state between asynchronous
         * calls and maintain state to finally enable/disable the actions button.
         * NOTE: To debug this function I recommend using console.log()
         */
        function entryChangeEnabledStateListener(evt)
        {

            // In this case, "this" represents the menu entry
            var state_enabled = evt.target.classList.contains("ui-enabled");
            if (entriesListenedLeft === 1 && entriesDisabled < menuEntries.length )
            {
                enableButton(true);
                entriesListenedLeft = menuEntries.length;
                entriesDisabled = 0;
            }
            else
            {
                // The entry is not the last, if is disabled count it
                if (!state_enabled)
                {
                    entriesDisabled++;
                }
                entriesListenedLeft--;
            }
        }

        // Bind the declared function to the buttons in the array menuEntries
        for (let m of menuEntries){
            m.on('actions-change-enabled-state', function(evt){
                entryChangeEnabledStateListener(evt);
            });
        }



        function update_action_btn(path){
            //Placeholder for capturing path changes.
        }

        finder.addPathChangedListener( update_action_btn );

        return btn;
    };

})(jQuery);

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

(function ($) {
    $.perc_build_delete_page_button = function (mcol, content) {
        var ut = $.perc_utils;
        var finder = $.perc_finder();
        var mcol_path = ['', 'site'];
        var warnOpenSpan = "<span id='perc-delete-warn-msg'>";
        var warnCloseSpan = "</span>";
        var spec;

        var btn = $('<a id="perc-finder-delete" class="perc-font-icon icon-remove fas fa-trash" title="' + I18N.message("perc.ui.delete.page.button@Click Delete Page") + '" href="#" ></a>')
            .off()
            .perc_button()
            .on("click",function(evt){
                deleteFn(evt);
            });

        function deleteFn(evt) {
            var encodedPath = mcol_path;
            if ($.perc_utils.isPathUnderDesign(encodedPath)) {
                encodedPath = $.perc_utils.encodePathArray(mcol_path);
            }
            $.PercFolderHelper().getAccessLevelByPath(encodedPath.join('/'), false, function (status, result) {
                if (status === $.PercFolderHelper().PERMISSION_ERROR) {
                    $.perc_utils.alert_dialog({ title: I18N.message("perc.ui.publish.title@Error"), content: result });
                    mcol.refresh();
                    return;
                }
                else if (result === $.PercFolderHelper().PERMISSION_READ) {
                    $.perc_utils.alert_dialog({ title: I18N.message("perc.ui.page.general@Warning"), content: I18N.message("perc.ui.delete.page.button@Delete Permissions") });
                    return;
                }
                else {
                    handleDelete();
                }
            });
        }

        /**
         ** Displays appropriate dialog to user when an asset cannot be deleted
         ** @param data (String) - response string from service
         ** @param textStatus (String) - status string
         ** @param errorThrown (String) - error string
         **/
        function asset_delete_handle_error(data, textStatus, errorThrown) {
            var title = I18N.message("perc.ui.deleteassetdialog.title@Delete Asset");
            delete_handle_error(data, "asset", title, function () {
                var assetId = spec.PathItem.id;
				 if (mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH) {
					purge_item(assetId, $.perc_paths.ASSET_PURGE, 'asset');
					return;
				}
                $.PercAssetService.forceDeleteAsset(assetId,
                    delete_success(assetId, 'asset'),
                    asset_delete_handle_error);
            });
        }


        /**
         ** Displays appropriate dialog to user when a page cannot be deleted
         ** @param data (String) - response string from service
         ** @param textStatus (String) - status string
         ** @param errorThrown (String) - error string
         **/
        function page_delete_handle_error(data, textStatus, errorThrown) {
            var title = I18N.message("perc.ui.deletepagedialog.title@Delete Page");
            delete_handle_error(data, "page", title, function () {
                var pageId = spec.PathItem.id;
                $.PercPageService.forceDeletePage(pageId,
                    delete_success(pageId, 'page'),
                    page_delete_handle_error);
            });
        }


        /**
         ** @param data (String) - response string from service
         ** @param type (String) - object type
         ** @param title (String) - dialog title
         **/
        function delete_handle_error(data, type, title, forceDeleteCallback) {
            var result = $.PercDeleteItemHelper.extractDeleteErrorMessage(data, spec.PathItem.name, type);
            if (result.canForceDelete) {
                if($("#perc-finder-delete-approved-ok").is(':visible')){
                    return;
                }
                showForceDeleteDialog(result.dialogid, title, result.content, result.chkBoxId, forceDeleteCallback);
            }
            else {
                ut.alert_dialog({
                    id: result.dialogid,
                    title: title,
                    content: warnOpenSpan + result.content + warnCloseSpan
                });
            }
        }

        function showForceDeleteDialog(id, title, content, chkBoxId, forceDeleteCallback) {
            var dia = $("<div/>").append(warnOpenSpan + content + warnCloseSpan).perc_dialog({
                id: id,
                title: title,
                success: function () {
                    if ($("#" + chkBoxId).length > 0) {
                        if ($("#" + chkBoxId).get(0).checked) {
                            forceDeleteCallback();
                        }
                    }
                },
                width: 500,
                resizable: false,
                percButtons: {
                    "Ok": {
                        id: 'perc-finder-delete-approved-ok',
                        cls: 'ui-state-disabled',
                        click: function () { }
                    },
                    "Cancel": {
                        id: 'perc-finder-delete-approved-cancel',
                        cls: 'perc-cancel',
                        click: function () { dia.remove(); }
                    }
                },
                open: function () {
                    $("#" + chkBoxId).on("click",function () {
                        if ($(this).get(0).checked) {
                            $("#perc-finder-delete-approved-ok").on("click",function () {
                                if ($("#" + chkBoxId).length > 0) {
                                    if ($("#" + chkBoxId).get(0).checked) {
                                        forceDeleteCallback();
                                        dia.remove();
                                    }
                                }
                            });
                            $("#perc-finder-delete-approved-ok").removeClass('ui-state-disabled');
                        }
                        else {
                            $("#perc-finder-delete-approved-ok").addClass('ui-state-disabled');
                        }
                    });
                }
            });

        }
        function delete_site() {
            $.PercBlockUI();
            $.perc_pagemanager.delete_site(spec.PathItem.name,
                function () {
                    dialog.remove();
                    var eventData = { type: 'site', name: spec.PathItem.name };
                    finder.fireActionEvent(finder.ACTIONS.DELETE, eventData);
                    setTimeout(function () {
                            $.PercDirtyController.setDirty(false);
                            $.PercNavigationManager.goToLocation(
                                $.PercNavigationManager.getView(), null, null, null, null, null, null);
                        },
                        200);
                },
                function (result) {
                    $.unblockUI();
                    site_delete_handle_error(result);
                });
        }

        function delete_asset() {
            var assetId = spec.PathItem.id;
            if (mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH) {
                purge_item(assetId, $.perc_paths.ASSET_PURGE, 'asset');
                return;
            }
            $.PercAssetService.deleteAsset(
                assetId,
                function () {
                    delete_success(assetId, 'asset');
                },
                asset_delete_handle_error
            );
        }

        function deleteFolder() {
            // do not validate if deleting folders and user is Admin
            if ($.PercNavigationManager.isAdmin()) {
                $.PercPathService.deleteFolderSkipValidation(mcol_path.join('/'), mcol_path[mcol_path.length - 1], mcol_path[1], function (data) {
                    cbDfSuccess(data);
                });
            }
            else {
                // call validation as usual
                $.PercPathService.deleteFolder(mcol_path.join('/'), mcol_path[mcol_path.length - 1], mcol_path[1], cbDfSuccess);
            }
        }

        function handleDelete() {

            $.perc_pathmanager.open_path(ut.acop(mcol_path), false, function (specResponse) {

                spec = specResponse;

                // Here is where everything starts

                // if we are deleting a page
                if (spec.PathItem.type === 'percPage') {

                    // we cant delete the landing page
                    if (spec.PathItem.category === 'LANDING_PAGE') {
                        return;
                    }

                    // if we are not deleting the landing page
                    else {

                        // check with server if we can delete this page
                        $.PercPageService.validateDeletePage(spec.PathItem.id,

                            // if we can delete this page
                            function () {

                                // before we delete the page we need to:
                                // 1. Save the current template, page, or asset they might be working on if they are dirty
                                // 2. Delete the page
                                // 3. Reload the template, page, or asset they were working on so they can see the effects
                                //    of having deleted the page on the template, page or asset they were working on

                                // prepare confirm dialog asking if they really want to remove the page
                                checkIfLinkedPage(spec);
                            }, page_delete_handle_error);

                    }
                }

                // if we are deleting a folder
                else if (spec.PathItem.type === 'Folder') {
                    if ((spec.PathItem.category === 'FOLDER') ||
                        spec.PathItem.category === 'SECTION_FOLDER' && mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH) {

                        deleteFolder();
                    }
                    else {
                        //not allowed
                        return;
                    }
                }

                // if we are deleting a theme folder or a theme file
                else if (spec.PathItem.type === 'FSFolder') {
                    // manually encode the url for non-Ascii characters
                    var url = $.perc_utils.encodePathArray(mcol_path);

                    $.PercPathService.deleteFSFolder(url.join('/'), mcol_path[mcol_path.length - 1], cbDfSuccess);
                }

                else if (spec.PathItem.type === 'FSFile') {
                    let url = "";
                    var paths = spec.PathItem.path.split("/");
                    paths = paths.slice(3, paths.length - 1);

                    $.each(paths, function (index, element) {
                        url = url + "/" + element;
                    });

                    // manually encode the url for non-Ascii characters
                    url = $.perc_utils.encodeURL(url);

                    $.PercWebResourcesService.deleteFile(url, mcol_path[mcol_path.length - 1], cbDfSuccess);
                }

                // if we are deleting an asset
                else {
                    // check with server if we can delete this asset
                    $.PercAssetService.validateDeleteAsset(
                        spec.PathItem.id,

                        // if we can delete this asset
                        function () {
							checkIfLinkedPageForAsset(spec);
                        }, asset_delete_handle_error
                    );

                }
            }, ut.show_error);
        }
		
		function getAssetDeleteQuestionString(spec,data){

            var message = mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH ? I18N.message("perc.ui.deleteassetdialog.purge@Confirm") : I18N.message("perc.ui.deleteassetdialog.warning@Confirm");
            var dialog = I18N.message("perc.ui.deleteassetdialog.tag@Asset") + ': ' + spec.PathItem.name + "<br/><br/>"	+message;
            if (data != null && data.ArrayList != null && data.ArrayList.length > 0) {

                dialog = I18N.message("perc.ui.publish.question@Remove From Site") + "<br/><br/>";
                $.each(data.ArrayList, function (index, value) {
                    if (index > 9) {
                        return false;
                    }
                    dialog += value.pagePath + '<br />';
                });
            }
            return dialog;
        }
		
		function validateIfAssetCanBeDeleted(spec,data,takeDownUrl) {
		   // prepare confirm dialog asking if they really want to remove the asset
			var dirtyType = $.PercDirtyController.dirtyObjectType;
			var title = mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH ? I18N.message('perc.ui.deleteassetdialog.title@Recycle Asset') : I18N.message('perc.ui.deleteassetdialog.title@Delete Asset');
			var message = mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH ? I18N.message("perc.ui.deleteassetdialog.purge@Confirm") : I18N.message("perc.ui.deleteassetdialog.warning@Confirm");
			var options = {
				id: 'perc-finder-delete-confirm',
				title: title,
				question: warnOpenSpan + getAssetDeleteQuestionString(spec,data) + warnCloseSpan,

				success: function () {
					$.PercDirtyController.setDirty(false);
					takeDownPageAndDeleteAsset(takeDownUrl,data);
				},
				yes: I18N.message("perc.ui.deletepagedialog.title@Delete Page")
			};
			ut.confirm_dialog(options);
  
        }
		
		function takeDownPageAndDeleteAsset(takeDownUrl,data){
			 var serviceCallback = function(status, results){
			if(status === $.PercServiceUtils.STATUS_ERROR)
			{
				page_delete_handle_error(results.request,results.textstatus,results.error);
			}
			else
			{
				delete_asset();
			}
		 };

		$.PercServiceUtils.makeJsonRequest(takeDownUrl, $.PercServiceUtils.TYPE_PUT, false, serviceCallback, data);
	}

        /**
         * Delete folder success callback, simply refresh the finder.
         */
        function cbDfSuccess(data) {
            setTimeout(function () { mcol.refresh(); }, 200);
            var eventData = { type: 'folder' };
            finder.fireActionEvent(finder.ACTIONS.DELETE, eventData);
        }

        function getDeleteQuestionString(spec,data){

            var message = mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH ? I18N.message("perc.ui.deletepagedialog.purge@Confirm") : I18N.message("perc.ui.deletepagedialog.warning@Confirm");
            var dialog = I18N.message("perc.ui.deletepagedialog.tag@Delete Page") + ': ' + spec.PathItem.name + "<br/><br/>"	+message;
            if (data != null && data.ArrayList != null && data.ArrayList.length > 0) {

                dialog = I18N.message("perc.ui.publish.question@Remove From Site") + "<br/><br/>";
                $.each(data.ArrayList, function (index, value) {
                    if (index > 9) {
                        return false;
                    }
                    dialog += value.pagePath + '<br />';
                });
            }
            return dialog;
        }

        function validateIfPageCanBeDeleted(spec,data,takeDownUrl) {
            var dirtyType = $.PercDirtyController.dirtyObjectType;
            var title = mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH ? I18N.message("perc.ui.deletepagedialog.title@Recycle Page") : I18N.message("perc.ui.deletepagedialog.title@Delete Page");
            var options = {
                id: 'perc-finder-delete-confirm',
                title: title,
                question: warnOpenSpan + getDeleteQuestionString(spec,data) + warnCloseSpan,
                success: function () {
					$.PercDirtyController.setDirty(false);
                    takeDownPageAndDeletePage(takeDownUrl,data,spec);
                },
                yes: I18N.message("perc.ui.deletepagedialog.title@Delete Page")
            };
            ut.confirm_dialog(options);
        }
		
		function takeDownPageAndDeletePage(takeDownUrl,data,spec){
			 var serviceCallback = function(status, results){
			if(status === $.PercServiceUtils.STATUS_ERROR)
			{
				page_delete_handle_error(results.request,results.textstatus,results.error);
			}
			else
			{
				delete_page(spec);
			}
		 };

		$.PercServiceUtils.makeJsonRequest(takeDownUrl, $.PercServiceUtils.TYPE_PUT, false, serviceCallback, data);
	}
        // recycles an item.  now calls purge_page() if path starts with /Recycling.
        function delete_page(spec) {
            if (mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH) {
                purge_item(spec.PathItem.id, $.perc_paths.PAGE_PURGE, 'page');
                return;
            }

            $.perc_pagemanager.delete_page(spec.PathItem.id,
                function () {
                    delete_success(spec.PathItem.id, 'page');
                },
                page_delete_handle_error);
        }
        function purge_item(id, path, type) {
            $.PercRecycleService.purgeItem(
                id,
                path,
                function(status, data) {
                    if (status === $.PercServiceUtils.STATUS_ERROR) {
                        console.error('Error!');
                    } else {
                        delete_success(id, type);
                    }
                }
            );
        }

        function checkIfLinkedPage(spec) {
            var findLinkedItemsUrl = $.perc_paths.ITEM_LINKED_TO_ITEM + "/" + spec.PathItem.id;
            var takeDownUrl =  $.perc_paths.PAGE_TAKEDOWN ;
            takeDownUrl+="/" + spec.PathItem.id;

            $.PercServiceUtils.makeJsonRequest(findLinkedItemsUrl, $.PercServiceUtils.TYPE_GET, false, function(status, result) {
                if (status === $.PercServiceUtils.STATUS_ERROR) {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result);
                    console.error(defaultMsg);
                    // if there is an error, we proceed with previous behavior (no confirm display)
                    validateIfPageCanBeDeleted(spec,null,null);

                }
                else {
                    validateIfPageCanBeDeleted(spec,result.data,takeDownUrl);

                }
            }, null);

        }
		
		function checkIfLinkedPageForAsset(spec) {
            var findLinkedItemsUrl = $.perc_paths.ITEM_LINKED_TO_ITEM + "/" + spec.PathItem.id;
            var takeDownUrl =  $.perc_paths.PAGE_TAKEDOWN ;
            takeDownUrl+="/" + spec.PathItem.id;

            $.PercServiceUtils.makeJsonRequest(findLinkedItemsUrl, $.PercServiceUtils.TYPE_GET, false, function(status, result) {
                if (status === $.PercServiceUtils.STATUS_ERROR) {
                    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result);
                    console.error(defaultMsg);
                    // if there is an error, we proceed with previous behavior (no confirm display)
                    validateIfAssetCanBeDeleted(spec,null,null);

                }
                else {
                    validateIfAssetCanBeDeleted(spec,result.data,takeDownUrl);

                }
            }, null);

        }

        /**
         * Handles cleanup after an item is successfully deleted.
         *
         * @param id the id of the item.
         */
        function delete_success(id, type) {
            setTimeout(function () { mcol.refresh(); }, 200);
            var isOpen = false;
            if (id===$.PercNavigationManager.getId()) {
                isOpen = true;
                content.clear();
            }

            var eventData = { type: type, id: id, isOpen: isOpen };
            finder.fireActionEvent(finder.ACTIONS.DELETE, eventData);

            // Refreshes the pages in the design manager carousel
            $("form.perc-template-pages-controls").trigger("submit");
            var clickEvent = jQuery.Event("click");
            clickEvent.deletedPageId = id;
            $(".resetPaging").trigger(clickEvent);

            // Reload the current view if it is not VIEW_EDITOR, without pageId (deleted) in memento.
            var view = $.PercNavigationManager.getView();
            if (type === 'page' && view !== $.PercNavigationManager.VIEW_EDITOR) {
                page_deleted(view);
            }
        }
        /**
         * Handles redirect if after deleting a page, the current view is not VIEW_EDITOR
         *
         * @param view the view to redirect to.
         */
        function page_deleted(view) {
            if (typeof memento != 'undefined') {
                var mem = { 'templateId': memento.templateId, 'pageId': null };
                // Use the PercNavigationManager to reload to the template editor without pageId
                var querystring = $.deparam.querystring();
                $.PercNavigationManager.goToLocation(
                    view,
                    querystring.site,
                    null,
                    null,
                    null,
                    querystring.path,
                    null,
                    mem);
            }
        }

        /**
         * Update the button state based on the current selection.
         * @param path {string} the path of the selected item, cannot be <code>null</code>.
         */
        function update_btn(path) {
            mcol_path = path;
            // Unfortunately need to call path manager to get info about
            // the node.
            $.perc_pathmanager.open_path(
                ut.acop(path),
                false,
                function (spec) {
                    var type = spec.PathItem.type;
                    var cat = spec.PathItem.category;
                    var disable = (typeof (cat) != 'undefined') &&
                         (cat === 'LANDING_PAGE' ||
                             (cat === 'SECTION_FOLDER' && mcol_path[1] !== $.perc_paths.RECYCLING_ROOT_NO_SLASH) ||
                             cat === 'SYSTEM');
                    if (type === 'Folder' && spec.PathItem.accessLevel !== $.PercFolderHelper().PERMISSION_ADMIN)
                        disable = true;
                    else if ((cat === 'ASSET' || cat === 'PAGE') && spec.PathItem.accessLevel === $.PercFolderHelper().PERMISSION_READ)
                        disable = true;

                    // Story CM-79: if the type is FSFile or FSFolder, the cat will still be SYSTEM, so we need to recheck
                    if ((type === "FSFolder" || type === "FSFile") && mcol_path.length >= 5 &&
                        spec.PathItem.accessLevel !== $.PercFolderHelper().PERMISSION_ADMIN) {
                        disable = false;
                    }

                    if (mcol_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH && mcol_path.length <= 3) {
                        disable = true;
                    }

                    if (!disable && mcol_path.length > 2) {
                        if (type === 'site') {
                            $(".perc-finder-menu #perc-finder-delete").removeClass('ui-enabled').addClass('ui-disabled').off('click');
                        }
                        else {
                            $(".perc-finder-menu #perc-finder-delete").removeClass('ui-disabled').addClass('ui-enabled').off().on('click',
                                function(evt){
                                deleteFn(evt);
                                });
                        }
                    }
                    else {
                        $(".perc-finder-menu #perc-finder-delete").removeClass('ui-enabled').addClass('ui-disabled').off('click');
                    }
                },
                function (request) {
                    // The path was just navigated to in the finder (e.g. immediately
                    // after folder create/rename), so it must exist. The lookup can
                    // transiently fail with a 404/500 while the JCR finishes indexing
                    // the new/renamed path - silently disable the delete button
                    // rather than showing a misleading "Path not found" error dialog.
                    $(".perc-finder-menu #perc-finder-delete").removeClass('ui-enabled').addClass('ui-disabled').off('click');
                }
            );

        }

        mcol.addPathChangedListener(update_btn);

        return btn;
    };
})(jQuery);

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
 * Handles the copy page action.
 */
(function ($) {
    $.perc_build_preview_button = function (mcol, content) {

        var btn = $("<a id='perc-finder-preview' class='perc-font-icon icon-eye-open fas fa-eye' href='#' title='" +I18N.message("perc.ui.preview.button@Launch Preview") + "'></a>")
            .perc_button().on("click",function (event) {
                launchPreview();
            });

        function launchPreview(event)
        {
            if ($("#perc-finder-listview .perc-datatable-row-highlighted").length > 0)
            {
                var listSelectedRowData = $("#perc-finder-listview .perc-datatable-row-highlighted").data("percRowData");

                if (listSelectedRowData.category === "LANDING_PAGE" || listSelectedRowData.category === "PAGE")
                {
                    mcol.launchPagePreview(listSelectedRowData.id);
                }
                else if (listSelectedRowData.category === "ASSET")
                {
                    mcol.launchAssetPreview(listSelectedRowData.id);
                }
            }
            else
            {
                var selectedPage = $(".mcol-opened.perc-listing-type-percPage");
                var selectedAsset = $(".mcol-opened.perc-listing-category-ASSET");

                if (selectedPage.length > 0)
                {
                    mcol.launchPagePreview(selectedPage.data("spec").id);
                }
                else if (selectedAsset.length > 0)
                {
                    mcol.launchAssetPreview(selectedAsset.data("spec").id);
                }
            }
        }

        function update_launch_preview_btn(path)
        {
            var selectedPageColumn = $(".mcol-opened.perc-listing-type-percPage");
            var selectedAssetColumn = $(".mcol-opened.perc-listing-category-ASSET");
            var selectedItemList = $("#perc-finder-listview .perc-datatable-row-highlighted");

            if (path[1] === "Sites" && path.length < 4)
            {
                enableButtonLaunchPreview(false);
            }
            else if(path[1]==="Recycling"){
                enableButtonLaunchPreview(false);
            }
            else if (selectedItemList.length > 0)
            {
                listSelectedRowData = selectedItemList.data("percRowData");
                if (listSelectedRowData.category == "LANDING_PAGE" || listSelectedRowData.category == "PAGE" || listSelectedRowData.category == "ASSET")
                {
                    enableButtonLaunchPreview(true);
                }
                else
                {
                    enableButtonLaunchPreview(false);
                }
            }
            else if (selectedPageColumn.length > 0)
            {
                var last_path = selectedPageColumn.data("spec").path.split("/");
                if (last_path.length == path.length && $(last_path).last()[0] == $(path).last()[0])
                {
                    enableButtonLaunchPreview(true);
                }
                else
                {
                    enableButtonLaunchPreview(false);
                }
            }
            else if (selectedAssetColumn.length > 0) {
                if (selectedAssetColumn.data("spec") !== undefined) {

                    last_path = selectedAssetColumn.data("spec").path.split("/");
                    if (last_path.length === path.length && $(last_path).last()[0] === $(path).last()[0]) {
                        enableButtonLaunchPreview(true);
                    } else {
                        enableButtonLaunchPreview(false);
                    }
                }
            }
            else
            {
                enableButtonLaunchPreview(false);
            }
        }

        /**
         * Helper function to enable or disable the new folder button on finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButtonLaunchPreview(flag)
        {
            if(flag)
            {
                $( "#perc-finder-preview" ).removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        launchPreview(evt);
                    } );

            }
            else
            {
                $( "#perc-finder-preview" ).addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
        }

        mcol.addPathChangedListener( update_launch_preview_btn );
        return btn;
    };
})(jQuery);


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
 * Creates the Content Finder, a top-level element used to explore
 * and interact with the directory structure.
 *
 * The finder is a singleton that can be retrieved by calling
 * $.perc_finder()
 */

var assetPagination = null;

(function($){

    var defaults = {
        // set the starting height of the finder
        height: 200,
        // set the minimum width/height of the finder on resize
        minWidth: 952,
        minHeight: 100,
        get_max_height: function get_max_finder_height () {
            // 100 >= header and finder header areas
            return $(window).innerHeight() - 150;
        }
    };


    if(assetPagination == null) {
        var url =  $.perc_paths.GET_ASSET_PAGINATION_CONFIG ;
        $.ajax( {
            type: 'GET',

            url: url,
            dataType: 'json',
            cache: false,
            success:  function(data){
                assetPagination = data;
            }
        });
    }
    $.perc_finder = function () {

        if($.perc_finderInstance == null)
            $.perc_finderInstance = finder();
        return $.perc_finderInstance;
    };

    function finder() {

        // WARN: expect this method to be called after body onload
        // determine the width of the native scrollbar
        // http://visualpulse.net/forums/index.php?topic=120.0
        $("body").append('<div id="perc-wide_scroll_div_one" style="width:50px;height:50px;overflow-y:hidden;position:absolute;top:-200px;left:-200px;"><div id="wide_scroll_div_two" style="height:100px;width:100%"></div></div>');
        var SCROLLBAR_WIDTH = $("#perc-wide_scroll_div_one").width() - $("#wide_scroll_div_two").innerWidth();
        $("#perc-wide_scroll_div_one").remove();

        // constant for finder listing id prefix
        var FINDER_LISTING_ID_PREFIX = "perc-finder-listing-";
        // maximum result for page, the user could configured this in the future.
        var MAX_RESULTS = 100;
        var flagChangeView = true,
            dragging = false,
            finderExpandStateCookie = "perc-finder-expand-state",
            first_dir = fn_first_dir(),
            top = make_top_level(first_dir),
            ut = $.perc_utils,
            current_path = [],
            currentItem = null;
        actionListeners = [],
            openListeners = [],
            _finderPathIdArray = {},
            _percCompareService = $.PercCompareService();
            path_changed = function(p){ current_path = p; },
            finderOpenInProgress = false,
            lastClickPath = null,
            dragDelay = ($.PercNavigationManager.isAutoTest() ? 0 : 250),
            actions = {
                DELETE: 'delete',
                FINDER_OPEN_START : 'open_start',
                FINDER_OPEN_END: 'open_end'},
                isLibMode = ((typeof gInitialScreen !== 'undefined') && (gInitialScreen === "library"));
        //Preload images
        $.perc_utils.preLoadImages(
            "/cm/images/images/loading.gif"
        );

        function finder_do_goto_or_search (event) {
            event.preventDefault();
            var val, isGoto, $control, $input, isNotAllowed;
            $control = $(this).parents('.perc-finder-goto-or-search');
            $input = $control.find('input.perc-finder-goto-or-search:first');
            val = $input.val();
            isGoto = /^\//.test(val);
            isNotAllowed=/\[/.test(val) || /]/.test(val);//check if bracket is there in url[]
            if(isNotAllowed) {
                return false;
            }
            if (val) {
                if(isGoto) {
                    //$('#mcol-path-summary').val(val);
                    $('#perc-finder-go-action').trigger('click');
                } else {
                    $('#perc-finder-item-search').val(val);
                    $('#perc-finder-search-submit').trigger('click');
                }
            }
            return false;
        }

        // attach event handlers to the dom
        $('body').on('click', '.perc-action-goto-or-search', finder_do_goto_or_search);
        $.perc_filterField($("#mcol-path-summary"), $.perc_textFilters.PATH);
        $("#mcol-path-summary").on("keyup",function(evt){
            if (evt.keyCode === 13){
                $("#mcol-path-summary").trigger("blur");
                finder_do_goto_or_search.apply(this, [evt]);
                $("#mcol-path-summary").trigger("focus");
                evt.preventDefault();
                evt.stopPropagation();
            }
            if (evt.keyCode === 27 || evt.keyCode === 9){
                $("#mcol-path-summary").val(getCurrentPath().join("/")).trigger("blur");
                $("#perc-finder-item-search").trigger("blur");
                evt.preventDefault();
                evt.stopPropagation();
            }
            // hide the message if it's visible
            showFinderErrorMessage(false);
        });
        $(document).on('mousedown',function(evt){
            if (evt.target.id !== "perc-finder-go-action" && evt.target.id !== "mcol-path-summary"){
                $("#mcol-path-summary").trigger("blur");
                if (evt.target.id !== "perc-finder-search-submit" &&     // Need this condition to clean path when performing search
                    evt.target.id !== "perc-finder-listing-Search" &&    // else any click in the screen will override the path in other view
                    $(evt.target).parents(".perc-datatable-row").length > 0)
                {
                    $("#mcol-path-summary").val(getCurrentPath().join("/"));
                }

            }
            if (evt.target.id !== "perc-finder-item-search"){
                $("#perc-finder-item-search").trigger("blur");
            }
            // hide the message if it's visible
            showFinderErrorMessage(false);
        });
        $("#perc-finder-go-action").on("click",function(){
            var viaGoButton = true;
            goToNewPath(viaGoButton);
        });

        function absPath(strPath){
            var path = strPath.split("/");
            for(let i=0; i < path.length; i++){
                if (path[i] === ".."){
                    if (i-1 >= 0){
                        path.splice(i-1, 2);
                        i = i - 2;
                    }
                    else{
                        path.splice(i, 1);
                        i--;
                    }
                }
                if(path[1] === "."){
                    path.splice(i, 1);
                    i--;
                }
            }
            return path.join("/");
        }

        function goToNewPath(viaGoButton){
            $("#perc-finder-choose-listview").removeClass('ui-state-disabled');
            $("#perc-finder-choose-columnview").removeClass('ui-state-disabled');
            $("#perc-finder-item-name").removeClass('mcol-opened');
            var newPath = $("#mcol-path-summary").val().trim();
            var currPath = getCurrentPath().join("/");
            //eliminate duplicate "/"
            newPath = newPath.replace( /\/(\/)+/g, '/');
            //Convert a relative path to an absolute and correct path
            newPath = absPath(newPath);
            newPath = (newPath.charAt(0) !== "/") ? "/" + newPath : newPath;
            newPath = (newPath === "/" || newPath === "")? "/" + getCurrentPath()[1] : newPath;
            if (newPath === currPath && viaGoButton === true &&
                ($.Percussion.getCurrentFinderView() !== $.Percussion.PERC_FINDER_SEARCH_RESULTS  && // This condition avoids the check when
                    $.Percussion.getCurrentFinderView() !== $.Percussion.PERC_FINDER_RESULT)){    // in search view to force the path change
                return;
            }else {

                $.PercPathService.validatePath(newPath, function(status, result){
                    if (status === $.PercServiceUtils.STATUS_SUCCESS){
                        //validatePath return the exact caseSensitive path.
                        $("#mcol-path-summary").val(result);
                        if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR) {
                            var viewWrapper = $.PercComponentWrapper("perc-action-finder-go-clicked", ["perc-ui-component-finder"]);
                            var isWrapperSet = $.PercViewReadyManager.setWrapper(viewWrapper);
                            if (!isWrapperSet) {
                                if (!isWrapperSet) {
                                    $.PercViewReadyManager.showRenderingProgressWarning();
                                    return;
                                }
                            }
                        }
                        open(result.split("/"), function(){});
                    }
                    else {
                        showFinderErrorMessage(true,result);
                    }
                });
            }
        }

        function validatePath(evt, newPath, callback){
            // encode the newPath if it is under design
            var encodedPath = newPath;
            if($.perc_utils.isPathUnderDesign(newPath))
            {
                encodedPath = $.perc_utils.encodePathArray(newPath);
            }

            $.PercPathService.validatePath(encodedPath.join("/"), function(status, result){
                if (status === $.PercServiceUtils.STATUS_SUCCESS){
                    callback(evt);
                }
                else {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.finder@Path not found") + newPath.join("/")});
                    refresh();
                }
            });
        }

        function showFinderErrorMessage(show, message){
            var $message = $('.perc-finder-message');
            if (show) {
                $message.empty().append('<i class="icon-bell fas fa-bell" aria-hidden="true"></i>');
                $('<label class="perc-finder-error"></label>').text(message).appendTo($message);
                $message.fadeIn();
            } else {
                $message.fadeOut(function () {$(this).empty();});
            }
            fixIframeHeight();
            fixHeight();
        }

        // SCOPE: finder state in cookie

        /** determine if finder should be collapsed or not
         Note: cookie contains 'expanded|collapsed:###' where ### is height of finder */
        function get_finder_is_collapsed_from_cookie () {
            return /collapsed/.test($.cookie(finderExpandStateCookie));
        }

        /** get the saved height, returns ### or '' */
        function get_finder_height_from_cookie () {
            return ('' + $.cookie(finderExpandStateCookie)).replace(/[^0-9.]/g, '');
        }

        /** set the finder height in the cookie */
        function set_finder_height_in_cookie (height) {
            if (!isLibMode && get_finder_height_from_cookie() !== height) {
                var state = get_finder_is_collapsed_from_cookie() ? 'collapsed' : 'expanded';

                var options = {"sameSite": "Lax"};
                if (window.isSecureContext) {
                    options.secure = true;
                }
                $.cookie(finderExpandStateCookie, state + height, options);
            }
        }

        /** set the finder expanded state in the cookie */
        function set_finder_expand_state_in_cookie (isExpanded) {
            if (!isLibMode) {
                var height, state;
                height = get_finder_height_from_cookie();
                state = isExpanded ? 'expanded' : 'collapsed';
                var options = {"sameSite": "Lax"};
                if (window.isSecureContext) {
                    options.secure = true;
                }
                $.cookie(finderExpandStateCookie, state + height, options);
            }
        }

        function notify_resize () {
            fixBottomHeight();
            fixIframeHeight();
            fixHeight();
            // refresh Architecture view
            if( $("#perc_site_map").length > 0 )   {
                try {
                    $("#perc_site_map").perc_site_map('layoutAll');
                }catch(error){
                    //Getting Initialization error in case site not selected... needs to be ignored
                }
            }
        }

        /**
         * Helper function to expand or collapse the finder.
         * @param expand {boolean} flag indicating an expand action request.
         */
        function expandCollapseFinder (expand) {
            var $button, $header, $finder = $(".perc-finder-body");
            if($finder.is(":visible") === expand) {
                return; // Nothing to do
            }
            $button = $('#perc-finder-expander');
            $header = $button.parents('.perc-finder-outer');
            set_finder_expand_state_in_cookie(expand);
            if (expand) {
                $header.removeAttr('collapsed');
                $finder.slideDown(notify_resize);
                $button.removeClass('icon-plus-sign')
                    .removeClass(' fas fa-plus').
                addClass('icon-minus-sign').
                addClass('fas fa-minus');
            } else {
                $header.attr('collapsed', true);
                $finder.slideUp(notify_resize);
                $button.removeClass('icon-minus-sign').
                removeClass('fas fa-minus').
                addClass('icon-plus-sign').
                addClass('fas fa-plus');
            }
            var frame  = $('#frame');
            var header = $('.perc-main');
            var bottom = $('#bottom');
        }

        /**
         * Toggle the finders expanded/collapsed state
         */
        function percFinderMaximizer (evt) {
            expandCollapseFinder(!$(".perc-finder-body").is(":visible"));
        }

        $("#perc-finder-expander").on("click",
            function(evt){
                percFinderMaximizer(evt);
            });

        /** if not easy to coerce val into a number return def */
        function integer (val, def) {
            val =  parseInt(val, 10);
            return isNaN(val) ? def : val;
        }

        var update_finder_height = (function () {

            // SCOPE: process resizing the finder

            /** current list of ui-resizable elements */
            var $finder_columns, toolbar, height;

            /** collect the current columns to update */
            function on_start_resize () {
                $('.perc-finder').addClass('ui-resizable-resizing');
                $finder_columns = $('.perc-finder .ui-resizable');
                toolbar = integer($('.perc-pagingbar-finder').outerHeight(), 0);
            }

            /** update the finder column heights */
            function on_resize () {
                set_finder_column_heights();
            }

            /** clean up jquery event setting the width, save state */
            function on_stop_resize (event) {
                notify_resize();
                $(this).css('width', 'auto');
                if(event)
                    set_finder_height_in_cookie(height);
                $('.perc-finder').removeClass('ui-resizable-resizing');
            }

            /** return scrollbar width if scrollbar is showing */
            function get_scrollbar_width (n) {
                return n && n.scrollWidth > n.clientWidth ? SCROLLBAR_WIDTH : 0 ;
            }

            /** set the height of the columns adjusting for scrollbar */
            function set_finder_column_heights () {
                var n = $('.perc-finder')[0];
                height = $('.perc-finder-body').height();
                $finder_columns.css('height', height - get_scrollbar_width(n) - toolbar);
            }

            /** set the finder height and update the column heights */
            function set_finder_body_height (new_height) {
                // WARN: this method sets height programmatically, don't call from resize event
                //If it is in library mode always keep the finder to maximum height
                if(isLibMode)
                    new_height = defaults.get_max_height()-100;

                new_height = integer(new_height, $('.perc-finder-body').height());
                if (new_height !== height) {
                    height = Math.max(new_height, defaults.minHeight);
                    height = Math.min(new_height, defaults.get_max_height());
                    $('.perc-finder-body').css('height', height);
                    set_finder_height_in_cookie(height);
                    // notify listeners that we resized the finder
                    $('.perc-finder').trigger('resize');
                }
                // new columns may have been added, so make sure their heights are set
                on_start_resize();
                set_finder_column_heights();
                on_stop_resize();
            }

            $(".perc-finder-body").resizable({
                handles: 's',
                minHeight: defaults.minHeight,
                maxHeight: defaults.get_max_height(),
                start: on_start_resize,
                resize: on_resize,
                stop: on_stop_resize
            });

            $(window).on("resize",function on_window_resize () {
                $('.perc-finder-body').resizable('option', 'maxHeight', defaults.get_max_height());
            });

            // initialize height from the cookie or use default
            set_finder_body_height(integer(get_finder_height_from_cookie(), defaults.height));

            // expose method to update the finder height
            return set_finder_body_height;

        });

        // WARN: don't initialize the finder expand until its height is initialized
        // initialize the finder in the ui to the correct expand state
        var expandFinder = $('[view=PERC_SITE]').length || !get_finder_is_collapsed_from_cookie();
        if(isLibMode)
            expandFinder = true;
        expandCollapseFinder(expandFinder);

        return {

            /** event pub/sub handler for finder resize events */
            on: function () {
                $('.perc-finder-body').on.apply($('.perc-finder-body'), arguments);
            },

            // Action constants
            ACTIONS: actions,
            //The top-level element (add it to a page to use the Finder).
            elem: top,

            //open a given path in the finder.
            open: open,

            //display a list of search results in the finder.
            search: search,

            addPathChangedListener : addPathChangedListener,
            executePathChangedListeners : executePathChangedListeners,
            finderOpenInProgress: finderOpenInProgress,

            /* notify finder to update finder columns and/or set a new height */
            update_finder_height: update_finder_height,

            goToNewPath: goToNewPath,

            refresh: refresh,

            idFromItem: idFromItem,

            addActionListener: addActionListener,

            removeActionListener: removeActionListener,

            fireActionEvent: fireActionEvent, // Only exposed so the button classes can fire.

            addOpenListener: addOpenListener,

            removeOpenListener: removeOpenListener,

            getCurrentPath: getCurrentPath,

            getPathItemByPath: getPathItemByPath,

            getPathItemById:getPathItemById,

            getParentPathItemByPath: getParentPathItemByPath,

            launchPagePreview: launchPagePreview,

            launchPagePreviewByPath: launchPagePreviewByPath,

            launchAssetPreview: launchAssetPreview,

            launchPageCompareView: launchPageCompareView,

            insertAfter: insert_after,

            maxResults: MAX_RESULTS,

            flagChangeView: flagChangeView,

            onDragStart: onDragStart,

            onDragStop: onDragStop,

            scrollIntoView: scroll_into_view,

            setStateButtonsDesignNode : setStateButtonsDesignNode,

            getCurrentItem : getCurrentItem,

            setCurrentItem : setCurrentItem,
			getCompareService : getCompareService

        };

		function getCompareService(){
			return _percCompareService;
		}

        function getCurrentItem(){
            return currentItem;
        }

        function setCurrentItem(item){
            currentItem = item;
            return currentItem;
        }

        function _addToPathIdArray(path, id)
        {
            _finderPathIdArray[path] = id;
        }

        /**
         * Helper function to create an id for a listing.
         * @param item {object} item summary object, cannot be <code>null</code>.
         * @return the id string
         * @type string
         */
        function idFromItem(item) {
            var postfix = typeof(item.id) === 'undefined' ?
                item.path.split("/")[1] :
                item.id;
            return FINDER_LISTING_ID_PREFIX + postfix;
        }

        function refresh(k){
            $('.mcol-opened').each(function(){
                $(this).removeClass('mcol-opened');
            });
            var fwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-finder');
            if($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR && (fwrapper == null || fwrapper.wrapperName !== "perc-action-finder-refresh")){
                var compArray = [];
                compArray.push("perc-ui-component-finder");

                var viewWrapper = $.PercComponentWrapper("perc-action-finder-refresh",compArray);
                var isWrapperSet = $.PercViewReadyManager.setWrapper(viewWrapper);
                if(!isWrapperSet){
                    $.PercViewReadyManager.showRenderingProgressWarning();
                    return;
                }
            }
            open( current_path,k);
        }

        /**
         * Return a copy of the finder's current path array object
         * @return the path array.
         * @type array
         */
        function getCurrentPath(){
            return current_path.slice(0);
        }

        function search( item_list ) {
            //Close whatever is currently open.
            close_after( first_dir );
            dir_children( first_dir ).filter( '.mcol-opened' ).removeClass( '.mcol-opened' );

            //Add a new directory to hold the search results.
            var dir = insert_after( first_dir );

            //Add the search results to the new directory.
            $.each( item_list, function() {
                add_item( dir, make_item( this, open_from_dir( dir ) ) );
            });

        }

        function open_from_dir( dir ) {
            //Open a given element in a given directory - used by the
            //click callback on each element.
            return function( next, path ) {
                var path_sans_name = ut.acop( path );
                path_sans_name.pop();
                open_next( next, dir, [ next.data( 'name' ) ], path_sans_name,
                    function(){} );
            };
        }

        function err( str ) {

            finderOpenInProgress = false;
            fireActionEvent(actions.FINDER_OPEN_END, null);

            current_path =  ["","Sites"];
            refresh(function(){});

            /*
                    window.parent.jQuery.perc_utils.alert_dialog({
                        title: 'Finder Error' ,
                        content: str,
                        okCallBack: function (){
                            return true ;
                        }
                    });
            */
        }

        function open( path, k ) {
            if(finderOpenInProgress)
                return; // Only one open action can be in progress at any one time
            finderOpenInProgress = true;
            fireActionEvent(actions.FINDER_OPEN_START, null);
            //We load the first directory using the root [""] path.
            var initial_loader = load_folder_path( [""] );
            //Enter the recursive _open function
            _open( first_dir, initial_loader, path.slice(1), [""], k );
        }


        function _open( dir, loader, path, new_path, k ) {

            // If the last element in the path array is empty, remove it.
            // If it's not removed, then an open operation will be always in progress
            // (take a look at the finderOpenInProgress variable).
            if ( path[ path.length - 1 ] === '' )
                path.pop();

            //store the next child item to find the correct page that contains the item
            if (path.length > 0)
                dir.data("child", path[0]);

            if (new_path.length > 1 && new_path[1] === $.perc_paths.DESIGN_ROOT_NO_SLASH)
            {
                setStateButtonsDesignNode(true);
            }
            else
            {
                setStateButtonsDesignNode(false);
            }

            //Load the directory contents from the server
            $('.perc-finder-panel-loading').remove();
            dir_container(dir).append('<div class="perc-finder-panel-loading"><span class="icon-spinner icon-spin icon-2x"></span>&nbsp;Loading...</div>');
            loader( onLoad, dir );
            function onLoad( children, content) {
                var fwrapper;
                if( content ) {
                    //If the contents are given directly, add them.
                    dir.find('.mcol-direc-wrapper').empty().addClass('mcol-direc-wrapper-last').append(content);
                } else {
                    //If we have a list of children, update the directory
                    //to reflect the current set of children.
                    update_dir( dir, children);
                }
                if( path.length === 0 ) {
                    //We have finished opening to our destination.

                    //Set the path summary to the correct path.
                    $("#mcol-path-summary").val( new_path.join('/') );
                    // here we are injecting the siteimprove plugin
                    var searchPath = $("#mcol-path-summary").val();
                    if((typeof searchPath !== 'undefined') && (searchPath.indexOf('/Sites') >= 0) && searchPath !== '/Sites') {
                        var siteName = getSiteNameByPath(searchPath);
                        injectSiteImprove(siteName, getCurrentPath());
                    }

                    finderOpenInProgress = false;
                    fireActionEvent(actions.FINDER_OPEN_END, null);

                    path_changed( new_path);

                    //Close anything after this.
                    close_after( dir );
                    dir_children( dir ).filter( '.mcol-opened' ).removeClass( 'mcol-opened' );

                    fwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-finder');
                    if(fwrapper != null)
                        fwrapper.handleComponentProgress('perc-ui-component-finder', "complete");
                    //Call the continuation.
                    if( k ){ k(); }
                } else {
                    //Find the element corresponding to the next path element.
                    var next = dir_children( dir ).filter( function() {
                        if (typeof($(this).data('name')) != "undefined" && typeof(path[0]) != "undefined"){
                            return $(this).data('name') === path[0];
                        }
                        return false;
                    });

                    $.PercQueuePostAJAX(function(){
                        setTimeout(function(){
                            open_next( next, dir, path, new_path, k );
                        }, 150);
                    });
                }
                update_finder_height();
                fwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-finder');
                if(fwrapper != null)
                    fwrapper.handleComponentProgress('perc-ui-component-finder', "processing");

            }
        }

        function pagePrevious(evt,target){
            var dir = target.parents("td");
            var newStartIndex = dir.data('startIndex') - MAX_RESULTS;
            dir.data('startIndex', newStartIndex);
            load_folder(dir, $(this).closest('.perc-paging-finder').is('.perc-paging-finder-bottom'));
        }

        function pageNext(evt,target){
            var dir = target.parents("td");
            var newStartIndex = dir.data('startIndex') + MAX_RESULTS;
            dir.data('startIndex', newStartIndex);
            load_folder(dir, $(this).closest('.perc-paging-finder').is('.perc-paging-finder-bottom'));
        }

        //Retrieve another page of paging result
        function load_folder(dir, scrollBottom) {
            //Generate the url with startIndex and maxResult.
            var path = dir.data('path');
            if(typeof path === 'undefined'){
                return;
            }
            var startIndex = dir.data('startIndex');
            var str_path = $.perc_utils.encodeURL(path.join("/")) + "/?startIndex=" + startIndex + "&maxResults=" + MAX_RESULTS;

            $.perc_pathmanager.open_path( str_path, true, getChildren, err, true );
            function getChildren( folder_spec ) {
                var children = {};
                $.each( $.perc_utils.convertCXFArray(folder_spec.PagedItemList.childrenInPage), function() {
                    //Use the postfix "_item" to avoid reserved name collision (e.g. toString, watch, toSource, etc).
                    children[ ut.extract_path_end( this.path ) + "_item"] = this;
                });
                dir.data('totalResult', folder_spec.PagedItemList.childrenCount);
                dir.data('startIndex', folder_spec.PagedItemList.startIndex);
                update_dir(dir, children);
                if (scrollBottom)
                    dir.find('.mcol-direc-wrapper').attr({ scrollTop: dir.find('.mcol-direc-wrapper').attr("scrollHeight") });
            }
        }

        //positionClass is to know is the header if top or bottom position.
        function pagingHeader(dir, position){
            var startIndex = dir.data('startIndex');
            var totalResult = dir.data('totalResult');

            //Calculate the header Text
            var headerText = "";
            if (MAX_RESULTS >= totalResult){
                var nItems = totalResult - (startIndex-1);
                headerText = nItems + " item" +(nItems!==1? "s": "");
            }else{
                var endIndex = ((startIndex-1 + MAX_RESULTS > totalResult)? totalResult : (startIndex-1 + MAX_RESULTS));
                headerText = startIndex + " - " + endIndex + " of " + totalResult;
            }

            //Generate the HTML header
            var header = $('<div class="perc-paging-finder"/>')
                .data('name', position)
                .append($('<span class="perc-paging-text" />').text(headerText))
                .append(
                    $('<div class="perc-paging-finder-navigator" />')
                        .append($('<a class="perc-paging-finder-previous" />').text('<<').on("click",
                            function(evt){
                                pagePrevious(evt,$(this));
                            }))
                        .append($('<a class="perc-paging-finder-next"/>').text('>>').on("click",
                            function(evt){
                                pageNext(evt,$(this));
                            }))
                ).addClass(position);
            header = $("<div/>").append(header).append("<div style='clear:both'/>");
            //Enable/disable navigation buttons
            //Check if have next items
            if ((totalResult - (startIndex-1 + MAX_RESULTS)) > 0)
                header.find('.perc-paging-finder-next').removeClass('perc-hide-navigator');
            else
                header.find('.perc-paging-finder-next').addClass('perc-hide-navigator');

            //Check if have previous items
            if (startIndex > MAX_RESULTS)
                header.find('.perc-paging-finder-previous').removeClass('perc-hide-navigator');
            else
                header.find('.perc-paging-finder-previous').addClass('perc-hide-navigator');

            return header;
        }

        function pagingHeaderCountOnly(dir, position){
            var totalResult = dir.data('totalResult');
            var nItems = totalResult;
            var headerText = nItems + " item" +(nItems!==1? "s": "");
            var header = $('<div class="perc-paging-finder perc-paging-finder-top "/>')
                .data('name', position)
                .append($('<span class="perc-paging-text" />').text(headerText))
                .append('');
            return header;
        }

        function update_dir( dir, children) {
            //Given a current list of children, we go through the
            //directory's elements to determine whether any have
            //been added or removed.

            // since we are reusing the DOM elements clean up the class name
            dir.find('.mcol-direc-wrapper').removeClass('mcol-direc-wrapper-last');

            //Get the current elements, indexed by name.
            var curr_children = {};
            $.each( dir_children( dir ), function(){
                //Use the postfix "_item" to avoid reserved name collision (e.g. toString, watch, toSource, etc).
                curr_children[ $(this).data('name') + "_item" ] = $(this);
            });
            var dChildren = ut.odiff( children, curr_children );

            //odiff gets the set difference of two objects.
            $.each( ut.odiff( curr_children, children ), function() {
                //Listings which have been deleted - close
                //them if open, then remove them.
                if( this.is( '.mcol-opened' ) )
                    close_after( dir );

                //Make sure the resizable handlers are not removed on any refresh event
                if(!dragging && !this.hasClass('ui-resizable-e') && !this.parent().hasClass('perc-view-column-fixed') )
                    this.remove();
            });

            //Add top paging Header
            //Don't add headers in the first column.
            if(dir.data('path').join('/') !== "" && dir.find(".perc-paging-finder-top").length === 0) {
                if(assetPagination && ( (dir.data('path').indexOf("Assets") >-1) || (dir.data('path').indexOf("Sites") >-1)) ) {
                    dir_container(dir).prepend(pagingHeaderCountOnly(dir, "perc-paging-finder-top"));
                }else{
                    dir_container(dir).prepend(pagingHeader(dir, "perc-paging-finder-top"));
                }
            }
            $.each( dChildren, function() {
                if (!isLibMode || this.path !== "/Search/") {
                    add_item(dir, make_item(this, open_from_dir(dir)));
                }
            });

            var tabIndex = 20;
            $( "#perc-finder-table-top" ).find('a').each(function (i, el) {
                this.setAttribute("tabindex", tabIndex++);
            });




        }

        function open_next( next, dir, path, new_path, k ) {
            if( next.length === 0 ) {
                path_changed( new_path );
                finderOpenInProgress = false;
                //Set the path summary to the correct path.
                $("#mcol-path-summary").val( new_path.join('/') );
                //err( "Child \"" + path[0] + "\" does not exist" );
                var fwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-finder');
                if(fwrapper != null)
                    fwrapper.handleComponentProgress('perc-ui-component-finder', "complete");
            } else if ( next.is( '.mcol-opened' ) ) {
                //Element has already been opened - just continue
                //our open operation at the next directory.
                new_path.push( path.shift() );
                scroll_into_view(next);
                //adding aria disabled
                var viewMenuAnchor_child = next.children();
                for(var i=0; i<viewMenuAnchor_child.length; i++){
                    var child = viewMenuAnchor_child[i];
                    if(child.nodeName=="IMG"){
                        child.tabIndex="0";
                        child.setAttribute("aria-disabled", "true");
                        break;
                    }
                }
                //Expose the data item for multiple purposes.
                if (typeof(next.data('spec')) != "undefined"){
                    currentItem = next.data('spec');
                    $('.perc_last_selected').removeClass("perc_last_selected");
                    next.addClass("perc_last_selected");
                }

                _open( dir.next(), next.data( 'loader' ), path, new_path, k);
            } else {
                //Element is not currently opened - close what is
                //opened, then create a new directory and load it
                //using the current element's loader.
                close_after( dir );
                dir_children( dir ).filter('.mcol-opened').removeClass('mcol-opened');
                next.addClass('mcol-opened');

                scroll_into_view( next );
                //Expose the data item for multiple purposes.
                if (typeof(next.data('spec')) != "undefined"){
                    currentItem = next.data('spec');
                    $('.perc_last_selected').removeClass("perc_last_selected");
                    next.addClass("perc_last_selected");
                }

                var next_dir = insert_after( dir );
                new_path.push( path.shift() );
                _open( next_dir, next.data('loader'), path, new_path, k );
            }
            update_finder_height();
        }

        //Make the element which represents a single listing
        //in a directory.
        function make_item( spec, open_rel ) {

            var tabindex = 10;
            var pref = (spec.type === 'Folder') ? 'a' : 'z';
            var item_path = ut.extract_path( spec.path );
            var isSystemCategory = false;
            var icon;
            if(spec && spec.category && spec.category ==='SYSTEM' && spec.type && spec.type ==='FSFile' &&
                spec.name && spec.name.indexOf('.') !==-1){
                // customizing for case of category:system && it is a file type or image type.
                var ImageFileTypes = ['tif','jpg','jpeg','gif','png','tiff','jfif','jpe','bmp','dib'];
                var myFileType = spec.name.substr(spec.name.indexOf(".") + 1);
                if(ImageFileTypes.indexOf(myFileType) > -1){
                    icon = ut.choose_icon( 'FSIMAGEFile', spec.icon, item_path );
                }else{
                    icon = ut.choose_icon( 'FSFile', spec.icon, item_path );
                }
            }else{ //default workflow
                icon = ut.choose_icon( spec.type, spec.icon, item_path );
            }
            var listing = $("<a />").addClass('mcol-listing')
                .attr("alt",  spec.name )
                .attr('id', idFromItem(spec))
                //.attr('tabindex', tabindex)
                .append($("<img src='"+ icon.src +"' style='float:left' alt='"+ icon.alt + "' title='" + icon.title + "' aria-hidden='" + icon.decorative + "' />" ))
                .append($("<div class='perc-finder-item-name' style='cursor: default; text-overflow : ellipsis;overflow : hidden'>" + spec.name + "</div>" )).attr('title', spec.name)
                .data( 'tag', pref + (spec.name + "").toLowerCase() )
                .data( 'name', item_path[ item_path.length - 1 ] )
                .data( 'spec', spec );

            _addToPathIdArray(spec.path, spec.id);
            if(spec.type)
                listing.addClass("perc-listing-type-" + spec.type);
            if(spec.category)
                listing.addClass("perc-listing-category-" + spec.category);

            if( spec.leaf ) {
                listing.data( 'loader',
                    function(onLoad){
                        make_leaf_summary(spec, function(itemPropsContent){
                            onLoad( null, itemPropsContent );
                        });
                    });
            } else {
                listing.data( 'loader',
                    load_folder_path( ut.acop( item_path ) ) );
            }

            var clickCount = 0;
            listing.on("click", function(evt){
                clickCount++;
                if (clickCount === 1) {
                    singleClickTimer = setTimeout(function() {
                        clickCount = 0;
                        onClick(evt);
                    }, 400);
                } else if (clickCount === 2) {
                    clearTimeout(singleClickTimer);
                    clickCount = 0;
                    doubleClick(evt);
                }

            });

            listing.on("keydown", function(evt){
                if(evt.code == "Enter" || evt.code == "Space"){
                    document.activeElement.click();
                }

            });


            if(isDraggableItem(spec))
            {
                listing.draggable( {
                    helper: function() {
                        return $(this).clone()
                            .css(('overflow', 'visible'),('width', this.offsetWidth)[0]);

                    },
                    appendTo: 'body',
                    refreshPositions: true,
                    zIndex: 9990,
                    revert: true,
                    revertDuration: 0,
                    start: onDragStart,
                    stop: onDragStop,
                    containment: "window",
                    scope: $.perc_iframe_scope,
                    scroll: true,
                    iframeFix: true,
                    delay: dragDelay
                });

            }

            if(isDroppableItem(spec))
            {
                listing.droppable( {
                    tolerance: 'pointer',
                    accept: dragAcceptor,
                    over: hoverStart,
                    out: hoverCancel,
                    scope: $.perc_iframe_scope,
                    drop: onDrop } );
            }

            var hoverCount = 1, hover_time = 500;

            function hoverStart(event, ui){
                var startCount = hoverCount;
                var itemPath = ui.draggable.data('spec').path;
                var targetPath = spec.path;
                var targetType = spec.type;
                if(!canDrop(itemPath, targetPath, ui.draggable.data('spec').type, targetType))
                    return;
                if(spec.accessLevel !== $.PercFolderHelper().PERMISSION_READ &&
                    ui.draggable.data('spec').accessLevel !== $.PercFolderHelper().PERMISSION_READ)
                    $(this).addClass("perc-finder-item-over");
                if($(this).hasClass("perc-listing-type-site"))
                    return; // do not expand a site node

                setTimeout( function(event){
                        if( hoverCount === startCount ) {
                            onClick(event);
                        }
                    },
                    hover_time );
            }

            function hoverCancel(event, ui){
                $(this).removeClass("perc-finder-item-over");
                hoverCount++;
            }

            function doubleClick(evt){
                validatePath(evt, item_path, function(){
                    if(spec.type==="Folder" || item_path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH)
                    {
                        onClick(evt);
                    }
                    else
                    {
                        fireOpenEvent(spec);
                    }
                });
            }

            /**
             * onDrop action called when dropping an item on a eligible drop zone.
             * @param event {object} the event object passed by the fired event handler.
             * @param ui {object} the special ui object passed by the jQuery drop event.
             */
            function onDrop(event, ui){
                var itemPath = ui.draggable.data('spec').path;
                var itemType = ui.draggable.data('spec').type;
                var targetPath = spec.path;
                var targetType = spec.type;
                $(this).removeClass("perc-finder-item-over");
                hoverCancel();
                if(spec.accessLevel === $.PercFolderHelper().PERMISSION_READ ||
                    ui.draggable.data('spec').accessLevel === $.PercFolderHelper().PERMISSION_READ)
                    return false;
                if(!canDrop(itemPath, targetPath, itemType, targetType))
                    return false;
                $.PercPathService.moveItem(
                    itemPath,
                    targetPath,
                    function(status, data){
                        if(status === $.PercServiceUtils.STATUS_SUCCESS)
                        {
                            var type = null;

                            if (itemType === "percPage") {
                                type = "page";
                            }
                            else if (itemType === "Folder" && targetPath.indexOf("/Sites")===0) {
                                type = "folder";
                            }

                            refresh();
                        }
                        else
                        {
                            var content = data;
                            if (data.indexOf("item with the same name already exists in the folder") !== -1)
                            {
                                var itemLabel = "asset";
                                if (itemType === "percPage")
                                {
                                    itemLabel = "page";
                                }
                                else if (itemType === "Folder")
                                {
                                    itemLabel = "folder";
                                }
                                content = I18N.message( "perc.ui.finder.move.error@Duplicate", [itemLabel, ui.draggable.data('spec').name, targetPath] );
                            }
                            $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: content});
                        }
                    }
                );


            }

            /**
             * Helper function to determine if the drop is allowed.
             * This is similar to drag acceptor stuff, but these checks
             * should pass drag acceptor so expansion still occurs.
             * @param itemPath {string} the source item path.
             * @param targetPath {string} the target path.
             * @param itemtype {string} the source item type.
             * @param targetType {string} the target type.
             */
            function canDrop(itemPath, targetPath, itemtype, targetType)
            {
                if(targetType && (targetType !== 'Folder' && targetType !== 'site'))
                {
                    return false;
                }
                var tPath = (targetPath.match("\/$") === "/")?targetPath.substr(0, targetPath.length - 1): targetPath;
                if(tPath === (itemPath.substring(0, itemPath.lastIndexOf("/"))))
                    return false;
                if(itemtype === 'Folder')
                {
                    var sPath = itemPath.substr(1).split("/");
                    if(sPath[sPath.length - 1] === "")
                        sPath.pop();
                    tPath = targetPath.substr(1).split("/");
                    if(tPath[tPath.length - 1] === "")
                        tPath.pop();
                    var isSame = true;
                    for(var i = 0; i < sPath.length; i++)
                    {
                        if(sPath[i] !== tPath[i])
                        {
                            isSame = false;
                            break;
                        }
                    }
                    if(isSame)
                        return false;
                }
                return true;
            }

            /**
             * Determine if the passed in item should be made draggable.
             */
            function isDraggableItem(item){
                var type = item.type;
                var cat = item.category;

                if(!type)
                    return false;
                if(type === 'site')
                    return false;
                if(type === 'percPage' && cat === 'LANDING_PAGE')
                    return false;
                if(type === 'Folder' && cat ==='SECTION_FOLDER')
                    return false;
                if(type === 'Folder' && item.accessLevel !== $.PercFolderHelper().PERMISSION_ADMIN)
                    return false;
                if(type === 'FSFile')
                    return false;
                if(type === 'FSFolder')
                    return false;
                return true;

            }

            /**
             * Determine if the passed in item should be made droppable.
             */
            function isDroppableItem(item){
                var type = item.type;
                var cat = item.category;
                if(!type && item.path === $.perc_paths.ASSETS_ROOT + "/")
                    return true;
                if(!type)
                    return false;
                if(type === 'percPage')
                    return false;
                return true;
            }

            /**
             * Decides if a dragged item will be accepted by the target drop element.
             * @param item {object} the item being dragged.
             */
            function dragAcceptor(item) {
                var their_path = item && item.data('spec') && item.data('spec').path;
                if( !their_path )
                    return false;

                var our_path = spec.path;
                // Do not allow a site item to be moved into a different
                // site
                if(spec.type && spec.type == 'site')
                {
                    var site1 = their_path.substr(1).split("/")[1];
                    var site2 = our_path.substr(1).split("/")[1];
                    if(site1 !== site2)
                        return false;
                }
                // Do not allow dropping asset into a non folder
                if(item.data('spec').category && item.data('spec').category === 'ASSET')
                {
                    if(!spec.type || spec.type !== 'Folder')
                        return false;
                }

                if( our_path.length >= their_path.length &&
                    $.grep( their_path, function(c, ii) { return c === our_path[ii]; } ).length === 0 ) {
                    //their path is a subset of our path - don't allow
                    //item to be dragged into itself or its children
                    return false;
                }

                var their_base = ut.extract_path( their_path )[1];
                var our_base = ut.extract_path( our_path )[1];
                if( their_base !== our_base ) {
                    return false;
                }

                return true;
            }

            return listing;
            function onClick(evt){
                if(evt) {
                    if (evt.currentTarget && evt.currentTarget.id === "perc-finder-listing-Search") {
                        $.Percussion.setView("search");

                        //Set the Search icon for when highlighted
                        $("#perc-finder-listing-Search").find("img").attr("src", "/cm/images/images/searchIcon_on.png");

                        return;
                    }
                    // evt.stopPropagation();
                }

                //Set the Search icon for when highlighted
                $("#perc-finder-listing-Search").find("img").attr("src", "/cm/images/images/searchIcon.png");

                $("#perc-finder-choose-listview").removeClass('ui-state-disabled');
                $("#perc-finder-choose-columnview").removeClass('ui-state-disabled');
                var $evtTarget = $(this);
                var $itemNameEl = $evtTarget.children(".perc-finder-item-name");
                var $inputField = $itemNameEl.find("#perc_finder_inline_field_edit"); //local to event target
                var len = $inputField.length;
                if(len === 0)
                {
                    var $editField = $("#perc_finder_inline_field_edit");
                    if($editField.length > 0)
                    {
                        lastClickPath = item_path;
                        $editField.trigger("blur");
                    }
                    $evtTarget.trigger("focus");

                    // Add a class to the last selected item and remove it from other items.
                    //$('.perc_last_selected').removeClass("perc_last_selected");
                    //$(this).addClass("perc_last_selected");
                    if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR) {
                        var viewWrapper = $.PercComponentWrapper("perc-action-finder-item-clicked", ["perc-ui-component-finder"]);
                        var isWrapperSet = $.PercViewReadyManager.setWrapper(viewWrapper);
                        if (!isWrapperSet) {
                            if (!isWrapperSet) {
                                $.PercViewReadyManager.showRenderingProgressWarning();
                                return;
                            }
                        }
                    }
                    open_rel( listing, ut.acop( item_path ));
                }
            }
        }

        /**
         * Executed when dragging starts for one or more finder items.
         */
        function onDragStart()
        {
            var $finderTable = $(".perc-finder-table");
            var props = {};
            props.left= $finderTable.offset().left;
            props.top = $finderTable.offset().top;
            props.width = $finderTable.width();
            props.height = $finderTable.height();
            dragging = true;
            // Add a mouse move listener to body only when dragging a finder item.
            $("body")
                .css("overflow", "visible")
                .on("mousemove.finderDrag", props, function(evt){
                    /* Determine if we are dragging within the finder table.
                       If we are not then disable all droppables within the finder else
                       enable them.
                    */
                    var right = evt.data.left + evt.data.width;
                    var bottom = evt.data.top + evt.data.height;
                    var inX = (evt.pageX >= evt.data.left) && (evt.pageX <= right);
                    var inY  = (evt.pageY >= evt.data.top) && (evt.pageY <= bottom);
                    enableDisableFinderDroppables(inY && inX);

                });


        }

        /**
         * Executed when dragging stops on the currently dragged finder items.
         */
        function onDragStop()
        {
            // Remove the mouse move listener and re-enable finder droppables
            dragging = false;
            $("body")
                .css("overflow", "hidden")
                .off("mousemove.finderDrag");
            enableDisableFinderDroppables(true);
        }
        /**
         * Creates the summary of the item properties and calls the call the callback function with the html content.
         * @param path, item path assumed not null or empty.
         * @param callback, the callback function assumed not null.
         */
        function make_leaf_summary( spec, callback ) {
            var summary = " ";
            $.perc_pathmanager.getItemProperties(spec.path, function(status, itemProps){
                if(status)
                {
                    var nameType = null;
                    var type = null;
                    var linkTag = null;
                    var isAssetResource = spec.category==="ASSET" || spec.category === "RESOURCE";
                    if(isAssetResource)
                    {
                        //Asset/Type
                        nameType = "Asset";
                        type = "Type";
                        if(!spec.path.includes("Recycling")){
                            linkTag = "<a href='#' class='perc-finder-preview-link' id='perc-asset-preview-link' title='Click for preview'>";
                        }else{
                            linkTag = "";
                        }

                    }
                    else
                    {
                        //Page/Template
                        nameType = "Page Link";
                        type = "Template";
                        if(!spec.path.includes("Recycling")){
                            linkTag = "<a href='#' class='perc-finder-preview-link' id='perc-page-preview-link' title='Click for preview'>";
                        }else{
                            linkTag = "";
                        }

                    }
                    var lpdate = itemProps.lastPublishedDate;
                    if (typeof lpdate === "undefined" || lpdate === null || lpdate.trim() === '')
                    {
                        lpdate = '';
                    }
                    else
                    {
                        var lastPublishedDateParts = $.perc_utils.splitDateTime(lpdate);
                        lpdate = '<div style="padding:9px 0 0 10px;">Last Published: <span></span></div>' +
                            '<div style="padding:3px 0 0 10px;"><span "perc_finder_details_lpdate">' + lastPublishedDateParts.date + " " + lastPublishedDateParts.time  + '</span></div>';
                    }

                    var lastModifiedDateParts = $.perc_utils.splitDateTime(itemProps.lastModifiedDate);

                    if (spec.path.split("/")[1] === $.perc_paths.DESIGN_ROOT_NO_SLASH)
                    {
                        var fileSize = $.perc_utils.formatFileSize(itemProps.size);
                        summary = '<div style="padding:10px 0 0 10px;">' + "Properties" + ': <span id="perc_finder_details_name">' + itemProps.name + '</span></div>' +
                            '<div style="padding:10px 0 0 10px;">' + "Size" + ': <span id="perc_finder_details_size">' + fileSize + '</span></div>' +
                            '<div style="padding:10px 0 0 10px;">Last Modified: <span></span></div>' +
                            '<div style="padding:3px 0 0 10px;"><span id="perc_finder_details_lmdate">' + lastModifiedDateParts.date + " " + lastModifiedDateParts.time  + '</span></div>';
                    }
                    else
                    {
                        summary = '<div style="padding:9px 0 0 10px;">' + nameType + ': <span id="perc_finder_details_name">' + linkTag + itemProps.name + '</a></span></div>' +
                            '<div style="padding:9px 0 0 10px;">' + type + ': <span id="perc_finder_details_type">' + itemProps.type + '</span></div>' +
                            '<div style="padding:9px 0 0 10px; width:170px;">Status: <div id="perc_finder_details_status" status="' + itemProps.status + '" workflow="' + itemProps.workflow + '" class="perc-ellipsis" title="' + itemProps.status + " (" + itemProps.workflow + ')">' + itemProps.status + " (" + itemProps.workflow + ")" + '</div></div>' +
                            '<div style="padding:9px 0 0 10px;">Last Modified: <span id="perc_finder_details_lmuser">' + itemProps.lastModifier + '</span></div>' +
                            '<div style="padding:3px 0 0 10px;"><span id="perc_finder_details_lmdate">' + lastModifiedDateParts.date + " " + lastModifiedDateParts.time  + '</span></div>' +
                            lpdate;
                    }
                    var $sum = $(summary);
                    if(isAssetResource)
                    {
                        $sum.find("#perc-asset-preview-link").each(function(){
                            $(this).off().on('click', function(){
                                launchAssetPreview(spec.id);
                            });
                        });
                    }
                    else
                    {
                        $sum.find("#perc-page-preview-link").each(function(){
                            $(this).off().on('click', function(){
                                launchPagePreview(spec.id);
                            });
                        });
                    }
                    callback($sum);
                }
                else
                {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: itemProps});
                    refresh();
                }
                var fwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-finder');
                if(fwrapper != null)
                    fwrapper.handleComponentProgress('perc-ui-component-finder', "complete");

            });
        }

        /**
         * Launch the asset preview for the specified asset.
         * @param id {string} the asset id, cannot be <code>null</code> or
         * empty.
         */
        function launchAssetPreview(id, revId){
            if(revId)
            {
                var ida = id.split("-");
                ida[0] = revId;
                id = ida.join("-");
            }
            $.PercAssetService.getAssetViewForAssetId(id, function(status, result){
                if(status == $.PercServiceUtils.STATUS_SUCCESS)
                {
                    var nRef = window.open(result, "percAssetPreviewWindow" + id.replace(/-/g, ""));
                    $(nRef.document).ready(function(){
                        if(revId)
                        {
                            window.setTimeout(function(){
                                nRef.document.title = nRef.document.title + I18N.message("perc.ui.finder@Revision") + revId + ")";
                            }, 1000); // There needs to be a delay for title to be ready
                        }
                        nRef.focus();
                    });
                }
                else
                {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result});
                }
            });
        }

        /**
         * Launch the page preview for the specified page.
         * @param id {string} the page id, cannot be <code>null</code> or
         * empty.
         */
        function launchPagePreview(id, revId){
            // Retrieve the path for the given page id to build the friendly URL and open hte preview
            $.PercPathService.getPathItemById(id, function(status, result, errorCode) {
                if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                    var href = result.PathItem.folderPaths + "/" + result.PathItem.name;
                    var mobilePreview = result.PathItem.mobilePreviewEnabled;
                    if(typeof mobilePreview === "undefined" || mobilePreview === null){
                        mobilePreview = false;
                    }
                    href = href.substring(1);

                    if(revId)
                    {
                        href += "?sys_revision=" + revId + "&percmobilepreview="+mobilePreview;
                    }
                    else{
                        href += "?percmobilepreview="+mobilePreview;
                    }

                    // IE doesn't accept dashes '-' as part of the window name.
                    // The 2nd param needs to be "" and not null because IE will not show
                    // any bars when null. Both IE and FF show the same header in the new
                    // window as the original by passing "" and follow the user's preference
                    // as whether to open in a tab or window.
                    var nRef = window.open(href, "percPagePreviewWindow" + id.replace(/-/g, ""));
                    $(nRef.document).ready(function() {
                        if(revId) {
                            window.setTimeout(function() {
                                nRef.document.title = nRef.document.title + I18N.message("perc.ui.finder@Revision") + revId + ")";
                            }, 1000); // There needs to be a delay for title to be ready
                        }
                        nRef.focus();
                    });
                }
                else {
                    // We failed retrieving the friendly URL. Show the error dialog
                    $.unblockUI();

                    var msg = "";
                    if (errorCode == "cannot.find.item")
                    {
                        msg = I18N.message( 'perc.ui.common.error@Preview Content Deleted' );
                    }
                    else
                    {
                        msg = result;
                    }

                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: msg});
                }
            });
        }

        function launchPageCompareView(itemId,itemName,selectedRev,latestRev,allRevisions){
            _percCompareService = $.PercCompareService();
			_percCompareService.params.itemId = itemId;
            _percCompareService.params.title = itemName;
            _percCompareService.params.revision1 = Number(selectedRev);
            _percCompareService.params.revision2= Number(latestRev);
            _percCompareService.params.allRevisions=allRevisions;
            _percCompareService.openComparisonWindow();
        }


        /**
         * Launch the page preview for the specified page.
         * @param path{string} the page path, cannot be <code>null</code> or
         * empty.
         * @param id {string} the page id, cannot be <code>null</code> or
         * empty.
         */
        function launchPagePreviewByPath(path,id,revId){
            // Retrieve the path for the given page id to build the friendly URL and open hte preview
            $.PercPathService.getPathItemForPath(path, function(status, result) {
                if(status == $.PercServiceUtils.STATUS_SUCCESS) {
                    //var href = result.PathItem.folderPaths.slice(1) + "/" + result.PathItem.name;

                    var href = result.PathItem.folderPaths + "/" + result.PathItem.name;
                    href = href.substring(1);
                    var mobilePreview = result.PathItem.mobilePreviewEnabled;
                    if(typeof mobilePreview === "undefined" || mobilePreview === null){
                        mobilePreview = false;
                    }

                    if(revId)
                    {
                        href += "?sys_revision=" + revId + "&percmobilepreview="+mobilePreview;
                    }
                    else{
                        href += "?percmobilepreview="+mobilePreview;
                    }

                    // IE doesn't accept dashes '-' as part of the window name.
                    // The 2nd param needs to be "" and not null because IE will not show
                    // any bars when null. Both IE and FF show the same header in the new
                    // window as the original by passing "" and follow the user's preference
                    // as whether to open in a tab or window.
                    var nRef = window.open(href, "percPagePreviewWindow" + id.replace(/-/g, ""));
                    $(nRef.document).ready(function() {
                        if(revId) {
                            window.setTimeout(function() {
                                nRef.document.title = nRef.document.title + I18N.message("perc.ui.finder@Revision") + revId + ")";
                            }, 1000); // There needs to be a delay for title to be ready
                        }
                        nRef.focus();
                    });
                }
                else {
                    // We failed retrieving the friendly URL. Show the error dialog
                    $.unblockUI();
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: result});
                }
            });
        }

        //Load a list of children from a given folder path
        function load_folder_path( path ) {
            return function(k, dir){
                // startIndex always will be 1 because this functions is called when the user do a click on a folder
                //Generate the url with startIndex and maxResult.
                var str_path = $.perc_utils.encodeURL(path.join("/")) + "/?startIndex=1" + "&maxResults=" + MAX_RESULTS;

                if(assetPagination && ((path.indexOf("Assets") >-1 )|| (path.indexOf("Sites") >-1))){
                    str_path = $.perc_utils.encodeURL(path.join("/"));
                }
                //check if we need to find a specific page for the next item and add the child in the url
                //if the child element doesn't exist the server returned the first page of the current folder
                if (!assetPagination && typeof(dir.data('child')) !== "undefined" && dir.data('child') !== ""){

                    if(str_path.indexOf("?") >-1){
                        str_path += "&child=" + $.perc_utils.encodeURL(dir.data('child'));
                    }else{
                        str_path +="/?child=" + $.perc_utils.encodeURL(dir.data('child'));
                    }
                    dir.data('child', ""); //clean the next child element.
                }

                $.perc_pathmanager.open_path( str_path, true, getChildren, err, true );
                function getChildren( folder_spec ) {
                    var children = {};
                    $.each( $.perc_utils.convertCXFArray(folder_spec.PagedItemList.childrenInPage),
                        function() {
                            //Use the postfix "_item" to avoid reserved name collision (e.g. toString, watch, toSource, etc).
                            children[ ut.extract_path_end( this.path ) + "_item"] = this;
                        });
                    dir.data('path', path);
                    //set the startIndex of the child element, if was not provided the child param the service return the original startindex
                    dir.data('startIndex', folder_spec.PagedItemList.startIndex);
                    dir.data('totalResult', folder_spec.PagedItemList.childrenCount);
                    k( children);
                }
            };
        }

        function add_item( dir, item ) {
            //Insertion sort by tag - start with item at the end,
            //then insert it before the first element with a greater
            //tag (if this element has the max. tag, it will remain
            //at the end).
            dir_container( dir ).append( item );
            /*
            dir_children( dir ).each( function() {
                    if( $(this).data( 'tag' ) > item.data( 'tag' ) ) {
                        $(this).before( item );
                        return false;
                    }
                });*/
        }


        function scroll_into_view( listing ) {
            //Scroll the listing into view, if it is not already.
            var par = listing.closest( '.mcol-direc-wrapper' );
            var yoff = listing.position().top + listing.outerHeight();
            var height = par.closest('td').height();
            if( yoff < 0 || yoff > height ) {
                par.animate( { scrollTop : yoff - height}, 200 );
            }
        }

        /**
         * Helper function to enable or disable list view button and finder action buttons when navigating design node.
         * @param flag(boolean) if <code>true</code> the buttons are disabled, otherwise the buttons are enabled.
         */
        function setStateButtonsDesignNode(disable)
        {
            if (disable)
            {
                $.percFinderButtons().disableAllButtonsButSite();
            }
        }

        /**
         * trivial utility functions.
         */
        function insert_after( dir ) {
            var newdir = new_dir();

            $("#perc-finder-table-top").append(newdir);
            return newdir;
        }

        function close_after( dir ) {
            if( dragging ) {
                //If we are dragging an element, it needs to remain in the DOM.
                dir.nextAll().hide();
            } else {
                dir.nextAll().remove();
            }
        }

        function new_dir (resizable) {
            // default to true unless explicitly told not to resize
            resizable = resizable !== false;
            var td = $('<td tabindex="-1" class="mcol-direc" />');
            var resize = $('<div class="perc-resize ui-resizable" />');
            var content = $('<div class="perc-resize-width mcol-direc-wrapper" />');
            function onresize (event, ui) {
                content.css('width', resize.width() - content.siblings().outerWidth());
            }
            function onstart (event, ui) {
                $('.perc-finder').addClass('ui-resizable-resizing');
            }
            function onstop (event, ui) {
                $('.perc-finder').removeClass('ui-resizable-resizing');
            }
            if (resizable) {
                resize.resizable({handles: 'e', resize: onresize, start: onstart, stop: onstop});
            }
            return td.append(resize.append(content));
        }

        function fn_first_dir () {
            var resizable = false, fdir;
            fdir = new_dir(resizable);
            fdir.find('.mcol-direc-wrapper').addClass('perc-view-column-fixed');
            fdir.find('.ui-resizable').append('<div class="ui-resizable-handle ui-resizable-e perc-resize-disabled" />');
            return fdir;
        }

        function make_top_level( dir ) {
            var dv = $("<div class='perc-finder-table'>"+
                "<table><tr id='perc-finder-table-top'></tr></table></div>");
            dv.find('tr').append( dir );
            return dv;
        }

        function dir_children( dir ) {
            return dir_container(dir).children();
        }

        function dir_container( dir ) {
            return dir.find('.mcol-direc-wrapper');
        }

        /**
         * Enables or disables the finder droppables.
         * @param enable {boolean} flag indicating an enable operation
         * if <code>true</code>
         */
        function enableDisableFinderDroppables(enable){

            $(".mcol-listing").each(function(){
                $(this).droppable();
                $(this).droppable("option", "disabled", !enable);

            });
            $("#perc-finder-listview table").droppable("option", "disabled", !enable);
        }

        /**
         * Retreives the site name from the current path in search bar
         * i.e. /Sites/mysite.com/page1.html will return 'mysite.com'
         * @param path {string} the full path currently displayed in the finder search bar in CM1
         * @return siteName the name of the site
         */
        function getSiteNameByPath(path) {
            if (!path || typeof (path) != 'string' || path.length < 1)
            {
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.finder@Nonvalid String")});
                return;
            }
            var index = path.indexOf('/', 1);
            var index2 = path.indexOf('/', index + 2);

            if(index2 !== -1) {
                var siteName = path.substring((index + 1), index2);
                return siteName;
            }
            return  path.substring(index + 1);
        }

        /**
         * Gets the PathItem corresponding to the given path, may be <code>null</code> if the object corresponding to the
         * path has never been expanded in the finder.
         *
         * @param path(String)
         *            must not be empty.
         * @return PathItem (@see PSPathItem for the structure.)
         *
         */
        function getPathItemByPath(path)
        {
            if (!path || typeof (path) != 'string' || path.length < 1)
            {
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.finder@Nonvalid String")});
                return;
            }
            var pItem = null;
            var objectId = null;
            $.each(_finderPathIdArray, function(key, value){
                if (key == path) {
                    objectId = value;
                    return;
                }else if (key.endsWith("/") && !path.endsWith("/")) {
                    altPath = path + "/";
                    if(key == altPath)
                    {
                        objectId = value;
                        return;
                    }
                }
            });
            if(objectId){
                pItem = getPathItemById(objectId.toString());
            }
            return pItem;
        }

        /**
         * Gets the PathItem corresponding to the given id, may be <code>null</code> if the object corresponding to the
         * id has never been expanded in the finder.
         *
         * @param objectId(String)
         *            must not be empty.
         * @return PathItem (@see PSPathItem for the structure.)
         *
         */
        function getPathItemById(objectId)
        {
            if (!objectId || typeof (objectId) != 'string' || objectId.length < 1)
            {
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.finder@Object ID Nonvalid String")});
                return;
            }
            var pItem = $("#" + FINDER_LISTING_ID_PREFIX + objectId).data('spec');

            return pItem;
        }

        /**
         * Gets the parent PathItem corresponding to the given path, may be <code>null</code> if the object corresponding to the
         * path has never been expanded in the finder.
         *
         * @param path(String)
         *            must not be empty.
         * @return PathItem (@see PSPathItem for the structure.)
         *
         */
        function getParentPathItemByPath(path)
        {
            if (!path || typeof (path) != 'string' || path.length < 1)
            {
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.finder@Path Nonvalid String")});
                return;
            }
            if(path.charAt(path.length-1)=="/")
                path = path.substring(0,path.length-1);
            path = path.substring(0,path.lastIndexOf("/"));
            return getPathItemByPath(path);
        }

        function addPathChangedListener( new_listener ) {
            (function( old_listener ) {
                path_changed = function(path){ old_listener(path); new_listener(path); };
            })(path_changed);
        }

        function executePathChangedListeners(path) {
            path_changed(path);
        }

        /**
         * Adds an open listener to the finder to be notified when an object
         * was requested to be opened (i.e. double clicked).
         * @param listener {Function} the open listener callback function that
         * will be called when an open event occurs. Cannot be <code>null</code>.
         */
        function addOpenListener(listener)
        {
            if($.inArray(listener, openListeners) == -1)
            {
                openListeners.push(listener);
            }
        }

        /**
         * Removes the specified open listener if it exists.
         * @param listener {Function} the open listener callback function to be
         * removed. Cannot be <code>null</code>.
         */
        function removeOpenListener(listener)
        {
            if($.inArray(listener, openListeners) > -1)
            {
                var len = openListeners.length;
                for(var i = 0; i < len; i++)
                {
                    if(openListeners[i] === listener)
                    {
                        openListeners.splice(i, 1);
                        return;
                    }
                }
            }
        }

        /**
         * Fires open event informing all registered open listeners.
         * @param info
         */
        function fireOpenEvent(info)
        {
            var len = openListeners.length;
            for(var i = 0; i < len; i++)
            {
                openListeners[i](info);
            }
        }


        /**
         * Adds an action listener to the finder to be notified when a finder
         * action occurs.
         * @param listener {Function} the action listener callback function that
         * will be called when an action event occurs. Cannot be <code>null</code>.
         */
        function addActionListener(listener)
        {
            if($.inArray(listener, actionListeners) == -1)
            {
                actionListeners.push(listener);
            }
        }

        /**
         * Removes the specified action listener if it exists.
         * @param listener {Function} the action listener callback function to be
         * removed. Cannot be <code>null</code>.
         */
        function removeActionListener(listener)
        {
            if($.inArray(listener, actionListeners) > -1)
            {
                var len = actionListeners.length;
                for(var i = 0; i < len; i++)
                {
                    if(actionListeners[i] === listener)
                    {
                        actionListeners.splice(i, 1);
                        return;
                    }
                }
            }
        }

        /**
         * Fires action event informing all registered action listeners.
         * @param action {string} the action type.
         * @param data {object} any extra data needed about the fired action. May
         * be <code>null</code>.
         */
        function fireActionEvent(action, data)
        {
            var len = actionListeners.length;
            for(var i = 0; i < len; i++)
            {
                actionListeners[i](action,
                    typeof(data) == 'object' ? data : null);
            }
        }

    }

    $.perc_finderInstance = null;

})(jQuery);

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
 * Top-level functions for Web Management page.
 */

(function($)
{
    $.percFinderButtons = function()
    {
        // singleton to keep track of dirty state across various types of resources such as pages, templates and assets
        var dirtyController = $.PercDirtyController;
        
        var finderButtons = {
            createButtons : createButtons,
            disableAllButtonsButSite : disableAllButtonsButSite 
        };
        return finderButtons;
        
        /**
         * Disables all the action buttons in the finder leaving the Create Site button as it is (enabled)
         */
        function disableAllButtonsButSite()
        {
            $( ".perc-finder-menu #perc-finder-preview" ).addClass('ui-disabled').removeClass('ui-enabled').off('click'); //Disabled
            $( ".perc-finder-menu #perc-finder-actions-button" ).addClass('ui-disabled').removeClass('ui-enabled').off('click'); //Disabled
            $( ".perc-finder-menu #perc-finder-delete" ).removeClass('ui-enabled').addClass('ui-disabled').off('click'); //Disabled
            $( ".perc-finder-menu #perc-finder-new-folder" ).addClass('ui-disabled').removeClass('ui-enabled').off('click'); //Disabled
            $( ".perc-finder-menu #mcol-new-page" ).addClass('ui-disabled').removeClass('ui-enabled').off('click'); //Disabled
            $( ".perc-finder-menu #mcol-new-asset" ).addClass('ui-disabled').removeClass('ui-enabled').off('click'); //Disabled
        }
        
        // this is called by PercPageView.js
        // creates buttons at the top right of the page Editor
        // adds them to .perc-finder-menu
        // each button invokes a handler when clicked
        // handlers get passed the finder and the contentviewer passed in from PercPageView
        function createButtons(finder, contentViewer)
        {
            // create the delete page button and its handler
            var dp = $.perc_build_delete_page_button( finder, contentViewer );
            var percButtons = $('<div class="perc-finder-buttonbar"/>');

            percButtons.append( dp );
            var np;
            var na;
            var isLibMode = typeof gInitialScreen !== 'undefined' && gInitialScreen === "library";
            if (!isLibMode) {
                // create the new page button and its handler
                 np = $.perc_build_new_page_button(finder, contentViewer);
                percButtons.append(np);
                
                // create the new asset button and its handler
                 na = $.PercNewAssetDialog.init(finder, contentViewer);
                percButtons.append(na);
            }
            // create the new folder button and its handler
            var nf = $.perc_build_new_folder_button( finder, contentViewer );
            percButtons.append( nf );

            //Adding new site event
            // create new site button and its handler {
            var ns;
            if (!isLibMode) {
                 ns = '<a id="perc-finder-new-site" class="perc-form ui-state-default perc-font-icon icon-sitemap fas fa-sitemap" href="#" title="' + I18N.message("perc.ui.finder.buttons@Click New Site") + '"></a>';
                percButtons.append(ns);
            }

            var lp = $.perc_build_preview_button( finder, contentViewer );
            percButtons.append( lp );
            
            // create the actions button
            var ab = $.perc_build_actions_button( finder, contentViewer);
            percButtons.append( ab );
            
           $(".perc-finder-menu ").append(percButtons);

		    var tabIndex = 19;
			$( ".perc-finder-menu" ).find('a').each(function (i, el) {
					this.setAttribute("tabindex", tabIndex--);
			});

            function onSuccessCallBackHandler(sitename)
            {
              $.PercNavigationManager.goToLocation(
                 $.PercNavigationManager.VIEW_DESIGN, sitename, null, null, null,
                 $.perc_paths.SITES_ROOT + "/" + sitename , null, null);
            }

            $newSiteDialog = $.perc_createNewSiteDialog(onSuccessCallBackHandler);

            $('#perc-finder-new-site').off('click').on("click",
                function(evt){
                    createFn(evt);
                });
            
            finder.addPathChangedListener( update_newsite_btn );

            var finderButtons = {
                "delete" : dp,
                "newPage" : np,
                "newFolder" : nf,
                "newAsset" : na,
                "newSite" : ns,
                "launchPreview" : lp,
                "launchAction" : ab
            };
            
            // return the list of buttons to PercPageView
            return finderButtons;
        }

        /**
         * Create new site function.
         */
        function createFn(evt) {
            // check to see if dirty before allowing creating a new site
            // show confirm dialog if dirty
            dirtyController.confirmIfDirty(function(){
            $newSiteDialog.perc_wizard('open');
            //remove the unwanted stupid z-index values
            $(".ui-dialog.ui-widget.ui-widget-content.ui-corner-all.perc-dialog.perc-dialog-corner-all.ui-draggable").find('div').css('z-index', '');
            });
        }

        /**
         * Update the new site button state based on the current user.
         * The button will only be enabled for Admin users.
         */
        function update_newsite_btn() {
            if(!$.PercNavigationManager.isAdmin()){
                $( ".perc-finder-menu #perc-finder-new-site" ).removeClass('ui-enabled').addClass('ui-disabled').off('click');
            }
            else{
                if(!$.PercNavigationManager.getPath().startsWith($.perc_paths.RECYCLING_ROOT)){
                    $( ".perc-finder-menu #perc-finder-new-site" ).removeClass('ui-disabled').addClass('ui-enabled').off('click').on('click',
                        function(evt){
                            createFn(evt);
                        } );
                }else{
                    $( ".perc-finder-menu #perc-finder-new-site" ).removeClass('ui-enabled').addClass('ui-disabled').off('click');
                }
            }
        }
    };
})(jQuery);

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
 * perc_upload_theme_file_dialog
 *
 * Show a dialog to upload theme files.
 */
(function($){
    /**
     * Public API
     */
    $.perc_upload_theme_file_dialog = {
            open: openUploadDialog
    };
    
    /**
     * Constructs the dialog
     * @param finder The finder object.
     */
    function openUploadDialog(finder)
    {
        // The current finder path could be ending with a file, and we don't want that
        var path = initializePath();
        
        // Build an absolute URL as a workaround for IE issue (popup security warning)
        // See details at http://support.microsoft.com/kb/925014/en-us?fr=1
        var baseUrl = window.location.protocol + "//" + window.location.host + "/cm";
        
        var buttons = {};
        buttonSave = {
                id : "perc-addItem-dialog-save"
        };
        buttonCancel =   {
                id : "perc-addItem-dialog-cancel",
                click : function() {
                    dialog.remove();
                }
        };
        buttons.Save = buttonSave;
        buttons.Cancel = buttonCancel;
        
        var dialog = createDialog();
        
        
        /**
         * Builds the dialog (and its form) invoking perc_dialog()
         * @returns jQuery element wrapping the dialog and form created with perc_dialog()
         */
        function createDialog()
        {
            var actionUrl = $.perc_paths.WEBRESOURCESMGT_FILE_UPLOAD;
            // Basic dialog content HTML markup
            var dialogContent = "<div id='perc-design-file-upload'>" +
                     '<form id="perc-theme-file-upload-form" name="perc-theme-file-upload-form" enctype="multipart/form-data" method="post" action="' + actionUrl +'">' +
//                    + '<span class="perc-required-legend"><label>* - ' + I18N.message("perc.ui.uploadtheme.form.text@denotes required field") + '</label></span>'
//                        + '<label class="perc-required-field" for="upload-theme-file-attachment" accesskey="F"><u>F</u>ile name:</label><br>'
                     '<label for="upload-theme-file-attachment">File name:</label><br>' +
                         '<input type="hidden" name="upload-theme-file-path" />' +
                         '<input type="file" size="50" name="upload-theme-file-attachment" />' +
                     '</form>' +
                    // hidden div that will hold the server response
                     '<div id="perc-theme-file-upload-response" style="display:none"></div>' +
                 "</div>";
            
            // Create the upload dialog
            var d = $(dialogContent).perc_dialog( {
                title: I18N.message( "perc.ui.uploadtheme.dialog.title@Upload File"),
                id: "perc-upload-theme-file-dialog",
                width: 686,
                resizable : false,
                modal: true,
                closeOnEscape : true,
                percButtons: buttons,
                open: function() {
                    // Initialize the Save button, biding its submit behavior and styling
                    initializeSaveButton();
                    
                    // Initialize the jQuery form plugin
                    $('#perc-theme-file-upload-form').ajaxForm({
                        // Element that will hold the server response after submitting the
                        target: '#perc-theme-file-upload-response',
                        // Function that must be triggered to close the dialog on success, or show an error dialog
                        success: handleResponse,
                        // Function to be called after an error occured (specially for timeouts, as the file upload
                        // service returns 200 always
                        error: handleError,
                        // Since we are submitting a file upload form, we must use an iframe as a target of the
                        // form. The jQuery form plugin will handle it automatically
                        iframe: true
                    }); 
                }
            });
            return $(d);
        }

        /**
         * Styles the Save button and binds the save logic to the button Save.
         */
        function initializeSaveButton()
        {
            // Build an absolute URL as a workaround for IE issue (popup security warning)
            // See details at http://support.microsoft.com/kb/925014/en-us?fr=1
            var baseUrl = window.location.protocol + "//" + window.location.host + "/cm";
            var buttonSave = $("#perc-addItem-dialog-save");
            
            buttonSave
                .off('click')
                // Bind function to the click event in the button
                .on("click",function(evt){
                    saveLogic(evt);
                });
        }
        
        /**
         * Logic involved after clicking the Save button
         */
        function saveLogic(event)
        {   
            if (checkFileFieldCompleted() === false)
            {
                return false;
            }
            checkElementWithSameNameOrUpload();
            return false;
        }
              
        /**
         * Checks that the field file was completed after clicking the Save button.
         * If not, it shows an error label below the field.
         * @return boolean true if the input file filed has been completed
         */
        function checkFileFieldCompleted()
        {
            var fileField = dialog.find('input:file');
            var errorLabel = dialog.find('label.perc_field_error');
            if (fileField.val() === '')
            {
                // Show the error message, if not shown previously
                if (errorLabel.length === 0)
                {
                    fileField.after('<label class="perc_field_error" for="upload-theme-file-attachment" style="display: block;">' + I18N.message("perc.ui.uploadtheme.form.text@Please select a file.") + '</label>');
                }
                return false;
            }
            else
            {
                errorLabel.remove();
                return true;
            }
        }
        
        /**
         * Checks if a file with the same name of the exists in the current finder Path (under Design node).
         */
        function checkElementWithSameNameOrUpload()
        {
            // There are differences with IE, FF and Chrome when getting the value from file input field
            var fileName = dialog.find('input:file').val().replace(/.+[\\\/]/, "");
            
            $.PercWebResourcesService.validateFileUpload(path, fileName, function(status, result)
                {
                    if (status === $.PercServiceUtils.STATUS_SUCCESS)
                    {
                        if (result.data === $.PercServiceUtils.STATUS_SUCCESS)
                        {
                            // No element with same name found in the path, proceed with the upload
                            uploadFile();
                        }
                        else
                        {
                            // An element with the same name has been found, confirm overwrite
                            var options = {
                                    id       : "perc-design-file-upload-file-exist-warning",
                                    title    : I18N.message( "perc.ui.uploadtheme.dialog.title@Warning"),
                                    question : result.data,
                                    yes      : "OK",
                                    // The user chose to overwrite the file
                                    success  : uploadFile 
                            };
                            $.perc_utils.confirm_dialog(options);
                        }
                    }
                    else
                    {
                        // There is a folder with the same name, this is an erroneous situation and that is why
                        var options = {
                                id      : "perc-design-file-upload-file-exist-error",
                                title   : I18N.message( "perc.ui.uploadtheme.dialog.title@Error"),
                                content : $.PercServiceUtils.extractDefaultErrorMessage(result.request)
                        };
                        $.perc_utils.alert_dialog(options);
                    }
                }
            );
        }
        
        /**
         * Uploads the file into the corresponding finder path.
         */
        function uploadFile()
        {
            $.PercBlockUI();
            
            // Upload the path hidden field and submit the form
            // There are differences with IE, FF and Chrome when getting the value from file input field
            var fileName = dialog.find('input:file').val().replace(/.+[\\\/]/, "");
            
            // manually encode the filename for non-Ascii characters
            fileName = $.perc_utils.encodeURL(fileName);
            var encodedPath = $.perc_utils.encodePathArray(path);

            dialog.find('input[name="upload-theme-file-path"]').val('/' + encodedPath.slice(3).join('/') + '/' + fileName);
            dialog.find('#perc-theme-file-upload-form').trigger("submit");
        }
        
        /**
         * Checks the response from the server. If there is any, show it with a dialog.
         */
        function handleResponse()
        {
            // We need the text from the response
            var textResponse = $('#perc-theme-file-upload-response').text();

            // If there is a message in the response, open an alert with the error message and
            // reopen the upload dialog
            if (textResponse === "")
            {
                $.unblockUI();
                dialog.remove();
                finder.refresh(function() {});
            }
            else
            {   
                var options = {
                    title: I18N.message("perc.ui.uploadtheme.dialog.title@Error"), 
                    content: textResponse
                };
                $.perc_utils.alert_dialog(options);
                $.unblockUI();
            }
        }
        
        /**
         * Checks the response from the server. If there is any, show it with a dialog.
         */
        function handleError(jqXHR, textStatus, errorThrown)
        {
            var textResponse = I18N.message("perc.ui.upload.theme.file.dialog@Unkown Error");
            
            if (textStatus === "timeout")
                textResponse = I18N.message("perc.ui.upload.theme.file.dialog@Operation time");
            
            var options = {
                title: I18N.message("perc.ui.uploadtheme.dialog.title@Error"), 
                content: textResponse
            };
            $.perc_utils.alert_dialog(options);
            $.unblockUI();
        }
        
        /**
         * Process the current finder path in order to determine if the last element is
         * a file, folder, etc. and set
         * @return String path Returns the intended path where the file will be uploaded
         */
        function initializePath()
        {
            var current_path = finder.getCurrentPath();
            
            // Get the selected item from Column or List mode with the class FSFile
            selectedItemSpec = $("#perc-finder-listview .perc-datatable-row-highlighted").data("percRowData");
            if (typeof selectedItemSpec === 'undefined')
            {
                var selectedItemSpec = $(".mcol-listing.perc-listing-type-FSFile.mcol-opened").data("spec");
            }
            
            // If we selected a file, pop out the last element
            if (typeof selectedItemSpec !== 'undefined' && selectedItemSpec.type === 'FSFile' && selectedItemSpec.leaf)
            {
                current_path.pop();
            }
            
            return current_path;
        }

    }// End of function: openUploadDialog
})(jQuery);

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
 * A delete helper class to provide supportive methods.
 */
(function($){
    $.PercDeleteItemHelper = {
        extractDeleteErrorMessage: extractDeleteErrorMessage
    };
    function extractDeleteErrorMessage(data, name, type){
        var id, content, canForceDelete = false,
            extractError = $.PercServiceUtils.extractDefaultErrorMessage(data),
            msgKeyBase = "perc.ui.delete" + type + "dialog.warning",
            chkBoxId = "perc_delete_" + type + "_force",
            response = data.responseText,
            matches = response.match(/User: (.*) is editing the item. Failed to delete item./);

        if (matches) {
            id = 'perc-finder-delete-error-open';
            var firstChar = type.charAt(0).toUpperCase();
            var remainder = type.substr(1);
            var label = firstChar + remainder + ": ";
            var matchesSplit = matches[1].split(" ");
            content = label + name + "<br/><br/>" +
                I18N.message(msgKeyBase + "@Open", [matchesSplit[0]]);
        }
        else
        if (response.indexOf(type + ".deleteNotAuthorized") > -1) {
            id = 'perc-finder-delete-auth';
            content = I18N.message(msgKeyBase + "@Not Authorized", [name]);
        }
        else
        if (response.indexOf(type + ".deleteTemplates") > -1) {
            id = 'perc-finder-delete-templates';
            content = I18N.message(msgKeyBase + "@Templates", [name]);
        } else if (response.indexOf(type + ".recycleFolderExists") > -1) {
            id = 'perc-finder-recycle-folder-exists';
            content = I18N.message(msgKeyBase + "@Recycle Folder Exists", [name]);
        }
        else
        if (response.indexOf(type + ".deleteApprovedPages") > -1) {
            id = 'perc-finder-delete-approved';
            content = I18N.message(msgKeyBase + "@Approved Pages", [name]) +
                "<br/><br/><input type='checkbox' id='" +
                chkBoxId +
                "' style='width:15px'/> <label class='perc_dialog_label'>" +
                I18N.message(msgKeyBase + "@Approved Pages Checkbox") +
                "</label>";
            canForceDelete = true;
        }
        else
        if (extractError !== "") {
            id = 'perc-finder-delete-error-open';
            content = extractError;
        }
        else {
            id = 'perc-finder-delete-error-open';
            content = I18N.message(msgKeyBase + "@GenericText");
        }
        var result = {
            "dialogid": id,
            "content": content,
            "canForceDelete": canForceDelete,
            "chkBoxId": chkBoxId
        };
        return result;
    }

})(jQuery);

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
 * JGA: refactored this so that we dont use a 100ms setInterval()
 * Instead we bind events that change the top part (finder, etc)
 * and change the iframe height based on that * 
 */

(function($)
{
    // Fixes the height and width of the Iframe
    fixIframeHeight = function() {
    	$("#perc-widget-library").width($("#tabs-3").width() - 45);
        var frame  = $('#frame');
        if(frame.length === 0)
            return;
             
        var header = $('.perc-main');
        var bottom = $('#bottom');
        
        var bot = bottom.position().top;
        header.height( bot );
		     
        var wh, ww;
        if( window.innerHeight ) {
            
            wh = window.innerHeight;
            ww = window.innerWidth;
            
        } else if( document.documentElement.clientHeight ) {
            //prevent IE freakout
            wh = document.documentElement.clientHeight - 4;
            ww = document.documentElement.clientWidth;
        }        
        frame.height( wh - bot);
        frame.width(ww);    
    };
    
    fixTemplateHeight = function(){
    	
    	fixBottomHeight();
    	
    };
    
    fixBottomHeight = function() {
    	var currentView = $.PercNavigationManager.getView();
    	var bottomContentDiv;
    	var bottomVerticalOffset;
    	
        if(currentView === $.PercNavigationManager.VIEW_DESIGN) {
            bottomContentDiv     = $('.perc-templates-layout');
            // 47px come from padding-top and padding bottom of the element selected
            bottomVerticalOffset = bottomContentDiv.position().top + 47;
        } else if(currentView === $.PercNavigationManager.VIEW_WORKFLOW) {
           bottomContentDiv     = $('.perc-whitebg');          
           bottomVerticalOffset = $('#tabs').position().top + 90; 
           // (90 = The difference between the start of the #tab div and .perc-whitebg div)
           $(".perc-finder-fix").css('padding-bottom', 0);
        } else if(currentView === $.PercNavigationManager.VIEW_PUBLISH) {
            bottomContentDiv     = $('.perc-whitebg');
            if ($("#tabs").length) 
            {
                bottomVerticalOffset = $('#tabs').position().top + 90;
            }
             else if($("#perc-pub-inline-help").length) {
                 bottomVerticalOffset = $('#perc-pub-inline-help').position().top + 90;
             }
        } else if(currentView === $.PercNavigationManager.VIEW_DASHBOARD) {
            bottomContentDiv     = $('.perc-body-background');
            bottomVerticalOffset = $('.perc-body-background').position().top;
        } else {
            return;
        }
    	
        if(bottomContentDiv.css('display')!=='none') {
            var wh, ww;
            if( window.innerHeight )
            {
                wh = window.innerHeight;
                ww = window.innerWidth; 
            }
            // for IE case
            else if( document.documentElement.clientHeight )
            {
                wh = document.documentElement.clientHeight - 4;
                ww = document.documentElement.clientWidth;    
            }
            bottomContentDiv.height(wh - bottomVerticalOffset);
        }
    
    };
    /**
     * Handles the Finder resize for pages that do not have the IFrames.
     */
    fixHeight = function(){
        
        var currentView = $.PercNavigationManager.getView();
        if( currentView === $.PercNavigationManager.VIEW_SITE_ARCH)
        {
            var percdropshadow = $('.perc-main');
            if(percdropshadow.length > 0)
            {
                var percdropshadowHeight = percdropshadow.height();    
                $('#perc_sa_container').css('margin-top', percdropshadowHeight + 3);
            }
        }

        // the resize event sets both the height and the width
        // but we want to leave the width auto so finder can grow/shrink when window is resized 
        $(".perc-finder").width("auto");
    };

})(jQuery);

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
 *  Implements an abstraction of datatable plugin.
 *  @author Jose Annunziato
 *
 */
(function($) {

    var MARGIN_PX = 16;
    var PADDING_BOTTOM_PX = 5;
    var tableDom;
    var configo;
    var totalPages = 0;

    /**
     *  PercDataTable(config)
     *  @param config
        { percData:
            [                                   Array of rows
                {                               Row 1
                    callback : rowclick,        Callback when clicking this row (optional) (future implementation)
                {   rowContent : [              Array of columns per row
                        [{content : "Comment 4", title : "/Site/Site1", callback : open}],      Array of elements in TDs.  Column 1
                        ["7/22/33","22:44:55 PM"]],                                             Elements can be text/html. Column 2
                    rowData : {commentId : 11123, pageId : 1123, pagePath : "/aewq/dsa/cxz"}},  Object associated with row
                {                               Row 2
                    rowContent : [
                        [{content : "Comment 5", title : "/Site/Site2"}],
                        ["8/22/33","22:44:55 PM"]],
                    rowData : {commentId : 21123, pageId : 2123, pagePath : "/sewq/dsa/cxz"}},...
         *
     */
    $.fn.PercDataTable = function(config) {
        // fix aoColumns config based on percVisibleColumns
        // throw out column configs that are not visible
        if(config.percVisibleColumns && config.aoColumns) {
            var newAoColumns = [];
            $.each(config.percVisibleColumns, function(index, visibleColumnIndex){
                newAoColumns.push(config.aoColumns[visibleColumnIndex]);
            });
            config.aoColumns = newAoColumns;
        }

        for(let c=0; c<config.aoColumns.length; c++){
            var aoColumn = config.aoColumns[c];
            aoColumn.sSortDataType = "perc-type-"+aoColumn.sType;
        }

        // build the HTML table and convert it to a dataTable
        config = $.extend(true, {}, defaultConfig, config);
        configo = config;
        tableDom = buildTableDomFromData(config);
        $(this).append(tableDom);
        tableDom.DataTable(config);

        // resize parent iframe's height to fit the table
        if(config.percExpandParentFrameVertically) {
            var height = tableDom.parents(".dataTables_wrapper").outerHeight() + MARGIN_PX;
            var width  = tableDom.parents(".dataTables_wrapper").outerWidth()  + MARGIN_PX;
            var parentFrame = getParentFrame();
            var frameHeight = height;
            var additionalHt = config.additionalIframeHeight?config.additionalIframeHeight:0;
            if(config.iDisplayLength) {
                var headHeight = tableDom.find("thead").height();
                var oneRowHeight = $(tableDom.find("tbody tr")[0]).height();
                var paginatorHeight = $(".dataTables_paginate").height();
                var frameMinHeight = headHeight + oneRowHeight * config.iDisplayLength + paginatorHeight + additionalHt;
                frameHeight = frameMinHeight;
            }
            if(config.percStayBelow) {
                var belowElement = $(config.percStayBelow);
                frameHeight += belowElement.offset().top + belowElement.outerHeight() + MARGIN_PX;
            }
            $(parentFrame).height(frameHeight + PADDING_BOTTOM_PX);
        }

        return tableDom;
    };

    function tableRedrawCallback() {
        var dataTable = $(this);
        var config = dataTable.data("config");

        if(config && config.percTableRedrawCallback)
        {
            if(typeof(config.percTableRedrawCallback) === "object")
            {
                $.each(config.percTableRedrawCallback, function(index, value){
                    this(dataTable);
                });
            }
            else if(typeof(config.percTableRedrawCallback) === "function")
            {
                config.percTableRedrawCallback(dataTable);
            }
        }
        setTimeout(function(){
            // prepend Pages label before page numbers
            var paginator = dataTable.parent().children(".dataTables_paginate.paging_full_numbers");
            paginator.find('.perc-datatable-paginator-pages-label').removeClass('paginate_button');
            // add page number attribute to each page for QA
            var pageNumbers = paginator.find("span span.paginate_button, span span.paginate_active");

            $.each(pageNumbers, function(index, element){
                $(element).attr("perc-page", index + 1 );
            });
            if(!totalPages || totalPages < 2)
                paginator.hide();
            else
                paginator.show();
            paginator.css("position","absolute");
        }, 1);

    }

    function getParentFrame() {
        var arrFrames = parent.document.getElementsByTagName("IFRAME");
        for (var i = 0; i < arrFrames.length; i++) {
            if (arrFrames[i].contentWindow === window)
                return arrFrames[i];
        }
    }

    function footerRedrawCallback( nFoot, aasData, iStart, iEnd, aiDisplay ) {
        if (configo.bPaginate){
            var config = configo;

            var itemsPerPage = config.iDisplayLength;
            var totalItemsCount = aiDisplay.length;
            var pages = Math.ceil(totalItemsCount / itemsPerPage);
            var currentPageNumber = Math.ceil(iEnd / itemsPerPage);
            var pageOfPages = currentPageNumber + " of " + pages + (pages === 1 ? " Page" : " Pages");
            totalPages = pages;
            if(pages === 0 || config.singlePage)
                pageOfPages = "";

            var pInfo = $(".perc-datatables-info");
            if(pInfo.length > 0){
                pInfo.html(pageOfPages);
            } else {
                $("<div class='datatables_info perc-datatables-info'>"+pageOfPages+"</div>")
                    .appendTo("body");
            }

            var gFooterBar = $(".perc-footer-bar");
            if(gFooterBar.length === 0){
                $("<div class='perc-footer-bar'>&nbsp;</div>")
                    .appendTo("body");
            }
        }
    }

    var defaultConfig = {
        percExpandParentFrameVertically : true,
        additionalIframeHeight : 0, //The Iframe height is calculated based on the rows and other things, if a gadget requires additional height they can specify by this property
        percColumnWidths : ["*","123"],
        percRowDblclickCallback : $.PercOpenPage,
        showPreviewBtnOnHover: false,
        iDisplayLength : 5,
        bFilter: false,
        bAutoWidth : false,
        bPaginate : true,
        bSort: true,
        sPaginationType : "full_numbers",
        bLengthChange : false,
        bInfo : true,
        fnDrawCallback : tableRedrawCallback,
        fnFooterCallback: footerRedrawCallback,
        oLanguage : {sZeroRecords: typeof I18N === "undefined" ? "No Pages Found" : I18N.message("perc.ui.workflow.status.gadget@No Pages Found"), oPaginate : {sFirst : "&lt;&lt;", sPrevious : "&lt;", sNext : "&gt;", sLast : "&gt;&gt;"}, sInfo : " ", sInfoEmpty : " "}
    };

    /**
     *  Builds a Table DOM from the array of arrays in the percData configuration
     *  @param config table configuration containing percData and percHeaders
     *
     *  Generates the following DOM
     *
     *  <pre>
     *  <table cellspacing="0" cellpadding="0" class="perc-datatable">
     *      <thead>
     *          <tr class="perc-datatable-head-row">
     *              <th class="perc-datatable-head-column perc-index-0 perc-first sorting_asc">Page</th>
     *              <th class="perc-datatable-head-column perc-index-1 sorting">Heading 2</th>
     *          </tr>
     *      </thead>
     *      <tbody>
     *          <tr class="perc-datatable-row perc-index-0 perc-first odd">
     *              <td valign="top" class="perc-datatable-column perc-index-0 perc-first sorting_1">
     *                  <div class="perc-datatable-columnrow perc-index-0 perc-first" title="">Comment 11</div>
     *                  <div class="perc-datatable-columnrow perc-index-1 perc-last" title="">/Site/Site22</div>
     *              </td>
     *              <td valign="top" class="perc-datatable-column perc-index-1 ">
     *                  <div class="perc-datatable-columnrow perc-index-0 perc-first" title="">12/22/33</div>
     *                  <div class="perc-datatable-columnrow perc-index-1 perc-last" title="">22:44:55 PM</div>
     *              </td>
     *           </tr>
     *           <tr class="perc-datatable-row perc-index-1 per-last even">
     *              <td valign="top" class="perc-datatable-column perc-index-0 perc-first sorting_1">
     *                  <div class="perc-datatable-columnrow perc-index-0 perc-first" title="">Comment 22</div>
     *                  <div class="perc-datatable-columnrow perc-index-1 perc-last" title="">/Site/Site33</div>
     *               </td>
     *               <td valign="top" class="perc-datatable-column perc-index-1 ">
     *                   <div class="perc-datatable-columnrow perc-index-0 perc-first" title="">13/22/33</div>
     *                   <div class="perc-datatable-columnrow perc-index-1 perc-last" title="">22:44:55 PM</div>
     *              </td>
     *          </tr>
     *      </tbody>
     *  </table>
     *  </pre>
     *
     */
    function buildTableDomFromData(config) {
        var data = config.percData;
        var headers = config.percHeaders;

        // create the table and body
        var table = $("<table class='perc-datatable' style='table-layout : fixed' cellpadding='0' cellspacing='0'>");
        var tbody = $("<tbody>");

        var aoColumns = config.aoColumns;

        // iterate over the data containing rows
        $.each(data, function(rowIndex, element){
            var row = element;

            // mark the first and last table rows
            var firstLast = "";
            if(rowIndex === 0)
                firstLast = "perc-first";
            else if(rowIndex === data.length-1)
                firstLast = "perc-last";

            // create the table row
            var rowTr = $("<tr class='perc-datatable-row perc-index-"+rowIndex+" "+firstLast+"'>");

            if(row.rowData)
                rowTr.data("percRowData", row.rowData);

            // bind click event callbacks
            if(config.percRowClickCallback)
            {
                if(row.rowData)
                {
                    rowTr.on("click",null,row.rowData,
                        function(evt){
                            config.percRowClickCallback(evt);
                        });
                }
                else
                {
                    rowTr.on("click",function(e){
                        config.percRowClickCallback(e);
                    });
                }
            }

            // bind mouseover event callbacks
            if(config.showPreviewBtnOnHover)
            {

                rowTr.on("mouseover",function() {
                    $(this).css('background-color', '#CAF589');
                    $(this).find('.perc-preview-col').show();

                }).on("mouseout",function(){
                    $(this).css('background-color', 'white');
                    $(this).find('.perc-preview-col').hide();
                });
            }

            if(config.percRowDblclickCallback)
            {
                if(row.rowData)
                {
                    rowTr.on("dblclick",row.rowData, function(e){
                        config.percRowDblclickCallback(e);
                    });
                }
                else
                {
                    rowTr.on("dblclick",function(e){
                        config.percRowDblclickCallback(e);
                    });
                }
            }

            // iterate over the columns in each row
            var aoIndex = 0;
            $.each(row.rowContent, function(colIndex, element){

                // skip over non visible columns
                if(config.percVisibleColumns)
                    if($.inArray(colIndex, config.percVisibleColumns)==-1)
                        return true;

                var aoColumn = aoColumns[aoIndex++];
                var sType = aoColumn.sType;
                var percType = "perc-type-"+sType;

                var column = element;
                // mark the first and last column
                var firstLast = "";
                if(colIndex == 0)
                    firstLast = "perc-first";
                else if(colIndex == row.rowContent.length-1)
                    firstLast = "perc-last";
                else
                    firstLast = "perc-middle";

                var headerClass = "";
                //Header classes are auto generated by the element text, if the element text happens to be invalid for a class name,
                //users of this table can pass another array from which the header classes can be created
                if(config.percHeaderClasses && config.percHeaderClasses[colIndex])
                {
                    headerClass = "perc-"+config.percHeaderClasses[colIndex].replace(/ /g,"-").toLowerCase();
                }
                else
                {
                    headerClass = "perc-"+config.percHeaders[colIndex].replace(/ /g,"-").toLowerCase();
                }

                // create the table data
                var columnTd = $("<td class='"+percType+" "+headerClass+" perc-datatable-column perc-ellipsis perc-index-"+colIndex+" perc-cell-"+colIndex+"-"+rowIndex+" "+firstLast+"' valign='top'>");
                if(aoIndex ==1){
                    columnTd = $("<td scope = row' class='"+percType+" "+headerClass+" perc-datatable-column perc-ellipsis perc-index-"+colIndex+" perc-cell-"+colIndex+"-"+rowIndex+" "+firstLast+"' valign='top'>");
                }
                var columnRow;
                if(typeof column === "object") {
                    var content = "";
                    var title = "";

                    // iterate over the rows within a table cell
                    if(Array.isArray(column)) {
                        $.each(column, function(colRowIndex, element){
                            if(!element)
                                element = "&nbsp;";
                            var columnRowData = element;
                            var firstLast = "";
                            if(colRowIndex === 0)
                                firstLast = "perc-first";
                            else if(colRowIndex === column.length-1)
                                firstLast = "perc-last";
                            else
                                firstLast = "perc-middle";
                            // if it's just a string, then that's the content, otherwise it's an object with content and maybe a title
                            if(typeof columnRowData == "string") {
                                content = columnRowData;
                            } else {
                                columnRowData = $.extend({ "content" : "", "title" : "" }, columnRowData);
                                content = columnRowData.content;
                                title = columnRowData.title;
                            }

                            if(title === "&nbsp;")
                                title = "";

                            // finally, wrap the content in a div and then add it to the table data
                            columnRow = $("<div style='width:100%' class='perc-datatable-columnrow perc-ellipsis perc-index-"+colRowIndex+" "+firstLast+"'>");
                            if(columnRowData.callback) {
                                var cBack = $("<span>")
                                    .attr("title", title)
                                    .append(content);
                                cBack
                                    .css("cursor", "pointer")
                                    .addClass("perc-callback");
                                columnRow.append(cBack);
                                if(row.rowData)
                                    cBack.on("click",null, row.rowData, function(e){
                                        columnRowData.callback(e);
                                    });
                                else
                                    cBack.on("click",function(e){
                                        columnRowData.callback(e);
                                    });
                            }
                            else {
                                columnRow
                                    .attr("title", title)
                                    .append(content);
                            }

                            columnTd.append(columnRow);
                        });
                    } else {
                        let title = element.title;
                        let content = element.content;
                        columnRow = $("<div title='"+title+"' class='perc-datatable-columnrow perc-ellipsis perc-index-0 perc-first'>")
                            .append(content);
                        columnTd.append(columnRow);
                    }
                } else {
                    columnRow = $("<div class='perc-datatable-columnrow perc-ellipsis perc-index-0 perc-first'>")
                        .append(element);
                    columnTd.append(columnRow);
                }
                // add the table data to the row
                rowTr.append(columnTd);
            });
            // add the row to the table body
            tbody.append(rowTr);
        });
        // add the table body to the table
        table.append(tbody);

        if(headers) {
            var thead = $("<thead>");
            var row = $("<tr class='perc-datatable-head-row'>");
            var aoIndex = 0;
            $.each(headers, function(index, element){
                // skip over non visible columns
                if(config.percVisibleColumns)
                    if($.inArray(index, config.percVisibleColumns)===-1)
                        return true;

                var aoColumn = aoColumns[aoIndex++];
                var sType = aoColumn.sType;
                var percType = "perc-type-"+sType;

                var columnWidth = "";
                if(config.percColumnWidths) {
                    if(index < config.percColumnWidths.length-1) {
                        columnWidth = config.percColumnWidths[index];
                        if(columnWidth === "*")
                            columnWidth = "";
                    } else {
                        columnWidth = config.percColumnWidths[config.percColumnWidths.length-1];
                    }
                }


                // if($.browser.browser.msie || $.browser.webkit)
                //     columnWidth = parseInt(columnWidth) + 20;


                if(columnWidth[columnWidth.length - 1] !== '%') {
                    columnWidth += "px";
                }

                var headerClass = "";
                if(config.percHeaderClasses && config.percHeaderClasses[index])
                {
                    headerClass = "perc-"+config.percHeaderClasses[index].replace(/ /g,"-").toLowerCase();
                }
                else
                {
                    headerClass = "perc-"+config.percHeaders[index].replace(/ /g,"-").toLowerCase();
                }


                var firstLast = "";
                if(index === 0)
                    firstLast = "perc-first";
                else if(index === headers.length-1)
                    firstLast = "perc-last";
                else
                    firstLast = "perc-middle";
                var head = $("<th scope='col' class='"+percType+" "+headerClass+" perc-datatable-head-column perc-index-"+index+" "+firstLast+"'>");

                head.width(columnWidth);

                var sortingDirection = $("<span class='perc-sort' style='padding: 0px 10px 0px 0px; border-bottom:none'>&nbsp;</span>");

                //Asign external sort function.
                if (typeof(config.sortFunction) !== "undefined" && !config.bSort){
                    var colName = config.percColNames[index];
                    var data = {};
                    data.colName = colName;
                    data.sortFunction = config.sortFunction;
                    head.on("click",function(e){
                        e.data = data;
                        sortingHandler(e,$(this));
                    });

                    //Avoid select text on double click in the headers.
                    if($.browser.mozilla)
                        head.css('MozUserSelect','none');
                    else if($.browser.msie)
                        head.on('selectstart',function(){return false;});

                    //SortOrder == asc or desc
                    if (colName === config.sortColumn){
                        head.addClass("sorting_" + config.sortOrder);
                    }
                }

                head.append(element);
                head.append(sortingDirection);
                row.append(head);
            });
            thead.append(row);
            table.append(thead);
        }
        if(config.bSort)
            declareCustomSortingFunctions();

        return table;
    }

    function sortingHandler(event,head){
        var element = event.data.colName;
        var callback = event.data.sortFunction;
        head.siblings().removeClass("sorting_asc").removeClass("sorting_desc");
        var order = "asc";
        if (head.is(".sorting_asc")){
            head.removeClass("sorting_asc").addClass("sorting_desc");
            order = "desc";
        }else if (head.is(".sorting_desc")){
            head.removeClass("sorting_desc").addClass("sorting_asc");
        }else{
            head.addClass("sorting_asc");
        }
        callback(element, order);
    }

    function declareCustomSortingFunctions() {
        // custom column sorting for Change, Views, and Template column
        // checks to see if all data is the same and if so it changes it
        // so that it is unique to force it to sort in reverse order
        $.fn.dataTableExt.afnSortData['perc-type-string'] = function  ( oSettings, iColumn ) {
            var aData = [];
            var data;
            this.api().column( iColumn, {order:'index'} ).nodes().map( function ( td, iColumn ) {
                var inText = $(td)[0].innerText;
                //CMS-8495 : Activity gadget sorting issue as the values 1, 11, 12 etc were being treated as string rather than numerals.
                if(isNumericValue(inText)){
                    inText = parseInt(inText);
                }
                aData.push(inText);
            } );

            return aData;
        };

        $.fn.dataTableExt.afnSortData['perc-type-numeric'] = $.fn.dataTableExt.afnSortData['perc-type-string'];

        // custom column sorting for date columns
        // changes the seconds so that if the date, minutes and hours are the same
        // it will be forced to sort in reverse order
        $.fn.dataTableExt.afnSortData['perc-type-date'] = function  ( oSettings, iColumn ) {
            var aData = [];
            this.api().column( iColumn, {order:'index'} ).nodes().map( function ( td, iColumn ) {
                var dateTimeArray = Array("", "");
                var date;
                var divs = $($(td)[0]).find('div');
                if(divs.length>1){
                    //blogs gadget has dates in two divs one for date and other for time.
                    dateTimeArray[0] = $(divs[0]).text();
                    dateTimeArray[1] = $(divs[1]).text();
                    var dateString = dateTimeArray.join(' ');
                    date = new Date(dateString);
                }else{
                    date = new Date($(td)[0].innerText);
                }
                aData.push(date);
            });
            return aData;
        };
    }

    //To check if the column value is numeric for sorting.
    function isNumericValue(str){
        return !isNaN(parseFloat(str)) && isFinite(str);
    }

})(jQuery); 

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

(function($)
{
    var resizable;
    var hide;
    var show;
    var fixedTable = null;
    var table;
    var configo;
    $.fn.PercFixedTableHeader = function(config)
    {
        $(".perc-fixedtableheader").remove();

        configo = config;
        table = $(this);
        var tableIndex = table.css("z-index");
        if(tableIndex == "auto")
            tableIndex = "1000";

        // clone the table for which we are making the header
        // blow away the body and just keep the header with all its styles and event bindings
        
        // Detect the browser and set the correct top:value for the fixed header Table. 
        if($.browser.msie ||$.browser.chrome ||$.browser.safari) {
            var topValue = "-3px";
        }
        else {
            var topValue = "1px";
        }
        
        fixedTable = $(this).clone(true);
        fixedTable.find("tbody").remove();
        fixedTable
            .css("position","relative")
            .css("margin-bottom","-30px")
            .css("top",topValue)
            .css("left","1px")
            .css("z-index", tableIndex + 1)
            .css("background","white")
            .attr("cellpadding", "0")
            .addClass("perc-fixedtableheader");

        configo.container.prepend(fixedTable);

        // blow away this widget if you click on a given element
        remove = config.remove;
        if(remove)
        {
            remove.on("click", function()
            {
                fixedTable.remove();
            });
        }

        // update the width of this widget if the window or container resizes resizes
        resizable = config.resizable;
        if(resizable)
        {
            resizable.on("resize", function()
            {
                update();
            });
        }

        // update this widget if the window is resized
        $(window).on("resize",function()
        {
            update();
        });

        function update()
        {
            // It is preferable to set the width of tables using CSS, than usding $(table).width()
            // since it is more cross browser compatible, specially when dealing with hidden
            // elements.
            fixedTable.css('width', table.css('width'));
        }

        update();
    };
})(jQuery);

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

(function($)
{
    var defaultConfig =
        {

        };
    var _folderDblClickCallback = function(){};
    $.fn.PercFinderListView = function(config, serviceContent)
    {
        var self = $(this);
        var serviceData;
        if(typeof config.folderDblClickCallback === "function") {
            _folderDblClickCallback = config.folderDblClickCallback;
        }
        if (serviceContent.PagedItemList !== undefined)
        {
            serviceData = $.perc_utils.convertCXFArray(serviceContent.PagedItemList.childrenInPage);
            self.data('totalResult', serviceContent.PagedItemList.childrenCount);
            self.data('startIndex', serviceContent.PagedItemList.startIndex);
        }
        else
        {
            serviceData = $.perc_utils.convertCXFArray(serviceContent.PathItem);
        }

        var c = parseContenIntoConfig(config.displayFormat, serviceData);

        var percData     = c.percData;
        var aoColumns    = c.percTypes;
        if(percData.length > 0)
        {
            var arrayRowContent = [];
            for(let s=0; s<aoColumns.length; s++)
            {
                arrayRowContent.push("&nbsp;");
            }

            percData.push({rowContent : arrayRowContent});
        }
        var percHeaders  = c.percHeaders;
        var percColNames = c.percColNames;
        var percWidths   = c.percWidths;
        var percColumnWidths = percWidths;

        var configDT = {percRowClickCallback : rowClickCallback, percRowDblclickCallback : rowDblclickCallback, percColumnWidths : percColumnWidths, percData : percData, percHeaders : percHeaders, aoColumns : aoColumns};
        configDT.oLanguage = {"sZeroRecords": "No Files Found"};
        configDT.bPaginate = false;
        configDT.bInfo = false;
        configDT.bSort = false;
        configDT.oLanguage.sZeroRecords = "No Items Found";
        configDT.sortFunction = config.sortFunction;
        configDT.sortColumn = config.sortColumn;
        configDT.sortOrder = config.sortOrder;
        configDT.percColNames = percColNames;

        self.empty();
        self.PercDataTable(configDT);

        var table = $(self.find("table"));
        var container = $(table.parents(".mcol-direc"));
        table.PercFixedTableHeader({"resizable":$(".perc-finder-body.ui-resizable"), "remove":$("#perc-finder-choose-columnview"), "container":container});

        if (typeof(config.callback) != "undefined")
            config.callback(self);

        createItemsDragAndDrop(table);

        $(".perc-datatable-row:last").off();
    };

    function createItemsDragAndDrop (table)
    {
        var allRows = table.find(".perc-datatable-row");
        $.map( allRows, function(val, i) {
            if ($(val).data("percRowData") !== undefined && $(val).data("percRowData").category === "ASSET")
            {
                $(val).css("cursor", "default");
                $(val).draggable( {
                    helper: function() {
                        return $('<div />')
                            .html($(this).find(".perc-datatable-columnrow").html())
                            .addClass("dataTables_wrapper")
                            .css('color', "white");
                    },
                    appendTo: 'body',
                    refreshPositions: true,
                    zIndex: 9990,
                    revert: true,
                    revertDuration: 0,
                    start: $.perc_finder().onDragStart,
                    stop: $.perc_finder().onDragStop,
                    delay: $.perc_finder().dragDelay
                });
            }
        });
        // This droppable is temporal to gain the effect of disablement like the column view
        // (it will be updated to be functional with the other list finder drag and dropping functionality
        $(table).droppable( {
            tolerance: 'pointer',
            accept: false,
            over: function(){},
            out: function(){},
            drop: function(){} } );
    }

    /**
     * Callback function invoked after clicking a row in the list view.
     * @param rowData jQuery.Event
     */
    function rowClickCallback(rowData)
    {
        $(".perc-datatable-row").removeClass("perc-datatable-row-highlighted");
        $(this).addClass("perc-datatable-row-highlighted");
        var newPath = $.merge([""], getItemFolderPath(rowData));

        // Reflect the path change in the input (pathbar) on top of the finder and after that,
        // and invoke the  "change path" listeners with the new one
        $("#mcol-path-summary").val(newPath.join('/'));

        //Set the current item in the Finder.
        if (typeof(rowData.data.id) != "undefined") {
            $.perc_finder().setCurrentItem(rowData.data);
        }
        $.perc_finder().flagChangeView = false;
        $.perc_finder().executePathChangedListeners(newPath);
        $.perc_finder().flagChangeView = true;
    }

    function getItemFolderPath(rowData)
    {
        if(!rowData.data)
            return "";
        var folderPath;
        if (rowData.data.type === "site") // if click on a site don't need to include the item name
        {
            folderPath = rowData.data.folderPaths[0];
        }
        else
        {
            folderPath = rowData.data.folderPaths[0] + "/" + rowData.data.name;
        }

        return folderPath.replace("Folders/$System$/", "").substring(2,folderPath.length).split("/");
    }

    function rowDblclickCallback(rowData)
    {
        if(rowData.data.type==="Folder" || rowData.data.type==="FSFolder")
        {
            _folderDblClickCallback("/" + getItemFolderPath(rowData).join("/"));
        }
        else
        {
            $.PercNavigationManager.openPathItem("/" + getItemFolderPath(rowData).join("/"));
        }
    }

    function parseContenIntoConfig(displayFormat, serviceData)
    {
        var percData    = [];
        var percTypes   = [];
        var percColNames = [];
        var percHeaders = [];
        var percWidths = [];

        var columns = displayFormat.SimpleDisplayFormat.columns;

        var c, s;
        for(s=0; s<serviceData.length; s++)
        {
            var dataRow = $.perc_utils.convertCXFArray(serviceData[s].columnData.column);
            var nameValueMap = nameValueObjectArrayToMap(dataRow, "name", "value");
            var percRow = [];

            var icon = $.perc_utils.choose_icon( serviceData[s].type, serviceData[s].icon, getItemFolderPath({data: serviceData[s]}) );
            var iconHtml = "<img style=\"float:left;\" src=\"" + icon.src + "\" alt=\""+ icon.alt + "\" title=\"" + icon.title +"\" aria-hidden=\"" + icon.decorative + "\" />";

            for(c=0; c<columns.length; c++)
            {
                var column = columns[c];
                var colName = column.name;
                var colLabel = column.label;
                var data = nameValueMap[colName];

                // format date with no seconds
                if (column.type.toLowerCase() === "date" && data !== null  && data !== "")
                {
                    var dateParts = $.perc_utils.splitDateTime(data);
                    var dateAndTime = dateParts.date + ", " + dateParts.time;
                    data = "<div title='"+dateAndTime+"'>"+ dateParts.date + "</div>";
                }

                if (column.type.toLowerCase() === "number" && data != null  && data !== "")
                {
                    data = '<div title = "' + data + ' Bytes" style="text-align:right;">' + $.perc_utils.formatFileSize(data) + "</div>";
                }
                // The first column should have this kind of tooltip (that's why c != 0)
                if (column.type.toLowerCase() === "text" && data != null  && data !== "" && c !== 0)
                {
                    data = '<span title = "' + data + '" style="font-weight: normal;">' + data + "</span>";
                }

                if(data == null || data == "")
                    data = "&nbsp;";

                if (c == 0 && iconHtml != "")
                {
                    var itemPath = "", itemPathRaw, serviceDataItem = serviceData[s];
                    // DANGER: folderPaths may contain an array
                    // What should we do if it is an array? For now, just using the fist one!
                    // HACK: CM-4488 search fails to parse results correctly
                    if (Array.isArray(serviceDataItem.folderPaths)) {
                        itemPathRaw = serviceDataItem.folderPaths[0];
                    } else {
                        itemPathRaw = serviceDataItem.folderPaths;
                    }
                    // to fix CMS-6402	.
                    if(itemPathRaw == undefined){
                        itemPathRaw ="" ;
                    }
                    if(serviceData[s].type == 'site') {
                        itemPath = itemPathRaw.replace('/' + $.perc_paths.SITES_ROOT, '');
                    }
                    else {
                        itemPath = itemPathRaw.replace('/' + $.perc_paths.SITES_ROOT, '')
                            .replace('/' + $.perc_paths.DESIGN_ROOT, '')
                            .replace('//Folders/$System$' + $.perc_paths.ASSETS_ROOT, '') + '/' + serviceData[s].name;
                    }
                    data = iconHtml + '<span title = "'+ itemPath + '" style="padding-left:4px;">' + data + "</span>";
                }

                percRow.push(data);
            }

            var percContent = {"rowContent" : percRow, "rowData" : serviceData[s] };
            percData.push(percContent);
        }


        for(c=0; c<columns.length; c++)
        {
            let column  = columns[c];
            percColNames.push(column.name);
            percHeaders.push(column.label);
            percWidths.push((column.width == -1 ? "*" : ($.browser.msie ? column.width - 20 : column.width ) ));
            var type = column.type.toLowerCase();
            if(type === "text") {
                type = "string";
            }

            if (c === 0)
            {
                type = "html";
            }
            percTypes.push({"sType" : type});
        }

        var config = {"percData" : percData, "percColNames":percColNames, "percHeaders":percHeaders, "percTypes":percTypes, "percWidths": percWidths};
        return config;
    }

    function nameValueObjectArrayToMap(dataRow, nameKey, valueKey)
    {
        var d;
        var map = {};
        for(d=0;d<dataRow.length;d++)
        {
            var data  = dataRow[d];
            var name  = data[nameKey];
            var value = data[valueKey];
            map[name] = value;
        }
        return map;
    }
})(jQuery);

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
 * @author Jose Annunziato
 */
(function($){
    $.fn.PercScrollingTemplateBrowser = function(config){
        
        var scrollable = $("<div class='perc-scrollable'>");
        var items = $("<div class='perc-items'>");
        scrollable.append(items);
        
        var prev = $("<a tabindex='0' style = 'margin:50px 0px' class='prevPage browse left' ></a>");
        var next = $("<a tabindex='0' style = 'margin:50px 0px' class='nextPage browse right'></a>");
        var clearboth = $("<div style='clear:both'>");
        
        $(this)
            .append(prev)
            .append(scrollable)
            .append(next)
            .append(clearboth);
        
        if(config.width)
            scrollable.width(config.width);
        
        var widgetDefId = null;
        if (config.widgetDefId)
            widgetDefId = config.widgetDefId;

        
        var siteName = config.siteName;
        var hiddenFieldId = config.hiddenFieldId?config.hiddenFieldId:"perc-select-template";
        var calbackfn = function(status, data){
            if(data.TemplateSummary.length === 0) {
                var empty = $("<div class='perc-empty'>" +I18N.message("perc.ui.scrolling.template.browser@No Templates Found") + "</div>")
                    .css("margin-top","70px");
                scrollable
                    .css("text-align","center")
                    .css("background","white")
                    .append(empty);
            } else {
            
                $.each( data.TemplateSummary, function(index, template) {
                    // add template instance to scrollable items
                    items.append(createTemplateEntry(this, config));
                    
                    // hide the id that appears at the top of the template
                    items.find(".item .item-id").hide();
                    
                    // bind click event to each item to handle selection
                    items.find(".item").on('click', function(){
                        var itemId = $(this).find(".item-id").text();
                        $("#" + config.hiddenFieldId).val(itemId);
                        items.find(".item").removeClass("perc-selected-item");
                        $(this).addClass("perc-selected-item");
                    });
					items.find(".item").on('keydown', function(event){
                        if(event.code == "Enter" || event.code == "Space"){
							document.activeElement.click();
						}
                    });

                    // select first item by default
                    $firstItem = items.find(".item:first");
                    $("#" + config.hiddenFieldId).val($firstItem.find(".item-id").text());
                    $firstItem.addClass("perc-selected-item");
                });
                
                // make it scollable
                scrollable.scrollable({
                    items: items,
                    size: 4,
                    keyboard: true
                });
                
                // after adding all the template entries, truncate the labels if they dont fit
                // $.PercTextOverflow($("div.perc-text-overflow"), 122);
            }
        };
        if(!config.isBase)
        {
        	$.PercSiteService.getTemplates(siteName, calbackfn, widgetDefId);
        }
        else
        {
        	$.PercSiteService.getBaseTemplates(config.baseType, calbackfn);
        }
            //Load template selector
        return $(this);
            
    };

    function createTemplateEntry(data, config){
        var temp = "<div for=\"@ITEM_ID@\" class=\"item\">"
         + "<div class=\"item-id\">@ITEM_ID@</div>"
         + "    <table>"
         + "        <tr><td align='left'>"
         + "            <img style='border:1px solid #E6E6E9' height = '86px' width = '122px' src=\"@IMG_SRC@\"/>" 
         + "        </td></tr>"
         + "        <tr><td>"
         + "            <div class='perc-text-overflow-container' style='text-overflow:ellipsis;width:122px;overflow:hidden;white-space:nowrap'>"
         + "                <div class='perc-text-overflow' style='float:none' title='@ITEM_TT@' alt='@ITEM_TT@'>@ITEM_LABEL@</div>"
         + "        </td></tr>"
         + "    </table>"        
         + "</div>";
        var tplName = data.name;
        var tplId = data.id;
        if(config.isBase){
        	tplName = tplName.replace("perc." + config.baseType + ".", "");
        	tplId = data.name;
        }
        return temp.replace(/@IMG_SRC@/, data.imageThumbPath)
            .replace(/@ITEM_ID@/g, tplId)
            .replace(/@ITEM_LABEL@/, tplName)
            .replace(/@ITEM_TT@/g, tplName);
    }

})(jQuery);

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
 * PercFinderView.js
 *
 * Handles user interaction with Finder on any but Editor page.
 * Most of the stuff has been duplicated over from PercPageView.js,
 * so when refactoring at a higher level view, please get rid of this file.
 *
 */
(function($, P)
{

    P.PercFinderView = function()
    {
        var percFinderListviewContainerInitialHeight;
        var percFinderListviewContainer;
        var PERC_FINDER_VIEW_COLUMN = "column";
        var PERC_FINDER_VIEW_LIST = "list";
        var PERC_FINDER_SEARCH_RESULTS = "search";
        var PERC_FINDER_SEARCH_TYPE_MYPAGES = "MyPages";
        var PERC_FINDER_RESULT = "result";
        var MAX_RESULTS;
        var utils = $.perc_utils;
        var currentContentPath;
        var currentContentId; // current page id being edited
        var finderButtons; // buttons on the top right of the finder: new site, page, delete site
        var dirty = false; // dirty page
        var finder = $.perc_finder(); // the finder, the miller column
        var contentId = null;
        var pageView = P.pageView();
        var dialogFlag = true;

        // Expose the setView method (so we can use it from perc_finder.js, for example)
        this.setView = setView;

        // choose column or list view
        var chooseColumnView = $("#perc-finder-choose-columnview").on("click",
            function(evt){
                setViewColumn(evt);
            });

        var chooseListView = $("#perc-finder-choose-listview").on("click",
            function(evt){
                setViewList(evt);
            });
        var chooseSearchView = $("#perc-finder-search-submit").on("click",
            function(evt){
                setViewSearch(evt);
            });
        var chooseMyPagesView = $("#perc-finder-choose-mypagesview").on("click",
            function(evt){
                setMyPagesView(evt);
            });

        setColumnViewButtonOn();

        // singleton to keep track of dirty state across various types of resources such as pages, templates and assets
        var dirtyController = $.PercDirtyController;

        // Interface to local API to pass around to Finder and Page Edit Dialog
        // so they can call back and update the Content tab
        var percFinderViewAPI = {
            reload: function()
            {
            },
            getContentId: function()
            {
                return currentContentId;
            },
            clear: function()
            {
                $('#frame').each(function()
                {
                    $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                });
            },
            confirm_if_dirty: confirm_if_dirty,
            save: save
        };

        /**
         * Helper function to enable or disable the search bar of the finder. This method is exposed to be used by perc_finder.js
         * @param flag(boolean) if <code>true</code> the search bar (including the button) is disabled, otherwise the is enabled.
         */
        this.disableSearchBar = function disableSearchBar(disable)
        {
            if (disable)
            {
                $("#perc-finder-item-search").attr('disabled', true);
                $("#perc-finder-item-search").val("");
                chooseSearchView.off("click");
            }
            else
            {
                $("#perc-finder-item-search").prop('disabled',false);
                chooseSearchView.on("click",function(evt){
                    setViewSearch(evt);
                });
            }
        };

        /**
         * Helper function to enable or disable the List View button of the finder. This method is exposed to be used by perc_finder.js
         * @param flag(boolean) if <code>true</code> the button is disabled, otherwise the button is enabled.
         */
        this.disableListViewButton = function disableListViewButton(disable)
        {
            if (disable)
            {
                chooseListView.off("click");
            }
            else
            {
                chooseListView.on("click",function(evt){
                    setViewList(evt);
                });
            }
        };

        var currentFinderView = PERC_FINDER_VIEW_COLUMN;
        // Expose the current view properties
        this.getCurrentFinderView = function()
        {
            return currentFinderView;
        };
        this.PERC_FINDER_VIEW_COLUMN = PERC_FINDER_VIEW_COLUMN;
        this.PERC_FINDER_VIEW_LIST = PERC_FINDER_VIEW_LIST;
        this.PERC_FINDER_SEARCH_RESULTS = PERC_FINDER_SEARCH_RESULTS;
        this.PERC_FINDER_RESULT = PERC_FINDER_RESULT;
        var pagingBar = "";

        // Bind the Enter key and Esc key on Input search field
        $("#perc-finder-item-search").on("focus",function(evt)
        {
            $(this).css('color', '#FFFFFF').css('background-color', '#5a5d69');
        }).on('keyup', function(evt)
        {
            if (evt.keyCode === 13)
            {
                $("#perc-finder-item-search").trigger("blur");
                performSearch();
                $("#perc-finder-item-search").trigger("focus");
                evt.preventDefault();
                evt.stopPropagation();
            }
            if (evt.keyCode === 27 || evt.keyCode === 9)
            {
                $("#perc-finder-item-search").css('color', '#CCCCCC').css('background-color', '#41434F').trigger("blur");
                evt.preventDefault();
                evt.stopPropagation();
            }
            $('.perc-finder-message').fadeOut(function () { $(this).empty(); });
        });

        //Set the newStartIndex and refresh the view
        function pagePrevious(event)
        {
            percFinderListviewContainer = $(".perc-finder").find('#perc-finder-listview');
            percFinderListviewContainer.data('startIndex', percFinderListviewContainer.data('startIndex') - MAX_RESULTS);
            percFinderListviewContainer.data('callback', updatePagingBar);
            refreshView();
        }

        //Set the newStartIndex and refresh the view
        function pageNext(event)
        {
            var percFinderListviewContainer = $(".perc-finder").find('#perc-finder-listview');
            percFinderListviewContainer.data('startIndex', percFinderListviewContainer.data('startIndex') + MAX_RESULTS);
            percFinderListviewContainer.data('callback', updatePagingBar);
            refreshView();
        }

        //Perform a refresh of the current view
        //This function is called after paging, sorting and new search when the user is already in search view
        function refreshView()
        {
            percFinderListviewContainer = $(".perc-finder").find('#perc-finder-listview');
            $(".perc-fixedtableheader").remove();
            percFinderListviewContainer.empty();
            percFinderListviewContainer.css('text-align', 'left');
            percFinderListviewContainer.append($('<div class="perc-finder-panel-loading"><span class="icon-spinner icon-spin icon-2x"></span>&nbsp;Loading...</div>'));
            var column = percFinderListviewContainer.data('sortColumn');
            var order = percFinderListviewContainer.data('sortOrder');
            var startIndex = percFinderListviewContainer.data('startIndex');
            var callbackFunction = percFinderListviewContainer.data('callback');
            var config = {
                "startIndex": startIndex,
                "maxResults": MAX_RESULTS,
                "callback": callbackFunction,
                "sortFunction": sortView,
                "sortColumn": column,
                "sortOrder": order
            };
            percFinderListviewContainer = $("#perc-finder-listview");
            percFinderListviewContainerInitialHeight = percFinderListviewContainer.height();
            if (currentFinderView === PERC_FINDER_VIEW_LIST)
            {
                config.path = percFinderListviewContainer.data('path');

                fillListView(config, function()
                {
                    expandTableBorders(true);
                });
            }
            else if (currentFinderView === PERC_FINDER_SEARCH_RESULTS || currentFinderView === PERC_FINDER_SEARCH_TYPE_MYPAGES)
            {
                var searchQuery = $("#perc-finder-listview").data('searchQuery');
                var searchType = percFinderListviewContainer.data('searchType');
                config.searchCriteria = {
                    "SearchCriteria": {
                        "query": searchQuery,
                        "searchType":searchType,
                        "startIndex": startIndex,
                        "maxResults": MAX_RESULTS,
                        "sortColumn": column,
                        "sortOrder": order
                    }
                };
                fillResultView(config, function()
                {
                    expandTableBorders(true);
                });
            }
        }

        function expandTableBorders(expand)
        {
			$('#perc-finder-listview table thead tr th').each(function(){
				$(this).attr('scope', "col");

			});

			$('#perc-finder-listview table tbody tr').each(function(){
				var myRow = $(this);
				myRow.find('td').each(function(j) {
					$(this).attr('scope', "row");
					return false;
				});

			});

		    if (percFinderListviewContainer)
            {
                var finder = $(".perc-finder");

                var lastRow = percFinderListviewContainer.find("tr:last");
                lastRowHeight = lastRow.height();
                var containerElement = $("#perc-finder-listview table:first");
                var containerHeight = containerElement.height();
                var fixedColumnHeight = $(".perc-view-column-fixed").height();
                var differenceValue = fixedColumnHeight - containerHeight;
                if (expand)
                {
                    if (lastRowHeight > 0)
                        lastRow.height(lastRowHeight + differenceValue);
                }
                else
                {
                    lastRow.height(20);
                }
            }
        }
        //Set the content of the paging bar (used by showPagingBar and updatePagingBar)
        function fillPagingBar(dir)
        {
            var startIndex = dir.data('startIndex');
            var totalResult = dir.data('totalResult');
            var endIndex = ((startIndex - 1 + MAX_RESULTS > totalResult) ? totalResult : (startIndex - 1 + MAX_RESULTS));
            var itemText = "0 Items";
            if (totalResult !== 0)
                itemText = startIndex + " - " + endIndex + " of " + totalResult + " Total";

            //Fill text info.
            pagingBar.find(".perc-pagingbar-items").text(itemText);

            //Enable/disable navigation buttons
            //Check if have next items
            if ((totalResult - (startIndex - 1 + MAX_RESULTS)) > 0)
                pagingBar.find('.perc-pagingbar-next').removeClass('perc-disabled-navigator').off('click').on("click",
                    function(evt){
                        pageNext(evt);
                    });
            else
                pagingBar.find('.perc-pagingbar-next').addClass('perc-disabled-navigator').off('click');

            //Check if have previous items
            if (startIndex > MAX_RESULTS)
                pagingBar.find('.perc-pagingbar-previous').removeClass('perc-disabled-navigator').off('click').on("click",
                    function(evt){
                        pagePrevious(evt);
                    });
            else
                pagingBar.find('.perc-pagingbar-previous').addClass('perc-disabled-navigator').off('click');
        }

        //Attach the paging bar
        function showPagingBar(dir)
        {
            if (currentFinderView === PERC_FINDER_VIEW_LIST || currentFinderView === PERC_FINDER_SEARCH_RESULTS)
            {
                var finderDiv = $(".perc-finder");
                //Generate the HTML Bar
                pagingBar = $('<div class="perc-pagingbar-finder"/>').append($('<div class="perc-pagingbar-navigator" />').append($('<a class="perc-pagingbar-previous" />').attr('title', 'Previous')).append($('<a class="perc-pagingbar-next"/>').attr('title', 'Next'))).append($('<span class="perc-pagingbar-items" />')
                );

                fillPagingBar(dir);
                var newHeight = finderDiv.height();
                finderDiv.append(pagingBar);
                fixIframeHeight();
                fixHeight();
            }
            // Update the lower part of Navigation/User page.
            var currentView = $.PercNavigationManager.getView();
            if (currentView === $.PercNavigationManager.VIEW_SITE_ARCH || currentView === $.PercNavigationManager.VIEW_USERS)
            {
                fixIframeHeight();
                fixBottomHeight();
                fixHeight();
                if ($("#perc_site_map").length > 0)
                {
                    $("#perc_site_map").perc_site_map('layoutAll');
                }
            }
            // make sure the finder adjusts height based on paging bar showing or hiding
            finder.update_finder_height();
        }

        //Update an existing paging bar
        function updatePagingBar(dir)
        {
            if (currentFinderView === PERC_FINDER_VIEW_LIST || currentFinderView === PERC_FINDER_SEARCH_RESULTS)
            {
                fillPagingBar(dir);
            }
        }

        //Create the new container and change view
        function addListViewContainer()
        {
            var finderTable = $(".perc-finder-table");
            var allColumns = finderTable.find("td.mcol-direc");
            var lastColumn = finderTable.find("td.mcol-direc:last");

            $(allColumns[0]).siblings().hide(); //Hide all columns
            var newColumn = $.perc_finderInstance.insertAfter(lastColumn); //Add a new column to contains the List View or Result
            newColumn.css("width", "100%");
            var newColumnContent = newColumn.find(".mcol-direc-wrapper");
            newColumnContent.find(".ui-resizable-handle").remove(); //Remove the div for resize width

            //Add List View container
            percFinderListviewContainer = $("<div id='perc-finder-listview'>");
            newColumnContent.css("width", "100%");
            newColumnContent.append(percFinderListviewContainer);

            return percFinderListviewContainer;
        }

        //Call the List View service with a specific config
        function fillListView(config, callback)
        {
            //Get and set the display format
            $.PercPathService.getDisplayFormat(function(status, displayFormat)
            {
                config.displayFormat = displayFormat;
                config.folderDblClickCallback = function(contentPath)
                {
                    _updateListViewContainerData(contentPath, 1, updatePagingBar);
                };
                $.PercPathService.getContentForPath(displayFormat.SimpleDisplayFormat, config, function(status, content)
                {
                    if (status)
                    {
                        percFinderListviewContainer.PercFinderListView(config, content);
                        var newPath = $("#mcol-path-summary").val().trim();
                        $.PercPathService.getPathItemForPath(newPath, function(status, content)
                        {
                            if (content.PathItem.type === "Folder" || content.PathItem.type === "FSFolder")
                            {
                                $(".perc-finder-menu #perc-finder-delete").removeClass('ui-enabled').addClass('ui-disabled').off('click');
                                if (callback)
                                    callback();
                                return;
                            }
                            var index = newPath.lastIndexOf('/');
                            newPath = newPath.substring(index + 1);
                            $('#perc-finder-listview td.perc-first').each(function()
                            {
                                var self = $(this);
                                var selectedText = self.find('span').text();
                                if (newPath === selectedText)
                                {
                                    self.parent().trigger("click");
                                    finder.scrollIntoView(self);
                                }
                            });
                            if (callback)
                                callback();
                        });
                    }
                    else
                    {
                        var error = $("<span style='font-weight: normal; margin-top: 15px; display:block'/>").text(result);
                        $("#perc-finder-listview").css('text-align', 'center').append(error);
                        $(".perc-finder-panel-loading").remove();
                    }

                    percFinderListviewContainer = $("#perc-finder-listview");
                    percFinderListviewContainerInitialHeight = percFinderListviewContainer.height();
                });
            });
        }

        //Call the Result View service with a specific config
        function fillResultView(config, callback)
        {
            //Get and set the display format
            $.PercPathService.getDisplayFormat(function(status, displayFormat)
            {
                config.displayFormat = displayFormat;
                config.searchCriteria.SearchCriteria.formatId = displayFormat.SimpleDisplayFormat.id;
                config.folderDblClickCallback = function(contentPath)
                {
                    _updateListViewContainerData(contentPath, 1, updatePagingBar);
                };
                $.PercSearchService.getSearchResult(config.searchCriteria, function(status, result)
                {
                    if (status)
                    {
                        $("#perc-finder-listview").PercFinderListView(config, result);
                        if (callback)
                            callback();
                    }
                    else
                    {
                        var error = $("<span style='font-weight: normal; margin-top: 15px; display:block'/>").text(result);
                        $("#perc-finder-listview").css('text-align', 'center').append(error);
                        $(".perc-finder-panel-loading").remove();
                    }
                });
            });
        }

        /**
         * Checks if the we are switching to the search view. If we are switching the view calls
         * setView, if not re-fills the result view.
         */
        function performSearch()
        {
            if (currentFinderView === PERC_FINDER_SEARCH_RESULTS)
            {
                // URL-encode the text to avoid jQuery bug:
                var encodedSearchText = encodeURIComponent($("#perc-finder-item-search").val().trim());
                var percFinderListviewContainer = $(".perc-finder").find('#perc-finder-listview');
                percFinderListviewContainer.data('searchQuery', encodedSearchText);
                percFinderListviewContainer.data('startIndex', 1);
                percFinderListviewContainer.data('sortColumn', "sys_title");
                percFinderListviewContainer.data('sortOrder', "asc");
                percFinderListviewContainer.data('callback', updatePagingBar);
                refreshView();
            }
            else
            {
                setView(PERC_FINDER_SEARCH_RESULTS);
            }
            setSearchViewButtonsStates();
            $("#mcol-path-summary").val("");
        }

        //Set the new column and order to sort and refresh the view
        function sortView(column, order)
        {
            var percFinderListviewContainer = $(".perc-finder").find('#perc-finder-listview');
            percFinderListviewContainer.data('startIndex', 1);
            percFinderListviewContainer.data('sortColumn', column);
            percFinderListviewContainer.data('sortOrder', order);
            percFinderListviewContainer.data('callback', updatePagingBar);
            refreshView();
        }

        /**
         * Calls the setView method to set the finder in Column mode.
         * It is generally bound to click events on buttons that switch views.
         */
        function setViewColumn(event)
        {
            setView(PERC_FINDER_VIEW_COLUMN);
        }

        /**
         * Calls the setView method to set the finder in List mode.
         * It is generally bound to click events on buttons that switch views.
         */
        function setViewList(event)
        {
            setView(PERC_FINDER_VIEW_LIST);
        }

        /**
         * Calls the setView method to set the finder in Search results mode.
         * It is generally bound to click events on buttons that switch views.
         */
        function setViewSearch(event)
        {
            performSearch();
        }

        function setMyPagesView(event)
        {
            setView(PERC_FINDER_SEARCH_TYPE_MYPAGES, true);
        }

        /** set the button state for the active view */
        function set_view_options_button_state(view) {
            $('.perc-finder-view-options a').removeClass('ui-active');
            switch (view) {
                case PERC_FINDER_VIEW_COLUMN:
                    chooseColumnView.addClass('ui-active');
                    break;
                case PERC_FINDER_VIEW_LIST:
                case PERC_FINDER_SEARCH_RESULTS:
                    chooseListView.addClass('ui-active');
                    break;
                case PERC_FINDER_SEARCH_TYPE_MYPAGES:
                    chooseMyPagesView.addClass('ui-active');
                    break;
                default:
                    chooseColumnView.addClass('ui-active');
            }
        }

        /**
         * Sets the finder view to their column, list or search results view
         * @param view a String property representing the desired view.
         * @param forceSet if true sets the view even if the currentFinderView is same as supplied view.
         */
        function setView(view, forceSet)
        {
            var force = forceSet?true:false;
            MAX_RESULTS = $.perc_finderInstance.maxResults;

            // clear the view icon active state
            set_view_options_button_state(view);

            if (view === currentFinderView && !force)
                return;

            if ((currentFinderView === PERC_FINDER_SEARCH_RESULTS && view === PERC_FINDER_VIEW_LIST) ||
                (currentFinderView === PERC_FINDER_SEARCH_TYPE_MYPAGES && view === PERC_FINDER_VIEW_LIST))
                return;

            currentFinderView = view;
            var finderTable = $(".perc-finder-table");
            var allColumns = finderTable.find("td.mcol-direc");
            var lastColumn = finderTable.find("td.mcol-direc:last");

            allColumns.find("#perc-finder-listview").parents("td").remove(); //Remove the column of List View.
            //Remove the Paging Bar and resize the Finder.
            var finderDiv = $(".perc-finder");
            if (pagingBar)
            {
                pagingBar.remove();
                fixIframeHeight();
                fixHeight();

                pagingBar = null;
                // Update the lower part of Navigation/User page.
                var currentView = $.PercNavigationManager.getView();
                if (currentView === $.PercNavigationManager.VIEW_SITE_ARCH || currentView === $.PercNavigationManager.VIEW_USERS)
                {
                    fixIframeHeight();
                    fixBottomHeight();
                    fixHeight();

                    if ($("#perc_site_map").length > 0)
                    {
                        $("#perc_site_map").perc_site_map('layoutAll');
                    }
                }
            }

            $('.perc-finder-body').attr('perc-view', view);

            if (view === PERC_FINDER_VIEW_COLUMN)
            {
                allColumns.show();
                finder.open(finder.getCurrentPath());
                setColumnViewButtonOn();
            }
            else if (view === PERC_FINDER_VIEW_LIST)
            {
                percFinderListviewContainer = addListViewContainer();
                var contentPath = lastColumn.data('path');
                //if the column doesn't have path data is a summary column
                if (typeof(contentPath) === 'undefined')
                {
                    lastColumn = lastColumn.prev();
                    contentPath = lastColumn.data('path').join("/");
                }
                else
                {
                    contentPath = contentPath.join("/");
                }
                _updateListViewContainerData(contentPath, lastColumn.data('startIndex'), showPagingBar);
                setListViewButtonOn();

            }
            else if (view === PERC_FINDER_SEARCH_RESULTS || view === PERC_FINDER_SEARCH_TYPE_MYPAGES)
            {
                $(".perc-view-column-fixed a").removeClass('mcol-opened');
                $("#perc-finder-listing-Search").addClass('mcol-opened');

                var percFinderListviewContainer = addListViewContainer();
                var searchQuery = encodeURIComponent($("#perc-finder-item-search").val().trim());
                if (searchQuery === null)
                {
                    // The original code appends the following message "unformatted (it lacks the
                    // columns of the search list view) and hardcoded":
                    //$("#perc-finder-listview").append("<p>Please enter the keyword to search for</p>");
                    //return;

                    // Instead we can make an "empty string search", that shows the same result as
                    // searching for an emtpy string in the search list view
                    searchQuery = "";
                }
                percFinderListviewContainer.data('searchQuery', searchQuery);
                percFinderListviewContainer.data('startIndex', 1);
                percFinderListviewContainer.data('sortColumn', "sys_title");
                percFinderListviewContainer.data('sortOrder', "asc");
                percFinderListviewContainer.data('callback', showPagingBar);
                if(view === PERC_FINDER_SEARCH_TYPE_MYPAGES)
                {
                    percFinderListviewContainer.data('searchType', PERC_FINDER_SEARCH_TYPE_MYPAGES);
                }
                refreshView();
                setListViewButtonOn(view);
                setSearchViewButtonsStates();
                finder.setStateButtonsDesignNode(false);
                $("#mcol-path-summary").val(searchQuery);
            }
        }

        /**
         * Updates the list view containers data and calls the refreshView method to refresh the list view.
         * @param {String} contentPath the root path of the list view
         * @param {int} startIndex the starting index of the list view
         * @param {function} pagingBarCallback the pagination bar call back.
         */
        function _updateListViewContainerData(contentPath, startIndex, pagingBarCallback)
        {
            percFinderListviewContainer.data('path', contentPath);
            percFinderListviewContainer.data('startIndex', startIndex);
            percFinderListviewContainer.data('sortColumn', 'sys_title');
            percFinderListviewContainer.data('sortOrder', 'asc');
            percFinderListviewContainer.data('callback', pagingBarCallback);

            refreshView();
        }

        /**
         * Sets the corresponding styles to column and list view button. Called when changing
         * views from the setView() method.
         * @param view If the current view is Search result, prevent the default behavior of the button.
         */
        function setListViewButtonOn(view)
        {
            $("#perc-finder-choose-view #perc-finder-choose-columnview").removeClass("ui-enabled");
            $("#perc-finder-choose-view #perc-finder-choose-listview").addClass("ui-enabled");

        }

        /**
         * Sets the corresponding styles to column and list view button. Called when changing
         * views from the setView() method.
         */
        function setColumnViewButtonOn()
        {
            $("#perc-finder-choose-view #perc-finder-choose-columnview").addClass("ui-enabled");
            $("#perc-finder-choose-view #perc-finder-choose-listview").removeClass("ui-enabled");
        }

        /**
         * Sets the corresponding styles to column and list view button. Called when changing
         * views from the setView() method.
         */
        function setViewButtonsOff()
        {
            $("#perc-finder-choose-view #perc-finder-choose-listview").removeClass("ui-enabled");
        }

        function setSearchViewButtonsStates()
        {
            $.percFinderButtons().disableAllButtonsButSite();
        }
        /**
         * Calls the views confirm_if_dirty method.
         */
        function confirm_if_dirty(callback, errorCallback, options)
        {
            // use the singleton to display a confirmation dialog if they want to discard changes or not
            dirtyController.confirmIfDirty(callback, errorCallback, options);
        }

        function openAsset(assetId, isEditMode)
        {
            $.PercRecentListService.setRecent($.PercRecentListService.RECENT_TYPE_ITEM,assetId)
                .done(function(){
                    $.perc_utils.info(I18N.message("perc.ui.finder.view@Added Asset") + assetId + I18N.message("perc.ui.finder.view@Recent Item List"));
                })
                .fail(function(message){
                    $.perc_utils.error(message);
                });
            var aName = $.PercNavigationManager.getName();
            var handler = isEditMode ? $.PercAssetController.getAssetEditorForAssetId : $.PercAssetController.getAssetViewForAssetId;
            handler(assetId, function(status, assetEditorUrl)
            {
                if (status === $.PercServiceUtils.STATUS_SUCCESS)
                {
                    $.PercIFrameView.renderAssetEditor(finder, null, assetEditorUrl, null, null, false);
                    addTransitionButtons("percAsset");
                    $("#perc-revisions-button").off("click").perc_button().removeClass("ui-meta-pre-disabled").addClass("ui-meta-pre-enabled").on("click",function()
                    {
                        var isEditMode = $.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT;
                        _openRevisions(assetId, aName, isEditMode);
                    });

                    $("#perc-pubhistory-button").off("click").perc_button().removeClass("ui-meta-pre-disabled").addClass("ui-meta-pre-enabled").on("click",function()
                    {
                        _openPublishingHistory(assetId, aName);
                    });

                    // Add Publishing dropdown
                    //if ($.PercNavigationManager.getMode() == $.PercNavigationManager.MODE_EDIT)
                    //{
                    $.PercItemPublisherService.getPublishActions(assetId, function(status, result)
                    {
                        if (status)
                        {
                            var pubActions = eval("(" + result + ")").PSPublishingActionList;
                            if (pubActions.length > 0)
                            {
                                var actionNames = ["Publishing"];
                                var disableAction = [false];
                                $.each(pubActions, function()
                                {
                                    actionNames.push(this.name);
                                    disableAction.push(this.enabled);

                                });
                                //Add Publishing dropdown menu in toolbar
                                var publishNowDropdown = $("#perc-dropdown-publish-now");
                                publishNowDropdown.PercDropdown({
                                    percDropdownRootClass: "perc-dropdown-publish-now",
                                    percDropdownOptionLabels: actionNames,
                                    percDropdownCallbacks: [function()
                                    {
                                    },
                                        _publishItem, _openSchedule, _publishItem, _publishItem, _publishItem],
                                    percDropdownCallbackData: ["Publishing", {
                                        assetId: assetId,
                                        aName: aName,
                                        trName: I18N.message("perc.ui.finder.view@Publish")
                                    }, {
                                        assetId: assetId,
                                        aName: aName
                                    }, {
                                        assetId: assetId,
                                        aName: aName,
                                        trName: I18N.message("perc.ui.finder.view@Take Down")
                                    }, {
                                        assetId: assetId,
                                        aName: aName,
                                        trName: I18N.message("perc.ui.finder.view@Stage")
                                    }, {
                                        assetId: assetId,
                                        aName: aName,
                                        trName: I18N.message("perc.ui.finder.view@Remove From Staging")
                                    },
                                        assetId],
                                    percDropdownDisabledFlag: disableAction
                                });
                            }
                        }
                    });
                    //}

                }
                else
                {
                    // could not open the asset editor
                    var dlgTitle = I18N.message("perc.ui.publish.title@Error");
                    var dlgContent = assetEditorUrl;
                    if (assetEditorUrl.indexOf("must be checked out by the current user") !== -1)
                    {
                        dlgTitle = I18N.message("perc.ui.webmgt.contentbrowser.warning.title@Open Asset");
                        dlgContent = I18N.message("perc.ui.webmgt.contentbrowser.warning@Asset Overridden", [contentName]);
                    }
                    else if (assetEditorUrl.indexOf("Item not found") !== -1)
                    {
                        dlgTitle = I18N.message("perc.ui.webmgt.contentbrowser.warning.title@Open Asset");
                        dlgContent = I18N.message("perc.ui.webmgt.contentbrowser.warning@Asset Deleted", [contentName]);
                    }
                    $.perc_utils.alert_dialog({
                        title: dlgTitle,
                        content: dlgContent,
                        okCallBack: function()
                        {
                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                        }
                    });
                }
            });
        }

        /**
         * Opens the revision dialog for the supplied assetId.
         */
        function _openRevisions(assetId, assetName, isEditMode)
        {
            var mode = isEditMode ? $.PercRevisionDialog.ITEM_MODE_VIEW : $.PercRevisionDialog.ITEM_MODE_EDIT;
            $.PercRevisionDialog.open(assetId, assetName, $.PercRevisionDialog.ITEM_TYPE_ASSET, mode);
        }

        /**
         * Opens the publishing history dialog for the supplied pageId.
         */
        function _openPublishingHistory(assetId, assetName)
        {
            $.PercPublishingHistoryDialog.open(assetId, assetName, $.PercPublishingHistoryDialog.ITEM_TYPE_ASSET);
        }

        //****Workflow related functions these needs to be moved common place as Page and Asset editors share this code.***//
        //**Look for Workflow functions end **/
        /**
         * If newId exists tries to check out the page and if succeeds checks in the current page. If there is a open
         * page or asset, checks it in.
         * navigation managers notify complete method to reload the page otherwise calls with false.
         */
        function checkOutCheckInPage(newId, notificationId, notifyComplete, pathType)
        {
            if (newId != null)
            {
                var type = "percPage";
                if (pathType === $.PercNavigationManager.PATH_TYPE_ASSET)
                    type = "percAsset";
                $.PercWorkflowController().checkOut(type, newId, function(status)
                {
                    if (status)
                    {
                        //We have successfully checked out the new page
                        //Check in the current page if exists
                        if (contentId && (newId !== contentId))
                        {
                            $.PercWorkflowController().checkIn(contentId, function(status)
                            {
                                notifyComplete(notificationId, true);
                            });
                        }
                        else
                        {
                            notifyComplete(notificationId, true);
                        }
                    }
                    else
                    {
                        notifyComplete(notificationId, false);
                    }
                });
            }
            else
            {
                $.PercWorkflowController().checkIn(contentId, function(status)
                {
                    notifyComplete(notificationId, true);
                });
            }
        }

        /**
         * Makes a call to workflow controller to determine if the specified item is
         * checked out to the current user.  Invokes the appropriate callback based
         * on the result.
         *
         * @param contentId the id of the item.
         * @param yesCallback function to perform if the item is checked out to current user.
         * @param noCallback function to perform if the item is not checked out to current user.
         */
        function doIfCheckedOutToCurrentUser(contentId, yesCallback, noCallback)
        {
            $.PercWorkflowController().isCheckedOutToCurrentUser(contentId, function(result)
            {
                if (result)
                {
                    yesCallback();
                }
                else
                {
                    noCallback();
                }
            });
        }

        /**
         * Makes a call to workflow controller to determine if the specified item exists.
         * Invokes the appropriate callback based on the result.
         *
         * @param contentId the id of the item.
         * @param existsCallback function to perform if the item exists.
         * @param doesNotExistCallback function to perform if the item does not exist.
         */
        function doIfItemExists(contentId, existsCallback, doesNotExistCallback)
        {
            $.PercWorkflowController().doesItemExist(contentId, function(result)
            {
                if (result)
                {
                    existsCallback();
                }
                else
                {
                    doesNotExistCallback();
                }
            });
        }

        /**
         * Saves the currently opened object depending on the specified type.
         *
         * @param type the object type (asset, page, template).
         */
        function save(type, callback)
        {
            if (type === "asset")
            {
                var newAsset = true;
                if ($.PercNavigationManager.getId())
                {
                    newAsset = false;
                }
                $.PercIFrameView.saveContent(newAsset);
                callback();
            }
            else if (type === "page" || type === "template")
            {
                if (typeof layoutModel !== 'undefined' && layoutModel != null)
                {
                    $.PercBlockUI();
                    layoutModel.save(function()
                    {
                        $.unblockUI();
                        callback();
                    });
                }
            }
        }

        /**
         * Makes a call to workflow controller and gets the transtions and renders them as buttons.
         * Adds the click events to workflow controller's transtion method after transition is done, reloads the page
         * by calling navigation manager go to method.
         * Adds save and close buttons for assets and just close button for page. The close button closes the page/asset
         * and uses the navigation manager to switch to the dashboard.
         */
        function addTransitionButtons(itemType)
        {
            //Inner function to add the save and close buttons inside the workflow transition callback function as we
            //want these to be added as the last buttons. If the contentid is null then just adds the save and close
            //buttons.
            function addSaveAndCloseButtons()
            {
                //Add save button if it is asset view
                if ($.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT &&
                    view === $.PercNavigationManager.VIEW_EDIT_ASSET)
                {
                    var saveButton = '<button style="float: right;" name="perc_wizard_save" title="Save" class="btn btn-primary" id="perc-save-content">Save</button>';
                    $("#perc-content-menu").append($(saveButton));
                    $("#perc-save-content").on("click",function()
                    {
                        var newAsset = !contentId || contentId == null;
                        if (!newAsset)
                        {
                            doIfItemExists(contentId, function()
                            {
                                doIfCheckedOutToCurrentUser(contentId, function()
                                {
                                    $.PercIFrameView.saveContent(newAsset);
                                }, function()
                                {
                                    //an Admin has overridden the current editor in another session
                                    $.perc_utils.alert_dialog({
                                        title: I18N.message("perc.ui.common.label@Save"),
                                        content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Overridden", ["asset"]),
                                        okCallBack: function()
                                        {
                                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                        }
                                    });
                                });
                            }, function()
                            {
                                $.perc_utils.alert_dialog({
                                    title: I18N.message("perc.ui.common.label@Save"),
                                    content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Deleted", ["asset"]),
                                    okCallBack: function()
                                    {
                                        $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                    }
                                });
                            });
                        }
                        else
                        {
                            $.PercIFrameView.saveContent(newAsset);
                        }
                    });
                }
                //Add edit button if this is readonly mode
                if ($.PercNavigationManager.getMode() !== $.PercNavigationManager.MODE_EDIT)
                {
                    var editButton = '<button name="perc_page_edit" title="Edit" class="btn btn-primary" id="perc-page-edit">' +I18N.message("perc.ui.finder.view@Edit") + '</button>';
                    $("#perc-content-menu").append($(editButton));
                    currentContentPath = $.PercNavigationManager.getPath();
                    $.PercPathService.getPathItemForPath(currentContentPath, function(message, item)
                    {
                        var currentItem = item.PathItem;
                        $("#perc-page-edit").data("currentItem", currentItem).on("click",function()
                        {
                            var item = $(this).data("currentItem");
                            if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR)
                            {
                                $.PercNavigationManager.handleOpenPage(item, true);
                            }
                            else if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDIT_ASSET)
                            {
                                //We are in AssetEditor but finder is having site selected, so need to find Asset
                                if(item.name === "Sites"){
                                    $.PercPathService.getPathItemById(contentId, function(status, data){
                                        if(status === $.PercServiceUtils.STATUS_SUCCESS) {
                                            $.PercNavigationManager.handleOpenAsset(data.PathItem, true);
                                        } else {
                                            $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: data});

                                        }
                                    });
                                }else{
                                    //CMS-8107 : item.name always returned the asset name.
                                    $.PercNavigationManager.handleOpenAsset(item, true);
                                }
                            }
                            else
                            {
                                // This should never happen.
                                var eMsg = I18N.message("perc.ui.finder.view@Cannot Open Unknown View");
                                $.perc_utils.alert_dialog({
                                    title: I18N.message("perc.ui.publish.title@Error"),
                                    content: eMsg
                                });
                            }
                        });
                    });

                }
                //Add close button, this will close the editor and switch to the dashboard
                var closeButton = "<button class='btn btn-primary' id='perc-page-close' title='Close'>" +I18N.message("perc.ui.change.pw@Close") + "</button>";
                $("#perc-content-menu").append($(closeButton));
                $("#perc-page-close").on("click",function()
                {
                    if (contentId)
                    {
                        doIfItemExists(contentId, function()
                        {
                            doIfCheckedOutToCurrentUser(contentId, function()
                            {
                                confirm_if_dirty(function()
                                {
                                    $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
                                    if ($.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT)
                                    {
                                        $.PercWorkflowController().checkIn(contentId, function(status)
                                        {
                                            contentId = null;
                                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                            $.unblockUI();
                                        });
                                    }
                                    else
                                    {
                                        contentId = null;
                                        $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                        $.unblockUI();
                                    }
                                });
                            }, function()
                            {
                                //just close the content browser, an Admin has overridden the current editor in another session
                                $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                            });
                        }, function()
                        {
                            //just close the content browser, the item has been deleted in another session
                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                        });
                    }
                });
            }
            /**
             * Adds a comment icon to the menu bar if comment exists.
             */
            function addCommentsIcon(){
                $.PercRevisionService.getLastComment(contentId, function(status, result){
                    if(status === $.PercServiceUtils.STATUS_SUCCESS){
                        if(result.data && result.data.length > 0){
                            var commentIcon = $("<a style='float: right;' tooltip='" + result.data + "' class='perc-last-comment-menubar'><span class='perc-font-icon icon-comment'/></a>");
                            commentIcon.tooltip({
                                delay: 500,
                                left:-150,
                                top:25,
                                bodyHandler: function(){
                                    return "<p style='padding:5px 20px 5px 20px;'>" + result.data + "</p>";
                                }
                            });
                            $("#tooltip").css("margin-right","20px");
                            $("#perc-content-menu").append(commentIcon);
                        }
                        else{
                            $.perc_utils.info(I18N.message("perc.ui.finder.view@No Comment"));
                        }
                    }
                    else{
                        $.perc_utils.info(I18N.message("perc.ui.finder.view@Failed To Get Comment Info") + contentId + I18N.message("perc.ui.finder.view@See Server Log"));
                    }
                });
            }

            //get the transition actions and add them.
            if ($.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT &&
                contentId)
            {
                $.PercWorkflowController().getTransitions(contentId, function(status, results)
                {
                    if (status)
                    {
                        var dropdownLabels = [];
                        var dropdownParams = [];
                        var dropdownActions = [];
                        var dropdownButtonImage = "";
                        var dropdownButtonImageOver = "";
                        results.unshift(results[0]);

                        $.each(results, function(index) {
                            if(typeof results[index] !== 'undefined' ){


                                var trName = results[index].name;
                                var trClass = results[index].cssClass;
                                var trAlt = results[index].alt;
                                trClass += " perc-wf-button";
                                var trNameNormal = trName.toLowerCase().replace(/[^a-zA-Z0-9\/]/g, '_');
                                var trId = "perc_item_transition_" + trNameNormal;
                                var baseImageName = "/cm/images/images/splitButtonWf" + trNameNormal;
                                var imageExt = ".gif";
                                var regImageFilename = baseImageName + imageExt;
                                var overImageFilename = baseImageName + "Over" + imageExt;

                                if (index === 0) {
                                    defaultButtonImage = regImageFilename;
                                    defaultButtonImageOver = overImageFilename;
                                    dropdownButtonImage = '/cm/images/images/splitButtonArrow.gif';
                                    dropdownButtonImageOver = '/cm/images/images/splitButtonArrowOn.gif';

                                }
                                var param = {
                                    name: trName,
                                    contentId: contentId,
                                    itemType: itemType
                                };
                                dropdownParams.push(param);
                                dropdownLabels.push(trName);
                                dropdownActions.push(handlePageWorkflowDropdownAction);
                            }
                        });

						if(dropdownActions.length >0){
                            // Add workflow dropdown
                            var pageWorkflowDropdown = $("#perc-dropdown-page-workflow");
                            pageWorkflowDropdown.append($('<button />').html(I18N.message("perc.ui.edit.workflow.step.dialog@" + $.perc_textFilters.IDNAMECDATA(dropdownLabels[0]))).css('display', 'inline-block').addClass('btn btn-primary perc-workflow-split-button-left perc-workflow-split-button-' + $.perc_textFilters.IDNAMECDATA(dropdownLabels[0]))).append($('<div />').addClass('perc-workflow').css('display', 'inline-block'));
                            pageWorkflowDropdown.children('div').eq(0).PercDropdown({
                                percDropdownRootClass: "perc-workflow",
                                percDropdownOptionLabels: dropdownLabels,
                                percDropdownCallbacks: dropdownActions,
                                percDropdownCallbackData: dropdownParams,
                                percDropdownTitleImage: dropdownButtonImage,
                                percDropdownTitleImageOver: dropdownButtonImageOver,
                                percDropdownShowExpandIcon: false,
                                autoArrows:false,
                                cssArrows: false,
                                percDropdownResizeToElement: "#perc-dropdown-page-workflow"
                            });
                            pageWorkflowDropdown.find('.perc-dropdown-title').off('click');
                            pageWorkflowDropdown.children('a, button').on("click",function()
                            {
                                dropdownActions[0](dropdownParams[0]);
                            });
						}
                        addSaveAndCloseButtons();
                    }
                });
            }
            else
            {
                addSaveAndCloseButtons();
                addCommentsIcon();
            }
        }
        /*** Workflow functions end **/
        /* ===========================
         * Create and configure Finder
         * ===========================
         */
        // get finder reference
        //finder = $.perc_finder();

        finder.addPathChangedListener(function(p)
        {
            //If the path change, return to the default view (column view)
            if (currentFinderView === PERC_FINDER_VIEW_COLUMN)
                return;
            if (finder.flagChangeView)
                setView(PERC_FINDER_VIEW_COLUMN);
        });

        $.PercNavigationManager.registerFinder(finder);
        $.PercNavigationManager.addLocationChangeListener(function(url, id, notifyComplete, params)
        {
            // Note: notifyComplete MUST be called by the listener so that the Navigation
            // Manager knows that local processing is done by the listener and knows
            // if we should continue and actually do the location change.

            var newId = params.id;
            var modeSwitch = $.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_READONLY &&
                params.mode === $.PercNavigationManager.MODE_EDIT;
            //Alert the user if he tries to open the same page/asset.
            if (!modeSwitch && !$.PercNavigationManager.isReopenAllowed() && contentId && contentId === newId)
            {
                var options = {
                    title: I18N.message("perc.ui.finder.view@Open") + type,
                    content: I18N.message("perc.ui.finder.view@The") + type + " '" + $.PercNavigationManager.getName() + I18N.message("perc.ui.finder.view@Already Open")
                };
                $.perc_utils.alert_dialog(options);
                notifyComplete(id, false);
                return;
            }

            //Check out the new page before opening it.


            // get dirty state from the singleton where the page, template, and/or asset have updated the status
            // if they have become dirty
            dirty = dirtyController.isDirty();
            if (dirty)
            {
                // confirm
                confirm_if_dirty(function()
                {
                    if (params.mode === $.PercNavigationManager.MODE_EDIT && contentId != null)
                    {
                        checkOutCheckInPage(newId, id, notifyComplete, params.pathType);
                    }
                    else
                    {
                        // Nothing to checkout before checkin
                        checkOutCheckInPage(null, id, notifyComplete, params.pathType);
                        notifyComplete(id, true);
                    }
                    return;
                });
            }
            else
            {
                if (params.mode === $.PercNavigationManager.MODE_EDIT && contentId != null)
                {
                    checkOutCheckInPage(newId, id, notifyComplete, params.pathType);
                }
                else
                {
                    // Nothing to checkout before checkin
                    checkOutCheckInPage(null, id, notifyComplete, params.pathType);
                    notifyComplete(id, true);
                }
            }
        });
        $(".perc-finder").append(finder.elem);

        // resize the width/height of the finder implemented in perc_finder.js
        finder.on('resize', function (event, ui) {
            //Refresh the arch view
            try {
                $("#perc_site_map").perc_site_map('layoutAll');
            }catch(error){
                //Gettign Initialization error in case site not selected... needs to be ignored
            }
        });

        // initialize the finder height
        finder.update_finder_height();

        finderButtons = $.percFinderButtons().createButtons(finder, percFinderViewAPI);
        // Is there a page specified to load
        var contentId = $.PercNavigationManager.getId();
        var pageMode = $.PercNavigationManager.getMode();
        var contentName = $.PercNavigationManager.getName();
        var type = $.PercNavigationManager.getPathType();
        var view = $.PercNavigationManager.getView();

        function checkForMigrationWarnings(contentId)
        {
            /** callback handler invoked iff contentId has empty migrations */
            function onPageHasEmptyMigrationWidgets()
            {
                // show modal and clear flag
                $.perc_utils.alert_dialog({

                    title: I18N.message(I18N.message("perc.ui.page.general@Warning")),
                    content: I18N.message("perc.ui.finder.view@Content Migration Failure"),
                    okCallBack: function()
                    {
                        $.PercPageService.clearFlagShowMigrationEmptyMessage(contentId);
                    }
                });
            }
            $.PercPageService.checkForEmptyMigrationWidgets(contentId, onPageHasEmptyMigrationWidgets);
        }
        if (type === $.PercNavigationManager.PATH_TYPE_PAGE && contentId && contentName)
        {
            //Make sure to check out the page, if not able to check out do not proceed further
            if (pageMode === $.PercNavigationManager.MODE_EDIT)
            {
                $.PercWorkflowController().checkOut("percPage", contentId, function(status)
                {
                    if (!status)
                    {
                        contentId = null;
                        contentName = null;
                        var frwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-editor-frame');
                        if(frwrapper != null)
                            frwrapper.handleComponentProgress('perc-ui-component-editor-frame', "complete");
                        var tbwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-editor-toolbar');
                        if(tbwrapper != null)
                            tbwrapper.handleComponentProgress('perc-ui-component-editor-toolbar', "complete");
                        return;
                    }
                    else
                    {
                        checkForMigrationWarnings(contentId);
                        pageView.openPage(contentId, contentName);
                        addTransitionButtons("percPage");
                    }
                });
            }
            else
            {
                pageView.openPage(contentId, contentName);
                addTransitionButtons("percPage");
            }
        }
        else if (view === $.PercNavigationManager.VIEW_EDIT_ASSET && contentId)
        {
            var assetId = contentId;
            // set the name of the asset label
            var path = $.PercNavigationManager.getPath();
            //$("#perc-pageEditor-menu-name").html(assetName);
            $("#perc-page-button").html('Asset:').append("<span id='perc-pageEditor-menu-name' title = " + contentName + "> " + contentName + "</span>");
            // render asset editor
            if (pageMode === $.PercNavigationManager.MODE_EDIT)
            {
                //Make sure to check out the asset before opening it.
                $.PercWorkflowController().checkOut("percAsset", assetId, function(status)
                {
                    if (status)//Workflow controller presents the appropriate error message to the user if fails to check out.
                    {
                        openAsset(assetId, true);
                    }
                });
            }
            else
            {
                openAsset(assetId, false);
            }

        }
        else if (view === $.PercNavigationManager.VIEW_EDIT_ASSET && !contentId)
        {
            var memento = $.PercNavigationManager.getMemento();
            if (memento.widgetId) {
                $.PercNewAssetDialog.openViewer(memento.folderPath, memento.widgetId);
            }
            else{
                $.PercNewAssetDialog.open();
            }
        }
        else if(!contentId)
        {
            var frwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-editor-frame');
            if(frwrapper != null)
                frwrapper.handleComponentProgress('perc-ui-component-editor-frame', "complete");
            var tbwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-editor-toolbar');
            if(tbwrapper != null)
                tbwrapper.handleComponentProgress('perc-ui-component-editor-toolbar', "complete");
        }
        //Snippet for displaying inline help when content area is empty. Story 99.


        if (contentName != null)
        {
            $("#perc-editor-inline-help").hide();
        }

        function handlePageWorkflowDropdownAction(params)
        {
            if(params.type === 'click'){
                params = params.data;
            }

            var contentId = params.contentId;
            var itemType = params.itemType;
            var trName = params.name;

            confirm_if_dirty(function()
            {
                var type = view === $.PercNavigationManager.VIEW_EDIT_ASSET ? "asset" : "page";
                doIfItemExists(params.contentId, function()
                {
                    doIfCheckedOutToCurrentUser(contentId, function()
                    {

                        checkIfLinkedPage(contentId,itemType,type,trName);



                    }, function()
                    {
                        //an Admin has overridden the current editor in another session
                        $.perc_utils.alert_dialog({
                            title: trName,
                            content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Overridden", [type]),
                            okCallBack: function()
                            {
                                $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                            }
                        });
                    });
                }, function()
                {
                    $.perc_utils.alert_dialog({
                        title: trName,
                        content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Overridden", [type]),
                        okCallBack: function()
                        {
                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                        }
                    });
                });
            });
        }

        function showCommentsDialog(pageId,itemType,trName){
            var buttons = {};
            buttons[trName] = {
                click: function()
                {
                    userComment = $("#perc-workflow-comment").val();
                    commentDialog.remove();
                    //html encode the string - see cms-3609
                    userComment = $('<div/>').text(userComment).html();
                    userComment = encodeURIComponent(userComment);

                    commentDialog.remove();

                    doTransition(contentId,itemType,trName,userComment);

                },
                id: "perc-workflow-comment-ok"
            };
            buttons.Cancel = {
                click: function()
                {

                    commentDialog.remove();
                },
                id: "perc-workflow-comment-cancel"
            };
            var userComment = "";
            var commentDialog = $("<div><div class='perc-workflow-comment-label' data='" + trName+ "'>" +I18N.message("perc.ui.finder.view@Enter Comments Limit") + "</div><textarea id=\"perc-workflow-comment\" name=\"perc-workflow-comment\" maxlength=\"500\"></textarea></div>")
                .perc_dialog(
                    {
                        dialogClass: 'perc-workflow-comment-dialog',
                        title: I18N.message("perc.ui.finder.view@Enter Comments"),
                        modal: true,
                        resizable: false,
                        "percButtons" : buttons,
                        width:400,
                        id: "perc-workflow-comment-dialog"
                    });
        }

        function doTransition(contentId,itemType,trName,userComment){



            $.PercBlockUI();
            $.PercWorkflowController().transition(contentId, itemType, trName, userComment, function(status)
            {
                if (status)
                {
                    contentId = null;
                    $.unblockUI();
                    $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                }



            });
            $.unblockUI();

        }

        function checkIfLinkedPage(pageId,itemType,type,trName) {
            if(type === "page" && trName === "Archive" ){


                var findLinkedItemsUrl = $.perc_paths.ITEM_LINKED_TO_ITEM + "/" + pageId;
                var takeDownUrl =  $.perc_paths.PAGE_TAKEDOWN ;
                takeDownUrl+="/" + pageId;

                $.PercServiceUtils.makeJsonRequest(findLinkedItemsUrl, $.PercServiceUtils.TYPE_GET, false, function(status, result) {
                    if (status === $.PercServiceUtils.STATUS_ERROR) {
                        var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result);
                        console.error(defaultMsg);
                        showCommentsDialog(pageId,itemType,trName);

                    }
                    else {
                        if (result.data != null && result.data.ArrayList != null && result.data.ArrayList.length > 0) {
                            relatedLinkArchiveConfirm(result.data,pageId,itemType,trName);
                        }else {
                            showCommentsDialog(pageId,itemType,trName);
                        }

                    }
                }, null);
            }else{
                showCommentsDialog(pageId,itemType,trName);
            }

        }

        function relatedLinkArchiveConfirm(data,pageId,itemType,trName)
        {
            var title = I18N.message("perc.ui.publish.title@Remove From Site");
            var options = {
                title: title,
                question: createDialogQuestion(data),
                cancel: function()
                {

                },
                success: function()
                {
                    showCommentsDialog(pageId,itemType,trName);
                }
            };
            $.perc_utils.confirm_dialog(options);
        }

        function createDialogQuestion(data) {
            var dialog = I18N.message("perc.ui.publish.question@Remove From Site") + '<br /><br />';
            $.each(data.ArrayList, function (index, value) {
                if (index > 9) {
                    return false;
                }
                dialog += value.pagePath + '<br />';
            });
            return dialog;
        }


        /**
         * Schedule the item(page/asset) for the supplied pageId/assetId.
         */
        function _openSchedule(callbackData)
        {
            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var itemId = callbackData.assetId;
            var assetName = callbackData.aName;

            $.PercScheduleDialog.open(itemId, assetName);
            $(".ui-datepicker-trigger").trigger("click");
            $("#ui-datepicker-div").css('z-index', 9501).css('display', 'none');
            $("#ui-timepicker-div").css('z-index', 9501).css('display', 'none');
            $("#perc-schedule-dialog-cancel").trigger("click");

        }
        /**
         * Check if Publish date is set for item before doing immediate publishing.
         */
        function _confirmPublish(scheduleDates)
        {
            var startDate = scheduleDates.startDate;
            var itemType = view === $.PercNavigationManager.VIEW_EDIT_ASSET ? "Asset" : "Page";
            var itemId = scheduleDates.itemId;
            if (startDate !== "")
            {
                var settings = {
                    id: "perc-confirm-publish-dialog",
                    title: I18N.message("perc.ui.page.general@Warning"),
                    question: I18N.message("perc.ui.finder.view@Item Scheduled Published") + startDate + I18N.message("perc.ui.finder.view@Continue To Publish"),
                    success: function()
                    {
                        $.PercBlockUI();
                        $.PercItemPublisherService.publishItem(itemId, itemType, _afterPublish);
                    },
                    cancel: function()
                    {
                    },
                    yes: I18N.message("perc.ui.finder.view@Continue Anyway")
                };
                utils.confirm_dialog(settings);
            }
            else
            {
                $.PercBlockUI();
                $.PercItemPublisherService.publishItem(itemId, itemType, _afterPublish);
            }
        }
        /**
         * Publish/Take Down the item(page/asset) for the supplied pageId/assetId.
         */
        function _publishItem(callbackData)
        {

            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var itemId = callbackData.assetId;
            var trName = callbackData.trName;
            var view = $.PercNavigationManager.getView();
            var itemType = view === $.PercNavigationManager.VIEW_EDIT_ASSET ? "Asset" : "Page";
            confirm_if_dirty(function()
            {
                doIfItemExists(itemId, function()
                {
                    /*doIfCheckedOutToCurrentUser(itemId, function()
                    {*/
                    if (trName === I18N.message("perc.ui.navMenu.publish@Publish"))
                    {
                        $.PercItemPublisherService.getScheduleDates(itemId, function(status, result)
                        {
                            if (status)
                            {
                                var scheduleDates = eval("(" + result + ")").ItemDates;
                                _confirmPublish(scheduleDates);
                            }
                            else
                            {
                                $.perc_utils.alert_dialog({
                                    content: I18N.message("perc.ui.finder.view@Get Saved Schedule"),
                                    title: I18N.message("perc.ui.publish.title@Error")
                                });
                                return false;
                            }

                        });
                    }
                    else if (trName === I18N.message("perc.ui.page.menu@Take Down"))
                    {
                        $.PercBlockUI();
                        $.PercItemPublisherService.takeDownItem(itemId, itemType, _afterPublish);
                    }
                    else if(trName === I18N.message("perc.ui.page.menu@Stage"))
                    {
                        $.PercBlockUI();
                        $.PercItemPublisherService.publishToStaging(itemId, itemType, _afterPublish);
                    }
                    else if(trName === I18N.message("perc.ui.page.menu@Remove from Staging"))
                    {
                        $.PercBlockUI();
                        $.PercItemPublisherService.removeFromStaging(itemId, itemType, _afterPublish);
                    }

                    /*}, function()
                    {
                        //an Admin has overridden the current editor in another session
                        $.perc_utils.alert_dialog({
                            title: trName,
                            content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Overridden", [itemType]),
                            okCallBack: function()
                            {
                                $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                            }
                        });
                    });*/
                }, function()
                {
                    $.perc_utils.alert_dialog({
                        title: trName,
                        content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Deleted", [itemType]),
                        okCallBack: function()
                        {
                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                        }
                    });
                });
            });

        }

        function _afterPublish(success, results)
        {
            $.unblockUI();
            if (!success)
            {
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results[0]);
                $.perc_utils.alert_dialog({
                    title: I18N.message("perc.ui.publish.title@Error"),
                    content: defMsg
                });
            }
            else
            {
                var SitePublishResponse = results[0].SitePublishResponse;
                if (SitePublishResponse.status === $.PercItemPublisherService.PUBLISHER_JOB_STATUS_FORBIDDEN)
                {
                    $.perc_utils.alert_dialog({
                        title: I18N.message("perc.ui.finder.view@Server Publish"),
                        content: I18N.message("perc.ui.publish.errordialog.message@Publish Not Allowed")
                    });
                }
                else if (SitePublishResponse.status === $.PercItemPublisherService.PUBLISHER_JOB_STATUS_BADCONFIG_MULTIPLE_SITES)
                {
                    $.perc_utils.alert_dialog({
                        title: I18N.message("perc.ui.page.general@Warning"),
                        content: I18N.message("perc.ui.publish.errordialog.message@Bad configuration multiple sites", [SitePublishResponse.warningMessage])
                    });
                }
                else if ( SitePublishResponse.status === $.PercItemPublisherService.PUBLISHER_JOB_STATUS_NOSTAGING_SERVERS)
                {
                    $.unblockUI();
                    $.perc_utils.alert_dialog(
                        {
                            title: I18N.message("perc.ui.finder.view@Server Publish"),
                            content: I18N.message("perc.ui.finder.view@No Staging Servers Available")
                        });
                }
                else if (typeof(SitePublishResponse.warningMessage) != "undefined" && SitePublishResponse.warningMessage !== "")
                {
                    $.perc_utils.alert_dialog({
                        title: I18N.message("perc.ui.page.general@Warning"),
                        content: SitePublishResponse.warningMessage,
                        okCallBack: function()
                        {
                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                        }
                    });
                }
                else
                {
                    $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                }
            }
        }
    };

})(jQuery, jQuery.Percussion);


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
 * PercIFrameView.js
 *
 * Handles user interaction with the iFrame.
 *
 */
(function($) {
    $.PercIFrameView = {
        renderAssetEditor : renderAssetEditor,
        saveContent : saveContent
    };

    // singleton to keep track of dirty state across various types of resources such as pages, templates and assets
    var dirtyController = $.PercDirtyController;
    var initialized = false;
    var assetPath = null;
    var newAsset, workflowId, frame, folderId;

    function renderAssetEditor(finder, wfId, editorUrl, path, pathArray, isNewAsset)
    {
        // get the frame at the bottom
        frame = $("#frame");
        frame.contents().remove();
        frame.off( ".reload" );
        assetPath = path;
        newAsset = isNewAsset;
        workflowId = wfId;
        // set the content to the url param
        // we only use sys_contentid and sys_revision when editing
        // but we dont need them when creating a brand new one
        frame[0].src = editorUrl;
        /*
         * However we do need the folderid (Adam Gent)
         */
        var _re = /sys_folderid=([0-9]+)/;
        _re = _re.exec(editorUrl);
        folderId = _re && _re.length > 0 ? _re[1] : null;

        //We are now loading the form, the clear the content presubmit handlers.
        $.PercContentPreSubmitHandlers.clearHandlers();

        // after whole form has loaded, override the workflowid, add url filter to name field
        frame.on("load",function(evt)
        {
            if(initialized === false){
                initialized = true;
                onIntialFrameLoad();
            }
            else{
                onLaterFrameLoads();
            }
        });


        fixIframeHeight();

        // render the save button
        if(newAsset)
        {
            var menuId = $("#perc-layout-menu").length > 0 ?
                '#perc-layout-menu'
                : '#perc-content-menu';
            $(menuId).html("");
            $('<button name="perc_wizard_save" class="btn btn-primary" id="perc-save-content" style="float:right; background-color: #00a8df; border-color: #00a3d9; color: #ffffff; border-radius: 4px; display:inline-block; cursor:pointer; padding-top: 6px; padding-bottom: 6px; padding-left: 12px; padding-right: 12px; text-align: center; font: 13.333px Arial !important; font-weight: normal; white-space: normal; vertical-align: middle; margin-top:11.5px; border-style:outset; border-width:2px;">' +I18N.message("perc.ui.common.label@Save")+' </button>')
                .appendTo(menuId);

            // render cancel button
            $('<button class="btn btn-primary" id="perc-cancel-content" style="float:right; background-color: #00a8df; border-color: #00a3d9; color: #ffffff; border-radius: 4px; display:inline-block; cursor:pointer; padding-top: 6px; padding-bottom: 6px; padding-left: 12px; padding-right: 12px; text-align: center; font: 13.333px Arial !important; font-weight: normal; white-space: normal; vertical-align: middle;margin-top:11.5px; border-style:outset; border-width:2px ">' +I18N.message("perc.ui.change.pw@Close") +  '</button>')
                .appendTo(menuId);

            // submit the form when save button is clicked
            $("#perc-save-content").off('click').on("click",function() { saveContent(true); });

            // reset the form when cancel button is clicked
            $("#perc-cancel-content").off('click').on("click",function() { cancel(); });


        }


    }

    // cancel and clear content of form
    function cancel() {
        $.PercNavigationManager.goToDashboard();
    }
    function onLaterFrameLoads(){
        //Make sure there are no errors.
        if(frame.contents().find("#perc-content-edit-errors").length === 0)
        {
            if(newAsset)
            {
                addAssetToFolder(frame);
                $.unblockUI();
            }
            else
            {
                $.PercPathService.getPathItemById($.PercNavigationManager.getId(),
                    function(status, result){
                        if(status === $.PercServiceUtils.STATUS_SUCCESS)
                        {
                            var name = result.PathItem.name;
                            $.PercNavigationManager.setReopenAllowed(true);
                            $.PercNavigationManager.goToLocation(
                                $.PercNavigationManager.VIEW_EDIT_ASSET,
                                $.PercNavigationManager.getSiteName(),
                                $.PercNavigationManager.getMode(),
                                $.PercNavigationManager.getId(),
                                name,
                                $.PercNavigationManager.getPath(),
                                $.PercNavigationManager.PATH_TYPE_ASSET);
                            $.unblockUI();
                        }
                        else
                        {
                            $.unblockUI();
                            $.perc_utils.alert_dialog({title: 'Error', content: result});
                        }
                    });
            }
        }
        else
        {
            // re-attach the url filter to the name field, disable 'Enter' on input fields
            updateContentForm(frame.contents().find("#perc-content-form"));
            $.unblockUI();
        }
    }
    function onIntialFrameLoad(){
        if(newAsset && workflowId)
        {
            frame.contents().find("[name=sys_workflowid]").val(workflowId);
        }

        var contentForm = frame.contents().find("#perc-content-form");
        // attach the url filter to the name field, disable 'Enter' on input fields
        updateContentForm(contentForm);

        if(!newAsset)
        {
            frame.contents().find("#perc-site-impact-panel").show();
            $.PercSiteImpactView.renderSiteImpact($.PercNavigationManager.getId(), $.PercSiteImpactView.ITEM_TYPE_ASSET,frame.contents().find("#perc-site-impact-panel"));
        }
    }
    /**
     * Saves the asset content by submitting the form of the iframe. If it is new asset then gets the content id from
     * the during the iframe reload and adds it to the folder. Then reloads the browser by calling the navigation manager
     * with new path.
     * @param isNew(boolean) If true the asset is saved and added to the folder and the browser is reloaded. Otherwise
     * the asset is saved.
     */
    function saveContent(isNew)
    {
        dirtyController.setDirty(false, "asset");
        $.PercBlockUI();

        //call all the pre submit handlers if nothing returns flase, submit the form.
        var dosubmit = true;
        $.each($.PercContentPreSubmitHandlers.getHandlers(),function(){
            if(!this()){
                dosubmit = false;
            }
        });

        var showMandatoryFieldAlertPopUp=false;
        showMandatoryFieldAlertPopUp = $.perc_utils.checkMandatoryFieldsEmpty(frame);
        if(showMandatoryFieldAlertPopUp){
            dosubmit = false;
        }

        if(!dosubmit)
        {
            $.unblockUI();
            return;
        }
        //We are done processing the handlers, as we are submitting the form, clear all handlers.
        $.PercContentPreSubmitHandlers.clearHandlers();

        // the form is in the frame.
        // the form submits to containing document, i.e., submits to itself and frame is reloaded
        $(window).removeData();
        frame.removeData();
        frame.contents().find("#perc-content-form").trigger("submit");
    }

    /**
     * Helper method to load the asset with the given parameters. This is a browser reload.
     * @param folderPath assumed not null.
     * @param assetName assumed not null.
     * @param assetId assumed not null.
     *
     */
    function loadAsset(folderPath, assetName, assetId)
    {
        $.PercNavigationManager.goToLocation(
            $.PercNavigationManager.VIEW_EDIT_ASSET,
            $.PercNavigationManager.getSiteName(),
            $.PercNavigationManager.getMode(),
            assetId,assetName,folderPath + "/" + assetName,$.PercNavigationManager.PATH_TYPE_ASSET);
    }

    /**
     * Helper method to update the content form.  Adds a url filter to the name field and
     * also disables the 'Enter' key on all input fields.
     * @param form the content form assumed not null.
     */
    function updateContentForm(form)
    {
        if (folderId) {
            /*
             * We need to put the folder id in the forms action for asset renaming to work.
             */
            var oldUrl = form.attr("action");
            oldUrl = oldUrl + "?sys_folderid=" + folderId + "&sys_asset_folderid=" + folderId;
            form.attr("action", oldUrl);
        }
        var nameField = form.find("[name=sys_title]");
        if(nameField.length > 0)
        {
            $.perc_filterField(nameField, $.perc_textFilters.URL);
        }

        form.find("[type=text]").on("keypress",function(event) {
            if(event.keyCode === 13)
            {
                return false;
            }
        });
    }

    /**
     * Helper method to add an asset contained in the content form of the specified frame to the
     * current folder.  After the asset is added to the folder, the finder is opened to show the
     * asset.
     * @param frame the frame which is being loaded and contains the content form assumed not null.
     */
    function addAssetToFolder(frame)
    {
        // a hidden field contains the content id, retrieve it
        var assetContentId = frame.contents().find("[name=sys_contentid]").val();
        if(assetContentId === undefined || assetContentId === "" )
        {
            $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.iframe.view@Unable To Create Asset")});
            return;
        }

        // put the asset in the current folder
        assetContentId = "-1-101-" + assetContentId;
        let path = "//Folders/$System$/Assets" + assetPath;
        $.PercAssetController.putAssetInFolder(assetContentId, path, function(status, res)
        {
            // after putting the asset in the folder, open the finder
            // in the current folder to show the new asset
            loadAsset($.perc_paths.ASSETS_ROOT + assetPath,frame.contents().find("[name=sys_title]").val(),res.AssetFolderRelationship.assetId);
        });
    }
})(jQuery);

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
                "<input type='text' required class='required' id='perc-page-linktext' aria-required='true' name='page_linktext' maxlength='512' autofocus /> <br/> ";

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
                "<input type='text' required  class='required' id='perc-page-name' aria-required='true' name='page_name' maxlength='255'/><br/> " +
                "</fieldset>" +
                "</div><br/>";

            if(!templateId)
            {
                dialogHtml = dialogHtml + "<div style='float:left;'><label for='perc-select-template'>" +I18N.message("perc.ui.new.page.dialog@Select A Template") + "</label><br/>" +
                    "  <input list='perc-page-items-datalist' id='perc-page-item-filter' />" +
                    "  <datalist id='perc-page-items-datalist'></datalist><br/>" +
                    "<a role='button' tabindex='0' title='"+I18N.message("perc.ui.template.create@Prev")+"' class='prevPage browse left'></a>" +
                    "<div class='perc-scrollable'><input type='hidden' id='perc-select-template' name='template'/>" +
                    "<div class='perc-items'>" +
                    "</div></div>" +
                    "<a role='button' tabindex='0' title='"+I18N.message("perc.ui.common.label@Next")+"' class='nextPage browse right' ></a></div>   ";
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
 * PercNewAssetDialog.js
 *
 * Author: Jose Annunziato
 * Date: 1/21/2010
 *
 * Creates a dialog that displays a list of asset editors.
 * Selecting an editor and clicking Ok opens the editor in the bottom frame.
 * The editor is used to create and configure the new asset.
 */
(function($)
{
    $.PercNewAssetDialog = {};
    $.PercNewAssetDialog.init = function(finder, contentViewer)
    {
        var utils = $.perc_utils;
        var finderPath;
        $.PercNewAssetDialog.open = function()
        {
            var taborder = 30;
            var v;

            var dialog;
            var buttons = {};

            // create the perc_dialog with a placeholder for the selectable items
            // selectable items will go in the div with class .perc-items
            // the items are inserted by the controller
            dialog = $("<div>" +
                "<p class='perc-field-error' id='perc-save-error'></p>" +
                "<form action='' method='GET'> " +
                "  <label for='perc-select-template'>"+ I18N.message( "perc.ui.newassetdialog.label@Select Asset Type" ) + ": </label>" +
                "  <input list='perc-items-datalist' id='perc-item-filter' autofocus />" +
                "  <datalist id='perc-items-datalist'></datalist><br/>" +
                "  <a class='prevPage browse left' style = 'margin:21px 0px 30px 0px'></a>" +
                "  <div class='perc-scrollable' style = 'height:70px'>" +
                "    <input type='hidden' id='perc-select-template'   name='template'/>" +
                "    <input type='hidden' id='perc-editor-url'        name='url'/>" +
                "    <input type='hidden' id='perc-workflow-id'       name='workflowId'/>" +
                // here is where the items will be inserted by the controller
                "    <div class='perc-items'>" +
                "    </div>" +
                "  </div>" +
                "  <a class='nextPage browse right' style = 'margin:21px 0px 30px 0px' ></a>" +
                "  <div class='ui-layout-south'>" +
                "    <div id='perc_buttons' style='z-index: 100;'></div>" +
                "  </div>" +
                "</form></div>")
                .perc_dialog(
                    {
                        title: I18N.message( "perc.ui.newassetdialog.title@New Asset" ),
                        buttons: buttons,
                        percButtons:
                            {
                                "Next":
                                    {
                                        click: function()
                                        {
                                            var contentId = $.PercNavigationManager.getMemento().lastOpenedItem;
                                            //If there is a opened page or asset check it in before creating new asset.
                                            if(!utils.isBlankString(contentId))
                                            {
                                                $.PercWorkflowController().checkIn(contentId, function(status)
                                                {
                                                    _submit();
                                                });
                                            }
                                            else
                                            {
                                                _submit();
                                            }
                                        },
                                        id:"perc-page-save"
                                    },
                                "Cancel":
                                    {
                                        click: function()
                                        {
                                            _remove();
                                            if(typeof($.PercNavigationManager.getMemento().lastLocation) != 'undefined')
                                            {
                                                var bookmark = $.PercNavigationManager.getBookmark();
                                                var last = $.PercNavigationManager.getMemento().lastLocation;
                                                if (decodeURIComponent(bookmark) !== last)
                                                {
                                                    window.location.href = last;
                                                }
                                                else
                                                    $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                            }
                                        },
                                        id:"perc-page-cancel"
                                    }
                            },
                        id: "perc-new-page-dialog",
                        width: 774,
                        modal: true
                    });


            var pseudoSelect = $('#perc-select-template_perc_is');
            var selectLocalStyle = "height: 160px; width: 410px; overflow-x: scroll; overflow-y: hidden;";
            pseudoSelect.attr("style", selectLocalStyle);

            // removes the dialog box when cancel is clicked
            function _remove()
            {
                dialog.remove();
            }


            function _submit()
            {
                dialog.find('form').trigger("submit");
            }

            // add a validator to the form
            // intercepts submit event and performs function in submitHandler
            // we are not going to actually submit the form,
            // instead we will get a hold of the URL associated with the selected editor
            // and open the editor in the #frame down below
            v = dialog.find('form').validate(
                {
                    errorClass: "perc-field-error",
                    validClass: "perc-field-success",
                    wrapper: "p",
                    validateHiddenFields: false,
                    debug: false,
                    submitHandler: function(form)
                    {
                        var editorUrl = $("#perc-editor-url").val( );
                        var workflowId = $("#perc-workflow-id").val( );

                        $.perc_pathmanager.open_containing_folder( finderPath, function( fspec, pathArray )
                        {
                            // close dialog
                            dialog.remove();

                            path = pathArray.toString().replace(/,/g,"/");
                            pathtemp = path;
                            path = path.replace($.perc_paths.ASSETS_ROOT,"");

                            // get the folder id
                            $.PercAssetController.getPathItemForPath(pathtemp, function(pathItemObj)
                            {
                                // the folderId format is ########-###-###
                                var folderId = pathItemObj.id;

                                // we only care about the last 3 digits after the last dash '-'
                                // because of a legacy representation
                                var oldFolderId = folderId.substring(folderId.lastIndexOf("-")+1);

                                // append the folder id to the URL of the form
                                //Adam Gent (added sys_asset_folderid for PSAddNewItemToFolder Core Extension)
                                editorUrl += "&sys_folderid="+oldFolderId+"&sys_asset_folderid="+oldFolderId+"&sys_workflowid="+workflowId;

                                // render the form editor in the bottom frame
                                $.PercIFrameView.renderAssetEditor(finder, workflowId, editorUrl, path, pathArray, true);
                                $("#perc-page-button").html('Asset:').append("<span id='perc-pageEditor-menu-name'> (New Asset)</span>" );
                                $.PercNavigationManager.clearId();

                                //Hide the inline help when in Editor mode and fix the iframe height.
                                $("#perc-editor-inline-help").hide();
                                fixIframeHeight();
                            });
                        });
                    }
                });

            // the controller uses the service to query for the list of editors
            // and then creates a datastructure and passes it to the callback
            $.PercAssetController.getAssetEditorLibrary(getCurrentFolderPath(), function(assetEditorLibrary)
            {
                // get the placeholder where to insert the selectable items
                var itemContainer = dialog.find('div.perc-scrollable div.perc-items');
                var datalistContainer = dialog.find('#perc-items-datalist');
                datalistContainer.empty();
                // iterate over the editors, create a div for each, and then insert it in div.perc-items
                let index = 0;
                $("div.perc-scrollable").scrollable(
                    {
                        items: ".perc-items",
                        size: 3,
                        keyboard: true
                    });

                for(a in assetEditorLibrary)
                {
                    // get the datastructure for the editor
                    var assetEditor = assetEditorLibrary[a];

                    // create the div and insert it into the place holder
                    itemContainer.append(createAssetEditorEntry(assetEditor));
                    datalistContainer.append(createAssetEditorListEntry(assetEditor,index))


                    //Wire the keydown event
                    $(".perc-items .item").on("keydown", function(event){
                        event.stopPropagation();
                        event.stopImmediatePropagation();

                        if(event.code == "Enter" || event.code == "Space"){
                            $(this).dblclick();
                        }

                    });

                    // bind click event to each item to handle selection
                    // each div has hidden inner divs with data specific to each item
                    // when the user clicks on the selection, get the values in the hidden inner divs
                    // and assign the values to the hidden input fields of the form
                    // the form will be submitted with the selected item's values in the hidden input fields
                    $(".perc-items .item").on('click', function(event)
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

                    $(".perc-items .item .item-id").hide();
                    $(".perc-items .item .item-editor-url").hide();
                    $(".perc-items .item .item-workflow-id").hide();

                    // select first item by default
                    $firstItem = $(".perc-items .item:first");
                    $("#perc-select-template").val($firstItem.find(".item-id").text());
                    $("#perc-editor-url").val($firstItem.find(".item-editor-url").text());
                    $("#perc-workflow-id").val($firstItem.find(".item-workflow-id").text());
                    $firstItem.addClass("perc-selected-item");
                    index++;
                }
            });

            $("#perc-item-filter").on("keydown",function(event){
                event.stopPropagation();
                event.stopImmediatePropagation();

                if(event.key==="Escape"){
                    $(this).val("");
                }

            });

            $("#perc-item-filter").on("change",function(event){
                let scroll = $("div.perc-scrollable").scrollable();
                let idx = 0;
                $('#perc-items-datalist').children('option').each(function () {
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


            var nm = $('#perc-page-name');
            var ti = $('#perc-page-title');
            var url = $('#perc-page-url');

            /**
             * Url filter function to allow only url valid chars.
             * @param txt the text to be filtered.
             */
            function url_filter( txt )
            {
                return txt.replace( /\s/g, '-' ).replace( /[^\$\{\}\^\[\]\`\=\,\;\`a-zA-Z0-9\-]/g, '' );
            }

            /**
             * Slash filter function to remove backslash chars.
             * @param txt the text to be filtered.
             */
            function slash_filter( txt)
            {
                return txt.replace(/\\/g, '');
            }

            $.perc_textAutoFill(nm, ti, slash_filter);
            $.perc_textAutoFill(nm, url, url_filter);
            $.perc_textAutoFill(nm, '#perc-page-linktitle', slash_filter);

            $.perc_filterField(nm, slash_filter);
            $.perc_filterField(ti, slash_filter);
            $.perc_filterField(url, url_filter);
            $.perc_filterField('#perc-page-linktitle', slash_filter);
        };// End open dialog

        $.PercNewAssetDialog.openViewer = function(folderPath,widgetId){
            $.PercAssetService.getAssetEditorForWidgetAndFolder(folderPath, widgetId, function(status, result){
                if(status === $.PercServiceUtils.STATUS_ERROR){
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.new.asset.dialog@Unknown Error Asset")});
                    return;
                }
                var assetEditor = result.AssetEditor.pop();
                var editorUrl = assetEditor.url;
                var workflowId = assetEditor.workflowId;
                path = folderPath.replace($.perc_paths.ASSETS_ROOT,"");
                // get the folder id
                $.PercAssetController.getPathItemForPath(folderPath, function(pathItemObj)
                {
                    // the folderId format is ########-###-###
                    var folderId = pathItemObj.id;

                    // we only care about the last 3 digits after the last dash '-'
                    // because of a legacy representation
                    var oldFolderId = folderId.substring(folderId.lastIndexOf("-")+1);

                    // append the folder id to the URL of the form
                    //Adam Gent (added sys_asset_folderid for PSAddNewItemToFolder Core Extension)
                    editorUrl += "&sys_folderid="+oldFolderId+"&sys_asset_folderid="+oldFolderId+"&sys_workflowid="+workflowId;

                    // render the form editor in the bottom frame
                    $.PercIFrameView.renderAssetEditor(finder, workflowId, editorUrl, path, folderPath.split("/"), true);
                    $("#perc-page-button").html('Asset:').append("<span id='perc-pageEditor-menu-name'> (New Asset)</span>" );
                    $.PercNavigationManager.clearId();

                    //Hide the inline help when in Editor mode and fix the iframe height.
                    $("#perc-editor-inline-help").hide();
                    fixIframeHeight();
                });

            });
        };

        function check_for_dirty_page(event)
        {
            $.PercFolderHelper().getAccessLevelByPath(finderPath.join('/'),false,function(status, result){
                if(status === $.PercFolderHelper().PERMISSION_ERROR || result === $.PercFolderHelper().PERMISSION_READ)
                {
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.page.general@Warning"), content: I18N.message("perc.ui.new.asset.dialog@Permission For Asset")});

                }
                else
                {
                    var currentItem = finder.getCurrentItem();
                    var folderPath = "";
                    if (currentItem != null){
                        folderPath = currentItem.folderPaths;
                        //if the current item is a Folder select the current path.
                        if (currentItem.type === "Folder"){
                            folderPath = currentItem.folderPath;
                        }
                    }
                    $.PercUserService.getAccessLevel(null,-1,function(status, result){
                        if(status === $.PercServiceUtils.STATUS_ERROR || result === $.PercUserService.ACCESS_READ || result === $.PercUserService.ACCESS_NONE)
                        {
                            $.perc_utils.alert_dialog({title: I18N.message("perc.ui.new.asset.dialog@New Asset"), content: I18N.message("perc.ui.new.asset.dialog@Not Authorized Asset")});

                        }
                        else
                        {
                            contentViewer.confirm_if_dirty(function(){
                                if($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDIT_ASSET)
                                    $.PercNavigationManager.clearMemento();
                                $.PercNavigationManager.goToLocation(
                                    $.PercNavigationManager.VIEW_EDIT_ASSET,
                                    $.PercNavigationManager.getSiteName(),
                                    'edit',
                                    null,
                                    null,
                                    $.PercNavigationManager.getPath(),
                                    $.PercNavigationManager.PATH_TYPE_ASSET,
                                    {lastLocation: $.PercNavigationManager.getBookmark(), lastOpenedItem:$.PercNavigationManager.getId()});
                            });
                        }
                    }, folderPath);
                }
            });
        }


        /**
         * Generates the html for a new datalist entry for the current asset type
         * @param {*} assetEditor
         * @param {*} idx The index of the editor in the overall list
         */
        function createAssetEditorListEntry(assetEditor, idx){
            return "<option value='" + assetEditor.title  + "' />";
        }

        function createAssetEditorEntry(assetEditor)
        {
            var temp =    "<button type='button' class='item' id='@ITEM_ID@'>" +
                "   <div class='item-id'>@ITEM_ID@</div>" +
                "   <div class='item-editor-url'>@ITEM_URL@</div>" +
                "   <div class='item-workflow-id'>@ITEM_WORKFLOW_ID@</div>" +
                "   <table style='vertical-align:middle'>" +
                "       <tr><td align='center' valign='middle'>" +
                "           <img src='/Rhythmyx@IMG_SRC@'/>" +
                "       </td><td style='vertical-align:middle'><span>@ITEM_LABEL@</span></td></tr>" +
                "   </table>";
            "</button>";
            return temp.replace(/@IMG_SRC@/, assetEditor.icon)
                .replace(/@ITEM_ID@/g, assetEditor.title)
                .replace(/@ITEM_URL@/, assetEditor.url)
                .replace(/@ITEM_WORKFLOW_ID@/, assetEditor.workflowId)
                .replace(/@ITEM_LABEL@/, assetEditor.title);
        }


        function getCurrentFolderPath()
        {
            return $.deparam.querystring().path;
        }

        var newAssetButton = $('<a id="mcol-new-asset" class="perc-font-icon" title="'+I18N.message("perc.ui.new.asset.dialog@Click New Asset")+'" href="#" class="ui-disabled"><span class="icon-plus fas fa-plus"></span><span class="icon-file-alt fas fa-file"></span></a>').perc_button();
        function updateBtn(path)
        {
            finderPath = path;

            // If current view is Search then keep the button disabled (since no path to create is defined in Finder)
            if ($.Percussion.getCurrentFinderView() === $.Percussion.PERC_FINDER_SEARCH_RESULTS || $.Percussion.getCurrentFinderView() === $.Percussion.PERC_FINDER_RESULT)
            {
                enableButton(false);
            }
            else if(path[1] === $.perc_paths.ASSETS_ROOT_NO_SLASH)
            {
                newAssetButton.show();
                enableButton(true);
                if(path.length < 3)
                {
                    enableButton(false);
                }
                else
                {
                    $.PercFolderHelper().getAccessLevelByPath(path.join('/'),true,function(status, result){
                        if(status === $.PercFolderHelper().PERMISSION_ERROR || result === $.PercFolderHelper().PERMISSION_READ)
                        {
                            enableButton(false);
                        }
                    });
                }
            }
            else
            {
                if(path[1] === $.perc_paths.DESIGN_ROOT_NO_SLASH || path[1] === $.perc_paths.SITES_ROOT_NO_SLASH || path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH)
                {
                    newAssetButton.hide();
                }
            }
        }

        /**
         * Helper function to enable or disable the new asset button on finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            if(flag){
                newAssetButton.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    function(evt){
                        check_for_dirty_page(evt);
                    } );
            }
            else{
                newAssetButton.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
        }

        finder.addPathChangedListener( updateBtn );
        return newAssetButton;
    };
})(jQuery);

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
 * PercPageView.js
 *
 * Handles user interaction with Edit page.
 *
 * (*) Iframe
 * (*) Handles dirty page and confirmation when changing tabs
 * (*) Loads content and layout tabs
 *
 */
(function($, P)
{
    var CONTENT_TAB = 0;
    var LAYOUT_TAB = 1;
    var STYLES_TAB = 2;
    var view = $.PercNavigationManager.getView();
    var dialogFlag = true;
    var querystring = $.deparam.querystring();
    var pageMode = $.PercNavigationManager.getMode();
    var isAdmin = $.PercNavigationManager.isAdmin();
    var isDesigner = $.PercNavigationManager.isDesigner();

    P.pageView = function()
    {
        $(".perc-page-status").hide();
        $(".perc-page-name").hide();

        // singleton to keep track of dirty state across various types of resources such as pages, templates and assets
        var dirtyController = $.PercDirtyController;

        if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR)
        {
            $.perc_iframe_fix($('#frame'));
        }

        var utils = $.perc_utils;
        var pageModel;
        var currentPageId; // current page id being edited
        var unassigned = "UNASSIGNED"; // UNASSIGNED template type
        var dirty = false; // dirty page
        // Interface to local API to pass around to Finder and Page Edit Dialog
        // so they can call back and update the Content tab
        var pageViewAPI = {
            resetPageName: resetPageName,
            reload: function()
            {
                currentTabIndex = $('#perc-pageEditor-tabs').tabs('option','active');
                if(typeof currentTabIndex === 'undefined'){
                    currentTabIndex = CONTENT_TAB;
                }
                loadTab(currentTabIndex,true);
            },
            getPageId: function()
            {
                return currentPageId;
            },
            clear: function()
            {
                $('#frame').each(function()
                {
                    this.open();
                    this.close();
                });
            },
            confirm_if_dirty: confirm_if_dirty,
            openPage: openPage,
            isDirty: isDirty
        };

        // hide region tool for pages
        $("#region-tool, #region-tool-help, #perc-region-tool-inspector, #perc-region-tool-menu, #perc-error-alert").css("visibility", "hidden");

        function openPage(pageId, pageName)
        {
            try {
                $.PercRecentListService.setRecent($.PercRecentListService.RECENT_TYPE_ITEM, pageId).done(function(){
                    $.perc_utils.info(I18N.message("perc.ui.page.recent@Added Recent Page", pageId));
                }).fail(function(message){
                    $.perc_utils.error(message);
                });
            }
            catch(err){
                $.perc_utils.error(I18N.message("perc.ui.page.recent@Error Adding Recent Page",pageId));
            }
            currentPageId = pageId;
            $(".perc-page-status").show();
            $(".perc-page-name").show();
            getPageStatus(currentPageId);
            var pglabel = $.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT?I18N.message('perc.ui.page.label@Editing Page'):I18N.message('perc.ui.page.label@Viewing Page');
            var pgType = $.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT?"page-editing":"page-viewing";
            if (pageName)
            {
                var titleValue = $.PercNavigationManager.getPath();
                $(".perc-page-name-name").html('<span class = "perc-title-value"><span class = "perc-page-name-text">' + pageName + '</span></span>');
                $(".perc-title-value").attr("title", titleValue);
                $(".perc-title-value").prepend('<span class = "perc-page-name-label">' + pglabel + '</span>');
                $(".perc-page-details").attr("type",pgType);
                $("#perc-pageEditor-menu-name").html(pageName);
                _addMyPagesAction(pageId);
                $(".perc-page-details").show();
            }

            /* If the default tab happens to already be selected, the content will not be loaded, so we do that below. */
            var $tabs = $("#perc-pageEditor-tabs");
            var currentTabIndex = $tabs.tabs('option','active');
            var defaultTabIndex = 0;

            if ($.PercNavigationManager.getMode() == $.PercNavigationManager.MODE_EDIT)
            {
                pageModel = P.pageModel($.perc_pagemanager, $.perc_templatemanager, pageId, function()
                {
                    // enable all tabs and select the first tab
                    $tabs.data('disabled.tabs', []).tabs("option", "active", defaultTabIndex);
                    $("#perc-pageEditor-tabs").find("li").each(function(i)
                    {
                        // Don't enable Layout and Style tabs if template type is UNASSIGNED
                        if (pageModel.getTemplateModel().getTemplateType() !== unassigned)
                        {
                            $(this).removeClass('ui-state-disabled');
                        }
                    });

                    // Don't enable Edit Template action if template type is UNASSIGNED
                    var enableMenu = pageModel.getTemplateModel().getTemplateType() !== unassigned;
                    if (isAdmin || isDesigner)
                    {
                        actionsDropdown.PercDropdown(
                            {
                                percDropdownRootClass: "perc-dropdown-page-actions",
                                percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@Edit Metadata"), I18N.message("perc.ui.page.menu@Edit Template"), I18N.message("perc.ui.page.menu@Change Template")],
                                percDropdownCallbacks: [function()
                                {}, function()
                                {
                                    $.perc_page_edit_dialog($.perc_finder(), pageViewAPI, currentPageId);
                                },
                                    _loadTemplate, _changeTemplate],
                                percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@Edit Metadata"), I18N.message("perc.ui.page.menu@Edit Template"), I18N.message("perc.ui.page.menu@Change Template")],
                                percDropdownDisabledFlag: [false, true, enableMenu, true]
                            });
                    }
                    else
                    {
                        actionsDropdown.PercDropdown(
                            {
                                percDropdownRootClass: "perc-dropdown-page-actions",
                                percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@Edit Metadata"), I18N.message("perc.ui.page.menu@Change Template")],
                                percDropdownCallbacks: [function()
                                {}, function()
                                {
                                    $.perc_page_edit_dialog($.perc_finder(), pageViewAPI, currentPageId);
                                },
                                    _changeTemplate],
                                percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@Edit Metadata"), I18N.message("perc.ui.page.menu@Change Template")],
                                percDropdownDisabledFlag: [false, true, true]
                            });
                    }

                    addMoreMenus();
                });
            }

            // Enable the Metadata
            $("#perc-metadata-button").off("click").perc_button().removeClass("ui-meta-pre-disabled").addClass("ui-meta-pre-enabled").on("click",function()
            {
                $.perc_page_edit_dialog($.perc_finder(), pageViewAPI, currentPageId);
            });

            // Add view dropdown on content tab
            var actionsDropdown = $("#perc-dropdown-actions");
            var viewDropdown = $("#perc-dropdown-view");


            if ($.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_READONLY)
            {
                //Action drop down menu in page readonly mode
                actionsDropdown.PercDropdown(
                    {
                        percDropdownRootClass: "perc-dropdown-page-actions",
                        percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@View Metadata")],
                        percDropdownCallbacks: [function()
                        {}, function()
                        {
                            $.perc_page_edit_dialog($.perc_finder(), pageViewAPI, currentPageId);
                        }],
                        percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@View Metadata")],
                        percDropdownDisabledFlag: [false, true]
                    });

                addMoreMenus();
            }

            /**
             * Sets the appropriate class to my pages, to indicate whether the page has already been added to the user pages or not,
             * Adds either Add to my pages click event or remove from my pages click event.
             * @param {Object} pageId assumed to be the currently opened page id.
             */
            function _addMyPagesAction(pageId)
            {
                $.PercPageService.isMyPage(pageId, function(status, result){
                    if(status === $.PercServiceUtils.STATUS_ERROR)
                    {
                        $(".perc-my-pages-action").addClass("perc-my-pages-error").attr("title", I18N.message("perc.ui.page.mypages@Error retrieving status"));
                    }
                    else
                    {
                        if(result)
                        {
                            $(".perc-my-pages-action").addClass("perc-remove-from-my-pages").attr("title", I18N.message("perc.ui.page.mypages@Remove from My Pages")).on("click",function(){_removeFromMyPages(pageId);});

                        }
                        else
                        {
                            $(".perc-my-pages-action").addClass("perc-add-to-my-pages").attr("title", I18N.message("perc.ui.page.mypages@Add to My Pages")).on("click",function(){_addToMyPages(pageId);});
                        }
                    }
                });
            }

            /**
             * Makes a call to the service to add to my pages and if successful toggles the class and click event to remove pages.
             * @param {Object} pageId assumed to be the currently opened page id.
             */
            function _addToMyPages(pageId)
            {
                $.PercPageService.addToMyPages(pageId, function(status, result){
                    if(status == $.PercServiceUtils.STATUS_ERROR)
                    {
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.labels@Error"),content: result});
                    }
                    else
                    {
                        $(".perc-my-pages-action").removeClass("perc-add-to-my-pages").addClass("perc-remove-from-my-pages").attr("title", I18N.message("perc.ui.page.mypages@Remove from My Pages")).off("click").on("click",function(){_removeFromMyPages(pageId);});
                    }
                });
            }

            /**
             * Makes a call to the service to remove from my pages and if successful toggles the class and click event to add pages.
             * @param {Object} pageId assumed to be the currently opened page id.
             */
            function _removeFromMyPages(pageId)
            {
                $.PercPageService.removeFromMyPages(pageId, function(status, result){
                    if(status === $.PercServiceUtils.STATUS_ERROR)
                    {
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.labels@Error"),content: result});
                    }
                    else
                    {
                        $(".perc-my-pages-action").removeClass("perc-remove-from-my-pages").addClass("perc-add-to-my-pages").attr("title", I18N.message("perc.ui.page.mypages@Add to My Pages")).off("click").on("click",function(){_addToMyPages(pageId);});
                    }
                });
            }

            function addMoreMenus()
            {
                if (isAdmin || isDesigner)
                {
                    // Add action dropdown on layout tab
                    var layoutActionsDropdown = $("#perc-dropdown-actions-layout");
                    layoutActionsDropdown.PercDropdown(
                        {
                            percDropdownRootClass: "perc-dropdown-actions-layout",
                            percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@Edit Template"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownCallbacks: [function()
                            {},
                                _loadTemplate, _changeTemplate],
                            percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@Edit TemplateEdit Template"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownDisabledFlag: [false, true, true]
                        });

                    // Add action dropdown on style tab
                    var styleActionsDropdown = $("#perc-dropdown-actions-style");
                    styleActionsDropdown.PercDropdown(
                        {
                            percDropdownRootClass: "perc-dropdown-actions-style",
                            percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@Edit Template"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownCallbacks: [function()
                            {},
                                _loadTemplate, _changeTemplate],
                            percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@Edit Template"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownDisabledFlag: [false, true, true]
                        });
                }
                else
                {
                    // Add action dropdown on layout tab
                    let layoutActionsDropdown = $("#perc-dropdown-actions-layout");
                    layoutActionsDropdown.PercDropdown(
                        {
                            percDropdownRootClass: "perc-dropdown-layout-actions",
                            percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownCallbacks: [function()
                            {},
                                _changeTemplate],
                            percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownDisabledFlag: [false, true]
                        });

                    // Add action dropdown on style tab
                    let styleActionsDropdown = $("#perc-dropdown-actions-style");
                    styleActionsDropdown.PercDropdown(
                        {
                            percDropdownRootClass: "perc-dropdown-actions-style",
                            percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@Actions"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownCallbacks: [function()
                            {},
                                _changeTemplate],
                            percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Action"), I18N.message("perc.ui.page.menu@Change Template")],
                            percDropdownDisabledFlag: [false, true]
                        });
                }

                // Add view dropdown on layout tab
                var layoutViewDropdown = $("#perc-dropdown-view-layout");
                layoutViewDropdown.PercDropdown(
                    {
                        percDropdownRootClass: "perc-dropdown-view-layout",
                        percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@View"), I18N.message("perc.ui.menu@JavaScript Off"), I18N.message("perc.ui.page.menu@Hide Guides")],
                        percDropdownCallbacks: [function()
                        {}, function()
                        {}, function()
                        {}],
                        percDropdownCallbackData: [I18N.message("perc.ui.page.menu@View"), I18N.message("perc.ui.page.menu@View"), I18N.message("perc.ui.page.menu@View")],
                        percDropdownDisabledFlag: [false, true, true]
                    });
                //Add View dropdown under Style tab
                var styleViewDropdown = $("#perc-dropdown-view-style");
                styleViewDropdown.PercDropdown(
                    {
                        percDropdownRootClass: "perc-dropdown-view-style",
                        percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@View"), I18N.message("perc.ui.menu@JavaScript Off")],
                        percDropdownCallbacks: [function()
                        {}, function()
                        {}],
                        percDropdownCallbackData: [I18N.message("perc.ui.page.menu@View"), I18N.message("perc.ui.menu@JavaScript Off")],
                        percDropdownDisabledFlag: [false, true]
                    });
                // Add Publishing dropdown
                $.PercItemPublisherService.getPublishActions(currentPageId, function(status, result)
                {
                    if (status)
                    {
                        var pubActions = eval("(" + result + ")").PSPublishingActionList;
                        if (pubActions.length > 0)
                        {
                            var actionNames = [I18N.message("perc.ui.page.menu@Publishing")];
                            var disableAction = [false];
                            $.each(pubActions, function()
                            {
                                actionNames.push(this.name);
                                disableAction.push(this.enabled);
                            });
                            //Add Publishing dropdown menu in toolbar
                            var publishNowDropdown = $("#perc-dropdown-publish-now");
                            publishNowDropdown.PercDropdown(
                                {
                                    percDropdownRootClass: "perc-dropdown-publish-now",
                                    percDropdownOptionLabels: actionNames,
                                    percDropdownCallbacks: [function()
                                    {},
                                        _publishItem, _openSchedule, _publishItem, _publishItem, _publishItem],
                                    percDropdownCallbackData: [I18N.message("perc.ui.page.menu@Publishing"), {
                                        pageId: currentPageId,
                                        pageName: pageName,
                                        trName: I18N.message("perc.ui.page.menu@Publish")
                                    }, {
                                        pageId: currentPageId,
                                        pageName: pageName,
                                        trName: I18N.message("perc.ui.page.menu@Publish")
                                    }, {
                                        pageId: currentPageId,
                                        pageName: pageName,
                                        trName: I18N.message("perc.ui.page.menu@Take Down")
                                    }, {
                                        pageId: currentPageId,
                                        pageName: pageName,
                                        trName: I18N.message("perc.ui.page.menu@Stage")
                                    }, {
                                        pageId: currentPageId,
                                        pageName: pageName,
                                        trName: I18N.message("perc.ui.page.menu@Remove from Staging")
                                    },
                                        currentPageId],
                                    percDropdownDisabledFlag: disableAction
                                });
                        }
                    }
                });
                $.PercAssetService.getUnusedAssets(currentPageId, populateOrphanAssets);
                // View dropdowm in editmode
                if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR){
                    var viewDropDownData =
                        {
                            percDropdownRootClass: "perc-dropdown-page-view",
                            percDropdownOptionLabels: [I18N.message("perc.ui.page.menu@View"), I18N.message("erc.ui.page.menu@Revisions"), I18N.message("perc.ui.page.menu@Comments"), I18N.message("perc.ui.page.menu@Preview"), I18N.message("perc.ui.page.menu@Publishing History"), I18N.message("perc.ui.menu@JavaScript Off")],
                            percDropdownCallbacks: [$.noop,_openRevisions, _openComments, _previewPage, _openPublishingHistory, $.noop],
                            percDropdownCallbackData: [I18N.message("perc.ui.page.menu@View"), {
                                pageId: currentPageId,
                                pageName: pageName
                            },{
                                pageId: currentPageId,
                                pageName: pageName
                            },
                                currentPageId, {
                                    pageId: currentPageId,
                                    pageName: pageName
                                },I18N.message("perc.ui.menu@JavaScript Off")],
                            percDropdownDisabledFlag: [false, true, true, true, true, true]
                        };

                    viewDropdown.PercDropdown(viewDropDownData);
                }

                // Add workflow transition buttons
                if (currentTabIndex === defaultTabIndex) loadTab(defaultTabIndex);

                // Init Orphan Assets Menu Actions
                initOrphanAssetsMenu();
            }
        }



        //handle Hover In image for unusedAssets
        function handleIn()
        {
            var self = $(this);
            self.attr("src", self.data("overIconSrc"));
        }

        //handle Hover Out image for unusedAssets
        function handleOut()
        {
            var self = $(this);
            self.attr("src", self.data("iconSrc"));
        }

        //Populate the Unused Assets tray
        function populateOrphanAssets(status, unusedAssets)
        {
            $("#perc_orphan_assets_expander").show();

            if (status === $.PercServiceUtils.STATUS_ERROR)
            {
                $("#perc_orphan_assets_maximizer").addClass("perc-disabled");
                $("#perc_orphan_assets_expander").addClass("perc-disabled");
                $.perc_utils.alert_dialog(
                    {
                        title: I18N.message("perc.ui.publish.title@Error"),
                        content: unusedAssets
                    });
                return;
            }

            if (unusedAssets.length > 0)
            {
                var orphanAssetsContainer = $(".perc-orphan-assets-list");
                orphanAssetsContainer.empty();
                var htmlAssets = "";
                for (let i = 0; i < unusedAssets.length; i++)
                {
                    var asset = unusedAssets[i];
                    var hoverText = I18N.message("perc.ui.page.general@Local");
                    var folderPaths = "";
                    if (typeof(asset.folderPaths) != "undefined" && asset.folderPaths.length > 0)
                    {
                        hoverText = asset.folderPaths[0].replace("//Folders/$System$", "") + "/" + asset.name;
                        folderPaths = asset.folderPaths;
                    }
                    orphanAssetsContainer.append(
                        $("<a />").attr("alt", hoverText).attr("title", hoverText).addClass("perc-orphan-asset").data('spec', {
                            "type": asset.type,
                            "id": asset.id,
                            "relationshipId": asset.relationshipId
                        }).append(
                            $("<div />").css("position", "relative").addClass("perc-widget-tool").append(
                                $("<img />").attr("src", asset.icon).data("iconSrc", asset.icon).data("overIconSrc", asset.overIcon).on('mouseenter',
                                    function(e){
                                        handleIn(e);
                                    }).on('mouseleave', function(e){
                                    handleOut(e);
                                })).append(
                                $("<div />").css("overflow", "hidden").addClass("perc-asset-label").append(
                                    $("<nobr />").html(asset.title)))).css('cursor', 'pointer').data('assetId', asset.id).data('assetType', asset.type).data('assetFolderPaths', folderPaths).data('widgetId', asset.widgetId).off("click").on("click",
                            function(evt){
                                selectOrphanAsset(evt,this);
                            })
                    );
                }
                // populates Orphan Assets tray and toggles it open/close
                $("#perc_orphan_assets_expander").off().on("click",function()
                {
                    clearSelection();
                    $.fn.percOrphanAssetsMaximizer(P);
                });

                // Set drag&drop behavior.
                orphanAssetsContainer.find('.perc-orphan-asset').draggable(
                    {
                        helper: function()
                        {
                            var helper = $(this).clone();
                            helper.find(".perc-asset-label").css('padding', "0px 0px 5px 5px");
                            return helper;
                        },
                        appendTo: 'body',
                        iframeFix:true,
                        refreshPositions: true,
                        zIndex: 9990,
                        revert: true,
                        revertDuration: 0,
                        delay: 25,
                        containment: "window",
                        scope : $.perc_iframe_scope,
                        scroll: true
                    });

                $("#perc_orphan_assets_maximizer").removeClass("perc-disabled");
                $("#perc_orphan_assets_expander").removeClass("perc-disabled");
            }
            else
            {
                $("#perc_orphan_assets_maximizer").addClass("perc-disabled");
                $("#perc_orphan_assets_expander").addClass("perc-disabled");
            }
        }
        // A snippet to adjust the frame size on resizing the window.
        $(window).on("resize",function()
        {
            fixIframeHeight();
        });

        //------------------------//
        // Orphan Assets
        //------------------------//
        function initOrphanAssetsMenu()
        {
            $("#perc_asset_library").disableSelection();
            $(".perc-ui-delete-asset").attr("src", "/cm/images/icons/editor/deleteInactive.png").off();
            $(".perc-ui-edit-asset").attr("src", "/cm/images/icons/editor/editInactive.png").off();
        }

        function deleteOrphanAsset(event)
        {
            var assets = $("#perc_asset_library").find(".perc-orphan-assets-list");
            var unused = assets.find(".perc-orphan-assets-selected");
            var orphanIds = [];
            var orphanTypes = [];
            var orphanIsShared = [];
            var orphanWidgetsIds = [];
            for (var i = 0; i < unused.length; i++)
            {
                orphanIds.push($(unused[i]).data('assetId'));
                orphanTypes.push($(unused[i]).data('assetType'));
                orphanWidgetsIds.push($(unused[i]).data('widgetId'));
                var folderPaths = $(unused).data('assetFolderPaths');
                if (typeof(folderPaths) != "undefined") orphanIsShared.push(true);
                else orphanIsShared.push(false);
            }

            var title = I18N.message("perc.ui.page.unused@Remove Unused Asset");
            var assetmsg = (i < 2) ? I18N.message("perc.ui.page.general@asset") : I18N.message("perc.ui.page.general@assets");
            var msg = I18N.message("perc.ui.page.unused@Remove Unused Assets Message", assetmsg);

            var options = {
                id: 'perc-orphan-asset-delete-dialog',
                title: title,
                question: msg,
                type: "YES_PREFERRED_NO",
                cancel: function()
                {},
                success: function()
                {
                    pageModel.clearOrphanAssets(orphanWidgetsIds, orphanTypes, orphanIds, orphanAssetDeleted);
                }
            };
            $.perc_utils.confirm_dialog(options);
        }

        function orphanAssetDeleted()
        {
            var assets = $("#perc_asset_library").find(".perc-orphan-assets-list");
            var unused = assets.find(".perc-orphan-assets-selected");
            $(unused).remove();

            clearSelection();

            // Is the orphan assets list empty?
            var remainingAssets = $("#perc_asset_library").find(".perc-orphan-assets-list").find("a");
            if (remainingAssets.length === 0)
            {
                $.fn.percOrphanAssetsMaximizer(P);
                $("#perc_orphan_assets_expander").addClass("perc-disabled").off();
                $("#perc_orphan_assets_maximizer").addClass("perc-disabled");
            }
        }

        function editOrphanAsset(event)
        {
            var assets = $("#perc_asset_library").find(".perc-orphan-assets-list");
            var unused = assets.find(".perc-orphan-assets-selected");

            var orphanId = $(unused).data('assetId');
            var orphanType = $(unused).data('assetType');
            var orphanWidgetId = $(unused).data('widgetId');

            var orphanIsShared = false;

            var widgetData = {
                widgetid: orphanWidgetId,
                widgetdefid: orphanType
            };

            pageModel.configureAsset(widgetData, orphanId, orphanIsShared, orphanAssetEdited);
        }

        function orphanAssetEdited() {}

        function selectOrphanAsset(e,obj)
        {
            if (e.shiftKey) $(obj).toggleClass("perc-orphan-assets-selected");
            else
            {
                if ($(obj).is(".perc-orphan-assets-selected")) clearSelection();
                else
                {
                    clearSelection();
                    $(obj).toggleClass("perc-orphan-assets-selected");
                }
            }

            // Manage button icons and events
            var parent = $(obj).parent();
            var selected = $(parent).find(".perc-orphan-assets-selected");
            if (selected.length > 0)
            {
                $(".perc-ui-delete-asset").attr("src", "/cm/images/icons/editor/delete.png").off("click").on("click",
                    function(evt){
                        deleteOrphanAsset(evt);
                    });
                $(".perc-ui-edit-asset").attr("src", "/cm/images/icons/editor/edit.png").on("click",
                    function(evt){
                        editOrphanAsset(evt);
                    });
            }
            else
            {
                $(".perc-ui-delete-asset").attr("src", "/cm/images/icons/editor/deleteInactive.png").off();
                $(".perc-ui-edit-asset").attr("src", "/cm/images/icons/editor/editInactive.png").off();
            }
            if (selected.length >= 2) $(".perc-ui-edit-asset").attr("src", "/cm/images/icons/editor/editInactive.png").off();
        }

        function clearSelection()
        {
            $(".perc-orphan-assets-list").find('a').removeClass("perc-orphan-assets-selected");
            $(".perc-ui-delete-asset").attr("src", "/cm/images/icons/editor/deleteInactive.png").off();
            $(".perc-ui-edit-asset").attr("src", "/cm/images/icons/editor/editInactive.png").off();
        }

        // Declare Content and Layout tabs
        $("#perc-pageEditor-tabs").tabs(
            {
                // Disable all Layout and Style tabs at load time
                disabled: [1, 2],
                beforeActivate: function( event, ui ){
                    // Ask for confirmation to navigate away from tab if the page has been modified
                    if (dirtyController.isDirty())
                    {
                        // if dirty, then show a confirmation dialog
                        dirtyController.confirmIfDirty(function()
                        {
                            // if they click ok, then reset dirty flag and proceed to select the tab
                            setDirty(false);
                            $('#perc-pageEditor-tabs').tabs("option", "active", ui.newTab.index() );
                            loadTab(ui.newTab.index(), false);
                            //Reset the JavaScript Off/On menu to JavaScript Off
                            resetJavaScriptMenu();
                        });
                        return false;
                    }else{
                        //Reset the JavaScript Off/On menu to JavaScript Off
                        resetJavaScriptMenu();
                    }
                },
                activate: function(event,ui)
                {
                    loadTab(ui.newTab.index(), true);
                }
            });

        if ($.PercNavigationManager.getView() === $.PercNavigationManager.VIEW_EDITOR && $.PercNavigationManager.getMode() === $.PercNavigationManager.MODE_EDIT)
        {
            $("#perc-wid-lib-expander").on("click", function()
            {
                $.fn.percWidLibMaximizer(P);
            });
        }

        function loadTab(index, addWrapper)
        {
            if(addWrapper){
                var viewWrapper = $.PercComponentWrapper("perc-action-page-tab-selected",["perc-ui-component-editor-toolbar","perc-ui-component-editor-frame"]);
                var isWrapperSet = $.PercViewReadyManager.setWrapper(viewWrapper);
                if(!isWrapperSet){
                    $.PercViewReadyManager.showRenderingProgressWarning();
                    return;
                }
            }
            if (index === CONTENT_TAB)
            {
                loadContent(currentPageId);
            }
            else if (index === LAYOUT_TAB)
            {
                loadLayout(currentPageId);
            }
            else if (index === STYLES_TAB)
            {
                loadCss(currentPageId);
            }
        }

        /**
         * Displays the template tray and let user change the template for selected page.
         */
        function _changeTemplate()
        {
            var successCallBack = function()
            {
                window.location.reload();
            };

            // Ask for confirmation to navigate away from tab if the page has been modified
            if (dirtyController.isDirty())
            {
                // if dirty, then show a confirmation dialog
                dirtyController.confirmIfDirty(function()
                {
                    // if they click ok, then reset dirty flag and proceed to select the tab
                    setDirty(false);
                    openChangeTemplateDialog();
                });
                return false;
            }
            else
            {
                openChangeTemplateDialog();
            }

            function openChangeTemplateDialog()
            {
                $.PercChangeTemplateDialog().openDialog(currentPageId, pageModel.getTemplateModel().getId(), $.PercNavigationManager.getSiteName(), successCallBack);
            }
        }

        /**
         * Get the template Id based on pageId and load the Template in Edit mode.
         */
        function _loadTemplate()
        {
            var memento = {
                'templateId': pageModel.getTemplateModel().getId(),
                'pageId': currentPageId,
                'tabId': "perc-tab-layout"
            };
            $.PercNavigationManager.goToLocation($.PercNavigationManager.VIEW_EDIT_TEMPLATE, $.PercNavigationManager.getSiteName(), null, null, null, $.PercNavigationManager.getPath(), null, memento);
        }

        /**
         * Resets the text of the JavaScript menu to JavaScript Off.
         */
        function resetJavaScriptMenu()
        {
            //Reset the JavaScript Off/On menu to JavaScript Off
            $(".perc-dropdown-option-DisableJavaScript").text(I18N.message("perc.ui.menu@JavaScript Off"));
        }

        function resetPageName()
        {
            $.ajax(
                {
                    url: $.perc_paths.PAGE_CREATE + "/" + currentPageId,
                    success: function(data)
                    {
                        $("#perc-pageEditor-menu-name").html(data.Page.name);
                    },
                    type: 'GET',
                    dataType: 'json'
                });
        }

        function confirm_if_dirty(callback, errorCallback, options)
        {
            options = options || {};
            errorCallback = errorCallback ||
                function()
                {};

            if (dirtyController.isDirty())
            {
                dirtyController.confirmIfDirty(callback, errorCallback, options);
            }
            else
            {
                //Page is not dirty, proceed
                callback();
            }
        }

        function loadContent(pageId)
        {
            if ($.PercNavigationManager.getMode() == $.PercNavigationManager.MODE_EDIT)
            {
                pageModel = P.pageModel($.perc_pagemanager, $.perc_templatemanager, pageId, setupContent);
                $.PercAssetService.getUnusedAssets(pageId, populateOrphanAssets);
                if ($.PercNavigationManager.isJavascriptOff()) pageModel.setJavaScriptOff(true);
            }
            else
            {
                //Load preview content into the iFrame for readonly mode
                var previewPath = $.perc_paths.PAGE_PREVIEW + currentPageId;
                $("#frame").contents().remove();
                $("#frame").attr("src", previewPath);
                $("#frame").off();
                $("#frame").on("load", function()
                {
                    fixIframeHeight();
                    window.setTimeout(function()
                    {
                        $.perc_utils.handleLinks($("#frame"));
                    }, 500);

                    var frwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-editor-frame');

                    if(frwrapper != null)
                        frwrapper.handleComponentProgress('perc-ui-component-editor-frame', "complete");

                    var tbwrapper = $.PercViewReadyManager.getWrapper('perc-ui-component-editor-toolbar');

                    if(tbwrapper != null)
                        tbwrapper.handleComponentProgress('perc-ui-component-editor-toolbar', "complete");
                });
            }
        }

        function loadLayout(pageId)
        {
            pageModel = P.pageModel($.perc_pagemanager, $.perc_templatemanager, pageId, setupLayout);
            if ($.PercNavigationManager.isJavascriptOff()) pageModel.setJavaScriptOff(true);
        }

        function loadCss(pageId)
        {
            pageModel = P.pageModel($.perc_pagemanager, $.perc_templatemanager, pageId, setUpCss);
            if ($.PercNavigationManager.isJavascriptOff()) pageModel.setJavaScriptOff(true);
        }

        function setupContent()
        {
            fixIframeHeight();
            P.contentView($("#frame"), pageModel);

        }

        function setupLayout()
        {
            fixIframeHeight();
            var layoutController = P.layoutController(pageModel);
            var sizeController = P.sizeController(pageModel);
            P.layoutView($("#frame"), pageModel, layoutController, sizeController, function(isDirty)
            {
                setDirty(isDirty);
            });
            $("#region-tool").draggable(
                {
                    helper: 'clone'
                });
        }

        function setDirty(isDirty)
        {
            dirtyController.setDirty(isDirty, "page");
            dirty = isDirty;
        }

        function isDirty()
        {
            return dirty;
        }

        /**
         * Sets up the css tab content by pasing the pageModel and binds the click event for the save button.
         */
        function setUpCss()
        {
            fixIframeHeight();
            var cssController = P.cssController(pageModel, $("#frame"), P.CSSPreviewView($("#frame"), pageModel));
            $("#perc-css-editor-save").on("click",function()
            {
                cssController.save(function()
                {});
            });

            $("#perc-css-editor-cancel").on("click",function()
            {
                pageModel.load();
                dirtyController.setDirty(false, "template");
            });
        }

        /**
         * Schedule the item(page/asset) for the supplied pageId/assetId.
         */
        function _openSchedule(callbackData)
        {
            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var itemId = callbackData.pageId;
            var pageName = callbackData.pageName;
            if (dialogFlag)
            {
                $("#ui-datepicker-div").css('z-index', 9501).css('display', 'none');
                $("#ui-timepicker-div").css('z-index', 9501).css('display', 'none');
                $("#perc-schedule-dialog-cancel").trigger("click");
                $.PercScheduleDialog.open(itemId, pageName);
                dialogFlag = false;
            }
            else
            {
                $("#ui-datepicker-div").css('z-index', 100000);
                $("#ui-timepicker-div").css('z-index', 100000);
                $.PercScheduleDialog.open(itemId, pageName);
            }
        }

        /**
         * Get the current status of the item.
         */
        function getPageStatus(pageId)
        {
            var pagePath = $.PercNavigationManager.getPath();
            $.perc_pathmanager.getItemProperties(pagePath, function(status, itemProps)
            {
                var pageStatus = itemProps.status;

                $(".perc-page-status-status").html(pageStatus);
                $.PercItemPublisherService.getScheduleDates(pageId, function(status, result)
                {
                    var scheduleDates = eval("(" + result + ")").ItemDates;
                    if (scheduleDates.startDate && pageStatus === I18N.message("perc.ui.page.general@Pending"))
                    {
                        $(".perc-page-status-status").html(I18N.message("perc.ui.page.general@Approved For") + " " + scheduleDates.startDate);
                    }
                });
            });
        }

        /**
         * Checks if Publish date is set for item before doing immediate publishing.
         * @param Object scheduleDates that respects the following form (all String members
         * could be the empty String ""):
         * <pre>
         * {
         *   endDate   : "04/30/2012 12:00 am",
         *   itemId    : "16777215-101-759",
         *   startDate : "04/24/2012 12:00 am"
         * }
         * </pre>
         */
        function _confirmPublish(scheduleDates)
        {
            var startDate = scheduleDates.startDate;
            var itemType = view == $.PercNavigationManager.VIEW_EDIT_ASSET ? "Asset" : "Page";
            var itemId = scheduleDates.itemId;
            if (startDate !== "")
            {
                var settings = {
                    id: "perc-confirm-publish-dialog",
                    title: I18N.message("perc.ui.page.general@Warning"),
                    question: I18N.message("perc.ui.page.confirmpublish@This item is scheduled",startDate),
                    success: function()
                    {
                        _immediateItemPublish(itemId, itemType);
                    },
                    cancel: function()
                    {},
                    yes: I18N.message("perc.ui.page.confirmpublish@Continue Anyway")
                };
                utils.confirm_dialog(settings);
            }
            else
            {
                _immediateItemPublish(itemId, itemType);
            }
        }

        /**
         * Invokes the publishing service. If an error is returned it shows it with a dialog and
         * stops the publishing proccess.
         * @param String itemId
         * @param String itemType
         */
        function _immediateItemPublish(itemId, itemType)
        {
            $.PercBlockUI();
            $.PercItemPublisherService.publishItem(itemId, itemType, _afterPublish);
        }

        /**
         * Publish/Take Down the item(page/asset) for the supplied pageId/assetId.
         */
        function _publishItem(callbackData)
        {
            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var itemId = callbackData.pageId;
            var trName = callbackData.trName;
            var itemType = view === $.PercNavigationManager.VIEW_EDIT_ASSET ? "Asset" : "Page";
            var siteName = $.PercNavigationManager.getSiteName();

            // The user can create a page without selecting a site
            if (siteName === undefined && itemType === "Page")
            {
                // Retrieve the page path folder by getting its data using its id,
                // Stripe the //Sites prefix from it and retrieve the site's name
                var currentItemPath = $.perc_finder().getPathItemById(itemId).folderPath;
                siteName = currentItemPath.replace('/' + $.perc_paths.SITES_ROOT, '').split('/')[1];
            }

            confirm_if_dirty(function()
            {
                doIfItemExists(itemId, function()
                    {
                        /*doIfCheckedOutToCurrentUser(itemId, function()
                            { */
                        doIfDefaultServerNotModified(siteName, function()
                            {
                                if (trName === I18N.message("perc.ui.page.menu@Publish"))
                                {
                                    $.PercItemPublisherService.getScheduleDates(itemId, function(status, result)
                                    {
                                        if (status)
                                        {
                                            var scheduleDates = eval("(" + result + ")").ItemDates;
                                            _confirmPublish(scheduleDates);
                                        }
                                        else
                                        {
                                            $.perc_utils.alert_dialog(
                                                {
                                                    content: I18N.message("perc.ui.page.confirmpublish@Unable to get the saved publish dates"),
                                                    title: I18N.message("perc.ui.labels@Error")
                                                });
                                            return false;
                                        }
                                    });
                                }
                                else if (trName === I18N.message("perc.ui.page.menu@Take Down"))
                                {
                                    // $.PercBlockUI();
                                    $.PercItemPublisherService.takeDownItem(itemId, itemType, _afterPublish);
                                }
                                else if(trName === I18N.message("perc.ui.page.menu@Stage"))
                                {
                                    $.PercBlockUI();
                                    $.PercItemPublisherService.publishToStaging(itemId, itemType, _afterPublish);
                                }
                                else if(trName === I18N.message("perc.ui.page.menu@Remove from Staging"))
                                {
                                    $.PercBlockUI();
                                    $.PercItemPublisherService.removeFromStaging(itemId, itemType, _afterPublish);
                                }
                            },
                            function()
                            {
                                //an Admin has overridden the current editor in another session
                                $.perc_utils.alert_dialog(
                                    {
                                        title: trName,
                                        content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Overridden"),
                                        okCallBack: function()
                                        {
                                            $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                        }
                                    });
                            });
                    },
                    function()
                    {
                        $.perc_utils.alert_dialog(
                            {
                                title: trName,
                                content: I18N.message("perc.ui.webmgt.contentbrowser.warning@Action Not Performed Deleted"),
                                okCallBack: function()
                                {
                                    $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                                }
                            });
                    });
            });
        }

        function _afterPublish(success, results)
        {
            if (!success)
            {
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results[0]);
                $.unblockUI();
                $.perc_utils.alert_dialog(
                    {
                        title: I18N.message('perc.ui.labels@Error'),
                        content: defMsg
                    });
            }
            else
            {
                var publishStatus = results[0].SitePublishResponse.status;
                if (publishStatus === $.PercItemPublisherService.PUBLISHER_JOB_STATUS_FORBIDDEN)
                {
                    $.unblockUI();
                    $.perc_utils.alert_dialog(
                        {
                            title: I18N.message("perc.ui.publish.errordialog.title@Server Publish"),
                            content: I18N.message("perc.ui.publish.errordialog.message@Publish Not Allowed")
                        });
                }
                else if ( publishStatus === $.PercItemPublisherService.PUBLISHER_JOB_STATUS_BADCONFIG)
                {
                    $.unblockUI();
                    $.perc_utils.alert_dialog(
                        {
                            title: I18N.message("perc.ui.publish.errordialog.title@Server Publish"),
                            content: I18N.message("perc.ui.publish.errordialog.message@Bad configuration")
                        });
                }
                else if ( publishStatus === $.PercItemPublisherService.PUBLISHER_JOB_STATUS_NOSTAGING_SERVERS)
                {
                    $.unblockUI();
                    $.perc_utils.alert_dialog(
                        {
                            title: I18N.message("perc.ui.publish.errordialog.title@Server Publish"),
                            content: I18N.message("")
                        });
                }
                else
                {
                    $.PercNavigationManager.goTo($.PercNavigationManager.VIEW_EDITOR, true);
                    $.unblockUI();
                }
            }
        }

        /**
         * Opens the revision dialog for the supplied pageId.
         */
        function _openRevisions(callbackData)
        {
            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var pageId = callbackData.pageId;
            var pageName = callbackData.pageName;
            $.PercRevisionDialog.open(pageId, pageName, $.PercRevisionDialog.ITEM_TYPE_PAGE, $.PercRevisionDialog.ITEM_MODE_VIEW);
        }

        /**
         * Opens the revision dialog for the supplied pageId.
         */
        function _openComments(callbackData)
        {
            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var pageId = callbackData.pageId;
            var pageName = callbackData.pageName;
            $.PercCommentsDialog.open(pageId, pageName, $.PercCommentsDialog.ITEM_TYPE_PAGE);
        }

        /**
         * Opens the publishing history dialog for the supplied pageId.
         */
        function _openPublishingHistory(callbackData)
        {
            if(callbackData.class === jQuery.Event.class){
                callbackData = callbackData.data;
            }

            var pageId = callbackData.pageId;
            var pageName = callbackData.pageName;
            $.PercPublishingHistoryDialog.open(pageId, pageName, $.PercPublishingHistoryDialog.ITEM_TYPE_PAGE);
        }

        function _previewPage(currentPageId)
        {
            confirm_if_dirty(function()
            {
                jQuery.perc_finder().launchPagePreview(currentPageId.data);
            });
        }

        /**
         * Makes a call to workflow controller to determine if the specified item is
         * checked out to the current user.  Invokes the appropriate callback based
         * on the result.
         *
         * @param contentId the id of the item.
         * @param yesCallback function to perform if the item is checked out to current user.
         * @param noCallback function to perform if the item is not checked out to current user.
         */
        function doIfCheckedOutToCurrentUser(contentId, yesCallback, noCallback)
        {
            $.PercWorkflowController().isCheckedOutToCurrentUser(contentId, function(result)
            {
                if (result)
                {
                    yesCallback();
                }
                else
                {
                    noCallback();
                }
            });
        }

        /**
         * Check if the default publish server is modified and needs a CM1 restart before publish
         * or remove from site.
         *
         * @param SiteName
         * @param callback function to perform if we dont need a CM1 restart.
         */
        function doIfDefaultServerNotModified(siteName, callback)
        {
            $.PercNavigationManager.loadSiteProperties(siteName, function(siteProperties)
            {
                $.PercItemPublisherService.isDefaultServerModified(siteProperties.id, function(status, result)
                {
                    if (status)
                    {
                        if (result)
                        {
                            $.perc_utils.alert_dialog(
                                {
                                    content: I18N.message("perc.ui.page.dialog@Restart Required"),
                                    title: I18N.message("perc.ui.uploadtheme.dialog.title@Warning")
                                });
                        }
                        else
                        {
                            callback();
                        }
                    }
                });
            });
        }

        /**
         * Makes a call to workflow controller to determine if the specified item exists.
         * Invokes the appropriate callback based on the result.
         *
         * @param contentId the id of the item.
         * @param existsCallback function to perform if the item exists.
         * @param doesNotExistCallback function to perform if the item does not exist.
         */
        function doIfItemExists(contentId, existsCallback, doesNotExistCallback)
        {
            $.PercWorkflowController().doesItemExist(contentId, function(result)
            {
                if (result)
                {
                    existsCallback();
                }
                else
                {
                    doesNotExistCallback();
                }
            });
        }

        /**
         * Return the public API for this class.
         */
        return pageViewAPI;
    };
})(jQuery, jQuery.Percussion);

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
 * PercAssetController.js
 * @author Jose Annunziato
 * @see PercAssetEditorModel.js
 * @see PercAssetService.js
 * @see PercPathService.js
 * 
 * 
 */
(function($)
{
	$.PercAssetController = {	
			getAssetEditorLibrary: getAssetEditorLibrary,
			getAssetEditorForAssetId: getAssetEditorForAssetId,
			getAssetViewForAssetId: getAssetViewForAssetId,
			getPathItemForPath : getPathItemForPath,
			putAssetInFolder : putAssetInFolder
	};

    /* =========================================================
     * Public Functions
     * ========================================================= */
     
    /**
     * Retrieves a list of editor objects used to render a library of editors in a dialog for the user to choose from.
     * Editor object contains icon and URL. URL points to location of HTML form for editing/creating the type of asset.
     * Editor object schema:
     * {AssetEditor  : [
     *      {"icon"       : "/path/to/image.png",
     *       "title"      : "Title String",
     *       "url"        : "http://URL/to/html/editor/for/editing/asset.html",
     *       "workflowId" : 4   (some number)
     *      }
     * ]}
     */
    function getAssetEditorLibrary(currentFolderPath, controllerCallback)
    {
        function serviceCallback(status, assetEditorLibrary)
        {
        	// iterate over the JSON response containing an array of asset editor objects
        	// create array of PercAssetEditorModel instances and return array
            var assetEditors = [];
            var assetEditor  = assetEditorLibrary.AssetEditor;
            for(i in assetEditor)
            {
                var icon       = assetEditor[i].icon;
                var title      = assetEditor[i].title;
                var url        = assetEditor[i].url;
                var workflowId = assetEditor[i].workflowId;
                var editor     = new $.PercAssetEditorModel(icon, title, url, workflowId);
                assetEditors.push(editor);
            }
            controllerCallback(assetEditors);
        }
        $.PercAssetService.getAssetEditorLibrary(currentFolderPath, serviceCallback);
    }
    
    /**
     * Retrieves the URL of the editor for a given asset id
     * @param assetId(String) the id of the asset we want to edit
     * @param callback(function(status(String), assetEditorUrl(String)))
     */
    function getAssetEditorForAssetId(assetId, callback)
    {
        $.PercAssetService.getAssetEditorForAssetId(assetId, function(status, assetEditorUrl){
        	callback(status, assetEditorUrl);
        });
    }
    
    /**
     * Retrieves the URL of the readonly view for a given asset id
     * @param assetId(String) the id of the asset we want to edit
     * @param callback(function(status(String), assetViewUrl(String)))
     */
    function getAssetViewForAssetId(assetId, callback)
    {
        $.PercAssetService.getAssetViewForAssetId(assetId, function(status, assetViewUrl){
         callback(status, assetViewUrl);
        });
    }
    
    /**
     * Gets the path item object from the path service given a path from the finder
     * @param path we want path item for
     * @param controllerCallback is method we call after building the path item instance and pass it back
     */ 

    function getPathItemForPath(path, controllerCallback)
    {
        function serviceCallback(status, pathItemJson)
        {
            var pathItemObj = new $.PercPathItemModel(
                                    pathItemJson.PathItem.id,
                                    pathItemJson.PathItem.folderPaths,
                                    pathItemJson.PathItem.icon,
                                    pathItemJson.PathItem.name,
                                    pathItemJson.PathItem.type,
                                    pathItemJson.PathItem.folderPath,
                                    pathItemJson.PathItem.leaf,
                                    pathItemJson.PathItem.path);
            controllerCallback(pathItemObj);
        }
        $.PercPathService.getPathItemForPath(path, serviceCallback);
    }
    
    /**
     * 
     */
    function putAssetInFolder(assetId, folderPath, controllerCallback)
    {
    	var assetFolderRelationship = {"AssetFolderRelationship" : {"assetId" : assetId,    "folderPath" : folderPath}};
    	$.PercAssetService.putAssetInFolder(assetFolderRelationship, controllerCallback);
    }
})(jQuery);

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
 * Workflow controller class to perform the workflow actions.
 * Makes appropriate calls to the workflow service to check out, check in and transition items.
 * based on the type of the object shows appropriate error messages.
 */
(function($)
{
    $.PercWorkflowController = function()
    {
        return {
            checkIn : checkIn,
            checkOut : checkOut,
            transition : transition,
            getTransitions : getTransitions,
            isCheckedOutToCurrentUser : isCheckedOutToCurrentUser,
            doesItemExist : doesItemExist
        };
    };

    /**
     * Workflow service instance to perform the service calls for different methods.
     */
    var wfService = $.PercWorkflowService();

    /**
     * Checkin is an implicit action, calls workflow service checkin method to check in the supplied item.
     * If there is any error performing this action shows the error to the user and calls the call back with false value.
     * If not calls the callback with true.
     * @param itemId(String), the id of the item(String format of guid) that needs to be checked in. Must not be blank.
     * @param callback(function (boolean)), if the callback function is not <code>null</code> then it is called by this
     * method with boolean true if checked in successfully otherwise false.
     */
    function checkIn(itemId, callback)
    {
        callback = callback == null?function(){}:callback;
        //Local callback function that is passed to the service checkin method, this callback function calls the passed in
        //callback with appropriate value.
        var chkInCb = function(status, results){
            if(status == $.PercServiceUtils.STATUS_ERROR){
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                $.perc_utils.debug("Error : " + defMsg);
                callback(false);
            }
            else{
                callback(true);
            }
        };
        wfService.checkIn(itemId, chkInCb);
    }

    /**
     * Calls workflow service checkout method. If the checkout user is same as logged in user then calls the callback
     * function with true value. If not depending on the assignment type of the user shows appropriate message.
     * Assignment type is Admin, then provides a confirmation dialog with an option to override. If user clicks on override
     * then calls _forceCheckOut to check it out to the admin.
     * Assignment type Assignee, shows an alert message to the user that the item is being edited by some other user.
     * Assignment type Reader, shows an alert message to the user that he is not authorized to open the item.
     *
     * @param itemType, the type of the item "percPage" or "percAsset" used in the warning messages.
     * @param itemId(String), the id of the item (String format of guid) that needs to be checked in. Must not be blank.
     * @param callback(function (boolean)), if the callback function is not <code>null</code> then it is called by this
     * method with boolean true if checked out successfully otherwise false.
     */
    function checkOut(itemType, itemId, callback)
    {
        callback = callback == null?function(){}:callback;
        wfService.checkOut(itemId, function(status, results)
        {
            if(status == $.PercServiceUtils.STATUS_SUCCESS){
                var itemStatus = results.data.ItemUserInfo;
                if(itemStatus.checkOutUser != itemStatus.currentUser)
                {
                    if(itemStatus.assignmentType == "Admin")
                    {
                        var options = {title:warningDlgTitle[itemType],
                            question:_getChkOutDlgContent(0, itemStatus, itemType),
                            cancel:function(){callback(false);},
                            success:function(){_forceCheckOut(itemId, callback);},
                            type:"OVERRIDE_OK"
                        };
                        $.perc_utils.confirm_dialog(options);
                    }
                    else if(itemStatus.assignmentType == "Assignee")
                    {
                        var options = {title:warningDlgTitle[itemType],
                            content:_getChkOutDlgContent(1, itemStatus, itemType)};
                        $.perc_utils.alert_dialog(options);
                        callback(false);
                        return;
                    }
                    else
                    {
                        var options = {title:warningDlgTitle[itemType],
                            content:_getChkOutDlgContent(2, itemStatus, itemType)};
                        $.perc_utils.alert_dialog(options);
                        callback(false);
                        return;
                    }
                }
                else
                {
                    callback(true);
                }
            }
            else{
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                if (defMsg.indexOf("Not a valid content id") != -1)
                {
                    var options = {title:warningDlgTitle[itemType],
                        content:_getChkOutDlgContent(3, itemStatus, itemType)};
                    $.perc_utils.alert_dialog(options);
                    callback(false);
                    return;
                }

                $.perc_utils.alert_dialog({title: I18N.message('perc.ui.labels@Error'), content: defMsg});
                callback(false);
            }
        });
    }

    /**
     * Helper method that calls the service to force check out the item to the admin.
     * This should be called if the logged in user has admin access.
     * @param itemId(String format of guid) assumed not null.
     * @param callback(function (boolean)), if the callback function is not <code>null</code> then it is called by this
     * method with boolean true if checked out successfully otherwise false.
     */
    function _forceCheckOut(itemId, callback)
    {
        callback = callback == null?function(){}:callback;
        wfService.forceCheckOut(itemId, function(status, results){
            if(status == $.PercServiceUtils.STATUS_SUCCESS){
                callback(true);
            }
            else{
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                $.perc_utils.alert_dialog({title: I18N.message('perc.ui.labels@Error'), content: defMsg});
                callback(false);
            }

        });
    }


    /**
     * Calls workflow service transtion method to transition the supplied item.
     * If there is any error performing this action shows the error to the user and calls the call back with false value.
     * If not calls the call back with true.
     * @param itemId(String), the id of the item(String format of guid) that needs to be checked in. Must not be blank.
     * @param callback(function (boolean)), , if the callback function is not <code>null</code> then it is called by this
     * method with boolean true if the item is transitioned successfully otherwise false.
     */
    function transition(itemId, itemType, transitionName, comment, callback)
    {
        callback = callback == null?function(){}:callback;
        var trCb = function(status, results){
            if(status == $.PercServiceUtils.STATUS_ERROR){
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                $.perc_utils.alert_dialog({title: I18N.message('perc.ui.labels@Error'), content: defMsg});
                callback(false);
            }
            else{
                var trResults = results.data;
                var fassets = trResults.ItemTransitionResults.failedAssets;
                if(typeof fassets !=='undefined' && (!Array.isArray(fassets) || !fassets.length) && fassets.length > 0)
                {
                    var type = (itemType === "percAsset")?"asset":"page";
                    var msg = I18N.message("perc.ui.workflow.steps.view@Cannot Publish", [type]) + "<br/><br/>";

                    if(Array.isArray(fassets))
                    {
                        $.each(fassets,function(){
                            var fp = this.folderPaths;
                            fp = fp.substring(fp.indexOf("/Assets")) + "/" + this.name;
                            msg += "<b>" + fp + "</b><br/>";
                        });
                    }
                    else
                    {
                        var fp = fassets.folderPaths;
                        fp = fp.substring(fp.indexOf("/Assets")) + "/" + fassets.name;
                        msg += "<b>" + fp + "</b><br/>";
                    }
                    msg += "<br/>" + I18N.message("perc.ui.workflow.steps.view@Remove Assets");

                    $.perc_utils.alert_dialog({title: I18N.message('perc.ui.labels@Error'), content: msg});
                    callback(false);
                }
                else
                {
                    callback(true);
                }
            }
        };
        wfService.transition(itemId, transitionName, comment, trCb);
    }

    /**
     * Returns the available transitions to the user  as a second param of the callback function.
     * An array of TransitionAction objects.
     * {"name":"Reject", "class":"perc-wf-reject","alt":"Reject"}
     * The first parameter is a boolean status, true in case of success and false in case of failure.
     * Shows the error message in case of error.
     * @param itemId(String), the id of the item for which the transitions are required. Must not be blank.
     * @param callback(boolean status, array of transitionActions)), if the callback function is not <code>null</code>
     * then it is called by this method.
     */
    function getTransitions(itemId, callback)
    {
        callback = callback == null?function(){}:callback;
        wfService.getTransitions(itemId, function(status, results){
            if(status === $.PercServiceUtils.STATUS_SUCCESS){
                var trAs = [];
                var triggers = results.data.ItemStateTransition.transitionTriggers;
                if(Array.isArray(triggers)){
                    $.each(triggers, function(index){
                        //As we already added Publish skip it if exists.
                        if(transitionActions[triggers[index]])
                            trAs.push(transitionActions[triggers[index]]);
                    });
                }
                else
                {
                    if(transitionActions[triggers])
                        trAs.push(transitionActions[triggers]);
                }
                callback(true, trAs);
            }
            else{
                var defMsg = $.PercServiceUtils.extractDefaultErrorMessage(results.request);
                $.perc_utils.alert_dialog({title: 'Error', content: defMsg});
                callback(false, []);
            }
        });
    }

    /**
     * Checks whether the item with the supplied id is checked out to the current user or not.
     * @param itemId(String), the id of the item. Must not be blank.
     * @param callback(function (boolean, string)), the callback function, calls it with a boolean and string value.
     * The boolean value will be true if it is checked out to the current user, otherwise false.  The string value will
     * contain true, false, or an error message.
     */
    function isCheckedOutToCurrentUser(itemId, callback)
    {
        callback = callback == null?function(){}:callback;
        var isChkCb = function(status, results){
            if(status === $.PercServiceUtils.STATUS_SUCCESS){
                callback(results.data === true, results.data);
            }
            else{
                callback(false, $.PercServiceUtils.extractDefaultErrorMessage(results.request));
            }
        };
        wfService.isCheckedOutToCurrentUser(itemId, isChkCb);
    }

    /**
     * Checks whether the item with the supplied id exists.
     * @param itemId(String), the id of the item. Must not be blank.
     * @param callback(function (boolean)), the callback function, calls it with a boolean value. The boolean value will be true
     * it exists, otherwise false.
     */
    function doesItemExist(itemId, callback)
    {
        callback = callback == null?function(){}:callback;
        /* isCheckedOutToCurrentUser(itemId, function(status, msg){
            if(typeof msg == "string")
                callback(status || msg.indexOf("does not exist") == -1);
            else
                callback(status);
        });
*/
        $.PercPathService.getPathItemById(itemId, function(status, data){
           callback(status);
        });
    }

    /**
     * Helper method that creates appropriate message for check out.
     * @param msgType(int) type of the message required.
     * @param itemStatus assumed not null, see PercWorkflowService#checkOut method for details.
     * @param itemType assumed not <code>null</code> (String "page" or "asset")
     */
    function _getChkOutDlgContent(msgType, itemStatus, itemType)
    {
        var msg = "";
        var type = "page";
        if(itemType == "percAsset")
            type = "asset";
        if(msgType == 0)
        {
            msg = I18N.message("perc.ui.workflow.steps.view@Override", [type, itemStatus.itemName, itemStatus.checkOutUser]);
        }
        else if(msgType==1)
        {
            msg = I18N.message("perc.ui.workflow.steps.view@Edited By", [type, itemStatus.itemName, itemStatus.checkOutUser]);
        }
        else if(msgType==2)
        {
            msg = I18N.message("perc.ui.workflow.steps.view@Not Authorized", [type, itemStatus.itemName]);
        }
        else if(msgType==3)
        {
            msg = I18N.message("perc.ui.workflow.steps.view@Deleted In Another Session", [type]);
        }
        return msg;
    }

    var warningDlgTitle = [];
    warningDlgTitle.percPage = "Open Page";
    warningDlgTitle.percAsset = "Open Asset";

    /**
     * Object map for transition trigger names and its classes.
     * The class consists of the appropriate buttons.
     */
    var transitionActions = [];
    transitionActions.Reject = {"name":"Reject", "cssClass":"perc-wf-reject","alt":"Reject"};
    transitionActions.Submit = {"name":"Submit", "cssClass":"perc-wf-submit","alt":"Submit"};
    transitionActions.Approve = {"name":"Approve", "cssClass":"perc-wf-approve","alt":"Approve"};
    transitionActions.Resubmit = {"name":"Resubmit", "cssClass":"perc-wf-resubmit","alt":"Resubmit"};
    transitionActions["Quick Edit"] = {"name":"Quick Edit", "cssClass":"perc-wf-edit","alt":"Edit"};
    transitionActions.Archive = {"name":"Archive", "cssClass":"perc-wf-archive","alt":"Archive"};

})(jQuery);

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

// PercPathModel.js
// Author: Jose Annunziato
// Date: 1/20/2010
// Data models related to path management.
// Paths of folders, sites, assets, etc., in the finder
(function($)
{
    // JGA
    // holds data for path item details
    // currently populated by PercAssetController.js
    // controller retrieves path item from service: PercPathService.js
    // used by the controller to retrieve a finder's path's id
    // the path's id is then used to invoke an asset's form editor
    // which needs the path's id to put the asset in the folder once it creates it
    $.PercPathItemModel = function( id,
					                folderPaths,
					                icon,
					                name,
					                type,
					                folderPath,
					                leaf,
					                path)
    {
        this.id = id;
        this.folderPaths = folderPaths;
        this.icon = icon;
        this.name = name;
        this.type = type;
        this.folderPath = folderPath;
        this.leaf = leaf;
        this.path = path;
/*        
        this.log   = function()
        {
            console.log("id          = " + this.id);
            console.log("folderPaths = " + this.folderPaths);
            console.log("icon        = " + this.icon);
            console.log("name        = " + this.name);
            console.log("type        = " + this.type);
            console.log("folderPath  = " + this.folderPath);
            console.log("leaf        = " + this.leaf);
            console.log("path        = " + this.path);
        };
        */
    };
})(jQuery);

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

(function($)
{
    $.PercWidgetModel = function()
    {
        // region id this widget is in
        this.id = null;
        
        // whether this widget is locked
        this.locked = false;
        
        // html from service
        this.html = null;
        
        // css object model
        this.ccs = null;
        
        // defines wether this is a Raw HTML widget or a Rich Text widget or one of many other types of widgets
        this.definitionId = null;
        
        //
        this.name = null;

        // user defined properties
        this.properties = null;
        
        // 
        this.assetIds = null;
        
        //the current relationship Id
        this.relationshipId = -1;
        
        //
        // setters and getters
        //
                
        this.setId = function(id)
        {
            this.id = id;
        };
        
        this.getId = function()
        {
            return this.id;
        };
        
        this.setHtml = function(html)
        {
            this.html = html;
        };
        
        this.getHtml = function()
        {
            return this.html;
        };
        
        this.setCss = function(css)
        {
            this.css = css;
        };
        
        this.getCss = function()
        {
            return this.css;
        };
        
        this.setDefinitionId = function(definitionId)
        {
            this.definitionId = definitionId;
        };
        
        this.getDefinitionId = function()
        {
            return this.definitionId;
        };
        
        this.setProperties = function(properties)
        {
            this.properties = properties;
        };
        
        this.getProperties = function()
        {
            return this.properties;
        };
        
        this.setName = function(name)
        {
            this.name = name;
        };
        
        this.getName = function()
        {
            return this.name;
        };
        
        this.setAssetIds = function(assetIds)
        {
            this.assetIds = assetIds;
        };
        
        this.getAssetIds = function()
        {
            return this.assetIds;
        };
        
        this.setRelationshipId = function(relationshipId)
        {
            this.relationshipId = relationshipId;
        };
        
        this.getRelationshipId = function()
        {
            return this.relationshipId;
        };
    };
    // JGA
    // Data model for Asset Drop Criteria which contains
    // information about a widget such as whether a widget
    // is locked or not and its owner: pageid or templateid
    $.PercAssetDropCriteriaModel = function(   widgetId,
                                               appendSupport,
                                               locked,
                                               multiItemSupport,
                                               ownerId,
                                               supportedContentTypes,
                                               assetShared,
                                               relationshipId)
    {
        this.widgetId              = widgetId;
        this.appendSupport         = appendSupport;
        this.locked                = locked;
        this.multiItemSupport      = multiItemSupport;
        this.ownerId               = ownerId;   // templateId or pageId that owns this widget
        this.supportedContentTypes = supportedContentTypes;
        this.assetShared           = assetShared;
        this.relationshipId        = relationshipId;
        this.log = function()
        {
            console.log("widgetId              = " + this.widgetId);
            console.log("appendSupport         = " + this.appendSupport);
            console.log("locked                = " + this.locked);
            console.log("multiItemSupport      = " + this.multiItemSupport);
            console.log("ownerId               = " + this.ownerId);
            console.log("supportedContentTypes = " + this.supportedContentTypes);
        };
    };
    
    // JGA
    // holds data for rendering an asset editor icon
    // and invoking its URL to open up the editor in
    // a separate dialog or in the iframe    
    $.PercAssetEditorModel = function( icon,
                                        title,
                                        url,
                                        workflowId)
    {
        this.icon         = icon;
        this.title        = title;
        this.url          = url;
        this.workflowId   = workflowId;
        this.log   = function()
        {
            console.log("icon       = " + this.icon);
            console.log("title      = " + this.title);
            console.log("url        = " + this.url);
            console.log("workFlowId = " + this.workflowId);
        };
    };
})(jQuery);
