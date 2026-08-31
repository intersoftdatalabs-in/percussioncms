/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  contentTypeExportFilename,
  contentTypeNameFromDesignXml,
  exportContentType,
  importContentType,
  invalidContentTypeImportName,
  parseContentDispositionFilename,
  rewriteContentTypeDesignXmlName,
} from "../../../../main/ts/api/developer/contentTypeImportExport";
import { PATHS } from "../../../../main/ts/api/paths";

const SAMPLE_XML =
  '<ItemDefData appName="psx_ceimportedOne" isHidden="false" objectType="1">' +
  '<PSXItemDefSummary editorUrl="../psx_ceimportedOne/importedOne.html" id="557"' +
  ' label="Imported One" name="importedOne" typeId="557" /></ItemDefData>';

describe("contentTypeExportFilename", () => {
  it("appends .xml and strips Windows-invalid characters", () => {
    expect(contentTypeExportFilename("percPage")).toBe("percPage.xml");
    expect(contentTypeExportFilename("a?b")).toBe("a_b.xml");
    expect(contentTypeExportFilename("a<b>c")).toBe("a_b_c.xml");
    expect(contentTypeExportFilename("a:b/c\\d")).toBe("a_b_c_d.xml");
    expect(contentTypeExportFilename("")).toBe("contenttype.xml");
    expect(contentTypeExportFilename("Foo.XML")).toBe("Foo.XML");
  });
});

describe("parseContentDispositionFilename", () => {
  it("prefers RFC 5987 filename*", () => {
    expect(
      parseContentDispositionFilename(
        "attachment; filename=\"caf_.xml\"; filename*=UTF-8''caf%C3%A9.xml",
      ),
    ).toBe("café.xml");
  });

  it("reads quoted filename", () => {
    expect(
      parseContentDispositionFilename('attachment; filename="percPage.xml"'),
    ).toBe("percPage.xml");
  });
});

describe("design XML name helpers", () => {
  it("reads PSXItemDefSummary name", () => {
    expect(contentTypeNameFromDesignXml(SAMPLE_XML)).toBe("importedOne");
  });

  it("rewrites unique name on summary", () => {
    const out = rewriteContentTypeDesignXmlName(SAMPLE_XML, "cd14unique");
    expect(contentTypeNameFromDesignXml(out)).toBe("cd14unique");
    expect(out).toContain("ItemDefData");
  });

  it("rejects missing XML and missing ItemDefData", () => {
    expect(() => rewriteContentTypeDesignXmlName("", "n")).toThrow(/required/i);
    expect(() => rewriteContentTypeDesignXmlName("<not>xml</not>", "n")).toThrow(
      /invalid/i,
    );
  });

  it("rejects ItemDefData without a summary name", () => {
    expect(() =>
      rewriteContentTypeDesignXmlName("<ItemDefData></ItemDefData>", "n"),
    ).toThrow(/missing name/i);
  });

  it("escapes XML attribute metacharacters in the rewritten name", () => {
    const out = rewriteContentTypeDesignXmlName(SAMPLE_XML, 'a&b"c');
    expect(out).toContain('name="a&amp;b&quot;c"');
    expect(out).not.toContain('name="a&b"');
  });
});

describe("invalidContentTypeImportName", () => {
  it("rejects blank, spaces, and wildcards", () => {
    expect(invalidContentTypeImportName("")).toMatch(/required/i);
    expect(invalidContentTypeImportName("has space")).toMatch(/spaces/i);
    expect(invalidContentTypeImportName("star*")).toMatch(/wildcard/i);
    expect(invalidContentTypeImportName("percPage")).toBeNull();
  });
});

describe("exportContentType / importContentType fetch", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("GET export returns xml and Content-Disposition filename", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(SAMPLE_XML, {
        status: 200,
        headers: {
          "Content-Type": "application/xml",
          "Content-Disposition":
            'attachment; filename="importedOne.xml"; filename*=UTF-8\'\'importedOne.xml',
        },
      }),
    );
    const out = await exportContentType("importedOne");
    expect(out.xml).toContain("ItemDefData");
    expect(out.filename).toBe("importedOne.xml");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/importedOne/export`,
    );
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("GET");
  });

  it("encodes idOrName on export and surfaces 404", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "not found" }), {
        status: 404,
        statusText: "Not Found",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(exportContentType("missing type")).rejects.toMatchObject({
      status: 404,
    });
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/${encodeURIComponent("missing type")}/export`,
    );
  });

  it("POST import sends raw XML not JSON", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ ContentTypeDetail: { name: "importedOne", label: "Imported One" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const created = await importContentType(SAMPLE_XML);
    expect(created.name).toBe("importedOne");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/import`,
    );
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(init.body).toBe(SAMPLE_XML);
    const headers = new Headers(init.headers);
    expect(headers.get("Content-Type")).toMatch(/application\/xml/i);
  });

  it("surfaces 400 invalid XML", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "invalid content-type design XML" }), {
        status: 400,
        statusText: "Bad Request",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(importContentType("<not-xml")).rejects.toMatchObject({ status: 400 });
  });

  it("surfaces 409 duplicate name", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "Content type already exists: percPage" }), {
        status: 409,
        statusText: "Conflict",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(importContentType(SAMPLE_XML)).rejects.toMatchObject({ status: 409 });
  });
});
