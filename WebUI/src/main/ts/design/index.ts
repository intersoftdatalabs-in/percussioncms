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

export { DesignShell, normalizeDesignSection } from "./DesignShell";
export type { DesignShellProps, DesignSection } from "./DesignShell";
export { TemplateLibraryPanel, templateSelectionKey } from "./TemplateLibraryPanel";
export { CreateTemplateDialog } from "./CreateTemplateDialog";
export { DeleteTemplateDialog } from "./DeleteTemplateDialog";
export { validateTemplateCreateInput } from "./templateCreate";
export { DEFAULT_CREATE_ASSEMBLER } from "./assemblerOptions";
export { TemplateDetailDrawer } from "./TemplateDetailDrawer";
export { TemplateSourceEditor } from "./TemplateSourceEditor";
export { AssemblerPicker } from "./AssemblerPicker";
export { TemplateSlotsPanel, slotRowsDirty, dirtySlotSaves } from "./TemplateSlotsPanel";
export {
  ASSEMBLER_OPTIONS,
  assemblerSelectOptions,
  isValidAssemblerValue,
} from "./assemblerOptions";
export {
  layoutDraftFromMap,
  layoutMapFromDraft,
  stylesDraftFromMap,
  stylesMapFromDraft,
  templateSlotKey,
  SLOT_SCHEMA_VERSION,
} from "./slotLayoutStyles";
export {
  bindingsEqual,
  cloneBindings,
  normalizeBindingsForSave,
  validateBindings,
} from "./templateBindings";
export { DESIGN_MSG, DESIGN_MSG_KEYS } from "./messages";
