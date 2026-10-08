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

const withTwoChoices: KeywordSummary = {
  ...loaded,
  choices: [
    { label: "High", value: "high", description: "top", sequence: 1 },
    { label: "Low", value: "low", description: "bottom", sequence: 2 },
  ],
};

function removeButton(label: string): HTMLButtonElement {
  const button = document.querySelector(
    `[data-testid="developer-kw-choice-remove"][data-choice-label="${label}"]`,
  );
  if (!(button instanceof HTMLButtonElement)) {
    throw new Error(`missing remove button for ${label}`);
  }
  return button;
}

describe("KeywordEditorPanel remove one choice", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(withTwoChoices);
    updateKeyword.mockResolvedValue(withTwoChoices);
  });

  it("does not write when remove is cancelled", async () => {
    renderEditor(withTwoChoices);
    await waitFor(() => {
      expect(removeButton("Low").disabled).toBe(false);
    });
    fireEvent.click(removeButton("Low"));
    expect(screen.getByTestId("developer-catalog-confirm-body").textContent).toContain("Low");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
    expect(screen.queryByTestId("developer-kw-remove-choice-notice")).toBeNull();
  });

  it("drops one choice only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await waitFor(() => {
      expect(removeButton("Low").disabled).toBe(false);
    });
    fireEvent.click(removeButton("Low"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(1);
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [{ label: "High", value: "high", description: "top", sequence: 1 }],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-remove-choice-notice").textContent).toBe(
      DEV_MSG.KW_REMOVE_CHOICE_SAVED,
    );
    expect(screen.getByTestId("developer-kw-choice").getAttribute("data-choice-label")).toBe(
      "High",
    );
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect(screen.getByTestId("developer-kw-delete")).toBeTruthy();
  });

  it.each([400, 403, 409])(
    "keeps previous choices when remove returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          choices: [{ label: "Invented", value: "invented", sequence: 9 }],
        },
      });
      renderEditor(withTwoChoices);
      await waitFor(() => {
        expect(removeButton("Low").disabled).toBe(false);
      });
      fireEvent.click(removeButton("Low"));
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-remove-choice-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-kw-remove-choice-error").textContent).toContain(
        `forced ${status}`,
      );
      expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain(
        "Invented",
      );
      expect(screen.queryByTestId("developer-kw-remove-choice-notice")).toBeNull();
      expect(deleteKeyword).not.toHaveBeenCalled();
    },
  );

  it("clears the last choice without deleting the keyword", async () => {
    getKeyword.mockResolvedValue(loaded);
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => ({
      ...loaded,
      choices: body.choices,
    }));
    renderEditor();
    await waitFor(() => {
      expect(removeButton("High").disabled).toBe(false);
    });
    fireEvent.click(removeButton("High"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choices-empty")).toBeTruthy();
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.queryAllByTestId("developer-kw-choice")).toHaveLength(0);
    expect(screen.getByTestId("developer-kw-editor")).toBeTruthy();
    expect(screen.getByTestId("developer-kw-delete")).toBeTruthy();
  });

  it("does not replace the list when a 200 changes the keyword label", async () => {
    updateKeyword.mockResolvedValue({
      ...withTwoChoices,
      label: "Renamed",
      choices: [{ label: "High", value: "high", description: "top", sequence: 1 }],
    });
    renderEditor(withTwoChoices);
    await waitFor(() => {
      expect(removeButton("Low").disabled).toBe(false);
    });
    fireEvent.click(removeButton("Low"));
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-remove-choice-error").textContent).toBe(
        DEV_MSG.KW_REMOVE_CHOICE_ERROR,
      );
    });
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
  });
});

function changeLabelButton(label: string): HTMLButtonElement {
  const button = document.querySelector(
    `[data-testid="developer-kw-choice-label-edit"][data-choice-label="${label}"]`,
  );
  if (!(button instanceof HTMLButtonElement)) {
    throw new Error(`missing change-label button for ${label}`);
  }
  return button;
}

describe("KeywordEditorPanel change one choice label", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(withTwoChoices);
    updateKeyword.mockResolvedValue(withTwoChoices);
  });

  async function openLabelEditor(label: string): Promise<void> {
    await waitFor(() => {
      expect(changeLabelButton(label).disabled).toBe(false);
    });
    fireEvent.click(changeLabelButton(label));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-label-input")).toBeTruthy();
    });
  }

  it("does not show the draft label before save", async () => {
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
      target: { value: "Medium" },
    });
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Medium"]'),
    ).toBeNull();
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeTruthy();
  });

  it("does not write a blank label", async () => {
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
      target: { value: "   " },
    });
    const button = screen.getByTestId("developer-kw-choice-label-save") as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    fireEvent.click(button);
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeTruthy();
  });

  it("does not write when the label edit is cancelled", async () => {
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
      target: { value: "Medium" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-label-cancel"));
    expect(screen.queryByTestId("developer-kw-choice-label-input")).toBeNull();
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeTruthy();
    expect(screen.getByTestId("developer-kw-add-choice-save")).toBeTruthy();
    expect(removeButton("Low").disabled).toBe(false);
  });

  it("does not write when the label is unchanged", async () => {
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.click(screen.getByTestId("developer-kw-choice-label-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-kw-choice-label-input")).toBeNull();
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeTruthy();
  });

  it("does not replace another choice when the label is a duplicate", async () => {
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
      target: { value: "HIGH" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-label-save"));
    expect(screen.getByTestId("developer-kw-choice-label-error").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_LABEL_DUPLICATE,
    );
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="High"]'),
    ).toBeTruthy();
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeTruthy();
    expect(screen.queryByTestId("developer-kw-choice-label-notice")).toBeNull();
  });

  it("shows the new label only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
      target: { value: " Medium " },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-label-save"));
    await waitFor(() => {
      expect(
        document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Medium"]'),
      ).toBeTruthy();
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Medium", value: "low", description: "bottom", sequence: 2 },
        ],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-label-notice").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_LABEL_SAVED,
    );
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeNull();
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="High"]'),
    ).toBeTruthy();
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect(screen.queryByTestId("developer-kw-choice-label-input")).toBeNull();
  });

  it.each([400, 403, 409])(
    "keeps the previous label when the update returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          choices: [{ label: "Later", value: "later", sequence: 9 }],
        },
      });
      renderEditor(withTwoChoices);
      await openLabelEditor("Low");
      fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
        target: { value: "Later" },
      });
      fireEvent.click(screen.getByTestId("developer-kw-choice-label-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-choice-label-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-kw-choice-label-error").textContent).toContain(
        `forced ${status}`,
      );
      expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain(
        "Later",
      );
      expect(
        document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
      ).toBeTruthy();
      expect(screen.queryByTestId("developer-kw-choice-label-notice")).toBeNull();
      expect(deleteKeyword).not.toHaveBeenCalled();
    },
  );

  it("does not replace the list when a 200 changes the keyword label", async () => {
    updateKeyword.mockResolvedValue({
      ...withTwoChoices,
      label: "Renamed",
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Medium", value: "low", description: "bottom", sequence: 2 },
      ],
    });
    renderEditor(withTwoChoices);
    await openLabelEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-label-input"), {
      target: { value: "Medium" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-label-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-label-error").textContent).toBe(
        DEV_MSG.KW_CHANGE_CHOICE_LABEL_ERROR,
      );
    });
    expect(screen.getAllByTestId("developer-kw-choice")).toHaveLength(2);
    expect(
      document.querySelector('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeTruthy();
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
  });
});

function changeValueButton(label: string): HTMLButtonElement {
  const button = document.querySelector(
    `[data-testid="developer-kw-choice-value-edit"][data-choice-label="${label}"]`,
  );
  if (!(button instanceof HTMLButtonElement)) {
    throw new Error(`missing change-value button for ${label}`);
  }
  return button;
}

function choiceByLabel(label: string): Element | null {
  return document.querySelector(
    `[data-testid="developer-kw-choice"][data-choice-label="${label}"]`,
  );
}

describe("KeywordEditorPanel change one choice value", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(withTwoChoices);
    updateKeyword.mockResolvedValue(withTwoChoices);
  });

  async function openValueEditor(label: string): Promise<void> {
    await waitFor(() => {
      expect(changeValueButton(label).disabled).toBe(false);
    });
    fireEvent.click(changeValueButton(label));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-value-input")).toBeTruthy();
    });
  }

  it("does not show the draft value before save", async () => {
    renderEditor(withTwoChoices);
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: "mid" },
    });
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("High")?.getAttribute("data-choice-value")).toBe("high");
  });

  it("does not write when the value edit is cancelled", async () => {
    renderEditor(withTwoChoices);
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: "mid" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-value-cancel"));
    expect(screen.queryByTestId("developer-kw-choice-value-input")).toBeNull();
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(screen.getByTestId("developer-kw-add-choice-save")).toBeTruthy();
    expect(changeLabelButton("Low").disabled).toBe(false);
    expect(removeButton("Low").disabled).toBe(false);
  });

  it("does not write when the value is unchanged or already the label", async () => {
    renderEditor(withTwoChoices);
    await openValueEditor("Low");
    fireEvent.click(screen.getByTestId("developer-kw-choice-value-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-kw-choice-value-input")).toBeNull();
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: "   " },
    });
    const button = screen.getByTestId("developer-kw-choice-value-save") as HTMLButtonElement;
    expect(button.disabled).toBe(false);
    fireEvent.click(button);
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
  });

  it("does not replace another choice when the value is a duplicate", async () => {
    renderEditor(withTwoChoices);
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: "HIGH" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-value-save"));
    expect(screen.getByTestId("developer-kw-choice-value-error").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_VALUE_DUPLICATE,
    );
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("High")?.getAttribute("data-choice-value")).toBe("high");
    expect(screen.queryByTestId("developer-kw-choice-value-notice")).toBeNull();
  });

  it("shows the new value only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: " mid " },
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    fireEvent.click(screen.getByTestId("developer-kw-choice-value-save"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("mid");
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Low", value: "mid", description: "bottom", sequence: 2 },
        ],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-value-notice").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_VALUE_SAVED,
    );
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("High")?.getAttribute("data-choice-value")).toBe("high");
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect(screen.queryByTestId("developer-kw-choice-value-input")).toBeNull();
    expect((screen.getByTestId("developer-kw-choices") as HTMLTextAreaElement).value).toContain(
      "Low|mid|2",
    );
  });

  it("stores a blank value as the label when that value is free", async () => {
    const distinct: KeywordSummary = {
      ...withTwoChoices,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "mid", description: "bottom", sequence: 2 },
      ],
    };
    getKeyword.mockResolvedValue(distinct);
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(distinct);
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-value-save"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("Low");
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Low", value: "Low", description: "bottom", sequence: 2 },
        ],
      }),
    );
    expect(choiceByLabel("High")?.getAttribute("data-choice-label")).toBe("High");
  });

  it.each([400, 403, 409])(
    "keeps the previous value when the update returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          choices: [{ label: "Low", value: "later", sequence: 9 }],
        },
      });
      renderEditor(withTwoChoices);
      await openValueEditor("Low");
      fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
        target: { value: "later" },
      });
      fireEvent.click(screen.getByTestId("developer-kw-choice-value-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-choice-value-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-kw-choice-value-error").textContent).toContain(
        `forced ${status}`,
      );
      expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain(
        "later",
      );
      expect(screen.queryByTestId("developer-kw-choice-value-notice")).toBeNull();
      expect(deleteKeyword).not.toHaveBeenCalled();
    },
  );

  it("does not replace the list when a 200 changes the keyword label", async () => {
    updateKeyword.mockResolvedValue({
      ...withTwoChoices,
      label: "Renamed",
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "mid", description: "bottom", sequence: 2 },
      ],
    });
    renderEditor(withTwoChoices);
    await openValueEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-value-input"), {
      target: { value: "mid" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-value-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-value-error").textContent).toBe(
        DEV_MSG.KW_CHANGE_CHOICE_VALUE_ERROR,
      );
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
  });
});

function changeDescriptionButton(label: string): HTMLButtonElement {
  const button = document.querySelector(
    `[data-testid="developer-kw-choice-description-edit"][data-choice-label="${label}"]`,
  );
  if (!(button instanceof HTMLButtonElement)) {
    throw new Error(`missing change-description button for ${label}`);
  }
  return button;
}

describe("KeywordEditorPanel set one choice description", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(withTwoChoices);
    updateKeyword.mockResolvedValue(withTwoChoices);
  });

  async function openDescriptionEditor(label: string): Promise<void> {
    await waitFor(() => {
      expect(changeDescriptionButton(label).disabled).toBe(false);
    });
    fireEvent.click(changeDescriptionButton(label));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-description-input")).toBeTruthy();
    });
  }

  it("does not show the draft description before save", async () => {
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: "note" },
    });
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
  });

  it("does not write when the description edit is cancelled", async () => {
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: "note" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-cancel"));
    expect(screen.queryByTestId("developer-kw-choice-description-input")).toBeNull();
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect(changeValueButton("Low").disabled).toBe(false);
    expect(changeLabelButton("Low").disabled).toBe(false);
    expect(removeButton("Low").disabled).toBe(false);
  });

  it("does not write when the description is unchanged", async () => {
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-kw-choice-description-input")).toBeNull();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
  });

  it("does not clear a stored description when the draft is blank", async () => {
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-description-error").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_BLANK,
    );
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
  });

  it("shows the new description only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: " note " },
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("note");
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Low", value: "low", description: "note", sequence: 2 },
        ],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-description-notice").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_SAVED,
    );
    expect(screen.getByTestId("developer-kw-saved-choices").textContent).toContain("note");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe(
      "Priority",
    );
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect(screen.queryByTestId("developer-kw-choice-description-input")).toBeNull();
    expect((screen.getByTestId("developer-kw-choices") as HTMLTextAreaElement).value).toContain(
      "Low|low|2|note",
    );
  });

  it("allows the same description on another choice", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: "top" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("top");
    });
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
    expect(choiceByLabel("High")?.getAttribute("data-choice-label")).toBe("High");
  });

  it.each([400, 403, 409])(
    "keeps the previous description when the update returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          description: "changed",
          choices: [{ label: "Low", value: "low", description: "later", sequence: 9 }],
        },
      });
      renderEditor(withTwoChoices);
      await openDescriptionEditor("Low");
      fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
        target: { value: "later" },
      });
      fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-choice-description-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-kw-choice-description-error").textContent).toContain(
        `forced ${status}`,
      );
      expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain(
        "later",
      );
      expect(screen.queryByTestId("developer-kw-choice-description-notice")).toBeNull();
      expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
        "Item priority",
      );
      expect(deleteKeyword).not.toHaveBeenCalled();
    },
  );

  it("does not replace the list when a 200 changes the keyword description", async () => {
    updateKeyword.mockResolvedValue({
      ...withTwoChoices,
      description: "changed keyword",
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "note", sequence: 2 },
      ],
    });
    renderEditor(withTwoChoices);
    await openDescriptionEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: "note" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-description-error").textContent).toBe(
        DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_ERROR,
      );
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
  });
});

function clearDescriptionButton(label: string): HTMLButtonElement {
  const button = document.querySelector(
    `[data-testid="developer-kw-choice-description-clear"][data-choice-label="${label}"]`,
  );
  if (!(button instanceof HTMLButtonElement)) {
    throw new Error(`missing clear-description button for ${label}`);
  }
  return button;
}

describe("KeywordEditorPanel clear one choice description", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(withTwoChoices);
    updateKeyword.mockResolvedValue(withTwoChoices);
  });

  async function openClearConfirm(label: string): Promise<void> {
    await waitFor(() => {
      expect(clearDescriptionButton(label).disabled).toBe(false);
    });
    fireEvent.click(clearDescriptionButton(label));
    await waitFor(() => {
      expect(screen.getByTestId("developer-catalog-confirm-body").textContent).toContain(label);
    });
  }

  it("does not write when clear is cancelled", async () => {
    renderEditor(withTwoChoices);
    await openClearConfirm("Low");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
  });

  it("does not write when the description is already empty", async () => {
    const emptyDescription: KeywordSummary = {
      ...withTwoChoices,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "   ", sequence: 2 },
      ],
    };
    getKeyword.mockResolvedValue(emptyDescription);
    renderEditor(emptyDescription);
    await openClearConfirm("Low");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("");
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
  });

  it("does not clear a stored description from the set form when the draft is blank", async () => {
    renderEditor(withTwoChoices);
    await waitFor(() => {
      expect(changeDescriptionButton("Low").disabled).toBe(false);
    });
    fireEvent.click(changeDescriptionButton("Low"));
    fireEvent.change(screen.getByTestId("developer-kw-choice-description-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-description-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-description-error").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_BLANK,
    );
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
  });

  it("shows an empty description only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => ({
      ...body,
      choices: body.choices?.map((choice) =>
        choice.description ? choice : { ...choice, description: undefined },
      ),
    }));
    renderEditor(withTwoChoices);
    await openClearConfirm("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("");
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Low", value: "low", description: "", sequence: 2 },
        ],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-description-clear-notice").textContent).toBe(
      DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_SAVED,
    );
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
    expect(choiceByLabel("High")?.getAttribute("data-choice-label")).toBe("High");
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe("Priority");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect((screen.getByTestId("developer-kw-choices") as HTMLTextAreaElement).value).not.toContain(
      "bottom",
    );
  });

  it.each([400, 403, 409])(
    "keeps the previous description when the clear returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          description: "changed",
          choices: [{ label: "Low", value: "low", description: "", sequence: 9 }],
        },
      });
      renderEditor(withTwoChoices);
      await openClearConfirm("Low");
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-choice-description-clear-error")).toBeTruthy();
      });
      expect(
        screen.getByTestId("developer-kw-choice-description-clear-error").textContent,
      ).toContain(`forced ${status}`);
      expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
      expect(choiceByLabel("High")?.getAttribute("data-choice-description")).toBe("top");
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).toContain("bottom");
      expect(screen.queryByTestId("developer-kw-choice-description-clear-notice")).toBeNull();
      expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
        "Item priority",
      );
      expect(deleteKeyword).not.toHaveBeenCalled();
    },
  );

  it("does not replace the list when a 200 changes the keyword description", async () => {
    updateKeyword.mockResolvedValue({
      ...withTwoChoices,
      description: "changed keyword",
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "", sequence: 2 },
      ],
    });
    renderEditor(withTwoChoices);
    await openClearConfirm("Low");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-description-clear-error").textContent).toBe(
        DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_ERROR,
      );
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
  });
});

function changeSequenceButton(label: string): HTMLButtonElement {
  const button = document.querySelector(
    `[data-testid="developer-kw-choice-sequence-edit"][data-choice-label="${label}"]`,
  );
  if (!(button instanceof HTMLButtonElement)) {
    throw new Error(`missing change-sequence button for ${label}`);
  }
  return button;
}

describe("KeywordEditorPanel change one choice sequence", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (key: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getKeyword.mockReset();
    updateKeyword.mockReset();
    createKeyword.mockReset();
    deleteKeyword.mockReset();
    getKeyword.mockResolvedValue(withTwoChoices);
    updateKeyword.mockResolvedValue(withTwoChoices);
  });

  async function openSequenceEditor(label: string): Promise<void> {
    await waitFor(() => {
      expect(changeSequenceButton(label).disabled).toBe(false);
    });
    fireEvent.click(changeSequenceButton(label));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-sequence-input")).toBeTruthy();
    });
  }

  it("does not show the draft sequence before save", async () => {
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    expect((screen.getByTestId("developer-kw-choice-sequence-input") as HTMLInputElement).value).toBe(
      "2",
    );
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: "9" },
    });
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    expect(choiceByLabel("High")?.getAttribute("data-choice-sequence")).toBe("1");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
  });

  it("does not write when the sequence edit is cancelled", async () => {
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-cancel"));
    expect(screen.queryByTestId("developer-kw-choice-sequence-input")).toBeNull();
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    expect(changeDescriptionButton("Low").disabled).toBe(false);
    expect(changeValueButton("Low").disabled).toBe(false);
    expect(changeLabelButton("Low").disabled).toBe(false);
    expect(removeButton("Low").disabled).toBe(false);
  });

  it("does not write when the sequence is unchanged", async () => {
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-kw-choice-sequence-input")).toBeNull();
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
  });

  it("does not write a blank sequence", async () => {
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-sequence-error").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_BLANK,
    );
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    expect(choiceByLabel("High")?.getAttribute("data-choice-sequence")).toBe("1");
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
  });

  it("does not write a non-integer sequence", async () => {
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: "1.5" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
    expect(updateKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-sequence-error").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_NOT_INTEGER,
    );
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
  });

  it("shows the new sequence only after the keyword update succeeds", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: " 9 " },
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("9");
    });
    expect(updateKeyword).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        label: "Priority",
        description: "Item priority",
        sequence: 4,
        choices: [
          { label: "High", value: "high", description: "top", sequence: 1 },
          { label: "Low", value: "low", description: "bottom", sequence: 9 },
        ],
      }),
    );
    expect(deleteKeyword).not.toHaveBeenCalled();
    expect(screen.getByTestId("developer-kw-choice-sequence-notice").textContent).toBe(
      DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_SAVED,
    );
    expect(
      choiceByLabel("Low")
        ?.querySelector('[data-testid="developer-kw-choice-sequence-text"]')
        ?.textContent,
    ).toContain("9");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
    expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
    expect(choiceByLabel("High")?.getAttribute("data-choice-sequence")).toBe("1");
    expect((screen.getByTestId("developer-kw-label") as HTMLInputElement).value).toBe("Priority");
    expect((screen.getByTestId("developer-kw-description") as HTMLInputElement).value).toBe(
      "Item priority",
    );
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
    expect(screen.queryByTestId("developer-kw-choice-sequence-input")).toBeNull();
    expect((screen.getByTestId("developer-kw-choices") as HTMLTextAreaElement).value).toContain(
      "Low|low|9|bottom",
    );
  });

  it("allows the same sequence on another choice", async () => {
    updateKeyword.mockImplementation(async (_id: string, body: KeywordSummary) => body);
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
    await waitFor(() => {
      expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("1");
    });
    expect(choiceByLabel("High")?.getAttribute("data-choice-sequence")).toBe("1");
    expect(choiceByLabel("High")?.getAttribute("data-choice-label")).toBe("High");
  });

  it.each([400, 403, 409])(
    "keeps the previous sequence when the update returns HTTP %s",
    async (status) => {
      updateKeyword.mockRejectedValue({
        status,
        statusText: "no",
        body: {
          message: `forced ${status}`,
          label: "Renamed",
          description: "changed",
          sequence: 9,
          choices: [{ label: "Low", value: "low", description: "bottom", sequence: 8 }],
        },
      });
      renderEditor(withTwoChoices);
      await openSequenceEditor("Low");
      fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
        target: { value: "8" },
      });
      fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-kw-choice-sequence-error")).toBeTruthy();
      });
      expect(screen.getByTestId("developer-kw-choice-sequence-error").textContent).toContain(
        `forced ${status}`,
      );
      expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-label")).toBe("Low");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-value")).toBe("low");
      expect(choiceByLabel("Low")?.getAttribute("data-choice-description")).toBe("bottom");
      expect(choiceByLabel("High")?.getAttribute("data-choice-sequence")).toBe("1");
      expect(screen.getByTestId("developer-kw-saved-choices").textContent).not.toContain("[8]");
      expect(screen.queryByTestId("developer-kw-choice-sequence-notice")).toBeNull();
      expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
      expect(deleteKeyword).not.toHaveBeenCalled();
    },
  );

  it("does not replace the list when a 200 changes the keyword sequence", async () => {
    updateKeyword.mockResolvedValue({
      ...withTwoChoices,
      sequence: 9,
      choices: [
        { label: "High", value: "high", description: "top", sequence: 1 },
        { label: "Low", value: "low", description: "bottom", sequence: 8 },
      ],
    });
    renderEditor(withTwoChoices);
    await openSequenceEditor("Low");
    fireEvent.change(screen.getByTestId("developer-kw-choice-sequence-input"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("developer-kw-choice-sequence-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-kw-choice-sequence-error").textContent).toBe(
        DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_ERROR,
      );
    });
    expect(choiceByLabel("Low")?.getAttribute("data-choice-sequence")).toBe("2");
    expect((screen.getByTestId("developer-kw-sequence") as HTMLInputElement).value).toBe("4");
  });
});
