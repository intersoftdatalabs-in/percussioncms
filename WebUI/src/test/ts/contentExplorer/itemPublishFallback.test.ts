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

vi.mock("../../../main/ts/publishing/publishActions", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/publishing/publishActions")>();
  return {
    ...actual,
    mapPublishResponse: () => ({ state: "error", token: "", message: "" }),
  };
});

import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import {
  publishSelectedItem,
  takedownSelectedItem,
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
