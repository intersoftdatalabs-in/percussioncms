/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import { ContentTypesPanel } from "../../../main/ts/developer/ContentTypesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/contentTypesApi", () => ({
  listContentTypes: vi.fn(),
}));

const listContentTypes = contentTypesApi.listContentTypes as ReturnType<typeof vi.fn>;

describe("ContentTypesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listContentTypes.mockReset();
  });

  it("lists content types on success", async () => {
    listContentTypes.mockResolvedValue([
      {
        name: "percPage",
        label: "Page",
        description: "Page type",
        guid: { stringValue: "0-1-2", longValue: 2 },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("Page");
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("percPage");
  });

  it("shows empty state when API returns no content types", async () => {
    listContentTypes.mockResolvedValue([]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue(new SessionRedirectError());
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ct-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(
      `${DEV_MSG.CT_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue(new Error("network down"));
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(
      `${DEV_MSG.CT_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ct-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listContentTypes.mockRejectedValue("boom");
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(DEV_MSG.CT_ERROR);
  });
});
