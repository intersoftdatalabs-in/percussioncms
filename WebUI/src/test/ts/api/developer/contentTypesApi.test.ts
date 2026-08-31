/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  asContentTypeText,
  contentTypeSelectionKey,
  getContentTypeAllowedTemplates,
  getFieldControlProperties,
  normalizeContentTypeControlProperties,
  normalizeContentTypeDesignGaps,
  normalizeContentTypeFields,
  normalizeContentTypeStringList,
  normalizeNamedObjectRefs,
  replaceContentTypeAllowedTemplates,
  replaceFieldControlProperties,
  setContentTypeAllowedWorkflows,
  setContentTypeEnabled,
  unwrapContentTypeDetail,
  unwrapContentTypeList,
  unwrapFieldControlProperties,
  unwrapNamedObjectRefList,
  unwrapObjectLockSummary,
  updateContentTypeDetail,
  wrapContentTypeDetailForWire,
  wrapContentTypeEnabledForWire,
  wrapContentTypeWorkflowsForWire,
  wrapFieldControlPropertiesForWire,
  wrapNamedObjectRefListForWire,
  getContentTypeItemExits,
  replaceContentTypeItemExits,
  unwrapContentTypeItemExits,
  wrapContentTypeItemExitsForWire,
} from "../../../../main/ts/api/developer/contentTypesApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("unwrapContentTypeList", () => {
  it("returns bare arrays", () => {
    expect(unwrapContentTypeList([{ name: "a" }])).toEqual([{ name: "a" }]);
  });

  it("unwraps ContentType envelope", () => {
    expect(
      unwrapContentTypeList({
        ContentType: [{ name: "page", label: "Page" }],
      }),
    ).toEqual([{ name: "page", label: "Page" }]);
  });

  it("unwraps Jackson ContentTypeList root array (#3706)", () => {
    expect(
      unwrapContentTypeList({
        ContentTypeList: [{ name: "percPage", label: "Page" }],
      }),
    ).toEqual([{ name: "percPage", label: "Page" }]);
  });

  it("unwraps nested ContentTypeList.ContentType (#3706)", () => {
    expect(
      unwrapContentTypeList({
        ContentTypeList: {
          ContentType: [
            { name: "percPage", label: "Page" },
            { name: "percFileAsset", label: "File" },
          ],
        },
      }),
    ).toEqual([
      { name: "percPage", label: "Page" },
      { name: "percFileAsset", label: "File" },
    ]);
  });

  it("unwraps ArrayList envelope and empty-collection beans (#3706)", () => {
    expect(
      unwrapContentTypeList({
        ArrayList: [{ name: "percPage", label: "Page" }],
      }),
    ).toEqual([{ name: "percPage", label: "Page" }]);
    expect(unwrapContentTypeList({ empty: true })).toEqual([]);
    expect(unwrapContentTypeList({ empty: false })).toEqual([]);
  });

  it("unwraps per-item ContentType wraps inside an array (#3706)", () => {
    expect(
      unwrapContentTypeList({
        ContentTypeList: [
          { ContentType: { name: "percPage", label: "Page" } },
          { ContentType: { name: "percFileAsset", label: "File" } },
        ],
      }),
    ).toEqual([
      { name: "percPage", label: "Page" },
      { name: "percFileAsset", label: "File" },
    ]);
  });

  it("unwraps single ContentType object", () => {
    expect(unwrapContentTypeList({ ContentType: { name: "only" } })).toEqual([
      { name: "only" },
    ]);
  });

  it("handles null and empty", () => {
    expect(unwrapContentTypeList(null)).toEqual([]);
    expect(unwrapContentTypeList({})).toEqual([]);
  });

  it("never returns a non-array (catalog .map safety, #3706/#3712)", () => {
    expect(unwrapContentTypeList({ empty: true })).toEqual([]);
    expect(unwrapContentTypeList({ empty: false })).toEqual([]);
    expect(unwrapContentTypeList("nope")).toEqual([]);
    expect(
      Array.isArray(unwrapContentTypeList({ ContentTypeList: { empty: true } })),
    ).toBe(true);
  });

  it("fills guid.stringValue from host/type/uuid on list rows (#3319)", () => {
    expect(
      unwrapContentTypeList([
        { name: "percPage", guid: { hostId: 0, type: 2, uuid: 301 } },
      ]),
    ).toEqual([
      {
        name: "percPage",
        guid: { hostId: 0, type: 2, uuid: 301, stringValue: "0-2-301" },
        guidString: "0-2-301",
      },
    ]);
  });

  it("coerces JAXB-wrapped name/label so catalog Open keys exist (#3810)", () => {
    expect(
      unwrapContentTypeList([
        {
          name: { value: "percPage" },
          label: { $: "Page" },
          description: { stringValue: "A page" },
          guid: { hostId: 0, type: 2, uuid: 301 },
        },
      ]),
    ).toEqual([
      {
        name: "percPage",
        label: "Page",
        description: "A page",
        guid: { hostId: 0, type: 2, uuid: 301, stringValue: "0-2-301" },
        guidString: "0-2-301",
      },
    ]);
  });
});

describe("asContentTypeText / contentTypeSelectionKey (#3810)", () => {
  it("unwraps JAXB string wrappers", () => {
    expect(asContentTypeText("percPage")).toBe("percPage");
    expect(asContentTypeText({ value: "percPage" })).toBe("percPage");
    expect(asContentTypeText({ $: "Page" })).toBe("Page");
    expect(asContentTypeText({ stringValue: "0-2-301" })).toBe("0-2-301");
    expect(asContentTypeText({ foo: 1 })).toBe("");
    expect(asContentTypeText(null)).toBe("");
  });

  it("prefers name then guid and never returns em-dash", () => {
    expect(contentTypeSelectionKey({ name: "percPage" })).toBe("percPage");
    expect(
      contentTypeSelectionKey({ guid: { stringValue: "0-2-9" } }),
    ).toBe("0-2-9");
    expect(contentTypeSelectionKey({ name: { value: "percFile" } })).toBe("percFile");
    expect(contentTypeSelectionKey({ label: "Broken" })).toBeNull();
    expect(contentTypeSelectionKey({ name: "—" })).toBeNull();
  });
});

describe("unwrapContentTypeDetail (#3319)", () => {
  it("unwraps Jackson ContentTypeDetail root so guid is reachable", () => {
    const flat = unwrapContentTypeDetail({
      ContentTypeDetail: {
        name: "percPage",
        guid: { stringValue: "0-2-301", type: 2, uuid: 301 },
        fields: [{ name: "sys_title" }],
      },
    });
    expect(flat.name).toBe("percPage");
    expect(flat.guid?.stringValue).toBe("0-2-301");
    expect(flat.guidString).toBe("0-2-301");
    expect(flat.fields).toHaveLength(1);
  });

  it("synthesizes guid.stringValue from numeric parts under root wrap", () => {
    const flat = unwrapContentTypeDetail({
      ContentTypeDetail: {
        name: "percPage",
        guid: { hostId: 0, type: 2, uuid: 301 },
      },
    });
    expect(flat.guid?.stringValue).toBe("0-2-301");
    expect(flat.guidString).toBe("0-2-301");
  });

  it("accepts flat payload and copies guidString", () => {
    const flat = unwrapContentTypeDetail({
      name: "percPage",
      guidString: "0-2-8",
    });
    expect(flat.guid?.stringValue).toBe("0-2-8");
    expect(flat.guidString).toBe("0-2-8");
  });

  it("returns empty object for unrelated envelopes", () => {
    expect(unwrapContentTypeDetail({ Error: { message: "x" } })).toEqual({});
    expect(unwrapContentTypeDetail(null)).toEqual({});
  });

  it("unwraps ContentType root and JAXB name wraps (#3810)", () => {
    const flat = unwrapContentTypeDetail({
      ContentType: {
        name: { value: "percPage" },
        label: { $: "Page" },
        guid: { stringValue: "0-2-301" },
        fields: [],
      },
    });
    expect(flat.name).toBe("percPage");
    expect(flat.label).toBe("Page");
    expect(flat.guid?.stringValue).toBe("0-2-301");
  });

  it("keeps ContentTypeDetail root when only allowedTemplates is present", () => {
    const flat = unwrapContentTypeDetail({
      ContentTypeDetail: {
        allowedTemplates: [{ name: "t1", guid: { stringValue: "1-101-9" } }],
      },
    });
    expect(flat.allowedTemplates).toEqual([
      { name: "t1", guid: { stringValue: "1-101-9" } },
    ]);
  });

  it("normalizes Jackson empty-collection beans to [] (#3712)", () => {
    const flat = unwrapContentTypeDetail({
      name: "percArchiveList",
      fields: { empty: false },
      childFieldSets: { empty: true },
      allowedWorkflows: { empty: false },
      allowedTemplates: { empty: true },
      designGaps: { empty: true },
    });
    expect(flat.fields).toEqual([]);
    expect(flat.childFieldSets).toEqual([]);
    expect(flat.allowedWorkflows).toEqual([]);
    expect(flat.allowedTemplates).toEqual([]);
    expect(flat.designGaps).toEqual([]);
  });

  it("unwraps JAXB single-item envelopes (#3712)", () => {
    const wf = { name: "Simple Workflow", label: "Simple Workflow", isDefault: true };
    const tpl = { name: "perc.page", label: "Page" };
    const field = { name: "sys_title", label: "Title" };
    const flat = unwrapContentTypeDetail({
      ContentTypeDetail: {
        name: "percArchiveList",
        fields: { ContentTypeField: field },
        childFieldSets: { childFieldSet: "rx_shared" },
        allowedWorkflows: { NamedObjectRef: wf },
        allowedTemplates: { NamedObjectRef: tpl },
        designGaps: {
          DesignGap: { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
        },
      },
    });
    expect(flat.fields).toEqual([field]);
    expect(flat.childFieldSets).toEqual(["rx_shared"]);
    expect(flat.allowedWorkflows).toEqual([wf]);
    expect(flat.allowedTemplates).toEqual([tpl]);
    expect(flat.designGaps).toEqual([
      { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
    ]);
  });

  it("wraps a lone association object and a lone {code,message} gap (#3712)", () => {
    const wf = { name: "Simple Workflow", isDefault: true };
    const flat = unwrapContentTypeDetail({
      name: "percArchiveList",
      allowedWorkflows: wf,
      allowedTemplates: { name: "perc.page" },
      fields: { name: "sys_title" },
      childFieldSets: "rx_shared",
      designGaps: { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
    });
    expect(flat.allowedWorkflows).toEqual([wf]);
    expect(flat.allowedTemplates).toEqual([{ name: "perc.page" }]);
    expect(flat.fields).toEqual([{ name: "sys_title" }]);
    expect(flat.childFieldSets).toEqual(["rx_shared"]);
    expect(flat.designGaps).toEqual([
      { code: "CT_ITEM_EXITS", message: "Item-level pre/post exits not exposed" },
    ]);
  });
});

describe("content-type Jackson list helpers (#3712)", () => {
  it("leaves real arrays unchanged", () => {
    expect(normalizeNamedObjectRefs([{ name: "a" }])).toEqual([{ name: "a" }]);
    expect(normalizeContentTypeDesignGaps([{ code: "X", message: "m" }])).toEqual([
      { code: "X", message: "m" },
    ]);
    expect(normalizeContentTypeFields([{ name: "sys_title" }])).toEqual([{ name: "sys_title" }]);
    expect(normalizeContentTypeStringList(["a", "b"])).toEqual(["a", "b"]);
    expect(normalizeContentTypeControlProperties([{ name: "height", value: "200" }])).toEqual([
      { name: "height", value: "200" },
    ]);
  });

  it("maps nullish and primitives to []", () => {
    expect(normalizeNamedObjectRefs(undefined)).toEqual([]);
    expect(normalizeNamedObjectRefs("nope")).toEqual([]);
    expect(normalizeContentTypeDesignGaps(null)).toEqual([]);
    expect(normalizeContentTypeDesignGaps(12)).toEqual([]);
    expect(normalizeContentTypeFields(undefined)).toEqual([]);
    expect(normalizeContentTypeStringList(undefined)).toEqual([]);
    expect(normalizeContentTypeStringList(9)).toEqual([]);
    expect(normalizeContentTypeControlProperties(undefined)).toEqual([]);
    expect(normalizeContentTypeControlProperties({ empty: true })).toEqual([]);
  });

  it("wraps a lone legacy designGaps string", () => {
    expect(normalizeContentTypeDesignGaps("legacy free-text gap")).toEqual([
      "legacy free-text gap",
    ]);
  });
});

describe("wrapContentTypeDetailForWire", () => {
  it("wraps a flat update under ContentTypeDetail", () => {
    expect(wrapContentTypeDetailForWire({ description: "d", enabled: true })).toEqual({
      ContentTypeDetail: { description: "d", enabled: true },
    });
  });
});

describe("unwrapObjectLockSummary", () => {
  it("unwraps Jackson ObjectLockSummary root", () => {
    expect(
      unwrapObjectLockSummary({
        ObjectLockSummary: { session: "s1", locker: "Admin", remainingTime: 30 },
      }),
    ).toEqual({ session: "s1", locker: "Admin", remainingTime: 30 });
  });

  it("accepts a flat lock body", () => {
    expect(
      unwrapObjectLockSummary({ locker: "Admin", remainingTime: 15 }),
    ).toEqual({ locker: "Admin", remainingTime: 15 });
  });

  it("returns empty object for null", () => {
    expect(unwrapObjectLockSummary(null)).toEqual({});
  });
});

describe("updateContentTypeDetail PUT without lock wrap", () => {
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

  it("PUTs save only — lock chrome owns lock/unlock (#3781)", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeDetail: { name: "percPage", label: "Page", description: "updated" },
      }),
    );

    const saved = await updateContentTypeDetail("percPage", { description: "updated" });
    expect(saved.description).toBe("updated");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.CONTENT_TYPES}/percPage`);
    expect(String(fetchMock.mock.calls[0][0])).not.toMatch(/\/enabled$/);
    expect(JSON.parse(String(init.body))).toEqual(
      wrapContentTypeDetailForWire({ description: "updated" }),
    );
  });
});

describe("setContentTypeEnabled CD-13 dedicated PUT (#3781)", () => {
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

  it("wraps enabled under ContentTypeEnabled", () => {
    expect(wrapContentTypeEnabledForWire(false)).toEqual({
      ContentTypeEnabled: { enabled: false },
    });
    expect(wrapContentTypeEnabledForWire(true)).toEqual({
      ContentTypeEnabled: { enabled: true },
    });
  });

  it("PUTs /contenttypes/{id}/enabled without lock or unlock", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeDetail: { name: "percPage", enabled: false },
      }),
    );

    const saved = await setContentTypeEnabled("percPage", false);
    expect(saved.enabled).toBe(false);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/enabled`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      ContentTypeEnabled: { enabled: false },
    });
  });

  it("encodes idOrName on the enabled path", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ContentTypeDetail: { name: "perc Page", enabled: true } }),
    );
    await setContentTypeEnabled("perc Page", true);
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/${encodeURIComponent("perc Page")}/enabled`,
    );
  });
});

describe("wrapContentTypeWorkflowsForWire", () => {
  it("wraps allowedWorkflows under ContentTypeWorkflows", () => {
    expect(
      wrapContentTypeWorkflowsForWire({
        allowedWorkflows: [{ name: "Simple Workflow" }],
        defaultWorkflow: { name: "Simple Workflow" },
      }),
    ).toEqual({
      ContentTypeWorkflows: {
        allowedWorkflows: [{ name: "Simple Workflow" }],
        defaultWorkflow: { name: "Simple Workflow" },
      },
    });
  });
});

describe("setContentTypeAllowedWorkflows CD-08 dedicated PUT (#3782)", () => {
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

  it("PUTs /contenttypes/{id}/allowedWorkflows without lock or unlock", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeDetail: {
          name: "percPage",
          allowedWorkflows: [{ name: "Standard Workflow" }],
          defaultWorkflow: { name: "Standard Workflow" },
        },
      }),
    );

    const saved = await setContentTypeAllowedWorkflows("percPage", {
      allowedWorkflows: [{ name: "Standard Workflow" }],
      defaultWorkflow: { name: "Standard Workflow" },
    });
    expect(saved.allowedWorkflows).toEqual([{ name: "Standard Workflow" }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/allowedWorkflows`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      ContentTypeWorkflows: {
        allowedWorkflows: [{ name: "Standard Workflow" }],
        defaultWorkflow: { name: "Standard Workflow" },
      },
    });
  });
});

describe("wrapNamedObjectRefListForWire / unwrapNamedObjectRefList (CD-12)", () => {
  it("wraps a list under NamedObjectRefList", () => {
    expect(wrapNamedObjectRefListForWire([{ name: "perc.page" }])).toEqual({
      NamedObjectRefList: [{ name: "perc.page" }],
    });
  });

  it("wraps an empty list (clears associations)", () => {
    expect(wrapNamedObjectRefListForWire([])).toEqual({ NamedObjectRefList: [] });
  });

  it("unwraps WRAP_ROOT NamedObjectRefList", () => {
    expect(
      unwrapNamedObjectRefList({
        NamedObjectRefList: [{ name: "perc.page", label: "Page" }],
      }),
    ).toEqual([{ name: "perc.page", label: "Page" }]);
  });

  it("unwraps a bare array", () => {
    expect(unwrapNamedObjectRefList([{ name: "perc.page" }])).toEqual([{ name: "perc.page" }]);
  });

  it("unwraps JAXB NamedObjectRef singleton and empty beans", () => {
    expect(
      unwrapNamedObjectRefList({ NamedObjectRef: { name: "perc.page" } }),
    ).toEqual([{ name: "perc.page" }]);
    expect(unwrapNamedObjectRefList({ empty: true })).toEqual([]);
    expect(unwrapNamedObjectRefList(null)).toEqual([]);
  });
});

describe("allowedTemplates GET/PUT (CD-12)", () => {
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

  it("GETs allowedTemplates and unwraps the list", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ NamedObjectRefList: [{ name: "perc.page", label: "Page" }] }),
    );
    const listed = await getContentTypeAllowedTemplates("percPage");
    expect(listed).toEqual([{ name: "perc.page", label: "Page" }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("GET");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/allowedTemplates`,
    );
  });

  it("PUTs allowedTemplates wrapped under NamedObjectRefList and returns the set", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ NamedObjectRefList: [{ name: "perc.page.summary" }] }),
    );
    const listed = await replaceContentTypeAllowedTemplates("percPage", [
      { name: "perc.page.summary" },
    ]);
    expect(listed).toEqual([{ name: "perc.page.summary" }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("PUT");
    expect(String(url)).toContain(`${PATHS.CONTENT_TYPES}/percPage/allowedTemplates`);
    expect(JSON.parse(String(init.body))).toEqual({
      NamedObjectRefList: [{ name: "perc.page.summary" }],
    });
  });

  it("PUTs an empty list to clear associations", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ NamedObjectRefList: [] }));
    const listed = await replaceContentTypeAllowedTemplates("percPage", []);
    expect(listed).toEqual([]);
    const body = JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body));
    expect(body).toEqual({ NamedObjectRefList: [] });
  });
});

describe("unwrapContentTypeItemExits / wrapContentTypeItemExitsForWire (CD-09)", () => {
  it("wraps under ContentTypeItemExits", () => {
    expect(
      wrapContentTypeItemExitsForWire({
        inputTranslations: [],
        outputTranslations: [],
        validations: [],
      }),
    ).toEqual({
      ContentTypeItemExits: {
        inputTranslations: [],
        outputTranslations: [],
        validations: [],
      },
    });
  });

  it("unwraps WRAP_ROOT ContentTypeItemExits", () => {
    const out = unwrapContentTypeItemExits({
      ContentTypeItemExits: {
        inputTranslations: [
          {
            extension: "Java/global/percussion/generic/sys_ToUpperCase",
            parameters: [{ value: "sys_title" }],
          },
        ],
        outputTranslations: [],
        validations: [],
        preExits: [],
        postExits: [],
        maxErrorsToStopValidation: 10,
        designGaps: [{ code: "CT_ITEM_EXIT_CONDITIONS", message: "read-only" }],
      },
    });
    expect(out.inputTranslations?.[0]?.extension).toBe(
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    expect(out.inputTranslations?.[0]?.parameters).toEqual([{ value: "sys_title" }]);
    expect(out.maxErrorsToStopValidation).toBe(10);
    expect(out.designGaps).toEqual([{ code: "CT_ITEM_EXIT_CONDITIONS", message: "read-only" }]);
  });

  it("unwraps JAXB singleton lists and empty beans", () => {
    const out = unwrapContentTypeItemExits({
      inputTranslations: {
        ContentTypeItemExit: { extension: "Java/global/percussion/generic/sys_ToUpperCase" },
      },
      outputTranslations: { empty: true },
      validations: [],
    });
    expect(out.inputTranslations).toHaveLength(1);
    expect(out.outputTranslations).toEqual([]);
    expect(out.validations).toEqual([]);
  });

  it("returns empty lists for null payload", () => {
    const out = unwrapContentTypeItemExits(null);
    expect(out.inputTranslations).toEqual([]);
    expect(out.outputTranslations).toEqual([]);
    expect(out.validations).toEqual([]);
    expect(out.preExits).toEqual([]);
    expect(out.postExits).toEqual([]);
  });
});

describe("itemExits GET/PUT (CD-09)", () => {
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

  it("GETs itemExits and unwraps the envelope", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeItemExits: {
          inputTranslations: [{ extension: "Java/global/percussion/generic/sys_ToUpperCase" }],
          outputTranslations: [],
          validations: [],
          preExits: [],
          postExits: [],
        },
      }),
    );
    const listed = await getContentTypeItemExits("percPage");
    expect(listed.inputTranslations?.[0]?.extension).toBe(
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("GET");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/itemExits`,
    );
  });

  it("PUTs itemExits wrapped under ContentTypeItemExits", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeItemExits: {
          inputTranslations: [
            { extension: "Java/global/percussion/generic/sys_ToUpperCase" },
          ],
          outputTranslations: [],
          validations: [],
          preExits: [],
          postExits: [],
        },
      }),
    );
    const listed = await replaceContentTypeItemExits("percPage", {
      inputTranslations: [
        {
          extension: "Java/global/percussion/generic/sys_ToUpperCase",
          parameters: [{ value: "sys_title" }],
        },
      ],
      outputTranslations: [],
      validations: [],
    });
    expect(listed.inputTranslations).toHaveLength(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("PUT");
    expect(String(url)).toContain(`${PATHS.CONTENT_TYPES}/percPage/itemExits`);
    const body = JSON.parse(String(init.body));
    expect(body.ContentTypeItemExits).toBeTruthy();
    expect(body.ContentTypeItemExits.inputTranslations[0].extension).toBe(
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    expect(body.ContentTypeItemExits.preExits).toBeUndefined();
    expect(body.ContentTypeItemExits.postExits).toBeUndefined();
  });

  it("encodes idOrName on the itemExits path", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ContentTypeItemExits: { inputTranslations: [], outputTranslations: [], validations: [] } }),
    );
    await getContentTypeItemExits("perc Page");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/${encodeURIComponent("perc Page")}/itemExits`,
    );
  });
});

describe("wrapFieldControlPropertiesForWire / unwrapFieldControlProperties (CD-07)", () => {
  it("wraps properties under ContentTypeFieldControlProperties without choices", () => {
    expect(
      wrapFieldControlPropertiesForWire({ properties: [{ name: "height", value: "200" }] }),
    ).toEqual({
      ContentTypeFieldControlProperties: {
        properties: [{ name: "height", value: "200" }],
      },
    });
  });

  it("wraps an empty properties list (clears parameters)", () => {
    expect(wrapFieldControlPropertiesForWire({ properties: [] })).toEqual({
      ContentTypeFieldControlProperties: { properties: [] },
    });
  });

  it("unwraps WRAP_ROOT ContentTypeFieldControlProperties", () => {
    expect(
      unwrapFieldControlProperties({
        ContentTypeFieldControlProperties: {
          fieldName: "sys_title",
          control: "sys_EditBox",
          properties: [{ name: "height", value: "200" }],
          choices: { type: "local" },
        },
      }),
    ).toEqual({
      fieldName: "sys_title",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
      choices: { type: "local" },
    });
  });

  it("wraps choices when provided and unwraps extras", () => {
    expect(
      wrapFieldControlPropertiesForWire({
        properties: [{ name: "height", value: "200" }],
        choices: { type: "none" },
      }),
    ).toEqual({
      ContentTypeFieldControlProperties: {
        properties: [{ name: "height", value: "200" }],
        choices: { type: "none" },
      },
    });
    expect(
      unwrapFieldControlProperties({
        ContentTypeFieldControlProperties: {
          properties: [],
          choices: {
            type: "local",
            entries: [{ value: "open", label: "Open" }],
            nullEntry: { value: "", label: "None", includeWhen: "always" },
            defaultSelected: [{ type: "nullEntry" }],
            filter: {
              lookupHref: "../sys_lookup/filter.xml",
              dependentFields: [{ fieldRef: "sys_communityid", dependencyType: "optional" }],
            },
          },
        },
      }).choices,
    ).toEqual({
      type: "local",
      entries: [{ value: "open", label: "Open" }],
      nullEntry: { value: "", label: "None", includeWhen: "always" },
      defaultSelected: [{ type: "nullEntry" }],
      filter: {
        lookupHref: "../sys_lookup/filter.xml",
        dependentFields: [{ fieldRef: "sys_communityid", dependencyType: "optional" }],
      },
    });
  });

  it("unwraps a flat body and JAXB property singleton", () => {
    expect(
      unwrapFieldControlProperties({
        fieldName: "sys_title",
        properties: { ContentTypeControlProperty: { name: "width", value: "400" } },
      }),
    ).toEqual({
      fieldName: "sys_title",
      properties: [{ name: "width", value: "400" }],
    });
  });

  it("unwraps empty beans and null", () => {
    expect(unwrapFieldControlProperties({ empty: true })).toEqual({ properties: [] });
    expect(unwrapFieldControlProperties(null)).toEqual({ properties: [] });
  });
});

describe("field controlProperties GET/PUT (CD-07)", () => {
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

  it("GETs controlProperties and unwraps values", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeFieldControlProperties: {
          fieldName: "sys_title",
          properties: [{ name: "height", value: "200" }],
        },
      }),
    );
    const loaded = await getFieldControlProperties("percPage", "sys_title");
    expect(loaded.properties).toEqual([{ name: "height", value: "200" }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("GET");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/fields/sys_title/controlProperties`,
    );
  });

  it("encodes type and field names on GET", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ContentTypeFieldControlProperties: { properties: [] } }),
    );
    await getFieldControlProperties("perc Page", "sys/title");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/${encodeURIComponent("perc Page")}/fields/${encodeURIComponent("sys/title")}/controlProperties`,
    );
  });

  it("PUTs controlProperties wrapped under ContentTypeFieldControlProperties", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeFieldControlProperties: {
          properties: [{ name: "height", value: "201" }],
        },
      }),
    );
    const listed = await replaceFieldControlProperties("percPage", "sys_title", [
      { name: "height", value: "201" },
    ]);
    expect(listed.properties).toEqual([{ name: "height", value: "201" }]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("PUT");
    expect(String(url)).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/fields/sys_title/controlProperties`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      ContentTypeFieldControlProperties: {
        properties: [{ name: "height", value: "201" }],
      },
    });
    expect(String(init.body)).not.toContain("choices");
  });

  it("PUTs an empty properties list to clear parameters", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ContentTypeFieldControlProperties: { properties: [] } }),
    );
    const listed = await replaceFieldControlProperties("percPage", "sys_title", []);
    expect(listed.properties).toEqual([]);
    const body = JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body));
    expect(body).toEqual({ ContentTypeFieldControlProperties: { properties: [] } });
  });

  it("PUTs choices when provided and omits them otherwise (#4046)", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeFieldControlProperties: {
          properties: [],
          choices: { type: "local", entries: [{ value: "open", label: "Open" }] },
        },
      }),
    );
    await replaceFieldControlProperties("percPage", "sys_title", [], {
      type: "local",
      entries: [{ value: "open", label: "Open" }],
    });
    const withChoices = JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body));
    expect(withChoices).toEqual({
      ContentTypeFieldControlProperties: {
        properties: [],
        choices: { type: "local", entries: [{ value: "open", label: "Open" }] },
      },
    });

    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ContentTypeFieldControlProperties: { properties: [] } }),
    );
    await replaceFieldControlProperties("percPage", "sys_title", []);
    const omitted = JSON.parse(String((fetchMock.mock.calls[1][1] as RequestInit).body));
    expect(omitted).toEqual({ ContentTypeFieldControlProperties: { properties: [] } });
    expect(JSON.stringify(omitted)).not.toContain("choices");
  });
});
