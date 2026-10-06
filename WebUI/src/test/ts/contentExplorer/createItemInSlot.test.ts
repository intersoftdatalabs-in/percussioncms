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

import { describe, expect, it, vi } from "vitest";
import type { PSExplorerRelationshipEdge } from "../../../main/ts/api/contentExplorer/relationship";
import {
  applyCreatedItemToSlot,
  gateCreateItemInSlot,
  runCreateItemInSlot,
} from "../../../main/ts/contentExplorer/createItemInSlot";

const assembly: PSExplorerRelationshipEdge = {
  relationshipId: 71,
  configName: "ActiveAssembly",
  category: "rs_activeassembly",
  dependentId: 4,
  label: "AA first",
  slotId: 5,
  sortRank: 0,
  templateId: 4,
  templateName: "Brief",
};

const base = {
  contentType: "percPage",
  folderPath: "/Sites/Enterprise",
  templateId: 4,
  allowedTemplateIds: [4, 8],
  allowedContentTypes: ["percPage"],
  knownSlotIds: [5],
  ownerId: 42,
  selection: { kind: "slot" as const, slotId: 5 },
};

describe("createItemInSlot", () => {
  it("refuses a non-slot and a missing type, folder, or template", () => {
    expect(gateCreateItemInSlot({ ...base, selection: null })).toEqual({
      ok: false,
      reason: "not_slot",
    });
    expect(
      gateCreateItemInSlot({ ...base, selection: { kind: "other" } }),
    ).toEqual({ ok: false, reason: "not_slot" });
    expect(
      gateCreateItemInSlot({ ...base, selection: { kind: "slot", slotId: 9 } }),
    ).toEqual({ ok: false, reason: "not_slot" });
    expect(gateCreateItemInSlot({ ...base, contentType: "  " })).toEqual({
      ok: false,
      reason: "no_type",
    });
    expect(gateCreateItemInSlot({ ...base, contentType: "percImage" })).toEqual({
      ok: false,
      reason: "type_not_allowed",
    });
    expect(gateCreateItemInSlot({ ...base, folderPath: " " })).toEqual({
      ok: false,
      reason: "no_folder",
    });
    expect(gateCreateItemInSlot({ ...base, templateId: 0 })).toEqual({
      ok: false,
      reason: "no_template",
    });
    expect(gateCreateItemInSlot({ ...base, templateId: 3 })).toEqual({
      ok: false,
      reason: "not_allowed",
    });
  });

  it("accepts one allowed type, folder, and snippet template", () => {
    expect(gateCreateItemInSlot(base)).toEqual({
      ok: true,
      ownerId: 42,
      slotId: 5,
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      templateId: 4,
    });
  });

  it("does not link when create fails or returns no id", async () => {
    const link = vi.fn();
    const failed = await runCreateItemInSlot({
      ownerId: 42,
      slotId: 5,
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      templateId: 4,
      templateName: "Brief",
      create: async () => {
        throw Object.assign(new Error("no"), { status: 400 });
      },
      link,
    });
    expect(failed).toMatchObject({ ok: false, phase: "create" });
    expect(link).not.toHaveBeenCalled();

    const empty = await runCreateItemInSlot({
      ownerId: 42,
      slotId: 5,
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      templateId: 4,
      templateName: "Brief",
      create: async () => ({ itemId: "", name: "" }),
      link,
    });
    expect(empty).toEqual({ ok: false, phase: "create" });
    expect(link).not.toHaveBeenCalled();
  });

  it("does not succeed when the link fails or returns another slot", async () => {
    const create = vi.fn().mockResolvedValue({
      itemId: "1-101-99",
      name: "New page",
    });
    const rejected = await runCreateItemInSlot({
      ownerId: 42,
      slotId: 5,
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      templateId: 4,
      templateName: "Brief",
      create,
      link: async () => {
        throw Object.assign(new Error("no"), { status: 409 });
      },
    });
    expect(rejected).toMatchObject({ ok: false, phase: "link" });
    expect(create).toHaveBeenCalledTimes(1);

    const mismatch = await runCreateItemInSlot({
      ownerId: 42,
      slotId: 5,
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      templateId: 4,
      templateName: "Brief",
      create,
      link: async () => ({
        relationshipId: 91,
        ownerId: 42,
        dependentId: 99,
        slotId: 8,
        templateId: 4,
        sortRank: 0,
      }),
    });
    expect(mismatch).toEqual({ ok: false, phase: "link" });
  });

  it("returns the new content id only after the link matches the slot", async () => {
    const result = await runCreateItemInSlot({
      ownerId: 42,
      slotId: 5,
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      templateId: 4,
      templateName: "Brief",
      create: async () => ({ itemId: "1-101-99", name: "New page" }),
      link: async (request) => ({
        relationshipId: 91,
        ownerId: request.ownerId,
        dependentId: request.dependentId,
        slotId: request.slotId,
        templateId: request.templateId,
        sortRank: 2,
      }),
    });
    expect(result).toMatchObject({
      ok: true,
      contentId: 99,
      label: "New page",
      templateName: "Brief",
    });
    if (!result.ok) {
      return;
    }
    const next = applyCreatedItemToSlot(
      [assembly],
      result.linked,
      result.templateName,
      result.label,
    );
    expect(next.map((edge) => edge.relationshipId)).toEqual([71, 91]);
    expect(next[1]?.label).toBe("New page");
    expect(next[1]?.slotId).toBe(5);
    expect(next[1]?.dependentId).toBe(99);
    expect(
      applyCreatedItemToSlot(
        [assembly],
        { ...result.linked, relationshipId: 0 },
        "Brief",
        "New page",
      ),
    ).toEqual([assembly]);
  });
});
