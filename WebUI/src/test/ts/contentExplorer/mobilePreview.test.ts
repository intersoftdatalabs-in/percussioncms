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

import { describe, expect, it } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  isMobilePreviewPage,
  openMobilePreview,
  resolveMobilePreviewUrl,
} from "../../../main/ts/contentExplorer/mobilePreview";
import {
  resolvePreviewTarget,
} from "../../../main/ts/contentExplorer/previewItem";

const SERVICES = "/services";

const PAGE: PSPathItem = {
  id: "16777215-101-551",
  name: "Corporate Investments Home",
  path: "/Sites/Corporate Investments/Corporate Investments Home",
  type: "rffHome",
  category: "page",
  accessLevel: "READ",
};

const PATH_ONLY_PAGE: PSPathItem = {
  name: "About",
  path: "/Sites/Demo/About",
  type: "percPage",
  category: "page",
  accessLevel: "READ",
};

const ASSET: PSPathItem = {
  id: "16777216-101-9",
  name: "logo.png",
  path: "/Assets/uploads/logo.png",
  type: "percImageAsset",
  category: "asset",
  accessLevel: "READ",
};

const FOLDER: PSPathItem = {
  id: "f1",
  name: "Pages",
  path: "/Sites/Corporate Investments/Pages/",
  type: "folder",
  accessLevel: "READ",
};

const TEMPLATE: PSPathItem = {
  id: "16777215-101-7",
  name: "rffPgGeneric",
  path: "/Design/templates/rffPgGeneric",
  type: "template",
  category: "template",
  accessLevel: "READ",
};

function recordingOpen(): {
  open: (url: string, target?: string) => Window;
  calls: { url: string; target?: string }[];
} {
  const calls: { url: string; target?: string }[] = [];
  return {
    calls,
    open: (url, target) => {
      calls.push({ url, target });
      return {} as Window;
    },
  };
}

describe("mobile preview of the selected page (#5078)", () => {
  it("opens page render with percmobilepreview=true and leaves desktop preview alone", () => {
    const desktop = resolvePreviewTarget(PAGE, SERVICES);
    expect(desktop.kind).toBe("page");
    expect(desktop.url).toBe(
      "/services/pagemanagement/render/page/16777215-101-551",
    );
    expect(desktop.url).not.toContain("percmobilepreview=true");

    expect(isMobilePreviewPage(PAGE)).toBe(true);
    expect(resolveMobilePreviewUrl(PAGE, SERVICES)).toBe(
      "/services/pagemanagement/render/page/16777215-101-551?percmobilepreview=true",
    );

    const recorder = recordingOpen();
    const result = openMobilePreview({
      item: PAGE,
      servicesRoot: SERVICES,
      openWindow: recorder.open,
    });
    expect(result).toEqual({
      status: "opened",
      url: "/services/pagemanagement/render/page/16777215-101-551?percmobilepreview=true",
    });
    expect(recorder.calls).toEqual([
      {
        url: "/services/pagemanagement/render/page/16777215-101-551?percmobilepreview=true",
        target: "percMobilePagePreview_16777215_101_551",
      },
    ]);
    expect(PAGE.name).toBe("Corporate Investments Home");
  });

  it("opens a path-only Sites page with percmobilepreview=true", () => {
    const desktop = resolvePreviewTarget(PATH_ONLY_PAGE, SERVICES);
    expect(desktop.url).toContain("percmobilepreview=false");
    expect(desktop.url).not.toContain("percmobilepreview=true");

    const recorder = recordingOpen();
    const result = openMobilePreview({
      item: PATH_ONLY_PAGE,
      servicesRoot: SERVICES,
      openWindow: recorder.open,
    });
    expect(result.status).toBe("opened");
    if (result.status !== "opened") {
      return;
    }
    expect(result.url).toBe(
      "/Sites/Demo/About?percmobilepreview=true",
    );
    expect(recorder.calls[0]?.target).toBe("percMobilePagePreview_SitesDemoAbout");
  });

  it("encodes spaces in a path-only page and still forces mobile true", () => {
    const item: PSPathItem = {
      name: "Q?A",
      path: "/Sites/Corporate Investments/Q?A",
      type: "page",
      accessLevel: "READ",
    };
    expect(resolveMobilePreviewUrl(item, SERVICES)).toBe(
      "/Sites/Corporate%20Investments/Q%3FA?percmobilepreview=true",
    );
  });

  it("does not open a window for an empty selection", () => {
    const recorder = recordingOpen();
    expect(
      openMobilePreview({
        item: null,
        servicesRoot: SERVICES,
        openWindow: recorder.open,
      }),
    ).toEqual({ status: "none" });
    expect(recorder.calls).toEqual([]);
  });

  it("names a folder and does not open a window", () => {
    const recorder = recordingOpen();
    expect(
      openMobilePreview({
        item: FOLDER,
        servicesRoot: SERVICES,
        openWindow: recorder.open,
      }),
    ).toEqual({ status: "folder", name: "Pages" });
    expect(recorder.calls).toEqual([]);
    expect(isMobilePreviewPage(FOLDER)).toBe(false);
  });

  it("names an asset and does not open a window", () => {
    const recorder = recordingOpen();
    expect(
      openMobilePreview({
        item: ASSET,
        servicesRoot: SERVICES,
        openWindow: recorder.open,
      }),
    ).toEqual({ status: "not-page", name: "logo.png" });
    expect(recorder.calls).toEqual([]);
    expect(resolvePreviewTarget(ASSET, SERVICES).kind).toBe("asset");
  });

  it("does not treat a template as a page", () => {
    const recorder = recordingOpen();
    expect(isMobilePreviewPage(TEMPLATE)).toBe(false);
    expect(
      openMobilePreview({
        item: TEMPLATE,
        servicesRoot: SERVICES,
        openWindow: recorder.open,
      }),
    ).toEqual({ status: "not-page", name: "rffPgGeneric" });
    expect(recorder.calls).toEqual([]);
  });

  it("does not claim success when the window is blocked or the opener throws", () => {
    const blocked = openMobilePreview({
      item: PAGE,
      servicesRoot: SERVICES,
      openWindow: () => null,
    });
    expect(blocked.status).toBe("blocked");
    if (blocked.status === "blocked") {
      expect(blocked.url).toContain("percmobilepreview=true");
    }

    const thrown = openMobilePreview({
      item: PAGE,
      servicesRoot: SERVICES,
      openWindow: () => {
        throw new Error("popup");
      },
    });
    expect(thrown.status).toBe("blocked");
  });
});
