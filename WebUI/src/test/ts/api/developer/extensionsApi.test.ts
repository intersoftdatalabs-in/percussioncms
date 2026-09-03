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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PATHS } from "../../../../main/ts/api/paths";
import {
  EXTENSION_DESIGN_GAPS,
  createExtension,
  deleteExtension,
  extensionClassName,
  formatExtensionInterfaces,
  getExtensionDetail,
  isExtensionWriteReady,
  isImmutableExtension,
  isImmutableExtensionContext,
  isValidExtensionName,
  listExtensions,
  normalizeExtensionName,
  parseExtensionInterfaces,
  saveExtension,
  unwrapExtension,
  withoutStaleExtensionWriteGap,
  wrapExtensionForWire,
} from "../../../../main/ts/api/developer/extensionsApi";

describe("unwrapExtension", () => {
  it("unwraps Jackson Extension root envelope", () => {
    const unwrapped = unwrapExtension({
      Extension: {
        extensionName: "my_user_ext",
        fqn: "Java/user/my_user_ext",
      },
    });
    expect(unwrapped.extensionName).toBe("my_user_ext");
    expect(unwrapped.fqn).toBe("Java/user/my_user_ext");
  });

  it("returns empty object for null payload", () => {
    expect(unwrapExtension(null)).toEqual({});
  });
});

describe("extension name and immutability helpers", () => {
  it("trims names", () => {
    expect(normalizeExtensionName("  my_ext  ")).toBe("my_ext");
    expect(normalizeExtensionName("")).toBe("");
    expect(normalizeExtensionName(null)).toBe("");
  });

  it("accepts Java identifier names and rejects junk", () => {
    expect(isValidExtensionName("my_user_ext")).toBe(true);
    expect(isValidExtensionName("MyExt")).toBe(true);
    expect(isValidExtensionName("_priv")).toBe(true);
    expect(isValidExtensionName("")).toBe(false);
    expect(isValidExtensionName("has space")).toBe(false);
    expect(isValidExtensionName("1bad")).toBe(false);
    expect(isValidExtensionName("a/b")).toBe(false);
    expect(isValidExtensionName("a-b")).toBe(false);
  });

  it("detects immutable system and handler contexts", () => {
    expect(isImmutableExtensionContext("global/percussion/")).toBe(true);
    expect(isImmutableExtensionContext("global/percussion")).toBe(true);
    expect(isImmutableExtensionContext("handlers/")).toBe(true);
    expect(isImmutableExtensionContext("user/")).toBe(false);
    expect(isImmutableExtension(null)).toBe(false);
    expect(
      isImmutableExtension({
        context: "global/percussion/",
        handlerName: "Java",
        extensionName: "sys_add",
      }),
    ).toBe(true);
    expect(
      isImmutableExtension({
        context: "user/",
        handlerName: "ExtensionHandler",
        extensionName: "Java",
      }),
    ).toBe(true);
    expect(
      isImmutableExtension({
        context: "user/",
        handlerName: "Java",
        extensionName: "my_user_ext",
      }),
    ).toBe(false);
  });

  it("requires name, interfaces, and className for Java create", () => {
    expect(
      isExtensionWriteReady({
        isNew: true,
        name: "",
        interfaces: ["com.percussion.extension.IPSUdfProcessor"],
        className: "com.example.MyExt",
      }),
    ).toBe(false);
    expect(
      isExtensionWriteReady({
        isNew: true,
        name: "my_user_ext",
        interfaces: [],
        className: "com.example.MyExt",
      }),
    ).toBe(false);
    expect(
      isExtensionWriteReady({
        isNew: true,
        name: "my_user_ext",
        interfaces: ["com.percussion.extension.IPSUdfProcessor"],
        className: "",
      }),
    ).toBe(false);
    expect(
      isExtensionWriteReady({
        isNew: true,
        name: "my_user_ext",
        interfaces: ["com.percussion.extension.IPSUdfProcessor"],
        className: "com.example.MyExt",
      }),
    ).toBe(true);
    expect(
      isExtensionWriteReady({
        isNew: false,
        name: "my_user_ext",
        interfaces: ["com.percussion.extension.IPSUdfProcessor"],
        className: "com.example.MyExt",
        immutable: true,
      }),
    ).toBe(false);
  });

  it("parses and formats interface lists", () => {
    expect(parseExtensionInterfaces("a\nb\n\nc")).toEqual(["a", "b", "c"]);
    expect(formatExtensionInterfaces(["a", "b"])).toBe("a\nb");
    expect(extensionClassName({ className: " com.example.X " })).toBe("com.example.X");
    expect(extensionClassName(undefined)).toBe("");
  });
});

describe("extension wire wrap", () => {
  it("wraps POST/PUT under Extension root", () => {
    expect(
      wrapExtensionForWire({
        extensionName: "my_user_ext",
        supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
        initParameters: { className: "com.example.MyExt" },
      }),
    ).toEqual({
      Extension: {
        extensionName: "my_user_ext",
        supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
        initParameters: { className: "com.example.MyExt" },
      },
    });
  });

  it("filters a stale REST write gap on GET detail", () => {
    expect(
      withoutStaleExtensionWriteGap([
        "Extension install / remove not supported via this API",
        "Workbench parameter dialog parity beyond fields on the wire DTO",
      ]),
    ).toEqual(["Workbench parameter dialog parity beyond fields on the wire DTO"]);
    expect(EXTENSION_DESIGN_GAPS.some((g) => /install/i.test(g))).toBe(false);
  });
});

describe("extensionsApi write paths", () => {
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

  it("lists and loads extensions", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse([{ extensionName: "sys_add", context: "global/percussion/" }]),
    );
    const list = await listExtensions();
    expect(list[0].extensionName).toBe("sys_add");
    expect(list[0].designGaps?.length).toBeGreaterThan(0);

    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        Extension: { extensionName: "sys_add", fqn: "Java/global/percussion/sys_add" },
      }),
    );
    const detail = await getExtensionDetail("sys_add");
    expect(detail.fqn).toBe("Java/global/percussion/sys_add");
    expect(String(fetchMock.mock.calls[1][0])).toContain(`${PATHS.EXTENSIONS}/item?key=`);
  });

  it("POSTs create body to /services/extensions", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        extensionName: "my_user_ext",
        fqn: "Java/user/my_user_ext",
        context: "user/",
      }),
    );
    const saved = await createExtension({
      extensionName: "my_user_ext",
      supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
      initParameters: { className: "com.example.MyExt" },
    });
    expect(saved.extensionName).toBe("my_user_ext");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toBe(PATHS.EXTENSIONS_ROOT);
    expect(JSON.parse(String(init.body))).toEqual({
      Extension: {
        extensionName: "my_user_ext",
        supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
        initParameters: { className: "com.example.MyExt" },
      },
    });
  });

  it("PUTs save body to catalog/item?key=", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ extensionName: "my_user_ext", deprecated: true }),
    );
    const saved = await saveExtension("my_user_ext", {
      extensionName: "my_user_ext",
      supportedInterfaces: ["com.percussion.extension.IPSUdfProcessor"],
      initParameters: { className: "com.example.MyExt" },
      deprecated: true,
    });
    expect(saved.deprecated).toBe(true);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.EXTENSIONS}/item?key=${encodeURIComponent("my_user_ext")}`,
    );
  });

  it("DELETEs catalog/item?key=", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteExtension("Java/user/my_user_ext");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.EXTENSIONS}/item?key=${encodeURIComponent("Java/user/my_user_ext")}`,
    );
  });
});
