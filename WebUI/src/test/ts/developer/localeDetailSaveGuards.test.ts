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
  canSaveLocaleFieldEdit,
  canStartDatePatternEdit,
  canStartDateTimePatternEdit,
  canStartLocaleFieldEdit,
  canStartTimePatternEdit,
  isLocaleCurrencySiblingEditing,
  isLocaleOtherEditorOpen,
  isLocalePatternSiblingEditing,
  isLocaleRegionalSiblingEditing,
  isLocaleSiblingEditorOpen,
  isLocaleSystemSiblingEditing,
  isLocaleTextSiblingEditing,
  type LocaleDescriptionSaveReadiness,
  type LocaleFieldEditorsEditing,
  type LocaleFieldSaveReadiness,
  type LocaleFieldStartReadiness,
  type LocalePatternStartState,
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

const idleFieldEditors: LocaleFieldEditorsEditing = {
  ...idleSiblings,
  descriptionEditing: false,
};

const fieldEditorKeys = Object.keys(idleFieldEditors) as (keyof LocaleFieldEditorsEditing)[];

const readyField: LocaleFieldSaveReadiness = {
  isNew: false,
  hasDetail: true,
  hasWriteKey: true,
  fieldEditing: true,
  dedicatedBlocked: false,
  siblingOpen: false,
};

describe("isLocaleOtherEditorOpen", () => {
  it("is idle when no editor is open", () => {
    for (const self of fieldEditorKeys) {
      expect(isLocaleOtherEditorOpen(idleFieldEditors, self)).toBe(false);
    }
  });

  it("ignores the self editor but flags every other open editor", () => {
    for (const self of fieldEditorKeys) {
      expect(isLocaleOtherEditorOpen({ ...idleFieldEditors, [self]: true }, self)).toBe(false);
      for (const other of fieldEditorKeys) {
        if (other === self) continue;
        expect(isLocaleOtherEditorOpen({ ...idleFieldEditors, [other]: true }, self)).toBe(true);
      }
    }
  });

  it("flags an open description editor from any other field path", () => {
    expect(isLocaleOtherEditorOpen({ ...idleFieldEditors, descriptionEditing: true }, "datePatternEditing")).toBe(
      true,
    );
    expect(isLocaleOtherEditorOpen({ ...idleFieldEditors, descriptionEditing: true }, "descriptionEditing")).toBe(
      false,
    );
  });
});

describe("canSaveLocaleFieldEdit", () => {
  it("allows a save when the editor is open, keyed, and nothing is running", () => {
    expect(canSaveLocaleFieldEdit(readyField)).toBe(true);
  });

  it("blocks create mode, missing detail, missing write key, and a closed editor", () => {
    expect(canSaveLocaleFieldEdit({ ...readyField, isNew: true })).toBe(false);
    expect(canSaveLocaleFieldEdit({ ...readyField, hasDetail: false })).toBe(false);
    expect(canSaveLocaleFieldEdit({ ...readyField, hasWriteKey: false })).toBe(false);
    expect(canSaveLocaleFieldEdit({ ...readyField, fieldEditing: false })).toBe(false);
  });

  it("blocks while another save path is running or a sibling editor is open", () => {
    expect(canSaveLocaleFieldEdit({ ...readyField, dedicatedBlocked: true })).toBe(false);
    expect(canSaveLocaleFieldEdit({ ...readyField, siblingOpen: true })).toBe(false);
  });
});

const readyStart: LocaleFieldStartReadiness = {
  isNew: false,
  hasDetail: true,
  dedicatedBlocked: false,
  siblingOpen: false,
};

describe("canStartLocaleFieldEdit", () => {
  it("allows an edit when keyed with detail and nothing is running", () => {
    expect(canStartLocaleFieldEdit(readyStart)).toBe(true);
  });

  it("blocks create mode, missing detail, and a running save", () => {
    expect(canStartLocaleFieldEdit({ ...readyStart, isNew: true })).toBe(false);
    expect(canStartLocaleFieldEdit({ ...readyStart, hasDetail: false })).toBe(false);
    expect(canStartLocaleFieldEdit({ ...readyStart, dedicatedBlocked: true })).toBe(false);
  });

  it("blocks while a sibling editor is open", () => {
    expect(canStartLocaleFieldEdit({ ...readyStart, siblingOpen: true })).toBe(false);
  });
});

const readyPatternStart: LocalePatternStartState = {
  isNew: false,
  hasDetail: true,
  dedicatedBlocked: false,
  editing: idleFieldEditors,
};

const patternStartGuards = {
  datePatternEditing: canStartDatePatternEdit,
  timePatternEditing: canStartTimePatternEdit,
  dateTimePatternEditing: canStartDateTimePatternEdit,
} as const;

type PatternStartField = keyof typeof patternStartGuards;

const patternStartFields = Object.keys(patternStartGuards) as PatternStartField[];

describe("pattern start guards", () => {
  it("allows an edit when idle and keyed", () => {
    for (const field of patternStartFields) {
      expect(patternStartGuards[field](readyPatternStart)).toBe(true);
    }
  });

  it("ignores the self editor flag", () => {
    for (const field of patternStartFields) {
      expect(
        patternStartGuards[field]({
          ...readyPatternStart,
          editing: { ...idleFieldEditors, [field]: true },
        }),
      ).toBe(true);
    }
  });

  it("blocks create mode, missing detail, and a running save", () => {
    for (const field of patternStartFields) {
      expect(patternStartGuards[field]({ ...readyPatternStart, isNew: true })).toBe(false);
      expect(patternStartGuards[field]({ ...readyPatternStart, hasDetail: false })).toBe(false);
      expect(patternStartGuards[field]({ ...readyPatternStart, dedicatedBlocked: true })).toBe(
        false,
      );
    }
  });

  it("blocks while any other editor is open", () => {
    for (const field of patternStartFields) {
      for (const other of fieldEditorKeys) {
        if (other === field) continue;
        expect(
          patternStartGuards[field]({
            ...readyPatternStart,
            editing: { ...idleFieldEditors, [other]: true },
          }),
        ).toBe(false);
      }
    }
  });
});
