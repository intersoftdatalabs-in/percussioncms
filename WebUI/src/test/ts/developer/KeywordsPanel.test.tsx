/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as keywordsApi from "../../../main/ts/api/developer/keywordsApi";
import { KeywordsPanel } from "../../../main/ts/developer/KeywordsPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/keywordsApi", () => ({
  listKeywords: vi.fn(),
}));

const listKeywords = keywordsApi.listKeywords as ReturnType<typeof vi.fn>;

describe("KeywordsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listKeywords.mockReset();
  });

  it("lists keywords on success", async () => {
    listKeywords.mockResolvedValue([
      {
        label: "Priority",
        value: "priority",
        description: "Priority keyword",
        choices: [{ label: "High", value: "high" }],
        guid: { stringValue: "0-1-10", longValue: 10 },
      },
    ]);
    render(<KeywordsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-kw-table").textContent).toContain("Priority");
    expect(screen.getByTestId("developer-kw-table").textContent).toContain("priority");
    const open = screen.getByTestId("developer-kw-open");
    expect(open.getAttribute("aria-label")).toBe("Open Priority");
    expect(open.getAttribute("type")).toBe("button");
  });

  it("shows empty state when API returns no keywords", async () => {
    listKeywords.mockResolvedValue([]);
    render(<KeywordsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listKeywords.mockRejectedValue(new SessionRedirectError());
    render(<KeywordsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-kw-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-kw-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listKeywords.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<KeywordsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-kw-error").textContent).toBe(
      `${DEV_MSG.KW_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listKeywords.mockRejectedValue(new Error("network down"));
    render(<KeywordsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-kw-error").textContent).toBe(
      `${DEV_MSG.KW_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-kw-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listKeywords.mockRejectedValue("boom");
    render(<KeywordsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-kw-error").textContent).toBe(DEV_MSG.KW_ERROR);
  });
});
