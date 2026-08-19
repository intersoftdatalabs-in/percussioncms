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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  PSFolderPermission,
  PSFolderProperties,
} from "../../../main/ts/api/contentExplorer/types";
import { FolderAclDialog } from "../../../main/ts/architecture/FolderAclDialog";

function permission(
  write: string[] = [],
): PSFolderPermission {
  return {
    accessLevel: "ADMIN",
    adminPrincipals: [{ type: "USER", name: "Admin" }],
    writePrincipals: write.map((n) => ({ type: "USER", name: n })),
    readPrincipals: [],
    viewPrincipals: [],
  };
}

function folderProps(write: string[] = []): PSFolderProperties {
  return {
    id: "folder-1",
    name: "About",
    permission: permission(write),
  };
}

describe("FolderAclDialog (#3588)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("does not render when closed", () => {
    render(
      <FolderAclDialog
        open={false}
        busy={false}
        folderId="folder-1"
        loadError={null}
        sectionTitle="About"
        currentUserIdentities={["Admin"]}
        onCancel={() => undefined}
      />,
    );
    expect(screen.queryByTestId("architecture-folder-acl-dialog")).toBeNull();
  });

  it("shows loading while the folder id is resolved", () => {
    render(
      <FolderAclDialog
        open
        busy
        folderId={null}
        loadError={null}
        sectionTitle="About"
        currentUserIdentities={["Admin"]}
        onCancel={() => undefined}
      />,
    );
    expect(screen.getByTestId("architecture-folder-acl-loading")).toBeTruthy();
    expect(screen.queryByTestId("folder-security-panel")).toBeNull();
  });

  it("shows a load error and does not mount the panel", () => {
    render(
      <FolderAclDialog
        open
        busy={false}
        folderId={null}
        loadError="Could not resolve the section folder"
        sectionTitle="About"
        currentUserIdentities={["Admin"]}
        onCancel={() => undefined}
      />,
    );
    expect(screen.getByTestId("architecture-folder-acl-load-error")).toBeTruthy();
    expect(screen.queryByTestId("folder-security-panel")).toBeNull();
  });

  it("adds a write principal and saves via pathApi override", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const load = vi.fn().mockResolvedValue(folderProps());
    render(
      <FolderAclDialog
        open
        busy={false}
        folderId="folder-1"
        loadError={null}
        sectionTitle="About"
        currentUserIdentities={["Admin"]}
        onCancel={() => undefined}
        load={load}
        save={save}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("folder-security-panel")).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId("folder-security-list-writePrincipals-add"),
    );
    fireEvent.change(
      screen.getByTestId("folder-security-list-writePrincipals-input"),
      { target: { value: "night3588" } },
    );
    fireEvent.click(
      screen.getByTestId("folder-security-list-writePrincipals-add-confirm"),
    );
    expect(
      screen.getByTestId("folder-security-list-writePrincipals-remove-night3588"),
    ).toBeTruthy();
    fireEvent.click(screen.getByTestId("folder-security-save"));
    await waitFor(() => {
      expect(save).toHaveBeenCalledTimes(1);
    });
    const saved = save.mock.calls[0][0] as PSFolderProperties;
    expect(saved.permission?.writePrincipals?.map((p) => p.name)).toContain(
      "night3588",
    );
  });

  it("removes a write principal and saves", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const load = vi.fn().mockResolvedValue(folderProps(["night3588"]));
    render(
      <FolderAclDialog
        open
        busy={false}
        folderId="folder-1"
        loadError={null}
        sectionTitle="About"
        currentUserIdentities={["Admin"]}
        onCancel={() => undefined}
        load={load}
        save={save}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId(
          "folder-security-list-writePrincipals-remove-night3588",
        ),
      ).toBeTruthy();
    });
    fireEvent.click(
      screen.getByTestId(
        "folder-security-list-writePrincipals-remove-night3588",
      ),
    );
    fireEvent.click(screen.getByTestId("folder-security-save"));
    await waitFor(() => {
      expect(save).toHaveBeenCalledTimes(1);
    });
    const saved = save.mock.calls[0][0] as PSFolderProperties;
    expect(saved.permission?.writePrincipals ?? []).toEqual([]);
  });

  it("cancel does not save", async () => {
    const onCancel = vi.fn();
    const save = vi.fn().mockResolvedValue(undefined);
    const load = vi.fn().mockResolvedValue(folderProps());
    render(
      <FolderAclDialog
        open
        busy={false}
        folderId="folder-1"
        loadError={null}
        sectionTitle="About"
        currentUserIdentities={["Admin"]}
        onCancel={onCancel}
        load={load}
        save={save}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("folder-security-panel")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("architecture-folder-acl-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();
  });
});
