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
 * React replacement entry point for the retired classic
 * `perc_page_edit_dialog` (#5733, parent #5622 slice 6 batch 1).
 *
 * Page metadata (filename, page summary, SEO, tags/categories, calendar,
 * additional head code) now opens in the React Content Editor host
 * (`WebUI/src/main/ts/editor/EditorHost.tsx` via
 * `spa.jsp?entry=editor&contentId=<pageId>&mode=edit|view`), which loads
 * and persists the same page fields through the same backing endpoints
 * (`GET`/`PUT /services/itemmanagement/item/fields/{id}`) as every other
 * content item. Classic callers (`PercPageView`, `perc_content_viewer`)
 * call `$.perc_page_metadata_editor.open(pageid)` instead of the deleted
 * `$.perc_page_edit_dialog(mcol, viewer, pageid)`.
 *
 * URL/window conventions mirror the TypeScript side so both openers reuse
 * the same named window:
 *   - `editorHostUrl.ts` (`buildEditorHostUrl`, `editorWindowName`,
 *     `EDITOR_WINDOW_FEATURES`)
 *   - `api/paths.ts` (`withCmsContextPrefix`)
 */
(function ($) {
  // Must stay identical to EDITOR_WINDOW_FEATURES in editorHostUrl.ts.
  var EDITOR_WINDOW_FEATURES =
    "popup=yes,width=960,height=800,scrollbars=yes,resizable=yes";

  /**
   * Mirrors `withCmsContextPrefix`: the CMS may be served under /Rhythmyx
   * or at the server root.
   */
  function cmsContextPrefix() {
    if (
      typeof window !== "undefined" &&
      window.location &&
      window.location.pathname
    ) {
      var path = String(window.location.pathname);
      if (path === "/Rhythmyx" || path.indexOf("/Rhythmyx/") === 0) {
        return "/Rhythmyx";
      }
    }
    return "";
  }

  /**
   * The classic dialog showed Save/Cancel in navigation EDIT mode and a
   * read-only view otherwise; map that to the host `mode` param. When the
   * navigation manager is unavailable (or has no mode API), default to
   * edit — the host still enforces server-side permissions.
   */
  function editorMode() {
    var nav = $.PercNavigationManager;
    if (
      nav &&
      typeof nav.getMode === "function" &&
      typeof nav.MODE_EDIT !== "undefined"
    ) {
      return nav.getMode() === nav.MODE_EDIT ? "edit" : "view";
    }
    return "edit";
  }

  function editorUrl(pageid, mode) {
    return (
      cmsContextPrefix() +
      "/cm/app/spa.jsp?entry=editor&contentId=" +
      encodeURIComponent(String(pageid)) +
      "&mode=" +
      (mode || editorMode())
    );
  }

  // Mirrors `editorWindowName`: one named window per page id.
  function editorWindowName(pageid) {
    return "percEditor_" + String(pageid);
  }

  function openPageMetadata(pageid) {
    if (typeof window === "undefined" || !window.open) {
      return;
    }
    var mode = editorMode();
    window.open(editorUrl(pageid, mode), editorWindowName(pageid), EDITOR_WINDOW_FEATURES);
  }

  $.perc_page_metadata_editor = {
    open: openPageMetadata,
    // Test seams (also used by callers that need the URL without opening).
    url: editorUrl,
    mode: editorMode,
    windowName: editorWindowName,
    windowFeatures: EDITOR_WINDOW_FEATURES,
  };
})(jQuery);
