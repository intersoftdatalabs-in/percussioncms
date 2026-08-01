/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { DefinitionEditor } from "@/widgetbuilder/editor/DefinitionEditor";
import { emptyDefinition } from "@/widgetbuilder/types";

describe("DefinitionEditor", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
  });

  it("shows validation messages and save when required fields are set", () => {
    const onSave = vi.fn();
    const onValidate = vi.fn();
    const value = { ...emptyDefinition(), label: "My Widget", prefix: "perc" };
    render(
      <DefinitionEditor
        value={value}
        messages={["Name required"]}
        onChange={() => undefined}
        onSave={onSave}
        onValidate={onValidate}
        onCancel={() => undefined}
      />,
    );
    expect(screen.getByText("Name required")).toBeDefined();
    // message() falls back to the segment after @ when I18N echoes the key
    fireEvent.click(screen.getByText("Save"));
    expect(onSave).toHaveBeenCalled();
    fireEvent.click(screen.getByText("Validate"));
    expect(onValidate).toHaveBeenCalled();
  });

  it("disables save/validate when label or prefix is blank", () => {
    const onSave = vi.fn();
    const onValidate = vi.fn();
    render(
      <DefinitionEditor
        value={emptyDefinition()}
        onChange={() => undefined}
        onSave={onSave}
        onValidate={onValidate}
        onCancel={() => undefined}
      />,
    );
    const save = screen.getByText("Save");
    const validate = screen.getByText("Validate");
    expect((save as HTMLButtonElement).disabled).toBe(true);
    expect((validate as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(save);
    fireEvent.click(validate);
    expect(onSave).not.toHaveBeenCalled();
    expect(onValidate).not.toHaveBeenCalled();
  });

  it("shows success status", () => {
    render(
      <DefinitionEditor
        value={{ ...emptyDefinition(), label: "X", prefix: "y" }}
        status="perc.ui.widgetbuilder.modern@Saved"
        onChange={() => undefined}
        onSave={() => undefined}
        onValidate={() => undefined}
        onCancel={() => undefined}
      />,
    );
    expect(screen.getByRole("status").textContent).toContain("Saved");
  });
});
