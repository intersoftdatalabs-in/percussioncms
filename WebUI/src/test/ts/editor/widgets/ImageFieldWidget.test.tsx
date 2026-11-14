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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  collectImageNulFieldErrors,
  imageFieldContainsNul,
  ImageFieldWidget,
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

describe("ImageFieldWidget alternative text", () => {
  afterEach(() => {
    cleanup();
  });

  const meta = {
    contentId: "42",
    field: "img",
    filename: "hero.png",
    contentType: "image/png",
    present: true,
  };

  it("renders the alt input with a stable test id and label", async () => {
    const onAlt = vi.fn();
    render(
      <ImageFieldWidget
        itemId="42"
        name="img"
        readOnly={false}
        altValue="A hero at dawn"
        onAlt={onAlt}
        loadMeta={async () => meta}
        onFile={vi.fn()}
      />,
    );
    const alt = (await screen.findByTestId(
      "editor-image-alt-img",
    )) as HTMLInputElement;
    expect(alt.value).toBe("A hero at dawn");
    expect(alt.getAttribute("aria-label")).toBe("Alternative text");
    expect(alt.getAttribute("name")).toBe("img_alt");
    expect(alt.disabled).toBe(false);
  });

  it("reports alt edits without touching the binary pick", async () => {
    const onAlt = vi.fn();
    const onFile = vi.fn();
    render(
      <ImageFieldWidget
        itemId="42"
        name="img"
        readOnly={false}
        altValue=""
        onAlt={onAlt}
        loadMeta={async () => meta}
        onFile={onFile}
      />,
    );
    const alt = await screen.findByTestId("editor-image-alt-img");
    fireEvent.change(alt, { target: { value: "New alt" } });
    expect(onAlt).toHaveBeenCalledWith("New alt");
    expect(onFile).not.toHaveBeenCalled();
  });

  it("disables the alt input in read-only view mode", async () => {
    render(
      <ImageFieldWidget
        itemId="42"
        name="img"
        readOnly
        altValue="A hero at dawn"
        onAlt={vi.fn()}
        loadMeta={async () => meta}
        onFile={vi.fn()}
      />,
    );
    const alt = (await screen.findByTestId(
      "editor-image-alt-img",
    )) as HTMLInputElement;
    expect(alt.disabled).toBe(true);
    expect(alt.value).toBe("A hero at dawn");
  });
});
