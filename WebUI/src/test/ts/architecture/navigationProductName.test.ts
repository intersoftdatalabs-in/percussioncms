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

import { describe, expect, it } from "vitest";
import { ARCH_MSG, ARCH_MSG_KEYS } from "../../../main/ts/architecture/messages";
import { fallbackLabelFromKey, MSG } from "../../../main/ts/i18n/message";

describe("Navigation product name (#3217)", () => {
  it("uses Navigation for shell title and loading fallbacks", () => {
    expect(fallbackLabelFromKey(ARCH_MSG_KEYS.TITLE)).toBe("Navigation");
    expect(ARCH_MSG.TITLE).toBe("Navigation");
    expect(fallbackLabelFromKey(ARCH_MSG_KEYS.SHELL_LOADING)).toBe(
      "Loading Navigation…",
    );
    expect(ARCH_MSG.SHELL_LOADING).toBe("Loading Navigation…");
  });

  it("uses Navigation for top-nav label and tooltip fallbacks", () => {
    expect(fallbackLabelFromKey(MSG.NAV_ARCHITECTURE)).toBe("Navigation");
    expect(fallbackLabelFromKey(MSG.NAV_ARCHITECTURE_TITLE)).toBe(
      "Site navigation",
    );
    expect(MSG.NAV_ARCHITECTURE).toBe("perc.ui.navMenu.architecture@Navigation");
    expect(MSG.NAV_ARCHITECTURE).not.toMatch(/@Architecture$/);
    expect(ARCH_MSG_KEYS.TITLE).toBe("perc.ui.architecture.modern@Navigation");
    expect(MSG.NAV_ARCHITECTURE_TITLE).toBe(
      "perc.ui.architecture.modern@Site navigation",
    );
  });
});
