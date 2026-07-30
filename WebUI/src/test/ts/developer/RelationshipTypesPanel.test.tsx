/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RelationshipTypesPanel } from "../../../main/ts/developer/RelationshipTypesPanel";

vi.mock("../../../main/ts/api/developer/relationshipTypesApi", () => ({
  listRelationshipTypes: vi.fn().mockResolvedValue([
    {
      name: "ActiveAssembly",
      label: "Active Assembly",
      category: "rs_activeassembly",
      categoryLabel: "Active Assembly",
      type: "system",
      systemType: true,
      allowCloning: true,
    },
  ]),
  getRelationshipTypeDetail: vi.fn().mockResolvedValue({
    name: "ActiveAssembly",
    label: "Active Assembly",
    categoryLabel: "Active Assembly",
    type: "system",
    effects: [
      {
        name: "sys_aaEffect",
        activationEndPoint: "owner",
        extensionRef: "Java/global/percussion/sys_aaEffect",
      },
    ],
    systemProperties: [{ name: "rs_allowcloning", value: "yes" }],
    userProperties: [],
    designGaps: ["Relationship type create / update / delete not supported via this API"],
  }),
}));

describe("RelationshipTypesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists relationship types and opens detail", async () => {
    render(<RelationshipTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-table").textContent).toContain("ActiveAssembly");
    fireEvent.click(screen.getByTestId("developer-rt-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-rt-effects-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-rt-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-rt-table")).toBeTruthy();
    });
  });
});
