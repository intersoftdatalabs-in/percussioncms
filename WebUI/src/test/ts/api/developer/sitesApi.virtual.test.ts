/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  buildVirtualSite,
  getVirtualSitePreviewStatus,
  getVirtualSiteProperties,
  parseVirtualSiteBuildResult,
  parseVirtualSitePreviewStatus,
  parseVirtualSiteProperties,
  parseVirtualSitePublishResult,
  publishVirtualSite,
  toVirtualSitePropertiesEnvelope,
  updateVirtualSiteProperties,
  virtualSitePreviewContentHref,
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
      remoteUrl: undefined,
      branch: undefined,
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
      remoteUrl: undefined,
      branch: undefined,
      configFile: undefined,
      siteKey: undefined,
      virtual: false,
    });
    expect(
      parseVirtualSiteProperties({
        VirtualSiteProperties: {
          sourceKind: "git-filesystem",
          rootPath: "docs",
          remoteUrl: "https://git.example.com/org/docs.git",
          branch: "main",
          virtual: true,
        },
      }),
    ).toEqual({
      sourceKind: "git-filesystem",
      rootPath: "docs",
      remoteUrl: "https://git.example.com/org/docs.git",
      branch: "main",
      configFile: undefined,
      siteKey: undefined,
      virtual: true,
    });
    expect(parseVirtualSiteProperties(null)).toEqual({});
  });

  it("getVirtualSiteProperties encodes name and hits /virtual", async () => {
    get.mockResolvedValue({ sourceKind: null, virtual: false });
    const out = await getVirtualSiteProperties("Help Docs");
    expect(get).toHaveBeenCalledWith(expect.stringMatching(/\/sites\/Help%20Docs\/virtual$/));
    expect(out.virtual).toBe(false);
  });

  it("toVirtualSitePropertiesEnvelope wraps VirtualSiteProperties root", () => {
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        virtual: true,
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        configFile: null,
        siteKey: null,
      },
    });
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "git-filesystem",
        rootPath: "docs",
        remoteUrl: "https://git.example.com/org/docs.git",
        branch: "main",
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "git-filesystem",
        rootPath: "docs",
        configFile: null,
        siteKey: null,
        remoteUrl: "https://git.example.com/org/docs.git",
        branch: "main",
      },
    });
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        remoteUrl: "",
        branch: "",
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        configFile: null,
        siteKey: null,
        remoteUrl: "",
        branch: "",
      },
    });
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "csv-filesystem",
        rootPath: "C:/csv-docs",
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "csv-filesystem",
        rootPath: "C:/csv-docs",
        configFile: null,
        siteKey: null,
      },
    });
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "sql-database",
        rootPath: "C:/sql-docs",
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "sql-database",
        rootPath: "C:/sql-docs",
        configFile: null,
        siteKey: null,
      },
    });
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "http-json",
        rootPath: "C:/http-json-docs",
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "http-json",
        rootPath: "C:/http-json-docs",
        configFile: null,
        siteKey: null,
      },
    });
    expect(
      toVirtualSitePropertiesEnvelope({
        sourceKind: "object-storage",
        rootPath: "C:/object-docs",
      }),
    ).toEqual({
      VirtualSiteProperties: {
        sourceKind: "object-storage",
        rootPath: "C:/object-docs",
        configFile: null,
        siteKey: null,
      },
    });
  });

  it("updateVirtualSiteProperties PUTs VirtualSiteProperties envelope", async () => {
    put.mockResolvedValue({
      VirtualSiteProperties: {
        sourceKind: "git-filesystem",
        rootPath: "C:/docs",
        virtual: true,
      },
    });
    const out = await updateVirtualSiteProperties("Help", {
      sourceKind: "git-filesystem",
      rootPath: "C:/docs",
    });
    expect(put).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help\/virtual$/),
      {
        VirtualSiteProperties: {
          sourceKind: "git-filesystem",
          rootPath: "C:/docs",
          configFile: null,
          siteKey: null,
        },
      },
    );
    expect(out.rootPath).toBe("C:/docs");
    expect(out.virtual).toBe(true);
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
      { VirtualSiteBuildRequest: {} },
    );
    expect(out.pagesWritten).toBe(5);
    expect(out.outputPath).toContain("virtual-sites");
  });

  it("buildVirtualSite sends outputRoot when provided", async () => {
    post.mockResolvedValue({ pagesWritten: 1 });
    await buildVirtualSite("Help", { outputRoot: "C:/custom/out" });
    expect(post).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help\/virtual\/build$/),
      { VirtualSiteBuildRequest: { outputRoot: "C:/custom/out" } },
    );
  });

  it("parseVirtualSitePreviewStatus handles wrap and missing payload", () => {
    expect(
      parseVirtualSitePreviewStatus({
        available: true,
        homePath: "8.2/index.html",
      }),
    ).toEqual(
      expect.objectContaining({ available: true, homePath: "8.2/index.html" }),
    );
    expect(
      parseVirtualSitePreviewStatus({
        VirtualSitePreviewStatus: { available: false, message: "none" },
      }),
    ).toEqual(expect.objectContaining({ available: false, message: "none" }));
    expect(parseVirtualSitePreviewStatus(null)).toEqual({});
  });

  it("getVirtualSitePreviewStatus GETs /virtual/preview", async () => {
    get.mockResolvedValue({ available: false, message: "none" });
    const out = await getVirtualSitePreviewStatus("Help Docs");
    expect(get).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help%20Docs\/virtual\/preview$/),
    );
    expect(out.available).toBe(false);
  });

  it("virtualSitePreviewContentHref encodes site and relative home", () => {
    const href = virtualSitePreviewContentHref("Help Docs", "8.2/index.html");
    expect(href).toMatch(/\/sites\/Help%20Docs\/virtual\/preview\/8.2\/index.html$/);
    expect(virtualSitePreviewContentHref("Help", "../secret")).not.toContain("..");
  });

  it("parseVirtualSitePublishResult handles plain and wrapped payloads", () => {
    expect(
      parseVirtualSitePublishResult({
        siteName: "Help",
        filesCopied: 12,
        publishPath: "C:/inetpub/wwwroot/help",
        hasLinkProblems: false,
      }),
    ).toEqual(
      expect.objectContaining({
        siteName: "Help",
        filesCopied: 12,
        publishPath: "C:/inetpub/wwwroot/help",
        hasLinkProblems: false,
      }),
    );
    expect(
      parseVirtualSitePublishResult({
        VirtualSitePublishResult: {
          publishPath: "C:/sites/help",
          filesCopied: 4,
          pagesWritten: 3,
          buildOutputPath: "C:/tmp/virtual-sites/help",
        },
      }),
    ).toEqual(
      expect.objectContaining({
        publishPath: "C:/sites/help",
        filesCopied: 4,
        pagesWritten: 3,
        buildOutputPath: "C:/tmp/virtual-sites/help",
      }),
    );
    expect(parseVirtualSitePublishResult(null)).toEqual({});
  });

  it("publishVirtualSite POSTs /virtual/publish and parses result", async () => {
    post.mockResolvedValue({
      siteName: "Help",
      publishPath: "C:/sites/help",
      filesCopied: 9,
      pagesWritten: 7,
    });
    const out = await publishVirtualSite("Help Docs");
    expect(post).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Help%20Docs\/virtual\/publish$/),
      {},
    );
    expect(out.filesCopied).toBe(9);
    expect(out.publishPath).toContain("sites/help");
  });
});
