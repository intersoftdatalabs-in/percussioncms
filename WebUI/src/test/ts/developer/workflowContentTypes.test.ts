/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import { buildAllowedContentTypesReplaceBody } from "../../../main/ts/developer/workflowContentTypes";

describe("workflowContentTypes helpers (SY-06)", () => {
  it("builds a full-replace body with payload refs only", () => {
    expect(
      buildAllowedContentTypesReplaceBody([
        {
          name: "percPage",
          label: "Page",
          guid: { stringValue: "0-2-311", uuid: 311 },
        },
        { name: "percImage", label: "Image" },
      ]),
    ).toEqual({
      allowedContentTypes: [
        {
          name: "percPage",
          guid: { stringValue: "0-2-311", uuid: 311 },
        },
        { name: "percImage" },
      ],
    });
  });

  it("allows an empty list to clear associations", () => {
    expect(buildAllowedContentTypesReplaceBody([])).toEqual({
      allowedContentTypes: [],
    });
  });
});
