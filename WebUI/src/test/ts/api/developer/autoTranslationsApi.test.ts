/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  autoTranslationRowKey,
  classifyAutoTranslationSaveError,
  duplicateAutoTranslationKey,
  isAutoTranslationLockError,
  isAutoTranslationRowReady,
  isAutoTranslationSetReady,
  isUnknownLocaleOrTypeError,
  listAutoTranslations,
  saveAutoTranslations,
  toAutoTranslationWriteBody,
  unwrapAutoTranslationRows,
  wrapAutoTranslationRowsForWire,
} from "../../../../main/ts/api/developer/autoTranslationsApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("auto-translation row helpers", () => {
  it("normalizes locale×type keys", () => {
    expect(
      autoTranslationRowKey({ locale: "FR_CA", contentTypeName: "percPage" }),
    ).toBe("fr-ca|percpage");
    expect(autoTranslationRowKey({ locale: "en-us", contentTypeId: 301 })).toBe("en-us|id:301");
  });

  it("requires locale, content type, workflow, and community", () => {
    expect(isAutoTranslationRowReady({})).toBe(false);
    expect(isAutoTranslationRowReady({ locale: "en-us" })).toBe(false);
    expect(
      isAutoTranslationRowReady({
        locale: "en-us",
        contentTypeName: "percPage",
        workflowName: "Default Workflow",
      }),
    ).toBe(false);
    expect(
      isAutoTranslationRowReady({
        locale: "en-us",
        contentTypeName: "percPage",
        workflowName: "Default Workflow",
        communityName: "Default",
      }),
    ).toBe(true);
  });

  it("treats ID-only content type, workflow, and community as ready", () => {
    expect(
      isAutoTranslationRowReady({
        locale: "en-us",
        contentTypeId: 301,
        workflowId: 4,
        communityId: 10,
      }),
    ).toBe(true);
    expect(
      isAutoTranslationRowReady({
        locale: "en-us",
        contentTypeId: 301,
        workflowId: 4,
      }),
    ).toBe(false);
  });

  it("treats empty set as ready and rejects duplicates", () => {
    expect(isAutoTranslationSetReady([])).toBe(true);
    const row = {
      locale: "en-us",
      contentTypeName: "percPage",
      workflowName: "Default Workflow",
      communityName: "Default",
    };
    expect(isAutoTranslationSetReady([row])).toBe(true);
    expect(isAutoTranslationSetReady([row, { ...row }])).toBe(false);
    expect(duplicateAutoTranslationKey([row, { ...row, locale: "EN_US" }])).toBe(
      "en-us|percpage",
    );
  });

  it("omits blank names from PUT body", () => {
    expect(
      toAutoTranslationWriteBody({
        locale: " FR_CA ",
        contentTypeName: "percPage",
        workflowName: "  ",
        workflowId: 4,
        communityName: "Default",
      }),
    ).toEqual({
      locale: "fr-ca",
      contentTypeName: "percPage",
      workflowId: 4,
      communityName: "Default",
    });
  });
});

describe("wrapAutoTranslationRowsForWire", () => {
  it("wraps PUT under AutoTranslationRow including empty clear", () => {
    expect(wrapAutoTranslationRowsForWire([])).toEqual({ AutoTranslationRow: [] });
    expect(
      wrapAutoTranslationRowsForWire([
        {
          locale: "fr-fr",
          contentTypeName: "percPage",
          workflowName: "Default Workflow",
          communityName: "Default",
        },
      ]),
    ).toEqual({
      AutoTranslationRow: [
        {
          locale: "fr-fr",
          contentTypeName: "percPage",
          workflowName: "Default Workflow",
          communityName: "Default",
        },
      ],
    });
  });
});

describe("unwrapAutoTranslationRows", () => {
  it("accepts a bare array, Jackson wrap, and empty", () => {
    expect(unwrapAutoTranslationRows([])).toEqual([]);
    expect(
      unwrapAutoTranslationRows([{ locale: "fr-fr", contentTypeName: "percPage" }]),
    ).toEqual([{ locale: "fr-fr", contentTypeName: "percPage" }]);
    expect(
      unwrapAutoTranslationRows({
        AutoTranslationRow: [{ locale: "en-us", contentTypeName: "percPage" }],
      }),
    ).toEqual([{ locale: "en-us", contentTypeName: "percPage" }]);
    expect(unwrapAutoTranslationRows(null)).toEqual([]);
  });
});

describe("auto-translation save error classifiers", () => {
  it("classifies unknown locale 400", () => {
    const err = {
      status: 400,
      statusText: "Bad Request",
      body: { message: "unknown locale: xx-xx" },
    };
    expect(isUnknownLocaleOrTypeError(err)).toBe(true);
    expect(classifyAutoTranslationSaveError(err)).toBe("unknown");
  });

  it("classifies unknown content type 400", () => {
    const err = {
      status: 400,
      statusText: "Bad Request",
      body: "unknown content type: missing",
    };
    expect(isUnknownLocaleOrTypeError(err)).toBe(true);
    expect(classifyAutoTranslationSaveError(err)).toBe("unknown");
  });

  it("does not treat other 400s as unknown locale/type", () => {
    const err = {
      status: 400,
      statusText: "Bad Request",
      body: { message: "duplicate locale/content-type row: en-us / percPage" },
    };
    expect(isUnknownLocaleOrTypeError(err)).toBe(false);
    expect(classifyAutoTranslationSaveError(err)).toBe("other");
  });

  it("classifies lock 409", () => {
    const err = {
      status: 409,
      statusText: "Conflict",
      body: { message: "Could not save auto-translations; locked by other" },
    };
    expect(isAutoTranslationLockError(err)).toBe(true);
    expect(classifyAutoTranslationSaveError(err)).toBe("lock");
  });

  it("classifies non-409 lock wording as lock", () => {
    const err = {
      status: 500,
      statusText: "Internal Server Error",
      body: { message: "Could not save auto-translations; locked by other" },
    };
    expect(isAutoTranslationLockError(err)).toBe(true);
    expect(classifyAutoTranslationSaveError(err)).toBe("lock");
  });

  it("classifies non-API errors as other", () => {
    const err = new Error("network");
    expect(isAutoTranslationLockError(err)).toBe(false);
    expect(classifyAutoTranslationSaveError(err)).toBe("other");
  });
});

describe("autoTranslationsApi HTTP", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("GETs /services/locales/auto-translations", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse([{ locale: "fr-fr", contentTypeName: "percPage" }]),
    );
    const list = await listAutoTranslations();
    expect(list).toEqual([{ locale: "fr-fr", contentTypeName: "percPage" }]);
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.AUTO_TRANSLATIONS);
  });

  it("PUTs the full replace list including empty clear", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse([]));
    const saved = await saveAutoTranslations([]);
    expect(saved).toEqual([]);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({ AutoTranslationRow: [] });
  });

  it("PUTs locale × content-type rows", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse([
        {
          locale: "fr-fr",
          contentTypeName: "percPage",
          workflowName: "Default Workflow",
          communityName: "Default",
        },
      ]),
    );
    const saved = await saveAutoTranslations([
      {
        locale: "fr-fr",
        contentTypeName: "percPage",
        workflowName: "Default Workflow",
        communityName: "Default",
      },
    ]);
    expect(saved[0]?.locale).toBe("fr-fr");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(String(init.body))).toEqual({
      AutoTranslationRow: [
        {
          locale: "fr-fr",
          contentTypeName: "percPage",
          workflowName: "Default Workflow",
          communityName: "Default",
        },
      ],
    });
  });
});
