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
 * Detail-list export-current-view to CSV (#6079 / parent #4530).
 *
 * <p>The Export CSV button above the list downloads the displayed rows
 * in display order with the visible columns only, built client-side
 * from the already-loaded page (zero new REST). Only the loaded page
 * is exported; paging stays server-side.</p>
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  DETAIL_COLUMN_VISIBILITY_STORAGE_KEY,
  DetailList,
} from "../../../main/ts/contentExplorer/DetailList";
import {
  buildDetailCsv,
  csvEscapeCell,
  detailCsvFileName,
} from "../../../main/ts/contentExplorer/detailCsvExport";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

function item(id: string, name: string, type = "percPage"): PSPathItem {
  return {
    id,
    path: `/Sites/Demo/${name}`,
    name,
    type,
    category: "page",
    accessLevel: "WRITE",
    leaf: true,
  } as PSPathItem;
}

const ROWS: PSPathItem[] = [
  item("c-3", "Cherry"),
  item("a-1", 'apple, "crisp"'),
  item("b-2", "Banana"),
];

function listingResponse(rows: PSPathItem[]): Response {
  return new Response(
    JSON.stringify({
      PagedItemList: {
        childrenInPage: rows,
        childrenCount: rows.length,
        startIndex: 0,
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

async function renderListing(): Promise<void> {
  mockFetch(async () => listingResponse(ROWS));
  render(
    <DetailList
      folderPath="/Sites/Demo"
      selectedItemId={null}
      onSelectItem={() => undefined}
    />,
  );
  await waitFor(() =>
    expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument(),
  );
}

/**
 * Render the list, click Export CSV, and return the downloaded Blob.
 * Stubs {@code URL.createObjectURL} so the CSV bytes are captured
 * without leaving the test runtime.
 */
async function exportBlob(): Promise<Blob> {
  let captured: Blob | null = null;
  const create = vi.fn((blob: Blob) => {
    captured = blob;
    return "blob:test-csv";
  });
  const realUrl = globalThis.URL;
  vi.stubGlobal("URL", { createObjectURL: create, revokeObjectURL: vi.fn() });
  try {
    await renderListing();
    fireEvent.click(screen.getByTestId("detail-export-csv"));
    expect(create).toHaveBeenCalledTimes(1);
    const blob = captured as unknown as Blob;
    expect(blob).toBeInstanceOf(Blob);
    expect(blob.type).toContain("text/csv");
    return blob;
  } finally {
    vi.stubGlobal("URL", realUrl);
  }
}

describe("csvEscapeCell", () => {
  it("leaves plain fields untouched", () => {
    expect(csvEscapeCell("Cherry")).toBe("Cherry");
    expect(csvEscapeCell("")).toBe("");
  });

  it("maps nullish values to empty fields", () => {
    expect(csvEscapeCell(null)).toBe("");
    expect(csvEscapeCell(undefined)).toBe("");
  });

  it("quotes fields with commas, quotes, or line breaks", () => {
    expect(csvEscapeCell("a,b")).toBe('"a,b"');
    expect(csvEscapeCell('say "hi"')).toBe('"say ""hi"""');
    expect(csvEscapeCell("line1\nline2")).toBe('"line1\nline2"');
    expect(csvEscapeCell("line1\r\nline2")).toBe('"line1\r\nline2"');
  });

  it("neutralizes spreadsheet formula injection (#6175)", () => {
    expect(csvEscapeCell("=cmd|' /C calc'!A0")).toBe(
      "'=cmd|' /C calc'!A0",
    );
    expect(csvEscapeCell("=1+1")).toBe("'=1+1");
    expect(csvEscapeCell("+2+3")).toBe("'+2+3");
    expect(csvEscapeCell("-2+3")).toBe("'-2+3");
    expect(csvEscapeCell("@SUM(A1:A2)")).toBe("'@SUM(A1:A2)");
    expect(csvEscapeCell("\t=1+1")).toBe("'\t=1+1");
    // The marker survives RFC 4180 quoting for hostile quoted fields.
    expect(csvEscapeCell('=HYPERLINK("http://evil", "click")')).toBe(
      `"'=HYPERLINK(""http://evil"", ""click"")"`,
    );
  });
});

describe("buildDetailCsv", () => {
  it("joins headers and rows with CRLF plus a trailing CRLF", () => {
    expect(
      buildDetailCsv(["Name", "Type"], [["Cherry", "percPage"]]),
    ).toBe("Name,Type\r\nCherry,percPage\r\n");
  });

  it("emits headers only when there are no rows", () => {
    expect(buildDetailCsv(["Name"], [])).toBe("Name\r\n");
  });

  it("quotes fields that need it", () => {
    expect(buildDetailCsv(["Name"], [['apple, "crisp"']])).toBe(
      'Name\r\n"apple, ""crisp"""\r\n',
    );
  });

  it("neutralizes formula-trigger row cells before quoting (#6175)", () => {
    expect(buildDetailCsv(["Name"], [["=cmd|' /C calc'!A0"]])).toBe(
      "Name\r\n'=cmd|' /C calc'!A0\r\n",
    );
  });
});

describe("detailCsvFileName", () => {
  it("uses the folder leaf segment", () => {
    expect(detailCsvFileName("//Folders/$System$/Assets")).toBe(
      "explorer-Assets.csv",
    );
    expect(detailCsvFileName("/Sites/Demo")).toBe("explorer-Demo.csv");
  });

  it("sanitizes separators, quotes, and control chars", () => {
    expect(detailCsvFileName("/Sites/a:b")).toBe("explorer-a_b.csv");
    expect(detailCsvFileName('/Sites/a"b')).toBe("explorer-a_b.csv");
  });

  it("falls back to folder for blank paths", () => {
    expect(detailCsvFileName(null)).toBe("explorer-folder.csv");
    expect(detailCsvFileName("")).toBe("explorer-folder.csv");
    expect(detailCsvFileName("///")).toBe("explorer-folder.csv");
  });
});

describe("detail-list CSV export button", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("exports the displayed rows with default Name/Type/Path headers", async () => {
    const text = await (await exportBlob()).text();
    const normalized = text.replace(/\r\n/g, "\n");
    expect(normalized).toBe(
      "Name,Type,Path\n" +
        "Cherry,percPage,/Sites/Demo/Cherry\n" +
        '"apple, ""crisp""",percPage,"/Sites/Demo/apple, ""crisp"""\n' +
        "Banana,percPage,/Sites/Demo/Banana\n",
    );
  });

  it("exports only the visible columns when one is hidden", async () => {
    localStorage.setItem(
      DETAIL_COLUMN_VISIBILITY_STORAGE_KEY,
      JSON.stringify(["path"]),
    );
    const text = await (await exportBlob()).text();
    const normalized = text.replace(/\r\n/g, "\n");
    expect(normalized).toBe(
      "Name,Type\n" +
        "Cherry,percPage\n" +
        '"apple, ""crisp""",percPage\n' +
        "Banana,percPage\n",
    );
  });

  it("exports rows in the active sort order", async () => {
    let captured: Blob | null = null;
    const create = vi.fn((blob: Blob) => {
      captured = blob;
      return "blob:test-csv";
    });
    const realUrl = globalThis.URL;
    vi.stubGlobal("URL", {
      createObjectURL: create,
      revokeObjectURL: vi.fn(),
    });
    try {
      await renderListing();
      fireEvent.click(screen.getByTestId("detail-col-sort-name"));
      await waitFor(() =>
        expect(
          screen.getByTestId("detail-col-header-name"),
        ).toHaveAttribute("aria-sort", "ascending"),
      );
      fireEvent.click(screen.getByTestId("detail-export-csv"));
      expect(create).toHaveBeenCalledTimes(1);
      const normalized = await (
        captured as unknown as Blob
      )
        .text()
        .then((t) => t.replace(/\r\n/g, "\n"));
      expect(normalized).toBe(
        "Name,Type,Path\n" +
          '"apple, ""crisp""",percPage,"/Sites/Demo/apple, ""crisp"""\n' +
          "Banana,percPage,/Sites/Demo/Banana\n" +
          "Cherry,percPage,/Sites/Demo/Cherry\n",
      );
    } finally {
      vi.stubGlobal("URL", realUrl);
    }
  });

  it("passes the accessibility gate with the export button present", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-export-csv")).toBeInTheDocument(),
    );
    await renderA11yGate(container);
  });
});
