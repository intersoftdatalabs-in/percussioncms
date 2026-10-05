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
});
