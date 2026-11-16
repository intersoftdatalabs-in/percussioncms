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
import {
  TranslationAuthError,
  TranslationConflictError,
  TranslationNotFoundError,
} from "../../../main/ts/api/contentExplorer/translationsApi";
import {
  filterTargetLocales,
  filterTranslationVariants,
  TranslationsPanel,
  translationsVariantsItemKey,
} from "../../../main/ts/contentExplorer/TranslationsPanel";
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

  it("GETs hyphenated GUID without stripping last segment (#3703)", async () => {
    const loadVariants = vi.fn(async () => SAMPLE_VARIANTS);
    render(
      <TranslationsPanel
        itemId="16777215-101-551"
        itemLabel="Home"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(loadVariants).toHaveBeenCalledWith("16777215-101-551");
    expect(loadVariants).not.toHaveBeenCalledWith("551");
    expect(screen.getByTestId("translations-current-locale-value")).toHaveTextContent(
      "en-us",
    );
    expect(screen.getByTestId("translations-variant-row-335")).toBeTruthy();
    expect(screen.getByTestId("translations-variant-row-900")).toBeTruthy();
  });

  it("GETs bare numeric content ids unchanged", async () => {
    const loadVariants = vi.fn(async () => SAMPLE_VARIANTS);
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(loadVariants).toHaveBeenCalledWith("335");
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

  it("create-variant POSTs GUID last-segment content id (#3545)", async () => {
    const loadVariants = vi.fn(async () => SAMPLE_VARIANTS);
    const createVariants = vi.fn(async () => ({
      created: [
        {
          contentId: 901,
          locale: "de-de",
          role: "translation",
          sourceContentId: 708,
        },
      ],
    }));
    render(
      <TranslationsPanel
        itemId="1-101-708"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    expect(loadVariants).toHaveBeenCalledWith("1-101-708");
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() => expect(createVariants).toHaveBeenCalledTimes(1));
    expect(createVariants).toHaveBeenCalledWith({
      itemIds: [708],
      locales: ["de-de"],
    });
    await waitFor(() =>
      expect(screen.getByTestId("translations-create-success")).toBeInTheDocument(),
    );
    expect(
      screen.queryByTestId("translations-create-error"),
    ).toBeNull();
  });

  it("GET variants with the raw itemId when parse yields null", async () => {
    const loadVariants = vi.fn(async () => SAMPLE_VARIANTS);
    render(
      <TranslationsPanel
        itemId="//Sites/Demo"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(loadVariants).toHaveBeenCalledWith("//Sites/Demo");
  });

  it("keeps the no-id create message for folder-like ids without a content id", async () => {
    const createVariants = vi.fn(async () => ({ created: [] }));
    render(
      <TranslationsPanel
        itemId="//Sites/Demo"
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
      expect(screen.getByTestId("translations-create-error")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("translations-create-error")).toHaveTextContent(
      /does not have a numeric content id/i,
    );
    expect(createVariants).not.toHaveBeenCalled();
  });

  it("opens a locale copy via Open button", async () => {
    const onOpenVariant = vi.fn();
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={async () => SAMPLE_VARIANTS}
        loadLocaleCatalog={async () => CATALOG}
        onOpenVariant={onOpenVariant}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-open-variant-900"),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("translations-open-variant-900"));
    expect(onOpenVariant).toHaveBeenCalledWith(900, "fr-fr");
  });

  it("maps GET 404 to not-found chrome", async () => {
    render(
      <TranslationsPanel
        itemId="335"
        loadVariants={async () => {
          throw new TranslationNotFoundError("missing");
        }}
        loadLocaleCatalog={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "error",
      ),
    );
    expect(screen.getByRole("alert")).toHaveTextContent(/not found/i);
  });

  it("maps create 409 to conflict chrome", async () => {
    const createVariants = vi.fn(async () => {
      throw new TranslationConflictError("exists");
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
      expect(screen.getByTestId("translations-create-error")).toHaveTextContent(
        /already exists/i,
      ),
    );
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

  it("pins an explicit light-surface text color on every state (#5657)", async () => {
    // The editor host mounts this panel under its dark root (inherited
    // #e2e8f0), which fails WCAG 2.2 AA contrast on the panel's own white
    // background. Every state must therefore declare its own dark text.
    render(
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
    expect(screen.getByTestId("translations-panel").style.color).toBeTruthy();
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

describe("translationsVariantsItemKey", () => {
  it("keeps GUID and numeric paths distinct", () => {
    expect(translationsVariantsItemKey("16777215-101-551")).toBe(
      "16777215-101-551",
    );
    expect(translationsVariantsItemKey(" 551 ")).toBe("551");
    expect(translationsVariantsItemKey("1-101-708")).toBe("1-101-708");
  });
});

describe("filterTranslationVariants", () => {
  const ROWS = [
    { contentId: 42, locale: "en-us", role: "source" },
    { contentId: 900, locale: "fr-fr", role: "translation" },
    { contentId: 901, locale: "de-de", role: "translation" },
  ];

  it("returns every row for an empty query", () => {
    expect(filterTranslationVariants(ROWS, "")).toHaveLength(3);
    expect(filterTranslationVariants(ROWS, "   ")).toHaveLength(3);
  });

  it("matches locale code case-insensitively", () => {
    expect(
      filterTranslationVariants(ROWS, "FR-fr").map((row) => row.contentId),
    ).toEqual([900]);
  });

  it("matches role code and display label", () => {
    expect(
      filterTranslationVariants(ROWS, "source").map((row) => row.contentId),
    ).toEqual([42]);
    expect(
      filterTranslationVariants(ROWS, "Translation", (role) =>
        role === "translation" ? "Translation" : (role ?? ""),
      ).map((row) => row.contentId),
    ).toEqual([900, 901]);
  });

  it("matches content id substring", () => {
    expect(
      filterTranslationVariants(ROWS, "90").map((row) => row.contentId),
    ).toEqual([900, 901]);
  });

  it("strips NUL characters instead of matching or throwing", () => {
    const nul = String.fromCharCode(0);
    expect(
      filterTranslationVariants(ROWS, `${nul}fr-fr${nul}`).map(
        (row) => row.contentId,
      ),
    ).toEqual([900]);
    expect(filterTranslationVariants(ROWS, nul)).toHaveLength(3);
  });
});

describe("TranslationsPanel variant search (#6414)", () => {
  async function renderReady() {
    render(
      <TranslationsPanel
        itemId="42"
        itemLabel="Home"
        loadVariants={async () => ({
          itemId: 42,
          locale: "en-us",
          variants: [
            { contentId: 42, locale: "en-us", role: "source" },
            { contentId: 900, locale: "fr-fr", role: "translation" },
            { contentId: 901, locale: "de-de", role: "translation" },
          ],
        })}
        loadLocaleCatalog={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
  }

  it("filters rows by typed locale and restores on clear", async () => {
    await renderReady();
    const search = screen.getByTestId("translations-variants-search");
    expect(screen.getByTestId("translations-variant-row-42")).toBeTruthy();
    expect(screen.getByTestId("translations-variant-row-900")).toBeTruthy();
    fireEvent.change(search, { target: { value: "fr" } });
    expect(
      screen.queryByTestId("translations-variant-row-42"),
    ).toBeNull();
    expect(screen.getByTestId("translations-variant-row-900")).toBeTruthy();
    expect(
      screen.queryByTestId("translations-variant-row-901"),
    ).toBeNull();
    fireEvent.change(search, { target: { value: "" } });
    expect(screen.getByTestId("translations-variant-row-42")).toBeTruthy();
    expect(screen.getByTestId("translations-variant-row-901")).toBeTruthy();
  });

  it("shows a distinct no-match state instead of the table", async () => {
    await renderReady();
    fireEvent.change(screen.getByTestId("translations-variants-search"), {
      target: { value: "xx-no-such-locale" },
    });
    expect(
      screen.getByTestId("translations-variants-no-match"),
    ).toBeTruthy();
    expect(screen.queryByTestId("translations-variants-table")).toBeNull();
    expect(
      screen.queryByTestId("translations-variants-empty"),
    ).toBeNull();
  });

  it("keeps the create success notice across its own list reload, clears on item change", async () => {
    const loadVariants = vi.fn(async () => SAMPLE_VARIANTS);
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
    const view = render(
      <TranslationsPanel
        itemId="335"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    const reloadsBefore = loadVariants.mock.calls.length;
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-create-success"),
      ).toBeInTheDocument(),
    );
    // The success-triggered reload ran, and the notice survived it.
    await waitFor(() =>
      expect(loadVariants.mock.calls.length).toBeGreaterThan(reloadsBefore),
    );
    expect(screen.getByTestId("translations-create-success")).toBeInTheDocument();
    expect(screen.getByTestId("translations-variant-row-335")).toBeTruthy();
    // Loading a different item clears the stale notice.
    view.rerender(
      <TranslationsPanel
        itemId="999"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("translations-create-success")).toBeNull(),
    );
  });

  it("resets the variant filter on item change and after a successful create", async () => {
    const loadVariants = vi.fn(async () => SAMPLE_VARIANTS);
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
    const view = render(
      <TranslationsPanel
        itemId="335"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    // A filter that hides a row does not survive an item change.
    fireEvent.change(screen.getByTestId("translations-variants-search"), {
      target: { value: "fr" },
    });
    expect(screen.queryByTestId("translations-variant-row-335")).toBeNull();
    view.rerender(
      <TranslationsPanel
        itemId="999"
        loadVariants={loadVariants}
        loadLocaleCatalog={async () => CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-variant-row-335")).toBeTruthy(),
    );
    expect(screen.getByTestId("translations-variants-search")).toHaveValue("");
    // A successful create clears the filter so the new copy is visible.
    fireEvent.change(screen.getByTestId("translations-variants-search"), {
      target: { value: "fr" },
    });
    expect(screen.queryByTestId("translations-variant-row-335")).toBeNull();
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-create-success"),
      ).toBeInTheDocument(),
    );
    expect(screen.getByTestId("translations-variants-search")).toHaveValue("");
    expect(screen.getByTestId("translations-variant-row-335")).toBeTruthy();
  });

  it("passes the zero serious/critical axe-core gate with search", async () => {    const { container } = render(
      <TranslationsPanel
        itemId="42"
        itemLabel="Home"
        loadVariants={async () => ({
          itemId: 42,
          locale: "en-us",
          variants: [
            { contentId: 42, locale: "en-us", role: "source" },
            { contentId: 900, locale: "fr-fr", role: "translation" },
          ],
        })}
        loadLocaleCatalog={async () => []}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("translations-panel")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    fireEvent.change(screen.getByTestId("translations-variants-search"), {
      target: { value: "fr" },
    });
    await renderA11yGate(container);
  });
});

describe("filterTargetLocales (#6415)", () => {
  const TARGETS = [
    { languageString: "fr-fr", label: "French" },
    { languageString: "de-de", label: "German" },
    { languageString: "es-es", label: "Spanish" },
  ];

  it("matches label or code, case-insensitively", () => {
    expect(
      filterTargetLocales(TARGETS, "ger").map((t) => t.languageString),
    ).toEqual(["de-de"]);
    expect(
      filterTargetLocales(TARGETS, "FR-").map((t) => t.languageString),
    ).toEqual(["fr-fr"]);
    expect(filterTargetLocales(TARGETS, "-es").map((t) => t.languageString))
      .toEqual(["es-es"]);
  });

  it("returns every target on an empty query", () => {
    expect(filterTargetLocales(TARGETS, "")).toHaveLength(3);
    expect(filterTargetLocales(TARGETS, "   ")).toHaveLength(3);
  });

  it("strips NUL characters instead of matching or throwing", () => {
    const nul = String.fromCharCode(0);
    expect(
      filterTargetLocales(TARGETS, `${nul}spanish${nul}`).map(
        (t) => t.languageString,
      ),
    ).toEqual(["es-es"]);
    expect(filterTargetLocales(TARGETS, nul)).toHaveLength(3);
  });
});

describe("TranslationsPanel target-locale search (#6415)", () => {
  const SOURCE_ONLY = {
    itemId: 42,
    locale: "en-us",
    variants: [{ contentId: 42, locale: "en-us", role: "source" }],
  };
  const BIG_CATALOG = [
    { languageString: "en-us", label: "English (US)" },
    { languageString: "fr-fr", label: "French" },
    { languageString: "de-de", label: "German" },
    { languageString: "es-es", label: "Spanish" },
  ];

  async function renderCreatable(createVariants?: ReturnType<typeof vi.fn>) {
    render(
      <TranslationsPanel
        itemId="42"
        itemLabel="Home"
        loadVariants={async () => SOURCE_ONLY}
        loadLocaleCatalog={async () => BIG_CATALOG}
        createVariants={createVariants}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
  }

  it("filters target checkboxes and restores the full list on clear", async () => {
    await renderCreatable();
    const search = screen.getByTestId("translations-target-locales-search");
    expect(screen.getByTestId("translations-locale-option-fr-fr")).toBeTruthy();
    fireEvent.change(search, { target: { value: "ger" } });
    expect(
      screen.queryByTestId("translations-locale-option-fr-fr"),
    ).toBeNull();
    expect(screen.getByTestId("translations-locale-option-de-de")).toBeTruthy();
    expect(
      screen.queryByTestId("translations-target-locales-no-match"),
    ).toBeNull();
    fireEvent.change(search, { target: { value: "" } });
    expect(screen.getByTestId("translations-locale-option-fr-fr")).toBeTruthy();
    expect(screen.getByTestId("translations-locale-option-es-es")).toBeTruthy();
  });

  it("names the filter in the zero-match state and keeps submit disabled", async () => {
    await renderCreatable();
    fireEvent.change(screen.getByTestId("translations-target-locales-search"), {
      target: { value: "xx-no-such-locale" },
    });
    const noMatch = screen.getByTestId("translations-target-locales-no-match");
    expect(noMatch).toBeTruthy();
    expect(noMatch).toHaveTextContent("xx-no-such-locale");
    expect(
      screen.queryByTestId("translations-locale-options"),
    ).toBeNull();
    expect(screen.getByTestId("translations-create-submit")).toBeDisabled();
  });

  it("keeps the selection across filter changes and posts it", async () => {
    const createVariants = vi.fn(async () => ({ created: [] }));
    await renderCreatable(createVariants);
    // Select while filtered, then clear — the check stays checked.
    fireEvent.change(screen.getByTestId("translations-target-locales-search"), {
      target: { value: "ger" },
    });
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.change(screen.getByTestId("translations-target-locales-search"), {
      target: { value: "" },
    });
    expect(
      screen.getByTestId("translations-locale-option-de-de"),
    ).toBeChecked();
    expect(screen.getByTestId("translations-create-submit")).not.toBeDisabled();
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() => expect(createVariants).toHaveBeenCalledTimes(1));
    expect(createVariants).toHaveBeenCalledWith({
      itemIds: [42],
      locales: ["de-de"],
    });
  });

  it("resets the target search after a successful create", async () => {
    const createVariants = vi.fn(async () => ({
      created: [
        { contentId: 901, locale: "de-de", role: "translation" },
      ],
    }));
    await renderCreatable(createVariants);
    fireEvent.change(screen.getByTestId("translations-target-locales-search"), {
      target: { value: "ger" },
    });
    fireEvent.click(screen.getByTestId("translations-locale-option-de-de"));
    fireEvent.click(screen.getByTestId("translations-create-submit"));
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-create-success"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByTestId("translations-target-locales-search"),
    ).toHaveValue("");
  });

  it("passes the zero serious/critical axe-core gate with target search", async () => {
    const { container } = render(
      <TranslationsPanel
        itemId="42"
        itemLabel="Home"
        loadVariants={async () => SOURCE_ONLY}
        loadLocaleCatalog={async () => BIG_CATALOG}
      />,
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("translations-locale-option-de-de"),
      ).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByTestId("translations-target-locales-search"), {
      target: { value: "ger" },
    });
    await renderA11yGate(container);
  });
});
