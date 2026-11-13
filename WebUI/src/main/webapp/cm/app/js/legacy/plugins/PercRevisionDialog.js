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
 * Revisions Dialog
 **/
(function ($) {
  //Public API for the revisions dialog.
  $.PercRevisionDialog = {
    open: openDialog,
    ITEM_TYPE_ASSET: "asset",
    ITEM_TYPE_PAGE: "page",
    ITEM_MODE_EDIT: "edit",
    ITEM_MODE_VIEW: "view",
  };
  /**
   * Opens the revisions dialog and shows the revisions in a table for the supplied item.
   * @param itemId(String), assumed to be a valid guid of the item (Page or Asset)
   * @param itemName(String) name of the item to be shown part of the dialog.
   * @param itemType(String), The type of the item. Use ITEM_TYPE_XXX constants.
   * @param itemMode (String), the mode for the dialog, view or edit, used for showing the appropriate actions.
   * Use ITEM_MODE_XXX constants.
   */
  function openDialog(itemId, itemName, itemType, itemMode) {
    //Makes a service call and gets the revisions, passes the createDialog as the callback
    var ctx = {
      itemId: itemId,
      itemName: itemName,
      itemType: itemType,
      itemMode: itemMode,
      dialog: null,
      allRevisions: new Map([]),
      latestRevision: null,
      itemCount: 0,
      isRestorable: false,
      restorableIcon: "restoreInactive.png",
    };
    $.PercRevisionService.getRevisionDetails(itemId, function (status, result) {
      createDialog(status, result, ctx);
    });
  } // End open dialog

  /**
   * Creates the dialog and sets the field values from the supplied result.data object.
   * On dialog open calls the addRevisionRows to add the revision rows.
   */
  function createDialog(status, result, ctx) {
    if (status === $.PercServiceUtils.STATUS_ERROR) {
      showRevisionServiceError(result);
      return;
    }
    var dialogHTML = createRevisionTable();
    ctx.dialog = $(dialogHTML).perc_dialog(
      buildRevisionDialogConfig(ctx, result),
    );
    $("#perc-revisions-container").css("min-height", "350px");
    $("#revisionsTable_paginate").css("right", "-25px");
  }

  /**
   * Shows the service error alert for a failed revision details request.
   */
  function showRevisionServiceError(result) {
    var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(
      result.request,
    );
    $.perc_utils.alert_dialog({ title: "Error", content: defaultMsg });
  }

  /**
   * Builds the perc_dialog configuration object for the revisions dialog.
   */
  function buildRevisionDialogConfig(ctx, result) {
    var percDialogObject = {
      resizable: false,
      title:
        I18N.message("perc.ui.revisionDialog.title@Revisions") +
        ": " +
        ctx.itemName,
      modal: true,
      closeOnEscape: false,
      percButtons: {},
      id: "perc-revisions-dialog",
      open: function () {
        addRevisionRows(result.data, ctx);
      },
      width: 700,
      height: 490,
    };

    percDialogObject.percButtons.Close = {
      click: function () {
        ctx.dialog.remove();
      },
      id: "perc-revisions-dialog-close",
    };
    return percDialogObject;
  }

  /**
   * Creates the revision table html, with wrapper div. Empty tbody is added.
   * @return revision dialog html.
   */
  function createRevisionTable() {
    return $(
      "<div class='dataTables_wrapper' style='height: 100%; z-index: 4470;position:relative;' id='perc-revisions-container'>" +
        "<table id='revisionsTable' style='width:100%'>" +
        "<thead>" +
        "<tr>" +
        "<th style='text-align:left;width:10%' id='perc-header-revision'><span class='col-name'>" +
        I18N.message("perc.ui.revisionDialog@Revision") +
        "</span></th>" +
        "<th style='text-align:left;width:20%' id='perc-header-status'><span class='col-name'>" +
        I18N.message("perc.ui.revisionDialog@Status") +
        "</span></th>" +
        "<th style='text-align:left;width:35%' id='perc-header-last-modified'><span class='col-name'>" +
        I18N.message("perc.ui.revisionDialog@Last Modified") +
        "</span></th>" +
        "<th style='text-align:left;width:20%' id='perc-header-last-modifier'><span class='col-name'>" +
        I18N.message("perc.ui.revisionDialog@Last Modifier") +
        "</span></th>" +
        "<th style='text-align:left;width:15%' id='perc-header-actions'><span class='col-name'>" +
        I18N.message("perc.ui.revisionDialog@Actions") +
        "</span></th>" +
        "</tr>" +
        "</thead>" +
        "<tbody></tbody>" +
        "</table>" +
        "</div>",
    );
  }

  /**
   * Creates the revision rows html appends them to the tbody element of the table and converts the table into
   * a datatable by adding the appropriate sort columns and handles the paging link display.
   * @param revisionData, The revision data is expected to be in the format of
   * com.percussion.itemmanagement.data.PSRevisions
   * @param ctx, dialog context carrying the item descriptors and accumulated revision state.
   */
  function addRevisionRows(revisionData, ctx) {
    var revisions = normalizeRevisions(revisionData.RevisionsSummary);
    ctx.itemCount = revisions.length;
    ctx.isRestorable = revisionData.RevisionsSummary.restorable;
    ctx.restorableIcon = ctx.isRestorable
      ? "restore.png"
      : "restoreInactive.png";
    var $tbody = $("#revisionsTable").find("tbody");

    for (var i in revisions) {
      appendRevisionRow($tbody, revisions[i], ctx);
    }

    bindRevisionRowHandlers(ctx);
    bindRowSelection(initRevisionsTable(ctx));
  }

  /**
   * Normalizes the revisions payload into an array (single-revision
   * responses arrive as a lone object instead of an array).
   */
  function normalizeRevisions(revSummary) {
    if (Array.isArray(revSummary.revisions)) {
      return revSummary.revisions;
    }
    return [revSummary.revisions];
  }

  /**
   * Builds one revision row, tracks it in the context revision map, and
   * appends it to the table body.
   */
  function appendRevisionRow($tbody, revdata, ctx) {
    var lastModifiedDate = new Date(revdata.lastModifiedDate);
    //This timestamp will be used to set the data-order attribute of datatable to sort it as date on basis of timestamp
    var timeStamp = lastModifiedDate.getTime();
    var lastModifiedDateParts = $.perc_utils.splitDateTime(
      revdata.lastModifiedDate,
    );
    var revObj = {
      revId: revdata.revId,
      status: revdata.status,
      lastModified:
        lastModifiedDateParts.date + " " + lastModifiedDateParts.time,
      modifier: revdata.lastModifier,
    };
    ctx.allRevisions.set(revObj.revId, revObj);

    if (ctx.itemCount === revdata.revId) {
      ctx.latestRevision = revObj;
    }
    var $rowHTML = $(buildRevisionRowHtml(revdata, revObj, timeStamp, ctx));
    $rowHTML.find("td:eq(2)").data("timedate", lastModifiedDate);
    $tbody.append($rowHTML);
  }

  /**
   * Builds the html for a single revision row.
   */
  function buildRevisionRowHtml(revdata, revObj, timeStamp, ctx) {
    return (
      "<tr>" +
      "<td style='vertical-align: middle'><div class='data-cell perc-ellipsis'>" +
      revdata.revId +
      "</div></td>" +
      "<td style='vertical-align: middle'><div for='" +
      revdata.status +
      "' class='data-cell perc-ellipsis'>" +
      revdata.status +
      "</div></td>" +
      "<td data-order ='" +
      timeStamp +
      "' style='vertical-align: middle''><div class='data-cell perc-ellipsis'>" +
      revObj.lastModified +
      "</div></td>" +
      "<td style='vertical-align: middle'><div class='data-cell perc-ellipsis'>" +
      revdata.lastModifier +
      "</div></td>" +
      "<td style='vertical-align: middle'><div class='data-cell perc-ellipsis'>" +
      buildActionCellHtml(revdata, ctx) +
      "</div></td>" +
      "</tr>"
    );
  }

  /**
   * Builds the actions cell: a Latest label for the current revision,
   * otherwise the preview / compare / restore icons.
   */
  function buildActionCellHtml(revdata, ctx) {
    if (ctx.itemCount === revdata.revId) {
      return (
        "<span title='" +
        I18N.message("perc.ui.revisionDialog.tooltip@LatestRevision") +
        "'>" +
        I18N.message("perc.ui.revisionDialog.label@Latest") +
        "</span>"
      );
    }
    return (
      "<img title= '" +
      I18N.message("perc.ui.revisionDialog.tooltip@PreviewRevision") +
      "' alt='" +
      I18N.message("perc.ui.revisionDialog.tooltip@PreviewRevision") +
      "' revId='" +
      revdata.revId +
      "' class='perc-revisions-preview-img' style='vertical-align:middle;margin-right:1px;' src='/cm/images/icons/editor/preview.png' />" +
      "<img title= '" +
      I18N.message("perc.ui.revisionDialog.compare@compareRevisions") +
      "' 'id='compare'  alt='Compare this revision to latest revision' " +
      "' revId='" +
      revdata.revId +
      "' class='perc-revisions-compare-img' style='vertical-align:middle;margin-right:1px;' src='/cm/images/icons/editor/revision-compare.png' />" +
      "<img title= '" +
      I18N.message("perc.ui.revisionDialog.tooltip@Restorable") +
      "' alt='restorableTitle' revId='" +
      revdata.revId +
      "' class='perc-revisions-restore-img' style='vertical-align:middle;margin-right:1px;' src='/cm/images/icons/editor/" +
      ctx.restorableIcon +
      "'  />"
    );
  }

  /**
   * Binds the compare / preview / hover / restore handlers for the rows.
   */
  function bindRevisionRowHandlers(ctx) {
    bindCompareHandler(ctx);
    bindPreviewHandler(ctx);
    bindPreviewHover();
    //Hanlde restore click events and icon hover events if the item is restorable
    if (ctx.isRestorable) {
      bindRestoreHandler(ctx);
      bindRestoreHover();
    }
  }

  /**
   * Binds the compare-icon clicks (page items only).
   */
  function bindCompareHandler(ctx) {
    $("#revisionsTable")
      .find(".perc-revisions-compare-img")
      .on("click", function () {
        var revId = $(this).attr("revId");
        if (ctx.itemType === $.PercRevisionDialog.ITEM_TYPE_PAGE) {
          $.perc_finder().launchPageCompareView(
            ctx.itemId,
            ctx.itemName,
            Number(revId),
            Number(ctx.latestRevision.revId),
            ctx.allRevisions,
          );
        }
      });
  }

  /**
   * Binds the preview-icon clicks for pages and assets.
   */
  function bindPreviewHandler(ctx) {
    $("#revisionsTable")
      .find(".perc-revisions-preview-img")
      .on("click", function () {
        var revId = $(this).attr("revId");
        if (ctx.itemType === $.PercRevisionDialog.ITEM_TYPE_PAGE) {
          $.perc_finder().launchPagePreview(ctx.itemId, revId);
        } else if (ctx.itemType === $.PercRevisionDialog.ITEM_TYPE_ASSET) {
          $.perc_finder().launchAssetPreview(ctx.itemId, revId);
        } else {
          var eMsg = "Cannot preview unknown type.";
          $.perc_utils.alert_dialog({ title: "Error", content: eMsg });
        }
      });
  }

  /**
   * Binds the preview-icon hover image swaps.
   */
  function bindPreviewHover() {
    $("#revisionsTable")
      .find(".perc-revisions-preview-img")
      .on("mouseenter", function () {
        $(this).attr("src", "/cm/images/icons/editor/previewOver.png");
      });
    $("#revisionsTable")
      .find(".perc-revisions-preview-img")
      .on("mouseleave", function () {
        $(this).attr("src", "/cm/images/icons/editor/preview.png");
      });
  }

  /**
   * Binds the restore-icon clicks behind a confirm dialog.
   */
  function bindRestoreHandler(ctx) {
    $("#revisionsTable")
      .find(".perc-revisions-restore-img")
      .on("click", function () {
        var revId = $(this).attr("revId");
        $.perc_utils.confirm_dialog({
          title: "Restore Revision",
          question:
            "Want to Restore Revision (" + revId + ") as current revision?",
          success: function () {
            $.blockUI();
            $.PercRevisionService.restoreRevision(
              ctx.itemId,
              revId,
              function (status, result) {
                afterRestore(status, result, ctx);
              },
            );
          },
          cancel: function () {},
        });
      });
  }

  /**
   * Binds the restore-icon hover image swaps.
   */
  function bindRestoreHover() {
    $("#revisionsTable")
      .find(".perc-revisions-restore-img")
      .on("mouseenter", function () {
        $(this).attr("src", "/cm/images/icons/editor/restoreOver.png");
      });
    $("#revisionsTable")
      .find(".perc-revisions-restore-img")
      .on("mouseleave", function () {
        $(this).attr("src", "/cm/images/icons/editor/restore.png");
      });
  }

  /**
   * Converts the revision table into a DataTable with paging and sorting.
   * @return the DataTable API instance.
   */
  function initRevisionsTable(ctx) {
    // DataTables 3.x (issue #6002): DataTable() returns the 1.10+ API
    // instance (table.$ below is the scoped-selector API, no fn* legacy).
    return $("#revisionsTable").DataTable({
      order: [[0, "desc"]],
      bFilter: false,
      bAutoWidth: false,
      // turn on pagination and use sequential and random access pagination controls
      bPaginate: true,
      sPaginationType: "full_numbers",
      iDisplayLength: 10,
      bLengthChange: false,
      bInfo: true,
      // if table has no rows show the following error
      oLanguage: { sZeroRecords: "No Revisions Found" },

      // if on first or last page, update the disabled color of the sequential pagination controls
      footerCallback: function (nFoot, aasData, iStart, iEnd, aiDisplay) {
        updatePaginationControls(iStart, iEnd, ctx.itemCount);
      },
    });
  }

  /**
   * Updates the disabled color of the sequential pagination controls
   * depending on the current page window.
   */
  function updatePaginationControls(iStart, iEnd, itemCount) {
    // set them all to their default active color
    $(".first").addClass("perc-active").removeClass("perc-disabled");
    $(".previous").addClass("perc-active").removeClass("perc-disabled");
    $(".next").addClass("perc-active").removeClass("perc-disabled");
    $(".last").addClass("perc-active").removeClass("perc-disabled");
    // if on the first page disable First and Previous controls
    if (iStart === 0) {
      $(".first").addClass("perc-disabled").removeClass("perc-active");
      $(".previous").addClass("perc-disabled").removeClass("perc-active");
    }
    // if on the last page disable Last and Next controls
    if (iEnd === itemCount) {
      $(".next").addClass("perc-disabled").removeClass("perc-active");
      $(".last").addClass("perc-disabled").removeClass("perc-active");
    }
  }

  /**
   * Binds single-row selection toggling on the revision table.
   */
  function bindRowSelection(table) {
    $("#revisionsTable tbody").on("click", "tr", function () {
      if ($(this).hasClass("selected")) {
        $(this).removeClass("selected");
      } else {
        table.$("tr.selected").removeClass("selected");
        $(this).addClass("selected");
      }
    });
  }

  /**
   * Call back function that is passed to the $.PercRevisionService.restoreRevision method.
   * If the status is error, then displays the error. Otherwise closes the dialog and opens the item in edit mode.
   * The parameters match with the caller $.PercServiceUtils.makeJsonRequest method.
   */
  function afterRestore(status, result, ctx) {
    if ($.blockUI) {
      $.unblockUI();
    }

    if (status === $.PercServiceUtils.STATUS_ERROR) {
      var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(
        result.request,
      );
      $.perc_utils.alert_dialog({
        title: I18N.message("perc.ui.publish.title@Error"),
        content: defaultMsg,
      });
      return;
    }
    ctx.dialog.remove();
    $.PercNavigationManager.setReopenAllowed(true);
    var item = {
      id: $.PercNavigationManager.getId(),
      name: $.PercNavigationManager.getName(),
      path: $.PercNavigationManager.getPath(),
    };
    if (
      $.PercNavigationManager.getView() ===
      $.PercNavigationManager.VIEW_EDITOR
    ) {
      $.PercNavigationManager.handleOpenPage(item, true);
    } else if (
      $.PercNavigationManager.getView() ===
      $.PercNavigationManager.VIEW_EDIT_ASSET
    ) {
      $.PercNavigationManager.handleOpenAsset(item, true);
    } else {
      // This should never happen.
      var eMsg = I18N.message(
        "perc.ui.revision.dialog@Cannot Open Unknown View",
      );
      $.perc_utils.alert_dialog({
        title: I18N.message("perc.ui.publish.title@Error"),
        content: eMsg,
      });
    }
  }
})(jQuery);
