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
import type { PSExplorerRelationshipEdge } from "../../../main/ts/api/contentExplorer/relationship";
import {
  applyRelationshipSnippetTemplate,
  canChangeRelationshipSnippetTemplate,
  gateRelationshipSnippetTemplate,
  relationshipTemplateLabel,
} from "../../../main/ts/contentExplorer/changeRelationshipTemplate";

const assembly: PSExplorerRelationshipEdge = {
  relationshipId: 71,
  configName: "ActiveAssembly",
  category: "rs_activeassembly",
  dependentId: 4,
  label: "AA first",
  slotId: 5,
  templateId: 4,
  templateName: "Brief",
};

const folder: PSExplorerRelationshipEdge = {
  relationshipId: 82,
  configName: "Folder",
  category: "rs_folder",
  dependentId: 3,
  label: "Folder row",
  slotId: 5,
  templateId: 4,
  templateName: "Brief",
};

describe("changeRelationshipSnippetTemplate", () => {
  it("allows one slotted Active Assembly row and refuses a folder", () => {
    expect(canChangeRelationshipSnippetTemplate(assembly)).toBe(true);
    expect(canChangeRelationshipSnippetTemplate(folder)).toBe(false);
    expect(
      canChangeRelationshipSnippetTemplate({
        ...assembly,
        category: "rs_translation",
        configName: "Translation",
      }),
    ).toBe(false);
    expect(canChangeRelationshipSnippetTemplate({ ...assembly, slotId: 0 })).toBe(
      false,
    );
  });

  it("does not post an empty choice, the current template, or a folder row", () => {
    expect(
      gateRelationshipSnippetTemplate({
        edge: null,
        templateId: 8,
        allowedIds: [8],
      }),
    ).toEqual({ ok: false, reason: "empty" });
    expect(
      gateRelationshipSnippetTemplate({
        edge: assembly,
        templateId: 0,
        allowedIds: [4, 8],
      }),
    ).toEqual({ ok: false, reason: "empty" });
    expect(
      gateRelationshipSnippetTemplate({
        edge: assembly,
        templateId: 4,
        allowedIds: [4, 8],
      }),
    ).toEqual({ ok: false, reason: "same" });
    expect(
      gateRelationshipSnippetTemplate({
        edge: folder,
        templateId: 8,
        allowedIds: [8],
      }),
    ).toEqual({ ok: false, reason: "folder" });
    expect(
      gateRelationshipSnippetTemplate({
        edge: assembly,
        templateId: 9,
        allowedIds: [8],
      }),
    ).toEqual({ ok: false, reason: "not_allowed" });
  });

  it("posts the current slot and a different allowed template", () => {
    expect(
      gateRelationshipSnippetTemplate({
        edge: assembly,
        templateId: 8,
        allowedIds: [4, 8],
      }),
    ).toEqual({ ok: true, relationshipId: 71, slotId: 5, templateId: 8 });
  });

  it("shows the new template on that row only and keeps the slot", () => {
    const sibling: PSExplorerRelationshipEdge = {
      ...assembly,
      relationshipId: 72,
      label: "AA last",
      templateId: 4,
      templateName: "Brief",
    };
    const next = applyRelationshipSnippetTemplate(
      [assembly, sibling],
      71,
      { relationshipId: 91, slotId: 9, templateId: 8 },
      "Full story",
    );
    expect(next[0]).toMatchObject({
      relationshipId: 91,
      slotId: 5,
      templateId: 8,
      templateName: "Full story",
    });
    expect(next[1]).toEqual(sibling);
    expect(relationshipTemplateLabel(next[0]!)).toBe("Full story");
    expect(relationshipTemplateLabel(assembly)).toBe("Brief");
    expect(relationshipTemplateLabel({ templateId: 4, templateName: "" })).toBe(
      "4",
    );
    expect(relationshipTemplateLabel({ templateId: 0 })).toBe("");
  });
});
