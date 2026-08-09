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

import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createObjectAcl,
  getAclForObject,
  saveObjectAcl,
} from "../../../main/ts/api/developer/aclApi";
import { loadDefaultAclTemplate } from "../../../main/ts/api/developer/preferencesApi";
import { ObjectAclSection } from "../../../main/ts/developer/ObjectAclSection";
import { systemDefaultAclTemplate } from "../../../main/ts/developer/defaultAclTemplate";

vi.mock("../../../main/ts/api/developer/aclApi", async () => {
  const actual = await vi.importActual<
    typeof import("../../../main/ts/api/developer/aclApi")
  >("../../../main/ts/api/developer/aclApi");
  return {
    ...actual,
    getAclForObject: vi.fn(),
    saveObjectAcl: vi.fn().mockResolvedValue(undefined),
    createObjectAcl: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/preferencesApi", () => ({
  loadDefaultAclTemplate: vi.fn(),
  saveDefaultAclTemplate: vi.fn(),
}));

vi.mock("../../../main/ts/i18n/message", () => ({
  message: (key: string) => key,
}));

describe("ObjectAclSection create + default template apply", () => {
  beforeEach(() => {
    vi.mocked(getAclForObject).mockRejectedValue({ status: 404, statusText: "Not Found", body: null });
    vi.mocked(createObjectAcl).mockImplementation(async (_guid, owner) => ({
      id: 50,
      name: "created-acl",
      guid: { stringValue: "0-4-50", uuid: 50 },
      aclEntries: [
        {
          id: 500,
          name: owner.name,
          type: { type: owner.type, name: owner.name },
          permissions: [{ permission: "OWNER" }],
        },
      ],
    }));
    vi.mocked(loadDefaultAclTemplate).mockResolvedValue({
      template: systemDefaultAclTemplate(),
      fromPreference: true,
    });
    vi.mocked(saveObjectAcl).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("merges default template onto created ACL and saves bulk", async () => {
    const mergedAfterSave = {
      id: 50,
      name: "created-acl",
      guid: { stringValue: "0-4-50", uuid: 50 },
      aclEntries: [
        {
          id: 500,
          name: "admin",
          type: { type: "USER", name: "admin" },
          permissions: [{ permission: "OWNER" }],
        },
        {
          id: 501,
          name: "Default",
          type: { type: "USER", name: "Default" },
          permissions: [
            { permission: "READ" },
            { permission: "UPDATE" },
            { permission: "DELETE" },
            { permission: "OWNER" },
          ],
        },
        {
          id: 502,
          name: "AnyCommunity",
          type: { type: "COMMUNITY", name: "AnyCommunity" },
          permissions: [{ permission: "RUNTIME_VISIBLE" }],
        },
      ],
    };
    // First call 404 (missing); after save, reload returns merged
    vi.mocked(getAclForObject)
      .mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: null })
      .mockResolvedValueOnce(mergedAfterSave);

    render(<ObjectAclSection objectGuid="0-2-301" testIdPrefix="developer-acl" />);

    await waitFor(() => {
      expect(screen.getByTestId("developer-acl-empty")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("developer-acl-owner-name"), {
      target: { value: "admin" },
    });
    fireEvent.click(screen.getByTestId("developer-acl-create"));

    await waitFor(() => {
      expect(createObjectAcl).toHaveBeenCalledWith("0-2-301", {
        name: "admin",
        type: "USER",
      });
    });
    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    const saved = vi.mocked(saveObjectAcl).mock.calls[0][0];
    const names = (saved.aclEntries as { name?: string }[]).map((e) => e.name);
    expect(names).toEqual(["admin", "Default", "AnyCommunity"]);

    await waitFor(() => {
      expect(screen.getByTestId("developer-acl-notice").textContent).toMatch(
        /template applied/i,
      );
    });
    // Specials render via DEV_MSG labels (i18n fallback includes key@English).
    expect(screen.getByTestId("developer-acl-special-badge-default")).toBeTruthy();
    expect(screen.getByTestId("developer-acl-special-badge-any-community")).toBeTruthy();
    expect(screen.getByTestId("developer-acl-label-id:501").textContent).toMatch(
      /Default/i,
    );
    expect(screen.getByTestId("developer-acl-label-id:502").textContent).toMatch(
      /Any community/i,
    );
  });

  it("still creates ACL when template load fails (best-effort)", async () => {
    vi.mocked(loadDefaultAclTemplate).mockRejectedValue({
      status: 500,
      statusText: "err",
      body: null,
    });

    render(<ObjectAclSection objectGuid="0-2-302" testIdPrefix="developer-acl" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-acl-empty")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-acl-owner-name"), {
      target: { value: "admin" },
    });
    fireEvent.click(screen.getByTestId("developer-acl-create"));

    await waitFor(() => {
      expect(createObjectAcl).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-acl-notice").textContent).toMatch(/saved/i);
    });
    expect(screen.getByTestId("developer-acl-error").textContent).toMatch(
      /could not apply/i,
    );
    expect(saveObjectAcl).not.toHaveBeenCalled();
  });

  /**
   * B4 peer mount evidence: default-template apply path works when objectKind is a
   * runtime-relevant design-object peer (display-format), not only content-type/template.
   */
  it("applies default template for display-format peer kind with runtime columns", async () => {
    const mergedAfterSave = {
      id: 60,
      name: "df-acl",
      guid: { stringValue: "0-4-60", uuid: 60 },
      aclEntries: [
        {
          id: 600,
          name: "admin",
          type: { type: "USER", name: "admin" },
          permissions: [{ permission: "OWNER" }],
        },
        {
          id: 601,
          name: "Default",
          type: { type: "USER", name: "Default" },
          permissions: [
            { permission: "READ" },
            { permission: "UPDATE" },
            { permission: "DELETE" },
            { permission: "OWNER" },
          ],
        },
        {
          id: 602,
          name: "AnyCommunity",
          type: { type: "COMMUNITY", name: "AnyCommunity" },
          permissions: [{ permission: "RUNTIME_VISIBLE" }],
        },
      ],
    };
    vi.mocked(getAclForObject)
      .mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: null })
      .mockResolvedValueOnce(mergedAfterSave);
    vi.mocked(createObjectAcl).mockImplementation(async (_guid, owner) => ({
      id: 60,
      name: "df-acl",
      guid: { stringValue: "0-4-60", uuid: 60 },
      aclEntries: [
        {
          id: 600,
          name: owner.name,
          type: { type: owner.type, name: owner.name },
          permissions: [{ permission: "OWNER" }],
        },
      ],
    }));

    render(
      <ObjectAclSection
        objectGuid="0-1-100"
        objectKind="display-format"
        testIdPrefix="developer-df-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-empty")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-df-acl-owner-name"), {
      target: { value: "admin" },
    });
    fireEvent.click(screen.getByTestId("developer-df-acl-create"));

    await waitFor(() => {
      expect(createObjectAcl).toHaveBeenCalledWith("0-1-100", {
        name: "admin",
        type: "USER",
      });
    });
    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-df-acl-notice").textContent).toMatch(
        /template applied/i,
      );
    });
    const table = screen.getByTestId("developer-df-acl-table");
    expect(table.getAttribute("data-acl-object-kind")).toBe("display-format");
    expect(table.getAttribute("data-acl-show-runtime")).toBe("true");
    expect(screen.getByTestId("developer-df-acl-special-badge-default")).toBeTruthy();
    expect(screen.getByTestId("developer-df-acl-special-badge-any-community")).toBeTruthy();
  });
});
