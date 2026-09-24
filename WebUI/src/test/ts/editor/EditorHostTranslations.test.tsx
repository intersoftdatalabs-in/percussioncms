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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TranslationAuthError, TranslationConflictError } from "../../../main/ts/api/contentExplorer/translationsApi";
import { EditorHost } from "../../../main/ts/editor/EditorHost";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

const fieldsFor = (id: string): ItemEditorFields => ({
  contentId: id,
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  revision: 1,
  fields: [{ name: "sys_title", value: `Title ${id}` }],
});

function renderHost(
  props: Partial<React.ComponentProps<typeof EditorHost>> = {},
  entry = "/editor?contentId=42&mode=edit",
) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              loadFields={async (id) => fieldsFor(id)}
              checkout={async () => undefined}
              loadType={async () => ({
                fields: [{ name: "sys_title", label: "Title" }],
              })}
              loadTransitions={async () => ({ transitionTriggers: [] })}
              loadContentTypes={async () => []}
              loadTranslationLocales={async () => [
                { languageString: "en-us", label: "English" },
                { languageString: "fr-fr", label: "French" },
                { languageString: "de-de", label: "German" },
              ]}
              {...props}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EditorHost translation variants (#4816)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("lists locales and opens the chosen variant", async () => {
    const loadFields = vi.fn(async (id: string) => fieldsFor(id));
    renderHost({
      loadFields,
      loadTranslationVariants: async () => ({
        itemId: 42,
        locale: "en-us",
        variants: [
          { contentId: 42, locale: "en-us", role: "source" },
          { contentId: 900, locale: "fr-fr", role: "translation" },
        ],
      }),
    });
    expect(await screen.findByTestId("translations-variant-row-900")).toBeTruthy();
    fireEvent.click(screen.getByTestId("translations-open-variant-900"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/900/);
    });
    expect(loadFields).toHaveBeenCalledWith("900");
    expect(screen.getByTestId("editor-host")).toBeTruthy();
  });

  it("creates one locale and opens that copy", async () => {
    const loadFields = vi.fn(async (id: string) => fieldsFor(id));
    const createTranslationVariants = vi.fn(async () => ({
      created: [{ contentId: 901, locale: "de-de", role: "translation" }],
    }));
    renderHost({
      loadFields,
      loadTranslationVariants: async (itemId) =>
        itemId === "901"
          ? {
              itemId: 901,
              locale: "de-de",
              variants: [{ contentId: 901, locale: "de-de", role: "translation" }],
            }
          : {
              itemId: 42,
              locale: "en-us",
              variants: [{ contentId: 42, locale: "en-us", role: "source" }],
            },
      createTranslationVariants,
    });
    fireEvent.click(await screen.findByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-content-id").textContent).toMatch(/901/);
    });
    expect(createTranslationVariants).toHaveBeenCalledWith({
      itemIds: [42],
      locales: ["de-de"],
    });
    expect(loadFields).toHaveBeenCalledWith("901");
  });

  it("shows permission and duplicate errors without blanking the host", async () => {
    const { unmount } = renderHost({
      loadTranslationVariants: async () => {
        throw new TranslationAuthError("Not allowed");
      },
    });
    expect(await screen.findByTestId("translations-panel")).toHaveAttribute(
      "data-testid-state",
      "auth",
    );
    expect(screen.getByTestId("editor-host")).toBeTruthy();
    expect(screen.getByTestId("editor-content-id")).toBeTruthy();
    unmount();

    renderHost({
      loadTranslationVariants: async () => ({
        itemId: 42,
        locale: "en-us",
        variants: [{ contentId: 42, locale: "en-us", role: "source" }],
      }),
      createTranslationVariants: async () => {
        throw new TranslationConflictError("exists");
      },
    });
    fireEvent.click(await screen.findByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    expect(await screen.findByTestId("translations-create-error")).toBeTruthy();
    expect(screen.getByTestId("editor-host")).toBeTruthy();
    expect(screen.getByTestId("editor-field-sys_title")).toBeTruthy();
  });
});
