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
import { FileFieldWidget } from "../../../../main/ts/editor/widgets/FileFieldWidget";
import { ImageFieldWidget } from "../../../../main/ts/editor/widgets/ImageFieldWidget";

describe("FileFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("shows existing filename and reports a chosen file", async () => {
    const onFile = vi.fn();
    render(
      <FileFieldWidget
        itemId="42"
        name="item_file_attachment"
        readOnly={false}
        loadMeta={async () => ({
          contentId: "42",
          field: "item_file_attachment",
          filename: "spec.pdf",
          contentType: "application/pdf",
          present: true,
        })}
        onFile={onFile}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-name-item_file_attachment").textContent).toContain(
        "spec.pdf",
      );
    });
    const input = screen.getByTestId("editor-file-item_file_attachment") as HTMLInputElement;
    const file = new File(["x"], "next.pdf", { type: "application/pdf" });
    fireEvent.change(input, { target: { files: [file] } });
    expect(onFile).toHaveBeenCalledWith(file);
  });

  it("image widget accepts image files", async () => {
    render(
      <ImageFieldWidget
        itemId="7"
        name="img"
        readOnly={false}
        loadMeta={async () => ({
          contentId: "7",
          field: "img",
          filename: "",
          contentType: "",
          present: false,
        })}
        onFile={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-img").getAttribute("data-editor-kind")).toBe(
        "image",
      );
    });
    expect((screen.getByTestId("editor-file-img") as HTMLInputElement).accept).toBe("image/*");
  });
});
