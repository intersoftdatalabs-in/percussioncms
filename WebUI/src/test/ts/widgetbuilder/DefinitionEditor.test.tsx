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

  it("shows validation messages and save", () => {
    const onSave = vi.fn();
    const onValidate = vi.fn();
    render(
      <DefinitionEditor
        value={emptyDefinition()}
        messages={["Name required"]}
        onChange={() => undefined}
        onSave={onSave}
        onValidate={onValidate}
        onCancel={() => undefined}
      />,
    );
    expect(screen.getByText("Name required")).toBeDefined();
    fireEvent.click(screen.getByText("perc.ui.widgetbuilder.modern@Save"));
    expect(onSave).toHaveBeenCalled();
    fireEvent.click(screen.getByText("perc.ui.widgetbuilder.modern@Validate"));
    expect(onValidate).toHaveBeenCalled();
  });

  it("shows success status", () => {
    render(
      <DefinitionEditor
        value={emptyDefinition()}
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
