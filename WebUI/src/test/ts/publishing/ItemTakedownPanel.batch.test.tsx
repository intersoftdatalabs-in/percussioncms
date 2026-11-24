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

import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { get } from "@/api/client";
import { takedownSelectedItem } from "@/contentExplorer/itemPublish";
import { ItemTakedownPanel } from "@/publishing/components/ItemTakedownPanel";
import { renderA11yGate } from "../contentExplorer/a11y";

vi.mock("@/contentExplorer/itemPublish", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/contentExplorer/itemPublish")>();
  return {
    ...actual,
    takedownSelectedItem: vi.fn(),
  };
});

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
  };
});

const getMock = vi.mocked(get);
const takeDown = vi.mocked(takedownSelectedItem);

/**
 * Single wall-clock budget for every polling gate in this file (#7189).
 * Each round trip here is purely microtask-driven (mocked {@code get} /
 * {@code takedownSelectedItem} promises → setState → render → focus
 * effect) with no real timers involved, so the gates are deterministic in
 * outcome; the budget is strictly a backstop for loaded-worker scheduling
 * lag. Every {@code waitFor} uses it explicitly: the 1s testing-library
 * default is the most-tripped cliff when hundreds of suite files share
 * the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT} in
 * #7122 and {@code PICKER_MOUNT_TIMEOUT} in #7151).
 */
const TAKEDOWN_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7189, same load-contention rationale as
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The sequential
 * backstop gates above plus the axe gate can jointly exceed the 20s
 * global default when the worker host is saturated. The primary gates
 * stay progress-based (single bounded backstop per distinct state);
 * this ceiling is strictly a last-resort backstop.
 */
const TAKEDOWN_TEST_TIMEOUT = 30_000;

function linkedBody(path: string): unknown {
  return { ArrayList: [{ id: "7", pagePath: path }] };
}

describe("ItemTakedownPanel batch", () => {
  beforeEach(() => {
    getMock.mockReset();
    takeDown.mockReset();
    getMock.mockImplementation(async (url: string) =>
      linkedBody(`/Sites/Demo/${encodeURIComponent(url).length}`),
    );
    takeDown.mockResolvedValue(true);
  });

  async function reviewBatch(ids: string): Promise<void> {
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: ids },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    // Deterministic drain (#7189): the review path awaits mocked lookups
    // (resolved-promise mocks — microtask-only) before mounting the
    // confirm, so flushing with act settles the mount without wall-clock
    // polling. The bounded gate below is strictly a backstop for
    // loaded-worker lag.
    await act(async () => {});
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
  }

  it("reviews several ids with one confirm listing linked pages per item", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock
      .mockResolvedValueOnce(linkedBody("/Sites/Demo/Home"))
      .mockResolvedValueOnce({ ArrayList: [] });
    await reviewBatch("42, 43");
    expect(getMock).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("item-takedown-batch-item-42").textContent).toContain(
      "/Sites/Demo/Home",
    );
    expect(
      screen.getByTestId("item-takedown-batch-linked-empty-43"),
    ).toBeTruthy();
  });

  it("takes down every id and reports the full run", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    await reviewBatch("42 43");
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(takeDown).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("item-takedown-success").textContent).toContain(
      "Take down started for all 2 items",
    );
    expect(
      screen.getByTestId("item-takedown-batch-report-item-42").textContent,
    ).toContain("taken down");
  });

  it("names a per-item failure and offers retry for the failed id", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown
      .mockResolvedValueOnce(true)
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: null });
    await reviewBatch("42,43");
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-error")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("item-takedown-success")).toBeNull();
    expect(screen.getByTestId("item-takedown-error").textContent).toContain(
      "Taken down 1 of 2",
    );
    expect(screen.getByTestId("item-takedown-error").textContent).toContain(
      "43 (Publish Forbidden (HTTP 403 Forbidden))",
    );
    expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
  });

  it("retry takes down only the failed ids and clears the button", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown
      .mockResolvedValueOnce(true)
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: null });
    await reviewBatch("42,43");
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(takeDown).toHaveBeenCalledTimes(2);
    takeDown.mockReset();
    takeDown.mockResolvedValue(true);
    fireEvent.click(screen.getByTestId("item-takedown-retry"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(takeDown).toHaveBeenCalledTimes(1);
    expect(takeDown.mock.calls[0]?.[0]).toMatchObject({ id: "43" });
    expect(screen.getByTestId("item-takedown-success").textContent).toContain(
      "Take down started for 1 item",
    );
    expect(
      screen.getByTestId("item-takedown-batch-report-item-43").textContent,
    ).toContain("taken down");
    expect(screen.queryByTestId("item-takedown-retry")).toBeNull();
  });

  it("retry on a single failed takedown resubmits the same item", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown.mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ItemTakedownPanel itemId="42" />);
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-submit")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    takeDown.mockReset();
    takeDown.mockResolvedValue(true);
    fireEvent.click(screen.getByTestId("item-takedown-retry"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(takeDown).toHaveBeenCalledTimes(1);
  });

  it("partial review names failures and retry re-reads only the failed ids", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock
      .mockRejectedValueOnce({
        status: 500,
        statusText: "Server Error",
        body: null,
      })
      .mockResolvedValue(linkedBody("/Sites/Demo/Home"));
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-error")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    // The failed id still confirms with no linked pages and is named.
    expect(screen.getByTestId("item-takedown-error").textContent).toContain(
      "Reviewed 1 of 2",
    );
    expect(screen.getByTestId("item-takedown-error").textContent).toContain(
      "42 (",
    );
    expect(screen.getByTestId("item-takedown-retry")).toBeTruthy();
    expect(screen.getByTestId("item-takedown-batch-item-42")).toBeTruthy();
    expect(
      screen.getByTestId("item-takedown-batch-linked-empty-42"),
    ).toBeTruthy();
    // Retry re-reads only the failed id and merges into the same confirm.
    const callsBefore = getMock.mock.calls.length;
    expect(callsBefore).toBe(2);
    fireEvent.click(screen.getByTestId("item-takedown-retry"));
    await waitFor(
      () => {
        expect(screen.queryByTestId("item-takedown-retry")).toBeNull();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(getMock.mock.calls.length).toBe(callsBefore + 1);
    expect(String(getMock.mock.calls[callsBefore]?.[0])).toContain("42");
    expect(
      screen.getByTestId("item-takedown-batch-linked-42").textContent,
    ).toContain("/Sites/Demo/Home");
    // The already-reviewed id keeps its linked pages.
    expect(
      screen.getByTestId("item-takedown-batch-linked-43").textContent,
    ).toContain("/Sites/Demo/Home");
    expect(screen.queryByTestId("item-takedown-error")).toBeNull();
  });

  it("full review success shows the confirm with no retry", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    await reviewBatch("42, 43");
    expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
    expect(screen.queryByTestId("item-takedown-retry")).toBeNull();
    expect(screen.queryByTestId("item-takedown-error")).toBeNull();
  });

  it("single-item review stays on the single confirm with no retry", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue(linkedBody("/Sites/Demo/Home"));
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("item-takedown-retry")).toBeNull();
    expect(screen.getByTestId("item-takedown-linked").textContent).toContain(
      "/Sites/Demo/Home",
    );
  });

  it("keeps a multi-id lookup out of the shared workspace item", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    const onItemIdChange = vi.fn();
    render(<ItemTakedownPanel onItemIdChange={onItemIdChange} />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42, 43" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-confirm")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(onItemIdChange).not.toHaveBeenCalled();
  });
});

describe("ItemTakedownPanel loading announcements (#7087)", () => {
  beforeEach(() => {
    getMock.mockReset();
    takeDown.mockReset();
    takeDown.mockResolvedValue(true);
  });

  it("announces the linked-page lookup as a polite live status", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    let resolveLookup!: (value: unknown) => void;
    getMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveLookup = resolve;
        }),
    );
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-loading")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("item-takedown-loading")).toHaveAttribute(
      "role",
      "status",
    );
    expect(screen.getByTestId("item-takedown-busy")).toHaveAttribute(
      "aria-busy",
      "true",
    );
    resolveLookup(linkedBody("/Sites/Demo/Home"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-submit")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    // The loading region stays mounted with empty text after the lookup
    // so the next run announces as a text change, not a mount-with-text.
    expect(screen.getByTestId("item-takedown-loading")).toHaveTextContent(
      "",
    );
    expect(screen.getByTestId("item-takedown-busy")).toHaveAttribute(
      "aria-busy",
      "false",
    );
  });

  it("keeps the loading live region mounted with empty text before the run", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    render(<ItemTakedownPanel />);
    const loading = screen.getByTestId("item-takedown-loading");
    expect(loading).toHaveAttribute("role", "status");
    expect(loading).toHaveTextContent("");
  });

  it("announces the take-down run and moves focus to the result", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    let resolveRun!: (value: boolean) => void;
    takeDown.mockImplementationOnce(
      () =>
        new Promise<boolean>((resolve) => {
          resolveRun = resolve;
        }),
    );
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-submit")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    screen.getByTestId("item-takedown-submit").focus();
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    // The submit phase previously rendered no indicator at all; it now
    // announces through the same polite live region as the lookup.
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-loading")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("item-takedown-loading")).toHaveAttribute(
      "role",
      "status",
    );
    resolveRun(true);
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    // Poll the focus landing, not just the result mount (#7189): the
    // completion effect moves focus after the success commit, so a
    // single-shot assertion can race effect scheduling under full-suite
    // load while the mount gate above already passed.
    await waitFor(
      () => {
        expect(document.activeElement).toBe(
          screen.getByTestId("item-takedown-success"),
        );
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
  });

  it("moves focus to the error result on a failed run", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    takeDown.mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-submit")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    screen.getByTestId("item-takedown-submit").focus();
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    // Deterministic drain (#7189): the rejected-promise mock is
    // microtask-only, so flushing with act settles the error commit
    // without wall-clock polling. The bounded gate below is strictly a
    // backstop for loaded-worker lag.
    await act(async () => {});
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-error")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    // Poll the focus landing, not just the error mount (#7189): the
    // completion effect moves focus after the error commit, so a
    // single-shot assertion can race effect scheduling under full-suite
    // load while the mount gate above already passed.
    await waitFor(
      () => {
        expect(document.activeElement).toBe(
          screen.getByTestId("item-takedown-error"),
        );
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
  });

  it("leaves focus alone when the run was not started from inside the panel", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    getMock.mockResolvedValue({ ArrayList: [] });
    render(<ItemTakedownPanel />);
    fireEvent.change(screen.getByTestId("item-takedown-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-takedown-review"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-submit")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    // fireEvent.click does not take focus, so the run starts with focus
    // outside the panel — completion must not yank it back.
    expect(
      screen.getByTestId("item-takedown").contains(document.activeElement),
    ).toBe(false);
    fireEvent.click(screen.getByTestId("item-takedown-submit"));
    await waitFor(
      () => {
        expect(screen.getByTestId("item-takedown-success")).toBeTruthy();
      },
      { timeout: TAKEDOWN_ROUND_TRIP_TIMEOUT },
    );
    expect(document.activeElement).not.toBe(
      screen.getByTestId("item-takedown-success"),
    );
  });

  it("renders with zero serious/critical a11y violations (#7087)", { timeout: TAKEDOWN_TEST_TIMEOUT }, async () => {
    const { container } = render(<ItemTakedownPanel />);
    await renderA11yGate(container);
  });
});
