/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { TemplatesPanel } from "../../../main/ts/developer/TemplatesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { TemplateDetailPanel } from "../../../main/ts/developer/TemplateDetailPanel";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn(),
}));

vi.mock("../../../main/ts/developer/TemplateDetailPanel", () => ({
  TemplateDetailPanel: vi.fn(),
}));

const TemplateDetailPanelMock = TemplateDetailPanel as unknown as ReturnType<
  typeof vi.fn
>;

const listTemplates = assemblyApi.listTemplates as ReturnType<typeof vi.fn>;

describe("TemplatesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listTemplates.mockReset();
    TemplateDetailPanelMock.mockReset();
    TemplateDetailPanelMock.mockImplementation(
      (props: { idOrName: string; onBack: () => void }) => (
        <div data-testid="developer-tpl-detail">
          <button type="button" data-testid="developer-tpl-back" onClick={props.onBack}>
            back
          </button>
          {props.idOrName}
        </div>
      ),
    );
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
    expect(screen.getByTestId("developer-tpl-import")).toBeTruthy();
  });

  it("shows empty state when API returns no templates", async () => {
    listTemplates.mockResolvedValue([]);
    render(<TemplatesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-tpl-import")).toBeTruthy();
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

  it("keeps a TemplateDetailPanel render throw inside the panel (#3377)", async () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    listTemplates.mockResolvedValue([
      {
        templateId: 42,
        templateName: "perc.page",
        templateLabel: "Page",
      },
    ]);
    TemplateDetailPanelMock.mockImplementation(() => {
      throw new Error("Cannot read properties of undefined (reading 'workflowName')");
    });
    render(
      <div>
        <div data-testid="developer-chrome">Developer chrome</div>
        <TemplatesPanel />
      </div>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-table")).toBeTruthy();
    });
    const openBtn = screen.getByRole("button", { name: /Open Page/i });
    openBtn.click();
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-panel-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-chrome")).toBeTruthy();
    expect(screen.queryByTestId("route-error")).toBeNull();
    expect(screen.queryByText(/Unable to load Developer/i)).toBeNull();
    spy.mockRestore();
  });
});
