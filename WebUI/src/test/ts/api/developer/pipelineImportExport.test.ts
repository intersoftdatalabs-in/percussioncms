/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  asPipelineApplicationExportXml,
  exportPipelineApplication,
  importPipelineApplication,
  invalidPipelineApplicationImportName,
  pipelineApplicationExportFilename,
  pipelineApplicationNameFromDesignXml,
  rewritePipelineApplicationXmlName,
} from "../../../../main/ts/api/developer/pipelineImportExport";
import { PATHS } from "../../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

const SAMPLE_XML =
  '<PSXApplication id="14" version="2.0">' +
  "<name>sys_export</name>" +
  "<description>demo</description>" +
  "<requestRoot>sys_export</requestRoot>" +
  "</PSXApplication>";

describe("pipelineImportExport filenames", () => {
  it("pipelineApplicationExportFilename sanitizes separators", () => {
    expect(pipelineApplicationExportFilename("sys_export")).toBe("sys_export.xml");
    expect(pipelineApplicationExportFilename("a/b")).toBe("a_b.xml");
    expect(pipelineApplicationExportFilename("   ")).toBe("pipeline.xml");
    expect(pipelineApplicationExportFilename("x.XML")).toBe("x.XML");
  });

  it("asPipelineApplicationExportXml rejects non-XML payloads", () => {
    expect(asPipelineApplicationExportXml(SAMPLE_XML)).toBe(SAMPLE_XML);
    expect(() => asPipelineApplicationExportXml({ name: "x" })).toThrow(
      /did not return XML/,
    );
    expect(() => asPipelineApplicationExportXml("oops")).toThrow(/did not return XML/);
  });
});

describe("pipelineImportExport design XML", () => {
  it("pipelineApplicationNameFromDesignXml reads the PSXApplication name", () => {
    expect(pipelineApplicationNameFromDesignXml(SAMPLE_XML)).toBe("sys_export");
    expect(pipelineApplicationNameFromDesignXml("<ItemDefData/>")).toBe("");
    expect(pipelineApplicationNameFromDesignXml("")).toBe("");
  });

  it("rewritePipelineApplicationXmlName renames name + default requestRoot", () => {
    const out = rewritePipelineApplicationXmlName(SAMPLE_XML, "pw_copy");
    expect(out).toContain("<name>pw_copy</name>");
    expect(out).toContain("<requestRoot>pw_copy</requestRoot>");
    expect(out).not.toContain("sys_export");
  });

  it("rewritePipelineApplicationXmlName keeps a custom requestRoot", () => {
    const custom = SAMPLE_XML.replace(
      "<requestRoot>sys_export</requestRoot>",
      "<requestRoot>custom/root</requestRoot>",
    );
    const out = rewritePipelineApplicationXmlName(custom, "pw_copy");
    expect(out).toContain("<name>pw_copy</name>");
    expect(out).toContain("<requestRoot>custom/root</requestRoot>");
  });

  it("rewritePipelineApplicationXmlName rejects invalid input", () => {
    expect(() => rewritePipelineApplicationXmlName("", "x")).toThrow(/required/);
    expect(() => rewritePipelineApplicationXmlName(SAMPLE_XML, " ")).toThrow(
      /name is required/,
    );
    expect(() => rewritePipelineApplicationXmlName("<ItemDefData/>", "x")).toThrow(
      /invalid pipeline application design XML/,
    );
    expect(() =>
      rewritePipelineApplicationXmlName('<PSXApplication id="1"/>', "x"),
    ).toThrow(/missing name/);
  });

  it("invalidPipelineApplicationImportName mirrors server rules", () => {
    expect(invalidPipelineApplicationImportName("plain_app-1")).toBeNull();
    expect(invalidPipelineApplicationImportName("  ")).toBe("name is required");
    expect(invalidPipelineApplicationImportName("../evil")).toContain("path separators");
    expect(invalidPipelineApplicationImportName("a/b")).toContain("path separators");
    expect(invalidPipelineApplicationImportName("a:b")).toContain("portable installs");
    expect(invalidPipelineApplicationImportName("a*b")).toContain("portable installs");
  });
});

describe("pipelineImportExport wire calls", () => {
  it("exportPipelineApplication downloads XML with header filename", async () => {
    const headers = new Headers({
      "Content-Disposition": 'attachment; filename="sys_export.xml"',
    });
    const spy = vi
      .spyOn(client, "getWithHeaders")
      .mockResolvedValue({ data: SAMPLE_XML, headers, status: 200 });

    const out = await exportPipelineApplication("sys_export");

    expect(spy).toHaveBeenCalledWith(
      `${PATHS.PIPELINES}/sys_export/export`,
      expect.objectContaining({ Accept: expect.stringContaining("application/xml") }),
    );
    expect(out.xml).toBe(SAMPLE_XML);
    expect(out.filename).toBe("sys_export.xml");
  });

  it("exportPipelineApplication falls back to XML-derived filename", async () => {
    const spy = vi
      .spyOn(client, "getWithHeaders")
      .mockResolvedValue({ data: SAMPLE_XML, headers: new Headers(), status: 200 });

    const out = await exportPipelineApplication("sys_export");

    expect(spy).toHaveBeenCalled();
    expect(out.filename).toBe("sys_export.xml");
  });

  it("importPipelineApplication posts XML and unwraps the detail", async () => {
    const spy = vi
      .spyOn(client, "postText")
      .mockResolvedValue({ ApplicationDetail: { name: "pw_copy", active: false } });

    const out = await importPipelineApplication(SAMPLE_XML);

    expect(spy).toHaveBeenCalledWith(
      `${PATHS.PIPELINES}/import`,
      SAMPLE_XML,
      expect.objectContaining({ "Content-Type": "application/xml" }),
    );
    expect(out.name).toBe("pw_copy");
  });
});
