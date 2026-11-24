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

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { EditorFieldRow } from "../../../main/ts/editor/controlKinds";
import {
  defaultPageMetadataOpenGroups,
  groupPageMetadataRows,
} from "../../../main/ts/editor/editorPageMetadata";
import {
  EditorPageMetadataPanel,
  PageSummaryPreview,
} from "../../../main/ts/editor/EditorPageMetadataPanel";
import { EditorSiteImpactPanel } from "../../../main/ts/editor/EditorSiteImpactPanel";

function row(name: string, value = ""): EditorFieldRow {
  return {
    name,
    value,
    label: name,
    readOnly: false,
    kind: "text",
    required: false,
  };
}

/**
 * Single wall-clock budget for every polling gate in this file (#7209).
 * Each round trip here is purely microtask-driven (resolved/rejected-promise
 * {@code loadSiteImpact} mocks → setState → render → focus effect) with no
 * real timers involved, so the gates are deterministic in outcome; the
 * budget is strictly a backstop for loaded-worker scheduling lag. Every
 * {@code findByTestId} / {@code waitFor} uses it explicitly: the 1s
 * testing-library default is the most-tripped cliff when hundreds of suite
 * files share the worker host (same rationale as
 * {@code TAKEDOWN_ROUND_TRIP_TIMEOUT} in #7189, {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122, and {@code PICKER_MOUNT_TIMEOUT} in #7151).
 */
const SITE_IMPACT_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7209, same load-contention rationale as
 * {@code TAKEDOWN_TEST_TIMEOUT} in #7189 and
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The sequential gates
 * in the retry tests can jointly exceed the 20s global default when the
 * worker host is saturated. The primary gates stay progress-based (single
 * bounded backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const SITE_IMPACT_TEST_TIMEOUT = 30_000;

describe("page metadata groups panel (#5739)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders the first group open and the rest collapsed with toggles", () => {
    const grouped = groupPageMetadataRows([
      row("sys_title", "Home"),
      row("page_title", "Home page"),
    ]);
    const renderRow = (entry: EditorFieldRow): React.ReactNode => (
      <div data-testid={`stub-row-${entry.name}`} />
    );
    render(
      <EditorPageMetadataPanel
        sections={grouped.sections}
        ungrouped={grouped.ungrouped}
        openGroups={defaultPageMetadataOpenGroups()}
        onToggleGroup={() => {}}
        renderRow={renderRow}
      />,
    );
    expect(screen.getByTestId("editor-metadata-group-page-summary")).toBeTruthy();
    expect(screen.getByTestId("stub-row-sys_title")).toBeTruthy();
    expect(screen.queryByTestId("stub-row-page_title")).toBeNull();
    const seoToggle = screen.getByTestId("editor-metadata-group-toggle-seo");
    expect(seoToggle.getAttribute("aria-expanded")).toBe("false");
    expect(
      screen
        .getByTestId("editor-metadata-group-toggle-page-summary")
        .getAttribute("aria-expanded"),
    ).toBe("true");
  });

  it("toggles a collapsed group open and shows the post-date note", () => {
    const grouped = groupPageMetadataRows([
      row("sys_contentpostdate", "2026-01-01"),
      row("page_title", "Home page"),
    ]);
    const onToggleGroup = vi.fn();
    const renderRow = (entry: EditorFieldRow): React.ReactNode => (
      <div data-testid={`stub-row-${entry.name}`} />
    );
    const open = defaultPageMetadataOpenGroups();
    const { rerender } = render(
      <EditorPageMetadataPanel
        sections={grouped.sections}
        ungrouped={grouped.ungrouped}
        openGroups={open}
        onToggleGroup={onToggleGroup}
        renderRow={renderRow}
      />,
    );
    fireEvent.click(screen.getByTestId("editor-metadata-group-toggle-seo"));
    expect(onToggleGroup).toHaveBeenCalledWith("seo");

    rerender(
      <EditorPageMetadataPanel
        sections={grouped.sections}
        ungrouped={grouped.ungrouped}
        openGroups={{ ...open, seo: true }}
        onToggleGroup={onToggleGroup}
        renderRow={renderRow}
      />,
    );
    expect(screen.getByTestId("stub-row-page_title")).toBeTruthy();
    expect(screen.getByTestId("editor-metadata-postdate-note")).toBeTruthy();
  });

  it("renders the auto-summary preview sanitized and read-only", () => {
    render(
      <PageSummaryPreview
        html={'<p>Preview<script>alert(1)</script></p><img src="x" onerror="alert(2)">'}
      />,
    );
    const preview = screen.getByTestId("editor-metadata-summary-preview");
    expect(preview.querySelector("script")).toBeNull();
    expect(preview.querySelector("[onerror]")).toBeNull();
    expect(preview.textContent).toContain("Preview");
    expect(preview.getAttribute("role")).toBe("region");
  });
});

describe("page site impact panel (#5739)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders impacting pages and templates", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => ({
          pages: [
            {
              id: "7",
              name: "Home",
              path: "/Sites/Demo/Home",
              site: "Demo",
              status: "Live",
              type: "Page",
            },
          ],
          templates: [{ name: "Base", site: "Demo" }],
        })}
      />,
    );
    expect(
      await screen.findByTestId("editor-site-impact-pages-table", undefined, {
        timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
      }),
    ).toBeTruthy();
    expect(screen.getByTestId("editor-site-impact-page-row").textContent).toContain(
      "Home",
    );
    expect(
      await screen.findByTestId("editor-site-impact-templates-table", undefined, {
        timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
      }),
    ).toBeTruthy();
  });

  it("renders empty and error states", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    const { unmount } = render(
      <EditorSiteImpactPanel itemId="42" loadSiteImpact={async () => ({ pages: [], templates: [] })} />,
    );
    expect(await screen.findByTestId("editor-site-impact-empty", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    expect(
      screen.getByTestId("editor-site-impact-templates-empty"),
    ).toBeTruthy();
    unmount();

    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => {
          throw new Error("boom");
        }}
      />,
    );
    expect(await screen.findByTestId("editor-site-impact-error", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
  });

  it("retry re-runs the load and renders rows after a failure (#6771)", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    let calls = 0;
    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => {
          calls += 1;
          if (calls === 1) {
            throw new Error("boom");
          }
          return {
            pages: [
              {
                id: "7",
                name: "Home",
                path: "/Sites/Demo/Home",
                site: "Demo",
                status: "Live",
                type: "Page",
              },
            ],
            templates: [],
          };
        }}
      />,
    );
    const retry = await screen.findByTestId("editor-retry-site-impact", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    });
    expect(screen.getByTestId("editor-site-impact-error")).toBeTruthy();
    fireEvent.click(retry);
    expect(await screen.findByTestId("editor-site-impact-pages-table", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    expect(screen.queryByTestId("editor-site-impact-error")).toBeNull();
    expect(calls).toBe(2);
  });

  it("a repeated failure keeps the alert and offers retry again (#6771)", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    let calls = 0;
    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => {
          calls += 1;
          throw new Error(`boom ${calls}`);
        }}
      />,
    );
    expect(await screen.findByTestId("editor-retry-site-impact", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-retry-site-impact"));
    // Deterministic drain (#7209, #7189 pattern): the rejected-promise mock
    // is microtask-only, so flushing with act settles the error commit
    // without wall-clock polling. The bounded gate below is strictly a
    // backstop for loaded-worker lag.
    await act(async () => {});
    expect(await screen.findByTestId("editor-site-impact-error", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    expect(screen.getByTestId("editor-retry-site-impact")).toBeTruthy();
    expect(calls).toBeGreaterThanOrEqual(2);
  });

  it("shows the loading indicator while the load is pending (#6771)", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    let resolveLoad: (value: { pages: never[]; templates: never[] }) => void = () => undefined;
    const pending = new Promise<{ pages: never[]; templates: never[] }>((resolve) => {
      resolveLoad = resolve;
    });
    render(<EditorSiteImpactPanel itemId="42" loadSiteImpact={() => pending} />);
    expect(await screen.findByTestId("editor-site-impact-loading", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    resolveLoad({ pages: [], templates: [] });
    expect(await screen.findByTestId("editor-site-impact-empty", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
  });

  it("announces the loading state through a live region (#6786)", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    let resolveLoad: (value: { pages: never[]; templates: never[] }) => void = () => undefined;
    const pending = new Promise<{ pages: never[]; templates: never[] }>((resolve) => {
      resolveLoad = resolve;
    });
    render(<EditorSiteImpactPanel itemId="42" loadSiteImpact={() => pending} />);
    const loading = await screen.findByTestId("editor-site-impact-loading", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    });
    expect(loading.getAttribute("role")).toBe("status");
    resolveLoad({ pages: [], templates: [] });
    expect(await screen.findByTestId("editor-site-impact-empty", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
  });

  it("retry success moves focus to the panel heading (#6786)", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    let calls = 0;
    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => {
          calls += 1;
          if (calls === 1) {
            throw new Error("boom");
          }
          return {
            pages: [
              {
                id: "7",
                name: "Home",
                path: "/Sites/Demo/Home",
                site: "Demo",
                status: "Live",
                type: "Page",
              },
            ],
            templates: [],
          };
        }}
      />,
    );
    const retry = await screen.findByTestId("editor-retry-site-impact", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    });
    fireEvent.click(retry);
    // Deterministic drain (#7209, #7189 pattern): the mocked second load
    // resolves on the microtask queue, so flushing with act settles the
    // success commit without wall-clock polling. The bounded gates below
    // are strictly backstops for loaded-worker lag.
    await act(async () => {});
    expect(await screen.findByTestId("editor-site-impact-pages-table", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    // Poll the focus landing, not just the table mount (#7209): the
    // completion effect moves focus after the success commit, so a
    // single-shot assertion can race effect scheduling under full-suite
    // load while the mount gate above already passed.
    await waitFor(
      () => {
        expect(document.activeElement).toBe(
          screen.getByTestId("editor-site-impact-heading"),
        );
      },
      { timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT },
    );
    expect(document.activeElement?.tagName).not.toBe("BODY");
  });

  it("a repeated failure returns focus to the retry button (#6786)", { timeout: SITE_IMPACT_TEST_TIMEOUT }, async () => {
    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => {
          throw new Error("boom");
        }}
      />,
    );
    expect(await screen.findByTestId("editor-retry-site-impact", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    fireEvent.click(screen.getByTestId("editor-retry-site-impact"));
    // Deterministic drain (#7209, #7189 pattern): the rejected-promise mock
    // is microtask-only, so flushing with act settles the error commit
    // without wall-clock polling. The bounded gates below are strictly
    // backstops for loaded-worker lag.
    await act(async () => {});
    expect(await screen.findByTestId("editor-site-impact-error", undefined, {
      timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT,
    })).toBeTruthy();
    // Poll the focus landing, not just the error mount (#7209): the
    // completion effect moves focus after the error commit, so a
    // single-shot assertion can race effect scheduling under full-suite
    // load while the mount gate above already passed.
    await waitFor(
      () => {
        expect(document.activeElement?.getAttribute("data-testid")).toBe(
          "editor-retry-site-impact",
        );
      },
      { timeout: SITE_IMPACT_ROUND_TRIP_TIMEOUT },
    );
    expect(document.activeElement?.tagName).not.toBe("BODY");
  });
});
