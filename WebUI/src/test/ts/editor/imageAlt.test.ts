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
  collectImageAltRequiredFieldErrors,
  imageAltCurrentValue,
  imageAltFieldName,
  imageAltIsDirty,
  imageAltSaveEntries,
  imageAltStoredValue,
  IMAGE_ALT_SUFFIX,
  managedImageAltNames,
} from "../../../main/ts/editor/imageAlt";

const REQUIRED = "This field is required.";

describe("imageAltFieldName", () => {
  it("names the sibling text entry that carries an image field's alt", () => {
    expect(IMAGE_ALT_SUFFIX).toBe("_alt");
    expect(imageAltFieldName("img")).toBe("img_alt");
    expect(imageAltFieldName("hero_image")).toBe("hero_image_alt");
  });
});

describe("imageAltStoredValue", () => {
  const fields = [
    { name: "sys_title", value: "Hero" },
    { name: "img_alt", value: "A hero at dawn" },
  ];

  it("reads the sibling entry and ignores the binary file name", () => {
    expect(imageAltStoredValue(fields, "img")).toBe("A hero at dawn");
    expect(imageAltStoredValue([{ name: "img", value: "hero.png" }], "img")).toBe("");
  });

  it("matches case-insensitively and tolerates a missing payload", () => {
    expect(imageAltStoredValue([{ name: "IMG_ALT", value: "Hi" }], "img")).toBe("Hi");
    expect(imageAltStoredValue([], "img")).toBe("");
    expect(imageAltStoredValue(undefined, "img")).toBe("");
  });
});

describe("imageAltCurrentValue", () => {
  const stored = [{ name: "img_alt", value: "Stored alt" }];

  it("prefers the unsaved draft entry over the stored sibling value", () => {
    expect(imageAltCurrentValue({ img_alt: "Typed alt" }, stored, "img")).toBe(
      "Typed alt",
    );
    expect(imageAltCurrentValue({}, stored, "img")).toBe("Stored alt");
    expect(imageAltCurrentValue({}, [], "img")).toBe("");
  });
});

describe("imageAltIsDirty", () => {
  const stored = [{ name: "img_alt", value: "Stored alt" }];

  it("is dirty only when the draft entry differs from storage", () => {
    expect(imageAltIsDirty(stored, {}, "img")).toBe(false);
    expect(imageAltIsDirty(stored, { img_alt: "Stored alt" }, "img")).toBe(false);
    expect(imageAltIsDirty(stored, { img_alt: "Changed alt" }, "img")).toBe(true);
    expect(imageAltIsDirty(stored, { img_alt: "" }, "img")).toBe(true);
    expect(imageAltIsDirty([], { img_alt: "" }, "img")).toBe(false);
  });
});

describe("managedImageAltNames", () => {
  it("gates only image fields whose payload defines the sibling entry", () => {
    expect(
      managedImageAltNames(["img", "banner"], [
        { name: "img_alt", value: "Hi" },
      ]),
    ).toEqual(["img"]);
    expect(managedImageAltNames(["img"], [])).toEqual([]);
    expect(managedImageAltNames(["img"], undefined)).toEqual([]);
    expect(
      managedImageAltNames(["img"], [{ name: "IMG_ALT", value: "Hi" }]),
    ).toEqual(["img"]);
  });
});

describe("collectImageAltRequiredFieldErrors", () => {
  it("refuses a blank or whitespace-only alt on a required image", () => {
    const rows = [
      { name: "img", kind: "image" as const, required: true },
      { name: "blank_img", kind: "image" as const, required: true },
      { name: "optional_img", kind: "image" as const, required: false },
      { name: "doc", kind: "file" as const, required: true },
    ];
    const errors = collectImageAltRequiredFieldErrors(
      rows,
      { blank_img: "   ", optional_img: "", doc: "" },
      REQUIRED,
    );
    expect(errors).toEqual({ img: REQUIRED, blank_img: REQUIRED });
  });

  it("keeps a non-blank alt and an optional blank alt out of the errors", () => {
    const errors = collectImageAltRequiredFieldErrors(
      [
        { name: "img", kind: "image" as const, required: true },
        { name: "optional_img", kind: "image" as const, required: false },
      ],
      { img: "A hero at dawn", optional_img: "" },
      REQUIRED,
    );
    expect(errors).toEqual({});
  });
});

describe("imageAltSaveEntries", () => {
  it("emits one sibling entry per image row for the fields PUT", () => {
    expect(
      imageAltSaveEntries(["img", "banner"], { img: "A hero", banner: "" }),
    ).toEqual([
      { name: "img_alt", value: "A hero" },
      { name: "banner_alt", value: "" },
    ]);
    expect(imageAltSaveEntries([], {})).toEqual([]);
  });
});
