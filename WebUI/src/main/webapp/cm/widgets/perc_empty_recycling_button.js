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
 * Finder Actions menu entry: Empty Recycling bin (bulk permanent purge).
 *
 * Depends on #2205 DELETE /pathmanagement/recycle/empty (Admin-only).
 * Peer of perc_restore_button.js / perc_delete_page_button.js purge flow.
 */
(function ($) {
  /**
   * Pure enablement rule (exported for unit tests via $.perc_empty_recycling_enabled).
   * Enabled only for Admin users when the finder path is under Recycling.
   *
   * @param {string[]} path finder path segments (e.g. ['', 'Recycling'] or ['', 'Recycling', 'Sites'])
   * @param {boolean} isAdmin whether the current user is Admin
   * @returns {boolean}
   */
  function isEmptyRecyclingEnabled(path, isAdmin) {
    if (!isAdmin) {
      return false;
    }
    if (!path || path.length < 2) {
      return false;
    }
    return path[1] === $.perc_paths.RECYCLING_ROOT_NO_SLASH;
  }

  /**
   * Summarize a successful empty-recycle result for optional user feedback.
   * Pure helper for tests.
   *
   * @param {object|null} data PSEmptyRecycleResult-shaped
   * @returns {{alreadyEmpty: boolean, partial: boolean, messageKey: string|null, messageArgs: string[]}}
   */
  function summarizeEmptyResult(data) {
    if (!data || typeof data !== "object") {
      return {
        alreadyEmpty: false,
        partial: false,
        messageKey: null,
        messageArgs: [],
      };
    }
    var undeleted =
      typeof data.undeletedCount === "number" ? data.undeletedCount : 0;
    if (data.alreadyEmpty === true) {
      return {
        alreadyEmpty: true,
        partial: false,
        messageKey: "perc.ui.empty.recycling@Already Empty",
        messageArgs: [],
      };
    }
    if (undeleted > 0) {
      return {
        alreadyEmpty: false,
        partial: true,
        messageKey: "perc.ui.empty.recycling@Partial",
        messageArgs: [String(undeleted)],
      };
    }
    return {
      alreadyEmpty: false,
      partial: false,
      messageKey: null,
      messageArgs: [],
    };
  }

  // Expose pure helpers for Vitest without requiring full finder bootstrap.
  $.perc_empty_recycling_enabled = isEmptyRecyclingEnabled;
  $.perc_empty_recycling_summarize = summarizeEmptyResult;

  $.perc_build_empty_recycling_button = function (finderRef, content) {
    var label = I18N.message("perc.ui.empty.recycling@Label");
    var tooltip = I18N.message("perc.ui.empty.recycling@Click");
    // Build via DOM APIs so i18n strings never flow through HTML parse (CWE-79).
    var btn = $("<a></a>")
      .attr({
        id: "perc-finder-empty-recycling",
        "data-testid": "perc-finder-empty-recycling",
        href: "#",
        title: tooltip,
      })
      .text(label)
      .on("click", function (event) {
        emptyRecyclingClick(event);
      });

    function emptyRecyclingClick(evt) {
      if (btn.hasClass("ui-disabled")) {
        return false;
      }
      if (!$.PercNavigationManager.isAdmin()) {
        $.perc_utils.alert_dialog({
          title: I18N.message("perc.ui.page.general@Warning"),
          content: I18N.message("perc.ui.empty.recycling@Not Authorized"),
        });
        return false;
      }

      var options = {
        id: "perc-finder-empty-recycling-confirm",
        title: I18N.message("perc.ui.empty.recycling@Title"),
        question:
          "<span id='perc-empty-recycling-warn-msg'>" +
          I18N.message("perc.ui.empty.recycling@Confirm") +
          "</span>",
        success: function () {
          performEmptyRecycling();
        },
        yes: I18N.message("perc.ui.empty.recycling@Empty"),
      };
      $.perc_utils.confirm_dialog(options);
      return false;
    }

    function performEmptyRecycling() {
      $.PercBlockUI($.PercBlockUIMode.CURSORONLY);
      $.PercRecycleService.emptyRecycling(function (status, data) {
        $.unblockUI();
        if (status === $.PercServiceUtils.STATUS_ERROR) {
          $.perc_utils.alert_dialog({
            title: I18N.message("perc.ui.publish.title@Error"),
            content: data || I18N.message("perc.ui.empty.recycling@Error"),
          });
          return;
        }

        var summary = summarizeEmptyResult(data);
        if (summary.messageKey) {
          var msg = I18N.message(summary.messageKey, summary.messageArgs);
          $.perc_utils.alert_dialog({
            title: I18N.message("perc.ui.empty.recycling@Title"),
            content: msg,
          });
        }

        if (finderRef && typeof finderRef.refresh === "function") {
          finderRef.refresh();
        }
      });
    }

    /**
     * Enable only under Recycling for Admin users.
     * @param {string[]} path finder path segments
     */
    function update_empty_recycling_btn(path) {
      var enabled = isEmptyRecyclingEnabled(
        path,
        $.PercNavigationManager.isAdmin(),
      );
      enableButton(enabled);
    }

    function enableButton(flag) {
      if (flag) {
        btn
          .removeClass("ui-disabled")
          .addClass("ui-enabled")
          .off("click")
          .on("click", function (evt) {
            emptyRecyclingClick(evt);
          });
      } else {
        btn.addClass("ui-disabled").removeClass("ui-enabled").off("click");
      }
      btn.trigger("actions-change-enabled-state");
    }

    finderRef.addPathChangedListener(update_empty_recycling_btn);
    // Start disabled until path listener fires.
    enableButton(false);
    return btn;
  };
})(jQuery);
