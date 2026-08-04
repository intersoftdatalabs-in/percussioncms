/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { BulkUploadWidget } from "@/dashboard/BulkUploadWidget";
import * as api from "@/api/dashboard/shellGadgetsApi";

vi.mock("@/api/dashboard/shellGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/shellGadgetsApi")>();
  return { ...actual, uploadAssetFiles: vi.fn() };
});

describe("BulkUploadWidget", () => {
  beforeEach(() => {
    vi.mocked(api.uploadAssetFiles).mockReset();
  });

  it("uploads selected files", async () => {
    vi.mocked(api.uploadAssetFiles).mockResolvedValue([
      { fileName: "a.txt", ok: true, assetName: "a.txt" },
    ]);
    render(<BulkUploadWidget />);
    const file = new File(["hi"], "a.txt", { type: "text/plain" });
    fireEvent.change(screen.getByTestId("bulk-upload-files"), {
      target: { files: [file] },
    });
    await waitFor(() => {
      expect(api.uploadAssetFiles).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(screen.getByTestId("bulk-upload-results")).toBeDefined();
    });
    expect(screen.getByText(/1\/1 succeeded/)).toBeDefined();
  });

  it("passes folder, asset type, and approve flag to uploadAssetFiles", async () => {
    vi.mocked(api.uploadAssetFiles).mockResolvedValue([
      { fileName: "img.png", ok: true, assetName: "img.png" },
    ]);
    render(<BulkUploadWidget />);

    fireEvent.change(screen.getByTestId("bulk-upload-folder"), {
      target: { value: "/Assets/custom/" },
    });
    fireEvent.change(screen.getByTestId("bulk-upload-type"), {
      target: { value: "image" },
    });
    // Approve checkbox is the only checkbox in the widget
    fireEvent.click(screen.getByRole("checkbox"));

    const file = new File(["png"], "img.png", { type: "image/png" });
    fireEvent.change(screen.getByTestId("bulk-upload-files"), {
      target: { files: [file] },
    });

    await waitFor(() => {
      expect(api.uploadAssetFiles).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          folder: "/Assets/custom/",
          assetType: "image",
          approveOnUpload: true,
        }),
      );
    });
  });

  it("shows per-file HTTP error from upload result (e.g. 405 mapping miss)", async () => {
    vi.mocked(api.uploadAssetFiles).mockResolvedValue([
      { fileName: "bad.bin", ok: false, error: "HTTP 405" },
    ]);
    render(<BulkUploadWidget />);
    const file = new File(["x"], "bad.bin", { type: "application/octet-stream" });
    fireEvent.change(screen.getByTestId("bulk-upload-files"), {
      target: { files: [file] },
    });
    await waitFor(() => {
      expect(screen.getByTestId("bulk-upload-results")).toBeDefined();
    });
    expect(screen.getByText(/0\/1 succeeded/)).toBeDefined();
    expect(screen.getByText(/HTTP 405/)).toBeDefined();
  });
});
