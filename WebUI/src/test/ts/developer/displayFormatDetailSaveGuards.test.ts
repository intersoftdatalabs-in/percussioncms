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
  canSaveDescriptionEdit,
  canSaveLabelEdit,
  isInlineSaveBusy,
  type DescriptionSaveReadiness,
  type LabelSaveReadiness,
} from "../../../main/ts/developer/displayFormatDetailSaveGuards";

const idleBusy = {
  busy: false,
  descriptionBusy: false,
  labelBusy: false,
  descriptionInflight: false,
  labelInflight: false,
  inflight: false,
};

const readyDescription: DescriptionSaveReadiness = {
  ...idleBusy,
  isNew: false,
  hasDetail: true,
  hasWriteKey: true,
  descriptionEditing: true,
  labelEditing: false,
};

const readyLabel: LabelSaveReadiness = {
  ...idleBusy,
  isNew: false,
  hasDetail: true,
  hasWriteKey: true,
  labelEditing: true,
  descriptionEditing: false,
};

describe("isInlineSaveBusy", () => {
  it("is idle when no save is running", () => {
    expect(isInlineSaveBusy(idleBusy)).toBe(false);
  });

  it("blocks when any single save path is running", () => {
    for (const key of Object.keys(idleBusy) as (keyof typeof idleBusy)[]) {
      expect(isInlineSaveBusy({ ...idleBusy, [key]: true })).toBe(true);
    }
  });
});

describe("canSaveDescriptionEdit", () => {
  it("allows a save when the editor is open and nothing is running", () => {
    expect(canSaveDescriptionEdit(readyDescription)).toBe(true);
  });

  it("blocks create mode, missing detail, and missing write key", () => {
    expect(canSaveDescriptionEdit({ ...readyDescription, isNew: true })).toBe(false);
    expect(canSaveDescriptionEdit({ ...readyDescription, hasDetail: false })).toBe(false);
    expect(canSaveDescriptionEdit({ ...readyDescription, hasWriteKey: false })).toBe(false);
  });

  it("blocks when the editor is closed or the sibling label editor is open", () => {
    expect(canSaveDescriptionEdit({ ...readyDescription, descriptionEditing: false })).toBe(
      false,
    );
    expect(canSaveDescriptionEdit({ ...readyDescription, labelEditing: true })).toBe(false);
  });

  it("blocks while any save path is running", () => {
    expect(canSaveDescriptionEdit({ ...readyDescription, busy: true })).toBe(false);
    expect(canSaveDescriptionEdit({ ...readyDescription, descriptionBusy: true })).toBe(false);
    expect(canSaveDescriptionEdit({ ...readyDescription, labelBusy: true })).toBe(false);
    expect(
      canSaveDescriptionEdit({ ...readyDescription, descriptionInflight: true }),
    ).toBe(false);
    expect(canSaveDescriptionEdit({ ...readyDescription, labelInflight: true })).toBe(false);
    expect(canSaveDescriptionEdit({ ...readyDescription, inflight: true })).toBe(false);
  });
});

describe("canSaveLabelEdit", () => {
  it("allows a save when the editor is open and nothing is running", () => {
    expect(canSaveLabelEdit(readyLabel)).toBe(true);
  });

  it("blocks create mode, missing detail, and missing write key", () => {
    expect(canSaveLabelEdit({ ...readyLabel, isNew: true })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, hasDetail: false })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, hasWriteKey: false })).toBe(false);
  });

  it("blocks when the editor is closed or the sibling description editor is open", () => {
    expect(canSaveLabelEdit({ ...readyLabel, labelEditing: false })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, descriptionEditing: true })).toBe(false);
  });

  it("blocks while any save path is running", () => {
    expect(canSaveLabelEdit({ ...readyLabel, busy: true })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, labelBusy: true })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, descriptionBusy: true })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, labelInflight: true })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, descriptionInflight: true })).toBe(false);
    expect(canSaveLabelEdit({ ...readyLabel, inflight: true })).toBe(false);
  });
});
