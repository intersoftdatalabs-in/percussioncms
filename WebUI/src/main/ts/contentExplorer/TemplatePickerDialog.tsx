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

import React, { useState } from "react";
import type { PageTemplateChoice } from "../editor/pageTemplates";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

export interface TemplatePickerDialogProps {
  templates: PageTemplateChoice[];
  onPick: (templateId: string) => void;
  onCancel: () => void;
}

export function TemplatePickerDialog({
  templates,
  onPick,
  onCancel,
}: TemplatePickerDialogProps): React.ReactElement {
  const [value, setValue] = useState(templates[0]?.id ?? "");
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-template-picker-title"
      data-testid="explorer-template-picker"
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 40,
      }}
    >
      <form
        style={{
          background: "#fff",
          color: "#0f172a",
          minWidth: 280,
          maxWidth: 420,
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(e) => {
          e.preventDefault();
          if (value) {
            onPick(value);
          }
        }}
      >
        <h2 id="explorer-template-picker-title" style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.TEMPLATE_PICKER_TITLE)}
        </h2>
        <label style={{ display: "block", fontSize: 13 }}>
          {message(EXPLORER_MSG.TEMPLATE_PICKER_LABEL)}
          <select
            data-testid="explorer-template-picker-select"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          >
            {templates.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 16 }}>
          <button type="button" data-testid="explorer-template-picker-cancel" onClick={onCancel}>
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button type="submit" data-testid="explorer-template-picker-ok" disabled={!value}>
            {message(EXPLORER_MSG.CONFIRM_OK)}
          </button>
        </div>
      </form>
    </div>
  );
}
