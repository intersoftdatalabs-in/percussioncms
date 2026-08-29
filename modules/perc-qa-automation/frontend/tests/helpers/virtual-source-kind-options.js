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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer Sites Virtual Site source-kind option contract (#3893).
 *
 * Cycle Verify failed when the live H2 SPA loaded a stale
 * perc-modern-ui.js entry that still imported an older developer
 * chunk (csv/sql/http-json present, object-storage absent). Dumping
 * the live option values makes that deploy miss obvious.
 */

"use strict";

function requiredVirtualSourceKindValues() {
  return [
    "repository",
    "git-filesystem",
    "csv-filesystem",
    "sql-database",
    "http-json",
    "object-storage",
    "rss-atom",
    "icalendar",
  ];
}

function missingVirtualSourceKindValues(liveValues) {
  const live = Array.isArray(liveValues) ? liveValues : [];
  return requiredVirtualSourceKindValues().filter((v) => !live.includes(v));
}

function formatMissingVirtualSourceKindMessage(missing, liveValues) {
  const miss = Array.isArray(missing) ? missing : [];
  const live = Array.isArray(liveValues) ? liveValues : [];
  return (
    `developer-site-virtual-source-kind missing [${miss.join(", ")}]; ` +
    `live options: [${live.join(", ")}]. Deploy the full WebUI ` +
    `cm/modern tree (stable entry perc-modern-ui.js + hashed developer ` +
    `chunk + any index.html), not only hashed files under assets/. ` +
    `See docker/scripts/hot-deploy-webui-modern.py / perc-devctl qa-deploy-webui (#3893).`
  );
}

module.exports = {
  requiredVirtualSourceKindValues,
  missingVirtualSourceKindValues,
  formatMissingVirtualSourceKindMessage,
};
