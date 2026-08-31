/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import { ContentTypesPanel } from "../../../main/ts/developer/ContentTypesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypesApi")>();
  return {
    ...actual,
    listContentTypes: vi.fn(),
    getContentTypeDetail: vi.fn().mockResolvedValue({
      name: "percPage",
      label: "Page",
      guid: { stringValue: "0-2-301" },
      fields: [],
    }),
    getContentTypeItemExits: vi.fn().mockResolvedValue({
      inputTranslations: [],
      outputTranslations: [],
      validations: [],
      preExits: [],
      postExits: [],
    }),
    getContentTypeAllowedTemplates: vi.fn().mockResolvedValue([]),
    getFieldControlProperties: vi.fn().mockResolvedValue({ properties: [] }),
    createContentType: vi.fn(),
    deleteContentType: vi.fn(),
    lockContentType: vi.fn().mockResolvedValue({ locker: "Admin", remainingTime: 30 }),
    unlockContentType: vi.fn().mockResolvedValue(undefined),
    includeContentTypeField: vi.fn(),
    getContentTypeIcon: vi.fn().mockResolvedValue({ source: "none" }),
    setContentTypeIcon: vi.fn().mockResolvedValue({ source: "none" }),
  };
});

vi.mock("../../../main/ts/api/developer/contentTypeFieldRules", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypeFieldRules")>();
  return {
    ...actual,
    getContentTypeFieldRuleExpressions: vi.fn().mockResolvedValue({
      fieldName: "",
      validation: [],
      visibility: [],
      inputTranslation: [],
      outputTranslation: [],
    }),
  };
});

vi.mock("../../../main/ts/developer/ObjectAclSection", () => ({
  ObjectAclSection: (props: {
    objectGuid?: string | null;
    objectKind?: string | null;
    testIdPrefix?: string;
  }) => (
    <div
      data-testid={`${props.testIdPrefix ?? "developer-acl"}-stub`}
      data-object-guid={props.objectGuid ?? ""}
      data-object-kind={props.objectKind ?? ""}
    />
  ),
}));

const listContentTypes = contentTypesApi.listContentTypes as ReturnType<typeof vi.fn>;
const createContentType = contentTypesApi.createContentType as ReturnType<typeof vi.fn>;
const getContentTypeDetail = contentTypesApi.getContentTypeDetail as ReturnType<typeof vi.fn>;

describe("ContentTypesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listContentTypes.mockReset();
    createContentType.mockReset();
  });

  it("lists content types on success", async () => {
    listContentTypes.mockResolvedValue([
      {
        name: "percPage",
        label: "Page",
        description: "Page type",
        guid: { stringValue: "0-1-2", longValue: 2 },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("Page");
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("percPage");
  });

  it("shows empty state when API returns no content types", async () => {
    listContentTypes.mockResolvedValue([]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-new")).toBeTruthy();
  });

  it("opens create chrome from New content type", async () => {
    listContentTypes.mockResolvedValue([]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-new"));
    expect(screen.getByTestId("developer-ct-create")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-create-save")).toBeDisabled();
  });

  it("create opens the new type detail", async () => {
    listContentTypes.mockResolvedValue([]);
    createContentType.mockResolvedValue({
      name: "qaType",
      label: "QA",
      guid: { stringValue: "0-2-99" },
    });
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-new"));
    fireEvent.change(screen.getByTestId("developer-ct-create-name"), {
      target: { value: "qaType" },
    });
    fireEvent.click(screen.getByTestId("developer-ct-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail")).toBeTruthy();
    });
    expect(createContentType).toHaveBeenCalledWith(
      expect.objectContaining({ name: "qaType" }),
    );
    expect(getContentTypeDetail).toHaveBeenCalledWith("qaType");
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue(new SessionRedirectError());
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-ct-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(
      `${DEV_MSG.CT_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listContentTypes.mockRejectedValue(new Error("network down"));
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(
      `${DEV_MSG.CT_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-ct-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listContentTypes.mockRejectedValue("boom");
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-error").textContent).toBe(DEV_MSG.CT_ERROR);
  });

  it("unwraps ContentTypeList envelope instead of throwing into the section boundary (#3706)", async () => {
    listContentTypes.mockResolvedValue({
      ContentTypeList: {
        ContentType: [
          {
            name: "percPage",
            label: "Page",
            description: "Page type",
            guid: { stringValue: "0-2-301", type: 2, uuid: 301 },
          },
        ],
      },
    });
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("Page");
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("percPage");
    expect(screen.queryByTestId("developer-section-error")).toBeNull();
    expect(screen.queryByText(/Unable to load/i)).toBeNull();
  });

  it("does not crash when a row label is a non-string object (#3706)", async () => {
    listContentTypes.mockResolvedValue([
      {
        name: "percPage",
        label: { value: "Page" },
        description: { $: "nested" },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("percPage");
    expect(screen.queryByTestId("developer-section-error")).toBeNull();
  });

  it("shows empty state for empty-collection beans instead of throwing (#3706)", async () => {
    listContentTypes.mockResolvedValue({ empty: true });
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-empty")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-section-error")).toBeNull();
  });

  it("exposes Open when name is a JAXB wrap and guid is present (#3810)", async () => {
    listContentTypes.mockResolvedValue([
      {
        name: { value: "percPage" },
        label: { $: "Page" },
        guid: { hostId: 0, type: 2, uuid: 301 },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-open")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-open").getAttribute("data-ct-name")).toBe(
      "percPage",
    );
    expect(screen.getByTestId("developer-ct-open").getAttribute("aria-label")).toMatch(
      /Open Page/i,
    );
  });

  it("exposes Open from guid when name is empty (#3810)", async () => {
    listContentTypes.mockResolvedValue([
      {
        label: "Untitled",
        guid: { stringValue: "0-2-9" },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-open")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-open").getAttribute("data-ct-name")).toBe(
      "0-2-9",
    );
  });

  it("hides Open when name and guid are both missing (#3810)", async () => {
    listContentTypes.mockResolvedValue([{ label: "Broken" }]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-table")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-ct-open")).toBeNull();
    expect(screen.getByTestId("developer-ct-table").textContent).toContain("Broken");
  });

  it("Open mounts content-type detail and Object ACL (#3810)", async () => {
    listContentTypes.mockResolvedValue([
      {
        name: "percPage",
        label: "Page",
        guid: { stringValue: "0-2-301" },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-open")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-ct-open"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-detail")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-acl-stub")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-guid")).toBe(
      "0-2-301",
    );
    expect(screen.getByTestId("developer-ct-acl-stub").getAttribute("data-object-kind")).toBe(
      "content-type",
    );
  });

  it("shows import wizard on the catalog (#4034)", async () => {
    listContentTypes.mockResolvedValue([
      {
        name: "percPage",
        label: "Page",
        guid: { stringValue: "0-2-301" },
      },
    ]);
    render(<ContentTypesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-import")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-import-file")).toBeTruthy();
    expect(screen.getByTestId("developer-ct-import-submit")).toBeTruthy();
  });
});
