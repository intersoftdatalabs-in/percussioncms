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
  canSaveContentTypeDescriptionEdit,
  canSaveContentTypeLabelEdit,
  isContentTypeInlineSaveBusy,
  type ContentTypeDescriptionSaveReadiness,
  type ContentTypeLabelSaveReadiness,
} from "../../../main/ts/developer/contentTypeDetailSaveGuards";

const idleBusy = {
  busy: false,
  labelBusy: false,
  descriptionBusy: false,
  labelInflight: false,
  descriptionInflight: false,
  requiredInflight: false,
  searchableInflight: false,
  requiredEditing: false,
  requiredBusy: false,
  searchableEditing: false,
  searchableBusy: false,
};

const readyLabel: ContentTypeLabelSaveReadiness = {
  ...idleBusy,
  hasDetail: true,
  hasLock: true,
  labelEditing: true,
  descriptionEditing: false,
};

const readyDescription: ContentTypeDescriptionSaveReadiness = {
  ...idleBusy,
  hasDetail: true,
  hasLock: true,
  descriptionEditing: true,
  labelEditing: false,
};

describe("isContentTypeInlineSaveBusy", () => {
  it("is idle when no save is running", () => {
    expect(isContentTypeInlineSaveBusy(idleBusy)).toBe(false);
  });

  it("blocks when any single save path is running", () => {
    for (const key of Object.keys(idleBusy) as (keyof typeof idleBusy)[]) {
      expect(isContentTypeInlineSaveBusy({ ...idleBusy, [key]: true })).toBe(true);
    }
  });
});

describe("canSaveContentTypeLabelEdit", () => {
  it("allows a save when the editor is open, locked, and nothing is running", () => {
    expect(canSaveContentTypeLabelEdit(readyLabel)).toBe(true);
  });

  it("blocks missing detail, missing lock, and a closed editor", () => {
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, hasDetail: false })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, hasLock: false })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, labelEditing: false })).toBe(false);
  });

  it("blocks when the sibling description editor is open", () => {
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, descriptionEditing: true })).toBe(false);
  });

  it("blocks while any save path is running", () => {
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, busy: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, labelBusy: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, descriptionBusy: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, labelInflight: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, descriptionInflight: true })).toBe(
      false,
    );
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, requiredInflight: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, searchableInflight: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, requiredEditing: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, requiredBusy: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, searchableEditing: true })).toBe(false);
    expect(canSaveContentTypeLabelEdit({ ...readyLabel, searchableBusy: true })).toBe(false);
  });
});

describe("canSaveContentTypeDescriptionEdit", () => {
  it("allows a save when the editor is open, locked, and nothing is running", () => {
    expect(canSaveContentTypeDescriptionEdit(readyDescription)).toBe(true);
  });

  it("blocks missing detail, missing lock, and a closed editor", () => {
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, hasDetail: false })).toBe(
      false,
    );
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, hasLock: false })).toBe(
      false,
    );
    expect(
      canSaveContentTypeDescriptionEdit({ ...readyDescription, descriptionEditing: false }),
    ).toBe(false);
  });

  it("blocks when the sibling label editor is open", () => {
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, labelEditing: true })).toBe(
      false,
    );
  });

  it("blocks while any save path is running", () => {
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, busy: true })).toBe(false);
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, descriptionBusy: true })).toBe(
      false,
    );
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, labelBusy: true })).toBe(
      false,
    );
    expect(
      canSaveContentTypeDescriptionEdit({ ...readyDescription, descriptionInflight: true }),
    ).toBe(false);
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, labelInflight: true })).toBe(
      false,
    );
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, requiredInflight: true })).toBe(
      false,
    );
    expect(
      canSaveContentTypeDescriptionEdit({ ...readyDescription, searchableInflight: true }),
    ).toBe(false);
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, requiredEditing: true })).toBe(
      false,
    );
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, requiredBusy: true })).toBe(
      false,
    );
    expect(
      canSaveContentTypeDescriptionEdit({ ...readyDescription, searchableEditing: true }),
    ).toBe(false);
    expect(canSaveContentTypeDescriptionEdit({ ...readyDescription, searchableBusy: true })).toBe(
      false,
    );
  });
});
