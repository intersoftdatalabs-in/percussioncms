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

/**
 * Contract tests for the batch visibility-identity client (#6783 / parent
 * #4530). Locks the envelope wrap/unwrap, the evaluator mapping (incl.
 * string-form numeric ids staying permissive-safe), and the named
 * transport failure the shell surfaces without hiding menus.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  getItemIdentityBatch,
  toVisibilityCommunityId,
  toVisibilityIdentityFields,
  unwrapItemIdentityList,
  wrapItemIdentityBatchRequest,
} from "../../../../main/ts/api/contentExplorer/identityApi";
import { isMenuVisibleByVisibilityContexts } from "../../../../main/ts/contentExplorer/menuVisibility";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("wrapItemIdentityBatchRequest", () => {
  it("trims, drops blanks, and de-duplicates", () => {
    expect(wrapItemIdentityBatchRequest([" 42 ", "42", " ", null, undefined])).toEqual({
      ItemIdentityBatchRequest: { ids: ["42"] },
    });
  });

  it("throws when no usable id remains", () => {
    expect(() => wrapItemIdentityBatchRequest([" ", null])).toThrow(
      /at least one id/,
    );
  });
});

describe("unwrapItemIdentityList", () => {
  it("accepts a bare array and drops rows without ids", () => {
    expect(
      unwrapItemIdentityList([
        { id: "42", checkoutStatus: "Checked Out" },
        { checkoutStatus: "Checked Out" },
      ]),
    ).toEqual([{ id: "42", checkoutStatus: "Checked Out" }]);
  });

  it("accepts envelope and single-object forms", () => {
    expect(
      unwrapItemIdentityList({
        ItemIdentity: [{ id: "42", workflowId: 5 }],
      }),
    ).toEqual([{ id: "42", workflowId: 5 }]);
    expect(
      unwrapItemIdentityList({ ItemIdentity: { id: "7", workflowId: 5 } }),
    ).toEqual([{ id: "7", workflowId: 5 }]);
    expect(unwrapItemIdentityList(null)).toEqual([]);
  });
});

describe("getItemIdentityBatch", () => {
  function jsonResponse(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("POSTs the wrapped ids to the identity batch endpoint", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      jsonResponse(200, [{ id: "42", checkoutStatus: "Checked Out By Me" }]),
    );
    const out = await getItemIdentityBatch(["42"]);
    expect(out).toEqual([{ id: "42", checkoutStatus: "Checked Out By Me" }]);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "explorer/identity/batch",
    );
    const init = global.fetch.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(init.body ?? "")).toContain("ItemIdentityBatchRequest");
  });

  it("names HTTP failures without inventing identity", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      jsonResponse(500, { message: "down" }),
    );
    await expect(getItemIdentityBatch(["42"])).rejects.toThrow(/down/);
  });

  it("falls back to the named error when the transport gives nothing", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(null, { status: 500 }),
    );
    await expect(getItemIdentityBatch(["42"])).rejects.toThrow(
      /Could not load visibility details \(HTTP 500\)/,
    );
  });
});

describe("toVisibilityIdentityFields", () => {
  it("maps known facts and leaves unknown ones undefined (permissive)", () => {
    expect(
      toVisibilityIdentityFields({
        id: "42",
        checkoutStatus: "Checked Out",
        contentTypeId: 123,
        objectType: "2",
        workflowId: "5",
        communityId: 1001,
        assignmentType: null,
        publishableType: null,
      }),
    ).toEqual({
      checkoutStatus: "Checked Out",
      contentTypeId: 123,
      objectType: 2,
      workflowId: 5,
    });
    expect(toVisibilityIdentityFields(null)).toEqual({});
    expect(
      toVisibilityIdentityFields({ id: "7", workflowId: 0, communityId: -1 }),
    ).toEqual({});
  });

  it("populates the evaluator so hide-rules fire with no evaluator change", () => {
    const fields = toVisibilityIdentityFields({
      id: "42",
      checkoutStatus: "Checked Out",
      contentTypeId: "123",
      objectType: "2",
      workflowId: "5",
    });
    expect(
      isMenuVisibleByVisibilityContexts(
        {
          name: "Edit",
          label: "Edit",
          sortRank: 1,
          menuType: "MENUITEM",
          visibilityContexts: [{ name: "6", value: "Checked Out" } as never],
        },
        fields,
      ),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(
        {
          name: "Type",
          label: "Type",
          sortRank: 1,
          menuType: "MENUITEM",
          visibilityContexts: [{ name: "3", value: "123" } as never],
        },
        fields,
      ),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(
        {
          name: "Flow",
          label: "Flow",
          sortRank: 1,
          menuType: "MENUITEM",
          visibilityContexts: [{ name: "9", value: "5" } as never],
        },
        fields,
      ),
    ).toBe(false);
  });
});

describe("toVisibilityCommunityId", () => {
  it("preserves raw id text for the strict-equality community rule", () => {
    expect(toVisibilityCommunityId({ id: "42", communityId: 1001 })).toBe(1001);
    expect(toVisibilityCommunityId({ id: "42", communityId: "1001" })).toBe("1001");
    expect(toVisibilityCommunityId({ id: "42", communityId: null })).toBeUndefined();
    expect(toVisibilityCommunityId({ id: "42", communityId: -1 })).toBeUndefined();
    expect(toVisibilityCommunityId(null)).toBeUndefined();
  });
});
