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
 * Helpers for the PublishingShell site-workspace item takedown
 * (Remove From Site) surface (#5930).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "publishing-shell",
  sitesSection: "publish-section-sites",
  siteCardPrefix: "publish-site-card-",
  workspace: "publish-site-workspace",
  panel: "item-takedown",
  input: "item-takedown-id",
  kind: "item-takedown-kind",
  kindPage: "item-takedown-kind-page",
  kindResource: "item-takedown-kind-resource",
  review: "item-takedown-review",
  confirm: "item-takedown-confirm",
  linked: "item-takedown-linked",
  linkedEmpty: "item-takedown-linked-empty",
  submit: "item-takedown-submit",
  success: "item-takedown-success",
  error: "item-takedown-error",
});

function publishingSitesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/cm/app/spa.jsp?entry=publish`;
}

/**
 * Linked-page lookup GET for the takedown confirm.
 * @param {string} url
 * @returns {boolean}
 */
function isFindLinkedItemsUrl(url) {
  return /\/services\/itemmanagement\/item\/findLinkedItems\/[^/?#]+/.test(
    String(url || ""),
  );
}

/**
 * Existing takedown route (GET with no linked pages, PUT of the linked-page
 * list when the confirm list is non-empty).
 * @param {string} url
 * @returns {boolean}
 */
function isTakedownUrl(url) {
  return /\/sitemanage\/publish\/takedown\/(page|resource)(\/staging)?\/[^/?#]+/.test(
    String(url || ""),
  );
}

function isWritePublishUrl(url) {
  const raw = String(url || "");
  return (
    /\/sitemanage\/publish\//i.test(raw) ||
    /\/publishmanagement\/servers\//i.test(raw) ||
    /\/itemmanagement\/item\/publish/i.test(raw) ||
    isTakedownUrl(raw)
  );
}

function isKnownPublishConsoleNoise(text) {
  const t = String(text || "");
  return /Download the React DevTools/i.test(t);
}

module.exports = {
  TEST_IDS,
  publishingSitesUrl,
  isFindLinkedItemsUrl,
  isTakedownUrl,
  isWritePublishUrl,
  isKnownPublishConsoleNoise,
};
