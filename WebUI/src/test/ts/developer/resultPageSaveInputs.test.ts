/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import { resolveResultPageSaveInputs } from "../../../main/ts/developer/PipelineDetailPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

describe("resolveResultPageSaveInputs", () => {
  it("rejects a blank resource name", () => {
    expect(resolveResultPageSaveInputs("   ", "pages/result.xsl")).toEqual({
      ok: false,
      error: DEV_MSG.PIPE_RESULT_PAGE_RESOURCE_REQUIRED,
    });
  });

  it("rejects a blank stylesheet URI", () => {
    expect(resolveResultPageSaveInputs("contenteditor", "   ")).toEqual({
      ok: false,
      error: DEV_MSG.PIPE_RESULT_PAGE_STYLESHEET_REQUIRED,
    });
  });

  it("normalizes a legacy file: URI before save", () => {
    expect(resolveResultPageSaveInputs("contenteditor", "file:pages/result.xsl")).toEqual({
      ok: true,
      resource: "contenteditor",
      stylesheetUri: "pages/result.xsl",
    });
  });

  it("trims inputs on the happy path", () => {
    expect(resolveResultPageSaveInputs("  contenteditor ", "  pages/result.xsl  ")).toEqual({
      ok: true,
      resource: "contenteditor",
      stylesheetUri: "pages/result.xsl",
    });
  });
});
