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

"use strict";

function publishingProductUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/cm/app/spa.jsp?entry=publish`;
}

function isIncrementalPublishUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/publish\//i.test(raw);
}

function isPubServersListUrl(url) {
  const raw = String(url || "");
  return /\/publishmanagement\/servers\/[^/?#]+\/?(\?|$)/i.test(raw);
}

function isIncrementalPreviewUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/(content|relatedcontent)\//i.test(
    raw,
  );
}

function isIncrementalContentListUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/content\//i.test(raw);
}

function isIncrementalRelatedListUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/relatedcontent\//i.test(raw);
}

function isIncrementalConfirmMessage(text) {
  const t = String(text || "");
  return /confirm incremental publish/i.test(t);
}

function mockPublishServer() {
  return {
    serverId: 4614,
    serverName: "FTP-Prod",
    name: "FTP-Prod",
    siteId: 42,
    serverType: "PRODUCTION",
  };
}

function mockIncrementalPublishResponse() {
  return {
    SitePublishResponse: {
      status: "Queuing content",
      delivered: "0",
      failures: "0",
      jobid: 4614,
    },
  };
}

function isKnownPublishConsoleNoise(text) {
  const t = String(text || "");
  return /Download the React DevTools/i.test(t);
}

module.exports = {
  publishingProductUrl,
  isIncrementalPublishUrl,
  isPubServersListUrl,
  isIncrementalPreviewUrl,
  isIncrementalContentListUrl,
  isIncrementalRelatedListUrl,
  isIncrementalConfirmMessage,
  mockPublishServer,
  mockIncrementalPublishResponse,
  isKnownPublishConsoleNoise,
};
