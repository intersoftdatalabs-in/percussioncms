/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { fetchAbout } from "@/api/about/aboutApi";
import * as client from "@/api/client";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return { ...actual, get: vi.fn() };
});

describe("aboutApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetchAbout GETs the about endpoint and returns the parsed detail", async () => {
    vi.mocked(client.get).mockResolvedValue({
      productName: "Percussion CMS",
      versionString: "Version 8.2.0 Build 20260731 (1)",
      copyright: "Percussion CMS Copyright (C) Percussion Software, Inc.  1999-2026",
      thirdPartyCopyright: "This product includes software developed by...",
    });

    const detail = await fetchAbout();

    expect(client.get).toHaveBeenCalledWith(PATHS.ABOUT, undefined, undefined);
    expect(detail.productName).toBe("Percussion CMS");
    expect(detail.versionString).toContain("8.2.0");
  });

  it("fetchAbout forwards an AbortSignal so callers can cancel the request", async () => {
    const controller = new AbortController();
    vi.mocked(client.get).mockResolvedValue({
      productName: "Percussion CMS",
      versionString: "Version 8.2.0",
      copyright: "Copyright",
      thirdPartyCopyright: "Third party",
    });

    await fetchAbout({ signal: controller.signal });

    expect(client.get).toHaveBeenCalledWith(PATHS.ABOUT, undefined, {
      signal: controller.signal,
    });
  });
});
