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
  /*
   * On document ready calls the render function, editor or read only.
   * FIXME
   */
  $(document).ready(function () {
    if ($(".perc-google-calendar").length > 0) {
      $.googleCalendarSetup().renderEditor();
    } else if ($(".perc-google-calendar-readonly").length > 0) {
      $.googleCalendarSetup().renderReadOnly();
    }
  });

  /**
   * Builds the MiniColors options for one calendar color input from that
   * input's own data-* attributes (issue #6723). The pre-#6723 init read
   * the attributes off $(document) inside the ready handler, so every
   * picker silently fell back to defaults.
   * @param {jQuery} $input the color input to read options from.
   * @returns {Object} the MiniColors option shape the editor consumes.
   */
  function calendarColorPickerOptions($input) {
    return {
      control: $input.attr("data-control") || "hue",
      defaultValue: $input.attr("data-defaultValue") || "",
      format: $input.attr("data-format") || "hex",
      keywords: $input.attr("data-keywords") || "",
      inline: $input.attr("data-inline") === "true",
      letterCase: $input.attr("data-letterCase") || "lowercase",
      opacity: $input.attr("data-opacity"),
      position: $input.attr("data-position") || "top left",
      swatches: $input.attr("data-swatches")
        ? $input.attr("data-swatches").split("|")
        : [],
      change: function (hex, opacity) {
        // Never let picker logging break the editor: console may be
        // absent on published pages, so guard instead of try/catch
        // (issue #6723; mkd-code-review tests.empty-catch).
        var log = hex ? hex : "transparent";
        if (opacity) log += ", " + opacity;
        if (typeof console !== "undefined" && typeof console.log === "function") {
          console.log(log);
        }
      },
      theme: "default",
    };
  }

  /**
   * Initializes MiniColors pickers for every calendar color input in scope.
   * Scoped by class (never by id): the template row carries no ids, so each
   * cloned row owns an independent picker (issue #6723).
   * @param {jQuery} $container element containing editor rows to initialize.
   */
  function initCalendarColorPickers($container) {
    // The container may be the editor table (init all data rows) or a
    // single fresh row (init just that row): .find() only sees
    // descendants, so include the container itself when it is an input.
    // The hidden template row is always skipped — it must stay a bare
    // template with no picker state for clones to inherit.
    var $inputs = $container
      .find(
        ".perc-google-calendar-background-color," +
          " .perc-google-calendar-text-color",
      )
      .add(
        $container.filter(
          ".perc-google-calendar-background-color," +
            " .perc-google-calendar-text-color",
        ),
      );
    $inputs
      .filter(function () {
        return $(this).closest("tr.hide").length === 0;
      })
      .each(function () {
        var $input = $(this);
        if ($input.data("minicolors-initialized")) {
          return;
        }
        $input.minicolors(calendarColorPickerOptions($input));
      });
  }

  /**
   * Clones the hidden template row into a fresh editor row (issue #6723).
   * Cloned without data/events so no MiniColors state leaks from the
   * template; ids are stripped defensively so added rows can never
   * duplicate an element id even if the markup regresses.
   * @param {jQuery} $table the editor table carrying the tr.hide template.
   * @returns {jQuery} the unattached fresh row.
   */
  function cloneCalendarRow($table) {
    var $clone = $table
      .find("tr.hide")
      .first()
      .clone(false)
      .removeClass("hide table-line");
    $clone.removeAttr("id").find("[id]").removeAttr("id");
    return $clone;
  }

  $.googleCalendarSetup = function () {
    var googleCalendarSetupApi = {
      renderEditor: renderEditor,
      renderReadOnly: renderReadOnly,
    };

    var self = this;

    /**
     * Respsonsible for rendering a read-only view of the control.
     */
    function renderReadOnly() {}

    /***
     * Responsible for rendering the Edit view of the control.
     */
    function renderEditor() {
      //Hide help to start
      $(".perc-help").hide();

      //Hide Google Setup to start;
      $(".perc-google-calendar").hide();

      //Load data from the data field - this is done in two steps
      //an input field gets the data and the name of the input field is passed to the id of a div that contains
      //our editor class - we get the id from the div and pull the data from the input field.
      var dataFieldName = $(".perc-google-calendar").attr("id");
      var dataStr = $("input[name='" + dataFieldName + "']").val();
      var data = null;
      dataStr = dataStr.trim();
      if (dataStr.length > 0) {
        data = JSON.parse(dataStr);
      }

      if (data != null) {
        var $table = $("#perc-google-calendar-setup-editor");
        for (var i = 0; i < data.config.length; i++) {
          var $clone = cloneCalendarRow($table);

          $clone
            .find(".perc-google-calendar-name")
            .val(data.config[i].calendarName);
          $clone
            .find(".perc-google-calendar-id")
            .val(data.config[i].calendarId);
          $clone
            .find(".perc-google-calendar-api-key")
            .val(data.config[i].apiKey); // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #6777, precedent #6728/#52) - operator-supplied key read from editor DOM at runtime, no secret in source.
          $clone
            .find(".perc-google-calendar-background-color")
            .val(data.config[i].backgroundColor);
          $clone
            .find(".perc-google-calendar-text-color")
            .val(data.config[i].textColor);

          $table.find("table").append($clone);
        }
        // Values are set before init so each restored row's picker opens
        // on that row's saved color (issue #6723).
        initCalendarColorPickers($table);
      }
      _attachEvents();
    }

    /**
     * Helper method that is responsible for attaching the click events and auto fill functionality.
     * Also attaches the presubmit handler to _preSubmitHandler.
     */
    function _attachEvents() {
      var $table = $("#perc-google-calendar-setup-editor");

      //Add the pre-submit handler to prepare data for saving.
      window.parent.jQuery.PercContentPreSubmitHandlers.addHandler(
        _preSubmitHandler,
      );

      //Widget behavioral events
      // Row buttons use delegation on the table: rows added later via the
      // + button own no copied handlers (clone(false), issue #6723), so
      // direct binding would leave added rows without remove/up/down.
      $table.on("click", ".perc-table-remove", function () {
        $(this).parents("tr").detach();
      });

      $(".perc-table-add").on("click", function () {
        var $clone = cloneCalendarRow($table);
        $table.find("table").append($clone);

        // Each added row gets an independent working picker (issue #6723).
        initCalendarColorPickers($clone);
        // Keyboard polish: land in the new row's first field. The native
        // method (not jQuery .trigger("focus")) transfers focus reliably
        // when called from inside a click dispatch.
        var $nameInput = $clone.find(".perc-google-calendar-name");
        var nameInput = $nameInput.get(0);
        if (nameInput && typeof nameInput.focus === "function") {
          nameInput.focus();
        }
      });

      $(".perc-toggle-help").on("click", function () {
        if ($(".perc-help").is(":visible")) {
          $(".perc-help").hide();
        } else {
          $(".perc-help").show();
        }
      });

      $(".perc-toggle-google-setup").on("click", function () {
        if ($(".perc-google-calendar").is(":visible")) {
          $(".perc-google-calendar").hide();
        } else {
          $(".perc-google-calendar").show();
        }
      });

      $table.on("click", ".perc-table-up", function () {
        var $row = $(this).parents("tr");
        if ($row.index() === 1) {
          return; // Don't go above the header
        }
        $row.prev().before($row.get(0));
      });

      $table.on("click", ".perc-table-down", function () {
        var $row = $(this).parents("tr");
        $row.next().after($row.get(0));
      });
    }

    /***
     * Validates the Widget data.
     * @returns true if everything is valid, false if not
     */
    function validateForm() {
      return true;
    }

    function convertToSlug(Text) {
      return Text.toLowerCase()
        .replace(/[^\w ]+/g, "")
        .replace(/ +/g, "-");
    }

    /***
     * Responsible for collecting Widget data into a Json object that can be saved to a field on the backing content type.
     */
    function getData() {
      var config = [];

      $(".perc-google-calendar-row").each(function () {
        if (!$(this).hasClass("hide")) {
          var alt = new Object();
          alt["calendarName"] = $(this)
            .find(".perc-google-calendar-name")
            .val();
          alt["calendarId"] = $(this).find(".perc-google-calendar-id").val();
          alt["apiKey"] = $(this).find(".perc-google-calendar-api-key").val(); // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #6777, precedent #6728/#52) - operator-supplied key read from editor DOM at runtime, no secret in source.
          alt["backgroundColor"] = $(this)
            .find(".perc-google-calendar-background-color")
            .val();
          alt["textColor"] = $(this)
            .find(".perc-google-calendar-text-color")
            .val();
          alt["className"] = convertToSlug(
            $(this).find(".perc-google-calendar-name").val(),
          );

          config.push(alt);
        }
      });

      return config;
    }
    /**
     * From the editor, reads the values and creates a JSON Object and assigns the JSON object
     * as the value of the hidden input of the Content Types field.
     */
    function _preSubmitHandler() {
      //Validate the Widget.
      if (!validateForm()) {
        return false;
      }

      $("#perc-content-edit-sys_title").val(
        $("#perc-content-edit-calendarName").val(),
      );

      //Get the Widget data and convert it to a JSON string that can be saved.
      var data = new Object();
      data.config = getData();

      var fieldName = $(".perc-google-calendar").attr("id");
      $("input[name='" + fieldName + "']").val(JSON.stringify(data));
      return true;
    }

    return googleCalendarSetupApi;
  };
})(jQuery);
