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
 * Type / snippet-template pick for slot create. Hosts open the picker;
 * dispatch creates via itemmanagement and POSTs addSlotRelationship.
 */

import type { AssemblySlotContext } from "./slotContext";

export interface SlotCreatePick {
  contentType: string;
  folderPath: string;
  templateId?: string;
  snippetTemplateId: number;
}

export interface SlotCreatePickerSession {
  slot: AssemblySlotContext;
  resolve: (value: SlotCreatePick | null) => void;
}

export function replaceSlotCreatePickerSession(
  previous: SlotCreatePickerSession | null,
  next: SlotCreatePickerSession,
): SlotCreatePickerSession {
  if (previous && previous !== next) {
    previous.resolve(null);
  }
  return next;
}

export function settleSlotCreatePickerSession(
  session: SlotCreatePickerSession | null,
  value: SlotCreatePick | null,
): null {
  session?.resolve(value);
  return null;
}

/**
 * Validate picker fields. Missing type or folder is {@code null} (stay in
 * dialog). Missing snippet template yields {@code snippetTemplateId: 0} so
 * dispatch can show needs-template instead of posting.
 */
export function resolveSlotCreatePick(input: {
  contentType: string;
  folderPath: string;
  snippetTemplateId: number;
  templateId?: string;
}): SlotCreatePick | null {
  const contentType = input.contentType.trim();
  const folderPath = input.folderPath.trim();
  if (!contentType || !folderPath) {
    return null;
  }
  const snippetTemplateId = Number(input.snippetTemplateId);
  const templateId = input.templateId?.trim();
  const pick: SlotCreatePick = {
    contentType,
    folderPath,
    snippetTemplateId:
      Number.isFinite(snippetTemplateId) && snippetTemplateId > 0
        ? snippetTemplateId
        : 0,
  };
  if (templateId) {
    pick.templateId = templateId;
  }
  return pick;
}
