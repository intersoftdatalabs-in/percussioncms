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
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { TemplateSourceEditor } from "../../../main/ts/design/TemplateSourceEditor";
import { DESIGN_MSG } from "../../../main/ts/design/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  getTemplateDetail: vi.fn(),
  updateTemplateDetail: vi.fn(),
  getSlotDetail: vi.fn(),
  updateSlotDetail: vi.fn(),
}));

const getTemplateDetail = assemblyApi.getTemplateDetail as ReturnType<typeof vi.fn>;
const updateTemplateDetail = assemblyApi.updateTemplateDetail as ReturnType<typeof vi.fn>;
const getSlotDetail = assemblyApi.getSlotDetail as ReturnType<typeof vi.fn>;
const updateSlotDetail = assemblyApi.updateSlotDetail as ReturnType<typeof vi.fn>;

const VELOCITY = "Java/global/percussion/assembly/velocityAssembler";
const HTML = "Java/global/percussion/assembly/htmlAssembler";

const baseDetail = {
  templateId: 11,
  name: "site.base",
  label: "Base",
  assembler: VELOCITY,
  mimeType: "text/html",
  templateType: "page",
  templateSource: "#header()\n$body\n",
  bindings: [
    { executionOrder: 1, variable: "$title", expression: "$sys.item.title" },
  ],
  slots: [{ name: "main", label: "Main" }],
};

describe("TemplateSourceEditor assembler + slots (#2810)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getTemplateDetail.mockReset();
    updateTemplateDetail.mockReset();
    getSlotDetail.mockReset();
    updateSlotDetail.mockReset();
    getTemplateDetail.mockResolvedValue({ ...baseDetail });
    getSlotDetail.mockResolvedValue({
      name: "main",
      label: "Main",
      slotLayout: { schemaVersion: 1, orientation: "horizontal" },
      slotStyles: { schemaVersion: 1, rootclass: "slot-main" },
    });
    updateTemplateDetail.mockImplementation(async (_id, body) => ({
      ...baseDetail,
      templateSource: body.templateSource ?? baseDetail.templateSource,
      assembler: body.assembler ?? baseDetail.assembler,
      bindings: body.bindings ?? baseDetail.bindings,
    }));
    updateSlotDetail.mockImplementation(async (_id, body) => ({
      name: "main",
      slotLayout: body.slotLayout,
      slotStyles: body.slotStyles,
    }));
  });

  it("loads assembler picker and slot layout fields", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-assembler-select")).toBeTruthy();
    });
    const select = screen.getByTestId("design-tpl-assembler-select") as HTMLSelectElement;
    expect(select.value).toBe(VELOCITY);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-slot-card-0")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("design-tpl-slot-orientation-0") as HTMLSelectElement).value,
    ).toBe("horizontal");
    expect(
      (screen.getByTestId("design-tpl-slot-rootclass-0") as HTMLInputElement).value,
    ).toBe("slot-main");
  });

  it("saves assembler change via template PUT", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-assembler-select")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("design-tpl-assembler-select"), {
      target: { value: HTML },
    });
    fireEvent.click(screen.getByTestId("design-tpl-editor-save"));
    await waitFor(() => {
      expect(updateTemplateDetail).toHaveBeenCalled();
    });
    const body = updateTemplateDetail.mock.calls.at(-1)?.[1];
    expect(body.assembler).toBe(HTML);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-notice").textContent).toBe(
        DESIGN_MSG.EDITOR_SAVED,
      );
    });
  });

  it("saves dirty slot layout/styles via slot PUT", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-slot-rootclass-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("design-tpl-slot-rootclass-0"), {
      target: { value: "updated-root" },
    });
    fireEvent.change(screen.getByTestId("design-tpl-slot-orientation-0"), {
      target: { value: "vertical" },
    });
    fireEvent.click(screen.getByTestId("design-tpl-editor-save"));
    await waitFor(() => {
      expect(updateSlotDetail).toHaveBeenCalled();
    });
    const [key, body] = updateSlotDetail.mock.calls.at(-1) || [];
    expect(key).toBe("main");
    expect(body.slotLayout.orientation).toBe("vertical");
    expect(body.slotStyles.rootclass).toBe("updated-root");
    expect(updateTemplateDetail).toHaveBeenCalled();
  });

  it("still saves source and bindings from #2809", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-source-edit")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("design-tpl-editor-source-edit"), {
      target: { value: "#footer()\n" },
    });
    fireEvent.click(screen.getByTestId("design-tpl-editor-save"));
    await waitFor(() => {
      expect(updateTemplateDetail).toHaveBeenCalled();
    });
    const body = updateTemplateDetail.mock.calls.at(-1)?.[1];
    expect(body.templateSource).toBe("#footer()\n");
    expect(body.bindings[0].variable).toBe("$title");
  });
});
