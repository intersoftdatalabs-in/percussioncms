/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  asTemplateExportXml,
  downloadXmlFile,
  exportTemplate,
  importTemplate,
  invalidTemplateImportName,
  parseContentDispositionFilename,
  rewriteTemplateDesignXmlName,
  stripImportedTemplateIdentity,
  templateExportFilename,
  templateNameFromDesignXml,
} from "../../../../main/ts/api/developer/templateImportExport";
import { PATHS } from "../../../../main/ts/api/paths";

const SAMPLE_XML =
  "<assembly-template>" +
  "<name>imported.one</name>" +
  "<label>Imported One</label>" +
  "<assembler>Java/global/percussion/assembly/htmlAssembler</assembler>" +
  "<template>#set($x=1)$x</template>" +
  "</assembly-template>";

describe("templateExportFilename", () => {
  it("appends .xml and strips path/control characters", () => {
    expect(templateExportFilename("perc.page")).toBe("perc.page.xml");
    expect(templateExportFilename('a"b')).toBe("a_b.xml");
    expect(templateExportFilename("a:b/c\\d")).toBe("a_b_c_d.xml");
    expect(templateExportFilename("")).toBe("template.xml");
    expect(templateExportFilename("Foo.XML")).toBe("Foo.XML");
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
    expect(parseContentDispositionFilename('attachment; filename="perc.page.xml"')).toBe(
      "perc.page.xml",
    );
  });
});

describe("design XML name helpers", () => {
  it("reads assembly-template name", () => {
    expect(templateNameFromDesignXml(SAMPLE_XML)).toBe("imported.one");
  });

  it("rewrites unique name", () => {
    const out = rewriteTemplateDesignXmlName(SAMPLE_XML, "as08unique");
    expect(templateNameFromDesignXml(out)).toBe("as08unique");
    expect(out).toContain("assembly-template");
  });

  it("rejects missing XML and missing assembly-template", () => {
    expect(() => rewriteTemplateDesignXmlName("", "n")).toThrow(/required/i);
    expect(() => rewriteTemplateDesignXmlName("<not>xml</not>", "n")).toThrow(/invalid/i);
  });

  it("rejects assembly-template without a name element", () => {
    expect(() =>
      rewriteTemplateDesignXmlName("<assembly-template></assembly-template>", "n"),
    ).toThrow(/missing name/i);
  });

  it("escapes XML metacharacters in the rewritten name", () => {
    const out = rewriteTemplateDesignXmlName(SAMPLE_XML, "a&b");
    expect(out).toContain("<name>a&amp;b</name>");
    expect(out).not.toContain("<name>a&b</name>");
  });

  it("strips exported binding ids so create-only import does not collide", () => {
    const withIds =
      SAMPLE_XML.replace("</assembly-template>", "<id>99</id><id>-5</id></assembly-template>");
    const stripped = stripImportedTemplateIdentity(withIds);
    expect(stripped).not.toMatch(/<id>/i);
    const rewritten = rewriteTemplateDesignXmlName(withIds, "as08unique");
    expect(rewritten).toContain("<name>as08unique</name>");
    expect(rewritten).not.toMatch(/<id>/i);
  });
});

describe("asTemplateExportXml", () => {
  it("accepts XML and rejects objects or non-XML text", () => {
    expect(asTemplateExportXml(SAMPLE_XML)).toBe(SAMPLE_XML);
    expect(() => asTemplateExportXml({ message: "oops" })).toThrow(/did not return XML/i);
    expect(() => asTemplateExportXml("not xml")).toThrow(/did not return XML/i);
  });
});

describe("downloadXmlFile", () => {
  it("defers revokeObjectURL so Firefox can start the download", () => {
    vi.useFakeTimers();
    const create = vi.fn(() => "blob:test-export");
    const revoke = vi.fn();
    const realUrl = globalThis.URL;
    vi.stubGlobal("URL", {
      createObjectURL: create,
      revokeObjectURL: revoke,
    });
    try {
      downloadXmlFile(SAMPLE_XML, "perc.page.xml");
      expect(create).toHaveBeenCalled();
      expect(revoke).not.toHaveBeenCalled();
      vi.advanceTimersByTime(1000);
      expect(revoke).toHaveBeenCalledWith("blob:test-export");
    } finally {
      vi.useRealTimers();
      vi.stubGlobal("URL", realUrl);
    }
  });
});

describe("invalidTemplateImportName", () => {
  it("rejects blank, spaces, and invalid characters", () => {
    expect(invalidTemplateImportName("")).toMatch(/required/i);
    expect(invalidTemplateImportName("has space")).toMatch(/spaces/i);
    expect(invalidTemplateImportName("1startsDigit")).toMatch(/letter/i);
    expect(invalidTemplateImportName("bad*name")).toMatch(/letter/i);
    expect(invalidTemplateImportName("perc.page")).toBeNull();
    expect(invalidTemplateImportName("as08unique")).toBeNull();
  });
});

describe("exportTemplate / importTemplate fetch", () => {
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
            'attachment; filename="imported.one.xml"; filename*=UTF-8\'\'imported.one.xml',
        },
      }),
    );
    const out = await exportTemplate("imported.one");
    expect(out.xml).toContain("assembly-template");
    expect(out.filename).toBe("imported.one.xml");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.TEMPLATES}/imported.one/export`);
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
    await expect(exportTemplate("missing name")).rejects.toMatchObject({
      status: 404,
    });
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.TEMPLATES}/${encodeURIComponent("missing name")}/export`,
    );
  });

  it("surfaces 403 non-Admin on export", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "Forbidden" }), {
        status: 403,
        statusText: "Forbidden",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(exportTemplate("perc.page")).rejects.toMatchObject({ status: 403 });
  });

  it("POST import sends raw XML not JSON", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ TemplateDetail: { name: "imported.one", label: "Imported One" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const created = await importTemplate(SAMPLE_XML);
    expect(created.name).toBe("imported.one");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.TEMPLATES}/import`);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(init.body).toBe(SAMPLE_XML);
    const headers = new Headers(init.headers);
    expect(headers.get("Content-Type")).toMatch(/application\/xml/i);
  });

  it("surfaces 400 invalid XML", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "invalid assembly-template XML" }), {
        status: 400,
        statusText: "Bad Request",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(importTemplate("<not-xml")).rejects.toMatchObject({ status: 400 });
  });

  it("surfaces 409 duplicate name", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "Template already exists: perc.page" }), {
        status: 409,
        statusText: "Conflict",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(importTemplate(SAMPLE_XML)).rejects.toMatchObject({ status: 409 });
  });

  it("surfaces 403 non-Admin on import", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: "Forbidden" }), {
        status: 403,
        statusText: "Forbidden",
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(importTemplate(SAMPLE_XML)).rejects.toMatchObject({ status: 403 });
  });
});
