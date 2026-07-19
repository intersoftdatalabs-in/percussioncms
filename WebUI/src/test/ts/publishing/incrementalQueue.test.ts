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
  extractQueueItems,
  hasMorePages,
  isQueueEmpty,
} from "@/publishing/incrementalQueue";

describe("incrementalQueue", () => {
  it("treats missing page as empty", () => {
    expect(isQueueEmpty(null)).toBe(true);
    expect(extractQueueItems(undefined)).toEqual([]);
  });

  it("extracts items array", () => {
    const page = { items: [{ id: 1 }, { id: 2 }], pageSize: 10 };
    expect(extractQueueItems(page)).toHaveLength(2);
    expect(isQueueEmpty(page)).toBe(false);
  });

  it("extracts PagedItemList.childrenInPage (product queue shape)", () => {
    const page = {
      PagedItemList: {
        childrenInPage: [{ id: 9 }, { id: 10 }],
        childrenCount: 2,
      },
    };
    expect(extractQueueItems(page)).toHaveLength(2);
    expect(isQueueEmpty(page)).toBe(false);
  });

  it("uses totalCount for paging", () => {
    const page = { items: [{ a: 1 }], totalCount: 5 };
    expect(hasMorePages(page, 1, 1)).toBe(true);
    expect(hasMorePages({ items: [{ a: 1 }], totalCount: 1 }, 1, 1)).toBe(
      false,
    );
  });

  it("assumes more when full page without totalCount", () => {
    const items = Array.from({ length: 10 }, (_, i) => ({ i }));
    expect(hasMorePages({ items }, 1, 10)).toBe(true);
    expect(hasMorePages({ items: [{ i: 1 }] }, 1, 10)).toBe(false);
  });
});
