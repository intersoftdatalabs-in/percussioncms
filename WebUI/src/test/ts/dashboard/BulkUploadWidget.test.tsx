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
});
