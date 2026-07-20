/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  PSFolderProperties,
  PSFolderPermission,
} from "../../../main/ts/api/contentExplorer/types";
import { FolderSecurityPanel } from "../../../main/ts/contentExplorer/FolderSecurityPanel";

function permission(
  admin: string[] = [],
  write: string[] = [],
  read: string[] = [],
  view: string[] = [],
  accessLevel: "ADMIN" | "WRITE" | "READ" | "VIEW" = "ADMIN",
): PSFolderPermission {
  return {
    accessLevel,
    adminPrincipals: admin.map((n) => ({ type: "USER", name: n })),
    writePrincipals: write.map((n) => ({ type: "USER", name: n })),
    readPrincipals: read.map((n) => ({ type: "USER", name: n })),
    viewPrincipals: view.map((n) => ({ type: "USER", name: n })),
  };
}

function makeProps(overrides: PSFolderProperties): PSFolderProperties {
  return { id: "f-1", name: "Sites", ...overrides };
}

let confirmSpy: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
});

afterEach(() => {
  confirmSpy.mockRestore();
  vi.restoreAllMocks();
});

describe("FolderSecurityPanel", () => {
  it("renders the read-only state when accessLevel is READ", () => {
    const props = makeProps({ permission: permission([], [], [], [], "READ") });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    expect(screen.getByTestId("folder-security-readonly")).toBeTruthy();
    // Save button is present but disabled.
    const save = screen.getByTestId("folder-security-save");
    expect(save).toBeTruthy();
    expect((save as HTMLButtonElement).disabled).toBe(true);
  });

  it("renders the access-denied state when accessLevel is VIEW", () => {
    const props = makeProps({ permission: permission([], [], [], [], "VIEW") });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    expect(screen.getByTestId("folder-security-no-access")).toBeTruthy();
  });

  it("admin sees an enabled save button (no changes = dirty=false = disabled)", () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    const save = screen.getByTestId("folder-security-save");
    expect(save).toBeTruthy();
    expect((save as HTMLButtonElement).disabled).toBe(true);
  });

  it("remove + add cycles flip the dirty indicator", () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    const dirtyInitial = screen.getByTestId("folder-security-dirty");
    expect(dirtyInitial.textContent).toBe("○");
    // Remove Admin from adminPrincipals: flips dirty.
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-remove-Admin"),
    );
    const dirtyAfter = screen.getByTestId("folder-security-dirty");
    expect(dirtyAfter.textContent).toBe("●");
    // The lockout warning is only shown when Save is clicked — see the
    // self-lockout tests below.
  });

  it("self-lockout warning is shown when the current user is removed from a level; save is aborted when confirm returns false", async () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    confirmSpy.mockReturnValue(false);
    const saveMock = vi.fn().mockResolvedValue(undefined);
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
        save={saveMock}
      />,
    );
    // Remove Admin from adminPrincipals.
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-remove-Admin"),
    );
    // Save triggers the lockout check → window.confirm() called.
    fireEvent.click(screen.getByTestId("folder-security-save"));
    await waitFor(() => {
      expect(confirmSpy).toHaveBeenCalledTimes(1);
    });
    // Cancel path: save not called.
    expect(saveMock).not.toHaveBeenCalled();
  });

  it("self-lockout warning allow path: confirm=true proceeds with the save", async () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    confirmSpy.mockReturnValue(true);
    const saveMock = vi.fn().mockResolvedValue(undefined);
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
        save={saveMock}
      />,
    );
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-remove-Admin"),
    );
    fireEvent.click(screen.getByTestId("folder-security-save"));
    await waitFor(() => {
      expect(confirmSpy).toHaveBeenCalledTimes(1);
      expect(saveMock).toHaveBeenCalledTimes(1);
    });
  });

  it("add-principal flow: typing + confirming appends to the list", () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    // Click the Add button on the admin list.
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-add"),
    );
    const input = screen.getByTestId(
      "folder-security-list-adminPrincipals-input",
    ) as HTMLInputElement;
    fireEvent.change(input, { target: { value: "Contributor" } });
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-add-confirm"),
    );
    expect(
      screen.getByTestId(
        "folder-security-list-adminPrincipals-remove-Contributor",
      ),
    ).toBeTruthy();
  });

  it("add-principal flow: empty input does NOT append", () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-add"),
    );
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-add-confirm"),
    );
    // No new row added.
    expect(
      screen.queryByTestId(
        "folder-security-list-adminPrincipals-remove-",
      ),
    ).toBeNull();
  });

  it("add-principal flow: duplicate name + type does NOT duplicate", () => {
    const props = makeProps({ permission: permission(["Admin"], [], [], []) });
    render(
      <FolderSecurityPanel
        folderId={props.id}
        currentUserIdentities={["Admin"]}
        initial={props}
      />,
    );
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-add"),
    );
    const input = screen.getByTestId(
      "folder-security-list-adminPrincipals-input",
    ) as HTMLInputElement;
    fireEvent.change(input, { target: { value: "Admin" } });
    fireEvent.click(
      screen.getByTestId("folder-security-list-adminPrincipals-add-confirm"),
    );
    // Only one Admin remove button still present.
    expect(
      screen.getAllByTestId("folder-security-list-adminPrincipals-remove-Admin")
        .length,
    ).toBe(1);
  });

  it("renders the loading state when no initial props are supplied and the load is in-flight", () => {
    const slowLoad = () => new Promise<PSFolderProperties>(() => {});
    render(
      <FolderSecurityPanel
        folderId="f-1"
        currentUserIdentities={["Admin"]}
        load={slowLoad}
      />,
    );
    expect(screen.getByTestId("folder-security-loading")).toBeTruthy();
  });

  it("renders the error state with retry button when load rejects", async () => {
    const fail = vi.fn().mockRejectedValue(new Error("boom"));
    render(
      <FolderSecurityPanel
        folderId="f-1"
        currentUserIdentities={["Admin"]}
        load={fail}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("folder-security-error")).toBeTruthy();
    });
    expect(screen.getByTestId("folder-security-retry")).toBeTruthy();
  });
});
