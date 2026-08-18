/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../main/ts/api/client";
import { createVirtualSite } from "../../../main/ts/api/contentExplorer/siteCreateApi";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  put: vi.fn(),
  post: vi.fn(),
  formatApiError: (err: unknown, fallback: string) =>
    err instanceof Error ? err.message : fallback,
}));

const post = client.post as ReturnType<typeof vi.fn>;
const put = client.put as ReturnType<typeof vi.fn>;

describe("createVirtualSite (#3521)", () => {
  beforeEach(() => {
    post.mockReset();
    put.mockReset();
  });

  it("POSTs without managed nav then PUTs VirtualSiteProperties when root set", async () => {
    post.mockResolvedValue({ Site: { name: "Docs" } });
    put.mockResolvedValue({
      VirtualSiteProperties: {
        sourceKind: "git-filesystem",
        rootPath: "/opt/Percussion",
        virtual: true,
      },
    });
    const created = await createVirtualSite({
      name: "Docs",
      baseTemplateName: "perc.base.plain",
      templateName: "DocsTemplate",
      virtualRootPath: "/opt/Percussion",
    });
    expect(created.name).toBe("Docs");
    expect(post).toHaveBeenCalledTimes(1);
    const body = post.mock.calls[0]?.[1] as {
      Site: { managedNavigation?: boolean; pageBased?: boolean };
    };
    expect(body.Site.managedNavigation).toBe(false);
    expect(body.Site.pageBased).toBeUndefined();
    expect(put).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/Docs\/virtual$/),
      {
        VirtualSiteProperties: {
          sourceKind: "git-filesystem",
          rootPath: "/opt/Percussion",
          configFile: null,
          siteKey: null,
        },
      },
    );
  });

  it("skips PUT when virtualRootPath is blank", async () => {
    post.mockResolvedValue({ name: "Handoff" });
    const created = await createVirtualSite({
      name: "Handoff",
      baseTemplateName: "perc.base.plain",
      templateName: "HandoffTemplate",
    });
    expect(created.name).toBe("Handoff");
    expect(put).not.toHaveBeenCalled();
  });
});
