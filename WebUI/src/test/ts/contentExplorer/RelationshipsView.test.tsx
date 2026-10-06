/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
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
        item={{ id: "1-101-708", folderPath: "/Sites/Foo" }}
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
      <RelationshipsView item={{ id: "42" }} loadServerSummary={mockLoad} />,
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
        item={{ id: "42", folderPath: "/p" }}
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

  it("does not fetch a timestamped asset title as a content id (#3811)", async () => {
    expect(
      relationshipSummaryItemId("New-percSimpleTextAsset-20260820165542"),
    ).toBe("");
    const loader = vi.fn();
    render(
      <RelationshipsView
        item={{
          id: "New-percSimpleTextAsset-20260820165542",
          path: "/Assets/uploads/New-percSimpleTextAsset-20260820165542",
        }}
        loadServerSummary={loader}
      />,
    );
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "auth",
      ),
    );
    expect(loader).not.toHaveBeenCalled();
  });

  it("cancel leaves the relationship (#4969)", async () => {
    const remove = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [
          {
            relationshipId: 7,
            configName: "Translation",
            category: "rs_translation",
            dependentId: 9,
            label: "Translation -> 9",
          },
        ]}
        removeEdge={remove}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-7")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-remove-7"));
    fireEvent.click(screen.getByTestId("relationships-remove-cancel"));
    expect(remove).not.toHaveBeenCalled();
    expect(screen.getByTestId("relationships-edge-7")).toBeTruthy();
    expect(screen.queryByTestId("relationships-removed")).toBeNull();
  });

  it("confirm removes the relationship and refreshes the list (#4969)", async () => {
    const remove = vi.fn().mockResolvedValue(undefined);
    let edges = [
      {
        relationshipId: 7,
        configName: "Translation",
        category: "rs_translation",
        dependentId: 9,
        label: "Translation -> 9",
      },
    ];
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => edges}
        removeEdge={async (itemId, relationshipId) => {
          await remove(itemId, relationshipId);
          edges = [];
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-7")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-remove-7"));
    fireEvent.click(screen.getByTestId("relationships-remove-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-removed")).toBeTruthy(),
    );
    expect(remove).toHaveBeenCalledWith("42", 7);
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-edge-7")).toBeNull(),
    );
  });

  it("HTTP 409 stays on the panel and does not claim success (#4969)", async () => {
    const remove = vi.fn().mockRejectedValue({ status: 409 });
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [
          {
            relationshipId: 7,
            configName: "Translation",
            category: "rs_translation",
            dependentId: 9,
            label: "Translation -> 9",
          },
        ]}
        removeEdge={remove}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-7")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-remove-7"));
    fireEvent.click(screen.getByTestId("relationships-remove-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-remove-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-removed")).toBeNull();
    expect(screen.getByTestId("relationships-edge-7")).toBeTruthy();
  });

  it("cancel leaves every owned relationship (#4988)", async () => {
    const remove = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [
          {
            relationshipId: 7,
            configName: "Translation",
            category: "rs_translation",
            dependentId: 9,
            label: "Translation -> 9",
          },
          {
            relationshipId: 3,
            configName: "Folder",
            category: "rs_folder",
            dependentId: 1,
            label: "Folder",
          },
        ]}
        removeEdge={remove}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-remove-all")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-remove-all"));
    fireEvent.click(screen.getByTestId("relationships-remove-all-cancel"));
    expect(remove).not.toHaveBeenCalled();
    expect(screen.getByTestId("relationships-edge-7")).toBeTruthy();
    expect(screen.getByTestId("relationships-folder-3")).toBeTruthy();
    expect(screen.queryByTestId("relationships-removed-all")).toBeNull();
  });

  it("confirm removes every owned relationship and keeps the folder row (#4988)", async () => {
    const remove = vi.fn().mockResolvedValue(undefined);
    let edges = [
      {
        relationshipId: 7,
        configName: "Translation",
        category: "rs_translation",
        dependentId: 9,
        label: "Translation -> 9",
      },
      {
        relationshipId: 11,
        configName: "Active Assembly",
        category: "rs_aa",
        dependentId: 4,
        label: "AA -> 4",
      },
      {
        relationshipId: 3,
        configName: "Folder",
        category: "rs_folder",
        dependentId: 1,
        label: "Folder",
      },
    ];
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => edges}
        removeEdge={async (itemId, relationshipId) => {
          await remove(itemId, relationshipId);
          edges = edges.filter((edge) => edge.relationshipId !== relationshipId);
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-11")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-remove-all"));
    fireEvent.click(screen.getByTestId("relationships-remove-all-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-removed-all")).toBeTruthy(),
    );
    expect(remove.mock.calls).toEqual([
      ["42", 7],
      ["42", 11],
    ]);
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-edge-7")).toBeNull(),
    );
    expect(screen.queryByTestId("relationships-edge-11")).toBeNull();
    expect(screen.getByTestId("relationships-folder-3")).toBeTruthy();
  });

  it("HTTP 409 on a later row does not claim every relationship was removed (#4988)", async () => {
    const remove = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce({ status: 409 });
    let edges = [
      {
        relationshipId: 7,
        configName: "Translation",
        category: "rs_translation",
        dependentId: 9,
        label: "Translation -> 9",
      },
      {
        relationshipId: 11,
        configName: "Active Assembly",
        category: "rs_aa",
        dependentId: 4,
        label: "AA -> 4",
      },
    ];
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => edges}
        removeEdge={async (itemId, relationshipId) => {
          await remove(itemId, relationshipId);
          if (relationshipId === 7) {
            edges = edges.filter((edge) => edge.relationshipId !== 7);
          }
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-11")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-remove-all"));
    fireEvent.click(screen.getByTestId("relationships-remove-all-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-remove-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-removed-all")).toBeNull();
    await waitFor(() =>
      expect(screen.queryByTestId("relationships-edge-7")).toBeNull(),
    );
    expect(screen.getByTestId("relationships-edge-11")).toBeTruthy();
  });

  it("cancel does not add a relationship (#5036)", async () => {
    const add = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => []}
        addEdge={add}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-view")).toHaveAttribute(
        "data-testid-state",
        "ok",
      ),
    );
    fireEvent.click(screen.getByTestId("relationships-add"));
    fireEvent.change(screen.getByTestId("relationships-add-target"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("relationships-add-cancel"));
    expect(add).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-added")).toBeNull();
  });

  it("confirm adds one relationship and shows it only after success (#5036)", async () => {
    const created = {
      relationshipId: 11,
      configName: "Translation",
      category: "rs_translation",
      dependentId: 9,
      label: "Translation -> 9",
    };
    let edges: typeof created[] = [];
    const add = vi.fn().mockImplementation(async () => {
      edges = [created];
      return created;
    });
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => edges}
        addEdge={add}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-add")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-edge-11")).toBeNull();
    fireEvent.click(screen.getByTestId("relationships-add"));
    fireEvent.change(screen.getByTestId("relationships-add-target"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("relationships-add-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-added")).toBeTruthy(),
    );
    expect(add).toHaveBeenCalledWith("42", "9", "Translation");
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-11")).toBeTruthy(),
    );
  });

  it("HTTP 409 does not claim a relationship was added (#5036)", async () => {
    const add = vi.fn().mockRejectedValue(Object.assign(new Error("no"), { status: 409 }));
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => []}
        addEdge={add}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-add")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-add"));
    fireEvent.change(screen.getByTestId("relationships-add-target"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("relationships-add-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-add-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-added")).toBeNull();
    expect(screen.queryByTestId("relationships-edge-11")).toBeNull();
  });

  it("a folder relationship type does not call the server (#5036)", async () => {
    const add = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => []}
        addEdge={add}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-add")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-add"));
    fireEvent.change(screen.getByTestId("relationships-add-target"), {
      target: { value: "9" },
    });
    fireEvent.change(screen.getByTestId("relationships-add-type"), {
      target: { value: "rs_folder" },
    });
    fireEvent.click(screen.getByTestId("relationships-add-confirm"));
    expect(add).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-added")).toBeNull();
    expect(screen.getByTestId("relationships-add-error")).toBeTruthy();
  });

  const aaFirst = {
    relationshipId: 71,
    configName: "Active Assembly",
    category: "rs_aa",
    dependentId: 4,
    label: "AA first",
  };
  const aaLast = {
    relationshipId: 72,
    configName: "Active Assembly",
    category: "rs_aa",
    dependentId: 5,
    label: "AA last",
  };
  const translationEdge = {
    relationshipId: 73,
    configName: "Translation",
    category: "rs_translation",
    dependentId: 9,
    label: "Translation stays",
  };

  function edgeOrder(): string[] {
    const list = screen.getByTestId("relationships-edge-list");
    return Array.from(list.querySelectorAll("li")).map(
      (row) => row.getAttribute("data-testid") ?? "",
    );
  }

  it("the first Active Assembly row cannot move up and the last cannot move down (#5201)", async () => {
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [aaFirst, translationEdge, aaLast]}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-71")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-move-up-71")).toBeNull();
    expect(screen.getByTestId("relationships-move-down-71")).toBeTruthy();
    expect(screen.getByTestId("relationships-move-up-72")).toBeTruthy();
    expect(screen.queryByTestId("relationships-move-down-72")).toBeNull();
    expect(screen.queryByTestId("relationships-move-up-73")).toBeNull();
    expect(screen.queryByTestId("relationships-move-down-73")).toBeNull();
    expect(screen.getByTestId("relationships-remove-73")).toBeTruthy();
  });

  it("a single Active Assembly row has no move controls (#5201)", async () => {
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [aaFirst]}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-71")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-move-up-71")).toBeNull();
    expect(screen.queryByTestId("relationships-move-down-71")).toBeNull();
  });

  it("cancel does not move a relationship (#5201)", async () => {
    const move = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [aaFirst, aaLast]}
        moveEdge={move}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-down-71")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-move-down-71"));
    fireEvent.click(screen.getByTestId("relationships-move-cancel"));
    expect(move).not.toHaveBeenCalled();
    expect(edgeOrder()).toEqual([
      "relationships-edge-71",
      "relationships-edge-72",
    ]);
    expect(screen.queryByTestId("relationships-moved")).toBeNull();
  });

  it("confirm moves one relationship and updates the list only after success (#5201)", async () => {
    let rows = [aaFirst, aaLast];
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    const move = vi.fn().mockImplementation(async () => {
      await gate;
      rows = [aaLast, aaFirst];
    });
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => rows}
        moveEdge={move}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-down-71")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-move-down-71"));
    fireEvent.click(screen.getByTestId("relationships-move-confirm"));
    expect(edgeOrder()).toEqual([
      "relationships-edge-71",
      "relationships-edge-72",
    ]);
    expect(screen.queryByTestId("relationships-moved")).toBeNull();
    release();
    await waitFor(() =>
      expect(screen.getByTestId("relationships-moved")).toBeTruthy(),
    );
    await waitFor(() =>
      expect(edgeOrder()).toEqual([
        "relationships-edge-72",
        "relationships-edge-71",
      ]),
    );
    expect(move).toHaveBeenCalledWith(71, "DOWN");
    expect(screen.queryByTestId("relationships-move-up-72")).toBeNull();
    expect(screen.getByTestId("relationships-move-up-71")).toBeTruthy();
  });

  async function expectMoveKeepsOrder(status: number): Promise<void> {
    const move = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error("no"), { status }));
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [aaFirst, aaLast]}
        moveEdge={move}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-up-72")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-move-up-72"));
    fireEvent.click(screen.getByTestId("relationships-move-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-moved")).toBeNull();
    expect(edgeOrder()).toEqual([
      "relationships-edge-71",
      "relationships-edge-72",
    ]);
    expect(move).toHaveBeenCalledWith(72, "UP");
  }

  it("HTTP 400 keeps the previous relationship order (#5201)", async () => {
    await expectMoveKeepsOrder(400);
  });

  it("HTTP 403 keeps the previous relationship order (#5201)", async () => {
    await expectMoveKeepsOrder(403);
  });

  it("HTTP 409 keeps the previous relationship order (#5201)", async () => {
    await expectMoveKeepsOrder(409);
  });

  it("move controls pass the zero serious/critical axe-core gate (#5201)", async () => {
    const { container } = render(
      <RelationshipsView
        item={{ id: "42", folderPath: "/p" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [aaFirst, aaLast]}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-down-71")).toBeTruthy(),
    );
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("relationships-move-down-71"));
    await renderA11yGate(container);
  });

  const relatedContent = {
    relationshipId: 81,
    configName: "Translation",
    category: "rs_translation",
    dependentId: 9,
    label: "Related page",
  };
  const relatedFolder = {
    relationshipId: 82,
    configName: "Folder",
    category: "rs_folder",
    dependentId: 3,
    label: "Folder row",
  };
  const relatedNoContent = {
    relationshipId: 83,
    configName: "Translation",
    category: "rs_translation",
    dependentId: 0,
    label: "No content id",
  };

  function renderRelatedList(
    openRelated: ReturnType<typeof vi.fn> = vi.fn(async () => ({
      ok: true,
      reason: "opened" as const,
    })),
  ) {
    render(
      <RelationshipsView
        item={{ id: "42", path: "/Sites/Foo/page" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [relatedContent, relatedFolder, relatedNoContent]}
        openRelated={openRelated}
        reserveRelatedWindow={() => null}
      />,
    );
    return openRelated;
  }

  it("cancel and Escape do not open the related item (#5218)", async () => {
    const openRelated = renderRelatedList(vi.fn());
    await waitFor(() =>
      expect(screen.getByTestId("relationships-open-81")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-open-82")).toBeNull();
    expect(screen.queryByTestId("relationships-open-83")).toBeNull();
    fireEvent.click(screen.getByTestId("relationships-open-81"));
    expect(screen.getByTestId("relationships-open-dialog")).toBeTruthy();
    fireEvent.click(screen.getByTestId("relationships-open-cancel"));
    expect(screen.queryByTestId("relationships-open-dialog")).toBeNull();
    expect(screen.queryByTestId("relationships-opened")).toBeNull();
    expect(openRelated).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("relationships-open-81"));
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("relationships-open-dialog")).toBeNull();
    expect(screen.queryByTestId("relationships-opened")).toBeNull();
    expect(openRelated).not.toHaveBeenCalled();
    expect(screen.getByTestId("relationships-edge-81")).toBeTruthy();
  });

  it("confirm opens EditorHost only after the related content id is known (#5218)", async () => {
    const openRelated = renderRelatedList();
    await waitFor(() =>
      expect(screen.getByTestId("relationships-open-81")).toHaveAttribute(
        "data-content-id",
        "9",
      ),
    );
    fireEvent.click(screen.getByTestId("relationships-open-81"));
    fireEvent.click(screen.getByTestId("relationships-open-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-opened")).toBeTruthy(),
    );
    expect(openRelated).toHaveBeenCalledWith(
      { contentId: 9, folder: false },
      { reservedWindow: null },
    );
    expect(screen.queryByTestId("relationships-open-error")).toBeNull();
    expect(screen.getByTestId("relationships-edge-81")).toBeTruthy();
    expect(screen.getByTestId("relationships-folder-82")).toBeTruthy();
  });

  it("HTTP 403 and 404 do not claim the related item opened (#5218)", async () => {
    const openRelated = renderRelatedList(
      vi
        .fn()
        .mockResolvedValueOnce({ ok: false, reason: "forbidden" })
        .mockResolvedValueOnce({ ok: false, reason: "not_found" }),
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-open-81")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-open-81"));
    fireEvent.click(screen.getByTestId("relationships-open-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-open-error")).toHaveTextContent(
        "HTTP 403",
      ),
    );
    expect(screen.queryByTestId("relationships-opened")).toBeNull();
    expect(screen.getByText(/Sites\/Foo\/page/)).toBeTruthy();

    fireEvent.click(screen.getByTestId("relationships-open-81"));
    fireEvent.click(screen.getByTestId("relationships-open-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-open-error")).toHaveTextContent(
        "HTTP 404",
      ),
    );
    expect(screen.queryByTestId("relationships-opened")).toBeNull();
    expect(openRelated).toHaveBeenCalledTimes(2);
  });

  it("open dialog passes the zero serious/critical axe-core gate (#5218)", async () => {
    const { container } = render(
      <RelationshipsView
        item={{ id: "42", folderPath: "/p" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [relatedContent, relatedFolder, relatedNoContent]}
        reserveRelatedWindow={() => null}
        openRelated={vi.fn()}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-open-81")).toBeTruthy(),
    );
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("relationships-open-81"));
    await renderA11yGate(container);
  });

  const slotted = {
    relationshipId: 71,
    configName: "ActiveAssembly",
    category: "rs_activeassembly",
    dependentId: 4,
    label: "AA first",
    slotId: 5,
    sortRank: 0,
    templateId: 4,
    templateName: "Brief",
  };
  const slottedSibling = {
    relationshipId: 72,
    configName: "ActiveAssembly",
    category: "rs_activeassembly",
    dependentId: 5,
    label: "AA last",
    slotId: 5,
    sortRank: 1,
    templateId: 4,
    templateName: "Brief",
  };
  const folderEdge = {
    relationshipId: 82,
    configName: "Folder",
    category: "rs_folder",
    dependentId: 3,
    label: "Folder row",
    slotId: 5,
    templateId: 4,
    templateName: "Brief",
  };
  const allowed = [
    { id: 4, name: "brief", label: "Brief" },
    { id: 8, name: "full", label: "Full story" },
  ];

  function templateText(relationshipId: number): string {
    return (
      screen
        .getByTestId(`relationships-template-${relationshipId}`)
        .textContent ?? ""
    );
  }

  it("a folder relationship has no snippet template control (#5219)", async () => {
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [folderEdge, translationEdge]}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-82")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-change-template-82")).toBeNull();
    expect(screen.queryByTestId("relationships-change-template-73")).toBeNull();
    expect(screen.queryByTestId("relationships-template-changed")).toBeNull();
  });

  it("cancel and an empty choice do not change the snippet template (#5219)", async () => {
    const change = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, folderEdge]}
        loadAllowedTemplates={async () => allowed}
        changeTemplate={change}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-change-template-71")).toBeTruthy(),
    );
    expect(templateText(71)).toContain("Brief");
    fireEvent.click(screen.getByTestId("relationships-change-template-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Brief" })).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-template-confirm"));
    expect(change).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-template-changed")).toBeNull();
    expect(screen.getByTestId("relationships-template-error")).toBeTruthy();
    expect(templateText(71)).toContain("Brief");

    fireEvent.change(screen.getByTestId("relationships-template-select"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("relationships-template-cancel"));
    expect(change).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-template-dialog")).toBeNull();
    expect(templateText(71)).toContain("Brief");
  });

  it("confirm writes one template and shows it only after success (#5219)", async () => {
    let release: (value: {
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }) => void = () => {};
    const gate = new Promise<{
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }>((resolve) => {
      release = resolve;
    });
    const change = vi.fn().mockReturnValue(gate);
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling]}
        loadAllowedTemplates={async () => allowed}
        changeTemplate={change}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-change-template-71")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-change-template-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Full story" })).toBeTruthy(),
    );
    fireEvent.change(screen.getByTestId("relationships-template-select"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("relationships-template-confirm"));
    expect(change).toHaveBeenCalledWith(71, 5, 8, 0);
    expect(templateText(71)).toContain("Brief");
    expect(screen.queryByTestId("relationships-template-changed")).toBeNull();
    release({
      relationshipId: 91,
      ownerId: 42,
      dependentId: 4,
      slotId: 5,
      templateId: 8,
      sortRank: 0,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-template-changed")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-edge-71")).toBeNull();
    expect(templateText(91)).toContain("Full story");
    expect(screen.getByTestId("relationships-template-91")).toHaveAttribute(
      "data-template-id",
      "8",
    );
    expect(templateText(72)).toContain("Brief");
    expect(change).toHaveBeenCalledTimes(1);
  });

  async function expectTemplateStays(status: number): Promise<void> {
    const change = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error("no"), { status }));
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted]}
        loadAllowedTemplates={async () => allowed}
        changeTemplate={change}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-change-template-71")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-change-template-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Full story" })).toBeTruthy(),
    );
    fireEvent.change(screen.getByTestId("relationships-template-select"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("relationships-template-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-template-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-template-changed")).toBeNull();
    expect(templateText(71)).toContain("Brief");
    expect(screen.getByTestId("relationships-template-71")).toHaveAttribute(
      "data-template-id",
      "4",
    );
    expect(change).toHaveBeenCalledWith(71, 5, 8, 0);
  }

  it("HTTP 400 leaves the previous snippet template (#5219)", async () => {
    await expectTemplateStays(400);
  });

  it("HTTP 403 leaves the previous snippet template (#5219)", async () => {
    await expectTemplateStays(403);
  });

  it("HTTP 409 leaves the previous snippet template (#5219)", async () => {
    await expectTemplateStays(409);
  });

  it("template dialog passes the zero serious/critical axe-core gate (#5219)", async () => {
    const { container } = render(
      <RelationshipsView
        item={{ id: "42", folderPath: "/p" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, folderEdge]}
        loadAllowedTemplates={async () => allowed}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-change-template-71")).toBeTruthy(),
    );
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("relationships-change-template-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Full story" })).toBeTruthy(),
    );
    await renderA11yGate(container);
  });

  const pageSlots = [
    { slotId: 5, name: "sidebar", label: "Sidebar", items: [] },
    { slotId: 9, name: "list", label: "List", items: [] },
  ];
  const noSlot = {
    relationshipId: 74,
    configName: "ActiveAssembly",
    category: "rs_activeassembly",
    dependentId: 6,
    label: "AA no slot",
    slotId: 0,
    templateId: 4,
    templateName: "Brief",
  };

  function slotOf(relationshipId: number): string | null {
    return screen
      .getByTestId(`relationships-edge-${relationshipId}`)
      .getAttribute("data-slot-id");
  }

  it("a folder and a slot with no relationship do not offer a move (#5265)", async () => {
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [folderEdge, translationEdge, noSlot]}
        loadPageSlots={async () => pageSlots}
        moveToSlot={vi.fn()}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-82")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-move-slot-82")).toBeNull();
    expect(screen.queryByTestId("relationships-move-slot-73")).toBeNull();
    expect(screen.queryByTestId("relationships-move-slot-74")).toBeNull();
    expect(screen.queryByTestId("relationships-slot-moved")).toBeNull();
    expect(screen.queryByTestId("relationships-slot-group-5")).toBeNull();
  });

  it("cancel and an empty slot choice do not move the relationship (#5265)", async () => {
    const move = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling, folderEdge]}
        loadPageSlots={async () => pageSlots}
        moveToSlot={move}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-slot-71")).toBeTruthy(),
    );
    expect(slotOf(71)).toBe("5");
    expect(screen.getByTestId("relationships-slot-group-5")).toBeTruthy();
    fireEvent.click(screen.getByTestId("relationships-move-slot-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "List" })).toBeTruthy(),
    );
    expect(screen.queryByRole("option", { name: "Sidebar" })).toBeNull();
    fireEvent.click(screen.getByTestId("relationships-slot-move-confirm"));
    expect(move).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-slot-moved")).toBeNull();
    expect(screen.getByTestId("relationships-slot-move-error")).toBeTruthy();
    expect(slotOf(71)).toBe("5");

    fireEvent.change(screen.getByTestId("relationships-slot-move-select"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("relationships-slot-move-cancel"));
    expect(move).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-slot-move-dialog")).toBeNull();
    expect(slotOf(71)).toBe("5");
    expect(screen.queryByTestId("relationships-slot-group-9")).toBeNull();
  });

  it("confirm lists the item in the destination slot only after success (#5265)", async () => {
    let release: (value: {
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }) => void = () => {};
    const gate = new Promise<{
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }>((resolve) => {
      release = resolve;
    });
    const move = vi.fn().mockReturnValue(gate);
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling]}
        loadPageSlots={async () => pageSlots}
        moveToSlot={move}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-slot-71")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-move-slot-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "List" })).toBeTruthy(),
    );
    fireEvent.change(screen.getByTestId("relationships-slot-move-select"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("relationships-slot-move-confirm"));
    expect(move).toHaveBeenCalledWith(71, 9, 4);
    expect(slotOf(71)).toBe("5");
    expect(slotOf(72)).toBe("5");
    expect(screen.queryByTestId("relationships-slot-moved")).toBeNull();
    expect(screen.queryByTestId("relationships-slot-group-9")).toBeNull();
    release({
      relationshipId: 91,
      ownerId: 42,
      dependentId: 4,
      slotId: 9,
      templateId: 4,
      sortRank: 0,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-slot-moved")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-edge-71")).toBeNull();
    expect(slotOf(91)).toBe("9");
    expect(slotOf(72)).toBe("5");
    expect(screen.getByTestId("relationships-slot-group-9")).toBeTruthy();
    expect(
      screen.getByTestId("relationships-slot-91").getAttribute("data-slot-id"),
    ).toBe("9");
    expect(templateText(91)).toContain("Brief");
    expect(move).toHaveBeenCalledTimes(1);
  });

  async function expectSlotStays(status: number): Promise<void> {
    const move = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error("no"), { status }));
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling]}
        loadPageSlots={async () => pageSlots}
        moveToSlot={move}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-slot-71")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-move-slot-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "List" })).toBeTruthy(),
    );
    fireEvent.change(screen.getByTestId("relationships-slot-move-select"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("relationships-slot-move-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-slot-move-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-slot-moved")).toBeNull();
    expect(slotOf(71)).toBe("5");
    expect(slotOf(72)).toBe("5");
    expect(screen.queryByTestId("relationships-slot-group-9")).toBeNull();
    expect(move).toHaveBeenCalledWith(71, 9, 4);
  }

  it("HTTP 400 leaves the relationship on the previous slot (#5265)", async () => {
    await expectSlotStays(400);
  });

  it("HTTP 403 leaves the relationship on the previous slot (#5265)", async () => {
    await expectSlotStays(403);
  });

  it("HTTP 409 leaves the relationship on the previous slot (#5265)", async () => {
    await expectSlotStays(409);
  });

  it("move-to-slot dialog passes the zero serious/critical axe-core gate (#5265)", async () => {
    const { container } = render(
      <RelationshipsView
        item={{ id: "42", folderPath: "/p" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, folderEdge]}
        loadPageSlots={async () => pageSlots}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-move-slot-71")).toBeTruthy(),
    );
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("relationships-move-slot-71"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "List" })).toBeTruthy(),
    );
    await renderA11yGate(container);
  });

  function idsInSlot(slotId: number): string[] {
    const group = screen.queryByTestId(`relationships-slot-group-${slotId}`);
    if (!group) {
      return [];
    }
    return Array.from(group.querySelectorAll("[data-testid^='relationships-edge-']")).map(
      (el) => (el.getAttribute("data-testid") ?? "").replace("relationships-edge-", ""),
    );
  }

  it("a folder row is not a slot and does not link an item (#5266)", async () => {
    const link = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [folderEdge, translationEdge]}
        loadAllowedTemplates={async () => allowed}
        linkExisting={link}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-82")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-link-slot-5")).toBeNull();
    expect(screen.queryByTestId("relationships-link-slot-done")).toBeNull();
    fireEvent.click(screen.getByTestId("relationships-select-edge-82"));
    fireEvent.click(screen.getByTestId("relationships-link-existing"));
    expect(link).not.toHaveBeenCalled();
    expect(screen.getByTestId("relationships-link-slot-error")).toBeTruthy();
    expect(screen.queryByTestId("relationships-link-slot-dialog")).toBeNull();
    expect(screen.queryByTestId("relationships-link-slot-done")).toBeNull();
  });

  it("cancel and a blank or missing target do not link into the slot (#5266)", async () => {
    const link = vi.fn();
    const add = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling, folderEdge]}
        loadAllowedTemplates={async () => allowed}
        linkExisting={link}
        addEdge={add}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-link-slot-5")).toBeTruthy(),
    );
    expect(idsInSlot(5)).toEqual(["71", "72"]);
    fireEvent.click(screen.getByTestId("relationships-link-slot-5"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Full story" })).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-link-slot-confirm"));
    expect(link).not.toHaveBeenCalled();
    expect(add).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-link-slot-done")).toBeNull();
    expect(screen.getByTestId("relationships-link-slot-error")).toBeTruthy();
    expect(idsInSlot(5)).toEqual(["71", "72"]);

    fireEvent.change(screen.getByTestId("relationships-link-slot-target"), {
      target: { value: "not-an-item" },
    });
    fireEvent.change(screen.getByTestId("relationships-link-slot-template"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("relationships-link-slot-confirm"));
    expect(link).not.toHaveBeenCalled();
    expect(idsInSlot(5)).toEqual(["71", "72"]);

    fireEvent.change(screen.getByTestId("relationships-link-slot-target"), {
      target: { value: "88" },
    });
    fireEvent.click(screen.getByTestId("relationships-link-slot-cancel"));
    expect(link).not.toHaveBeenCalled();
    expect(add).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-link-slot-dialog")).toBeNull();
    expect(screen.queryByTestId("relationships-link-slot-done")).toBeNull();
    expect(idsInSlot(5)).toEqual(["71", "72"]);
  });

  it("lists the item in the selected slot only after the slot add succeeds (#5266)", async () => {
    let release: (value: {
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }) => void = () => {};
    const gate = new Promise<{
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }>((resolve) => {
      release = resolve;
    });
    const link = vi.fn().mockReturnValue(gate);
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling]}
        loadAllowedTemplates={async () => allowed}
        linkExisting={link}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-link-slot-5")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-select-slot-5"));
    fireEvent.click(screen.getByTestId("relationships-link-existing"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Brief" })).toBeTruthy(),
    );
    fireEvent.change(screen.getByTestId("relationships-link-slot-target"), {
      target: { value: "88" },
    });
    fireEvent.change(screen.getByTestId("relationships-link-slot-template"), {
      target: { value: "4" },
    });
    fireEvent.click(screen.getByTestId("relationships-link-slot-confirm"));
    expect(link).toHaveBeenCalledWith({
      ownerId: 42,
      dependentId: 88,
      slotId: 5,
      templateId: 4,
    });
    expect(screen.queryByTestId("relationships-edge-91")).toBeNull();
    expect(screen.queryByTestId("relationships-link-slot-done")).toBeNull();
    expect(idsInSlot(5)).toEqual(["71", "72"]);
    release({
      relationshipId: 91,
      ownerId: 42,
      dependentId: 88,
      slotId: 5,
      templateId: 4,
      sortRank: 2,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-link-slot-done")).toBeTruthy(),
    );
    expect(idsInSlot(5)).toEqual(["71", "72", "91"]);
    expect(screen.getByTestId("relationships-edge-91")).toHaveAttribute(
      "data-dependent-id",
      "88",
    );
    expect(screen.getByTestId("relationships-edge-91")).toHaveAttribute(
      "data-slot-id",
      "5",
    );
    expect(link).toHaveBeenCalledTimes(1);
  });

  async function expectLinkStays(status: number): Promise<void> {
    const link = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error("no"), { status }));
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted]}
        loadAllowedTemplates={async () => allowed}
        linkExisting={link}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-link-slot-5")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("relationships-link-slot-5"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Full story" })).toBeTruthy(),
    );
    fireEvent.change(screen.getByTestId("relationships-link-slot-target"), {
      target: { value: "88" },
    });
    fireEvent.change(screen.getByTestId("relationships-link-slot-template"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("relationships-link-slot-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-link-slot-error")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-link-slot-done")).toBeNull();
    expect(screen.queryByTestId("relationships-edge-91")).toBeNull();
    expect(idsInSlot(5)).toEqual(["71"]);
    expect(link).toHaveBeenCalledTimes(1);
  }

  it("HTTP 400 does not list the item in the slot (#5266)", async () => {
    await expectLinkStays(400);
  });

  it("HTTP 403 does not list the item in the slot (#5266)", async () => {
    await expectLinkStays(403);
  });

  it("HTTP 409 does not list the item in the slot (#5266)", async () => {
    await expectLinkStays(409);
  });

  it("link-into-slot dialog passes the zero serious/critical axe-core gate (#5266)", async () => {
    const { container } = render(
      <RelationshipsView
        item={{ id: "42", folderPath: "/p" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, folderEdge]}
        loadAllowedTemplates={async () => allowed}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-link-slot-5")).toBeTruthy(),
    );
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("relationships-link-slot-5"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Brief" })).toBeTruthy(),
    );
    await renderA11yGate(container);
  });

  const pageTypes = [{ id: 3, name: "percPage", label: "Page" }];

  function renderCreate(
    extra: Partial<ComponentProps<typeof RelationshipsView>> = {},
  ) {
    return render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [slotted, slottedSibling, folderEdge]}
        loadAllowedTemplates={async () => allowed}
        loadAllowedTypes={async () => pageTypes}
        {...extra}
      />,
    );
  }

  async function openCreateDialog(): Promise<void> {
    fireEvent.click(screen.getByTestId("relationships-create-slot-5"));
    await waitFor(() =>
      expect(screen.getByRole("option", { name: "Page" })).toBeTruthy(),
    );
  }

  function fillCreate(template = "4"): void {
    fireEvent.change(screen.getByTestId("relationships-create-slot-type"), {
      target: { value: "percPage" },
    });
    fireEvent.change(screen.getByTestId("relationships-create-slot-folder"), {
      target: { value: "/Sites/Enterprise" },
    });
    fireEvent.change(screen.getByTestId("relationships-create-slot-template"), {
      target: { value: template },
    });
  }

  it("a folder row is not a slot and does not create an item (#5267)", async () => {
    const create = vi.fn();
    render(
      <RelationshipsView
        item={{ id: "42" }}
        loadServerSummary={mockLoad}
        loadEdges={async () => [folderEdge, translationEdge]}
        loadAllowedTypes={async () => pageTypes}
        loadAllowedTemplates={async () => allowed}
        createItem={create}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("relationships-edge-82")).toBeTruthy(),
    );
    expect(screen.queryByTestId("relationships-create-slot-5")).toBeNull();
    fireEvent.click(screen.getByTestId("relationships-create-in-slot"));
    expect(create).not.toHaveBeenCalled();
    expect(screen.getByTestId("relationships-create-slot-error")).toBeTruthy();
    expect(screen.queryByTestId("relationships-create-slot-dialog")).toBeNull();
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
  });

  it("cancel and a missing type, folder, or template do not create (#5267)", async () => {
    const create = vi.fn();
    const link = vi.fn();
    const open = vi.fn();
    renderCreate({ createItem: create, linkCreated: link, openCreated: open });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-5")).toBeTruthy(),
    );
    expect(idsInSlot(5)).toEqual(["71", "72"]);
    await openCreateDialog();
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    expect(create).not.toHaveBeenCalled();
    expect(link).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
    expect(screen.getByTestId("relationships-create-slot-error")).toBeTruthy();

    fireEvent.change(screen.getByTestId("relationships-create-slot-folder"), {
      target: { value: "/Sites/Enterprise" },
    });
    fireEvent.change(screen.getByTestId("relationships-create-slot-template"), {
      target: { value: "4" },
    });
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    expect(create).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("relationships-create-slot-type"), {
      target: { value: "percPage" },
    });
    fireEvent.change(screen.getByTestId("relationships-create-slot-folder"), {
      target: { value: "  " },
    });
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    expect(create).not.toHaveBeenCalled();

    fillCreate("9");
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    expect(create).not.toHaveBeenCalled();
    expect(idsInSlot(5)).toEqual(["71", "72"]);

    fillCreate("4");
    fireEvent.click(screen.getByTestId("relationships-create-slot-cancel"));
    expect(create).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-create-slot-dialog")).toBeNull();
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
  });

  it("lists the item and opens the editor only after the link succeeds (#5267)", async () => {
    let releaseCreate: (value: { itemId: string; name: string }) => void = () => {};
    const created = new Promise<{ itemId: string; name: string }>((resolve) => {
      releaseCreate = resolve;
    });
    let releaseLink: (value: {
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }) => void = () => {};
    const linked = new Promise<{
      relationshipId: number;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
      sortRank: number;
    }>((resolve) => {
      releaseLink = resolve;
    });
    const create = vi.fn().mockReturnValue(created);
    const link = vi.fn().mockReturnValue(linked);
    const linkExisting = vi.fn();
    const open = vi.fn().mockResolvedValue(true);
    renderCreate({
      createItem: create,
      linkCreated: link,
      linkExisting,
      openCreated: open,
      reserveCreatedWindow: () => null,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-5")).toBeTruthy(),
    );
    await openCreateDialog();
    fillCreate();
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    expect(create).toHaveBeenCalledWith({
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
    });
    expect(link).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-edge-91")).toBeNull();
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
    releaseCreate({ itemId: "1-101-99", name: "New page" });
    await waitFor(() =>
      expect(link).toHaveBeenCalledWith({
        ownerId: 42,
        dependentId: 99,
        slotId: 5,
        templateId: 4,
      }),
    );
    expect(open).not.toHaveBeenCalled();
    expect(idsInSlot(5)).toEqual(["71", "72"]);
    releaseLink({
      relationshipId: 91,
      ownerId: 42,
      dependentId: 99,
      slotId: 5,
      templateId: 4,
      sortRank: 2,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-done")).toBeTruthy(),
    );
    expect(open).toHaveBeenCalledWith(
      { id: 99, mode: "edit" },
      { reservedWindow: null },
    );
    expect(linkExisting).not.toHaveBeenCalled();
    expect(idsInSlot(5)).toEqual(["71", "72", "91"]);
    expect(screen.getByTestId("relationships-edge-91").textContent).toContain(
      "New page",
    );
    expect(screen.getByTestId("relationships-edge-91")).toHaveAttribute(
      "data-dependent-id",
      "99",
    );
    expect(screen.getByTestId("relationships-edge-91")).toHaveAttribute(
      "data-slot-id",
      "5",
    );
  });

  async function expectCreateStays(status: number): Promise<void> {
    const create = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error("no"), { status }));
    const link = vi.fn();
    const open = vi.fn();
    const reserved = { closed: false, close: vi.fn() };
    renderCreate({
      createItem: create,
      linkCreated: link,
      openCreated: open,
      reserveCreatedWindow: () => reserved as unknown as Window,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-5")).toBeTruthy(),
    );
    await openCreateDialog();
    fillCreate();
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-error")).toBeTruthy(),
    );
    expect(link).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
    expect(reserved.close).toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
    expect(screen.queryByTestId("relationships-edge-91")).toBeNull();
    expect(idsInSlot(5)).toEqual(["71", "72"]);
  }

  it("HTTP 400 does not list a created item in the slot (#5267)", async () => {
    await expectCreateStays(400);
  });

  it("HTTP 403 does not list a created item in the slot (#5267)", async () => {
    await expectCreateStays(403);
  });

  it("HTTP 409 does not list a created item in the slot (#5267)", async () => {
    await expectCreateStays(409);
  });

  it("a failed link does not list the item or open the editor (#5267)", async () => {
    const create = vi.fn().mockResolvedValue({
      itemId: "1-101-99",
      name: "New page",
    });
    const link = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error("no"), { status: 409 }));
    const open = vi.fn();
    renderCreate({ createItem: create, linkCreated: link, openCreated: open });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-5")).toBeTruthy(),
    );
    await openCreateDialog();
    fillCreate();
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-error")).toBeTruthy(),
    );
    expect(create).toHaveBeenCalledTimes(1);
    expect(link).toHaveBeenCalledTimes(1);
    expect(open).not.toHaveBeenCalled();
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
    expect(screen.queryByTestId("relationships-edge-91")).toBeNull();
  });

  it("does not claim success when the editor does not open (#5267)", async () => {
    const open = vi.fn().mockResolvedValue(false);
    renderCreate({
      createItem: async () => ({ itemId: "99", name: "New page" }),
      linkCreated: async (request) => ({
        relationshipId: 91,
        ownerId: request.ownerId,
        dependentId: request.dependentId,
        slotId: request.slotId,
        templateId: request.templateId,
        sortRank: 1,
      }),
      openCreated: open,
    });
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-5")).toBeTruthy(),
    );
    await openCreateDialog();
    fillCreate();
    fireEvent.click(screen.getByTestId("relationships-create-slot-confirm"));
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-error")).toBeTruthy(),
    );
    expect(open).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("relationships-create-slot-done")).toBeNull();
    expect(idsInSlot(5)).toEqual(["71", "72", "91"]);
  });

  it("create-in-slot dialog passes the zero serious/critical axe-core gate (#5267)", async () => {
    const { container } = renderCreate();
    await waitFor(() =>
      expect(screen.getByTestId("relationships-create-slot-5")).toBeTruthy(),
    );
    await renderA11yGate(container);
    await openCreateDialog();
    await renderA11yGate(container);
  });
});
