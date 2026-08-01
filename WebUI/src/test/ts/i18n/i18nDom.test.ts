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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, it, expect } from "vitest";
import { I18N_KEY_ATTR, i18nKeyAttr } from "@/i18n/i18nDom";

describe("i18nDom", () => {
  it("emits data-i18n-key for TMX catalog keys", () => {
    const key = "perc.ui.navMenu.home@Home";
    expect(i18nKeyAttr(key)).toEqual({ [I18N_KEY_ATTR]: key });
    expect(I18N_KEY_ATTR).toBe("data-i18n-key");
  });
});
