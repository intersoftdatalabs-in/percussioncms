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

import { beforeEach, describe, expect, it } from "vitest";
import { DEFAULT_CREATE_ASSEMBLER } from "../../../main/ts/design/assemblerOptions";
import { DESIGN_MSG } from "../../../main/ts/design/messages";
import { validateTemplateCreateInput } from "../../../main/ts/design/templateCreate";

describe("validateTemplateCreateInput (#3305)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("accepts a modern catalog name and default assembler", () => {
    const v = validateTemplateCreateInput(
      " site.html.snippet ",
      DEFAULT_CREATE_ASSEMBLER,
    );
    expect(v.ok).toBe(true);
    expect(v.name).toBe("site.html.snippet");
    expect(v.message).toBeNull();
  });

  it("rejects blank, spaces, and illegal characters", () => {
    expect(validateTemplateCreateInput("", DEFAULT_CREATE_ASSEMBLER).message).toBe(
      DESIGN_MSG.TPL_CREATE_NAME_REQUIRED,
    );
    expect(
      validateTemplateCreateInput("has space", DEFAULT_CREATE_ASSEMBLER).message,
    ).toBe(DESIGN_MSG.TPL_CREATE_NAME_SPACES);
    expect(
      validateTemplateCreateInput("1bad", DEFAULT_CREATE_ASSEMBLER).message,
    ).toBe(DESIGN_MSG.TPL_CREATE_NAME_FORMAT);
  });

  it("rejects a blank assembler", () => {
    const v = validateTemplateCreateInput("site.html.snippet", "  ");
    expect(v.ok).toBe(false);
    expect(v.field).toBe("assembler");
    expect(v.message).toBe(DESIGN_MSG.TPL_CREATE_ASSEMBLER_REQUIRED);
  });
});
