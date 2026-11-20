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
 * Readiness guard for the locale detail description inline editor.
 *
 * <p>The description save in {@code LocaleDetailPanel} chains the create-mode
 * / detail / write-key preconditions, the shared {@code dedicatedWriteBlocked}
 * busy fold, and one flag per sibling inline editor. The inline condition
 * chain pushed {@code handleDescriptionSave} over the cognitive/cyclomatic
 * budget (residual #6803, parent #6747), so the predicates live here as pure,
 * unit-tested helpers and the panel keeps only a single guard call per save
 * path plus outcome helpers. No behavior change.
 */
export interface LocaleSiblingEditorsEditing {
  labelEditing: boolean;
  datePatternEditing: boolean;
  timePatternEditing: boolean;
  dateTimePatternEditing: boolean;
  decimalSepEditing: boolean;
  groupingSepEditing: boolean;
  currencyCodeEditing: boolean;
  currencyPatternEditing: boolean;
  textDirEditing: boolean;
  firstDayEditing: boolean;
  measurementEditing: boolean;
  defaultTzEditing: boolean;
  numberingSystemEditing: boolean;
  calendarEditing: boolean;
  statusEditing: boolean;
}

/** True when the sibling label editor owns the in-flight slot. */
export function isLocaleTextSiblingEditing(editing: LocaleSiblingEditorsEditing): boolean {
  return editing.labelEditing;
}

/** True when a sibling date/time pattern editor owns the in-flight slot. */
export function isLocalePatternSiblingEditing(editing: LocaleSiblingEditorsEditing): boolean {
  return (
    editing.datePatternEditing || editing.timePatternEditing || editing.dateTimePatternEditing
  );
}

/** True when a sibling currency/number editor owns the in-flight slot. */
export function isLocaleCurrencySiblingEditing(editing: LocaleSiblingEditorsEditing): boolean {
  return (
    editing.decimalSepEditing ||
    editing.groupingSepEditing ||
    editing.currencyCodeEditing ||
    editing.currencyPatternEditing
  );
}

/** True when a sibling regional editor owns the in-flight slot. */
export function isLocaleRegionalSiblingEditing(editing: LocaleSiblingEditorsEditing): boolean {
  return (
    editing.textDirEditing ||
    editing.firstDayEditing ||
    editing.measurementEditing ||
    editing.defaultTzEditing
  );
}

/** True when a sibling system editor owns the in-flight slot. */
export function isLocaleSystemSiblingEditing(editing: LocaleSiblingEditorsEditing): boolean {
  return editing.numberingSystemEditing || editing.calendarEditing || editing.statusEditing;
}

/**
 * True when any sibling inline editor is open. The description editor must
 * not save while another editor owns the in-flight slot.
 */
export function isLocaleSiblingEditorOpen(editing: LocaleSiblingEditorsEditing): boolean {
  return (
    isLocaleTextSiblingEditing(editing) ||
    isLocalePatternSiblingEditing(editing) ||
    isLocaleCurrencySiblingEditing(editing) ||
    isLocaleRegionalSiblingEditing(editing) ||
    isLocaleSystemSiblingEditing(editing)
  );
}

export interface LocaleDescriptionSaveReadiness {
  isNew: boolean;
  hasDetail: boolean;
  hasWriteKey: boolean;
  descriptionEditing: boolean;
  dedicatedBlocked: boolean;
  siblings: LocaleSiblingEditorsEditing;
}

/**
 * True when the description editor may attempt a save. Unsaved (create-mode)
 * locales have no write key yet, and a running save or an open sibling
 * editor blocks a new attempt.
 */
export function canSaveLocaleDescriptionEdit(state: LocaleDescriptionSaveReadiness): boolean {
  if (state.isNew || !state.hasDetail || !state.hasWriteKey || !state.descriptionEditing) {
    return false;
  }
  return !state.dedicatedBlocked && !isLocaleSiblingEditorOpen(state.siblings);
}
