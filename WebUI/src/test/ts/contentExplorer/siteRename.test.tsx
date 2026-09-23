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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../main/ts/api/client";
import { renameSite } from "../../../main/ts/api/developer/sitesApi";
import { SiteRenamePanel } from "../../../main/ts/contentExplorer/SiteRenamePanel";

vi.mock("../../../main/ts/api/client", () => ({
  post: vi.fn(),
  isApiError: (err: unknown) =>
    !!err && typeof err === "object" && typeof (err as { status?: unknown }).status === "number",
}));

const post = client.post as ReturnType<typeof vi.fn>;

describe("renameSite API (#4764)", () => {
  beforeEach(() => {
    post.mockReset();
  });

  it("POSTs RenameSiteRequest and returns the new name", async () => {
    post.mockResolvedValue({ Site: { name: "RenamedSite" } });
    const site = await renameSite("Nightly Site", "RenamedSite");
    expect(site.name).toBe("RenamedSite");
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/sites/Nightly%20Site/rename"),
      { RenameSiteRequest: { name: "RenamedSite" } },
    );
  });
});

describe("SiteRenamePanel (#4764)", () => {
  it("cancel does not submit", () => {
    const submit = vi.fn();
    const onCancel = vi.fn();
    render(
      <SiteRenamePanel
        siteName="NightlySite"
        submit={submit}
        onRenamed={() => undefined}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByTestId("site-rename-cancel"));
    expect(submit).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalled();
  });

  it("maps 409 and leaves the callback uncalled", async () => {
    const submit = vi.fn().mockRejectedValue({ status: 409, statusText: "Conflict", body: {} });
    const onRenamed = vi.fn();
    render(
      <SiteRenamePanel
        siteName="NightlySite"
        submit={submit}
        onRenamed={onRenamed}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByTestId("site-rename-name"), {
      target: { value: "Taken" },
    });
    fireEvent.click(screen.getByTestId("site-rename-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("site-rename-error").textContent).toMatch(/already uses/i);
    });
    expect(onRenamed).not.toHaveBeenCalled();
  });

  it("reports the new name after a successful rename", async () => {
    const submit = vi.fn().mockResolvedValue({ name: "RenamedSite" });
    const onRenamed = vi.fn();
    render(
      <SiteRenamePanel
        siteName="NightlySite"
        submit={submit}
        onRenamed={onRenamed}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByTestId("site-rename-name"), {
      target: { value: "RenamedSite" },
    });
    fireEvent.click(screen.getByTestId("site-rename-submit"));
    await waitFor(() => {
      expect(onRenamed).toHaveBeenCalledWith("RenamedSite");
    });
  });
});
