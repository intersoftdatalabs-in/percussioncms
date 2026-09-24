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
 * Helpers for EditorHost translation variants (#4816).
 *
 * <p>CMS URL paths use {@code /}.</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-create-item");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  contentId: "editor-content-id",
  panel: "translations-panel",
  open900: "translations-open-variant-900",
  localeDe: "translations-locale-option-de-de",
  create: "translations-create-submit",
  createError: "translations-create-error",
});

module.exports = { TEST_IDS, editorSpaUrl };
