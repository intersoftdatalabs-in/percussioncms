/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { DefinitionList } from "@/widgetbuilder/DefinitionList";

describe("DefinitionList", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
  });

  it("shows empty state", () => {
    render(
      <DefinitionList
        summaries={[]}
        onNew={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onDeploy={() => undefined}
      />,
    );
    expect(screen.getByText("perc.ui.widgetbuilder.modern@Empty")).toBeDefined();
  });

  it("renders rows and edit action", () => {
    const onEdit = vi.fn();
    render(
      <DefinitionList
        summaries={[{ widgetId: 1, label: "MyWidget", prefix: "mw", version: "1" }]}
        onNew={() => undefined}
        onEdit={onEdit}
        onDelete={() => undefined}
        onDeploy={() => undefined}
      />,
    );
    expect(screen.getByText("MyWidget")).toBeDefined();
    fireEvent.click(screen.getByText("perc.ui.widgetbuilder.modern@Edit"));
    expect(onEdit).toHaveBeenCalledWith(1);
  });
});
