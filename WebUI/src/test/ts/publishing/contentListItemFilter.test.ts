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
  itemFilterChoiceId,
  itemFilterChoices,
  NO_ITEM_FILTER_LABEL,
  storedFilterToken,
  storedItemFilterLabel,
} from "@/publishing/contentListItemFilter";

describe("content list item filter choices", () => {
  it("addresses a filter by name, not by a colliding uuid", () => {
    expect(itemFilterChoiceId({ name: " public ", filterId: { uuid: 1 } })).toBe("public");
    expect(
      itemFilterChoiceId({
        name: "preview",
        filterId: { uuid: 1, stringValue: "6373757-7-1" },
      }),
    ).toBe("preview");
    expect(itemFilterChoiceId({ name: "  ", filterId: { uuid: 42 } })).toBeUndefined();
    expect(itemFilterChoiceId({ filterId: { uuid: 42 } })).toBeUndefined();
  });

  it("keeps filters that share a uuid and drops blank names", () => {
    expect(
      itemFilterChoices([
        { name: "preview", filterId: { uuid: 1 } },
        { name: "  ", filterId: { uuid: 3 } },
        { name: "public", filterId: { uuid: 1 } },
        { name: "again", filterId: {} },
      ]),
    ).toEqual([
      { id: "again", name: "again" },
      { id: "preview", name: "preview" },
      { id: "public", name: "public" },
    ]);
  });

  it("prefers the loaded name as the select token", () => {
    expect(storedFilterToken({ itemFilterName: " public ", itemFilterId: "1" })).toBe(
      "public",
    );
    expect(storedFilterToken({ itemFilterId: "9" })).toBe("9");
    expect(storedFilterToken(null)).toBe("");
  });

  it("shows the loaded name, else the id, else no filter", () => {
    expect(storedItemFilterLabel({ itemFilterName: " public ", itemFilterId: "1" })).toBe(
      "public",
    );
    expect(storedItemFilterLabel({ itemFilterId: "9" })).toBe("9");
    expect(storedItemFilterLabel(null)).toBe(NO_ITEM_FILTER_LABEL);
    expect(storedItemFilterLabel({ itemFilterName: "  ", itemFilterId: "" })).toBe(
      NO_ITEM_FILTER_LABEL,
    );
  });
});
