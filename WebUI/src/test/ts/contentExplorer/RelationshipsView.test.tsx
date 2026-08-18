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
import {
  RelationshipsView,
  relationshipSummaryItemId,
} from "../../../main/ts/contentExplorer/views/RelationshipsView";
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

describe("RelationshipsView", () => {
  it("renders the 4 IA-primary rows + a supplementary details panel for AA / reverse", async () => {
    render(
      <RelationshipsView
        item={{ id: "p-1", folderPath: "/Sites/Foo" }}
        aaLinkCount={3}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(screen.getByTestId("relationships-row-outgoing")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-incoming")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-taxonomy")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-local")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-aa")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-reverse")).toBeTruthy();
  });

  it("no longer renders the client-side preview banner (US8 ships)", async () => {
    render(
      <RelationshipsView item={{ id: "x" }} loadServerSummary={mockLoad} />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(
      screen.queryByTestId("relationships-client-side-preview"),
    ).toBeNull();
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = render(
      <RelationshipsView
        item={{ id: "x", folderPath: "/p" }}
        aaLinkCount={3}
        loadServerSummary={mockLoad}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    await renderA11yGate(container);
  });

  it("parses a GUID id before calling /relationships (#3557)", async () => {
    expect(relationshipSummaryItemId("1-101-708")).toBe("708");
    expect(relationshipSummaryItemId(708)).toBe("708");
    const loader = vi.fn().mockResolvedValue(SYNTHETIC_SERVER);
    render(
      <RelationshipsView
        item={{ id: "1-101-708", folderPath: "/Sites/Foo" }}
        loadServerSummary={loader}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    expect(loader).toHaveBeenCalledWith("708");
  });

  it("renders the auth placeholder and does not call loadServerSummary when item.id is missing", async () => {
    const loader = vi.fn();
    render(<RelationshipsView item={{}} loadServerSummary={loader} />);
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
    );
    expect(loader).not.toHaveBeenCalled();
  });
});
