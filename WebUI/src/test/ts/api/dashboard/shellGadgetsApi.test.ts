/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  normalizeThemeSummary,
  resolveAssetUploadUrl,
  fetchThemeSummaries,
  uploadAssetFile,
  uploadAssetFiles,
} from "@/api/dashboard/shellGadgetsApi";
import * as client from "@/api/client";
import * as csrf from "@/api/csrf";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return { ...actual, get: vi.fn() };
});

vi.mock("@/api/csrf", () => ({
  getCsrfToken: vi.fn(() => null),
}));

describe("shellGadgetsApi", () => {
  const originalPathname = window.location.pathname;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(csrf.getCsrfToken).mockReturnValue(null);
  });

  afterEach(() => {
    // restore pathname when tests rewrote it via history
    window.history.replaceState({}, "", originalPathname || "/");
  });

  it("resolveAssetUploadUrl defaults to exact /cm/uploadAssetFile", () => {
    // jsdom default pathname is often /
    expect(resolveAssetUploadUrl()).toBe("/cm/uploadAssetFile");
  });

  it("resolveAssetUploadUrl prefers /cm when on cm path", () => {
    window.history.replaceState({}, "", "/cm/app/home");
    expect(resolveAssetUploadUrl()).toBe("/cm/uploadAssetFile");
  });

  it("resolveAssetUploadUrl uses Rhythmyx context when on that path", () => {
    window.history.replaceState({}, "", "/Rhythmyx/ui/index.jsp");
    expect(resolveAssetUploadUrl()).toBe("/Rhythmyx/uploadAssetFile");
  });

  it("uploadAssetFile POSTs multipart to exact upload URL with folder and type", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ result: "uploaded.txt" }),
    });
    vi.stubGlobal("fetch", fetchMock);
    vi.mocked(csrf.getCsrfToken).mockReturnValue({
      headerName: "OWASP_CSRFTOKEN",
      token: "csrf-token-value",
    });

    const file = new File(["payload"], "uploaded.txt", { type: "text/plain" });
    const result = await uploadAssetFile({
      file,
      folder: "/Assets/uploads/",
      assetType: "file",
      approveOnUpload: true,
    });

    expect(result).toEqual({
      fileName: "uploaded.txt",
      ok: true,
      assetName: "uploaded.txt",
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    // Exact path (no trailing slash / pathInfo) — must match web.xml exact mapping (GH-1812)
    expect(url).toBe("/cm/uploadAssetFile");
    expect(init.method).toBe("POST");
    expect(init.credentials).toBe("same-origin");
    expect(init.body).toBeInstanceOf(FormData);
    const form = init.body as FormData;
    expect(form.get("folder")).toBe("/Assets/uploads/");
    expect(form.get("assetType")).toBe("file");
    expect(form.get("approveOnUpload")).toBe("true");
    expect(form.get("file")).toBeInstanceOf(File);
    const headers = init.headers as Headers;
    expect(headers.get("OWASP_CSRFTOKEN")).toBe("csrf-token-value");
    expect(headers.get("Accept")).toMatch(/json/);

    vi.unstubAllGlobals();
  });

  it("uploadAssetFile surfaces HTTP status when response is not ok", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 405,
      text: async () => "",
    });
    vi.stubGlobal("fetch", fetchMock);

    const file = new File(["x"], "x.txt", { type: "text/plain" });
    const result = await uploadAssetFile({ file });
    expect(result).toEqual({
      fileName: "x.txt",
      ok: false,
      error: "HTTP 405",
    });

    vi.unstubAllGlobals();
  });

  it("uploadAssetFiles uploads each file sequentially", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ result: "a.txt" }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ result: "b.txt" }),
      });
    vi.stubGlobal("fetch", fetchMock);

    const files = [
      new File(["a"], "a.txt", { type: "text/plain" }),
      new File(["b"], "b.txt", { type: "text/plain" }),
    ];
    const out = await uploadAssetFiles(files, { folder: "/Assets/uploads/" });
    expect(out).toHaveLength(2);
    expect(out.every((r) => r.ok)).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(2);

    vi.unstubAllGlobals();
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
