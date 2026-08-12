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

import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  getAuditLogEntry,
  queryAuditLogEntries,
  SYSTEM_AUDIT_LOG_ENTRY_ROOT,
  SYSTEM_AUDIT_LOG_PAGE_ROOT,
  unwrapSystemAuditLogEntry,
  unwrapSystemAuditLogPage,
} from "../../../../main/ts/api/auditlog/auditLogApi";
import * as client from "../../../../main/ts/api/client";

vi.mock("../../../../main/ts/api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../../main/ts/api/client")>(
    "../../../../main/ts/api/client",
  );
  return {
    ...actual,
    get: vi.fn(),
  };
});

const sampleEntry = {
  auditId: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  eventTime: "2026-08-09T15:00:00Z",
  moduleCode: "AUTH",
  eventType: "AUTH_LOGIN",
  outcome: "SUCCESS",
  actor: "Admin",
  userMessage: "User Admin logged in",
  logMessage: "[AUTH-1001]-[aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee] login ok",
};

describe("unwrapSystemAuditLogPage", () => {
  it("unwraps WRAP_ROOT SystemAuditLogPage so entries and total bind (#3089)", () => {
    const page = unwrapSystemAuditLogPage({
      [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
        entries: [sampleEntry],
        total: 1,
        offset: 0,
        limit: 50,
      },
    });
    expect(page.total).toBe(1);
    expect(page.entries).toHaveLength(1);
    expect(page.entries?.[0]?.auditId).toBe(sampleEntry.auditId);
    expect(page.entries?.[0]?.moduleCode).toBe("AUTH");
  });

  it("accepts flat page bodies (tests / already-unwrapped proxies)", () => {
    const page = unwrapSystemAuditLogPage({
      entries: [sampleEntry],
      total: 3,
      offset: 10,
      limit: 25,
    });
    expect(page.total).toBe(3);
    expect(page.offset).toBe(10);
    expect(page.limit).toBe(25);
    expect(page.entries).toHaveLength(1);
  });

  it("returns empty page for null or mis-shaped payloads (not throw)", () => {
    expect(unwrapSystemAuditLogPage(null).total).toBe(0);
    expect(unwrapSystemAuditLogPage(undefined).entries).toEqual([]);
    expect(unwrapSystemAuditLogPage({ Error: { message: "x" } }).total).toBe(0);
  });

  it("unwraps Jackson WRAP_ROOT entries object so map never sees a non-array (#3195)", () => {
    const page = unwrapSystemAuditLogPage({
      [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
        entries: {
          [SYSTEM_AUDIT_LOG_ENTRY_ROOT]: [sampleEntry, sampleEntry],
        },
        total: 2,
        offset: 0,
        limit: 50,
      },
    });
    expect(Array.isArray(page.entries)).toBe(true);
    expect(page.entries).toHaveLength(2);
    expect(page.entries?.[0]?.auditId).toBe(sampleEntry.auditId);
    expect(page.total).toBe(2);
  });

  it("unwraps a single WRAP_ROOT entry object as a one-item list (#3195)", () => {
    const page = unwrapSystemAuditLogPage({
      [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
        entries: { [SYSTEM_AUDIT_LOG_ENTRY_ROOT]: sampleEntry },
        total: 1,
      },
    });
    expect(page.entries).toHaveLength(1);
    expect(page.entries?.[0]?.actor).toBe("Admin");
  });

  it("unwraps nested SystemAuditLogPage envelopes", () => {
    const page = unwrapSystemAuditLogPage({
      [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
        [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
          entries: [sampleEntry],
          total: 1,
        },
      },
    });
    expect(page.entries).toHaveLength(1);
    expect(page.total).toBe(1);
  });

  it("without unwrap WRAP_ROOT would hide entries (regression guard)", () => {
    const wire = {
      [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
        entries: [sampleEntry],
        total: 1,
      },
    };
    // Naive flat read (pre-#3089 asPage) sees no top-level entries.
    const naiveEntries = Array.isArray((wire as { entries?: unknown }).entries)
      ? (wire as { entries: unknown[] }).entries
      : [];
    expect(naiveEntries).toHaveLength(0);
    expect(unwrapSystemAuditLogPage(wire).entries).toHaveLength(1);
  });
});

describe("unwrapSystemAuditLogEntry", () => {
  it("unwraps WRAP_ROOT SystemAuditLogEntry for detail panel", () => {
    const entry = unwrapSystemAuditLogEntry({
      [SYSTEM_AUDIT_LOG_ENTRY_ROOT]: sampleEntry,
    });
    expect(entry?.auditId).toBe(sampleEntry.auditId);
    expect(entry?.userMessage).toContain("logged in");
  });

  it("accepts flat entry bodies", () => {
    expect(unwrapSystemAuditLogEntry(sampleEntry)?.actor).toBe("Admin");
  });

  it("returns null for empty/mis-shaped", () => {
    expect(unwrapSystemAuditLogEntry(null)).toBeNull();
    expect(unwrapSystemAuditLogEntry({})).toBeNull();
  });
});

describe("queryAuditLogEntries / getAuditLogEntry", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("queryAuditLogEntries unwraps wrapped GET payload", async () => {
    vi.mocked(client.get).mockResolvedValue({
      [SYSTEM_AUDIT_LOG_PAGE_ROOT]: {
        entries: [sampleEntry],
        total: 1,
        offset: 0,
        limit: 50,
      },
    });
    const page = await queryAuditLogEntries({ module: "AUTH" });
    expect(page.total).toBe(1);
    expect(page.entries?.[0]?.moduleCode).toBe("AUTH");
    expect(client.get).toHaveBeenCalled();
  });

  it("getAuditLogEntry unwraps wrapped detail payload", async () => {
    vi.mocked(client.get).mockResolvedValue({
      [SYSTEM_AUDIT_LOG_ENTRY_ROOT]: sampleEntry,
    });
    const entry = await getAuditLogEntry(sampleEntry.auditId);
    expect(entry.auditId).toBe(sampleEntry.auditId);
    expect(entry.logMessage).toContain("AUTH-1001");
  });
});
