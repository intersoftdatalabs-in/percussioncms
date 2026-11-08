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
 * No-CodeMirror contract for React/TipTap surfaces (#5701 slice 1 / #5745).
 *
 * The TipTap-native source view is a plain textarea (PR #5688) — the SPA
 * tree takes no {@code codemirror} / {@code @codemirror/*} npm dependency
 * and no CodeMirror module import. The CodeMirror 5 esbuild bundle
 * ({@code modules/perc-tinymce}, {@code codemirror.bundle.*}) exists only
 * for the TinyMCE {@code codemirror} plugin + classic dialogs until the
 * packaging slice (#5734) deletes them; it must never leak into
 * {@code src/main/ts}.
 *
 * Prose mentions of "CodeMirror" in comments are allowed; module imports,
 * require calls, package.json dependency entries, and bundle script
 * references are not.
 */

import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, resolve } from "node:path";
import { describe, expect, it } from "vitest";

const tsRoot = resolve(__dirname, "../../../../main/ts");
const frontendPackageJson = resolve(
  __dirname,
  "../../../../main/frontend/package.json",
);
const webuiPackageJson = resolve(__dirname, "../../../../../package.json");

const SCAN_EXTENSIONS = new Set([".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs"]);

/** Static/dynamic import or require of the CM5 package or any CM6 package. */
const CODEMIRROR_IMPORT_PATTERNS = [
  /from\s+["'](@codemirror\/[^"']+|codemirror)["']/,
  /import\s+["'](@codemirror\/[^"']+|codemirror)["']/,
  /import\s*\(\s*["'](@codemirror\/[^"']+|codemirror)["']/,
  /require\s*\(\s*["'](@codemirror\/[^"']+|codemirror)["']/,
  /codemirror\.bundle/,
];

function walkFiles(dir: string, acc: string[] = []): string[] {
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) {
      if (name === "node_modules" || name === "dist") {
        continue;
      }
      walkFiles(full, acc);
    } else {
      acc.push(full);
    }
  }
  return acc;
}

function read(path: string): string {
  // Normalize CRLF/LF so assertions are portable on Windows checkouts.
  return readFileSync(path, "utf8").replace(/\r\n/g, "\n");
}

function dependencyNames(packageJsonPath: string): string[] {
  const pkg = JSON.parse(read(packageJsonPath)) as Record<string, unknown>;
  const names: string[] = [];
  for (const section of [
    "dependencies",
    "devDependencies",
    "peerDependencies",
    "optionalDependencies",
  ]) {
    const deps = pkg[section];
    if (deps !== null && typeof deps === "object" && deps !== undefined) {
      names.push(...Object.keys(deps as Record<string, unknown>));
    }
  }
  return names;
}

describe("no CodeMirror imports in React/TipTap surfaces (#5745)", () => {
  it("src/main/ts has no codemirror/@codemirror module import or bundle reference", () => {
    const offenders: string[] = [];
    for (const file of walkFiles(tsRoot)) {
      const dot = file.lastIndexOf(".");
      if (dot < 0 || !SCAN_EXTENSIONS.has(file.slice(dot).toLowerCase())) {
        continue;
      }
      const text = read(file);
      if (CODEMIRROR_IMPORT_PATTERNS.some((pattern) => pattern.test(text))) {
        offenders.push(file);
      }
    }
    expect(offenders, "files importing CodeMirror").toEqual([]);
  });

  it("SPA package.json manifests declare no codemirror dependency", () => {
    for (const manifest of [frontendPackageJson, webuiPackageJson]) {
      const offenders = dependencyNames(manifest).filter(
        (name) => name === "codemirror" || name.startsWith("@codemirror/"),
      );
      expect(offenders, `${manifest} codemirror deps`).toEqual([]);
    }
  });
});
