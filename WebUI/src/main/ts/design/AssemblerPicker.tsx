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

import React from "react";
import { catalogColors } from "../developer/catalogStyles";
import { assemblerSelectOptions } from "./assemblerOptions";
import { DESIGN_MSG } from "./messages";

const selectStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  maxWidth: 480,
  boxSizing: "border-box",
};

/**
 * Design SPA assembler picker (#2810). Controlled select of modern assemblers
 * plus the current value when it is outside the catalog.
 */
export function AssemblerPicker({
  value,
  disabled,
  onChange,
}: {
  value: string;
  disabled?: boolean;
  onChange: (next: string) => void;
}): React.ReactElement {
  const options = assemblerSelectOptions(value);
  return (
    <section data-testid="design-tpl-assembler" style={{ marginBottom: "16px" }}>
      <h3 style={{ fontSize: "1rem" }}>{DESIGN_MSG.EDITOR_ASSEMBLER}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
        {DESIGN_MSG.EDITOR_ASSEMBLER_HINT}
      </p>
      <label htmlFor="design-tpl-assembler-select" style={{ display: "block", marginBottom: 4 }}>
        {DESIGN_MSG.FIELD_ASSEMBLER}
      </label>
      <select
        id="design-tpl-assembler-select"
        data-testid="design-tpl-assembler-select"
        aria-label={DESIGN_MSG.EDITOR_ASSEMBLER_ARIA}
        style={selectStyle}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value} title={o.hint}>
            {o.label}
            {o.recommended ? " ★" : ""}
            {` — ${o.value.split("/").pop() || o.value}`}
          </option>
        ))}
      </select>
    </section>
  );
}
