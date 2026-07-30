/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ExtensionsPanel } from "../../../main/ts/developer/ExtensionsPanel";

vi.mock("../../../main/ts/api/developer/extensionsApi", () => ({
  listExtensions: vi.fn().mockResolvedValue([
    {
      extensionName: "sys_add",
      handlerName: "Java",
      context: "global/percussion/",
      fqn: "Java/global/percussion/sys_add",
      category: "sys",
    },
  ]),
  getExtensionDetail: vi.fn().mockResolvedValue({
    extensionName: "sys_add",
    fqn: "Java/global/percussion/sys_add",
    supportedInterfaces: ["com.percussion.extension.IPSExtension"],
    runtimeParameters: [{ name: "htmlParams", dataType: "java.util.Map" }],
  }),
}));

describe("ExtensionsPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("lists extensions and opens detail", async () => {
    render(<ExtensionsPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-table").textContent).toContain("sys_add");
    fireEvent.click(screen.getByTestId("developer-ex-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ex-params-table")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-ex-back"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ex-table")).toBeTruthy();
    });
  });
});
