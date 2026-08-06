/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { TemplatesPanel } from "../../../main/ts/developer/TemplatesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn(),
}));

const listTemplates = assemblyApi.listTemplates as ReturnType<typeof vi.fn>;

describe("TemplatesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listTemplates.mockReset();
  });

  it("lists templates on success", async () => {
    listTemplates.mockResolvedValue([
      {
        templateId: 42,
        templateName: "perc.page",
        templateLabel: "Page",
        templateDescription: "Page template",
      },
    ]);
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-table").textContent).toContain("Page");
    expect(screen.getByTestId("developer-tpl-table").textContent).toContain("perc.page");
  });

  it("shows empty state when API returns no templates", async () => {
    listTemplates.mockResolvedValue([]);
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listTemplates.mockRejectedValue(new SessionRedirectError());
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-tpl-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listTemplates.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-error").textContent).toBe(
      `${DEV_MSG.TPL_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listTemplates.mockRejectedValue(new Error("network down"));
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-error").textContent).toBe(
      `${DEV_MSG.TPL_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-tpl-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listTemplates.mockRejectedValue("boom");
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-error").textContent).toBe(DEV_MSG.TPL_ERROR);
  });
});
