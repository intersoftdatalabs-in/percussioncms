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
 * Slot + snippet-template picker for arrange change-template. Apply returns
 * a {@link SlotTemplateSlotPick}; the host / dispatcher POSTs
 * changeSlotTemplateSlot. Cancel does not change the relationship.
 */

import React, { useEffect, useId, useState } from "react";
import type {
  SlotAllowedChoice,
  SlotCanvasSlot,
} from "../api/contentExplorer/slotRelationshipApi";
import { EXPLORER_MSG } from "../contentExplorer/messages";
import { message } from "../i18n/message";
import styles from "./AssemblyHost.module.css";
import { ASSEMBLY_MSG } from "./messages";
import {
  resolveSlotTemplateSlotPick,
  type SlotTemplateSlotPick,
} from "./slotTemplateSlotPick";

export interface SlotChangeDialogProps {
  slots: SlotCanvasSlot[];
  initialSlotId: number;
  initialTemplateId?: number | null;
  loadTemplates: (slotId: number) => Promise<SlotAllowedChoice[]>;
  onCancel: () => void;
  onApply: (pick: SlotTemplateSlotPick) => void;
  /** Prefix for data-testid (AA vs Explorer). */
  testIdPrefix?: string;
}

export function SlotChangeDialog({
  slots,
  initialSlotId,
  initialTemplateId,
  loadTemplates,
  onCancel,
  onApply,
  testIdPrefix = "assembly-slot-change",
}: SlotChangeDialogProps): React.ReactElement {
  const titleId = useId();
  const slotFieldId = useId();
  const templateFieldId = useId();
  const [pickedSlot, setPickedSlot] = useState(
    initialSlotId > 0 ? String(initialSlotId) : "",
  );
  const [pickedTemplate, setPickedTemplate] = useState(
    initialTemplateId != null && initialTemplateId > 0
      ? String(initialTemplateId)
      : "",
  );
  const [templateChoices, setTemplateChoices] = useState<SlotAllowedChoice[]>(
    [],
  );
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    const slotId = Number(pickedSlot);
    if (!Number.isFinite(slotId) || slotId <= 0) {
      setTemplateChoices([]);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const tpls = await loadTemplates(slotId);
        if (cancelled) {
          return;
        }
        setTemplateChoices(tpls);
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
  }, [pickedSlot, loadTemplates]);

  function handleApply(): void {
    const pick = resolveSlotTemplateSlotPick({
      slotId: Number(pickedSlot),
      templateId: Number(pickedTemplate),
    });
    if (!pick) {
      setNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
      return;
    }
    if (pick.templateId <= 0) {
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
        <h2 id={titleId}>{message(ASSEMBLY_MSG.SLOT_CHANGE_TITLE)}</h2>
        {notice ? (
          <div role="alert" data-testid={`${testIdPrefix}-notice`}>
            {notice}
          </div>
        ) : null}
        <label htmlFor={slotFieldId}>
          {message(ASSEMBLY_MSG.SLOTS)}
          <select
            id={slotFieldId}
            data-testid={`${testIdPrefix}-slot`}
            value={pickedSlot}
            onChange={(e) => {
              setPickedSlot(e.target.value);
              setPickedTemplate("");
            }}
          >
            {slots.map((s) => (
              <option key={s.slotId} value={s.slotId}>
                {s.label || s.name || s.slotId}
              </option>
            ))}
          </select>
        </label>
        <label htmlFor={templateFieldId}>
          {message(ASSEMBLY_MSG.SLOT_TEMPLATE_LABEL)}
          <select
            id={templateFieldId}
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
