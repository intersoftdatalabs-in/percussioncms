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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { BulkStageDialog } from "../../../main/ts/contentExplorer/BulkStageDialog";
import type { BulkStageReportInput } from "../../../main/ts/contentExplorer/itemPublish";
import { renderA11yGate } from "./a11y";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "page",
    path: "/Sites/Demo/page",
    type: "percPage",
    id: "42",
    ...overrides,
  };
}

const page = (id: string, name: string): PSPathItem =>
  item({ id, name, path: `/Sites/Demo/${name}` });
const folder = (id: string, name: string): PSPathItem =>
  item({
    id,
    name,
    path: `/Sites/Demo/${name}`,
    type: "folder",
    leaf: false,
  });

describe("BulkStageDialog (#6469)", () => {
  it("confirm lists eligible items, names skipped folders, cancel runs nothing", async () => {
    const runBatch = vi.fn();
    const onDone = vi.fn();
    const { container } = render(
      <BulkStageDialog
        mode="stage"
        items={[
          page("42", "Home"),
          page("43", "About"),
          folder("7", "News"),
        ]}
        runBatch={runBatch}
        onDone={onDone}
      />,
    );
    expect(screen.getByTestId("explorer-bulk-stage-dialog")).toHaveAttribute(
      "data-mode",
      "stage",
    );
    expect(screen.getByTestId("explorer-bulk-stage-items")).toHaveTextContent(
      /Home/,
    );
    expect(screen.getByTestId("explorer-bulk-stage-items")).toHaveTextContent(
      /About/,
    );
    expect(
      screen.getByTestId("explorer-bulk-stage-items"),
    ).not.toHaveTextContent(/News/);
    expect(screen.getByTestId("explorer-bulk-stage-skipped")).toHaveTextContent(
      /News/,
    );
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-cancel"));
    expect(runBatch).not.toHaveBeenCalled();
    expect(onDone).toHaveBeenCalledWith({ ran: false, refresh: false });
    await renderA11yGate(container);
  });

  it("confirm runs the batch and reports each row with summary counts", async () => {
    // The stub only saw the eligible rows; the dialog merges the
    // partition plan skips so the folder is still reported.
    const report: BulkStageReportInput = {
      okIds: ["42"],
      skippedFolders: [],
      skippedOther: [],
      failures: [
        { id: "43", name: "About", status: 409, message: "HTTP 409" },
      ],
    };
    const runBatch = vi.fn().mockResolvedValue(report);
    const onDone = vi.fn();
    const { container } = render(
      <BulkStageDialog
        mode="stage"
        items={[
          page("42", "Home"),
          page("43", "About"),
          folder("7", "News"),
        ]}
        runBatch={runBatch}
        onDone={onDone}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-confirm"));
    const summary = await screen.findByTestId("explorer-bulk-stage-summary");
    expect(summary).toHaveTextContent(/1 staged, 1 skipped, 1 failed/);
    const rows = screen.getAllByTestId("explorer-bulk-stage-report-row");
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveAttribute("data-outcome", "ok");
    expect(rows[0]).toHaveTextContent(/Home/);
    expect(rows[0]).toHaveTextContent(/Staged/);
    expect(rows[1]).toHaveAttribute("data-outcome", "failed");
    expect(rows[1]).toHaveTextContent(/About/);
    expect(rows[1]).toHaveTextContent(/HTTP 409/);
    expect(rows[2]).toHaveAttribute("data-outcome", "skipped-folder");
    expect(rows[2]).toHaveTextContent(/News/);
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-close"));
    expect(onDone).toHaveBeenCalledWith({ ran: true, refresh: true });
    await renderA11yGate(container);
  });

  it("report with no successes closes without refresh", async () => {
    const runBatch = vi.fn().mockResolvedValue({
      okIds: [],
      skippedFolders: [],
      skippedOther: [],
      failures: [
        { id: "42", name: "Home", status: 403, message: "HTTP 403" },
      ],
    });
    const onDone = vi.fn();
    render(
      <BulkStageDialog
        mode="stage"
        items={[page("42", "Home")]}
        runBatch={runBatch}
        onDone={onDone}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-confirm"));
    await screen.findByTestId("explorer-bulk-stage-summary");
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-close"));
    expect(onDone).toHaveBeenCalledWith({ ran: true, refresh: false });
  });

  it("folders-only selection shows nothing-eligible and closes cleanly", async () => {
    const runBatch = vi.fn();
    const onDone = vi.fn();
    const { container } = render(
      <BulkStageDialog
        mode="unstage"
        items={[folder("7", "News")]}
        runBatch={runBatch}
        onDone={onDone}
      />,
    );
    expect(screen.getByTestId("explorer-bulk-stage-dialog")).toHaveAttribute(
      "data-mode",
      "unstage",
    );
    expect(screen.queryByTestId("explorer-bulk-stage-confirm")).toBeNull();
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-close"));
    expect(runBatch).not.toHaveBeenCalled();
    expect(onDone).toHaveBeenCalledWith({ ran: false, refresh: false });
    await renderA11yGate(container);
  });

  it("unstage mode reports removed rows with unstage chrome", async () => {
    const runBatch = vi.fn().mockResolvedValue({
      okIds: ["42"],
      skippedFolders: ["News"],
      skippedOther: [],
      failures: [],
    });
    const onDone = vi.fn();
    render(
      <BulkStageDialog
        mode="unstage"
        items={[page("42", "Home"), folder("7", "News")]}
        runBatch={runBatch}
        onDone={onDone}
      />,
    );
    expect(screen.getByTestId("explorer-bulk-stage-confirm")).toHaveTextContent(
      /Remove 1 item/,
    );
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-confirm"));
    const summary = await screen.findByTestId("explorer-bulk-stage-summary");
    expect(summary).toHaveTextContent(/1 removed from staging/);
    const rows = screen.getAllByTestId("explorer-bulk-stage-report-row");
    expect(rows[0]).toHaveTextContent(/Removed from staging/);
    fireEvent.click(screen.getByTestId("explorer-bulk-stage-close"));
    expect(onDone).toHaveBeenCalledWith({ ran: true, refresh: true });
  });
});
