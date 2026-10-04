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
  storedItemFilterLabel,
} from "@/publishing/contentListItemFilter";

describe("content list item filter choices", () => {
  it("uses the numeric uuid, then a digit string, then the last guid segment", () => {
    expect(itemFilterChoiceId({ name: "public", filterId: { uuid: 42 } })).toBe("42");
    expect(
      itemFilterChoiceId({ name: "preview", filterId: { stringValue: "77" } }),
    ).toBe("77");
    expect(
      itemFilterChoiceId({
        name: "staging",
        filterId: { stringValue: "0-7-88" },
      }),
    ).toBe("88");
    expect(itemFilterChoiceId({ name: "x" })).toBeUndefined();
    expect(itemFilterChoiceId({ name: "x", filterId: { uuid: 0 } })).toBeUndefined();
  });

  it("drops nameless or id-less rows and sorts by name", () => {
    expect(
      itemFilterChoices([
        { name: "preview", filterId: { uuid: 2 } },
        { name: "  ", filterId: { uuid: 3 } },
        { name: "public", filterId: { uuid: 1 } },
        { name: "again", filterId: {} },
      ]),
    ).toEqual([
      { id: "2", name: "preview" },
      { id: "1", name: "public" },
    ]);
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
