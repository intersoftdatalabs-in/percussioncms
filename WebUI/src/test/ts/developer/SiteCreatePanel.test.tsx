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
import * as sitesApi from "../../../main/ts/api/developer/sitesApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { SiteCreatePanel } from "../../../main/ts/developer/SiteCreatePanel";

vi.mock("../../../main/ts/api/developer/sitesApi", async () => {
  const actual = await vi.importActual<typeof sitesApi>(
    "../../../main/ts/api/developer/sitesApi",
  );
  return {
    ...actual,
    createSite: vi.fn(),
  };
});

const createSite = sitesApi.createSite as ReturnType<typeof vi.fn>;

describe("SiteCreatePanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    createSite.mockReset();
  });

  it("does not POST until name is valid", () => {
    render(<SiteCreatePanel onBack={() => undefined} />);
    fireEvent.click(screen.getByTestId("developer-site-create-save"));
    expect(createSite).not.toHaveBeenCalled();
  });

  it("creates a site and shows notice", async () => {
    createSite.mockResolvedValue({ name: "NightlySite" });
    const onCreated = vi.fn();
    render(<SiteCreatePanel onBack={() => undefined} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("developer-site-create-name"), {
      target: { value: "NightlySite" },
    });
    fireEvent.change(screen.getByTestId("developer-site-create-description"), {
      target: { value: "  Docs  " },
    });
    fireEvent.click(screen.getByTestId("developer-site-create-save"));
    await waitFor(() => {
      expect(createSite).toHaveBeenCalledWith({
        name: "NightlySite",
        description: "Docs",
      });
    });
    expect(screen.getByTestId("developer-site-create-notice").textContent).toBe(
      DEV_MSG.SITE_CREATED,
    );
    expect(onCreated).toHaveBeenCalled();
  });

  it("maps 409 to duplicate message", async () => {
    createSite.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: "exists",
    });
    render(<SiteCreatePanel onBack={() => undefined} />);
    fireEvent.change(screen.getByTestId("developer-site-create-name"), {
      target: { value: "NightlySite" },
    });
    fireEvent.click(screen.getByTestId("developer-site-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-site-create-error").textContent).toContain(
        DEV_MSG.SITE_DUPLICATE,
      );
    });
  });
});
