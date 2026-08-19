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
  deleteTemplate: vi.fn(),
  getSlotDetail: vi.fn(),
  updateSlotDetail: vi.fn(),
}));

const getTemplateDetail = assemblyApi.getTemplateDetail as ReturnType<
  typeof vi.fn
>;
const updateTemplateDetail = assemblyApi.updateTemplateDetail as ReturnType<
  typeof vi.fn
>;
const deleteTemplate = assemblyApi.deleteTemplate as ReturnType<typeof vi.fn>;
const getSlotDetail = assemblyApi.getSlotDetail as ReturnType<typeof vi.fn>;

const baseDetail = {
  templateId: 11,
  name: "site.base",
  label: "Base",
  assembler: "Java/global/percussion/assembly/velocityAssembler",
  mimeType: "text/html",
  templateType: "page",
  templateSource: "#header()\n$body\n",
  bindings: [
    { executionOrder: 1, variable: "$title", expression: "$sys.item.title" },
  ],
  slots: [{ name: "main" }],
};

describe("TemplateSourceEditor (#2809)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getTemplateDetail.mockReset();
    updateTemplateDetail.mockReset();
    deleteTemplate.mockReset();
    getSlotDetail.mockReset();
    getSlotDetail.mockResolvedValue({
      name: "main",
      slotLayout: { schemaVersion: 1 },
      slotStyles: { schemaVersion: 1 },
    });
    getTemplateDetail.mockResolvedValue({ ...baseDetail });
    updateTemplateDetail.mockImplementation(async (_id, body) => ({
      ...baseDetail,
      templateSource: body.templateSource ?? baseDetail.templateSource,
      bindings: body.bindings ?? baseDetail.bindings,
    }));
  });

  it("loads source and bindings", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-source-edit")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("design-tpl-editor-source-edit") as HTMLTextAreaElement)
        .value,
    ).toContain("#header()");
    expect(
      (screen.getByTestId("design-tpl-binding-var-0") as HTMLInputElement).value,
    ).toBe("$title");
    expect(
      (screen.getByTestId("design-tpl-editor-save") as HTMLButtonElement).disabled,
    ).toBe(true);
  });

  it("blocks save when a new binding is incomplete", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-binding-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("design-tpl-binding-add"));
    const saveBtn = screen.getByTestId("design-tpl-editor-save") as HTMLButtonElement;
    expect(saveBtn.disabled).toBe(false);
    fireEvent.click(saveBtn);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-validation")).toBeTruthy();
    });
    expect(screen.getByTestId("design-tpl-editor-validation").textContent).toMatch(
      /variable is required/i,
    );
    expect(updateTemplateDetail).not.toHaveBeenCalled();
  });

  it("saves source and binding edits", async () => {
    render(<TemplateSourceEditor idOrName="site.base" onBack={() => undefined} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-source-edit")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("design-tpl-editor-source-edit"), {
      target: { value: "#footer()\n" },
    });
    fireEvent.change(screen.getByTestId("design-tpl-binding-expr-0"), {
      target: { value: "$sys.item.fields.title" },
    });
    fireEvent.click(screen.getByTestId("design-tpl-editor-save"));
    await waitFor(() => {
      expect(updateTemplateDetail).toHaveBeenCalled();
    });
    const body = updateTemplateDetail.mock.calls.at(-1)?.[1];
    expect(body.templateSource).toBe("#footer()\n");
    expect(body.bindings[0].expression).toBe("$sys.item.fields.title");
    expect(body.bindings[0].variable).toBe("$title");
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-notice").textContent).toBe(
        DESIGN_MSG.EDITOR_SAVED,
      );
    });
  });

  it("shows load error and allows back", async () => {
    getTemplateDetail.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    const onBack = vi.fn();
    render(<TemplateSourceEditor idOrName="missing" onBack={onBack} />);
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-error")).toBeTruthy();
    });
    expect(screen.getByTestId("design-tpl-editor-error").textContent).toContain(
      "(404)",
    );
    fireEvent.click(screen.getByTestId("design-tpl-editor-back"));
    expect(onBack).toHaveBeenCalled();
  });

  it("deletes from the editor after confirm", async () => {
    const onDeleted = vi.fn();
    deleteTemplate.mockResolvedValue(undefined);
    render(
      <TemplateSourceEditor
        idOrName="site.base"
        onBack={() => undefined}
        onDeleted={onDeleted}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-editor-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("design-tpl-editor-delete"));
    expect(screen.getByTestId("design-tpl-delete-dialog")).toBeTruthy();
    fireEvent.click(screen.getByTestId("design-tpl-delete-submit"));
    await waitFor(() => {
      expect(deleteTemplate).toHaveBeenCalledWith("site.base");
    });
    await waitFor(() => {
      expect(onDeleted).toHaveBeenCalled();
    });
  });
});
