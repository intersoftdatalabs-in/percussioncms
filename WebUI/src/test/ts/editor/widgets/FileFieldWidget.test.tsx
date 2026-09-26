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
  blobPreviewSrc,
  displayBinaryFileName,
  FileFieldWidget,
  saveBinaryDownload,
} from "../../../../main/ts/editor/widgets/FileFieldWidget";
import { ImageFieldWidget } from "../../../../main/ts/editor/widgets/ImageFieldWidget";

describe("FileFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("disables the file input in read-only view mode", async () => {
    render(
      <FileFieldWidget
        itemId="42"
        name="item_file_attachment"
        readOnly
        loadMeta={async () => ({
          contentId: "42",
          field: "item_file_attachment",
          filename: "spec.pdf",
          contentType: "application/pdf",
          present: true,
        })}
        onFile={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-item_file_attachment")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("editor-file-item_file_attachment") as HTMLInputElement).disabled,
    ).toBe(true);
    const download = (await screen.findByTestId(
      "editor-file-download-item_file_attachment",
    )) as HTMLButtonElement;
    expect(download.disabled).toBe(false);
    expect(screen.queryByTestId("editor-file-clear-item_file_attachment")).toBeNull();
  });

  it("marks a stored file cleared locally and does not download", async () => {
    const onClear = vi.fn();
    const onFile = vi.fn();
    const downloadBinary = vi.fn();
    render(
      <FileFieldWidget
        itemId="42"
        name="item_file_attachment"
        readOnly={false}
        loadMeta={async () => ({
          contentId: "42",
          field: "item_file_attachment",
          filename: "brief.pdf",
          contentType: "application/pdf",
          present: true,
        })}
        downloadBinary={downloadBinary}
        onFile={onFile}
        onClear={onClear}
      />,
    );
    const clear = (await screen.findByTestId(
      "editor-file-clear-item_file_attachment",
    )) as HTMLButtonElement;
    fireEvent.click(clear);
    expect(onClear).toHaveBeenCalledWith(true);
    expect(onFile).toHaveBeenCalledWith(null);
    expect(downloadBinary).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-file-clear-item_file_attachment")).toBeNull();
  });

  it("names a field with no binary and does not save an empty file", async () => {
    const downloadBinary = vi.fn();
    render(
      <FileFieldWidget
        itemId="42"
        name="img"
        readOnly
        loadMeta={async () => ({
          contentId: "42",
          field: "img",
          filename: "",
          contentType: "",
          present: false,
        })}
        downloadBinary={downloadBinary}
        onFile={vi.fn()}
      />,
    );
    const button = await screen.findByTestId("editor-file-download-img");
    fireEvent.click(button);
    expect(downloadBinary).not.toHaveBeenCalled();
    expect(screen.getByTestId("editor-file-download-error-img").textContent).toContain("img");
    expect(saveBinaryDownload(new Uint8Array(), "empty.bin")).toBe(false);
  });

  it("downloads the stored name, not an unsaved pick, and keeps 403 on the field", async () => {
    const downloadBinary = vi.fn(async () => ({
      filename: "stored.pdf",
      bytes: new Uint8Array([1, 2, 3]),
    }));
    const onFile = vi.fn();
    render(
      <FileFieldWidget
        itemId="42"
        name="item_file_attachment"
        readOnly={false}
        loadMeta={async () => ({
          contentId: "42",
          field: "item_file_attachment",
          filename: "stored.pdf",
          contentType: "application/pdf",
          present: true,
        })}
        downloadBinary={downloadBinary}
        onFile={onFile}
      />,
    );
    const input = (await screen.findByTestId(
      "editor-file-item_file_attachment",
    )) as HTMLInputElement;
    fireEvent.change(input, {
      target: { files: [new File(["new"], "unsaved.pdf", { type: "application/pdf" })] },
    });
    fireEvent.click(screen.getByTestId("editor-file-download-item_file_attachment"));
    await waitFor(() => {
      expect(downloadBinary).toHaveBeenCalledWith(
        "42",
        "item_file_attachment",
        expect.any(String),
      );
    });
    expect(downloadBinary.mock.calls[0]?.[2]).toBe("stored.pdf");
    expect(saveBinaryDownload(new Uint8Array([9]), "stored.pdf")).toBe(true);

    downloadBinary.mockRejectedValueOnce({ status: 403 });
    fireEvent.click(screen.getByTestId("editor-file-download-item_file_attachment"));
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-file-download-error-item_file_attachment").textContent,
      ).toContain("item_file_attachment");
    });
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

  it("disables the image input in read-only view mode", async () => {
    render(
      <ImageFieldWidget
        itemId="7"
        name="img"
        readOnly
        loadMeta={async () => ({
          contentId: "7",
          field: "img",
          filename: "hero.png",
          contentType: "image/png",
          present: true,
        })}
        onFile={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-file-img")).toBeTruthy();
    });
    expect((screen.getByTestId("editor-file-img") as HTMLInputElement).disabled).toBe(
      true,
    );
  });

  it("strips HTML metacharacters from stored and chosen filenames", async () => {
    expect(displayBinaryFileName('<img src=x onerror=alert(1)>x.pdf')).toBe(
      "img src=x onerror=alert(1)x.pdf",
    );
    const onFile = vi.fn();
    render(
      <FileFieldWidget
        itemId="42"
        name="item_file_attachment"
        readOnly={false}
        loadMeta={async () => ({
          contentId: "42",
          field: "item_file_attachment",
          filename: '<script>alert(1)</script>stored.pdf',
          contentType: "application/pdf",
          present: true,
        })}
        onFile={onFile}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("editor-file-name-item_file_attachment").textContent,
      ).toBe("scriptalert(1)/scriptstored.pdf");
    });
    const label = screen.getByTestId("editor-file-name-item_file_attachment");
    expect(label.innerHTML).not.toMatch(/<script/i);
    const input = screen.getByTestId(
      "editor-file-item_file_attachment",
    ) as HTMLInputElement;
    const file = new File(["x"], '<img src=x onerror=alert(1)>next.pdf', {
      type: "application/pdf",
    });
    fireEvent.change(input, { target: { files: [file] } });
    expect(onFile).toHaveBeenCalledWith(file);
    await waitFor(() => {
      expect(label.textContent).toBe("img src=x onerror=alert(1)next.pdf");
    });
    expect(label.innerHTML).not.toMatch(/<img/i);
  });

  it("only assigns blob: URLs as the image preview src", async () => {
    expect(blobPreviewSrc("blob:http://localhost/abc")).toBe(
      "blob:http://localhost/abc",
    );
    expect(blobPreviewSrc("javascript:alert(1)")).toBe("");
    expect(blobPreviewSrc("https://evil.example/x.png")).toBe("");
    expect(blobPreviewSrc("<img src=x onerror=alert(1)>")).toBe("");

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
      expect(screen.getByTestId("editor-file-img")).toBeTruthy();
    });
    const input = screen.getByTestId("editor-file-img") as HTMLInputElement;
    const file = new File(["x"], "hero.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });
    const preview = await waitFor(() => screen.getByTestId("editor-image-preview-img"));
    expect(preview.getAttribute("src") || "").toMatch(/^blob:/);
  });
});
