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

import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchPreviewLocation } from "../../../../main/ts/api/contentExplorer/assemblyApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("fetchPreviewLocation", () => {
  it("unwraps PreviewLocation envelope and returns previewUrl", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          PreviewLocation: {
            previewUrl: "/assembler/render?sys_template=7",
            contentId: 42,
            templateId: 7,
            revision: 1,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const loc = await fetchPreviewLocation(42, 7, 1);
    expect(loc.previewUrl).toContain("/assembler/render");
    expect(loc.templateId).toBe(7);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "assembly/preview-location",
    );
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "templateId=7",
    );
  });
});
