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
 * PercCategoryView.js
 *
 */
(function ($) {
  var dirtyController = $.PercDirtyController;
  var sitesList = [];

  $.PercCategoryView = function () {
    var viewApi = {
      init: init,
      getCategories: getCategories,
      showSelectedCategoryEditor: showSelectedCategoryEditor,
      alertDialog: alertDialog,
      errorDialog: errorDialog,
      confirmDialog: confirmDialog,
      editCategories: editCategories,
      displayCategoryDetails: displayCategoryDetails,
      deleteCategory: deleteCategory,
      handleDelete: handleDelete,
      getCurrentDate: getCurrentDate,
      visitTreeForBaseProperties: visitTreeForBaseProperties,
      manageDynaProps: manageDynaProps,
      getUpdatedCategoryArray: getUpdatedCategoryArray,
      updateCategoryXML: updateCategoryXML,
      save: save,
      findUpTargetNode: findUpTargetNode,
      moveNodeUp: moveNodeUp,
      findDownTargetNode: findDownTargetNode,
      moveNodeDown: moveNodeDown,
      publishToDTS: publishToDTS,
    };

    // A snippet to adjust the frame size on resizing the window.
    $(window).on("resize", function () {
      fixIframeHeight();
      fixTemplateHeight();
    });

    var container = $("#perc-category-tree");
    var controller = $.PercCategoryController;
    var sitename = "";
    var categories;
    var editing = false;
    var isMoved = false;
    var isDelete = false;
    var isPublished = false;
    var siteSelection;
    var originalTitle = null;
    // Category UIDs are internal CMS keys (not security credentials). Use Web
    // Crypto only — never Math.random — so CodeQL js/insecure-randomness stays clear.
    // randomUUID when available; otherwise getRandomValues; last resort is a
    // monotonic time+counter token (no PRNG) for pre-WebCrypto browsers.
    var generateUid = function () {
      var delim = "-";
      var hex = "";
      var i;
      var c = typeof crypto !== "undefined" ? crypto : null;

      if (c && typeof c.randomUUID === "function") {
        return c.randomUUID();
      }

      if (c && typeof c.getRandomValues === "function") {
        var buf = new Uint8Array(16);
        c.getRandomValues(buf);
        // RFC 4122 version-4 bits for a well-formed UUID string
        buf[6] = (buf[6] & 0x0f) | 0x40;
        buf[8] = (buf[8] & 0x3f) | 0x80;
        for (i = 0; i < buf.length; i++) {
          hex += ("0" + buf[i].toString(16)).slice(-2);
        }
        return (
          hex.substring(0, 8) +
          delim +
          hex.substring(8, 12) +
          delim +
          hex.substring(12, 16) +
          delim +
          hex.substring(16, 20) +
          delim +
          hex.substring(20, 32)
        );
      }

      // No Web Crypto: unique enough for client-side category keys without Math.random
      generateUid._seq = (generateUid._seq || 0) + 1;
      var t = Date.now().toString(16);
      var p =
        typeof performance !== "undefined" &&
        typeof performance.now === "function"
          ? Math.floor(performance.now() * 1000).toString(16)
          : "0";
      var s = generateUid._seq.toString(16);
      var pad = function (str, n) {
        while (str.length < n) {
          str = "0" + str;
        }
        return str.slice(-n);
      };
      return (
        pad(t, 8) +
        delim +
        pad(p, 4) +
        delim +
        "4" +
        pad(s, 3) +
        delim +
        "a" +
        pad(s, 3) +
        delim +
        pad(t + p + s, 12)
      );
    };

    controller.init(viewApi);

    function addSitesToDropdown(
      selectionid,
      siteArray,
      selectedArray,
      allSitesOption,
    ) {
      var optionsAsString = "";
      if (allSitesOption) {
        optionsAsString += '<option value="">All Sites</option>';
      }
      for (i = 0; i < siteArray.length; i++) {
        selectedString = "";
        if (
          selectedArray != null &&
          $.inArray(siteArray[i], selectedArray) > -1
        ) {
          selectedString = " selected='selected'";
        }
        optionsAsString +=
          '<option value="' +
          siteArray[i] +
          '"' +
          selectedString +
          ">" +
          siteArray[i] +
          "</option>";
      }
      $(selectionid).html(optionsAsString);
    }

    function stringToList(string) {
      if (string != null) return string.split(",");
      else return [];
    }

    function getSelectedSites(node) {
      if (node == null || typeof node == "undefined") {
        return sitesList;
      }
      if (
        node.data.allowedSites == null ||
        typeof node.data.allowedSites == "undefined"
      ) {
        return getAllowedSites(node);
      }
      return node.data.allowedSites.split(",");
    }

    function getAllowedSites(node) {
      if (node == null || typeof node == "undefined") {
        return sitesList;
      }
      return getSelectedSites(node.getParent());
    }

    function init() {
      $.PercSiteService.getSites(function (status, result) {
        var optionsAsString = "";
        for (i = 0; i < result.SiteSummary.length; i++) {
          sitesList.push(result.SiteSummary[i].name);
        }
        addSitesToDropdown(
          "#perc-category-site-dropdown",
          sitesList,
          null,
          true,
        );
        sitename = $("#perc-category-site-dropdown").find(":selected").val();
        controller.getCategories(sitename);
      });

      $("#perc-category-site-dropdown").on("click", function () {
        siteSelection = $("#perc-category-site-dropdown")
          .find(":selected")
          .val();
      });

      $("#perc-category-site-dropdown").on("change", function () {
        if (editing) {
          currentlyEditing();
          if (typeof siteSelection !== "undefined") {
            $("#perc-category-site-dropdown")
              .val(siteSelection)
              .trigger("chosen:updated");
          }
          return;
        }
        sitename = $("#perc-category-site-dropdown").find(":selected").val();

        container.fancytree("destroy");
        controller.getCategories(this.value);
      });

      $("#perc-categories-add-category-button")
        .off("keydown")
        .on("keydown", function (event) {
          if (event.code == "Enter" || event.code == "Space") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-add-category-button")
        .off("click")
        .on("click", function () {
          if (!$.PercNavigationManager.isAdmin()) {
            alertDialog(
              I18N.message("perc.ui.category.view@User Admin"),
              I18N.message("perc.ui.category.view@User Admin Delete"),
            );
            return;
          }

          if (editing) {
            currentlyEditing();
            return;
          }

          var node = newNode(false);
          displayCategoryDetails(node);
          showSelectedCategoryEditor(node);
        });

      $("#perc-categories-add-child-category-button")
        .off("keydown")
        .on("keydown", function (event) {
          if (event.code == "Enter" || event.code == "Space") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-add-child-category-button")
        .off("click")
        .on("click", function () {
          if (!$.PercNavigationManager.isAdmin()) {
            alertDialog(
              I18N.message("perc.ui.category.view@User Admin"),
              I18N.message("perc.ui.category.view@User Admin Delete"),
            );
            return;
          }

          if (editing) {
            currentlyEditing();
            return;
          }

          var node = newNode(true);
          displayCategoryDetails(node);
          showSelectedCategoryEditor(node);
        });
      $("#perc-categories-delete-category-button")
        .off("keydown")
        .on("keydown", function (event) {
          if (event.code == "Enter" || event.code == "Space") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-delete-category-button")
        .off("click")
        .on("click", function () {
          if (!$.PercNavigationManager.isAdmin()) {
            alertDialog(
              I18N.message("perc.ui.category.view@User Admin"),
              I18N.message("perc.ui.category.view@User Admin Delete"),
            );
            return;
          }
          if (editing) {
            currentlyEditing();
            return;
          }

          var tree = container.fancytree("getTree");
          if (tree.count() === 1)
            alertDialog(
              I18N.message("perc.ui.category.view@Delete Category"),
              I18N.message("perc.ui.category.view@Cannot Delete Node"),
            );
          else {
            isDelete = true;

            deleteCategory();
            editing = false;
          }
        });

      $("#perc-categories-edit-category-button")
        .off("keydown")
        .on("keydown", function (event) {
          if (event.code == "Enter" || event.code == "Space") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-edit-category-button")
        .off("click")
        .on("click", function () {
          if (!$.PercNavigationManager.isAdmin()) {
            alertDialog(
              I18N.message("perc.ui.category.view@User Admin"),
              I18N.message("perc.ui.category.view@User Admin Edit"),
            );
            return;
          }

          if (editing) {
            currentlyEditing();
            return;
          }

          var node = container.fancytree("getActiveNode");
          displayCategoryDetails(node);
          showSelectedCategoryEditor(node);
        });

      $("#perc-categories-moveup-button")
        .off("keydown")
        .on("keydown", function (event) {
          if (event.code == "Enter" || event.code == "Space") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-moveup-button")
        .off("click")
        .on("click", function () {
          if (editing) {
            currentlyEditing();
            return;
          }

          var node = container.fancytree("getActiveNode");

          var targetNode = findUpTargetNode(node);
          if (targetNode != null) moveNodeUp(node, targetNode);

          displayCategoryDetails(container.fancytree("getActiveNode"));
        });

      $("#perc-categories-movedown-button")
        .off("keydown")
        .on("keydown", function (event) {
          if (event.code == "Enter" || event.code == "Space") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-movedown-button")
        .off("click")
        .on("click", function () {
          if (editing) {
            currentlyEditing();
            return;
          }

          var node = container.fancytree("getActiveNode");
          var targetNode = findDownTargetNode(node);

          if (targetNode != null) moveNodeDown(node, targetNode);

          displayCategoryDetails(container.fancytree("getActiveNode"));
        });

      //Bind Save event
      $("#perc-category-save")
        .off("click")
        .on("click", function () {
          var node = container.fancytree("getActiveNode");
          if (node != null && node.data.title === "New Category") {
            alertDialog("Error", "You must change the category name.");
            return;
          }
          save();
        });
      //Bind Cancel event
      $("#perc-category-cancel")
        .off("click")
        .on("click", function () {
          var node = container.fancytree("getActiveNode");
          editing = false;
          if (!node.data.saved) {
            parent = node.parent;
            parent.activate();
            if (node.parent.childList.length !== 0) node.remove();

            node = parent;
          } else {
            node.data.title = originalTitle;
            node.render();
          }
          displayCategoryDetails(node);
          try {
            node.childList[0].activate();
          } catch (err) {}
        });

      $("#perc-categories-publish-staging")
        .off("click")
        .on("click", function (event) {
          if (event.code == "Enter") {
            document.activeElement.click();
          }
        });
      $("#perc-categories-publish-staging")
        .off("click")
        .on("click", function () {
          if (editing) {
            currentlyEditing();
            return;
          }

          var node = container.fancytree("getActiveNode");
          publishToDTS(node, "Staging");
        });

      $("#perc-categories-publish-production")
        .off("click")
        .on("click", function (event) {
          if (event.code == "Enter") {
            document.activeElement.click();
          }
        });

      $("#perc-categories-publish-production")
        .off("click")
        .on("click", function () {
          if (editing) {
            currentlyEditing();
            return;
          }

          var node = container.fancytree("getActiveNode");
          publishToDTS(node, "Production");
        });

      $("#perc-categories-publish-both")
        .off("click")
        .on("click", function (event) {
          if (event.code == "Enter") {
            document.activeElement.click();
          }
        });

      $("#perc-categories-publish-both")
        .off("click")
        .on("click", function () {
          if (editing) {
            currentlyEditing();
            return;
          }

          var node = container.fancytree("getActiveNode");
          publishToDTS(node, "Both");
        });
    }

    function getCategories(categoryJson) {
      var treedata = categoryJson;

      var categorytree = treedata.topLevelNodes;

      if (
        categorytree == null ||
        typeof categorytree == "undefined" ||
        categorytree.length === 0
      ) {
        categorytree = [
          // Pass an array of nodes.
          {
            id: generateUid(),
            title: "New Category",
            selectable: true,
            showInPgMetaData: true,
            createdBy: "system",
            creationDate: getCurrentDate(),
            deleted: false,
            activate: true,
            saved: false,
            initialViewCollapsed: true,
          },
        ];
      }
      // Always destroy any existing tree before re-initializing so post-save
      // reloads do not leave a stale instance (new/updated children may not
      // appear until a full browser refresh otherwise). GH-784 / v8.1.7 #1169.
      var hadTree = !!(
        container.data("ui-fancytree") || container.data("fancytree")
      );
      try {
        if (hadTree) {
          container.fancytree("destroy");
        }
      } catch (e) {
        // No tree yet: safe to ignore. Real destroy mid-teardown: log so we
        // do not silently re-init against a half-destroyed element.
        if (hadTree) {
          if (typeof console !== "undefined" && console.warn) {
            console.warn(
              "PercCategoryView: fancytree destroy failed; continuing re-init",
              e,
            );
          }
          // Best-effort cleanup of widget data so re-init can attach cleanly.
          try {
            container.removeData("ui-fancytree");
            container.removeData("fancytree");
          } catch (cleanupErr) {
            /* ignore cleanup errors */
          }
        }
      }

      container.fancytree({
        selectMode: 3,
        keyboard: true,
        autoCollapse: true,
        children: categorytree,
        onPostInit: function (isReloading, isError) {
          visitTreeForBaseProperties();
          this.activateKey("_2");
          $("a.fancytree-title").each(function () {
            this.title = this.innerHTML;
            this.tabIndex = "0";
            this.setAttribute("role", "button");
          });
        },
        onQueryActivate: function (flag, node) {
          if (editing) {
            currentlyEditing();
            return false;
          }
        },
        onActivate: function (node) {
          displayCategoryDetails(node);
        },
        dnd: {
          preventVoidMoves: true, // Prevent dropping nodes 'before self', etc.
          onDragStart: function (node) {
            return true;
          },
          onDragEnter: function (node, sourceNode) {
            // Prevent dropping a parent below another parent (only sort
            // nodes under the same parent)
            if (node.parent !== sourceNode.parent) {
              return false;
            }
            // Don't allow dropping *over* a node (would create a child)
            return ["before", "after"];
          },
          onDrop: function (node, sourceNode, hitMode, ui, draggable) {
            /** This function MUST be defined to enable dropping of items on
             *  the tree.
             */
            sourceNode.move(node, hitMode);
            isMoved = true;
            save();
          },
        },
      });
    }

    function visitTreeForBaseProperties() {
      var treeRoot = container.fancytree("getRoot");
      var tree = null;
      try {
        tree = container.fancytree("getTree");
      } catch (e) {
        tree = null;
      }

      // autoCollapse causes each expand to collapse siblings. During bulk
      // restore that can race animations ("setExpanded while animating").
      // Temporarily disable autoCollapse for this programmatic restore.
      // GH-784 / v8.1.7 #1169.
      var autoCollapse =
        tree && tree.options ? tree.options.autoCollapse : false;
      if (tree && tree.options) {
        tree.options.autoCollapse = false;
      }

      try {
        treeRoot.visit(function (node) {
          node.data.saved = true;
          // Only force-expand nodes explicitly marked as not collapsed.
          // Do not expand purely because a node has children — that undoes
          // the user's manual collapse state on every save/reload.
          if (node.data.initialViewCollapsed === "false") {
            if (typeof node.setExpanded === "function") {
              node.setExpanded(true, { noAnimation: true });
            } else if (typeof node.expand === "function") {
              node.expand(true);
            }
          }
        });
      } finally {
        if (tree && tree.options) {
          tree.options.autoCollapse = autoCollapse;
        }
      }
    }

    function displayCategoryDetails(node) {
      if (node == null) return;
      originalTitle = node.data.title;
      $("#perc-category-save-cancel-block").hide();

      $("#perc-category-name-field").prop("disabled", true);
      $("#perc-category-name-field").addClass("perc-category-field-readonly");
      $("#perc-category-name-field").attr("aria-disabled", "true");

      $("#perc-category-name-field").val(node.data.title);

      $("#perc-category-selectable-field").prop("disabled", true);
      $("#perc-category-selectable-field").addClass(
        "perc-category-field-readonly",
      );
      $("#perc-category-selectable-field").attr("aria-disabled", "true");
      var selectable = node.data.selectable;
      if (selectable === true || selectable === "true") {
        $("#perc-category-selectable-field").prop("checked", true);
      } else {
        $("#perc-category-selectable-field").prop("checked", false);
      }

      $("#perc-category-show-in-page-field").prop("disabled", true);
      $("#perc-category-show-in-page-field").addClass(
        "perc-category-field-readonly",
      );
      $("#perc-category-show-in-page-field").attr("aria-disabled", "true");
      var sinpmd = node.data.showInPgMetaData;
      if (sinpmd === "true" || sinpmd === true) {
        $("#perc-category-show-in-page-field").prop("checked", true);
      } else {
        $("#perc-category-show-in-page-field").prop("checked", false);
      }

      $("#perc-allowedsites-field").addClass("perc-category-field-readonly");
      $("#perc-allowedsites-field").prop("disabled", true);
      $("#perc-allowedsites-field").attr("aria-disabled", "true");

      addSitesToDropdown(
        "#perc-allowedsites-field",
        getAllowedSites(node),
        getSelectedSites(node),
      );

      $("#perc-category-createdby-field").val(node.data.createdBy);
      $("#perc-category-creationdt-field").val(node.data.creationDate);
      $("#perc-category-lstmodifiedby-field").val(node.data.lastModifiedBy);
      $("#perc-category-lstmodifieddt-field").val(node.data.lastModifiedDate);
    }

    function showSelectedCategoryEditor(node) {
      editing = true;
      originalTitle = node.data.title;

      $("#perc-category-name-field").prop("disabled", false);
      $("#perc-category-name-field").attr("aria-disabled", "false");

      $("#perc-category-name-field").on("keyup", function () {
        var node = container.fancytree("getActiveNode");
        var text = $(this).val();
        if (text === "") text = "[empty]";
        node.data.title = $(this).val();
        node.render();
      });

      $("#perc-category-name-field").removeClass(
        "perc-category-field-readonly",
      );

      $("#perc-allowedsites-field").removeClass("perc-category-field-readonly");
      $("#perc-allowedsites-field").prop("disabled", false);
      $("#perc-allowedsites-field").attr("aria-disabled", "false");

      $("#perc-category-selectable-field").prop("disabled", false);
      $("#perc-category-selectable-field").removeClass(
        "perc-category-field-readonly",
      );
      $("#perc-category-selectable-field").attr("aria-disabled", "false");

      $(
        '#perc-category-selectable-field option[value="' + sitename + '"]',
      ).prop("disabled", true);

      $("#perc-category-selectable-field option").on("click", function () {
        $(
          '#perc-category-selectable-field option[value="' + sitename + '"]',
        ).prop("selected", true);
      });

      $("#perc-category-show-in-page-field")
        .prop("disabled", false)
        .removeClass("perc-category-field-readonly");
      $("#perc-category-show-in-page-field").attr("aria-disabled", "false");

      $("#perc-category-save-cancel-block").show();
      $("#perc-category-name-field").trigger("focus");
    }

    function getCurrentDate() {
      var d = new Date();
      var output = d.toISOString();
      //As output will be '2022-02-22T13:18:21.942Z', so remove 'Z'
      output = output.replace("Z", "");

      return output;
    }

    function currentlyEditing() {
      var parentNode;

      w = 400;
      $.perc_utils.alert_dialog({
        title: I18N.message("perc.ui.category.view@Editing Category"),
        content: I18N.message("perc.ui.category.view@Editing Category Dialog"),
        width: w,
        okCallBack: function () {},
      });
    }

    function confirmDialog(title, message, w) {
      $.perc_utils.confirm_dialog({
        title: title,
        question: message,
        success: function () {
          if (isDelete) {
            handleDelete();
          }

          controller.getCategories();
        },
        cancel: function () {},
      });
    }

    function alertDialog(title, message, w) {
      var parentNode;

      if (w == null || w === undefined || w === "" || w < 1) w = 400;
      $.perc_utils.alert_dialog({
        title: title,
        content: message,
        width: w,
        okCallBack: function () {
          if (isDelete) {
            handleDelete();
          }

          controller.getCategories();
        },
      });
    }

    function errorDialog(title, message, w, useCallback) {
      var parentNode;

      if (w == null || w === undefined || w === "" || w < 1) w = 400;
      $.perc_utils.alert_dialog({
        title: title,
        content: message,
        width: w,
        okCallBack: function () {},
      });
    }

    function handleDelete() {
      isDelete = false;
      var node = container.fancytree("getActiveNode");
      parentNode = node.getParent();
      var upTarget = findUpTargetNode(node);

      if (node.hasChildren()) {
        node.visit(function (node) {
          node.data.deleted = true;
        });
      }
      node.data.deleted = true;
      node.data.lastModifiedBy = $.PercNavigationManager.getUserName();
      node.data.lastModifiedDate = getCurrentDate();

      updateCategoryXML();

      node.remove();

      var switchtoNode = null;
      if (upTarget != null) switchtoNode = upTarget;
      else if (parentNode != null) {
        switchtoNode = parentNode;
      }
      container.fancytree("getTree").activateKey(switchtoNode.data.key);
      displayCategoryDetails(switchtoNode);
      controller.getCategories();
    }

    function newNode(child) {
      var root = container.fancytree("getRoot");

      var destinationNode = null;
      var children = root.childList;
      if (
        children == null ||
        typeof children == "undefined" ||
        children.length === 0
      ) {
        destinationNode = root;
        child = true;
      } else if (
        children.length === 1 &&
        children[0].data.title === "New Category"
      ) {
        return children[0];
      } else {
        var destinationNode = container.fancytree("getActiveNode");
        if (
          destinationNode == null ||
          typeof destinationNode == "undefined" ||
          !destinationNode.hasOwnProperty("parent")
        ) {
          destinationNode = root;
          child = true;
        }
      }

      if (child === true) {
        addTo = destinationNode;
      } else addTo = destinationNode.getParent();

      var uid = generateUid();
      var child = addTo.addChild({
        id: uid,
        key: uid, // Explicit key helps fancytree track the node across rebuilds
        title: "New Category",
        selectable: true,
        showInPgMetaData: true,
        createdBy: $.PercNavigationManager.getUserName(),
        creationDate: getCurrentDate(),
        deleted: false,
        activate: true,
        saved: false,
        initialViewCollapsed: true,
      });

      child.visitParents(function (childnode) {
        // Prefer noAnimation when setExpanded exists to avoid "while animating" warnings
        // and ignored setActive/makeVisible (GH-784 / v8.1.7 #1169).
        if (typeof childnode.setExpanded === "function") {
          childnode.setExpanded(true, { noAnimation: true });
        } else if (typeof childnode.expand === "function") {
          childnode.expand(true);
        }
      }, true);

      if (typeof child.setActive === "function") {
        child.setActive(true);
      } else if (typeof child.activate === "function") {
        child.activate();
      }

      return child;
    }

    function editCategories(node) {
      var nodeKey = node.data.key;
      var childNode;

      if ($("#perc-allowedsites-field option:not(:checked)").length === 0) {
        allowedSites = null;
      } else {
        allowedSites = $("#perc-allowedsites-field option:selected")
          .map(function () {
            return $(this).text();
          })
          .get()
          .join(",");
      }

      var categoryname = $("#perc-category-name-field").val().trim();

      if (originalTitle !== categoryname)
        node.data.previousCategoryName = originalTitle;

      node.data.lastModifiedBy = $.PercNavigationManager.getUserName();
      node.data.lastModifiedDate = getCurrentDate();

      if (isPublished) {
        node.data.publishDate = node.data.lastModifiedDate;
        isPublished = false;
      }

      node.data.title = categoryname;

      var selectable = $("#perc-category-selectable-field").prop("checked");

      if (selectable === true) {
        node.data.selectable = "true";
      } else {
        node.data.selectable = "false";
      }
      var showInPage = $("#perc-category-show-in-page-field").prop("checked");

      if (showInPage === true) {
        node.data.showInPgMetaData = "true";
      } else {
        node.data.showInPgMetaData = "false";
      }

      if (node.data.createdBy == null) {
        node.data.createdBy = $.PercNavigationManager.getUserName();
      }

      if (node.data.creationDate == null) {
        node.data.creationDate = getCurrentDate();
      }

      node.data.allowedSites = allowedSites;
      //  Add site save

      return node;
    }

    function deleteCategory() {
      var node = container.fancytree("getActiveNode");

      if (node.hasChildren() === false) {
        confirmDialog(
          I18N.message("perc.ui.category.view@Delete Category"),
          I18N.message("perc.ui.category.view@Are You Sure"),
        );
      } else {
        confirmDialog(
          I18N.message("perc.ui.category.view@Delete Category"),
          I18N.message("perc.ui.category.view@Category And Children Deleted"),
        );
      }
    }

    function manageDynaProps() {
      var treeRoot = container.fancytree("getRoot");
      var children = [];
      treeRoot.visit(function (node) {
        var parent = node.getParent();

        if (parent.data.title == null) {
          children.push(
            node.toDict(true, function (dict) {
              delete dict.activate;
              delete dict.addClass;
              delete dict.expand;
              delete dict.focus;
              delete dict.hideCheckbox;
              delete dict.icon;
              delete dict.isFolder;
              delete dict.isLazy;
              delete dict.key;
              delete dict.noLink;
              delete dict.select;
              delete dict.tooltip;
              delete dict.saved;
              delete dict.unselectable;
            }),
          );
        }
      });

      return children;
    }

    function getUpdatedCategoryArray(tempChildList) {
      var children = [];

      for (i = 0; i < tempChildList.length; i++) {
        children.push(tempChildList[i].data);
      }

      return children;
    }

    function updateCategoryXML() {
      var catArray = manageDynaProps();
      controller.editCategories(
        catArray,
        sitename,
        function () {
          var node = container.fancytree("getActiveNode");
          displayCategoryDetails(node);
          node.data.saved = true;
          editing = false;
        },
        function () {},
      );
    }

    function save() {
      var node = container.fancytree("getActiveNode");

      if (!isMoved) {
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
      if (parentNode.data.title == null) {
        var treeRoot = container.fancytree("getRoot");

        treeRoot.visit(function (node) {
          var parent = node.getParent();

          if (parent.data.title == null) {
            i++;
            if (sourceNode.data.id !== node.data.id) tempNode = node;
            else {
              if (i > 1) {
                targetNode = tempNode;
                return false;
              }
              return false;
            }
          }
        });

        return targetNode;
      } else {
        parentNode.visit(function (node) {
          i++;
          var p = node.getParent();
          if (p.data.id === parentNode.data.id) {
            if (sourceNode.data.id !== node.data.id) tempNode = node;
            else {
              if (i > 1) {
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
      node.move(targetNode, "before");

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
      if (parentNode.data.title == null) {
        var treeRoot = container.fancytree("getRoot");

        treeRoot.visit(function (node) {
          var parent = node.getParent();

          if (parent.data.title == null) {
            if (sourceNode.data.id !== node.data.id) {
              if (i > 0) {
                targetNode = node;
                return false;
              }
            } else {
              i++;
            }
          }
        });

        return targetNode;
      } else {
        parentNode.visit(function (node) {
          var p = node.getParent();
          if (p.data.id === parentNode.data.id) {
            if (sourceNode.data.id !== node.data.id) {
              if (i > 0) {
                targetNode = node;
                return false;
              }
            } else {
              i++;
            }
          }
        });

        return targetNode;
      }
    }

    function moveNodeDown(node, targetNode) {
      node.move(targetNode, "after");

      isMoved = true;
      save();
    }

    function publishToDTS(node, deliveryServer) {
      var catArray = manageDynaProps();
      if (
        sitename == null ||
        typeof sitename == "undefined" ||
        sitename === ""
      ) {
        alertDialog(
          I18N.message("perc.ui.category.view@Select A Site"),
          I18N.message("perc.ui.category.view@Select A Site Content"),
        );
        return;
      }
      controller.publishToDTS(catArray, deliveryServer, sitename);

      isPublished = true;
      save();
    }
  };
})(jQuery);
