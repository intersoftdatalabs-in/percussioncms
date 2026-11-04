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
import {
  collectFileNulFieldErrors,
  fileFieldContainsNul,
  pickedFileNameContainsNul,
} from "../../../main/ts/editor/fileField";

const NUL = "\u0000";
const MESSAGE = "That file name contains a character that cannot be saved.";

describe("fileFieldContainsNul", () => {
  it("rejects an embedded NUL and accepts an ordinary or empty file name", () => {
    expect(fileFieldContainsNul("brief.pdf")).toBe(false);
    expect(fileFieldContainsNul("")).toBe(false);
    expect(fileFieldContainsNul("   ")).toBe(false);
    expect(fileFieldContainsNul("brief" + NUL + ".pdf")).toBe(true);
    expect(fileFieldContainsNul(NUL)).toBe(true);
    expect(fileFieldContainsNul(null)).toBe(false);
    expect(fileFieldContainsNul(undefined)).toBe(false);
  });
});

describe("pickedFileNameContainsNul", () => {
  it("refuses a picked file whose name carries a NUL", () => {
    expect(pickedFileNameContainsNul(null)).toBe(false);
    expect(pickedFileNameContainsNul(undefined)).toBe(false);
    expect(pickedFileNameContainsNul(new File(["x"], "brief.pdf"))).toBe(false);
    expect(pickedFileNameContainsNul(new File(["x"], "brief" + NUL + ".pdf"))).toBe(
      true,
    );
  });
});

describe("collectFileNulFieldErrors", () => {
  it("flags only file rows whose stored name carries a NUL", () => {
    const errors = collectFileNulFieldErrors(
      [
        { name: "ok_file", kind: "file", value: "brief.pdf" },
        { name: "bad_file", kind: "file", value: "bad" + NUL + ".pdf" },
        { name: "text_field", kind: "text", value: "bad" + NUL + ".txt" },
        { name: "image_field", kind: "image", value: "bad" + NUL + ".png" },
      ],
      {},
      MESSAGE,
    );
    expect(errors).toEqual({ bad_file: MESSAGE });
  });

  it("flags a picked file name that carries a NUL", () => {
    const errors = collectFileNulFieldErrors(
      [{ name: "picked_file", kind: "file", value: "" }],
      { picked_file: new File(["x"], "brief" + NUL + ".pdf") },
      MESSAGE,
    );
    expect(errors).toEqual({ picked_file: MESSAGE });
  });

  it("keeps an ordinary picked file and an ordinary stored name out of the errors", () => {
    const errors = collectFileNulFieldErrors(
      [{ name: "ok_file", kind: "file", value: "brief.pdf" }],
      { ok_file: new File(["x"], "hero.pdf") },
      MESSAGE,
    );
    expect(errors).toEqual({});
  });
});
