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
import {
  isAllowedPageTemplate,
  pageChangeTemplatePath,
  pageTemplateIdFromFields,
  resolvePageTemplateSelection,
  withPageTemplateField,
} from "../../../main/ts/editor/editorPageTemplate";

const CHOICES = [
  { id: "16777215-101-1", name: "Base" },
  { id: "16777215-101-2", name: "Plain" },
];

describe("editorPageTemplate", () => {
  it("reads templateid from item fields", () => {
    expect(
      pageTemplateIdFromFields([
        { name: "sys_title", value: "Home" },
        { name: "templateid", value: " 16777215-101-1 " },
      ]),
    ).toBe("16777215-101-1");
    expect(pageTemplateIdFromFields([])).toBe("");
  });

  it("keeps the current choice and rejects ids that are not allowed", () => {
    expect(resolvePageTemplateSelection("16777215-101-2", CHOICES)).toBe(
      "16777215-101-2",
    );
    expect(resolvePageTemplateSelection("Plain", CHOICES)).toBe("16777215-101-2");
    expect(resolvePageTemplateSelection("missing", CHOICES)).toBe("16777215-101-1");
    expect(resolvePageTemplateSelection("", [])).toBe("");
    expect(isAllowedPageTemplate("16777215-101-2", CHOICES)).toBe(true);
    expect(isAllowedPageTemplate("nope", CHOICES)).toBe(false);
  });

  it("writes templateid onto the field payload used by item save", () => {
    const updated = withPageTemplateField(
      [
        { name: "sys_title", value: "Home" },
        { name: "templateid", value: "16777215-101-1" },
      ],
      "16777215-101-2",
    );
    expect(updated).toEqual([
      { name: "sys_title", value: "Home" },
      { name: "templateid", value: "16777215-101-2" },
    ]);
    expect(
      withPageTemplateField([{ name: "sys_title", value: "Home" }], "16777215-101-2"),
    ).toEqual([
      { name: "sys_title", value: "Home" },
      { name: "templateid", value: "16777215-101-2" },
    ]);
  });

  it("targets the existing page changeTemplate path", () => {
    const path = pageChangeTemplatePath("42", "16777215-101-2");
    expect(path).toContain("/pagemanagement/page/changeTemplate/42/16777215-101-2");
    expect(path.startsWith("/")).toBe(true);
  });
});
