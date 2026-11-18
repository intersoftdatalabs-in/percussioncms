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
 * Readiness guards for the display-format detail inline editors.
 *
 * <p>The description and label editors share the same "another save is
 * running" shape (component busy flag plus three inflight refs). The inline
 * condition chains in {@code DisplayFormatDetailPanel} pushed
 * {@code handleDescriptionSave} over the cognitive/cyclomatic budget
 * (residual #6636, parent #5381), so the predicates live here as pure,
 * unit-tested helpers and the panel keeps only a single guard call.
 */
export interface InlineSaveBusy {
  busy: boolean;
  descriptionBusy: boolean;
  labelBusy: boolean;
  descriptionInflight: boolean;
  labelInflight: boolean;
  inflight: boolean;
}

/**
 * True when any display-format save path is already running. Inline field
 * saves must not start while the main save, the sibling field save, or a
 * previous attempt of the same field is still in flight.
 */
export function isInlineSaveBusy(flags: InlineSaveBusy): boolean {
  return (
    flags.busy ||
    flags.descriptionBusy ||
    flags.labelBusy ||
    flags.descriptionInflight ||
    flags.labelInflight ||
    flags.inflight
  );
}

export interface DescriptionSaveReadiness extends InlineSaveBusy {
  isNew: boolean;
  hasDetail: boolean;
  hasWriteKey: boolean;
  descriptionEditing: boolean;
  labelEditing: boolean;
}

/**
 * True when the description editor may attempt a save. Unsaved (create-mode)
 * formats have no write key yet, and the sibling label editor owns the
 * in-flight slot while it is open.
 */
export function canSaveDescriptionEdit(state: DescriptionSaveReadiness): boolean {
  if (state.isNew || !state.hasDetail || !state.hasWriteKey) {
    return false;
  }
  if (!state.descriptionEditing || state.labelEditing) {
    return false;
  }
  return !isInlineSaveBusy(state);
}

export interface LabelSaveReadiness extends InlineSaveBusy {
  isNew: boolean;
  hasDetail: boolean;
  hasWriteKey: boolean;
  labelEditing: boolean;
  descriptionEditing: boolean;
}

/**
 * True when the label editor may attempt a save. Mirror of
 * {@link canSaveDescriptionEdit} with the editor roles swapped.
 */
export function canSaveLabelEdit(state: LabelSaveReadiness): boolean {
  if (state.isNew || !state.hasDetail || !state.hasWriteKey) {
    return false;
  }
  if (!state.labelEditing || state.descriptionEditing) {
    return false;
  }
  return !isInlineSaveBusy(state);
}
