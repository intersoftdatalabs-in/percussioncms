/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";

const post = vi.fn();

vi.mock("../../../main/ts/api/client", () => ({
  post: (...args: unknown[]) => post(...args),
  get: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

import {
  COMMUNITY_VISIBILITY_TYPE_HEADER,
  getCommunityVisibility,
} from "../../../main/ts/api/developer/assemblyApi";

describe("getCommunityVisibility headers", () => {
  beforeEach(() => {
    post.mockReset();
    post.mockResolvedValue([
      {
        visibleObjects: [{ name: "percPage", type: "NODEDEF" }],
      },
    ]);
  });

  it("omits filter header when objectType is undefined", async () => {
    await getCommunityVisibility({ stringValue: "0-13-10", uuid: 10 });
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/communities/visibility"),
      [{ stringValue: "0-13-10", uuid: 10 }],
      undefined,
    );
  });

  it("omits filter header when objectType is blank or whitespace", async () => {
    await getCommunityVisibility({ stringValue: "0-13-10" }, "   ");
    expect(post).toHaveBeenCalledWith(
      expect.any(String),
      expect.any(Array),
      undefined,
    );
    await getCommunityVisibility({ stringValue: "0-13-10" }, "");
    expect(post).toHaveBeenLastCalledWith(
      expect.any(String),
      expect.any(Array),
      undefined,
    );
  });

  it("sends X-Object-Type (not bare type) when objectType is non-empty", async () => {
    await getCommunityVisibility({ stringValue: "0-13-10" }, "  NODEDEF  ");
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/communities/visibility"),
      [{ stringValue: "0-13-10" }],
      { [COMMUNITY_VISIBILITY_TYPE_HEADER]: "NODEDEF" },
    );
    const headers = post.mock.calls.at(-1)?.[2] as Record<string, string>;
    expect(headers).not.toHaveProperty("type");
    expect(headers[COMMUNITY_VISIBILITY_TYPE_HEADER]).toBe("NODEDEF");
  });
});
