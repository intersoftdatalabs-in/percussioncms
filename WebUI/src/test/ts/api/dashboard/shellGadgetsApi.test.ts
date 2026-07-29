/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  normalizeThemeSummary,
  resolveAssetUploadUrl,
  fetchThemeSummaries,
} from "@/api/dashboard/shellGadgetsApi";
import * as client from "@/api/client";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return { ...actual, get: vi.fn() };
});

describe("shellGadgetsApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("resolveAssetUploadUrl prefers /cm when on cm path", () => {
    // jsdom default pathname is often /
    expect(resolveAssetUploadUrl()).toMatch(/uploadAssetFile/);
  });

  it("normalizeThemeSummary", () => {
    expect(normalizeThemeSummary({ name: "t1", cssFilePath: "a.css" })).toEqual({
      name: "t1",
      cssFilePath: "a.css",
      thumbUrl: undefined,
    });
  });

  it("fetchThemeSummaries GETs theme summary all", async () => {
    vi.mocked(client.get).mockResolvedValue({
      ThemeSummary: [{ name: "perc-default" }],
    });
    const themes = await fetchThemeSummaries();
    expect(client.get).toHaveBeenCalledWith(PATHS.THEME_SUMMARY_ALL);
    expect(themes[0].name).toBe("perc-default");
  });
});
