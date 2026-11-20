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
  canSaveLocaleDescriptionEdit,
  isLocaleCurrencySiblingEditing,
  isLocalePatternSiblingEditing,
  isLocaleRegionalSiblingEditing,
  isLocaleSiblingEditorOpen,
  isLocaleSystemSiblingEditing,
  isLocaleTextSiblingEditing,
  type LocaleDescriptionSaveReadiness,
  type LocaleSiblingEditorsEditing,
} from "../../../main/ts/developer/localeDetailSaveGuards";

const idleSiblings: LocaleSiblingEditorsEditing = {
  labelEditing: false,
  datePatternEditing: false,
  timePatternEditing: false,
  dateTimePatternEditing: false,
  decimalSepEditing: false,
  groupingSepEditing: false,
  currencyCodeEditing: false,
  currencyPatternEditing: false,
  textDirEditing: false,
  firstDayEditing: false,
  measurementEditing: false,
  defaultTzEditing: false,
  numberingSystemEditing: false,
  calendarEditing: false,
  statusEditing: false,
};

const siblingKeys = Object.keys(idleSiblings) as (keyof LocaleSiblingEditorsEditing)[];

const ready: LocaleDescriptionSaveReadiness = {
  isNew: false,
  hasDetail: true,
  hasWriteKey: true,
  descriptionEditing: true,
  dedicatedBlocked: false,
  siblings: idleSiblings,
};

describe("locale sibling editor groups", () => {
  it("is idle when no sibling editor is open", () => {
    expect(isLocaleTextSiblingEditing(idleSiblings)).toBe(false);
    expect(isLocalePatternSiblingEditing(idleSiblings)).toBe(false);
    expect(isLocaleCurrencySiblingEditing(idleSiblings)).toBe(false);
    expect(isLocaleRegionalSiblingEditing(idleSiblings)).toBe(false);
    expect(isLocaleSystemSiblingEditing(idleSiblings)).toBe(false);
    expect(isLocaleSiblingEditorOpen(idleSiblings)).toBe(false);
  });

  it("flags an open sibling in every group", () => {
    expect(isLocaleTextSiblingEditing({ ...idleSiblings, labelEditing: true })).toBe(true);
    expect(isLocalePatternSiblingEditing({ ...idleSiblings, datePatternEditing: true })).toBe(true);
    expect(isLocalePatternSiblingEditing({ ...idleSiblings, timePatternEditing: true })).toBe(true);
    expect(
      isLocalePatternSiblingEditing({ ...idleSiblings, dateTimePatternEditing: true }),
    ).toBe(true);
    expect(isLocaleCurrencySiblingEditing({ ...idleSiblings, decimalSepEditing: true })).toBe(true);
    expect(isLocaleCurrencySiblingEditing({ ...idleSiblings, groupingSepEditing: true })).toBe(true);
    expect(isLocaleCurrencySiblingEditing({ ...idleSiblings, currencyCodeEditing: true })).toBe(
      true,
    );
    expect(isLocaleCurrencySiblingEditing({ ...idleSiblings, currencyPatternEditing: true })).toBe(
      true,
    );
    expect(isLocaleRegionalSiblingEditing({ ...idleSiblings, textDirEditing: true })).toBe(true);
    expect(isLocaleRegionalSiblingEditing({ ...idleSiblings, firstDayEditing: true })).toBe(true);
    expect(isLocaleRegionalSiblingEditing({ ...idleSiblings, measurementEditing: true })).toBe(true);
    expect(isLocaleRegionalSiblingEditing({ ...idleSiblings, defaultTzEditing: true })).toBe(true);
    expect(
      isLocaleSystemSiblingEditing({ ...idleSiblings, numberingSystemEditing: true }),
    ).toBe(true);
    expect(isLocaleSystemSiblingEditing({ ...idleSiblings, calendarEditing: true })).toBe(true);
    expect(isLocaleSystemSiblingEditing({ ...idleSiblings, statusEditing: true })).toBe(true);
  });

  it("reports any single open sibling through the rollup", () => {
    for (const key of siblingKeys) {
      expect(isLocaleSiblingEditorOpen({ ...idleSiblings, [key]: true })).toBe(true);
    }
  });
});

describe("canSaveLocaleDescriptionEdit", () => {
  it("allows a save when the editor is open, keyed, and nothing is running", () => {
    expect(canSaveLocaleDescriptionEdit(ready)).toBe(true);
  });

  it("blocks create mode, missing detail, missing write key, and a closed editor", () => {
    expect(canSaveLocaleDescriptionEdit({ ...ready, isNew: true })).toBe(false);
    expect(canSaveLocaleDescriptionEdit({ ...ready, hasDetail: false })).toBe(false);
    expect(canSaveLocaleDescriptionEdit({ ...ready, hasWriteKey: false })).toBe(false);
    expect(canSaveLocaleDescriptionEdit({ ...ready, descriptionEditing: false })).toBe(false);
  });

  it("blocks while another save path is running", () => {
    expect(canSaveLocaleDescriptionEdit({ ...ready, dedicatedBlocked: true })).toBe(false);
  });

  it("blocks while any sibling editor is open", () => {
    for (const key of siblingKeys) {
      expect(
        canSaveLocaleDescriptionEdit({ ...ready, siblings: { ...idleSiblings, [key]: true } }),
      ).toBe(false);
    }
  });
});
