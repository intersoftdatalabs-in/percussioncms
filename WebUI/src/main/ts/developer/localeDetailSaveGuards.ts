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

/**
 * Full 16-editor snapshot for the generic field save guard (follow-on #6841,
 * parent #6747). The description-centric {@link LocaleSiblingEditorsEditing}
 * omits the description editor (it is `self` on that path); every other field
 * save must also treat an open description editor as a blocking sibling, so
 * the generic path carries all sixteen flags plus the self key to exclude.
 */
export interface LocaleFieldEditorsEditing extends LocaleSiblingEditorsEditing {
  descriptionEditing: boolean;
}

export interface LocaleFieldSaveReadiness {
  isNew: boolean;
  hasDetail: boolean;
  hasWriteKey: boolean;
  fieldEditing: boolean;
  dedicatedBlocked: boolean;
  siblingOpen: boolean;
}

/**
 * True when the field editor may attempt a save. Unsaved (create-mode)
 * locales have no write key yet, and a running save or an open sibling
 * editor blocks a new attempt. All thirteen over-budget sibling save guards
 * in {@code LocaleDetailPanel} funnel through this single predicate, so the
 * inline ~20-term OR chains (plus the duplicated terms some handlers carried)
 * collapse to one guard call per save path. No behavior change: the sibling
 * set stays "every other editor open", duplicates included.
 */
export function canSaveLocaleFieldEdit(state: LocaleFieldSaveReadiness): boolean {
  if (state.isNew || !state.hasDetail || !state.hasWriteKey || !state.fieldEditing) {
    return false;
  }
  return !state.dedicatedBlocked && !state.siblingOpen;
}

/**
 * True when any editor other than {@code except} is open. Clearing the self
 * flag before the rollup keeps the predicate a flat two-branch check while
 * preserving the exact "every other editor" semantics of the inline guards.
 */
export function isLocaleOtherEditorOpen(
  editing: LocaleFieldEditorsEditing,
  except: keyof LocaleFieldEditorsEditing,
): boolean {
  const cleared: LocaleFieldEditorsEditing = { ...editing };
  cleared[except] = false;
  return cleared.descriptionEditing || isLocaleSiblingEditorOpen(cleared);
}

export interface LocaleFieldStartReadiness {
  isNew: boolean;
  hasDetail: boolean;
  dedicatedBlocked: boolean;
  siblingOpen: boolean;
}

/**
 * True when the field editor may enter edit mode. Starting an edit needs no
 * write key and no open self flag (unlike the save path): create-mode locales
 * have nothing to edit yet, a missing detail means no shown value to draft
 * from, and a running save or an open sibling editor blocks a new attempt.
 * The three pattern start guards in {@code LocaleDetailPanel}
 * ({@code startDatePatternEdit}, {@code startTimePatternEdit},
 * {@code startDateTimePatternEdit}) funnel through this single predicate, so
 * each inline ~19-term OR chain (residual #6888, parent #6841) collapses to
 * one guard call per start path. No behavior change: the sibling set stays
 * "every other editor open", self excluded.
 */
export function canStartLocaleFieldEdit(state: LocaleFieldStartReadiness): boolean {
  if (state.isNew || !state.hasDetail || state.dedicatedBlocked) {
    return false;
  }
  return !state.siblingOpen;
}

export interface LocalePatternStartState {
  isNew: boolean;
  hasDetail: boolean;
  dedicatedBlocked: boolean;
  editing: LocaleFieldEditorsEditing;
}

/**
 * True when the date pattern editor may enter edit mode. Thin per-field
 * wrapper that excludes the date pattern editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startDatePatternEdit}.
 */
export function canStartDatePatternEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "datePatternEditing"),
  });
}

/**
 * True when the time pattern editor may enter edit mode. Thin per-field
 * wrapper that excludes the time pattern editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startTimePatternEdit}.
 */
export function canStartTimePatternEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "timePatternEditing"),
  });
}

/**
 * True when the date/time pattern editor may enter edit mode. Thin per-field
 * wrapper that excludes the date/time pattern editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startDateTimePatternEdit}.
 */
export function canStartDateTimePatternEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "dateTimePatternEditing"),
  });
}

/**
 * True when the label editor may enter edit mode. Thin per-field wrapper
 * that excludes the label editor itself from the sibling rollup, preserving
 * the exact inline semantics of {@code startLabelEdit} (residual #6952,
 * parent #6841). No behavior change: the sibling set stays "every other
 * editor open", self excluded.
 */
export function canStartLabelEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "labelEditing"),
  });
}

/**
 * True when the description editor may enter edit mode. Thin per-field
 * wrapper that excludes the description editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startDescriptionEdit} (residual #6952, parent #6841). No behavior
 * change: the sibling set stays "every other editor open", self excluded.
 */
export function canStartDescriptionEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "descriptionEditing"),
  });
}

/**
 * True when the decimal separator editor may enter edit mode. Thin per-field
 * wrapper that excludes the decimal separator editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startDecimalSepEdit} (residual #6952, parent #6841). No behavior
 * change: the sibling set stays "every other editor open", self excluded.
 */
export function canStartDecimalSepEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "decimalSepEditing"),
  });
}

/**
 * True when the grouping separator editor may enter edit mode. Thin
 * per-field wrapper that excludes the grouping separator editor itself from
 * the sibling rollup, preserving the exact inline semantics of
 * {@code startGroupingSepEdit} (residual #6952, parent #6841). No behavior
 * change: the sibling set stays "every other editor open", self excluded.
 */
export function canStartGroupingSepEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "groupingSepEditing"),
  });
}

/**
 * True when the currency code editor may enter edit mode. Thin per-field
 * wrapper that excludes the currency code editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startCurrencyCodeEdit} (residual #6952, parent #6841). No behavior
 * change: the sibling set stays "every other editor open", self excluded.
 */
export function canStartCurrencyCodeEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "currencyCodeEditing"),
  });
}

/**
 * True when the currency pattern editor may enter edit mode. Thin per-field
 * wrapper that excludes the currency pattern editor itself from the sibling
 * rollup, preserving the exact inline semantics of
 * {@code startCurrencyPatternEdit} (residual #6952, parent #6841). No
 * behavior change: the sibling set stays "every other editor open", self
 * excluded.
 */
export function canStartCurrencyPatternEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "currencyPatternEditing"),
  });
}

/**
 * True when the text direction editor may enter edit mode. Thin per-field
 * wrapper that excludes the text direction editor itself from the sibling
 * rollup, preserving the exact inline semantics of {@code startTextDirEdit}
 * (residual #6952, parent #6841). No behavior change: the sibling set stays
 * "every other editor open", self excluded.
 */
export function canStartTextDirEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "textDirEditing"),
  });
}

/**
 * True when the first-day-of-week editor may enter edit mode. Thin per-field
 * wrapper that excludes the first-day editor itself from the sibling rollup,
 * preserving the exact inline semantics of {@code startFirstDayEdit}
 * (residual #6952, parent #6841). No behavior change: the sibling set stays
 * "every other editor open", self excluded.
 */
export function canStartFirstDayEdit(state: LocalePatternStartState): boolean {
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "firstDayEditing"),
  });
}

/**
 * True when the measurement system editor may enter edit mode. Thin
 * per-field wrapper preserving the exact inline semantics of
 * {@code startMeasurementEdit} (residual #6952, parent #6841): unlike the
 * sibling starters above, that inline chain also names the measurement
 * editor itself, so an already-open measurement editor keeps blocking a
 * restart. No behavior change.
 */
export function canStartMeasurementEdit(state: LocalePatternStartState): boolean {
  if (state.editing.measurementEditing) {
    return false;
  }
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "measurementEditing"),
  });
}

/**
 * True when the default time-zone editor may enter edit mode. Thin per-field
 * wrapper preserving the exact inline semantics of
 * {@code startDefaultTzEdit} (residual #6952, parent #6841): unlike the
 * sibling starters above, that inline chain also names the default-TZ editor
 * itself, so an already-open default-TZ editor keeps blocking a restart. No
 * behavior change.
 */
export function canStartDefaultTzEdit(state: LocalePatternStartState): boolean {
  if (state.editing.defaultTzEditing) {
    return false;
  }
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "defaultTzEditing"),
  });
}

/**
 * True when the numbering system editor may enter edit mode. Thin per-field
 * wrapper preserving the exact inline semantics of
 * {@code startNumberingSystemEdit} (residual #6952, parent #6841): unlike the
 * sibling starters above, that inline chain also names the numbering-system
 * editor itself, so an already-open numbering-system editor keeps blocking a
 * restart. No behavior change.
 */
export function canStartNumberingSystemEdit(state: LocalePatternStartState): boolean {
  if (state.editing.numberingSystemEditing) {
    return false;
  }
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "numberingSystemEditing"),
  });
}

/**
 * True when the calendar editor may enter edit mode. Thin per-field wrapper
 * preserving the exact inline semantics of {@code startCalendarEdit}
 * (residual #6952, parent #6841): unlike the sibling starters above, that
 * inline chain also names the calendar editor itself, so an already-open
 * calendar editor keeps blocking a restart. No behavior change.
 */
export function canStartCalendarEdit(state: LocalePatternStartState): boolean {
  if (state.editing.calendarEditing) {
    return false;
  }
  return canStartLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleOtherEditorOpen(state.editing, "calendarEditing"),
  });
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
 * editor blocks a new attempt. Delegates to the generic field predicate so
 * there is exactly one readiness truth table to pin.
 */
export function canSaveLocaleDescriptionEdit(state: LocaleDescriptionSaveReadiness): boolean {
  return canSaveLocaleFieldEdit({
    isNew: state.isNew,
    hasDetail: state.hasDetail,
    hasWriteKey: state.hasWriteKey,
    fieldEditing: state.descriptionEditing,
    dedicatedBlocked: state.dedicatedBlocked,
    siblingOpen: isLocaleSiblingEditorOpen(state.siblings),
  });
}
