/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DependencyViewer } from "../../../main/ts/contentExplorer/views/DependencyViewer";
import type { PSNodeRelationshipSummary } from "../../../main/ts/api/contentExplorer/relationship";
import { renderA11yGate } from "./a11y";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const DEPENDENCY_VIEWER_TEST_TIMEOUT = 30_000;

const SYNTHETIC_SERVER: PSNodeRelationshipSummary = {
  outgoing: { count: 2, byType: [{ type: "translation", count: 2 }] },
  incoming: { count: 1, byType: [{ type: "translation", count: 1 }] },
  taxonomy: { count: 3, nodes: ["a", "b", "c"] },
  local: { count: 2, links: [{ type: "local", targetId: "asset-1" }] },
  reverse: {
    count: 4,
    byType: [
      { type: "translation", count: 2 },
      { type: "linkback", count: 2 },
    ],
  },
};

function mockLoad(): Promise<PSNodeRelationshipSummary> {
  return Promise.resolve(SYNTHETIC_SERVER);
}

function deferred<T>(value: T) {
  let resolve!: (v: T) => void;
  const promise = new Promise<T>((r) => (resolve = r));
  return { promise, resolve };
}

describe("DependencyViewer", () => {
  it("renders the 6 dimension rows for a page item", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    render(
      <DependencyViewer
        item={{ id: "42", folderPath: "/Sites/Foo" }}
        aaLinkCount={2}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("dependency-row-outgoing")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-incoming")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-aa")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-taxonomy")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-local")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-reverse")).toBeTruthy();
  });

  it("shows the AA count when known", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    render(
      <DependencyViewer
        item={{ id: "42", folderPath: "/p" }}
        aaLinkCount={5}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    const row = screen.getByTestId("dependency-row-aa");
    expect(row.textContent).toContain("5 AA links");
  });

  it("no longer renders the client-side preview banner (US8 ships)", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    render(
      <DependencyViewer item={{ id: "42" }} loadServerSummary={mockLoad} />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("dependency-client-side-preview")).toBeNull();
  });

  it("renders the loading skeleton until the server summary resolves", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const d = deferred<PSNodeRelationshipSummary>(SYNTHETIC_SERVER);
    render(
      <DependencyViewer
        item={{ id: "42" }}
        loadServerSummary={() => d.promise}
      />,
    );
    expect(screen.getByTestId("dependency-viewer")).toHaveAttribute(
      "data-testid-state",
      "loading",
    );
    d.resolve(SYNTHETIC_SERVER);
    await waitFor(() =>
      expect(screen.getByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
  });

  it("renders an explicit empty state when every dimension is zero", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const empty = {
      outgoing: { count: 0, byType: [] },
      incoming: { count: 0, byType: [] },
      taxonomy: { count: 0, nodes: [] },
      local: { count: 0, links: [] },
      reverse: { count: 0, byType: [] },
    };
    render(
      <DependencyViewer
        item={{ id: "42" }}
        aaLinkCount={0}
        loadServerSummary={() => Promise.resolve(empty)}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("dependency-empty")).toBeTruthy(),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("dependency-edges")).toBeNull();
    expect(screen.getByTestId("dependency-row-outgoing")).toBeTruthy();
  });

  it("lists known edges and does not show the empty state", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    render(
      <DependencyViewer
        item={{ id: "42" }}
        aaLinkCount={2}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("dependency-edges")).toBeTruthy(),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("dependency-empty")).toBeNull();
    expect(screen.getByTestId("dependency-edge-local").textContent).toContain(
      "asset-1",
    );
  });

  it("maps HTTP 404 to a not-found state, not success or forbidden", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    render(
      <DependencyViewer
        item={{ id: "42" }}
        loadServerSummary={() => Promise.reject({ status: 404 })}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "missing",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("dependency-empty")).toBeNull();
    expect(screen.queryByTestId("dependency-edges")).toBeNull();
  });

  it("renders the auth placeholder when the loader throws 403", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const denied = { status: 403, statusText: "Forbidden" };
    render(
      <DependencyViewer
        item={{ id: "42" }}
        loadServerSummary={() => Promise.reject(denied)}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
  });

  it("passes the zero serious/critical axe-core gate", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const { container } = render(
      <DependencyViewer
        item={{ id: "42", folderPath: "/p" }}
        aaLinkCount={3}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    await renderA11yGate(container);
  });

  it("parses a GUID id before calling /relationships (#3571)", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const loader = vi.fn().mockResolvedValue(SYNTHETIC_SERVER);
    const { container } = render(
      <DependencyViewer
        item={{ id: "1-101-708", folderPath: "/Sites/Foo" }}
        loadServerSummary={loader}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(loader).toHaveBeenCalledWith("708");
    await renderA11yGate(container);
  });

  it("renders error and does not call loadServerSummary for an unparseable id", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const loader = vi.fn();
    const { container } = render(
      <DependencyViewer item={{ id: "nope" }} loadServerSummary={loader} />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "error",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(loader).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("renders the auth placeholder and does not call loadServerSummary when item.id is missing", { timeout: DEPENDENCY_VIEWER_TEST_TIMEOUT }, async () => {
    const loader = vi.fn();
    render(<DependencyViewer item={{}} loadServerSummary={loader} />);
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
      { timeout: DEPENDENCY_VIEWER_ROUND_TRIP_TIMEOUT },
    );
    expect(loader).not.toHaveBeenCalled();
  });
});
