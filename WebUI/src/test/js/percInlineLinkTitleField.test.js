/**
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

/**
 * Client residual for custom inline-link title field (#2243 / parent #946 slice 4).
 *
 * Covers:
 *   - PercPathService.getInlineRenderLink titleField query wiring (dual tree)
 *   - TinyMCE option registration + getInlineLinkTitleField trim/empty peers
 *     (percadvlink / percadvimage / rxinline source contract)
 *
 * Server resolve/fallback is covered by PSInlineLinkTitleResolverTest +
 * PSRenderLinkServiceInlineTitleTest. Playwright residual lives under
 * modules/perc-qa-automation (bug-2243-inline-link-title-field.spec.js).
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, "../../../..");
const CM_ROOT = resolve(__dirname, "../../main/webapp/cm");

const PATH_SERVICE_COPIES = [
  {
    label: "cm/services",
    path: resolve(CM_ROOT, "services/PercPathService.js"),
  },
  {
    label: "war/services",
    path: resolve(__dirname, "../../../war/services/PercPathService.js"),
  },
  {
    label: "legacy/services",
    path: resolve(CM_ROOT, "app/js/legacy/services/PercPathService.js"),
  },
];

const TINYMCE_PLUGINS = [
  {
    name: "percadvlink",
    path: resolve(
      REPO_ROOT,
      "modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percadvlink/plugin.js",
    ),
  },
  {
    name: "percadvimage",
    path: resolve(
      REPO_ROOT,
      "modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percadvimage/plugin.js",
    ),
  },
  {
    name: "rxinline",
    path: resolve(
      REPO_ROOT,
      "modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/rxinline/plugin.js",
    ),
  },
];

const RENDER_LINK_PREVIEW =
  "/Rhythmyx/services/pagemanagement/renderlink/preview";

/**
 * Pure peer of TinyMCE getInlineLinkTitleField() — option value trim / empty.
 * Product keeps this nested in plugins; tests assert source contract + this peer.
 *
 * @param {unknown} raw
 * @returns {string}
 */
export function normalizeInlineLinkTitleField(raw) {
  if (raw == null) {
    return "";
  }
  return String(raw).trim();
}

/**
 * Pure peer of PercPathService URL construction for titleField pass-through.
 *
 * @param {string} basePreviewPath e.g. RENDER_LINK_PREVIEW constant
 * @param {string} itemId
 * @param {unknown} [titleField]
 * @returns {string}
 */
export function buildInlineRenderLinkPreviewUrl(
  basePreviewPath,
  itemId,
  titleField,
) {
  let svcUrl = `${basePreviewPath}/${itemId}/default`;
  if (titleField != null && String(titleField).trim() !== "") {
    svcUrl += "?titleField=" + encodeURIComponent(String(titleField).trim());
  }
  return svcUrl;
}

/**
 * Client peer of PSInlineLinkTitleResolver.resolve (documented fallback chain).
 * Server is authoritative; this keeps the client residual contract green without
 * product rework.
 *
 * @param {string|null|undefined} configuredFieldName
 * @param {Record<string, unknown>|null|undefined} fields
 * @param {string|null|undefined} typeDefault
 * @returns {string}
 */
export function resolveInlineLinkTitleClient(
  configuredFieldName,
  fields,
  typeDefault,
) {
  const map = fields && typeof fields === "object" ? fields : {};
  const fieldAsString = (name) => {
    if (name == null || String(name).trim() === "") {
      return null;
    }
    const key = String(name).trim();
    let value = map[key];
    if (value == null) {
      const lower = key.toLowerCase();
      const hit = Object.keys(map).find((k) => k.toLowerCase() === lower);
      value = hit != null ? map[hit] : null;
    }
    if (value == null) {
      return null;
    }
    const s = String(value).trim();
    return s === "" ? null : s;
  };

  if (
    configuredFieldName != null &&
    String(configuredFieldName).trim() !== ""
  ) {
    const configured = fieldAsString(configuredFieldName);
    if (configured != null) {
      return configured;
    }
    const cfg = String(configuredFieldName).trim();
    if (cfg.toLowerCase() !== "displaytitle") {
      const displayTitle = fieldAsString("displaytitle");
      if (displayTitle != null) {
        return displayTitle;
      }
    }
  }
  return typeDefault == null ? "" : String(typeDefault);
}

let $;
let makeJsonRequestSpy;

function installPathService(sourcePath) {
  document.body.innerHTML = "";
  if (typeof jquery === "function") {
    $ = jquery(globalThis.window);
    if (typeof $ !== "function" || !$.fn) {
      $ = jquery;
      if (!$.fn) $.fn = $.prototype;
    }
  } else {
    $ = globalThis.window.jQuery || globalThis.window.$;
    if (!$.fn) $.fn = $.prototype;
  }

  makeJsonRequestSpy = vi.fn(function (url, type, sync, callback) {
    makeJsonRequestSpy.last = { url, type, sync, callback };
  });

  $.PercServiceUtils = {
    STATUS_SUCCESS: "success",
    STATUS_ERROR: "error",
    TYPE_GET: "GET",
    makeJsonRequest: makeJsonRequestSpy,
    extractDefaultErrorMessage: vi.fn(() => "default error"),
  };
  $.perc_paths = {
    RENDER_LINK_PREVIEW,
  };
  // Unused by getInlineRenderLink but required by other PercPathService members
  // if the IIFE ever evaluates more aggressively.
  $.perc_utils = {
    getDisplayFormat: vi.fn(() => "default"),
  };
  $.Percussion = {
    getCurrentFinderView: vi.fn(() => null),
    PERC_FINDER_SEARCH_RESULTS: "search",
    PERC_FINDER_RESULT: "result",
  };
  $.PercNavigationManager = {
    getPath: vi.fn(() => "/"),
  };

  const factory = new Function(
    "jQuery",
    "$",
    readFileSync(sourcePath, "utf8") + "\nreturn $.PercPathService;",
  );
  return factory($, $);
}

describe("normalizeInlineLinkTitleField (TinyMCE option peer)", () => {
  it("returns empty for null/undefined", () => {
    expect(normalizeInlineLinkTitleField(null)).toBe("");
    expect(normalizeInlineLinkTitleField(undefined)).toBe("");
  });

  it("trims whitespace and preserves field name", () => {
    expect(normalizeInlineLinkTitleField("  page_title  ")).toBe("page_title");
    expect(normalizeInlineLinkTitleField("pagetitle")).toBe("pagetitle");
  });

  it("empty string and whitespace-only become empty (server type default)", () => {
    expect(normalizeInlineLinkTitleField("")).toBe("");
    expect(normalizeInlineLinkTitleField("   ")).toBe("");
  });
});

describe("buildInlineRenderLinkPreviewUrl (PercPathService peer)", () => {
  it("omits titleField when absent, null, or blank", () => {
    const base = RENDER_LINK_PREVIEW;
    expect(buildInlineRenderLinkPreviewUrl(base, "123")).toBe(
      `${base}/123/default`,
    );
    expect(buildInlineRenderLinkPreviewUrl(base, "123", null)).toBe(
      `${base}/123/default`,
    );
    expect(buildInlineRenderLinkPreviewUrl(base, "123", "  ")).toBe(
      `${base}/123/default`,
    );
  });

  it("appends encoded titleField query when non-blank", () => {
    expect(
      buildInlineRenderLinkPreviewUrl(RENDER_LINK_PREVIEW, "abc", "page_title"),
    ).toBe(`${RENDER_LINK_PREVIEW}/abc/default?titleField=page_title`);
    expect(
      buildInlineRenderLinkPreviewUrl(
        RENDER_LINK_PREVIEW,
        "id1",
        "  custom field  ",
      ),
    ).toBe(`${RENDER_LINK_PREVIEW}/id1/default?titleField=custom%20field`);
  });
});

describe("resolveInlineLinkTitleClient (server resolver peer contract)", () => {
  it("blank config uses type default only (BC)", () => {
    expect(
      resolveInlineLinkTitleClient(null, { pagetitle: "X" }, "Link Title"),
    ).toBe("Link Title");
    expect(
      resolveInlineLinkTitleClient("  ", { pagetitle: "X" }, "Link Title"),
    ).toBe("Link Title");
  });

  it("uses configured field when present and non-blank", () => {
    expect(
      resolveInlineLinkTitleClient(
        "pagetitle",
        {
          pagetitle: "Custom",
          displaytitle: "Disp",
          resource_link_title: "Nav",
        },
        "Nav",
      ),
    ).toBe("Custom");
  });

  it("falls back to displaytitle then type default", () => {
    expect(
      resolveInlineLinkTitleClient(
        "missing",
        { displaytitle: "Disp", resource_link_title: "Nav" },
        "Nav",
      ),
    ).toBe("Disp");
    expect(
      resolveInlineLinkTitleClient(
        "missing",
        { resource_link_title: "Nav" },
        "Nav",
      ),
    ).toBe("Nav");
    expect(resolveInlineLinkTitleClient("missing", {}, null)).toBe("");
  });

  it("does not double-apply displaytitle when configured field is displaytitle", () => {
    expect(
      resolveInlineLinkTitleClient("displaytitle", {}, "type-default"),
    ).toBe("type-default");
  });

  it("matches field names case-insensitively", () => {
    expect(
      resolveInlineLinkTitleClient("PageTitle", { pagetitle: "Hit" }, "def"),
    ).toBe("Hit");
  });
});

describe.each(PATH_SERVICE_COPIES)(
  "PercPathService.getInlineRenderLink ($label)",
  ({ path: servicePath }) => {
    let service;

    beforeEach(() => {
      service = installPathService(servicePath);
    });

    it("GETs preview without query when titleField omitted", () => {
      const cb = vi.fn();
      service.getInlineRenderLink("42", cb);
      expect(makeJsonRequestSpy).toHaveBeenCalledTimes(1);
      const [url, type] = makeJsonRequestSpy.mock.calls[0];
      expect(url).toBe(`${RENDER_LINK_PREVIEW}/42/default`);
      expect(type).toBe("GET");
      expect(url).not.toContain("titleField");
    });

    it("appends ?titleField= for custom control setting", () => {
      const cb = vi.fn();
      service.getInlineRenderLink("99", cb, "page_title");
      const url = makeJsonRequestSpy.mock.calls[0][0];
      expect(url).toBe(
        `${RENDER_LINK_PREVIEW}/99/default?titleField=page_title`,
      );
    });

    it("trims and encodes titleField; blank skips query", () => {
      service.getInlineRenderLink("1", vi.fn(), "  resource_link_title  ");
      expect(makeJsonRequestSpy.mock.calls[0][0]).toContain(
        "titleField=resource_link_title",
      );

      makeJsonRequestSpy.mockClear();
      service.getInlineRenderLink("1", vi.fn(), "   ");
      expect(makeJsonRequestSpy.mock.calls[0][0]).toBe(
        `${RENDER_LINK_PREVIEW}/1/default`,
      );
    });

    it("forwards success data and error message", () => {
      const cb = vi.fn();
      service.getInlineRenderLink("7", cb, "pagetitle");
      makeJsonRequestSpy.last.callback("success", {
        data: { InlineRenderLink: { title: "T", url: "/x" } },
      });
      expect(cb).toHaveBeenCalledWith(true, {
        InlineRenderLink: { title: "T", url: "/x" },
      });

      cb.mockClear();
      service.getInlineRenderLink("7", cb);
      makeJsonRequestSpy.last.callback("error", { request: {} });
      expect(cb).toHaveBeenCalledWith(false, "default error");
    });
  },
);

describe("TinyMCE plugin source contract (inlineLinkTitleField)", () => {
  for (const plugin of TINYMCE_PLUGINS) {
    it(`${plugin.name} registers inlineLinkTitleField with empty default`, () => {
      const src = readFileSync(plugin.path, "utf8");
      expect(src).toContain('inlineLinkTitleField"');
      expect(src).toMatch(
        /options\.register\s*\(\s*["']inlineLinkTitleField["']/,
      );
      // Empty default = product type defaults at resolve time
      expect(src).toMatch(/default:\s*["']["']/);
    });
  }

  it("percadvlink and percadvimage define getInlineLinkTitleField and pass it to getInlineRenderLink", () => {
    for (const name of ["percadvlink", "percadvimage"]) {
      const plugin = TINYMCE_PLUGINS.find((p) => p.name === name);
      const src = readFileSync(plugin.path, "utf8");
      expect(src).toContain("function getInlineLinkTitleField");
      expect(src).toMatch(
        /options\.get\s*\(\s*["']inlineLinkTitleField["']\s*\)/,
      );
      expect(src).toContain("getInlineLinkTitleField()");
      // Pass-through to path service (3rd arg)
      expect(src).toMatch(/getInlineRenderLink\s*\(/);
    }
  });

  it("sys_Templates.xsl wires InlineLinkTitleField into TinyMCE init", () => {
    const xsl = readFileSync(
      resolve(
        REPO_ROOT,
        "system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl",
      ),
      "utf8",
    );
    expect(xsl).toContain('name="InlineLinkTitleField"');
    expect(xsl).toContain("inlineLinkTitleField");
  });
});
