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

import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ItemPublishNowPanel } from "@/publishing/components/ItemPublishNowPanel";
import { publishSelectedItem } from "@/contentExplorer/itemPublish";
import { renderA11yGate } from "../contentExplorer/a11y";

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    publishSelectedItem: vi.fn(),
  };
});

const publishNow = vi.mocked(publishSelectedItem);

/**
 * Single wall-clock budget for the publish-now batch round trips (#7202).
 * Every round trip here is purely microtask-driven (resolved/rejected
 * `publishSelectedItem` promises → state commits → re-render, plus the
 * passive focus effect on the submitting transition), so the test drains
 * them deterministically with {@code act} and keeps exactly one polling
 * gate as a backstop for loaded-worker scheduling lag — never the 1s
 * testing-library defaults that expire under full-suite CPU contention
 * (same family as #7122 / #7152). Every {@code waitFor} in this file uses
 * it explicitly.
 */
const ITEM_PUBLISH_NOW_BATCH_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7202, same load-contention rationale as
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The RTL budgets above
 * plus the axe gate can jointly exceed the 20s global default when the
 * worker host is saturated. The primary gates stay progress-based (act
 * drains + bounded single backstops); this ceiling is strictly a
 * last-resort backstop.
 */
const ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT = 30_000;

describe("ItemPublishNowPanel batch retry", () => {
  beforeEach(() => {
    publishNow.mockReset();
    publishNow.mockResolvedValue(true);
  });

  async function reviewBatch(ids: string): Promise<void> {
    render(<ItemPublishNowPanel />);
    fireEvent.change(screen.getByTestId("item-publish-now-id"), {
      target: { value: ids },
    });
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-publish-now-confirm")).toBeTruthy();
      },
      { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
    );
  }

  it(
    "reviews several ids with one confirm listing each id",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      await reviewBatch("42, 43");
      expect(
        screen.getByTestId("item-publish-now-batch-item-42"),
      ).toBeTruthy();
      expect(
        screen.getByTestId("item-publish-now-batch-item-43"),
      ).toBeTruthy();
      expect(
        screen.getByTestId("item-publish-now-confirm").textContent,
      ).toContain("2");
      expect(publishNow).not.toHaveBeenCalled();
    },
  );

  it(
    "publishes every id and reports the full run",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      const onPublished = vi.fn();
      render(<ItemPublishNowPanel onPublished={onPublished} />);
      fireEvent.change(screen.getByTestId("item-publish-now-id"), {
        target: { value: "42 43" },
      });
      fireEvent.click(screen.getByTestId("item-publish-now-review"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain (#7202): the batch round trip is purely
      // microtask-driven — the resolved publish promises settle and the
      // success/report states commit — with no real timers involved.
      // Flushing with act instead of asserting synchronously removes the
      // starvation cliff under full-suite load.
      await act(async () => {});
      expect(publishNow).toHaveBeenCalledTimes(2);
      expect(
        screen.getByTestId("item-publish-now-success").textContent,
      ).toContain("all 2 items");
      expect(
        screen.getByTestId("item-publish-now-batch-report-item-42")
          .textContent,
      ).toContain("publish started");
      expect(screen.queryByTestId("item-publish-now-retry")).toBeNull();
      expect(onPublished).toHaveBeenCalled();
    },
  );

  it(
    "names a per-item failure and offers retry for the failed id",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      publishNow
        .mockResolvedValueOnce(true)
        .mockRejectedValueOnce({
          status: 403,
          statusText: "Forbidden",
          body: null,
        });
      await reviewBatch("42,43");
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-error")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the partial-failure commit (#7202).
      await act(async () => {});
      expect(screen.queryByTestId("item-publish-now-success")).toBeNull();
      expect(screen.getByTestId("item-publish-now-error").textContent).toContain(
        "Published 1 of 2",
      );
      expect(screen.getByTestId("item-publish-now-error").textContent).toContain(
        "43 (Publish Forbidden (HTTP 403 Forbidden))",
      );
      expect(
        screen.getByTestId("item-publish-now-batch-report-item-43")
          .textContent,
      ).toContain("43");
      expect(screen.getByTestId("item-publish-now-retry")).toBeTruthy();
    },
  );

  it(
    "retry republishes only the failed ids and clears the report",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      publishNow
        .mockResolvedValueOnce(true)
        .mockRejectedValueOnce({
          status: 409,
          statusText: "Conflict",
          body: null,
        });
      await reviewBatch("42,43");
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-retry")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the first-run commit before counting calls
      // (#7202).
      await act(async () => {});
      expect(publishNow).toHaveBeenCalledTimes(2);
      publishNow.mockReset();
      publishNow.mockResolvedValue(true);
      fireEvent.click(screen.getByTestId("item-publish-now-retry"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the retry-run commit (#7202).
      await act(async () => {});
      expect(publishNow).toHaveBeenCalledTimes(1);
      expect(publishNow.mock.calls[0]?.[0]).toMatchObject({ id: "43" });
      expect(
        screen.getByTestId("item-publish-now-success").textContent,
      ).toContain("Publish now started for 1 item");
      expect(screen.queryByTestId("item-publish-now-retry")).toBeNull();
      expect(
        screen.getByTestId("item-publish-now-batch-report-item-43")
          .textContent,
      ).toContain("publish started");
    },
  );

  it(
    "retry on a single failed publish resubmits the same item",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      publishNow.mockRejectedValueOnce({
        status: 404,
        statusText: "Not Found",
        body: null,
      });
      render(<ItemPublishNowPanel itemId="42" />);
      fireEvent.click(screen.getByTestId("item-publish-now-review"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-retry")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the failed-run commit (#7202).
      await act(async () => {});
      publishNow.mockReset();
      publishNow.mockResolvedValue(true);
      fireEvent.click(screen.getByTestId("item-publish-now-retry"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the retry-run commit (#7202).
      await act(async () => {});
      expect(publishNow).toHaveBeenCalledTimes(1);
      expect(publishNow.mock.calls[0]?.[0]).toMatchObject({ id: "42" });
    },
  );

  it(
    "keeps a multi-id lookup out of the shared workspace item",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      const onItemIdChange = vi.fn();
      render(<ItemPublishNowPanel onItemIdChange={onItemIdChange} />);
      fireEvent.change(screen.getByTestId("item-publish-now-id"), {
        target: { value: "42, 43" },
      });
      fireEvent.click(screen.getByTestId("item-publish-now-review"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-confirm")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      expect(onItemIdChange).not.toHaveBeenCalled();
    },
  );
});

describe("ItemPublishNowPanel loading announcements (#7087)", () => {
  beforeEach(() => {
    publishNow.mockReset();
    publishNow.mockResolvedValue(true);
  });

  async function reviewTwoIds(): Promise<void> {
    render(<ItemPublishNowPanel />);
    fireEvent.change(screen.getByTestId("item-publish-now-id"), {
      target: { value: "42 43" },
    });
    fireEvent.click(screen.getByTestId("item-publish-now-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
      },
      { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
    );
  }

  it(
    "announces the run as a polite live status and marks the panel busy",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      let resolveFirst!: (value: boolean) => void;
      publishNow.mockImplementationOnce(
        () =>
          new Promise<boolean>((resolve) => {
            resolveFirst = resolve;
          }),
      );
      await reviewTwoIds();
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-loading")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      expect(screen.getByTestId("item-publish-now-loading")).toHaveAttribute(
        "role",
        "status",
      );
      expect(screen.getByTestId("item-publish-now-busy")).toHaveAttribute(
        "aria-busy",
        "true",
      );
      resolveFirst(true);
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the run-completion commit (#7202).
      await act(async () => {});
      // The loading region stays mounted with empty text after the run so
      // the next run announces as a text change, not a mount-with-text.
      expect(screen.getByTestId("item-publish-now-loading")).toHaveTextContent(
        "",
      );
      expect(screen.getByTestId("item-publish-now-busy")).toHaveAttribute(
        "aria-busy",
        "false",
      );
    },
  );

  it(
    "keeps the loading live region mounted with empty text before the run",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      render(<ItemPublishNowPanel />);
      const loading = screen.getByTestId("item-publish-now-loading");
      expect(loading).toHaveAttribute("role", "status");
      expect(loading).toHaveTextContent("");
    },
  );

  it(
    "moves focus to the announced result when focus stayed in the panel",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      await reviewTwoIds();
      screen.getByTestId("item-publish-now-submit").focus();
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain (#7202): the focus move runs in a passive
      // effect on the submitting true→false transition, after the success
      // text commits. A bare 1s success gate could expire under
      // full-suite CPU contention before the batch promises settled, and
      // even once the text lands the focus effect still needs a flush —
      // without it the assertion races the effect and activeElement is
      // still the submit button. Flushing with act settles the effect
      // without racing worker lag.
      await act(async () => {});
      expect(document.activeElement).toBe(
        screen.getByTestId("item-publish-now-success"),
      );
    },
  );

  it(
    "moves focus to the error result on a failed run",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      publishNow.mockRejectedValueOnce({
        status: 403,
        statusText: "Forbidden",
        body: null,
      });
      render(<ItemPublishNowPanel itemId="42" />);
      fireEvent.click(screen.getByTestId("item-publish-now-review"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-submit")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      screen.getByTestId("item-publish-now-submit").focus();
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-error")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the failure commit + focus effect (#7202,
      // same rationale as the success-focus test above).
      await act(async () => {});
      expect(document.activeElement).toBe(
        screen.getByTestId("item-publish-now-error"),
      );
    },
  );

  it(
    "leaves focus alone when the run was not started from inside the panel",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      let resolveFirst!: (value: boolean) => void;
      publishNow.mockImplementationOnce(
        () =>
          new Promise<boolean>((resolve) => {
            resolveFirst = resolve;
          }),
      );
      await reviewTwoIds();
      // fireEvent.click does not take focus, so the run starts with focus
      // outside the panel — completion must not yank it back.
      expect(
        screen.getByTestId("item-publish-now").contains(document.activeElement),
      ).toBe(false);
      fireEvent.click(screen.getByTestId("item-publish-now-submit"));
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-loading")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      resolveFirst(true);
      await waitFor(
        () => {
          expect(screen.getByTestId("item-publish-now-success")).toBeTruthy();
        },
        { timeout: ITEM_PUBLISH_NOW_BATCH_TIMEOUT },
      );
      // Deterministic drain for the run-completion commit (#7202).
      await act(async () => {});
      expect(document.activeElement).not.toBe(
        screen.getByTestId("item-publish-now-success"),
      );
    },
  );

  it(
    "renders with zero serious/critical a11y violations (#7087)",
    { timeout: ITEM_PUBLISH_NOW_BATCH_TEST_TIMEOUT },
    async () => {
      const { container } = render(<ItemPublishNowPanel />);
      await renderA11yGate(container);
    },
  );
});
