/**
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
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    // jsdom provides browser globals: window, document, DOMParser, URLSearchParams, etc.
    environment: "jsdom",
    // Expose describe/it/expect/vi/beforeEach/afterEach without imports in test files
    globals: true,
    // Install jQuery as a global before each test file runs
    setupFiles: ["src/test/js/setup.js"],
    include: ["src/test/js/**/*.test.js"],
    // Reporters: verbose gives per-test output in CI logs
    reporter: "verbose",
  },
});
