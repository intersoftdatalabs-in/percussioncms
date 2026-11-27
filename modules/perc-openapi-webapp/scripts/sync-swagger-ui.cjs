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
 * Copies the npm-tracked swagger-ui-dist bundles into the Maven build
 * staging directory (issue #7281, parent audit #5627).
 *
 * The /openapi viewer must work air-gapped: index.html loads these files
 * from the local ./swagger-ui/ path instead of the unpkg CDN. Only the two
 * bundles the viewer needs are copied (bundle JS + CSS) — the rest of the
 * swagger-ui-dist package stays out of the WAR.
 *
 * All path joins use path.join (cross-platform: Windows + Unix).
 */

const fs = require("node:fs");
const path = require("node:path");

const MODULE_DIR = path.join(__dirname, "..");
const DIST_DIR = path.join(MODULE_DIR, "node_modules", "swagger-ui-dist");
const OUT_DIR = path.join(MODULE_DIR, "target", "generated-ui", "swagger-ui");

const FILES = ["swagger-ui-bundle.js", "swagger-ui.css"];

for (const file of FILES) {
  const src = path.join(DIST_DIR, file);
  if (!fs.existsSync(src)) {
    throw new Error(`swagger-ui-dist asset missing: ${src} (run npm install first)`);
  }
}

fs.mkdirSync(OUT_DIR, { recursive: true });
for (const file of FILES) {
  fs.copyFileSync(path.join(DIST_DIR, file), path.join(OUT_DIR, file));
}

console.log(`synced ${FILES.length} swagger-ui-dist assets to ${OUT_DIR}`);
