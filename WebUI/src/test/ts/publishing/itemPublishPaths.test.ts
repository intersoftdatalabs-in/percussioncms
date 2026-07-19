/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import {
  itemPublishPaths,
  publishingShellHref,
} from "@/publishing/itemPublishPaths";

describe("itemPublishPaths", () => {
  it("documents sitemanage publish item endpoints", () => {
    const p = itemPublishPaths("/services");
    expect(p.pagePublish).toBe("/services/sitemanage/publish/page");
    expect(p.resourcePublish).toContain("/publish/resource");
    expect(p.pageTakedown).toContain("/takedown/page");
    expect(p.publishingActions).toContain("publishingActions");
  });
});

describe("publishingShellHref", () => {
  it("maps status and logs sections for history deep links", () => {
    expect(publishingShellHref({ section: "status" })).toContain(
      "view=publish",
    );
    expect(publishingShellHref({ section: "status" })).toContain(
      "section=status",
    );
    expect(
      publishingShellHref({ section: "logs", siteId: "9" }),
    ).toContain("siteId=9");
  });
});
