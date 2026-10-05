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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { jsonResponse, mockFetch } from "./setup";

const adminBootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "explorer",
  isAdmin: true,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

const PAGE = {
  id: "42",
  name: "Home",
  path: "/Sites/Demo/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

const FOLDER = {
  id: "fold-1",
  name: "News",
  path: "/Sites/Demo/News/",
  type: "folder",
  category: "folder",
  leaf: false,
};

const AUDIT = [
  {
    name: "Workflow_AuditTrail",
    label: "Audit Trail",
    sortRank: 1,
    menuType: "MENUITEM" as const,
    url: "../sys_cxSupport/contenteditorurls.html?sys_userview=sys_audittrail&sys_command=preview",
  },
];

const HISTORY = {
  RevisionsSummary: {
    restorable: true,
    currentRevision: 2,
    revisions: [
      {
        revId: 1,
        lastModifiedDate: "2026-01-01",
        lastModifier: "Admin",
        status: "Draft",
      },
    ],
    comments: [
      {
        comment: "Looks good",
        commenter: "Admin",
        commentType: "Approve",
        commentDate: "2026-01-02",
      },
    ],
  },
};

function renderShell(ui: ReactElement) {
  return render(
    <BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>,
  );
}

function requestUrl(input: RequestInfo | URL): string {
  if (typeof input === "string") return input;
  if (input instanceof Request) return input.url;
  return String(input);
}

function stubList(
  onRequest?: (url: string) => Response | Promise<Response> | undefined,
) {
  return mockFetch(async (input: RequestInfo | URL) => {
    const url = requestUrl(input);
    if (url.includes("paginatedFolder") || url.includes("/path/folder/")) {
      return jsonResponse({
        PagedItemList: {
          childrenInPage: [PAGE, FOLDER],
          childrenCount: 2,
          startIndex: 0,
        },
        PathItem: [],
      });
    }
    const extra = await onRequest?.(url);
    if (extra) {
      return extra;
    }
    return jsonResponse({});
  });
}

function renderExplorer() {
  return renderShell(
    <ContentExplorerShell
      initialPath="/Sites/Demo"
      loadDisplayFormats={async () => []}
      loadMenuActions={async () => AUDIT}
    />,
  );
}

async function openAuditTrail() {
  const audit = await screen.findByTestId(
    "action-toolbar-item-Workflow_AuditTrail",
  );
  fireEvent.click(audit);
}

describe("Explorer audit trail of the selected item (#5245)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("an empty selection does not claim an audit trail", async () => {
    const fetchMock = stubList();
    renderExplorer();
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument(),
    );
    await openAuditTrail();
    expect(await screen.findByTestId("explorer-revisions-hint")).toBeTruthy();
    expect(screen.queryByTestId("revisions-audit-table")).toBeNull();
    expect(screen.queryByTestId("audit-row-0")).toBeNull();
    expect(screen.queryByTestId("explorer-flush-cache-status")).toBeNull();
    expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
      /Select a content item first/,
    );
    const urls = fetchMock.mock.calls.map((call) => requestUrl(call[0]));
    expect(urls.some((url) => url.includes("contenteditorurls"))).toBe(false);
    expect(urls.some((url) => url.includes("item/revisions"))).toBe(false);
  });

  it("a folder does not claim an audit trail", async () => {
    const fetchMock = stubList();
    renderExplorer();
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-fold-1")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-row-fold-1"));
    await openAuditTrail();
    expect(await screen.findByTestId("explorer-revisions-hint")).toBeTruthy();
    expect(screen.queryByTestId("revisions-audit-table")).toBeNull();
    expect(screen.queryByTestId("audit-row-0")).toBeNull();
    expect(screen.queryByTestId("explorer-flush-cache-status")).toBeNull();
    expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
      /Select a content item first/,
    );
    const urls = fetchMock.mock.calls.map((call) => requestUrl(call[0]));
    expect(urls.some((url) => url.includes("contenteditorurls"))).toBe(false);
  });

  it("shows date, user, type, and comment only after history loads", async () => {
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    stubList(async (url) => {
      if (!url.includes("item/revisions")) {
        return undefined;
      }
      await gate;
      return jsonResponse(HISTORY);
    });
    renderExplorer();
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-row-42"));
    await openAuditTrail();
    expect(await screen.findByTestId("revisions-panel")).toHaveAttribute(
      "data-testid-state",
      "loading",
    );
    expect(screen.queryByTestId("audit-row-0")).toBeNull();
    expect(screen.queryByTestId("revisions-audit-empty")).toBeNull();
    release();
    await waitFor(() =>
      expect(screen.getByTestId("audit-comment-0")).toHaveTextContent(
        "Looks good",
      ),
    );
    expect(screen.getByTestId("audit-date-0")).toHaveTextContent("2026-01-02");
    expect(screen.getByTestId("audit-user-0")).toHaveTextContent("Admin");
    expect(screen.getByTestId("audit-type-0")).toHaveTextContent("Approve");
    expect(screen.queryByTestId("revisions-restore-1")).toBeNull();
    expect(screen.queryByTestId("revisions-compare-run")).toBeNull();
  });

  it("an empty history shows the empty message, not a fake row", async () => {
    stubList((url) => {
      if (!url.includes("item/revisions")) {
        return undefined;
      }
      return jsonResponse({
        RevisionsSummary: {
          restorable: false,
          revisions: [],
          comments: {
            comment: " ",
            commenter: "",
            commentType: "",
            commentDate: "",
          },
        },
      });
    });
    renderExplorer();
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-42")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-row-42"));
    await openAuditTrail();
    expect(
      (await screen.findByTestId("revisions-audit-empty")).textContent,
    ).toMatch(/No workflow comments/);
    expect(screen.queryByTestId("audit-row-0")).toBeNull();
  });

  it.each([403, 404])(
    "HTTP %s stays in the panel and is not an empty trail",
    async (status) => {
      stubList((url) => {
        if (!url.includes("item/revisions")) {
          return undefined;
        }
        return jsonResponse(
          { message: "No workflow comments are recorded for this item" },
          status,
        );
      });
      renderExplorer();
      await waitFor(() =>
        expect(screen.getByTestId("detail-row-42")).toBeInTheDocument(),
      );
      fireEvent.click(screen.getByTestId("detail-row-42"));
      await openAuditTrail();
      expect(
        (await screen.findByTestId("revisions-load-error")).textContent,
      ).toMatch(new RegExp(String(status)));
      expect(screen.getByTestId("revisions-panel")).toHaveAttribute(
        "data-testid-state",
        "error",
      );
      expect(screen.queryByTestId("revisions-audit-empty")).toBeNull();
      expect(screen.queryByTestId("audit-row-0")).toBeNull();
    },
  );
});
