/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import {
  emptyDefinition,
  extractValidationMessages,
  fromServer,
  toServerPayload,
} from "@/widgetbuilder/types";

describe("widgetbuilder types", () => {
  it("wraps payload for server", () => {
    const def = emptyDefinition();
    def.label = "L";
    def.prefix = "p";
    const payload = toServerPayload(def) as {
      WidgetBuilderDefinitionData: { label: string };
    };
    expect(payload.WidgetBuilderDefinitionData.label).toBe("L");
  });

  it("unwraps server definition", () => {
    const def = fromServer({
      WidgetBuilderDefinitionData: { label: "X", prefix: "y", widgetId: 9 },
    });
    expect(def.label).toBe("X");
    expect(def.widgetId).toBe(9);
  });

  it("extracts validation messages", () => {
    const msgs = extractValidationMessages({
      results: [{ message: "bad name" }],
    });
    expect(msgs).toEqual(["bad name"]);
  });
});
