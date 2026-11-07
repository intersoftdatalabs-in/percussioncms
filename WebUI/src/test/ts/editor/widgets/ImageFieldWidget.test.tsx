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
  collectImageNulFieldErrors,
  imageFieldContainsNul,
} from "../../../../main/ts/editor/widgets/ImageFieldWidget";

const NUL = "\u0000";
const MESSAGE = "That image name contains a character that cannot be saved.";

describe("imageFieldContainsNul", () => {
  it("rejects an embedded NUL and accepts an ordinary or empty image name", () => {
    expect(imageFieldContainsNul("hero.png")).toBe(false);
    expect(imageFieldContainsNul("")).toBe(false);
    expect(imageFieldContainsNul("   ")).toBe(false);
    expect(imageFieldContainsNul("ba" + NUL + "d.png")).toBe(true);
    expect(imageFieldContainsNul(NUL)).toBe(true);
    expect(imageFieldContainsNul(null)).toBe(false);
    expect(imageFieldContainsNul(undefined)).toBe(false);
  });
});

describe("collectImageNulFieldErrors", () => {
  it("flags only image rows whose stored name carries a NUL", () => {
    const errors = collectImageNulFieldErrors(
      [
        { name: "ok_image", kind: "image", value: "hero.png" },
        { name: "bad_image", kind: "image", value: "ba" + NUL + "d.png" },
        { name: "text_field", kind: "text", value: "ba" + NUL + "d.txt" },
        { name: "file_field", kind: "file", value: "ba" + NUL + ".pdf" },
      ],
      {},
      MESSAGE,
    );
    expect(errors).toEqual({ bad_image: MESSAGE });
  });

  it("flags a picked image file name that carries a NUL", () => {
    const errors = collectImageNulFieldErrors(
      [{ name: "picked_image", kind: "image", value: "" }],
      { picked_image: new File(["x"], "ba" + NUL + "d.png") },
      MESSAGE,
    );
    expect(errors).toEqual({ picked_image: MESSAGE });
  });

  it("keeps an ordinary picked image and an ordinary stored name out of the errors", () => {
    const errors = collectImageNulFieldErrors(
      [{ name: "ok_image", kind: "image", value: "hero.png" }],
      { ok_image: new File(["x"], "next.png") },
      MESSAGE,
    );
    expect(errors).toEqual({});
  });
});
