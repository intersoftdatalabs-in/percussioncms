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
import { describe, expect, it, vi } from "vitest";
import { TranslationAuthError } from "../../../main/ts/api/contentExplorer/translationsApi";
import { TranslationsPanel } from "../../../main/ts/contentExplorer/TranslationsPanel";
import { renderA11yGate } from "./a11y";

const SAMPLE_VARIANTS = {
  itemId: 335,
  locale: "en-us",
  variants: [
    {
      contentId: 335,
      locale: "en-us",
      role: "source",
      revision: 1,
    },
    {
      contentId: 900,
      locale: "fr-fr",
      role: "translation",
      sourceContentId: 335,
    },
  ],
};

const CATALOG = [
  { languageString: "en-us", label: "English (US)" },
  { languageString: "fr-fr", label: "French" },
  { languageString: "de-de", label: "German" },
];

describe("TranslationsPanel", () => {
  it("renders current locale and variant rows from injected loader", async () => {
    render(
      <TranslationsPanel
        itemId="335"
        itemLabel="Home"
        loadVariants={async () => SAMPLE_VARIANTS}
        loadLocaleCatalog={async () => CATALOG}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(screen.getByTestId("translations-current-locale-value")).toHaveTextContent(
      "en-us",
    );
    expect(screen.getByTestId("translations-variant-row-335")).toBeTruthy();
    expect(screen.getByTestId("translations-variant-row-900")).toBeTruthy();
    expect(screen.getByTestId("translations-inflight-note")).toBeTruthy();
  });

  it("shows only locales not already present as create targets", async () => {
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={async () => SAMPLE_VARIANTS}
        loadLocaleCatalog={async () => CATALOG}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.queryByTestId("translations-locale-option-en-us"),
    ).toBeNull();
    expect(
      screen.queryByTestId("translations-locale-option-fr-fr"),
    ).toBeNull();
  });

  it("create-variant POSTs selected locales via injection seam", async () => {
    const createVariants = vi.fn(async () => ({
      created: [
        {
          contentId: 901,
          locale: "de-de",
          role: "translation",
          sourceContentId: 335,
        },
      ],
    }));
    const onCreated = vi.fn();
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={async () => SAMPLE_VARIANTS}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
        onCreated={onCreated}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() => expect(createVariants).toHaveBeenCalledTimes(1));
    expect(createVariants).toHaveBeenCalledWith({
      itemIds: [335],
      locales: ["de-de"],
    });
    await waitFor(() =>
      expect(screen.getByTestId("translations-create-success")).toBeInTheDocument(),
    );
    expect(onCreated).toHaveBeenCalled();
  });

  it("maps create 403 to permission chrome", async () => {
    const createVariants = vi.fn(async () => {
      throw new TranslationAuthError("no");
    });
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={async () => SAMPLE_VARIANTS}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-create-forbidden"),
      ).toBeInTheDocument(),
    );
  });

  it("renders auth state when loader is forbidden", async () => {
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={async () => {
          throw new TranslationAuthError("denied");
        }}
        loadLocaleCatalog={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
    );
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = render(
      <TranslationsPanel
        itemId="335"
        itemLabel="Page"
        loadVariants={async () => SAMPLE_VARIANTS}
        loadLocaleCatalog={async () => CATALOG}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    await renderA11yGate(container);
  });
});
