/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  addSystemDefField,
  deleteSystemDefField,
  getSystemDef,
  getSystemDefFieldControlProperties,
  isSystemDefFieldAddReady,
  isValidSystemDefFieldName,
  replaceSystemDefFieldControlProperties,
  unwrapSystemDefControlProperties,
  unwrapSystemDefDetail,
  unwrapSystemDefStylesheets,
  updateSystemDef,
  wrapSystemDefControlPropertiesForWire,
  wrapSystemDefDetailForWire,
  wrapSystemDefFieldForWire,
  wrapSystemDefStylesheetsForWire,
  wrapSystemDefApplicationFlowForWire,
  getSystemDefStylesheets,
  replaceSystemDefStylesheets,
  getSystemDefApplicationFlow,
  replaceSystemDefApplicationFlow,
  isValidSystemDefCommandHandler,
  isValidSystemDefStylesheetHref,
  isValidSystemDefApplicationFlowHref,
  unwrapSystemDefApplicationFlow,
} from "../../../../main/ts/api/developer/systemDefApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("system def field name validation", () => {
  it("accepts letter-start word names and rejects junk", () => {
    expect(isValidSystemDefFieldName("sys_title")).toBe(true);
    expect(isValidSystemDefFieldName("qa4030ab")).toBe(true);
    expect(isValidSystemDefFieldName("A")).toBe(true);
    expect(isValidSystemDefFieldName("")).toBe(false);
    expect(isValidSystemDefFieldName("1bad")).toBe(false);
    expect(isValidSystemDefFieldName("has space")).toBe(false);
    expect(isValidSystemDefFieldName("bad-name")).toBe(false);
    expect(isValidSystemDefFieldName("a".repeat(51))).toBe(false);
    expect(isValidSystemDefFieldName(" sys_title")).toBe(false);
    expect(isValidSystemDefFieldName("sys_title ")).toBe(false);
  });

  it("disables add until the name is valid", () => {
    expect(isSystemDefFieldAddReady("")).toBe(false);
    expect(isSystemDefFieldAddReady("1x")).toBe(false);
    expect(isSystemDefFieldAddReady("qa_field")).toBe(true);
  });
});

describe("system def wire wrap", () => {
  it("wraps PUT under SystemDefDetail root", () => {
    expect(
      wrapSystemDefDetailForWire({
        fields: [{ name: "sys_title", searchable: true, occurrence: "required" }],
      }),
    ).toEqual({
      SystemDefDetail: {
        fields: [{ name: "sys_title", searchable: true, occurrence: "required" }],
      },
    });
  });

  it("wraps POST under SystemDefField root", () => {
    expect(wrapSystemDefFieldForWire({ name: "qa_note", dataType: "text" })).toEqual({
      SystemDefField: { name: "qa_note", dataType: "text" },
    });
  });

  it("unwraps SystemDefDetail envelope, JAXB one-item fields, and flat bodies", () => {
    expect(
      unwrapSystemDefDetail({
        SystemDefDetail: {
          fieldCount: 1,
          fields: { SystemDefField: { name: "sys_title" } },
          designGaps: "control",
        },
      }),
    ).toEqual({
      fieldCount: 1,
      cacheTimeoutMinutes: undefined,
      fields: [{ name: "sys_title" }],
      designGaps: ["control"],
    });
    expect(unwrapSystemDefDetail({ fieldCount: 0, fields: [], designGaps: [] })).toEqual({
      fieldCount: 0,
      cacheTimeoutMinutes: undefined,
      fields: [],
      designGaps: [],
    });
    expect(unwrapSystemDefDetail(null)).toEqual({ fields: [] });
  });

  it("wraps PUT control properties under SystemDefControlProperties root", () => {
    expect(
      wrapSystemDefControlPropertiesForWire({
        properties: [{ name: "height", value: "200" }],
      }),
    ).toEqual({
      SystemDefControlProperties: {
        properties: [{ name: "height", value: "200" }],
      },
    });
  });

  it("unwraps SystemDefControlProperties envelope and flat bodies", () => {
    expect(
      unwrapSystemDefControlProperties({
        SystemDefControlProperties: {
          fieldName: "sys_title",
          control: "sys_EditBox",
          properties: [{ name: "height", value: "200" }],
        },
      }),
    ).toEqual({
      fieldName: "sys_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
    });
    expect(unwrapSystemDefControlProperties({ properties: [] })).toEqual({
      properties: [],
    });
    expect(unwrapSystemDefControlProperties(null)).toEqual({ properties: [] });
  });

  it("wraps PUT stylesheets under SystemDefStylesheets root", () => {
    expect(
      wrapSystemDefStylesheetsForWire({
        handlers: [
          {
            commandHandler: "preview",
            href: "file:../sys_resources/stylesheets/activeEdit.xsl",
          },
        ],
      }),
    ).toEqual({
      SystemDefStylesheets: {
        handlers: [
          {
            commandHandler: "preview",
            href: "file:../sys_resources/stylesheets/activeEdit.xsl",
          },
        ],
      },
    });
  });

  it("unwraps SystemDefStylesheets envelope and JAXB one-item handlers", () => {
    expect(
      unwrapSystemDefStylesheets({
        SystemDefStylesheets: {
          handlers: {
            SystemDefCommandHandlerStylesheet: {
              commandHandler: "preview",
              href: "file:../sys_resources/stylesheets/activeEdit.xsl",
            },
          },
        },
      }),
    ).toEqual({
      handlers: [
        {
          commandHandler: "preview",
          href: "file:../sys_resources/stylesheets/activeEdit.xsl",
        },
      ],
    });
    expect(unwrapSystemDefStylesheets(null)).toEqual({ handlers: [] });
  });

  it("validates command handler names and stylesheet hrefs", () => {
    expect(isValidSystemDefCommandHandler("preview")).toBe(true);
    expect(isValidSystemDefCommandHandler("qa4452")).toBe(true);
    expect(isValidSystemDefCommandHandler("1bad")).toBe(false);
    expect(isValidSystemDefCommandHandler("has space")).toBe(false);
    expect(
      isValidSystemDefStylesheetHref("file:../sys_resources/stylesheets/activeEdit.xsl"),
    ).toBe(true);
    expect(isValidSystemDefStylesheetHref("https://evil.example/x.xsl")).toBe(false);
    expect(
      isValidSystemDefStylesheetHref("file:../sys_resources/stylesheets/../x.xsl"),
    ).toBe(false);
    expect(isValidSystemDefApplicationFlowHref("../sys_cx/mainpage.html")).toBe(true);
    expect(isValidSystemDefApplicationFlowHref("")).toBe(true);
    expect(isValidSystemDefApplicationFlowHref("https://evil.example/x.html")).toBe(false);
    expect(isValidSystemDefApplicationFlowHref("../sys_cx/../x.html")).toBe(false);
  });

  it("wraps PUT application flow under SystemDefApplicationFlow root", () => {
    expect(
      wrapSystemDefApplicationFlowForWire({
        handlers: [
          {
            commandHandler: "relate",
            href: "../sys_cx/mainpage.html",
          },
        ],
      }),
    ).toEqual({
      SystemDefApplicationFlow: {
        handlers: [
          {
            commandHandler: "relate",
            href: "../sys_cx/mainpage.html",
          },
        ],
      },
    });
  });

  it("unwraps SystemDefApplicationFlow envelope and JAXB one-item handlers", () => {
    expect(
      unwrapSystemDefApplicationFlow({
        SystemDefApplicationFlow: {
          handlers: {
            SystemDefCommandHandlerRedirect: {
              commandHandler: "relate",
              href: "../sys_cx/mainpage.html",
            },
          },
        },
      }),
    ).toEqual({
      handlers: [
        {
          commandHandler: "relate",
          href: "../sys_cx/mainpage.html",
        },
      ],
    });
    expect(unwrapSystemDefApplicationFlow(null)).toEqual({ handlers: [] });
  });
});

describe("systemDefApi write paths", () => {
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

  it("GETs and unwraps /services/systemdef", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SystemDefDetail: { fieldCount: 1, fields: [{ name: "sys_title" }] },
      }),
    );
    const detail = await getSystemDef();
    expect(detail.fields?.[0]?.name).toBe("sys_title");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.SYSTEM_DEF);
  });

  it("PUTs wrapped body to /services/systemdef", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ fields: [{ name: "sys_title", searchable: false }] }),
    );
    const saved = await updateSystemDef({
      fields: [{ name: "sys_title", searchable: false, occurrence: "optional" }],
    });
    expect(saved.fields?.[0]?.searchable).toBe(false);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefDetail: {
        fields: [{ name: "sys_title", searchable: false, occurrence: "optional" }],
      },
    });
  });

  it("POSTs wrapped add body to /services/systemdef/fields", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ fields: [{ name: "qa_note" }] }));
    const saved = await addSystemDefField({ name: "qa_note", dataType: "text" });
    expect(saved.fields?.[0]?.name).toBe("qa_note");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SYSTEM_DEF}/fields`);
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefField: { name: "qa_note", dataType: "text" },
    });
  });

  it("DELETEs /services/systemdef/fields/{fieldName}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteSystemDefField("qa_note");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SYSTEM_DEF}/fields/qa_note`);
  });

  it("GETs wrapped control properties", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SystemDefControlProperties: {
          fieldName: "sys_title",
          properties: [{ name: "height", value: "200" }],
        },
      }),
    );
    const out = await getSystemDefFieldControlProperties("sys_title");
    expect(out.properties?.[0]?.value).toBe("200");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.SYSTEM_DEF}/fields/sys_title/controlProperties`,
    );
  });

  it("PUTs wrapped control properties", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        properties: [{ name: "width", value: "640" }],
      }),
    );
    const saved = await replaceSystemDefFieldControlProperties("sys_title", {
      properties: [{ name: "width", value: "640" }],
    });
    expect(saved.properties?.[0]?.value).toBe("640");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefControlProperties: {
        properties: [{ name: "width", value: "640" }],
      },
    });
  });

  it("GETs and unwraps /services/systemdef/stylesheets", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SystemDefStylesheets: {
          handlers: [
            {
              commandHandler: "preview",
              href: "file:../sys_resources/stylesheets/activeEdit.xsl",
            },
          ],
        },
      }),
    );
    const loaded = await getSystemDefStylesheets();
    expect(loaded.handlers?.[0]?.commandHandler).toBe("preview");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SYSTEM_DEF}/stylesheets`);
  });

  it("PUTs wrapped stylesheets", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        handlers: [
          {
            commandHandler: "preview",
            href: "file:../sys_resources/stylesheets/contentEdit.xsl",
          },
        ],
      }),
    );
    const saved = await replaceSystemDefStylesheets({
      handlers: [
        {
          commandHandler: "preview",
          href: "file:../sys_resources/stylesheets/contentEdit.xsl",
        },
      ],
    });
    expect(saved.handlers?.[0]?.href).toContain("contentEdit.xsl");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefStylesheets: {
        handlers: [
          {
            commandHandler: "preview",
            href: "file:../sys_resources/stylesheets/contentEdit.xsl",
          },
        ],
      },
    });
  });

  it("GETs and unwraps /services/systemdef/applicationFlow", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SystemDefApplicationFlow: {
          handlers: [
            {
              commandHandler: "relate",
              href: "../sys_cx/mainpage.html",
            },
          ],
        },
      }),
    );
    const loaded = await getSystemDefApplicationFlow();
    expect(loaded.handlers?.[0]?.commandHandler).toBe("relate");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SYSTEM_DEF}/applicationFlow`);
  });

  it("PUTs wrapped application flow", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        handlers: [
          {
            commandHandler: "relate",
            href: "../sys_action/checkoutedit.xml",
          },
        ],
      }),
    );
    const saved = await replaceSystemDefApplicationFlow({
      handlers: [
        {
          commandHandler: "relate",
          href: "../sys_action/checkoutedit.xml",
        },
      ],
    });
    expect(saved.handlers?.[0]?.href).toContain("checkoutedit.xml");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefApplicationFlow: {
        handlers: [
          {
            commandHandler: "relate",
            href: "../sys_action/checkoutedit.xml",
          },
        ],
      },
    });
  });
});
