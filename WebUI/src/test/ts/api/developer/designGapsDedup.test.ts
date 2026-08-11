/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CONTROL_DESIGN_GAPS,
  getControlDetail,
} from "../../../../main/ts/api/developer/controlsApi";
import {
  RELATIONSHIP_TYPE_DESIGN_GAPS,
  getRelationshipTypeDetail,
} from "../../../../main/ts/api/developer/relationshipTypesApi";
import {
  SERVER_CONFIG_DESIGN_GAPS,
  getServerConfigDetail,
} from "../../../../main/ts/api/developer/serverConfigsApi";
import * as client from "../../../../main/ts/api/client";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("REST-GAPS-02 SPA designGaps fallbacks", () => {
  it("exposes non-empty catalog constants for server-omitted list gaps", () => {
    expect(SERVER_CONFIG_DESIGN_GAPS.length).toBeGreaterThan(0);
    expect(CONTROL_DESIGN_GAPS.length).toBeGreaterThan(0);
    expect(RELATIONSHIP_TYPE_DESIGN_GAPS.length).toBeGreaterThan(0);
  });

  it("getServerConfigDetail fills designGaps when wire omits them", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      name: "LOG_CONFIG",
      displayName: "Logging",
    });
    const detail = await getServerConfigDetail("LOG_CONFIG");
    expect(detail.designGaps).toEqual(SERVER_CONFIG_DESIGN_GAPS);
    spy.mockRestore();
  });

  it("getControlDetail keeps server designGaps when present", async () => {
    const serverGaps = ["server-only-gap"];
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      name: "sys_EditBox",
      designGaps: serverGaps,
    });
    const detail = await getControlDetail("sys_EditBox");
    expect(detail.designGaps).toEqual(serverGaps);
    spy.mockRestore();
  });

  it("getRelationshipTypeDetail fills designGaps when wire omits them", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue({
      name: "rs_folder",
    });
    const detail = await getRelationshipTypeDetail("rs_folder");
    expect(detail.designGaps).toEqual(RELATIONSHIP_TYPE_DESIGN_GAPS);
    spy.mockRestore();
  });
});
