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
 *  PercInlineEditDataTable.js
 *  @author federicoromanelli
 *
 *  Description:
 *  Thin plugin layer over jQuery's datatable plugin http://www.datatables.net/
 *  to implement Percussion specific custom behavior.
 *
 *  @param percHeaders   (string[]) Labels for the headers
 *  @param percColsLeft  (int[]) indices of columns to show if gadget is on the left
 *  @param percColsRight (int[]) indices of columns to show if gadget is on the right
 *  @param percData      (string[][]) 2 dimentional array of data. rows of columns
 *  @param percHeaderClasses (string[]) classes to add to each of the headers
 *
 *  Maintenance note (GH #6197): the former monolithic `init` was decomposed
 *  into small IIFE-scope helpers threaded on a grid context
 *  (`{ table, config, columnWidths, headers, percRows, indices, oTable }`).
 *  Zero behavior change is intended: statement order, DataTables 1.10+ call
 *  sites, event wiring, and the caller-visible mutations of the config arrays
 *  are preserved exactly as they were inside `init`.
 */
(function ($) {
  var nEditing = null;

  // Exposed API/interface
  $.PercInlineEditDataTable = {
    init: init,
    enableTable: enableTable,
  };

  var defaultConfig = {
    percHeaderClasses: [],
    percDeleteRow: true,
    percEditableCols: [],
    bFilter: false,
    bAutoWidth: false,
    bPaginate: false,
    bInfo: false,
    bSort: false,
    percNoTableHeaders: false,
    percAddRowElementId: "perc-inline-edit-datatable-add-row",
    percNewRowDefaultValues: [],
    percShowValuesPlaceholders: false,
    percPlaceHolderValues: [],
    drawCallback: function () {
      // fix the ellipsis on every draw
      if ($.browser.msie) {
        $(".perc-ellipsis").each(function () {
          handleOverflow($(this));
        });
      }
    },
  };

  function enableTable(container, enabled) {
    var table = $(container);
    if (enabled) {
      table.find("td").removeClass("perc-disabled");
    } else {
      table.find("td").addClass("perc-disabled");
    }
  }

  function init(container, config, dlgCallbackFunc) {
    nEditing = null;
    var ctx = createGridContext(container, config);
    buildGridHeaders(ctx);
    buildGridRows(ctx);
    ctx.oTable = createDataTable(ctx);
    if (ctx.percRows.length == 0) {
      addRow(ctx);
    }
    wireGridEvents(ctx);
    fixEllipsisForIE(ctx.table);
    ctx.table.css("width", "inherit");
    addPlaceHolder(ctx.table);
    return ctx.oTable;
  }

  // Shared per-grid state threaded through the helpers below. `oTable` is
  // assigned once the DataTable instance exists; handlers registered before
  // that only run on user interaction, i.e. after the assignment.
  function createGridContext(container, rawConfig) {
    var table = $(container);
    $("#" + rawConfig.percAddRowElementId).off("click");
    $(table).find("td span").off("click");
    $(table).find("td input").off("click").off("focusout");
    $(table).find("a.perc-inline-edit-datatable-delete-row").off("click");
    table.html("");

    var columnWidths = [];
    if (rawConfig.percColumnWidths && rawConfig.percColumnWidths.length > 0)
      columnWidths = rawConfig.percColumnWidths;

    // merge and override default and custom configurations
    var config = $.extend({}, defaultConfig, rawConfig);

    table.addClass("perc-inline-edit-datatable");

    return {
      table: table,
      config: config,
      columnWidths: columnWidths,
      headers: config.percHeaders,
      percRows: config.percData,
      indices: 0,
      oTable: null,
    };
  }

  function buildGridHeaders(ctx) {
    var config = ctx.config;
    if (config.percDeleteRow) {
      ctx.headers.push("");
      ctx.columnWidths.push("10");
      config.percNewRowDefaultValues.push("");
    }
    ctx.indices = ctx.headers.length;

    // create headers
    ctx.table.append("<thead><tr>");
    var tableHeaderRow = ctx.table.find("thead tr");
    for (var i = 0; i < ctx.indices; i++) {
      tableHeaderRow.append(
        buildHeaderCell(config.percHeaderClasses, ctx.columnWidths, ctx.headers[i], i),
      );
    }
  }

  function buildHeaderCell(headerClasses, columnWidths, header, index) {
    var headerClass = "";
    if (headerClasses && headerClasses.length > 0)
      headerClass = headerClasses[index];
    var headerWidth = widthAt(columnWidths, index);
    return (
      "<th class='perc-datatable-header perc-col-" +
      index +
      " " +
      headerClass +
      "' style='width:" +
      headerWidth +
      "px; max-width:" +
      headerWidth +
      "px'><span>" +
      header +
      "</span></th>"
    );
  }

  function widthAt(columnWidths, index) {
    if (columnWidths && columnWidths.length > 0)
      return columnWidths[index];
    return "";
  }

  function buildGridRows(ctx) {
    // create rows
    ctx.table.append("<tbody>");
    var tableBody = ctx.table.find("tbody");
    tableBody.html("");
    for (var r = 0; r < ctx.percRows.length; r++) {
      appendGridRow(ctx, tableBody, ctx.percRows[r], r);
    }
    if (ctx.config.percNoTableHeaders) {
      ctx.table.find("thead").css("display", "none");
    }
  }

  function appendGridRow(ctx, tableBody, percRow, rowIndex) {
    if (ctx.config.percDeleteRow) {
      percRow.rowContent.push(
        '<a class="perc-inline-edit-datatable-delete-row" href=""></a>',
      );
    }
    var tableRow = $("<tr class='perc-row-" + rowIndex + "'>");
    for (var i = 0; i < ctx.indices; i++) {
      tableRow.append(buildGridCell(ctx, percRow, rowIndex, i));
    }
    tableBody.append(tableRow);
  }

  function buildGridCell(ctx, percRow, rowIndex, colIndex) {
    var columnWidth = widthAt(ctx.columnWidths, colIndex);
    var tableData = $(
      "<td style='width:" +
        columnWidth +
        "px; max-width:" +
        columnWidth +
        "px' id='perc-cell-" +
        rowIndex +
        "-" +
        colIndex +
        "' class='perc-cell-" +
        rowIndex +
        "-" +
        colIndex +
        "'>",
    );
    var data = percRow.rowContent[colIndex];
    if (data === "" || data === undefined) data = "&nbsp;";

    var placeHolderClass = "";
    if (ctx.config.percShowValuesPlaceholders) {
      var placeHolderValue = placeHolderForCell(ctx.config, rowIndex, colIndex);
      if (data.trim() === placeHolderValue) {
        placeHolderClass = "perc-placeholder";
      }
    }

    tableData.append(
      "<span class='" + placeHolderClass + "'>" + data + "</span>",
    );
    return tableData;
  }

  function placeHolderForCell(config, rowIndex, colIndex) {
    if ($.type(config.percPlaceHolderValues[colIndex]) === "string") {
      return config.percPlaceHolderValues[colIndex];
    }
    return config.percPlaceHolderValues[rowIndex][colIndex];
  }

  function resolvePlaceHolderValue(ctx, nRow, colIndex) {
    return placeHolderForCell(ctx.config, ctx.oTable.row(nRow).index(), colIndex);
  }

  // DataTables 3.x (issue #6002): the npm datatables.net line removed the
  // legacy fn* instance API, so this widget uses the 1.10+ API throughout
  // (DataTable() constructor, row()/cell()/draw()/destroy()). A previous
  // grid on the same table is destroyed first; callers additionally pass
  // bDestroy/destroy for re-init safety.
  function createDataTable(ctx) {
    if ($.fn.DataTable.isDataTable(ctx.table)) {
      ctx.table.DataTable().destroy();
    }
    return ctx.table.DataTable(ctx.config);
  }

  function wireGridEvents(ctx) {
    rebindCellInputs(ctx);
    rebindCellSpans(ctx);
    wireAddRowButton(ctx);
    bindDeleteRowButtons(ctx);
  }

  function rebindCellInputs(ctx) {
    $(ctx.table)
      .find("td input")
      .on("focusout", function (e) {
        tableCellFocusOut(ctx, this, e);
      });
  }

  function rebindCellSpans(ctx) {
    $(ctx.table)
      .find("td span")
      .on("click", function (e) {
        tableCellOnClick(ctx, this, e);
      });
  }

  function bindDeleteRowButtons(ctx) {
    $(ctx.table)
      .find("a.perc-inline-edit-datatable-delete-row")
      .on("click", function (e) {
        deleteRowButtonAction(ctx, e);
      });
  }

  function rebindDeleteButton(ctx, nRow) {
    $(nRow)
      .find(".perc-inline-edit-datatable-delete-row")
      .on("click", function (e) {
        deleteRowButtonAction(ctx, e);
      });
  }

  function wireAddRowButton(ctx) {
    if (ctx.config.percAddRowElementId && ctx.config.percAddRowElementId != "") {
      // Add row action
      $("#" + ctx.config.percAddRowElementId).on("click", function (e) {
        handleAddRowClick(ctx, e);
      });
    }
  }

  function handleAddRowClick(ctx, e) {
    e.preventDefault();
    var data = ctx.oTable.rows().data().toArray();
    if (data.length === 0) {
      addRow(ctx);
      rebindCellInputs(ctx);
      return;
    }
    var firstRowValues = firstRowCellTexts(data[0]);
    if (
      data.length > 1 ||
      (data.length === 1 && firstRowValues[0] !== "" && firstRowValues[1] !== "")
    ) {
      addRow(ctx);
      rebindCellInputs(ctx);
    }
  }

  function firstRowCellTexts(firstRow) {
    return [extractCellValue(firstRow[0]), extractCellValue(firstRow[1])];
  }

  // Reads the trimmed text of a DataTable cell payload, which is either an
  // HTML fragment (span-wrapped) or a raw string. Throws on undefined input,
  // exactly like the inline code it replaces.
  function extractCellValue(cellHtml) {
    if ($(cellHtml).length > 0) {
      return $(cellHtml).text().trim();
    }
    return cellHtml.trim();
  }

  // Row-edit variant: tolerates undefined payloads (yields "") like the
  // original editRow fallback.
  function extractRowCellValue(cellHtml) {
    if ($(cellHtml).length > 0) {
      return $(cellHtml).text().trim();
    }
    var value = "";
    if (typeof cellHtml != "undefined") {
      value = cellHtml.trim();
    }
    return value;
  }

  function blankIfPlaceHolder(config, value, placeHolderValue) {
    if (config.percShowValuesPlaceholders && value === placeHolderValue) {
      return "";
    }
    return value;
  }

  function createCellEditor(colIndex, placeHolderValue, value) {
    return $("<input/>")
      .attr("id", "inputEdition" + colIndex)
      .attr("placeholder", placeHolderValue)
      .attr("type", "text")
      .val(value);
  }

  function appendEditorLabel(table, colIndex) {
    var label = $("<label/>")
      .addClass("visuallyhidden")
      .attr("for", "inputEdition" + colIndex)
      .text("Search:");
    $(table).parent().append(label);
  }

  function editCol(ctx, nRow, nCol) {
    if (typeof nRow === "undefined" || nCol === "undefined") {
      return;
    }
    var config = ctx.config;
    var aData = ctx.oTable.row(nRow).data();
    $(nRow).addClass("perc-edit-mode");
    var idParts = nCol.id.split("-");
    var colNumber = parseInt(idParts.pop());
    if (!config.percEditableCols[colNumber]) return;
    var placeHolderValue = resolvePlaceHolderValue(ctx, nRow, colNumber);
    var value = blankIfPlaceHolder(
      config,
      extractCellValue(aData[colNumber]),
      placeHolderValue,
    );
    $(nCol).empty().append(createCellEditor(colNumber, placeHolderValue, value));
    rebindCellInputs(ctx);
    appendEditorLabel(ctx.table, colNumber);
    addPlaceHolder(ctx.table);

    $(nCol).find("input").trigger("focus");
  }

  // Edit Cells
  function editRow(ctx, nRow, isOnlyRow) {
    var jqTds = $(">td", nRow);
    $(nRow).addClass("perc-edit-mode");
    for (var i = 0; i < jqTds.length; i++) {
      //because third column is for delete button.
      if (i > 1) {
        break;
      }
      editRowCell(ctx, nRow, jqTds, i);
    }
    addPlaceHolder(ctx.table);

    if (!isOnlyRow) {
      $(jqTds[0]).find("input").trigger("focus");
    }
  }

  function editRowCell(ctx, nRow, jqTds, colIndex) {
    var placeHolderValue = resolvePlaceHolderValue(ctx, nRow, colIndex);
    appendEditorLabel(ctx.table, colIndex);
    var value = blankIfPlaceHolder(
      ctx.config,
      extractRowCellValue(ctx.oTable.row(nRow).data()[colIndex]),
      placeHolderValue,
    );
    $(jqTds[colIndex])
      .empty()
      .append(createCellEditor(colIndex, placeHolderValue, value));
  }

  function restoreRow(ctx, nRow) {
    var oTable = ctx.oTable;
    var aData = oTable.row(nRow).data();
    var jqTds = $(">td", nRow);
    var length = jqTds.length;
    if (ctx.config.percDeleteRow) length--;
    for (var i = 0, iLen = length; i < iLen; i++) {
      oTable.cell(nRow, i).data("<span>" + aData[i] + "</span>");
    }

    oTable.draw();
    $(nRow).append(
      '<td><a class="perc-inline-edit-datatable-delete-row" href=""></a></td>',
    );
  }

  function saveRow(ctx, nRow) {
    var config = ctx.config;
    var oTable = ctx.oTable;
    var jqInputs = $("input", nRow);
    $(nRow).removeClass("perc-edit-mode");
    var savedFirst = resolveSavedCellValue(ctx, nRow, 0, jqInputs[0].value);
    var savedSecond = resolveSavedCellValue(ctx, nRow, 1, jqInputs[1].value);

    oTable.cell(nRow, 0).data(
      "<span class='" + savedFirst.placeHolderClass + "'>" + savedFirst.value + "</span>",
    );
    oTable.cell(nRow, 1).data(
      "<span class='" + savedSecond.placeHolderClass + "'>" + savedSecond.value + "</span>",
    );
    oTable.draw();
    if (config.percDeleteRow) {
      $(nRow).append(
        '<td style="width:10px; max-width:10px"><a style="display:none;" class="perc-inline-edit-datatable-delete-row" href=""></a></td>',
      );
    }
    var newRowsDefaultValues = defaultConfig.percNewRowDefaultValues;
    if (jqInputs[0].value.trim() === newRowsDefaultValues[0]) {
      deleteRow(ctx, nRow);
    }
  }

  function resolveSavedCellValue(ctx, nRow, colIndex, rawValue) {
    var saved = { value: rawValue, placeHolderClass: "" };
    if (ctx.config.percShowValuesPlaceholders && rawValue.trim() === "") {
      saved.placeHolderClass = "perc-placeholder";
      saved.value = resolvePlaceHolderValue(ctx, nRow, colIndex);
    }
    return saved;
  }

  // Best-effort restoreRow wrapper (DataTables 3.x, issue #6002):
  // restoreRow may throw when the edited row was detached mid-edit.
  // Returns false in that case; callers proceed with the grid update
  // regardless (pre-existing addRow semantics preserved).
  function tryRestoreRow(ctx, nRow) {
    try {
      restoreRow(ctx, nRow);
      return true;
    } catch (e) {
      return false;
    }
  }

  function saveCol(ctx, nRow, nCol) {
    var jqInputs = $("input", nCol);
    $(nRow).removeClass("perc-edit-mode");
    var idParts = nCol.id.split("-");
    var colNumber = parseInt(idParts.pop());
    var saved = resolveSavedCellValue(ctx, nRow, colNumber, jqInputs[0].value);

    oTableCellData(ctx, nRow, colNumber, saved.value);
    ctx.oTable.draw();
    rebindCellSpans(ctx);
    var newRowsDefaultValues = defaultConfig.percNewRowDefaultValues;
    if (jqInputs[0].value.trim() === newRowsDefaultValues[0]) {
      deleteRow(ctx, nRow);
    }

    if (colNumber === 1) {
      displayDeleteButton(ctx, nRow);
    }
  }

  // saveCol always rewrites the cell with the placeholder class, matching
  // the historical inline statement (not the computed saved class).
  function oTableCellData(ctx, nRow, colNumber, value) {
    ctx.oTable.cell(nRow, colNumber).data(
      "<span class='perc-placeholder'>" + value + "</span>",
    );
  }

  function displayDeleteButton(ctx, nRow) {
    var jqTds = $("td", nRow);
    if ($(jqTds[1]).find("span").text().trim() !== "") {
      $(nRow)
        .find(".perc-inline-edit-datatable-delete-row")
        .css("display", "block");
      rebindDeleteButton(ctx, nRow);
    }
  }

  function addColIds(ctx, nRow) {
    var jqTds = $(">td", nRow);
    var rowNumber = ctx.oTable.row(nRow).index();
    for (var i = 0, iLen = jqTds.length; i < iLen; i++) {
      $(jqTds[i]).attr("id", "perc-cell-" + rowNumber + "-" + i);
    }
  }

  function tableCellFocusOut(ctx, input, e) {
    $(ctx.table).parent().find("label.visuallyhidden-with-placeholder").remove();
    $(ctx.table).parent().find("label.visuallyhidden").remove();
    e.stopPropagation();
    e.preventDefault();

    var nRow = $(input).parents("tr")[0];
    var nCol = $(input).parents("td")[0];
    if (ctx.oTable.rows().data().length === 1) {
      nEditing = nRow;
    }
    if (nEditing === nRow) {
      var jqInputs = $("input", nRow);
      if (jqInputs.length > 1) {
        saveRow(ctx, nEditing);
      } else {
        saveCol(ctx, nEditing, nCol);
      }
      nEditing = null;

      continueRowEdit(ctx, nRow, jqInputs);
    }
  }

  function continueRowEdit(ctx, nRow, jqInputs) {
    if (jqInputs.length <= 1) {
      return;
    }
    nEditing = nRow;
    var jqTds = $("td", nRow);
    if ($(jqTds[0]).find("span").text().trim() === "") {
      return;
    }
    if ($(jqTds[1]).find("span").text().trim() === "") {
      editCol(ctx, nRow, jqTds[1]);
    } else {
      $(nRow)
        .find(".perc-inline-edit-datatable-delete-row")
        .css("display", "block");
    }
  }

  function tableCellOnClick(ctx, cell, e) {
    e.stopPropagation();
    e.preventDefault();
    if (!$(cell).parents("td").hasClass("perc-disabled")) {
      /* Get the row as a parent of the link that was clicked on */
      var nRow = $(cell).parents("tr")[0];
      var nCol = $(cell).parents("td")[0];

      if (nEditing !== null && nEditing !== nRow) {
        /* Currently editing - but not this row - restore the old before continuing to edit mode */
        tryRestoreRow(ctx, nEditing);
      }
      /* No edit in progress (or just restored) - let's start one */
      editCol(ctx, nRow, nCol);
      nEditing = nRow;
    }
  }

  function addRow(ctx, isOnlyRow) {
    var newRowsDefaultValues = Array.from(ctx.config.percNewRowDefaultValues);

    var newRow = ctx.oTable.row.add(newRowsDefaultValues);

    if (nEditing !== null && nEditing !== nRow) {
      // Best-effort restore only: a half-edited row must not block addRow.
      tryRestoreRow(ctx, nEditing);
    }
    ctx.oTable.draw(false);
    var nRow = newRow.node();
    addColIds(ctx, nRow);
    editRow(ctx, nRow, isOnlyRow);
    nEditing = nRow;
  }
  // Delete row action
  function deleteRowButtonAction(ctx, e) {
    e.stopPropagation();
    e.preventDefault();
    var nRow = $(e.currentTarget).parents("tr")[0];
    deleteRow(ctx, nRow);
  }

  function deleteRow(ctx, nRow) {
    var oTable = ctx.oTable;
    oTable.row(nRow).remove().draw();
    if (oTable.rows().data().length === 0) {
      $("#" + ctx.config.percAddRowElementId).trigger("click");
    } else if (oTable.rows().data().length === 1) {
      pruneLastEmptyRow(ctx);
    }
  }

  function pruneLastEmptyRow(ctx) {
    var tableId = ctx.oTable.table().node().id;
    var lastRow = $("#" + tableId).find("tbody tr");
    var cells = $(lastRow).find("td");
    var val1 = $(cells[0]).find("span").text();
    var val2 = $(cells[1]).find("span").text();
    if (val1.trim() === "" && val2.trim() === "") {
      deleteRow(ctx, lastRow);
    }
  }

  function addPlaceHolder(table) {
    if (!(
      "placeholder" in $("<input>")[0] || "placeHolder" in $("<input>")[0]
    )) {
      table.find("input[placeholder]").placeHolder({ hideOnFocus: false });
    }
  }

  // only fix ellipsis in IE
  function fixEllipsisForIE(table) {
    if (!$.browser.msie) {
      return;
    }
    // fix text overflow at first
    $(".perc-ellipsis").each(function () {
      handleOverflow($(table));
    });

    // fix text overflow when window resizes
    $(window).on("resize", function () {
      $(".perc-ellipsis").each(function () {
        handleOverflow($(table));
      });
    });
  }

  function handleOverflow(element) {
    var title = element.attr("title");
    if (title === "") return true;
    var width = element.parents("td").width();
    element.css("width", width);
  }
})(jQuery);
