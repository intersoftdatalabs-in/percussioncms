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
 * Tag list control. Adds a pre submit handler.
 */
(function ($) {
  $(document).ready(function () {
    window.parent.jQuery.PercContentPreSubmitHandlers.addHandler(
      updateTagListField,
    );

    //Allow selection of multiple values
    function split(val) {
      return val.split(/,\s*/);
    }
    function extractLast(term) {
      return split(term).pop();
    }

    $.perc_filterField($("#page_tags-display"), $.perc_textFilters.TAGS);

    if (
      window.percTagListSource !== undefined &&
      percTagListSource.length > 0
    ) {
      // jQuery UI autocomplete (issue #7178): the retired bassistance
      // autocomplete plugin is gone; jQuery UI 1.14 is declared on this
      // control (jquery-ui.min.js FileDescriptor) and owns $.fn.autocomplete
      // now. minChars: 1 becomes minLength: 1; selectFirst: false is the
      // jQuery UI default (autoFocus: false); multiple: true becomes the
      // split/extractLast multi-value pattern (helpers already present in
      // this file); scrollHeight: 135 moves into open as a menu max-height.
      $("#page_tags-display").autocomplete({
        minLength: 1,
        autoFocus: false,
        source: function (request, response) {
          response(
            $.ui.autocomplete.filter(
              percTagListSource,
              extractLast(request.term),
            ),
          );
        },
        focus: function () {
          // prevent value insertion on focus (multi-value pattern)
          return false;
        },
        select: function (event, ui) {
          var terms = split(this.value);
          terms.pop();
          terms.push(ui.item.value);
          terms.push("");
          this.value = terms.join(", ");
          return false;
        },
        open: function () {
          $("#page_tags-display")
            .autocomplete("widget")
            .css({ "max-height": "135px", "overflow-y": "auto" });
        },
      });
    }
  });

  /**
   * Filter a text input field with a passed in filter.
   * @param tgt {String or jQuery} the input field to be filtered. Cannot be <code>null</code>.
   * @param filter {Function} the filter function, the fuction receives the currently
   * typed in char and should return the char back if ok or return empty string if
   * it should be filtered out.
   */
  (function ($) {
    $.perc_filterField = function (tgt, filter) {
      $(tgt).on("keypress.filterField", { filter: filter }, function (evt) {
        var filter = function (txt) {
          return txt;
        };

        var rawCode = evt.charCode ? evt.charCode : evt.which;
        if (rawCode == 0 || rawCode == 8) return;
        var theChar = String.fromCharCode(rawCode);
        if (typeof evt.data.filter == "function") filter = evt.data.filter;
        var filtered = filter(theChar);
        if (filtered.length == 0) {
          evt.preventDefault();
        } else if (theChar != filtered) {
          // the filter changed the char to something else, update the field
          // and retain correct cursor caret positioning.
          // Native caret (issue #7178): the retired jquery.caret.js plugin
          // is gone; selectionStart/selectionEnd + setSelectionRange cover
          // this text input.

          var field = evt.target;
          var start =
            typeof field.selectionStart === "number"
              ? field.selectionStart
              : field.value.length;
          var end =
            typeof field.selectionEnd === "number"
              ? field.selectionEnd
              : field.value.length;
          var t1 = field.value.slice(0, start);
          var t2 = field.value.slice(end);
          var newVal = t1 + filtered + t2;
          field.value = newVal;
          var cursorPos = t1.length + 1;
          if (typeof field.setSelectionRange === "function") {
            try {
              field.setSelectionRange(cursorPos, cursorPos);
            } catch (ignore) {
              // non-text input types reject setSelectionRange; leave caret alone.
            }
          }
          evt.preventDefault();
        }
      });

      $(tgt).on("blur.filterField", { filter: filter }, function (evt) {
        // Run through the filter on blur
        tgt.val(evt.data.filter(tgt.val()));
      });
    };
  })(jQuery);

  /**
   * Define text filters for tags
   */
  (function ($) {
    $.perc_textFilters = {
      // This filter allows only characters which are valid in name and/or id attributes.
      // Note: This filter does NOT force starting alpha, which IS a requirement of the W3C spec.
      // Allowed Characters: alpha-numeric, " ", '-', '_' and ','
      TAGS: function (txt) {
        return txt.replace(/[^a-zA-Z0-9\-\_\, ]/g, "");
      },
    };
  })(jQuery);

  function updateTagListField() {
    //This needs to be changed to get dynamically from the field name.
    var $tagSelect = $("#perc-content-edit-page_tags");
    var newTags = $("#page_tags-display").val();
    var newTagsArray = newTags ? newTags.split(",") : [];
    var processedArray = [];

    $.each(newTagsArray, function () {
      var thisTag = this.trim().toLowerCase();
      if (thisTag.length > 0) {
        processedArray.push(thisTag);
      }
    });
    processedArray = $.uniqueSort(processedArray);
    $.each(processedArray, function () {
      $tagSelect.append("<option selected='selected'>" + this + "</option>");
    });
    return true;
  }
})(jQuery);
