/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  it("renders the 6 dimension rows for a page item", async () => {
    render(
      <DependencyViewer
        item={{ id: "p-1", folderPath: "/Sites/Foo" }}
        aaLinkCount={2}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(screen.getByTestId("dependency-row-outgoing")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-incoming")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-aa")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-taxonomy")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-local")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-reverse")).toBeTruthy();
  });

  it("shows the AA count when known", async () => {
    render(
      <DependencyViewer
        item={{ id: "x", folderPath: "/p" }}
        aaLinkCount={5}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    const row = screen.getByTestId("dependency-row-aa");
    expect(row.textContent).toContain("5 AA links");
  });

  it("no longer renders the client-side preview banner (US8 ships)", async () => {
    render(
      <DependencyViewer item={{ id: "x" }} loadServerSummary={mockLoad} />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(screen.queryByTestId("dependency-client-side-preview")).toBeNull();
  });

  it("renders the loading skeleton until the server summary resolves", async () => {
    const d = deferred<PSNodeRelationshipSummary>(SYNTHETIC_SERVER);
    render(
      <DependencyViewer
        item={{ id: "x" }}
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
    );
  });

  it("renders the auth placeholder when the loader throws 403", async () => {
    const denied = { status: 403, statusText: "Forbidden" };
    render(
      <DependencyViewer
        item={{ id: "private" }}
        loadServerSummary={() => Promise.reject(denied)}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
    );
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = render(
      <DependencyViewer
        item={{ id: "x", folderPath: "/p" }}
        aaLinkCount={3}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    await renderA11yGate(container);
  });

  it("renders the auth placeholder and does not call loadServerSummary when item.id is missing", async () => {
    const loader = vi.fn();
    render(<DependencyViewer item={{}} loadServerSummary={loader} />);
    await waitFor(() =>
      expect(screen.queryByTestId("dependency-viewer")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
    );
    expect(loader).not.toHaveBeenCalled();
  });
});
