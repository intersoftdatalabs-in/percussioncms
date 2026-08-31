/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as sharedFieldsApi from "../../../main/ts/api/developer/sharedFieldsApi";
import * as systemDefApi from "../../../main/ts/api/developer/systemDefApi";
import { ContentTypeIncludeFieldSection } from "../../../main/ts/developer/ContentTypeIncludeFieldSection";
import { DEV_MSG } from "../../../main/ts/developer/messages";

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

describe("ContentTypeIncludeFieldSection catalog", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    getSystemDef.mockReset();
    listSharedFieldGroups.mockReset();
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
