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

import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SiteRootBrowser } from "@/publishing/design/SiteRootBrowser";

vi.mock("@/api/home/homeApi", () => ({
  fetchFolderChildren: vi.fn().mockResolvedValue([
    { name: "Home", path: "//Sites/Demo/Home", folder: true },
  ]),
}));

describe("SiteRootBrowser", () => {
  it("renders and shows path", async () => {
    render(<SiteRootBrowser rootPath="//Sites/Demo" />);
    expect(screen.getByTestId("site-root-browser")).toBeTruthy();
    expect(screen.getByText("//Sites/Demo")).toBeTruthy();
  });
});
