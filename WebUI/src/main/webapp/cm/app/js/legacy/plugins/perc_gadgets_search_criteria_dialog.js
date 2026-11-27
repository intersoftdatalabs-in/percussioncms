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
 * Schedules Dialog
 */
(function ($) {
  $.perc_gadgets_search_criteria_dialog = {
    openSearchCriteriaDialog: openSearchCriteriaDialog,
    getSearchConfig: getSearchConfig,
  };

  function getSearchConfig(metakey, callback) {
    percJQuery.PercMetadataService.find(metakey, function (status, data) {
      if (status === percJQuery.PercServiceUtils.STATUS_SUCCESS) {
        if (data != null) {
          var dataObj = JSON.parse(data.metadata.data);
          callback(dataObj);
        } else {
          callback({});
        }
      } else {
        callback({});
      }
    });
  }
  /**
   * Builds the type-specific section of the search-criteria dialog markup:
   * the site/template selects for pages, the asset-type select for assets,
   * and an empty string for any other type.
   */
  function buildTypeSpecificCriteriaMarkup(criteriaData) {
    if (criteriaData.type === "pages") {
      var pagesMarkup =
        '<div class="perc-search-criteria-dialog-fields-containers" id="perc-search-criteria-dialog-site-container">';
      pagesMarkup +=
        '<label id="perc-search-criteria-dialog-site-label" for="perc-search-criteria-dialog-site"> Site: </label>';
      pagesMarkup +=
        '<div class="perc-search-criteria-dialog-select-containers" ><select name="perc-search-criteria-dialog-site" id="perc-search-criteria-dialog-site">';
      pagesMarkup += "</select></div>";
      pagesMarkup += "</div>";
      pagesMarkup +=
        '<div class="perc-search-criteria-dialog-fields-containers" id="perc-search-criteria-dialog-template-container">';
      pagesMarkup +=
        '<label class="perc-disabled" id="perc-search-criteria-dialog-template-label" for="perc-search-criteria-dialog-template"> Template: </label>';
      pagesMarkup +=
        '<div class="perc-search-criteria-dialog-select-containers"><select name="perc-search-criteria-dialog-template" id="perc-search-criteria-dialog-template" disabled="disabled">';
      pagesMarkup += "</select></div>";
      pagesMarkup += "</div>";
      return pagesMarkup;
    }
    if (criteriaData.type === "assets") {
      var assetsMarkup =
        '<div class="perc-search-criteria-dialog-fields-containers" id="perc-search-criteria-dialog-assettype-container">';
      assetsMarkup +=
        '<label id="perc-search-criteria-dialog-assettype-label" for="perc-search-criteria-dialog-assettype"> Asset Type: </label>';
      assetsMarkup +=
        '<div class="perc-search-criteria-dialog-select-containers"><select name="perc-search-criteria-dialog-assettype" id="perc-search-criteria-dialog-assettype" >';
      assetsMarkup += '<option value="@all">All</option>';
      assetsMarkup += "</select></div>";
      assetsMarkup += "</div>";
      return assetsMarkup;
    }
    return "";
  }
  /**
   * Builds the type-independent section of the search-criteria dialog
   * markup: the workflow/state selects and the last-modified-by field.
   */
  function buildCommonCriteriaMarkup() {
    var commonMarkup =
      '<div class="perc-search-criteria-dialog-fields-containers" id="perc-search-criteria-dialog-workflow-container">';
    commonMarkup +=
      '<label id="perc-search-criteria-dialog-workflow-label" for="perc-search-criteria-dialog-workflow"> Workflow: </label>';
    commonMarkup +=
      '<div class="perc-search-criteria-dialog-select-containers"><select name="perc-search-criteria-dialog-workflow" id="perc-search-criteria-dialog-workflow" >';
    commonMarkup += '<option value="@all">All</option>';
    commonMarkup += "</select></div>";
    commonMarkup += "</div>";
    commonMarkup +=
      '<div class="perc-search-criteria-dialog-fields-containers" id="perc-search-criteria-dialog-state-container">';
    commonMarkup +=
      '<label class="perc-disabled" id="perc-search-criteria-dialog-state-label" for="perc-search-criteria-dialog-state"> State: </label>';
    commonMarkup +=
      '<div class="perc-search-criteria-dialog-select-containers"><select name="perc-search-criteria-dialog-state" id="perc-search-criteria-dialog-state" disabled="disabled" >';
    commonMarkup += '<option value="@all">All</option>';
    commonMarkup += "</select></div>";
    commonMarkup += "</div>";
    commonMarkup +=
      '<div class="perc-search-criteria-dialog-fields-containers" id="perc-search-criteria-dialog-created-by-container">';
    commonMarkup +=
      '<label id="perc-search-criteria-dialog-created-by-label" for="perc-search-criteria-dialog-created-by-field"> Last Modified By: </label><br/>';
    commonMarkup +=
      '<div class="perc-search-criteria-dialog-select-containers" ><input id="perc-search-criteria-dialog-created-by-field" name="perc-search-criteria-dialog-created-by-field"/></div>';
    commonMarkup += "</div>";
    commonMarkup += "</div>";
    commonMarkup += "</form>";
    commonMarkup += "</div>";
    return commonMarkup;
  }
  /**
   * Builds the full search-criteria dialog markup for the given criteria.
   */
  function buildSearchCriteriaDialogMarkup(criteriaData) {
    var dialogContent = '<div class="perc-search-criteria-dialog-content" >';
    dialogContent += '<form id="perc-search-criteria-dialog-form" action="#">';
    dialogContent += '<div id="perc-search-criteria-dialog-general-container">';
    dialogContent += buildTypeSpecificCriteriaMarkup(criteriaData);
    dialogContent += buildCommonCriteriaMarkup();
    return dialogContent;
  }
  /**
   * Reads the raw field values out of the rendered search-criteria dialog.
   */
  function readSearchCriteriaFields(dialog) {
    return {
      siteValue: dialog.find("#perc-search-criteria-dialog-site").val(),
      templateName: dialog
        .find("#perc-search-criteria-dialog-template option:selected")
        .html(),
      templateValue: dialog.find("#perc-search-criteria-dialog-template").val(),
      assetTypeName: dialog
        .find("#perc-search-criteria-dialog-assettype option:selected")
        .html(),
      assetTypeValue: dialog
        .find("#perc-search-criteria-dialog-assettype")
        .val(),
      workflowName: dialog
        .find("#perc-search-criteria-dialog-workflow option:selected")
        .html(),
      workflowValue: dialog.find("#perc-search-criteria-dialog-workflow").val(),
      stateName: dialog
        .find("#perc-search-criteria-dialog-state option:selected")
        .html(),
      stateValue: dialog.find("#perc-search-criteria-dialog-state").val(),
      modifiedValue: dialog
        .find("#perc-search-criteria-dialog-created-by-field")
        .val(),
    };
  }
  /**
   * Maps a stored criterion value to its display name.
   */
  function criterionDisplayName(value, label) {
    return value === "@all" ? "All" : label;
  }
  /**
   * Builds a criterion entry from a value that is stored as-is.
   */
  function rawSearchCriterion(value, label) {
    return { name: criterionDisplayName(value, label), value: value };
  }
  /**
   * Builds a criterion entry whose null value defaults to "@all".
   */
  function defaultedSearchCriterion(value, label) {
    var normalizedValue = value == null ? "@all" : value;
    return {
      name: criterionDisplayName(normalizedValue, label),
      value: normalizedValue,
    };
  }
  /**
   * Builds the modified-by criterion entry whose null/blank value
   * defaults to "@all".
   */
  function blankDefaultedSearchCriterion(value) {
    var normalizedValue =
      value == null || value.trim() === "" ? "@all" : value;
    return {
      name: normalizedValue === "@all" ? "All" : value,
      value: normalizedValue,
    };
  }
  /**
   * Collects the Search-button return payload from the rendered dialog
   * fields, preserving the per-field null/blank defaulting the dialog
   * has always applied.
   */
  function collectSearchCriteriaReturnData(fields, criteriaData) {
    var returnData = {};
    if (criteriaData.type === "pages") {
      returnData.site = rawSearchCriterion(fields.siteValue, fields.siteValue);
      returnData.template = defaultedSearchCriterion(
        fields.templateValue,
        fields.templateName,
      );
    } else if (criteriaData.type === "assets") {
      returnData.assetType = rawSearchCriterion(
        fields.assetTypeValue,
        fields.assetTypeName,
      );
    }
    returnData.workflow = rawSearchCriterion(
      fields.workflowValue,
      fields.workflowName,
    );
    returnData.state = defaultedSearchCriterion(
      fields.stateValue,
      fields.stateName,
    );
    returnData.modifiedby = blankDefaultedSearchCriterion(fields.modifiedValue);
    return returnData;
  }
  /**
   * Loads the site select options for page criteria.
   */
  function loadSiteOptions(dialog, criteriaData) {
    percJQuery.PercReusableSearchService.getSites(function (status, result) {
      for (var i = 0; i < result.length; i++) {
        var value = result[i].value;
        var optionElement = $("<OPTION>").html(value).attr("value", value);
        if (criteriaData.site != null) {
          if (value === criteriaData.site.value) {
            optionElement.attr("selected", "selected");
          }
        }
        dialog
          .find("#perc-search-criteria-dialog-site")
          .append(optionElement);
      }
      if (criteriaData.site.value !== "@all") {
        dialog.find("#perc-search-criteria-dialog-site").trigger("change");
      }
    });
  }
  /**
   * Loads the workflow select options.
   */
  function loadWorkflowOptions(dialog, criteriaData) {
    percJQuery.PercReusableSearchService.getWorkflows(function (
      status,
      result,
    ) {
      for (var i = 0; i < result.length; i++) {
        var value = result[i].displayValue;
        var name = result[i].value;
        var optionElement = $("<OPTION>").html(name).attr("value", value);
        if (criteriaData.workflow != null) {
          if (value === criteriaData.workflow["value"]) {
            optionElement.attr("selected", "selected");
          }
        }
        dialog
          .find("#perc-search-criteria-dialog-workflow")
          .append(optionElement);
      }
      if (criteriaData.workflow["value"] !== "@all") {
        dialog.find("#perc-search-criteria-dialog-workflow").trigger("change");
      }
    });
  }
  /**
   * Loads the asset-type select options for asset criteria.
   */
  function loadAssetTypeOptions(dialog, criteriaData) {
    percJQuery.PercAssetService.getAssetTypes(
      "no",
      function (status, result) {
        for (var i = 0; i < result.length; i++) {
          var value = result[i].contentTypeId;
          var name = result[i].widgetLabel;
          var optionElement = $("<OPTION>").html(name).attr("value", value);
          if (criteriaData.assetType != null) {
            if (value === criteriaData.assetType["value"]) {
              optionElement.attr("selected", "selected");
            }
          }
          dialog
            .find("#perc-search-criteria-dialog-assettype")
            .append(optionElement);
        }
      },
    );
  }
  /**
   * Loads the criteria-dependent select options into the dialog.
   */
  function loadSearchCriteriaFields(dialog, criteriaData) {
    if (criteriaData.type === "pages") {
      loadSiteOptions(dialog, criteriaData);
    }
    loadWorkflowOptions(dialog, criteriaData);
    if (criteriaData.type === "assets") {
      loadAssetTypeOptions(dialog, criteriaData);
    }
  }
  /**
   * Disables the template select and marks its label disabled.
   */
  function disableTemplateCombo(dialog) {
    dialog
      .find("#perc-search-criteria-dialog-template")
      .prop("disabled", true)
      .html("");
    dialog
      .find("#perc-search-criteria-dialog-template-label")
      .addClass("perc-disabled");
  }
  /**
   * Enables the template select and marks its label enabled.
   */
  function enableTemplateCombo(dialog) {
    dialog.find("#perc-search-criteria-dialog-template").prop("disabled", false);
    dialog
      .find("#perc-search-criteria-dialog-template-label")
      .removeClass("perc-disabled");
  }
  /**
   * Loads the template select options for the given site.
   */
  function loadSiteTemplatesCombo(dialog, criteriaData, siteName, callback) {
    percJQuery.PercSiteService.getTemplates(
      siteName,
      function (status, result) {
        if (status === percJQuery.PercServiceUtils.STATUS_SUCCESS) {
          var data = percJQuery.perc_utils.convertCXFArray(
            result.TemplateSummary,
          );
          dialog
            .find("#perc-search-criteria-dialog-template")
            .html("")
            .append($("<OPTION>").html("All").attr("value", "@all"));
          for (var i = 0; i < data.length; i++) {
            var id = data[i].id;

            //CMS-7925 : The filter was not working expected while filtering on templates. Content id extracted and passed to filter results on templates.
            var contentId = percJQuery.perc_utils.getContentId(id);

            var name = data[i].name;
            var optionElement = $("<OPTION>").html(name).attr("value", id);
            if (criteriaData.template != null) {
              if (id === criteriaData.template["value"]) {
                optionElement.attr("selected", "selected");
              }
            }
            dialog
              .find("#perc-search-criteria-dialog-template")
              .append(optionElement)
              .attr("forsite", siteName);
          }
          callback();
        }
      },
    );
  }
  /**
   * Handles site-select changes: "@all" clears and disables the template
   * select, any other site reloads the templates.
   */
  function handleSiteSelectionChange(dialog, criteriaData, siteValue) {
    if (siteValue === "@all") {
      disableTemplateCombo(dialog);
      return;
    }
    loadSiteTemplatesCombo(dialog, criteriaData, siteValue, function () {
      enableTemplateCombo(dialog);
    });
  }
  /**
   * Disables the state select and marks its label disabled.
   */
  function disableStateCombo(dialog) {
    dialog
      .find("#perc-search-criteria-dialog-state")
      .prop("disabled", true)
      .html("");
    dialog
      .find("#perc-search-criteria-dialog-state-label")
      .addClass("perc-disabled");
  }
  /**
   * Enables the state select and marks its label enabled.
   */
  function enableStateCombo(dialog) {
    dialog.find("#perc-search-criteria-dialog-state").prop("disabled", false);
    dialog
      .find("#perc-search-criteria-dialog-state-label")
      .removeClass("perc-disabled");
  }
  /**
   * Loads the state select options for the given workflow.
   */
  function loadWorkflowStatesCombo(dialog, criteriaData, workflow, callback) {
    percJQuery.PercReusableSearchService.getStates(
      workflow,
      function (status, result) {
        if (status === percJQuery.PercServiceUtils.STATUS_SUCCESS) {
          dialog
            .find("#perc-search-criteria-dialog-state")
            .html("")
            .append($("<OPTION>").html("All").attr("value", "@all"));

          for (var i = 0; i < result.length; i++) {
            var value = result[i].displayValue;
            var name = result[i].value;
            var optionElement = $("<OPTION>").html(name).attr("value", value);
            if (criteriaData.state != null) {
              if (value === criteriaData.state["value"]) {
                optionElement.attr("selected", "selected");
              }
            }

            dialog
              .find("#perc-search-criteria-dialog-state")
              .append(optionElement)
              .attr("forworkflow", workflow);
          }
          callback();
        }
      },
    );
  }
  /**
   * Handles workflow-select changes: "@all" clears and disables the state
   * select, any other workflow reloads the states.
   */
  function handleWorkflowSelectionChange(
    dialog,
    criteriaData,
    workflowValue,
    workflowLabel,
  ) {
    if (workflowValue === "@all") {
      disableStateCombo(dialog);
      return;
    }
    loadWorkflowStatesCombo(dialog, criteriaData, workflowLabel, function () {
      enableStateCombo(dialog);
    });
  }
  /**
   * Wires the last-modified-by autocomplete from the user list.
   */
  function initModifiedByTypeAhead(dialog) {
    // BEGINNING TYPE_AHEAD
    var inputField = dialog.find(
      "#perc-search-criteria-dialog-created-by-field",
    );

    percJQuery.PercReusableSearchService.getUsers(
      "*",
      function (status, results) {
        if (results !== undefined && results != null) {
          resultItems = results;

          // add autocomplete to input field
          inputField
            .autocomplete({
              // jQuery UI autocomplete (issue #7178): the retired
              // bassistance autocomplete plugin is gone; jQuery UI 1.14
              // (npm, loaded by common_js.jsp) owns $.fn.autocomplete now.
              // source replaces the data array; minLength: 0 preserves
              // minChars: 0 (suggest on empty input via the focus
              // handler); max showed the whole list, which jQuery UI does
              // by default. The menu width/scroll cap from
              // width/scrollHeight move into open (the IE width branch is
              // dead on modern browsers).
              source: resultItems,
              minLength: 0,
              open: function () {
                inputField
                  .autocomplete("widget")
                  .css({
                    width: "334px",
                    "max-height": "101px",
                    "overflow-y": "auto",
                  });
              },
            })
            .on("focus", function () {
              inputField.autocomplete("search", "");
            })
            .on("autocompleteselect", function () {
              // callback used after options load
            });
        }
      },
    );
    // END TYPE_AHEAD
  }
  /**
   * Prefills the last-modified-by field from previously saved criteria.
   */
  function prefillModifiedByField(dialog, criteriaData) {
    if (criteriaData.modifiedby == null) {
      return;
    }
    dialog
      .find("#perc-search-criteria-dialog-created-by-field")
      .val(
        criteriaData.modifiedby["value"] === "@all"
          ? ""
          : criteriaData.modifiedby["value"],
      );
  }
  /**
   * Opens the schedule dialog.
   * @param itemId(String), assumed to be a valid guid of the item (Page or Asset)
   */
  function openSearchCriteriaDialog(
    criteriaData,
    saveCallback,
    cancelCallback,
  ) {
    // Set the basic markup for the dialog and assign it to the module's dialog variable
    var dialogContent = buildSearchCriteriaDialogMarkup(criteriaData);

    buttons = {
      Search: {
        id: "perc-search-criteria-dialog-save",
        click: function () {
          saveCallback(
            collectSearchCriteriaReturnData(
              readSearchCriteriaFields(dialog),
              criteriaData,
            ),
          );
          dialog.remove();
        },
      },
      Cancel: {
        id: "perc-search-criteria-dialog-cancel",
        click: function () {
          cancelCallback();
          dialog.remove();
        },
      },
    };
    dialog = percJQuery(dialogContent).perc_dialog({
      id: "perc-search-criteria-dialog",
      title: "Filters",
      width: 686,
      height: 420,
      resizable: false,
      modal: true,
      percButtons: buttons,
    });

    dialog.find("#perc-search-criteria-dialog-site").on("change", function () {
      handleSiteSelectionChange(dialog, criteriaData, $(this).val());
    });

    dialog
      .find("#perc-search-criteria-dialog-workflow")
      .on("change", function () {
        handleWorkflowSelectionChange(
          dialog,
          criteriaData,
          $(this).val(),
          $(this).find("option:selected").html(),
        );
      });

    initModifiedByTypeAhead(dialog);
    prefillModifiedByField(dialog, criteriaData);
    loadSearchCriteriaFields(dialog, criteriaData);
  } // End open dialog
})(jQuery);
