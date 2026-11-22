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
 * Keyed-message coverage for the publishing Publish-Now / Takedown
 * batch-helper failure literals (#7071, parent #4532).
 *
 * <p>{@code itemPublishNowBatch.ts} / {@code itemTakedownBatch.ts} built
 * per-item failure text from hardcoded English ("not published",
 * "not taken down", "Review failed", "Take down failed"). Those literals are
 * now catalog-routed through new {@code MSG.PUBLISH_*_BATCH_*} keys with
 * byte-identical en-us defaults. This spec proves the routing: a stub
 * session catalog returns translated markers, and the no-catalog path keeps
 * the en-us defaults byte-identical (so the existing batch suites stay
 * green).</p>
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import { MSG } from "@/i18n/message";
import {
  reviewTakedownBatch,
  runTakedownBatch,
} from "@/publishing/itemTakedownBatch";
import { runPublishNowBatch } from "@/publishing/itemPublishNowBatch";

type I18NWindow = Window & {
  I18N?: { message: (key: string) => string };
};

/** Catalog stub: every new batch key resolves to a locale marker. */
function stubBatchCatalog(): void {
  (window as unknown as I18NWindow).I18N = {
    message: (key: string) => {
      switch (key) {
        case MSG.PUBLISH_NOW_BATCH_NOT_PUBLISHED:
          return "non publié (catalogue)";
        case MSG.PUBLISH_TAKEDOWN_BATCH_NOT_TAKEN_DOWN:
          return "non retiré (catalogue)";
        case MSG.PUBLISH_TAKEDOWN_BATCH_REVIEW_FAILED:
          return "échec de révision (catalogue)";
        case MSG.PUBLISH_TAKEDOWN_BATCH_RUN_FAILED:
          return "échec du retrait (catalogue)";
        default:
          return key;
      }
    },
  };
}

afterEach(() => {
  vi.restoreAllMocks();
  delete (window as unknown as I18NWindow).I18N;
});

describe("publishing batch-helper failure literals (#7071)", () => {
  it("routes the publish-now false-return row through the session catalog", async () => {
    stubBatchCatalog();
    const batch = await runPublishNowBatch(["42"], "page", async () => false);
    expect(batch.failures).toEqual([
      { id: "42", message: "non publié (catalogue)" },
    ]);
  });

  it("routes the takedown false-return row through the session catalog", async () => {
    stubBatchCatalog();
    const report = await runTakedownBatch(
      ["42"],
      "page",
      {},
      async () => false,
    );
    expect(report.failures).toEqual([
      { id: "42", message: "non retiré (catalogue)" },
    ]);
  });

  it("routes the takedown review fallback through the session catalog", async () => {
    stubBatchCatalog();
    const report = await reviewTakedownBatch(["42"], async () => {
      throw "kaput";
    });
    expect(report.failures).toEqual([
      { id: "42", message: "échec de révision (catalogue)" },
    ]);
  });

  it("routes the takedown run fallback through the session catalog", async () => {
    stubBatchCatalog();
    const report = await runTakedownBatch(["42"], "page", {}, async () => {
      throw "kaput";
    });
    expect(report.failures).toEqual([
      { id: "42", message: "échec du retrait (catalogue)" },
    ]);
  });

  it("keeps the en-us defaults byte-identical without a session catalog", async () => {
    const publishBatch = await runPublishNowBatch(
      ["42"],
      "page",
      async () => false,
    );
    expect(publishBatch.failures).toEqual([
      { id: "42", message: "not published" },
    ]);
    const review = await reviewTakedownBatch(["42"], async () => {
      throw "kaput";
    });
    expect(review.failures).toEqual([{ id: "42", message: "Review failed" }]);
    const run = await runTakedownBatch(["42"], "page", {}, async () => false);
    expect(run.failures).toEqual([{ id: "42", message: "not taken down" }]);
    const runThrow = await runTakedownBatch(
      ["42"],
      "page",
      {},
      async () => {
        throw "kaput";
      },
    );
    expect(runThrow.failures).toEqual([
      { id: "42", message: "Take down failed" },
    ]);
  });
});
