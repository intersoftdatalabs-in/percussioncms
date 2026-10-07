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

import { beforeEach, describe, expect, it, vi } from "vitest";
import { get, post, put } from "../../../../main/ts/api/client";
import {
  createKeyword,
  getKeyword,
  listKeywords,
  updateKeyword,
  wrapKeywordForWire,
} from "../../../../main/ts/api/developer/keywordsApi";
import { PATHS } from "../../../../main/ts/api/paths";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

const getMock = get as ReturnType<typeof vi.fn>;
const postMock = post as ReturnType<typeof vi.fn>;
const putMock = put as ReturnType<typeof vi.fn>;

const flat = {
  label: "Priority",
  description: "Item priority",
  sequence: 4,
  choices: [{ label: "High", value: "high", sequence: 1 }],
};

describe("keywordsApi wire root", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
    putMock.mockReset();
  });

  it("wraps create and update under Keyword and unwraps the response", async () => {
    expect(wrapKeywordForWire(flat)).toEqual({ Keyword: flat });
    postMock.mockResolvedValue({ Keyword: { ...flat, value: "priority" } });
    putMock.mockResolvedValue({ Keyword: flat });
    getMock.mockResolvedValue(flat);

    const created = await createKeyword(flat);
    expect(postMock).toHaveBeenCalledWith(PATHS.KEYWORDS, { Keyword: flat });
    expect(created.label).toBe("Priority");
    expect(created.value).toBe("priority");

    const updated = await updateKeyword("42", flat);
    expect(putMock).toHaveBeenCalledWith(`${PATHS.KEYWORDS}/42`, { Keyword: flat });
    expect(updated.choices).toEqual(flat.choices);

    const loaded = await getKeyword("42");
    expect(getMock).toHaveBeenCalledWith(`${PATHS.KEYWORDS}/42`);
    expect(loaded.label).toBe("Priority");
  });

  it("turns a single JAXB choice object into a list", async () => {
    getMock.mockResolvedValueOnce([
      {
        label: "Priority",
        choices: { label: "High", value: "high", sequence: 1 },
      },
    ]);
    const rows = await listKeywords(true);
    expect(rows[0].choices).toEqual([{ label: "High", value: "high", sequence: 1 }]);

    getMock.mockResolvedValueOnce({
      label: "Priority",
      choices: { KeywordChoice: [{ label: "Low", value: "low", sequence: 2 }] },
    });
    const one = await getKeyword("7");
    expect(one.choices).toEqual([{ label: "Low", value: "low", sequence: 2 }]);
  });
});
