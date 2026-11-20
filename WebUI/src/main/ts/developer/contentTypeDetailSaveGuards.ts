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
 * Readiness guards for the content-type detail inline editors.
 *
 * <p>The label and description editors share the same "another save is
 * running" shape (component busy flag, per-field busy flags, inflight refs,
 * plus the sibling required/searchable field saves). The inline condition
 * chains in {@code ContentTypeDetailPanel} pushed
 * {@code handleLabelSave} and {@code handleDescriptionSave} over the
 * cognitive/cyclomatic budget (residual #6802, parent #6747), so the
 * predicates live here as pure, unit-tested helpers and the panel keeps
 * only a single guard call per save path.
 */
export interface ContentTypeInlineSaveBusy {
  busy: boolean;
  labelBusy: boolean;
  descriptionBusy: boolean;
  labelInflight: boolean;
  descriptionInflight: boolean;
  requiredInflight: boolean;
  searchableInflight: boolean;
  requiredEditing: boolean;
  requiredBusy: boolean;
  searchableEditing: boolean;
  searchableBusy: boolean;
}

/**
 * True when any content-type save path is already running. Inline field
 * saves must not start while the main save, a sibling field save, or a
 * previous attempt of the same field is still in flight, nor while the
 * sibling required/searchable editors own the in-flight slot.
 */
export function isContentTypeInlineSaveBusy(flags: ContentTypeInlineSaveBusy): boolean {
  return (
    flags.busy ||
    flags.labelBusy ||
    flags.descriptionBusy ||
    flags.labelInflight ||
    flags.descriptionInflight ||
    flags.requiredInflight ||
    flags.searchableInflight ||
    flags.requiredEditing ||
    flags.requiredBusy ||
    flags.searchableEditing ||
    flags.searchableBusy
  );
}

export interface ContentTypeLabelSaveReadiness extends ContentTypeInlineSaveBusy {
  hasDetail: boolean;
  hasLock: boolean;
  labelEditing: boolean;
  descriptionEditing: boolean;
}

/**
 * True when the label editor may attempt a save. The sibling description
 * editor owns the in-flight slot while it is open.
 */
export function canSaveContentTypeLabelEdit(state: ContentTypeLabelSaveReadiness): boolean {
  if (!state.hasDetail || !state.hasLock || !state.labelEditing) {
    return false;
  }
  if (state.descriptionEditing) {
    return false;
  }
  return !isContentTypeInlineSaveBusy(state);
}

export interface ContentTypeDescriptionSaveReadiness extends ContentTypeInlineSaveBusy {
  hasDetail: boolean;
  hasLock: boolean;
  descriptionEditing: boolean;
  labelEditing: boolean;
}

/**
 * True when the description editor may attempt a save. Mirror of
 * {@link canSaveContentTypeLabelEdit} with the editor roles swapped.
 */
export function canSaveContentTypeDescriptionEdit(
  state: ContentTypeDescriptionSaveReadiness,
): boolean {
  if (!state.hasDetail || !state.hasLock || !state.descriptionEditing) {
    return false;
  }
  if (state.labelEditing) {
    return false;
  }
  return !isContentTypeInlineSaveBusy(state);
}
