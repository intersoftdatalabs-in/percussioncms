/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  buildVirtualSite,
  getVirtualSiteProperties,
  parseVirtualSiteBuildResult,
  parseVirtualSiteProperties,
  updateVirtualSiteProperties,
} from "../../../../main/ts/api/developer/sitesApi";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  put: vi.fn(),
  post: vi.fn(),
}));

const get = client.get as ReturnType<typeof vi.fn>;
const put = client.put as ReturnType<typeof vi.fn>;
const post = client.post as ReturnType<typeof vi.fn>;

describe("sitesApi virtual properties", () => {
  beforeEach(() => {
    get.mockReset();
    put.mockReset();
    post.mockReset();
  });

  it("parseVirtualSiteProperties handles plain and wrapped payloads", () => {
    expect(
      parseVirtualSiteProperties({
        sourceKind: "git-filesystem",
        rootPath: "/docs",
        virtual: true,
      }),
    ).toEqual({
      sourceKind: "git-filesystem",
      rootPath: "/docs",
      configFile: undefined,
      siteKey: undefined,
      virtual: true,
    });
    expect(
      parseVirtualSiteProperties({
        VirtualSiteProperties: {
          sourceKind: "repository",
          virtual: false,
        },
      }),
    ).toEqual({
      sourceKind: "repository",
      rootPath: undefined,
      configFile: undefined,
      siteKey: undefined,
      virtual: false,
    });
    expect(parseVirtualSiteProperties(null)).toEqual({});
  });

  it("getVirtualSiteProperties encodes name and hits /virtual", async () => {
    get.mockResolvedValue({ sourceKind: null, virtual: false });
    const out = await getVirtualSiteProperties("Help Docs");
    expect(get).toHaveBeenCalledWith(expect.stringMatching(/\/sites\/Help%20Docs\/virtual$/));
    expect(out.virtual).toBe(false);
  });

  it("updateVirtualSiteProperties PUTs body", async () => {
    put.mockResolvedValue({
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
      virtual: true,
    });
    const out = await updateVirtualSiteProperties("Help", {
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
    });
    expect(put).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help\/virtual$/),
      expect.objectContaining({ sourceKind: "git-filesystem", rootPath: "C:/docs" }),
    );
    expect(out.rootPath).toBe("C:/docs");
  });

  it("parseVirtualSiteBuildResult handles plain and wrapped payloads", () => {
    expect(
      parseVirtualSiteBuildResult({
        siteName: "Help",
        pagesWritten: 3,
        hasLinkProblems: false,
        linkProblems: [],
      }),
    ).toEqual(
      expect.objectContaining({
        siteName: "Help",
        pagesWritten: 3,
        hasLinkProblems: false,
        linkProblems: [],
      }),
    );
    expect(
      parseVirtualSiteBuildResult({
        VirtualSiteBuildResult: {
          outputPath: "C:/tmp/out",
          pagesWritten: 1,
          linkProblemCount: 2,
          hasLinkProblems: true,
          linkProblems: ["a", "b"],
        },
      }),
    ).toEqual(
      expect.objectContaining({
        outputPath: "C:/tmp/out",
        pagesWritten: 1,
        linkProblemCount: 2,
        hasLinkProblems: true,
        linkProblems: ["a", "b"],
      }),
    );
    expect(parseVirtualSiteBuildResult(null)).toEqual({});
  });

  it("buildVirtualSite POSTs /virtual/build and parses result", async () => {
    post.mockResolvedValue({
      siteName: "Help",
      outputPath: "C:/tmp/virtual-sites/help",
      pagesWritten: 5,
      linkProblemCount: 0,
      hasLinkProblems: false,
    });
    const out = await buildVirtualSite("Help Docs");
    expect(post).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help%20Docs\/virtual\/build$/),
      {},
    );
    expect(out.pagesWritten).toBe(5);
    expect(out.outputPath).toContain("virtual-sites");
  });

  it("buildVirtualSite sends outputRoot when provided", async () => {
    post.mockResolvedValue({ pagesWritten: 1 });
    await buildVirtualSite("Help", { outputRoot: "C:/custom/out" });
    expect(post).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help\/virtual\/build$/),
      { outputRoot: "C:/custom/out" },
    );
  });
});
