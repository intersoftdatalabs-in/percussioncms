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

import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  auditCellText,
  formatAuditPageSummary,
  formatEventTime,
  SecurityAuditLogViewer,
  truncate,
} from "../../../main/ts/admin/tools/SecurityAuditLogViewer";
import * as auditApi from "../../../main/ts/api/auditlog/auditLogApi";
import { AuditLogForbiddenError } from "../../../main/ts/api/auditlog/auditLogApi";

vi.mock("../../../main/ts/api/auditlog/auditLogApi", async () => {
  const actual = await vi.importActual<
    typeof import("../../../main/ts/api/auditlog/auditLogApi")
  >("../../../main/ts/api/auditlog/auditLogApi");
  return {
    ...actual,
    queryAuditLogEntries: vi.fn(),
    getAuditLogEntry: vi.fn(),
  };
});

const sampleEntry = {
  auditId: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  eventTime: "2026-08-09T15:00:00Z",
  moduleCode: "AUTH",
  eventType: "LOGIN",
  outcome: "SUCCESS",
  actor: "Admin",
  target: "Admin",
  userMessage: "User Admin logged in",
  logMessage: "[AUTH-1001]-[aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee] login ok",
};

describe("SecurityAuditLogViewer", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("formatAuditPageSummary substitutes placeholders", () => {
    expect(formatAuditPageSummary(1, 50, 120)).toContain("1");
    expect(formatAuditPageSummary(1, 50, 120)).toContain("50");
    expect(formatAuditPageSummary(1, 50, 120)).toContain("120");
  });

  it("formatEventTime never returns a non-string (Instant objects)", () => {
    expect(formatEventTime(undefined)).toBe("—");
    expect(formatEventTime("2026-08-09T15:00:00Z")).not.toBe("—");
    expect(typeof formatEventTime({ epochSecond: 1_786_291_200, nano: 0 })).toBe(
      "string",
    );
    expect(formatEventTime({ epochSecond: 1_786_291_200, nano: 0 })).not.toBe(
      "—",
    );
  });

  it("renders without TypeError when query returns a non-array entries envelope", async () => {
    vi.mocked(auditApi.queryAuditLogEntries).mockResolvedValue({
      entries: { SystemAuditLogEntry: [sampleEntry] } as unknown as typeof sampleEntry[],
      total: 1,
      offset: 0,
      limit: 50,
    });

    render(<SecurityAuditLogViewer />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-security-audit-log")).toBeDefined();
    });
    expect(screen.getByTestId("audit-log-table")).toBeDefined();
    expect(screen.queryByTestId("route-error")).toBeNull();
  });

  it("truncate keeps max chars then ellipsis (no off-by-one)", () => {
    expect(truncate("short", 40)).toBe("short");
    expect(truncate(undefined)).toBe("—");
    const long = "a".repeat(50);
    const out = truncate(long, 40);
    expect(out.endsWith("…")).toBe(true);
    expect(out.slice(0, -1)).toHaveLength(40);
  });

  it("truncate does not throw on non-string wire values", () => {
    expect(truncate(1131, 40)).toBe("1131");
    expect(truncate({ value: "list" }, 40)).toBe("list");
    expect(truncate({ text: "  wrapped  " }, 40)).toBe("wrapped");
    expect(truncate({ epochSecond: 1 }, 40)).toBe("—");
    expect(auditCellText(1131)).toBe("1131");
    expect(auditCellText(null)).toBe("");
  });

  it("renders rows when target is a number (no AdminSectionErrorBoundary)", async () => {
    vi.mocked(auditApi.queryAuditLogEntries).mockResolvedValue({
      entries: [
        {
          ...sampleEntry,
          target: 1131 as unknown as string,
          userMessage: { value: "Content item 1131 created" } as unknown as string,
        },
      ],
      total: 1,
      offset: 0,
      limit: 50,
    });

    render(<SecurityAuditLogViewer />);

    await waitFor(() => {
      expect(
        screen.getByTestId(`audit-log-row-${sampleEntry.auditId}`),
      ).toBeDefined();
    });
    expect(screen.queryByTestId("admin-section-error")).toBeNull();
    expect(screen.getByTestId("audit-log-table").textContent).toContain("1131");
    expect(screen.getByTestId("audit-log-table").textContent).toContain(
      "Content item 1131 created",
    );
  });

  it("renders table rows after successful load", async () => {
    vi.mocked(auditApi.queryAuditLogEntries).mockResolvedValue({
      entries: [sampleEntry],
      total: 1,
      offset: 0,
      limit: 50,
    });

    render(<SecurityAuditLogViewer />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-security-audit-log")).toBeDefined();
    });
    await waitFor(() => {
      expect(
        screen.getByTestId(`audit-log-row-${sampleEntry.auditId}`),
      ).toBeDefined();
    });
    const row = screen.getByTestId(`audit-log-row-${sampleEntry.auditId}`);
    expect(row.textContent).toContain("AUTH");
    expect(row.textContent).toContain("SUCCESS");
    expect(row.textContent).toContain("Admin");
    expect(auditApi.queryAuditLogEntries).toHaveBeenCalled();
  });

  it("shows forbidden state on 403", async () => {
    vi.mocked(auditApi.queryAuditLogEntries).mockRejectedValue(
      new AuditLogForbiddenError(),
    );

    render(<SecurityAuditLogViewer />);

    await waitFor(() => {
      expect(screen.getByTestId("audit-log-error")).toBeDefined();
    });
    expect(screen.getByTestId("audit-log-error").textContent).toMatch(
      /permission|audit log/i,
    );
  });

  it("loads detail when a row is clicked", async () => {
    vi.mocked(auditApi.queryAuditLogEntries).mockResolvedValue({
      entries: [sampleEntry],
      total: 1,
      offset: 0,
      limit: 50,
    });
    vi.mocked(auditApi.getAuditLogEntry).mockResolvedValue(sampleEntry);

    render(<SecurityAuditLogViewer />);

    await waitFor(() => {
      expect(
        screen.getByTestId(`audit-log-row-${sampleEntry.auditId}`),
      ).toBeDefined();
    });

    fireEvent.click(
      screen.getByTestId(`audit-log-row-${sampleEntry.auditId}`),
    );

    await waitFor(() => {
      expect(screen.getByTestId("audit-log-detail")).toBeDefined();
    });
    await waitFor(() => {
      expect(screen.getByTestId("audit-detail-user-message").textContent).toContain(
        "logged in",
      );
    });
    expect(screen.getByTestId("audit-detail-log-message").textContent).toContain(
      "AUTH-1001",
    );
    expect(auditApi.getAuditLogEntry).toHaveBeenCalledWith(sampleEntry.auditId);
  });

  it("applies filters on submit", async () => {
    vi.mocked(auditApi.queryAuditLogEntries).mockResolvedValue({
      entries: [],
      total: 0,
      offset: 0,
      limit: 50,
    });

    render(<SecurityAuditLogViewer />);

    await waitFor(() => {
      expect(screen.getByTestId("audit-filter-module")).toBeDefined();
    });

    fireEvent.change(screen.getByTestId("audit-filter-module"), {
      target: { value: "AUTH" },
    });
    fireEvent.change(screen.getByTestId("audit-filter-actor"), {
      target: { value: "Admin" },
    });
    fireEvent.click(screen.getByTestId("audit-filter-apply"));

    await waitFor(() => {
      const calls = vi.mocked(auditApi.queryAuditLogEntries).mock.calls;
      const last = calls[calls.length - 1]?.[0];
      expect(last?.module).toBe("AUTH");
      expect(last?.actor).toBe("Admin");
    });
  });
});
