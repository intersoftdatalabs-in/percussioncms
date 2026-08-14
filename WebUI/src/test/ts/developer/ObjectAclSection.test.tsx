/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ObjectAcl } from "../../../main/ts/api/developer/types";
import { ObjectAclSection } from "../../../main/ts/developer/ObjectAclSection";

const getAclForObject = vi.fn();
const saveObjectAcl = vi.fn();
const createObjectAcl = vi.fn();

vi.mock("../../../main/ts/api/developer/aclApi", async () => {
  const actual = await vi.importActual<
    typeof import("../../../main/ts/api/developer/aclApi")
  >("../../../main/ts/api/developer/aclApi");
  return {
    ...actual,
    getAclForObject: (...args: unknown[]) => getAclForObject(...args),
    saveObjectAcl: (...args: unknown[]) => saveObjectAcl(...args),
    createObjectAcl: (...args: unknown[]) => createObjectAcl(...args),
  };
});

const aclWithDefaultOnly: ObjectAcl = {
  id: 1,
  name: "object-acl",
  guid: { stringValue: "0-4-1" },
  aclEntries: [
    {
      id: 10,
      name: "Default",
      type: { type: "USER", name: "Default" },
      permissions: [{ permission: "READ" }, { permission: "UPDATE" }],
    },
    {
      id: 11,
      name: "Admin",
      type: { type: "ROLE", name: "Admin" },
      permissions: [{ permission: "OWNER" }],
    },
  ],
};

const aclWithBothSpecials: ObjectAcl = {
  id: 2,
  name: "full-acl",
  guid: { stringValue: "0-4-2" },
  aclEntries: [
    {
      id: 20,
      name: "Admin",
      type: { type: "ROLE", name: "Admin" },
      permissions: [{ permission: "OWNER" }],
    },
    {
      id: 21,
      name: "AnyCommunity",
      type: { type: "COMMUNITY", name: "AnyCommunity" },
      permissions: [{ permission: "RUNTIME_VISIBLE" }],
    },
    {
      id: 22,
      name: "Default",
      type: { type: "USER", name: "Default" },
      permissions: [{ permission: "READ" }],
    },
  ],
};

describe("ObjectAclSection special Default / AnyCommunity UX", () => {
  beforeEach(() => {
    getAclForObject.mockReset();
    saveObjectAcl.mockReset();
    createObjectAcl.mockReset();
    saveObjectAcl.mockResolvedValue(undefined);
  });

  it("labels Default as protected and hides remove; offers Add AnyCommunity when missing", async () => {
    getAclForObject.mockResolvedValue(aclWithDefaultOnly);
    render(
      <ObjectAclSection
        objectGuid="0-2-301"
        objectKind="content-type"
        testIdPrefix="t-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-table")).toBeTruthy();
    });

    expect(screen.getByTestId("t-acl-special-hint")).toBeTruthy();
    expect(screen.getByTestId("t-acl-label-id:10").textContent).toMatch(/Default/i);
    expect(screen.getByTestId("t-acl-special-badge-default")).toBeTruthy();
    expect(screen.getByTestId("t-acl-type-id:10").textContent).toMatch(/USER/i);
    expect(screen.getByTestId("t-acl-protected-id:10")).toBeTruthy();
    expect(screen.queryByTestId("t-acl-remove-id:10")).toBeNull();

    // Ordinary Admin remains removable
    expect(screen.getByTestId("t-acl-remove-id:11")).toBeTruthy();

    // AnyCommunity missing → add action present
    expect(screen.getByTestId("t-acl-special-actions")).toBeTruthy();
    expect(screen.getByTestId("t-acl-add-any-community")).toBeTruthy();
    expect(screen.queryByTestId("t-acl-add-default")).toBeNull();
  });

  it("shows specials and runtime columns for display-format peer kind (B4)", async () => {
    getAclForObject.mockResolvedValue(aclWithBothSpecials);
    render(
      <ObjectAclSection
        objectGuid="0-1-100"
        objectKind="display-format"
        testIdPrefix="df-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("df-acl-table")).toBeTruthy();
    });
    expect(screen.getByTestId("df-acl-table").getAttribute("data-acl-object-kind")).toBe(
      "display-format",
    );
    expect(screen.getByTestId("df-acl-table").getAttribute("data-acl-show-runtime")).toBe(
      "true",
    );
    expect(screen.getByTestId("df-acl-special-badge-default")).toBeTruthy();
    expect(screen.getByTestId("df-acl-special-badge-any-community")).toBeTruthy();
    expect(screen.getByTestId("df-acl-layer-runtime")).toBeTruthy();
  });

  it("exposes kind-aware data-acl attrs on no-guid peer shell (B4 product path #2642)", () => {
    // Runtime-relevant peer (site): show-runtime true even when guid missing
    const { unmount } = render(
      <ObjectAclSection
        objectGuid={null}
        objectKind="site"
        testIdPrefix="developer-site-acl"
      />,
    );
    const siteSection = screen.getByTestId("developer-site-acl-section");
    expect(siteSection.getAttribute("data-acl-object-kind")).toBe("site");
    expect(siteSection.getAttribute("data-acl-show-runtime")).toBe("true");
    expect(siteSection.getAttribute("data-acl-has-guid")).toBe("false");
    expect(screen.getByTestId("developer-site-acl-no-guid").textContent).toMatch(
      /This site has no object GUID|cannot load ACL/i,
    );
    unmount();

    // Non-runtime kind (keyword): hide runtime columns unless force-show
    render(
      <ObjectAclSection
        objectGuid={undefined}
        objectKind="keyword"
        testIdPrefix="developer-kw-acl"
      />,
    );
    const kwSection = screen.getByTestId("developer-kw-acl-section");
    expect(kwSection.getAttribute("data-acl-object-kind")).toBe("keyword");
    expect(kwSection.getAttribute("data-acl-show-runtime")).toBe("false");
    expect(kwSection.getAttribute("data-acl-has-guid")).toBe("false");
    expect(screen.getByTestId("developer-kw-acl-no-guid").textContent).toMatch(
      /Object GUID not available|cannot load ACL/i,
    );
  });

  it("shows existing no-GUID message for action-menu and view (#3380)", () => {
    const { unmount } = render(
      <ObjectAclSection
        objectGuid={null}
        objectKind="action-menu"
        testIdPrefix="developer-am-acl"
      />,
    );
    expect(screen.getByTestId("developer-am-acl-no-guid").textContent).toMatch(
      /Object GUID not available|cannot load ACL/i,
    );
    unmount();

    render(
      <ObjectAclSection
        objectGuid={undefined}
        objectKind="view"
        testIdPrefix="developer-vw-acl"
      />,
    );
    expect(screen.getByTestId("developer-vw-acl-no-guid").textContent).toMatch(
      /Object GUID not available|cannot load ACL/i,
    );
  });

  it("uses display-format kind-aware no-guid copy (#3203)", () => {
    render(
      <ObjectAclSection
        objectGuid={null}
        objectKind="display-format"
        testIdPrefix="developer-df-acl"
      />,
    );
    const section = screen.getByTestId("developer-df-acl-section");
    expect(section.getAttribute("data-acl-object-kind")).toBe("display-format");
    expect(section.getAttribute("data-acl-show-runtime")).toBe("true");
    expect(screen.getByTestId("developer-df-acl-no-guid").textContent).toMatch(
      /This display format has no object GUID|cannot load ACL/i,
    );
  });

  it("groups permission columns under Design access and Runtime visibility (CD-19)", async () => {
    getAclForObject.mockResolvedValue(aclWithBothSpecials);
    render(
      <ObjectAclSection
        objectGuid="0-2-301"
        objectKind="content-type"
        testIdPrefix="t-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-table")).toBeTruthy();
    });

    const table = screen.getByTestId("t-acl-table");
    expect(table.getAttribute("data-acl-show-runtime")).toBe("true");
    expect(table.getAttribute("data-acl-object-kind")).toBe("content-type");

    expect(screen.getByTestId("t-acl-layer-headers")).toBeTruthy();
    expect(screen.getByTestId("t-acl-layer-design").textContent).toMatch(/Design access/i);
    expect(screen.getByTestId("t-acl-layer-runtime").textContent).toMatch(
      /Runtime visibility/i,
    );

    // Design: Read, Update, Delete, Modify ACL; Runtime: Visible
    expect(screen.getByTestId("t-acl-perm-header-READ").textContent).toMatch(/Read/i);
    expect(screen.getByTestId("t-acl-perm-header-OWNER").textContent).toMatch(/Modify ACL/i);
    expect(screen.getByTestId("t-acl-perm-header-RUNTIME_VISIBLE").textContent).toMatch(
      /Visible/i,
    );

    // Runtime checkbox still toggles for AnyCommunity
    const rt = screen.getByTestId("t-acl-perm-id:21-RUNTIME_VISIBLE") as HTMLInputElement;
    expect(rt.checked).toBe(true);
    fireEvent.click(rt);
    expect(rt.checked).toBe(false);
  });

  it("hides Runtime visibility columns for non-runtime-relevant object kinds", async () => {
    getAclForObject.mockResolvedValue(aclWithDefaultOnly);
    render(
      <ObjectAclSection
        objectGuid="0-2-301"
        objectKind="keyword"
        testIdPrefix="t-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-table")).toBeTruthy();
    });

    expect(screen.getByTestId("t-acl-table").getAttribute("data-acl-show-runtime")).toBe(
      "false",
    );
    expect(screen.getByTestId("t-acl-layer-design")).toBeTruthy();
    expect(screen.queryByTestId("t-acl-layer-runtime")).toBeNull();
    expect(screen.queryByTestId("t-acl-perm-header-RUNTIME_VISIBLE")).toBeNull();
    expect(screen.getByTestId("t-acl-perm-header-READ")).toBeTruthy();
    expect(screen.getByTestId("t-acl-perm-header-OWNER")).toBeTruthy();
  });

  it("orders Default and AnyCommunity first and blocks remove on both", async () => {
    getAclForObject.mockResolvedValue(aclWithBothSpecials);
    render(
      <ObjectAclSection
        objectGuid="0-2-301"
        objectKind="content-type"
        testIdPrefix="t-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-table")).toBeTruthy();
    });

    const rows = screen.getAllByTestId(/^t-acl-row-/);
    expect(rows[0].getAttribute("data-special-acl")).toBe("default");
    expect(rows[1].getAttribute("data-special-acl")).toBe("any-community");
    expect(rows[2].getAttribute("data-special-acl")).toBeNull();

    expect(screen.getByTestId("t-acl-label-id:22").textContent).toMatch(/Default/i);
    expect(screen.getByTestId("t-acl-label-id:21").textContent).toMatch(/Any community/i);
    expect(screen.getByTestId("t-acl-type-id:21").textContent).toMatch(/COMMUNITY/i);
    expect(screen.queryByTestId("t-acl-remove-id:22")).toBeNull();
    expect(screen.queryByTestId("t-acl-remove-id:21")).toBeNull();
    expect(screen.queryByTestId("t-acl-special-actions")).toBeNull();
  });

  it("adds missing AnyCommunity via special action with correct REST shape on save", async () => {
    getAclForObject.mockResolvedValue(aclWithDefaultOnly);
    render(
      <ObjectAclSection
        objectGuid="0-2-301"
        objectKind="content-type"
        testIdPrefix="t-acl"
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-add-any-community")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("t-acl-add-any-community"));

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-row-__new:1")).toBeTruthy();
    });
    expect(screen.getByTestId("t-acl-row-__new:1").getAttribute("data-special-acl")).toBe(
      "any-community",
    );
    expect(screen.getByTestId("t-acl-label-__new:1").textContent).toMatch(/Any community/i);
    expect(screen.queryByTestId("t-acl-remove-__new:1")).toBeNull();

    // Permission toggle on special entry still works
    const rt = screen.getByTestId("t-acl-perm-__new:1-RUNTIME_VISIBLE");
    fireEvent.click(rt);
    expect((rt as HTMLInputElement).checked).toBe(true);

    fireEvent.click(screen.getByTestId("t-acl-save"));
    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    const payload = saveObjectAcl.mock.calls.at(-1)?.[0] as ObjectAcl;
    const entries = Array.isArray(payload.aclEntries) ? payload.aclEntries : [];
    const any = entries.find(
      (e) => (e.name || e.principal?.name) === "AnyCommunity",
    );
    expect(any).toBeTruthy();
    expect(any?.type?.type).toBe("COMMUNITY");
    expect(any?.principal?.type).toBe("COMMUNITY");
    const perms = (Array.isArray(any?.permissions) ? any!.permissions : []).map(
      (p) => p.permission,
    );
    expect(perms).toEqual(expect.arrayContaining(["READ", "RUNTIME_VISIBLE"]));
  });

  it("coerces typed name Default to USER special and rejects second Default", async () => {
    getAclForObject.mockResolvedValue({
      id: 3,
      name: "emptyish",
      aclEntries: [
        {
          id: 30,
          name: "Admin",
          type: { type: "ROLE", name: "Admin" },
          permissions: [{ permission: "OWNER" }],
        },
      ],
    });
    render(<ObjectAclSection objectGuid="0-2-301" testIdPrefix="t-acl" />);

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-add-default")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("t-acl-add-name"), {
      target: { value: "Default" },
    });
    // Even if user picks ROLE, special name coerces to USER
    fireEvent.change(screen.getByTestId("t-acl-add-type"), {
      target: { value: "ROLE" },
    });
    fireEvent.click(screen.getByTestId("t-acl-add"));

    await waitFor(() => {
      expect(screen.getByTestId("t-acl-row-__new:1").getAttribute("data-special-acl")).toBe(
        "default",
      );
    });

    fireEvent.change(screen.getByTestId("t-acl-add-name"), {
      target: { value: "Default" },
    });
    fireEvent.click(screen.getByTestId("t-acl-add"));
    await waitFor(() => {
      expect(screen.getByTestId("t-acl-error").textContent).toMatch(/already exists/i);
    });
  });

  it("does not remove protected Default via removeEntry even if control forced", async () => {
    getAclForObject.mockResolvedValue(aclWithDefaultOnly);
    render(<ObjectAclSection objectGuid="0-2-301" testIdPrefix="t-acl" />);
    await waitFor(() => {
      expect(screen.getByTestId("t-acl-row-id:10")).toBeTruthy();
    });
    // No remove control; Admin remove still works
    fireEvent.click(screen.getByTestId("t-acl-remove-id:11"));
    expect(screen.queryByTestId("t-acl-row-id:11")).toBeNull();
    expect(screen.getByTestId("t-acl-row-id:10")).toBeTruthy();
  });

  it("coerces mis-typed Default ROLE payload to USER on save", async () => {
    getAclForObject.mockResolvedValue({
      id: 9,
      name: "legacy-acl",
      aclEntries: [
        {
          id: 90,
          name: "Default",
          type: { type: "ROLE", name: "Default" },
          permissions: [{ permission: "READ" }],
        },
      ],
    });
    render(<ObjectAclSection objectGuid="0-2-301" testIdPrefix="t-acl" />);
    await waitFor(() => {
      expect(screen.getByTestId("t-acl-row-id:90")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("t-acl-perm-id:90-UPDATE"));
    fireEvent.click(screen.getByTestId("t-acl-save"));
    await waitFor(() => {
      expect(saveObjectAcl).toHaveBeenCalled();
    });
    const payload = saveObjectAcl.mock.calls.at(-1)?.[0] as ObjectAcl;
    const entries = Array.isArray(payload.aclEntries) ? payload.aclEntries : [];
    const d = entries.find((e) => e.name === "Default");
    expect(d?.type?.type).toBe("USER");
    expect(d?.principal?.type).toBe("USER");
  });
});
