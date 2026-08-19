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

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import {
  DesignShell,
  normalizeDesignSection,
} from "../../../main/ts/design/DesignShell";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listTemplates: vi.fn(),
  getTemplateDetail: vi.fn(),
  createTemplate: vi.fn(),
  deleteTemplate: vi.fn(),
  updateTemplateDetail: vi.fn(),
  getSlotDetail: vi.fn(),
  updateSlotDetail: vi.fn(),
}));

const listTemplates = assemblyApi.listTemplates as ReturnType<typeof vi.fn>;

describe("normalizeDesignSection", () => {
  it("defaults unknown to templates", () => {
    expect(normalizeDesignSection(undefined)).toBe("templates");
    expect(normalizeDesignSection("nope")).toBe("templates");
    expect(normalizeDesignSection("library")).toBe("templates");
    expect(normalizeDesignSection("templates")).toBe("templates");
  });
});

describe("DesignShell (#2808)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listTemplates.mockReset();
    listTemplates.mockResolvedValue([]);
  });

  it("renders shell chrome and templates panel", async () => {
    render(<DesignShell embedded />);
    expect(screen.getByTestId("perc-design-shell")).toBeTruthy();
    expect(screen.getByTestId("perc-design-shell").getAttribute("data-embedded")).toBe(
      "true",
    );
    expect(screen.getByTestId("design-shell-title")).toBeTruthy();
    expect(screen.getByTestId("tab-design-templates")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("design-tpl-empty")).toBeTruthy();
    });
  });
});
