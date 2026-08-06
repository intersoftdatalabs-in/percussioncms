/**
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unit tests for classic Finder root display-label mapping
 * (WebUI/src/main/webapp/cm/plugins/perc_finder_root_display.js).
 *
 * Paths / identity stay English; only display labels use
 * perc.ui.finder.root@* TMX keys.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { beforeEach, afterEach, describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/plugins/perc_finder_root_display.js",
);
const FINDER_SRC = resolve(
  __dirname,
  "../../main/webapp/cm/widgets/perc_finder.js",
);

const KNOWN_ROOTS = ["Sites", "Assets", "Design", "Search", "Recycling"];

function loadHelper() {
  const code = readFileSync(SRC_PATH, "utf8");
  (0, eval)(code);
  return globalThis.percFinderRootDisplay;
}

describe("percFinderRootDisplay", () => {
  let api;
  let prevI18N;
  let prevApi;

  beforeEach(() => {
    prevI18N = globalThis.I18N;
    prevApi = globalThis.percFinderRootDisplay;
    delete globalThis.I18N;
    delete globalThis.percFinderRootDisplay;
    api = loadHelper();
  });

  afterEach(() => {
    if (prevI18N === undefined) {
      delete globalThis.I18N;
    } else {
      globalThis.I18N = prevI18N;
    }
    if (prevApi === undefined) {
      delete globalThis.percFinderRootDisplay;
    } else {
      globalThis.percFinderRootDisplay = prevApi;
    }
  });

  it("maps each known English root to perc.ui.finder.root@* key", () => {
    for (const root of KNOWN_ROOTS) {
      expect(api.i18nKeyForFinderRoot(root)).toBe(
        `perc.ui.finder.root@${root}`,
      );
    }
  });

  it("returns null for non-root folder names and empty input", () => {
    expect(api.i18nKeyForFinderRoot("MySite")).toBeNull();
    expect(api.i18nKeyForFinderRoot("sites")).toBeNull(); // case-sensitive
    expect(api.i18nKeyForFinderRoot("")).toBeNull();
    expect(api.i18nKeyForFinderRoot(null)).toBeNull();
    expect(api.i18nKeyForFinderRoot(undefined)).toBeNull();
  });

  it("displayLabel uses injected messageFn for known roots only", () => {
    const es = {
      "perc.ui.finder.root@Sites": "Sitios",
      "perc.ui.finder.root@Assets": "Activos",
      "perc.ui.finder.root@Design": "Diseño",
      "perc.ui.finder.root@Search": "Buscar",
      "perc.ui.finder.root@Recycling": "Reciclaje",
    };
    const messageFn = (key) => es[key];
    expect(api.displayLabelForFinderRoot("Sites", messageFn)).toBe("Sitios");
    expect(api.displayLabelForFinderRoot("Assets", messageFn)).toBe("Activos");
    expect(api.displayLabelForFinderRoot("Design", messageFn)).toBe("Diseño");
    expect(api.displayLabelForFinderRoot("Search", messageFn)).toBe("Buscar");
    expect(api.displayLabelForFinderRoot("Recycling", messageFn)).toBe(
      "Reciclaje",
    );
    // Non-root: identity preserved (path segment / folder name).
    expect(api.displayLabelForFinderRoot("MySite", messageFn)).toBe("MySite");
  });

  it("falls back to English when messageFn missing / empty / throws", () => {
    expect(api.displayLabelForFinderRoot("Sites")).toBe("Sites");
    expect(api.displayLabelForFinderRoot("Sites", () => "")).toBe("Sites");
    expect(api.displayLabelForFinderRoot("Sites", () => null)).toBe("Sites");
    expect(
      api.displayLabelForFinderRoot("Sites", () => {
        throw new Error("tmx down");
      }),
    ).toBe("Sites");
  });

  it("uses global I18N.message when messageFn omitted", () => {
    globalThis.I18N = {
      message(key) {
        if (key === "perc.ui.finder.root@Design") return "Diseño";
        return key;
      },
    };
    // Reload so closure binds to current global (already bound via global param).
    expect(api.displayLabelForFinderRoot("Design")).toBe("Diseño");
    expect(api.displayLabelForFinderRoot("Other")).toBe("Other");
  });

  it("nullish englishName becomes empty string on display", () => {
    expect(api.displayLabelForFinderRoot(null)).toBe("");
    expect(api.displayLabelForFinderRoot(undefined)).toBe("");
  });

  it("FINDER_ROOT_I18N_KEYS covers exactly the five repository roots", () => {
    expect(Object.keys(api.FINDER_ROOT_I18N_KEYS).sort()).toEqual(
      [...KNOWN_ROOTS].sort(),
    );
  });
});

describe("perc_finder.js make_item wiring (source contract)", () => {
  it("uses percFinderRootDisplay for visible name and keeps data/spec English", () => {
    const src = readFileSync(FINDER_SRC, "utf8");
    // Display path: alt / title / item-name div use displayLabel
    expect(src).toMatch(/displayLabelForFinderRoot\s*\(\s*spec\.name\s*\)/);
    expect(src).toMatch(/\.attr\(\s*["']alt["']\s*,\s*displayLabel\s*\)/);
    expect(src).toMatch(/\.attr\(\s*["']title["']\s*,\s*displayLabel\s*\)/);
    // Identity: data name and tag still from English path/name
    expect(src).toMatch(/\.data\(\s*["']spec["']\s*,\s*spec\s*\)/);
    expect(src).toMatch(
      /\.data\(\s*["']name["']\s*,\s*item_path\[\s*item_path\.length\s*-\s*1\s*\]\s*\)/,
    );
  });
});
