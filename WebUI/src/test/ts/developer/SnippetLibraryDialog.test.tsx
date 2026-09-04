/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { listVelocitySnippets } from "../../../main/ts/api/developer/velocitySnippetsApi";
import { SnippetLibraryDialog } from "../../../main/ts/developer/SnippetLibraryDialog";

vi.mock("../../../main/ts/api/developer/velocitySnippetsApi", () => ({
  listVelocitySnippets: vi.fn(),
}));

const listMock = vi.mocked(listVelocitySnippets);

const catalog = [
  {
    id: "field.field",
    title: "field",
    category: "field",
    insertText: '#field("rx:title")',
  },
  {
    id: "slot.slot_simple",
    title: "slot_simple",
    category: "slot",
    insertText: '#slot_simple("rffList")',
  },
  {
    id: "misc.inner",
    title: "inner",
    category: "misc",
    insertText: "#inner()",
  },
];

describe("SnippetLibraryDialog", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listMock.mockReset();
    listMock.mockResolvedValue(catalog);
  });

  afterEach(() => {
    cleanup();
    delete (window as { I18N?: unknown }).I18N;
  });

  it("loads catalog and inserts selected snippet text", async () => {
    const onInsert = vi.fn();
    const onCancel = vi.fn();
    render(
      <SnippetLibraryDialog open={true} onCancel={onCancel} onInsert={onInsert} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-snippet-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-tpl-snippet-row-field.field"));
    expect(screen.getByTestId("developer-tpl-snippet-preview").textContent).toContain(
      "#field",
    );
    fireEvent.click(screen.getByTestId("developer-tpl-snippet-insert"));
    expect(onInsert).toHaveBeenCalledWith('#field("rx:title")', catalog[0]);
  });

  it("filters by category and search query", async () => {
    render(
      <SnippetLibraryDialog open={true} onCancel={vi.fn()} onInsert={vi.fn()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-snippet-row-misc.inner")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-tpl-snippet-cat-slot"));
    expect(screen.queryByTestId("developer-tpl-snippet-row-field.field")).toBeNull();
    expect(screen.getByTestId("developer-tpl-snippet-row-slot.slot_simple")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-tpl-snippet-cat-all"));
    fireEvent.change(screen.getByTestId("developer-tpl-snippet-filter"), {
      target: { value: "inner" },
    });
    expect(screen.getByTestId("developer-tpl-snippet-row-misc.inner")).toBeTruthy();
    expect(screen.queryByTestId("developer-tpl-snippet-row-field.field")).toBeNull();
  });

  it("shows load error when catalog GET fails", async () => {
    listMock.mockRejectedValue(new Error("network"));
    render(
      <SnippetLibraryDialog open={true} onCancel={vi.fn()} onInsert={vi.fn()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-tpl-snippet-error")).toBeTruthy();
    });
  });
});
