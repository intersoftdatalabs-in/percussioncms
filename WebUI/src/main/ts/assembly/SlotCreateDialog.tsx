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
 * Allowed type + snippet template picker for slot create. Apply returns a
 * {@link SlotCreatePick}; the host / dispatcher creates the item and POSTs
 * the relationship. Cancel does not create or add.
 */

import React, { useEffect, useId, useState } from "react";
import type { SlotAllowedChoice } from "../api/contentExplorer/slotRelationshipApi";
import { EXPLORER_MSG } from "../contentExplorer/messages";
import { message } from "../i18n/message";
import styles from "./AssemblyHost.module.css";
import { ASSEMBLY_MSG } from "./messages";
import { resolveSlotCreatePick, type SlotCreatePick } from "./slotCreatePick";

export interface SlotCreateDialogProps {
  slotId: number;
  initialFolder: string;
  loadTypes: (slotId: number) => Promise<SlotAllowedChoice[]>;
  loadTemplates: (slotId: number) => Promise<SlotAllowedChoice[]>;
  onCancel: () => void;
  onApply: (pick: SlotCreatePick) => void;
  /** Prefix for data-testid (AA vs Explorer). */
  testIdPrefix?: string;
}

export function SlotCreateDialog({
  slotId,
  initialFolder,
  loadTypes,
  loadTemplates,
  onCancel,
  onApply,
  testIdPrefix = "assembly-slot-create",
}: SlotCreateDialogProps): React.ReactElement {
  const titleId = useId();
  const folderId = useId();
  const typeId = useId();
  const templateId = useId();
  const [folderPath, setFolderPath] = useState(initialFolder);
  const [typeChoices, setTypeChoices] = useState<SlotAllowedChoice[]>([]);
  const [templateChoices, setTemplateChoices] = useState<SlotAllowedChoice[]>(
    [],
  );
  const [pickedType, setPickedType] = useState("");
  const [pickedTemplate, setPickedTemplate] = useState("");
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const types = await loadTypes(slotId);
        const tpls = await loadTemplates(slotId);
        if (cancelled) {
          return;
        }
        setTypeChoices(types);
        setTemplateChoices(tpls);
        setPickedType((cur) => cur || types[0]?.name || String(types[0]?.id ?? ""));
        setPickedTemplate((cur) => cur || (tpls[0] ? String(tpls[0].id) : ""));
      } catch {
        if (!cancelled) {
          setNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [slotId, loadTypes, loadTemplates]);

  function handleApply(): void {
    const pick = resolveSlotCreatePick({
      contentType: pickedType,
      folderPath,
      snippetTemplateId: Number(pickedTemplate),
    });
    if (!pick) {
      setNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
      return;
    }
    if (pick.snippetTemplateId <= 0) {
      setNotice(message(EXPLORER_MSG.ACTION_NEEDS_TEMPLATE));
      return;
    }
    onApply(pick);
  }

  return (
    <div
      className={styles.dialog}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      data-testid={`${testIdPrefix}-dialog`}
    >
      <div className={`${styles.dialogPanel} ${styles.picker}`}>
        <h2 id={titleId}>{message(ASSEMBLY_MSG.SLOT_CREATE_TITLE)}</h2>
        {notice ? (
          <div role="alert" data-testid={`${testIdPrefix}-notice`}>
            {notice}
          </div>
        ) : null}
        <label htmlFor={folderId}>
          {message(ASSEMBLY_MSG.SLOT_FOLDER_LABEL)}
          <input
            id={folderId}
            data-testid={`${testIdPrefix}-folder`}
            value={folderPath}
            onChange={(e) => setFolderPath(e.target.value)}
          />
        </label>
        <label htmlFor={typeId}>
          {message(ASSEMBLY_MSG.SLOT_TYPE_LABEL)}
          <select
            id={typeId}
            data-testid={`${testIdPrefix}-type`}
            value={pickedType}
            onChange={(e) => setPickedType(e.target.value)}
          >
            {typeChoices.map((t) => (
              <option key={t.id} value={t.name || String(t.id)}>
                {t.label || t.name || t.id}
              </option>
            ))}
          </select>
        </label>
        <label htmlFor={templateId}>
          {message(ASSEMBLY_MSG.SLOT_TEMPLATE_LABEL)}
          <select
            id={templateId}
            data-testid={`${testIdPrefix}-template`}
            value={pickedTemplate}
            onChange={(e) => setPickedTemplate(e.target.value)}
          >
            {templateChoices.map((t) => (
              <option key={t.id} value={t.id}>
                {t.label || t.name || t.id}
              </option>
            ))}
          </select>
        </label>
        <div className={styles.pickerActions}>
          <button
            type="button"
            data-testid={`${testIdPrefix}-cancel`}
            onClick={onCancel}
          >
            {message(ASSEMBLY_MSG.SLOT_CANCEL)}
          </button>
          <button
            type="button"
            data-testid={`${testIdPrefix}-apply`}
            onClick={handleApply}
          >
            {message(ASSEMBLY_MSG.SLOT_APPLY)}
          </button>
        </div>
      </div>
    </div>
  );
}
