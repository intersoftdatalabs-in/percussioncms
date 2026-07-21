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

(function ($, P) {
  P.cssGalleryView = function (controller) {
    // Use singleton controller to keep track of dirty status
    var dirtyController = $.PercDirtyController;
    var root = $("#perc-css-gallery");

    render();

    function _dashify(nameString) {
      if (typeof nameString === "string") return nameString.replace(/ /g, "-");
      else return nameString;
    }

    function render() {
      root.empty();

      var galleryButtonBar =
        '<div class="perc-buttons" id="perc-css-gallery-button-bar">' +
        "  <form>" +
        "  </form>" +
        "</div>";

      root.append('<div id="perc-css-gallery-status"></div>');
      root.append(galleryButtonBar);

      // Get back each theme entry. data contains a DOM object.

      controller.getThemeList(function (status, data) {
        if (status === true) {
          //////////////////////////////////
          // Iterate over each theme entry
          //////////////////////////////////
          var themes = data.ThemeSummary;
          root.append(
            "<table id='perc-themes-table'><tr id='perc-themes-table-row'></tr><table>"
          );
          if (themes.length > 0) {
            $(themes).each(function () {
              // Events to trigger for each theme entry.
              var $nameAsEntered = this.name;
              var $thumbUrl = this.thumbUrl;
              var $name = _dashify($nameAsEntered);
              // renderGalleryEntry now binds the click handler directly
              // on the freshly-built element. The previous
              // `$("#theme-" + $name).on("click", ...)` binding threw
              // an unrecoverable jQuery "Syntax error, unrecognized
              // expression" whenever `name` contained a CSS-selector
              // metacharacter, which a malicious theme name can do.
              $("#perc-themes-table-row").append(
                renderGalleryEntry($name, $thumbUrl, $nameAsEntered)
              );
            });

            // And finally select the given bits
            controller.getTemplateTheme(function (themeName) {
              var theme = _dashify(themeName);
              selectTheme(theme);
            });
          }
        } else {
          root.append(
            "<div>" +
              I18N.message("perc.ui.css.galery.view@Gallery Cannot Load") +
              "</div>"
          );
        }
      });
    }

    function saveCSS(callback) {
      var currentView = $.PercNavigationManager.getView();
      if (currentView === $.PercNavigationManager.VIEW_EDITOR) {
        controller.save(callback);
      } else if (currentView === $.PercNavigationManager.VIEW_EDIT_TEMPLATE) {
        callbackFunc = callback || function () {};
        controller.setOverrideCSS();
        controller.save(function (status, data) {
          if (status === true) {
            dirtyController.setDirty(false, "template");
            callbackFunc();
          }
        });
      }
    }

    // Send back one HTML fragment for a given gallery entry. Called from render() above.

    function renderGalleryEntry(name, thumbUrl, nameAsEntered) {
      // Build the entry via the jQuery DOM API so user-controlled
      // fields (name, thumbUrl, nameAsEntered) are never concatenated
      // into an HTML string and parsed back into the DOM. jQuery's
      // `.attr()` and `.text()` escape their arguments for the relevant
      // context.
      var $cell = $("<td/>");
      var $container = $("<div/>", {
        id: "theme-" + name + "-container",
        class: "perc-css-gallery-item",
      }).css("display", "table-cell");
      var $link = $("<a/>", {
        id: "theme-" + name,
        tabindex: "0",
        title: name,
        role: "button",
        href: "#",
      });
      $link.append(
        $("<img/>", { src: thumbUrl, alt: "perc.ui.template.layout@Theme" }),
        $("<br/>"),
        $("<span/>", {
          class: "perc-css-gallery-item-name",
        })
          .css("text-align", "center")
          .text(nameAsEntered)
      );
      $container.append($link);
      $cell.append($container);
      // Bind the click handler on the freshly-built element rather than
      // via `$("#theme-" + name)`, which throws an unrecoverable
      // jQuery "Syntax error, unrecognized expression" whenever `name`
      // contains a CSS-selector metacharacter (e.g. `<`, `>`, `"`).
      $link.on("click", function () {
        dirtyController.setDirty(true, "style", saveCSS);
        controller.setTemplateTheme(nameAsEntered, function (success) {
          $("#perc-css-gallery-edit-save").removeClass("ui-state-disabled");
          selectTheme(name);
        });
      });
      return $cell;
    }

    // called on click of a theme to change the UI appropriately to reflect the newly selected theme.

    function selectTheme(sThemeName) {
      /**
       * Used to change the visual element of selecting a theme. Either in response to a getTemplateThemeName
       * or after a setTheme()
       */

      var selector = ".perc-css-gallery-item";
      var selector_selected = "#theme-" + sThemeName + "-container";
      var selector_closeLinks = ".perc-theme-clear";
      var selector_closeLink_selected = "#theme-" + sThemeName + "-clear";

      $(selector).removeClass("perc-css-gallery-themeHighlighted");
      $(selector_closeLinks).css("display", "none");
      $(selector_selected).addClass("perc-css-gallery-themeHighlighted");
      $(selector_closeLink_selected).css("display", "inline");
    }
  };
})(jQuery, jQuery.Percussion);
