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
  committedDisplayTitleAfterAttempt,
  displayTitleDraftAfterFailure,
  itemPropertiesPathAfterSave,
  planItemPropertiesSave,
} from "../../../main/ts/contentExplorer/setItemDisplayTitle";

describe("set item display title (#5246)", () => {
  it("posts the loaded name when only the display title changes", () => {
    const plan = planItemPropertiesSave({
      itemPath: "/Assets/item",
      loadedName: "qa-item",
      draftName: "qa-item",
      draftDisplayTitle: "Shown after save",
    });
    expect(plan).toEqual({
      ok: true,
      itemPath: "/Assets/item",
      name: "qa-item",
      displayTitle: "Shown after save",
      nameChanged: false,
    });
  });

  it("still posts a renamed item name", () => {
    const plan = planItemPropertiesSave({
      itemPath: "/Assets/item",
      loadedName: "old-name",
      draftName: " new-name ",
      draftDisplayTitle: "title",
    });
    expect(plan.ok).toBe(true);
    if (!plan.ok) {
      return;
    }
    expect(plan.name).toBe("new-name");
    expect(plan.nameChanged).toBe(true);
    expect(
      itemPropertiesPathAfterSave("/Assets/folder/old-name", plan.name, true),
    ).toBe("/Assets/folder/new-name");
  });

  it("reloads the same path when the name did not change", () => {
    expect(
      itemPropertiesPathAfterSave("/Assets/folder/qa-item", "qa-item", false),
    ).toBe("/Assets/folder/qa-item");
  });

  it("rejects a blank name before any write", () => {
    expect(
      planItemPropertiesSave({
        itemPath: "/Assets/item",
        loadedName: "qa-item",
        draftName: "  ",
        draftDisplayTitle: "title",
      }),
    ).toEqual({ ok: false, reason: "blank-name" });
  });

  it("shows the reloaded title only after a successful save", () => {
    expect(
      committedDisplayTitleAfterAttempt("Old title", {
        outcome: "saved",
        reloadedTitle: "From server",
      }),
    ).toBe("From server");
  });

  it("keeps the previous title on cancel, HTTP 400/403/409, and a failed reload", () => {
    const previous = "Old title";
    expect(
      committedDisplayTitleAfterAttempt(previous, { outcome: "cancelled" }),
    ).toBe(previous);
    expect(
      committedDisplayTitleAfterAttempt(previous, {
        outcome: "http",
        http: 400,
      }),
    ).toBe(previous);
    expect(
      committedDisplayTitleAfterAttempt(previous, {
        outcome: "http",
        http: 403,
      }),
    ).toBe(previous);
    expect(
      committedDisplayTitleAfterAttempt(previous, {
        outcome: "http",
        http: 409,
      }),
    ).toBe(previous);
    expect(
      committedDisplayTitleAfterAttempt(previous, { outcome: "reload-failed" }),
    ).toBe(previous);
    expect(displayTitleDraftAfterFailure(previous, "Typed", 400)).toBe(previous);
    expect(displayTitleDraftAfterFailure(previous, "Typed", 403)).toBe(previous);
    expect(displayTitleDraftAfterFailure(previous, "Typed", 409)).toBe(previous);
    expect(displayTitleDraftAfterFailure(previous, "Typed", 500)).toBe("Typed");
  });
});
