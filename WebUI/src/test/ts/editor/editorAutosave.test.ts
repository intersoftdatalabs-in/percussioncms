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
  AUTOSAVE_DRAFT_VERSION,
  autosavedDraftDiffers,
  clearAutosavedEditorDraft,
  editorAutosaveKey,
  readAutosavedEditorDraft,
  shouldOfferAutosaveRestore,
  writeAutosavedEditorDraft,
  type AutosaveStorage,
  type AutosavedEditorDraft,
} from "../../../main/ts/editor/editorAutosave";

function memoryStorage(seed: Record<string, string> = {}): AutosaveStorage {
  const store = new Map(Object.entries(seed));
  return {
    getItem: (key: string) => (store.has(key) ? store.get(key)! : null),
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
  };
}

const loaded = [{ name: "sys_title", value: "Home" }];

describe("editorAutosave (#6076)", () => {
  it("keys storage per content id", () => {
    expect(editorAutosaveKey("42")).toBe("perc-editor-autosave-42");
    expect(editorAutosaveKey("43")).not.toBe(editorAutosaveKey("42"));
  });

  it("round-trips the draft with revision", () => {
    const storage = memoryStorage();
    writeAutosavedEditorDraft(storage, "42", { sys_title: "Draft title" }, 3);
    const stored = readAutosavedEditorDraft(storage, "42");
    expect(stored).not.toBeNull();
    expect(stored?.version).toBe(AUTOSAVE_DRAFT_VERSION);
    expect(stored?.contentId).toBe("42");
    expect(stored?.draft).toEqual({ sys_title: "Draft title" });
    expect(stored?.revision).toBe(3);
    expect(typeof stored?.savedAt).toBe("string");
  });

  it("reads corrupt, foreign, or versioned payloads as absent", () => {
    const key = editorAutosaveKey("42");
    expect(
      readAutosavedEditorDraft(memoryStorage({ [key]: "not-json{" }), "42"),
    ).toBeNull();
    expect(
      readAutosavedEditorDraft(
        memoryStorage({
          [key]: JSON.stringify({
            version: 999,
            contentId: "42",
            savedAt: "x",
            draft: {},
          }),
        }),
        "42",
      ),
    ).toBeNull();
    expect(
      readAutosavedEditorDraft(
        memoryStorage({
          [key]: JSON.stringify({
            version: AUTOSAVE_DRAFT_VERSION,
            contentId: "43",
            savedAt: "x",
            draft: {},
          }),
        }),
        "42",
      ),
    ).toBeNull();
    expect(readAutosavedEditorDraft(null, "42")).toBeNull();
    expect(readAutosavedEditorDraft(memoryStorage(), "42")).toBeNull();
  });

  it("clears the stored entry", () => {
    const storage = memoryStorage();
    writeAutosavedEditorDraft(storage, "42", { sys_title: "Draft" });
    clearAutosavedEditorDraft(storage, "42");
    expect(readAutosavedEditorDraft(storage, "42")).toBeNull();
  });

  it("differs only when a loaded value changed", () => {
    const same: AutosavedEditorDraft = {
      version: AUTOSAVE_DRAFT_VERSION,
      contentId: "42",
      savedAt: "t",
      revision: 1,
      draft: { sys_title: "Home" },
    };
    const changed: AutosavedEditorDraft = {
      ...same,
      draft: { sys_title: "Draft title" },
    };
    expect(autosavedDraftDiffers(same, loaded)).toBe(false);
    expect(autosavedDraftDiffers(changed, loaded)).toBe(true);
    expect(autosavedDraftDiffers(null, loaded)).toBe(false);
    expect(autosavedDraftDiffers(changed, undefined)).toBe(false);
  });

  it("offers restore only for a differing stored draft", () => {
    const stored: AutosavedEditorDraft = {
      version: AUTOSAVE_DRAFT_VERSION,
      contentId: "42",
      savedAt: "t",
      revision: 1,
      draft: { sys_title: "Draft title" },
    };
    expect(shouldOfferAutosaveRestore(stored, loaded)).toBe(true);
    expect(
      shouldOfferAutosaveRestore({ ...stored, draft: { sys_title: "Home" } }, loaded),
    ).toBe(false);
    expect(shouldOfferAutosaveRestore(null, loaded)).toBe(false);
  });
});
