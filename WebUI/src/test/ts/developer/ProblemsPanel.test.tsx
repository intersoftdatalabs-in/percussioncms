/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as api from "../../../main/ts/api/developer/problemsApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ProblemsPanel } from "../../../main/ts/developer/ProblemsPanel";

vi.mock("../../../main/ts/api/developer/problemsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/problemsApi")>();
  return {
    ...actual,
    listDesignProblems: vi.fn(),
  };
});

const listDesignProblems = api.listDesignProblems as ReturnType<typeof vi.fn>;

describe("ProblemsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listDesignProblems.mockReset();
  });

  it("lists the invalid-session fixture and navigates to source", async () => {
    const onNavigate = vi.fn();
    listDesignProblems.mockResolvedValue([
      {
        id: "invalid-session",
        severity: "ERROR",
        message: "Open editor is missing a required name.",
        objectName: "Invalid open editor (fixture)",
        location: "name",
        navigateSection: "content-types",
      },
    ]);
    render(<ProblemsPanel onNavigateToSource={onNavigate} />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-prob-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-prob-message").textContent).toContain(
      "Open editor is missing a required name.",
    );
    fireEvent.click(screen.getByTestId("developer-prob-navigate"));
    expect(onNavigate).toHaveBeenCalledWith("content-types");
  });

  it("shows empty state when the session has no problems", async () => {
    listDesignProblems.mockResolvedValue([]);
    render(<ProblemsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-prob-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-prob-empty").textContent).toBe(DEV_MSG.PROB_EMPTY);
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listDesignProblems.mockRejectedValue(new SessionRedirectError());
    render(<ProblemsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-prob-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-prob-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
  });
});
