/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as contentTypesApi from "../../../main/ts/api/developer/contentTypesApi";
import * as sharedFieldsApi from "../../../main/ts/api/developer/sharedFieldsApi";
import * as systemDefApi from "../../../main/ts/api/developer/systemDefApi";
import { ContentTypeIncludeFieldSection } from "../../../main/ts/developer/ContentTypeIncludeFieldSection";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/contentTypesApi")>();
  return { ...actual, includeContentTypeField: vi.fn() };
});

vi.mock("../../../main/ts/api/developer/systemDefApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/systemDefApi")>();
  return { ...actual, getSystemDef: vi.fn() };
});

vi.mock("../../../main/ts/api/developer/sharedFieldsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/sharedFieldsApi")>();
  return {
    ...actual,
    listSharedFieldGroups: vi.fn(),
    getSharedFieldGroupDetail: vi.fn(),
  };
});

const getSystemDef = vi.mocked(systemDefApi.getSystemDef);
const listSharedFieldGroups = vi.mocked(sharedFieldsApi.listSharedFieldGroups);
const getSharedFieldGroupDetail = vi.mocked(sharedFieldsApi.getSharedFieldGroupDetail);
const includeContentTypeField = vi.mocked(contentTypesApi.includeContentTypeField);

describe("ContentTypeIncludeFieldSection catalog", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSystemDef.mockReset();
    listSharedFieldGroups.mockReset();
    getSharedFieldGroupDetail.mockReset();
    includeContentTypeField.mockReset();
  });

  it("surfaces a catalog-load error instead of an empty picker", async () => {
    getSystemDef.mockRejectedValue({ status: 500, statusText: "Error", body: null });
    listSharedFieldGroups.mockResolvedValue([]);
    render(
      <ContentTypeIncludeFieldSection
        idOrName="percPage"
        existingFields={[]}
        canEdit={true}
        onBusy={() => undefined}
        onIncluded={() => undefined}
        onError={() => undefined}
        onNotice={() => undefined}
        onLockLost={() => undefined}
      />,
    );
    expect(screen.getByTestId("developer-ct-include-catalog-loading")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("developer-ct-include-catalog-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-ct-include-catalog-error").textContent).toBe(
      DEV_MSG.CT_INCLUDE_CATALOG_ERROR,
    );
    expect(screen.queryByTestId("developer-ct-include-catalog-loading")).toBeNull();
  });
});

describe("ContentTypeIncludeFieldSection shared include (slice 124)", () => {
  const sharedCatalog = {
    SharedFieldGroupDetail: {
      fields: [{ name: "shared_summary", label: "Summary" }],
    },
  };

  function mockSharedCatalog() {
    getSystemDef.mockResolvedValue({
      SystemDefDetail: { fields: [{ name: "sys_title" }] },
    });
    listSharedFieldGroups.mockResolvedValue([{ name: "seo" }]);
    getSharedFieldGroupDetail.mockResolvedValue(sharedCatalog);
  }

  function renderSection(overrides: {
    onIncluded?: (detail: unknown) => void;
    onError?: (message: string | null) => void;
    onNotice?: (message: string | null) => void;
    onLockLost?: () => void;
  } = {}) {
    const props = {
      onIncluded: vi.fn(),
      onError: vi.fn(),
      onNotice: vi.fn(),
      onLockLost: vi.fn(),
      ...overrides,
    };
    render(
      <ContentTypeIncludeFieldSection
        idOrName="percPage"
        existingFields={[]}
        canEdit={true}
        onBusy={() => undefined}
        onIncluded={props.onIncluded as (detail: never) => void}
        onError={props.onError}
        onNotice={props.onNotice}
        onLockLost={props.onLockLost}
      />,
    );
    return props;
  }

  async function pickSharedSummary() {
    fireEvent.change(screen.getByTestId("developer-ct-include-origin"), {
      target: { value: "shared" },
    });
    const pick = screen.getByTestId("developer-ct-include-pick");
    await waitFor(() => {
      expect(
        Array.from((pick as HTMLSelectElement).options).some(
          (o) => o.value === "shared_summary",
        ),
      ).toBe(true);
    });
    fireEvent.change(pick, { target: { value: "shared_summary" } });
  }

  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSystemDef.mockReset();
    listSharedFieldGroups.mockReset();
    getSharedFieldGroupDetail.mockReset();
    includeContentTypeField.mockReset();
    mockSharedCatalog();
  });

  it("includes one shared field with origin shared and reports success", async () => {
    const updated = { ContentTypeDetail: { name: "percPage" } };
    includeContentTypeField.mockResolvedValue(updated as never);
    const props = renderSection();

    await pickSharedSummary();
    fireEvent.click(screen.getByTestId("developer-ct-include-submit"));

    await waitFor(() => {
      expect(includeContentTypeField).toHaveBeenCalledWith("percPage", {
        name: "shared_summary",
        fieldType: "shared",
      });
    });
    await waitFor(() => {
      expect(props.onIncluded).toHaveBeenCalledWith(updated);
    });
    expect(props.onNotice).toHaveBeenCalledWith(DEV_MSG.CT_INCLUDED);
    expect(props.onLockLost).not.toHaveBeenCalled();
    expect(props.onError).toHaveBeenCalledWith(null);
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-ct-include-name") as HTMLInputElement).value,
      ).toBe("");
    });
  });

  it("a missing held lock (409) reports the lock lost and claims no success", async () => {
    includeContentTypeField.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "design lock required: lock not held" },
    });
    const props = renderSection();

    await pickSharedSummary();
    fireEvent.click(screen.getByTestId("developer-ct-include-submit"));

    await waitFor(() => {
      expect(includeContentTypeField).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      expect(props.onLockLost).toHaveBeenCalledTimes(1);
    });
    expect(props.onIncluded).not.toHaveBeenCalled();
    expect(props.onNotice).toHaveBeenCalledWith(null);
    expect(props.onNotice.mock.calls.every((call) => call[0] == null)).toBe(true);
    await waitFor(() => {
      expect(props.onError).toHaveBeenCalled();
    });
    const lastError = props.onError.mock.calls[props.onError.mock.calls.length - 1][0];
    expect(typeof lastError).toBe("string");
    expect((lastError as string).length).toBeGreaterThan(0);
  });

  it("cancelling the pick (clearing the name) does not write", async () => {
    const props = renderSection();

    await pickSharedSummary();
    expect(
      (screen.getByTestId("developer-ct-include-submit") as HTMLButtonElement).disabled,
    ).toBe(false);

    fireEvent.change(screen.getByTestId("developer-ct-include-pick"), {
      target: { value: "" },
    });
    await waitFor(() => {
      expect(
        (screen.getByTestId("developer-ct-include-submit") as HTMLButtonElement)
          .disabled,
      ).toBe(true);
    });
    fireEvent.change(screen.getByTestId("developer-ct-include-name"), {
      target: { value: "   " },
    });
    expect(
      (screen.getByTestId("developer-ct-include-submit") as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(includeContentTypeField).not.toHaveBeenCalled();
    expect(props.onIncluded).not.toHaveBeenCalled();
    expect(props.onNotice).not.toHaveBeenCalled();
  });
});
