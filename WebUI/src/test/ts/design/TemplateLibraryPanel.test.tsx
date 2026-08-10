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
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import {
  TemplateLibraryPanel,
  templateSelectionKey,
} from "../../../main/ts/design/TemplateLibraryPanel";
import { DESIGN_MSG } from "../../../main/ts/design/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn(),
  getTemplateDetail: vi.fn(),
  updateTemplateDetail: vi.fn(),
  getSlotDetail: vi.fn(),
  updateSlotDetail: vi.fn(),
}));

const listTemplates = assemblyApi.listTemplates as ReturnType<typeof vi.fn>;
const getTemplateDetail = assemblyApi.getTemplateDetail as ReturnType<
  typeof vi.fn
>;
const getSlotDetail = assemblyApi.getSlotDetail as ReturnType<typeof vi.fn>;

describe("templateSelectionKey", () => {
  it("prefers name then id", () => {
    expect(
      templateSelectionKey({ templateName: "a", templateId: 1 }),
    ).toBe("a");
    expect(templateSelectionKey({ templateId: 9 })).toBe("9");
    expect(templateSelectionKey({})).toBeNull();
  });
});

describe("TemplateLibraryPanel (#2808)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listTemplates.mockReset();
    getTemplateDetail.mockReset();
    getSlotDetail.mockReset();
    getSlotDetail.mockResolvedValue({
      name: "main",
      slotLayout: { schemaVersion: 1 },
      slotStyles: { schemaVersion: 1 },
    });
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
    render(<TemplateLibraryPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-table")).toBeTruthy();
    });
    expect(screen.getByTestId("design-tpl-table").textContent).toContain("Page");
    expect(screen.getByTestId("design-tpl-table").textContent).toContain(
      "perc.page",
    );
  });

  it("shows empty state when API returns no templates", async () => {
    listTemplates.mockResolvedValue([]);
    render(<TemplateLibraryPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message on SessionRedirectError", async () => {
    listTemplates.mockRejectedValue(new SessionRedirectError());
    render(<TemplateLibraryPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("design-tpl-error").textContent).toBe(
      DESIGN_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("design-tpl-empty")).toBeNull();
  });

  it("shows ApiError status", async () => {
    listTemplates.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<TemplateLibraryPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-error")).toBeTruthy();
    });
    expect(screen.getByTestId("design-tpl-error").textContent).toBe(
      `${DESIGN_MSG.TPL_ERROR} (500)`,
    );
  });

  it("opens source + JEXL editor when a row is opened", async () => {
    listTemplates.mockResolvedValue([
      {
        templateId: 7,
        templateName: "site.base",
        templateLabel: "Base",
        templateDescription: "Base template",
      },
    ]);
    getTemplateDetail.mockResolvedValue({
      templateId: 7,
      name: "site.base",
      label: "Base",
      description: "Base template",
      assembler: "Java/global/percussion/assembly/velocityAssembler",
      mimeType: "text/html",
      templateType: "page",
      templateSource: "$sys.template",
      bindings: [{ variable: "x", expression: "1" }],
      slots: [{ name: "main" }],
    });
    render(<TemplateLibraryPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-open-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("design-tpl-open-0"));
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor")).toBeTruthy();
    });
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-source-edit")).toBeTruthy();
    });
    expect(screen.getByTestId("design-tpl-editor-name").textContent).toContain(
      "site.base",
    );
    expect(
      (screen.getByTestId("design-tpl-assembler-select") as HTMLSelectElement).value,
    ).toContain("velocityAssembler");
    expect(screen.getByTestId("design-tpl-editor-bindings-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("design-tpl-editor-back"));
    await waitFor(() => {
      expect(screen.queryByTestId("design-tpl-editor")).toBeNull();
      expect(screen.getByTestId("design-tpl-table")).toBeTruthy();
    });
  });
});
