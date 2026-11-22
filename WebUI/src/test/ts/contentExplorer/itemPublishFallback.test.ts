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

/**
 * Keyed-message coverage for the Explorer demand-publish / demand-takedown
 * preflight throw fallbacks (#6987, parent #4532).
 *
 * <p>Production {@code mapPublishResponse} always fills {@code message} /
 * {@code token} today, so the fallbacks are defensive — the same shape as
 * the editor peer ({@code editorPublish.ts}, #6932). This spec forces the
 * empty-preflight branch via a module mock and proves the thrown text is
 * catalog-resolved ({@code message(EXPLORER_MSG.*)}) rather than hardcoded
 * English: a stub session catalog returns a translated marker, and the
 * no-catalog path keeps the en-us default byte-identical.</p>
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

const findChildren = vi.hoisted(() => vi.fn());

vi.mock("../../../main/ts/api/contentExplorer/pathApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/contentExplorer/pathApi")>();
  return { ...actual, findChildren };
});

vi.mock("../../../main/ts/publishing/publishActions", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/publishing/publishActions")>();
  return {
    ...actual,
    mapPublishResponse: () => ({ state: "error", token: "", message: "" }),
  };
});

import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";
import {
  publishSelectedFolder,
  publishSelectedItem,
  publishSelectedItems,
  removeFromStagingSelectedItem,
  stageSelectedItem,
  takedownSelectedItem,
  takedownSelectedItems,
} from "../../../main/ts/contentExplorer/itemPublish";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "page",
    path: "/Sites/Demo/page",
    type: "percPage",
    id: "42",
    ...overrides,
  };
}

function okFetch(): void {
  // Fresh Response per call — a reused body is "unusable" on the second read.
  vi.spyOn(global, "fetch").mockImplementation(
    async () =>
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
  );
}

afterEach(() => {
  findChildren.mockReset();
  vi.restoreAllMocks();
  delete (window as unknown as Record<string, unknown>).I18N;
});

describe("demand-publish preflight throw fallback (#6987)", () => {
  it("resolves the publish fallback through the session catalog, not hardcoded English", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) =>
        key === EXPLORER_MSG.DEMAND_PUBLISH_FAILED
          ? "Échec de publication (catalogue)"
          : key,
    };
    okFetch();
    await expect(publishSelectedItem(item())).rejects.toThrow(
      "Échec de publication (catalogue)",
    );
  });

  it("resolves the takedown fallback through the session catalog, not hardcoded English", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) =>
        key === EXPLORER_MSG.DEMAND_TAKEDOWN_FAILED
          ? "Retrait (catalogue)"
          : key,
    };
    okFetch();
    await expect(takedownSelectedItem(item())).rejects.toThrow(
      "Retrait (catalogue)",
    );
  });

  it("keeps the en-us defaults byte-identical without a session catalog", async () => {
    okFetch();
    await expect(publishSelectedItem(item())).rejects.toThrow("Publish failed");
    await expect(takedownSelectedItem(item())).rejects.toThrow(
      "Takedown failed",
    );
  });
});

describe("batch-helper failure literals (#7056)", () => {
  type I18NWindow = Window & {
    I18N?: { message: (key: string) => string };
  };

  /** Catalog stub: every new batch key resolves to a locale marker. */
  function stubBatchCatalog(): void {
    (window as unknown as I18NWindow).I18N = {
      message: (key: string) => {
        switch (key) {
          case EXPLORER_MSG.BATCH_MISSING_CONTENT_ID:
            return "identifiant de contenu manquant (catalogue)";
          case EXPLORER_MSG.BATCH_NOT_PUBLISHED:
            return "non publié (catalogue)";
          case EXPLORER_MSG.BATCH_PUBLISH_FAILED:
            return "échec de publication (catalogue)";
          case EXPLORER_MSG.BATCH_NOT_TAKEN_DOWN:
            return "non retiré (catalogue)";
          case EXPLORER_MSG.BATCH_TAKEDOWN_FAILED:
            return "échec du retrait (catalogue)";
          case EXPLORER_MSG.BATCH_NOT_STAGED:
            return "non stagé (catalogue)";
          case EXPLORER_MSG.BATCH_NOT_REMOVED:
            return "non déstagé (catalogue)";
          default:
            return key;
        }
      },
    };
  }

  it("routes the publish batch fallbacks through the session catalog", async () => {
    stubBatchCatalog();
    vi.spyOn(global, "fetch").mockRejectedValue("kaput");
    const failed = await publishSelectedItems([item()]);
    expect(failed.failures).toHaveLength(1);
    expect(failed.failures[0]?.message).toBe(
      "échec de publication (catalogue)",
    );
  });

  it("routes the takedown batch fallbacks through the session catalog", async () => {
    stubBatchCatalog();
    vi.spyOn(global, "fetch").mockImplementation(async (input) => {
      if (String(input).includes("findLinkedItems")) {
        return new Response(JSON.stringify({ ArrayList: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      return Promise.reject("kaput");
    });
    const failed = await takedownSelectedItems([item()]);
    expect(failed.takenDownIds).toEqual([]);
    expect(failed.failures).toHaveLength(1);
    expect(failed.failures[0]?.message).toBe("échec du retrait (catalogue)");
  });

  it("routes missing-id and not-submitted rows through the session catalog", async () => {
    stubBatchCatalog();
    findChildren.mockResolvedValue([item({ id: "" })]);
    const folder = await publishSelectedFolder(
      item({
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        leaf: false,
      }),
    );
    expect(folder.failures).toHaveLength(1);
    expect(folder.failures[0]?.message).toBe(
      "identifiant de contenu manquant (catalogue)",
    );

    const notPublished = await publishSelectedItems([item({ id: "" })]);
    expect(notPublished.failures).toHaveLength(1);
    expect(notPublished.failures[0]?.message).toBe("non publié (catalogue)");

    const notTakenDown = await takedownSelectedItems([item({ id: "" })]);
    expect(notTakenDown.failures).toHaveLength(1);
    expect(notTakenDown.failures[0]?.message).toBe("non retiré (catalogue)");
  });

  it("keeps the batch en-us defaults byte-identical without a session catalog", async () => {
    vi.spyOn(global, "fetch").mockRejectedValue("kaput");
    const failed = await publishSelectedItems([item()]);
    expect(failed.failures[0]?.message).toBe("publish failed");

    findChildren.mockResolvedValue([item({ id: "" })]);
    const folder = await publishSelectedFolder(
      item({
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        leaf: false,
      }),
    );
    expect(folder.failures[0]?.message).toBe("missing content id");

    const notPublished = await publishSelectedItems([item({ id: "" })]);
    expect(notPublished.failures[0]?.message).toBe("not published");

    const notTakenDown = await takedownSelectedItems([item({ id: "" })]);
    expect(notTakenDown.failures[0]?.message).toBe("not taken down");

    // Folder-flow stage/unstage literals are defensive (a row with an id
    // always submits); the key wiring still resolves byte-identical.
    expect(message(EXPLORER_MSG.BATCH_NOT_STAGED)).toBe("not staged");
    expect(message(EXPLORER_MSG.BATCH_NOT_REMOVED)).toBe("not removed");
  });

  it("routes the defensive stage/unstage folder-flow keys through the catalog", () => {
    stubBatchCatalog();
    expect(message(EXPLORER_MSG.BATCH_NOT_STAGED)).toBe(
      "non stagé (catalogue)",
    );
    expect(message(EXPLORER_MSG.BATCH_NOT_REMOVED)).toBe(
      "non déstagé (catalogue)",
    );
  });
});

describe("staging preflight naming decision (#7056)", () => {
  it("surfaces Stage failed / Remove from staging failed instead of Publish failed", async () => {
    // The module mock above forces an empty preflight so the fallback fires.
    okFetch();
    await expect(stageSelectedItem(item())).rejects.toThrow("Stage failed");
    await expect(removeFromStagingSelectedItem(item())).rejects.toThrow(
      "Remove from staging failed",
    );
  });

  it("routes the staging preflight fallbacks through the session catalog", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) =>
        key === EXPLORER_MSG.BULK_STAGE_FAILED
          ? "Échec de staging (catalogue)"
          : key === EXPLORER_MSG.BULK_UNSTAGE_FAILED
            ? "Échec de déstaging (catalogue)"
            : key,
    };
    okFetch();
    await expect(stageSelectedItem(item())).rejects.toThrow(
      "Échec de staging (catalogue)",
    );
    await expect(removeFromStagingSelectedItem(item())).rejects.toThrow(
      "Échec de déstaging (catalogue)",
    );
  });
});
