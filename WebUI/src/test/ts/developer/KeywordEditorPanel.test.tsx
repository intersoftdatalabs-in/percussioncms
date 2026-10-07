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
import * as keywordsApi from "../../../main/ts/api/developer/keywordsApi";
import type { KeywordSummary } from "../../../main/ts/api/developer/types";
import { KeywordEditorPanel } from "../../../main/ts/developer/KeywordEditorPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/keywordsApi", () => ({
  createKeyword: vi.fn(),
  deleteKeyword: vi.fn(),
  getKeyword: vi.fn(),
  updateKeyword: vi.fn(),
}));

const getKeyword = keywordsApi.getKeyword as ReturnType<typeof vi.fn>;
const updateKeyword = keywordsApi.updateKeyword as ReturnType<typeof vi.fn>;
const createKeyword = keywordsApi.createKeyword as ReturnType<typeof vi.fn>;
const deleteKeyword = keywordsApi.deleteKeyword as ReturnType<typeof vi.fn>;

const loaded: KeywordSummary = {
  label: "Priority",
  description: "Item priority",
  sequence: 4,
  guid: { uuid: 42, stringValue: "0-1-42" },
  choices: [{ label: "High", value: "high", description: "top", sequence: 1 }],
};

function renderEditor(initial: KeywordSummary | null = loaded) {
  const onSaved = vi.fn();
  const onDeleted = vi.fn();
  const onBack = vi.fn();
  const view = render(
    <KeywordEditorPanel
      initial={initial}
      onBack={onBack}
      onSaved={onSaved}
      onDeleted={onDeleted}
    />,
  );
  return { onSaved, onDeleted, onBack, unmount: () => view.unmount() };
}

async function readyToAdd(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("developer-kw-add-choice-save")).toBeTruthy();
  });
  await waitFor(() => {
    expect(
      (screen.getByTestId("developer-kw-add-choice-label") as HTMLInputElement).disabled,
    ).toBe(false);
  });
  fireEvent.change(screen.getByTestId("developer-kw-add-choice-label"), {
    target: { value: "Low" },
  });
  await waitFor(() => {
    expect(
      (screen.getByTestId("developer-kw-add-choice-save") as HTMLButtonElement).disabled,
    ).toBe(false);
  });
}

describe("KeywordEditorPanel add one choice", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(loaded);
    updateKeyword.mockResolvedValue(loaded);
  });

  it("renders a single JAXB choice object without throwing", async () => {
    const single = {
      ...loaded,
      choices: { label: "High", value: "high", description: "top", sequence: 1 },
    } as unknown as KeywordSummary;
    getKeyword.mockResolvedValue(single);
    renderEditor(single);
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice").getAttribute("data-choice-label")).toBe(
        "High",
      );
    });
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
  });

  it("lists the saved choice and does not show a draft before save", async () => {
    renderEditor();
    await readyToAdd();
    expect(screen.getByTestId("developer-kw-choice").getAttribute("data-choice-label")).toBe(
      "High",
    );
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
    expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain("Low");
    expect(updateKeyword).not.toHaveBeenCalled();
  });

  it("does not write a blank choice", async () => {
    renderEditor();
    await waitFor(() => {
      expect(getKeyword).toHaveBeenCalledWith("42");
    });
    const button = screen.getByTestId("developer-kw-add-choice-save") as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    fireEvent.click(button);
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
  });

  it("does not write when add is cancelled", async () => {
    const { onBack } = renderEditor();
    await readyToAdd();
    fireEvent.change(screen.getByTestId("developer-kw-add-choice-value"), {
      target: { value: "low" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-add-choice-cancel"));
    expect(
      (screen.getByTestId("developer-kw-add-choice-label") as HTMLInputElement).value,
    ).toBe("");
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
    fireEvent.click(screen.getByTestId("developer-kw-cancel"));
    expect(onBack).toHaveBeenCalled();
    expect(updateKeyword).not.toHaveBeenCalled();
  });

  it("does not add a second row for a duplicate label or value", async () => {
    renderEditor();
    await readyToAdd();
    fireEvent.change(screen.getByTestId("developer-kw-add-choice-label"), {
      target: { value: "HIGH" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-add-choice-save"));
    expect(screen.getByTestId("developer-kw-add-choice-error").textContent).toBe(
      DEV_MSG.KW_ADD_CHOICE_DUPLICATE,
    );
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);

    fireEvent.change(screen.getByTestId("developer-kw-add-choice-label"), {
      target: { value: "Other" },
    });
    fireEvent.change(screen.getByTestId("developer-kw-add-choice-value"), {
      target: { value: "HIGH" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-add-choice-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
  });

  it("shows the new choice only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor();
    await readyToAdd();
    fireEvent.change(screen.getByTestId("developer-kw-add-choice-value"), {
      target: { value: "low" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-add-choice-save"));
    await waitFor(() => {
      expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Low", value: "low", sequence: 2 },
        ],
      }),
    );
    expect(screen.getByTestId("developer-kw-add-choice-notice").textContent).toBe(
      DEV_MSG.KW_ADD_CHOICE_SAVED,
    );
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect(screen.getByTestId("developer-kw-saved-choices").textContent).toContain("Low");
  });

  it.each([400, 403, 409])(
    "keeps previous choices when the update returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          choices: [
            { label: "High", value: "high", sequence: 1 },
            { label: "Invented", value: "invented", sequence: 9 },
          ],
        },
      });
      renderEditor();
      await readyToAdd();
      fireEvent.click(screen.getByTestId("developer-kw-add-choice-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-add-choice-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-kw-add-choice-error").textContent).toContain(
        `forced ${status}`,
      );
      expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain(
        "Invented",
      );
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain("Low");
      expect(screen.queryByTestId("developer-kw-add-choice-notice")).toBeNull();
    },
  );

  it("does not replace the list when a 200 changes the keyword label", async () => {
    updateKeyword.mockResolvedValue({
      ...loaded,
      label: "Renamed",
      choices: [
        ...(loaded.choices ?? []),
        { label: "Low", value: "Low", sequence: 2 },
      ],
    });
    renderEditor();
    await readyToAdd();
    fireEvent.click(screen.getByTestId("developer-kw-add-choice-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-add-choice-error").textContent).toBe(
        DEV_MSG.KW_ADD_CHOICE_ERROR,
      );
    });
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
  });

  it("hides add-choice on create and still saves and deletes a keyword", async () => {
    getKeyword.mockResolvedValue(loaded);
    const created = renderEditor(null);
    expect(screen.queryByTestId("developer-kw-add-choice")).toBeNull();
    fireEvent.change(screen.getByTestId("developer-kw-label"), {
      target: { value: "Colors" },
    });
    fireEvent.change(screen.getByTestId("developer-kw-choices"), {
      target: { value: "Red|red|0" },
    });
    createKeyword.mockResolvedValue({ ...loaded, label: "Colors" });
    fireEvent.click(screen.getByTestId("developer-kw-save"));
    await waitFor(() => {
      expect(createKeyword).toHaveBeenCalledWith(
        expect.objectContaining({
          label: "Colors",
          choices: [{ label: "Red", value: "red", sequence: 0 }],
        }),
      );
    });
    expect(created.onSaved).toHaveBeenCalled();
    expect(updateKeyword).not.toHaveBeenCalled();
    created.unmount();

    const edited = renderEditor();
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-add-choice")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-kw-description"), {
      target: { value: "Still priority" },
    });
    updateKeyword.mockResolvedValue({ ...loaded, description: "Still priority" });
    fireEvent.click(screen.getByTestId("developer-kw-save"));
    await waitFor(() => {
      expect(updateKeyword).toHaveBeenCalledWith(
        "42",
        expect.objectContaining({
          label: "Priority",
          description: "Still priority",
          sequence: 4,
        }),
      );
    });
    expect(edited.onSaved).toHaveBeenCalled();
    edited.unmount();

    const removed = renderEditor();
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-delete")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-kw-delete"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(deleteKeyword).toHaveBeenCalledWith("42");
    });
  });
});
