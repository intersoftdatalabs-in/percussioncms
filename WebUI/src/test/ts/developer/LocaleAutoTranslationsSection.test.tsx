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
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { listCommunities } from "../../../main/ts/api/developer/assemblyApi";
import {
  listAutoTranslations,
  saveAutoTranslations,
} from "../../../main/ts/api/developer/autoTranslationsApi";
import { listContentTypes } from "../../../main/ts/api/developer/contentTypesApi";
import { listWorkflows } from "../../../main/ts/api/developer/workflowsApi";
import { LocaleAutoTranslationsSection } from "../../../main/ts/developer/LocaleAutoTranslationsSection";

vi.mock("../../../main/ts/api/developer/autoTranslationsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/autoTranslationsApi")
  >();
  return {
    ...actual,
    listAutoTranslations: vi.fn(),
    saveAutoTranslations: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/contentTypesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/contentTypesApi")
  >();
  return {
    ...actual,
    listContentTypes: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/workflowsApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/workflowsApi")
  >();
  return {
    ...actual,
    listWorkflows: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/developer/assemblyApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/assemblyApi")
  >();
  return {
    ...actual,
    listCommunities: vi.fn(),
  };
});

const listAt = vi.mocked(listAutoTranslations);
const saveAt = vi.mocked(saveAutoTranslations);
const typesMock = vi.mocked(listContentTypes);
const workflowsMock = vi.mocked(listWorkflows);
const communitiesMock = vi.mocked(listCommunities);

const frRow = {
  locale: "fr-ca",
  contentTypeName: "percPage",
  workflowName: "Default Workflow",
  communityName: "Default",
};

const enRow = {
  locale: "en-us",
  contentTypeName: "percPage",
  workflowName: "Default Workflow",
  communityName: "Default",
};

function mockCatalogs(): void {
  typesMock.mockResolvedValue([{ name: "percPage", label: "Page" }]);
  workflowsMock.mockResolvedValue([{ workflowName: "Default Workflow" }]);
  communitiesMock.mockResolvedValue([{ name: "Default", label: "Default" }]);
}

describe("LocaleAutoTranslationsSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listAt.mockReset();
    saveAt.mockReset();
    typesMock.mockReset();
    workflowsMock.mockReset();
    communitiesMock.mockReset();
    mockCatalogs();
  });

  afterEach(() => {
    cleanup();
  });

  it("loads the full set but shows only this locale's rows with a fixed locale column", async () => {
    listAt.mockResolvedValue([enRow, frRow]);
    render(<LocaleAutoTranslationsSection languageString="fr-ca" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-table")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-loc-set-at-row-1")).toBeNull();
    expect(screen.getByTestId("developer-loc-set-at-locale-0").textContent).toBe("fr-ca");
    expect((screen.getByTestId("developer-loc-set-at-type-0") as HTMLInputElement).value).toBe(
      "percPage",
    );
    expect(screen.getByTestId("developer-loc-set-at-text").getAttribute("data-loc-at-count")).toBe(
      "1",
    );
  });

  it("saves replacements merged with the other locales' untouched rows", async () => {
    listAt.mockResolvedValue([enRow, frRow]);
    saveAt.mockImplementation(async (rows) => rows);
    render(<LocaleAutoTranslationsSection languageString="fr-ca" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-row-0")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-loc-set-at-type-0"), {
      target: { value: "percFile" },
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId("developer-loc-set-at-save"));
    });
    await waitFor(() => {
      expect(saveAt).toHaveBeenCalledTimes(1);
    });
    expect(saveAt.mock.calls[0][0]).toEqual([
      enRow,
      { ...frRow, contentTypeName: "percFile" },
    ]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-notice")).toBeTruthy();
    });
  });

  it("saves an empty list as a clear of only this locale", async () => {
    listAt.mockResolvedValue([enRow, frRow]);
    saveAt.mockImplementation(async (rows) => rows);
    render(<LocaleAutoTranslationsSection languageString="fr-ca" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-row-0")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-loc-set-at-remove-0"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-empty")).toBeTruthy();
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId("developer-loc-set-at-save"));
    });
    await waitFor(() => {
      expect(saveAt).toHaveBeenCalledTimes(1);
    });
    expect(saveAt.mock.calls[0][0]).toEqual([enRow]);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-notice")).toBeTruthy();
    });
  });

  it("surfaces an unknown content type 400 without claiming success", async () => {
    listAt.mockResolvedValue([frRow]);
    saveAt.mockRejectedValue({
      name: "ApiError",
      status: 400,
      body: { message: "unknown content type: xx-nope-5704" },
    });
    render(<LocaleAutoTranslationsSection languageString="fr-ca" />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-row-0")).toBeTruthy();
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId("developer-loc-set-at-save"));
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-loc-set-at-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-loc-set-at-notice")).toBeNull();
  });
});
