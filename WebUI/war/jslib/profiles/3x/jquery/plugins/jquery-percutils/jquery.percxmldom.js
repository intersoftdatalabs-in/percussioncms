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
 * First-party replacement for the retired jQuery xmlDOM Plugin v1.0
 * (Out West Media, 2009-04-06, MIT/GPL dual — issue #7180, parent audit
 * #5627).
 *
 * The retired plugin was a thin wrapper around the native DOMParser plus
 * jQuery wrapping and error reporting; every in-repo consumer
 * (PercPageModel, PercTemplateModel, perc_template_layout_class,
 * perc_template_layout_helper, perc_content_viewer) only uses the
 * `$.xmlDOM(xml)` / `$.xmlDOM(xml, onErrorFn)` surface. This shim keeps
 * that exact contract natively:
 *
 *   - parses with `new DOMParser().parseFromString(xml, "text/xml")`
 *   - rejects non-XML documents and `<parsererror>` payloads
 *   - calls `onErrorFn(msg)` when provided, otherwise triggers the
 *     `xmlParseError` event on `document`, and returns an empty set
 *   - returns the parsed document wrapped in jQuery on success, with
 *     `.context` pinned to the document (classic callers read
 *     `$.xmlDOM(s).context.documentElement`; newer jQuery builds no
 *     longer set `.context` themselves)
 *
 * The only behavior intentionally dropped is the IE-only
 * `window.ActiveXObject` / `Microsoft.XMLDOM` fallback: the CMS baseline
 * is evergreen browsers with a native DOMParser (same premise as the
 * native `JFeed` replacement in
 * modules/perc-common-ui-bundle/src/main/js/shims/jfeed-shim.js).
 */
(function ($) {
  $.xmlDOM = function (xml, onErrorFn) {
    try {
      var xmlDoc = new DOMParser().parseFromString(xml, "text/xml");
      if ($.isXMLDoc(xmlDoc)) {
        var err = $("parsererror", xmlDoc);
        if (err.length === 1) {
          throw "Error: " + $(xmlDoc).text();
        }
      } else {
        throw "Unable to parse XML";
      }
    } catch (e) {
      var msg = e.name === undefined ? e : e.name + ": " + e.message;
      if (typeof onErrorFn === "function") {
        onErrorFn(msg);
      } else {
        $(document).trigger("xmlParseError", [msg]);
      }
      return $([]);
    }
    var $doc = $(xmlDoc);
    // Classic jQuery set .context to the wrapped document and the template
    // layout callers depend on it; pin it explicitly so the contract holds
    // on jQuery builds that no longer maintain .context themselves.
    $doc.context = xmlDoc;
    return $doc;
  };
})(jQuery);
