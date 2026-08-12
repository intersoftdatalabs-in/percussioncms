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
  countNavTreeNodes,
  flattenNavTree,
  isEmptySectionTreeWire,
  mapSectionNodeToTree,
  normalizeChildNodes,
  parseSectionNodePayload,
  sectionTypeLabel,
} from "../../../../main/ts/api/architecture/mapSectionTree";
import type { SectionNodeWire } from "../../../../main/ts/api/architecture/types";

describe("mapSectionTree (#3095)", () => {
  it("parses Jackson-rooted SectionNode payload", () => {
    const wire = parseSectionNodePayload({
      SectionNode: {
        id: "root-1",
        title: "Home",
        folderPath: "//Sites/Demo",
        sectionType: "section",
        childNodes: [
          {
            id: "child-1",
            title: "About",
            sectionType: "section",
            childNodes: [],
          },
        ],
      },
    });
    expect(wire).not.toBeNull();
    expect(wire!.id).toBe("root-1");
    expect(normalizeChildNodes(wire!.childNodes)).toHaveLength(1);
  });

  it("maps nested children and counts nodes", () => {
    const root = mapSectionNodeToTree({
      id: "r",
      title: "Root",
      sectionType: "section",
      childNodes: [
        {
          id: "a",
          title: "A",
          sectionType: "sectionlink",
          childNodes: [
            { id: "a1", title: "A1", sectionType: "externallink" },
          ],
        },
        { id: "b", title: "B", sectionType: "blog", requiresLogin: true },
      ],
    });
    expect(root.title).toBe("Root");
    expect(root.children).toHaveLength(2);
    expect(root.children[0].sectionType).toBe("sectionlink");
    expect(root.children[0].children[0].sectionType).toBe("externallink");
    expect(root.children[1].requiresLogin).toBe(true);
    expect(countNavTreeNodes(root)).toBe(4);
    expect(flattenNavTree(root).map((n) => n.id)).toEqual([
      "r",
      "a",
      "a1",
      "b",
    ]);
  });

  it("guards against cyclic child references", () => {
    const cyclic: SectionNodeWire = {
      id: "x",
      title: "X",
      childNodes: [],
    };
    // self-reference via shared object graph
    cyclic.childNodes = [cyclic];
    const tree = mapSectionNodeToTree(cyclic);
    expect(tree.id).toBe("x");
    // Child that is self is skipped by seen-set on childId
    expect(tree.children).toHaveLength(0);
  });

  it("normalizes single child object and empty payloads", () => {
    expect(normalizeChildNodes(null)).toEqual([]);
    expect(
      normalizeChildNodes({ id: "only", title: "Only" }),
    ).toHaveLength(1);
    expect(parseSectionNodePayload(null)).toBeNull();
    expect(parseSectionNodePayload("nope")).toBeNull();
    expect(mapSectionNodeToTree({ title: "NoId" }).title).toBe("NoId");
  });

  it("treats missing-id empty children as empty nav tree (#3218)", () => {
    expect(
      isEmptySectionTreeWire({ title: "BareSite", childNodes: [] }),
    ).toBe(true);
    expect(isEmptySectionTreeWire(null)).toBe(true);
    expect(
      isEmptySectionTreeWire({
        id: "root-1",
        title: "Home",
        childNodes: [],
      }),
    ).toBe(false);
  });

  it("sectionTypeLabel badges non-default types", () => {
    expect(sectionTypeLabel("section")).toBeNull();
    expect(sectionTypeLabel("sectionlink")).toMatch(/section link/i);
    expect(sectionTypeLabel("externallink")).toMatch(/external/i);
    expect(sectionTypeLabel("blog")).toMatch(/blog/i);
  });
});
