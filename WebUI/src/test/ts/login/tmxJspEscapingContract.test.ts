/*
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
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

/**
 * Contract: tmx.jsp must EcmaScript-escape catalog values before emitting the
 * login I18N map. Naive replaceAll of quotes left FR/IT strings like
 * Sélectionnez "Remplacer" as invalid JS, so dropdown locale changes for those
 * languages never reassigned window.I18N.message.
 *
 * Escaping lives in {@code PSTmxJsCatalog.toJsObjectEntries} (GH-1611), which
 * uses {@code XSSValidation.escapeJavaScript} offline-tested in perc-i18n.
 * The JSP must call that helper rather than inlining XSSValidation or a
 * broken one-liner replaceAll.
 */
describe("tmx.jsp JS escaping contract", () => {
  const jspPath = resolve(
    __dirname,
    "../../../main/webapp/tmx/tmx.jsp",
  );
  const text = readFileSync(jspPath, "utf8");

  it("emits mode=js entries via PSTmxJsCatalog (not naive replaceAll)", () => {
    expect(text).toContain("com.percussion.i18n.PSTmxJsCatalog");
    expect(text).toContain("PSTmxJsCatalog.toJsObjectEntries");
    // Must not regress to the broken one-liner escape in the JSP.
    expect(text).not.toMatch(
      /getString\([^)]+\)\.replaceAll\(\s*"\\""\s*,\s*"\\\\\\""\s*\)/,
    );
    // Must not re-introduce inline XSSValidation calls that bypass the catalog
    // helper (locale fallback + regression tests live on PSTmxJsCatalog).
    expect(text).not.toContain("XSSValidation.escapeJavaScript");
  });
});
