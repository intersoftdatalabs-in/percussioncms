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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  TEMPLATE_DETAIL_ROOT,
  createTemplate,
  getTemplateDetail,
  unwrapTemplateDetail,
  updateTemplateDetail,
  wrapTemplateDetailForWire,
} from "../../../../main/ts/api/developer/assemblyApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("unwrapTemplateDetail / wrapTemplateDetailForWire (#3039)", () => {
  it("unwraps Jackson TemplateDetail root with templateSource", () => {
    const source = "#header()\n$body\n";
    const flat = unwrapTemplateDetail({
      [TEMPLATE_DETAIL_ROOT]: {
        name: "perc.page",
        label: "Page",
        templateSource: source,
        bindings: [{ executionOrder: 1, variable: "$x", expression: "1" }],
      },
    });
    expect(flat.name).toBe("perc.page");
    expect(flat.templateSource).toBe(source);
    expect(flat.bindings?.[0]?.variable).toBe("$x");
  });

  it("accepts flat payload for tests / already-unwrapped clients", () => {
    const flat = unwrapTemplateDetail({
      name: "site.base",
      templateSource: "#footer()\n",
    });
    expect(flat.templateSource).toBe("#footer()\n");
  });

  it("preserves intentionally empty source", () => {
    const flat = unwrapTemplateDetail({
      TemplateDetail: { name: "empty", templateSource: "" },
    });
    expect(flat.templateSource).toBe("");
  });

  it("returns empty object for unrelated envelopes", () => {
    expect(unwrapTemplateDetail({ Error: { message: "x" } })).toEqual({});
    expect(unwrapTemplateDetail(null)).toEqual({});
  });

  it("wrapTemplateDetailForWire nests under TemplateDetail root", () => {
    const body = { templateSource: "#new\n", label: "L" };
    expect(wrapTemplateDetailForWire(body)).toEqual({
      TemplateDetail: body,
    });
  });
});

describe("getTemplateDetail / updateTemplateDetail wire binding (#3039)", () => {
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

  it("getTemplateDetail unwraps WRAP_ROOT payload so templateSource is bound", async () => {
    const source = "<html>$sys.item</html>";
    fetchMock.mockResolvedValue(
      jsonResponse({
        TemplateDetail: {
          name: "perc.page",
          templateSource: source,
        },
      }),
    );

    const d = await getTemplateDetail("perc.page");
    expect(d.templateSource).toBe(source);
    expect(d.name).toBe("perc.page");
    expect(fetchMock).toHaveBeenCalled();
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain(`${PATHS.TEMPLATES}/`);
    expect(url).toContain(encodeURIComponent("perc.page"));
  });

  it("updateTemplateDetail wraps request and unwraps response", async () => {
    const savedSource = "#saved\n";
    fetchMock.mockResolvedValue(
      jsonResponse({
        TemplateDetail: {
          name: "site.base",
          templateSource: savedSource,
        },
      }),
    );

    const out = await updateTemplateDetail("site.base", {
      templateSource: savedSource,
    });
    expect(out.templateSource).toBe(savedSource);

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    const sent = JSON.parse(String(init.body));
    expect(sent).toEqual({
      TemplateDetail: { templateSource: savedSource },
    });
  });

  it("createTemplate wraps request and unwraps response", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        TemplateDetail: {
          name: "site.html.snippet",
          assembler: "Java/global/percussion/assembly/htmlAssembler",
        },
      }),
    );

    const out = await createTemplate({
      name: "site.html.snippet",
      assembler: "Java/global/percussion/assembly/htmlAssembler",
    });
    expect(out.name).toBe("site.html.snippet");

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    const sent = JSON.parse(String(init.body));
    expect(sent).toEqual({
      TemplateDetail: {
        name: "site.html.snippet",
        assembler: "Java/global/percussion/assembly/htmlAssembler",
      },
    });
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.TEMPLATES);
  });
});
