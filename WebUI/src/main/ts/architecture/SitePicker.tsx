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
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";
import { ARCH_MSG } from "./messages";

export interface SiteOption {
  name: string;
}

export interface SitePickerProps {
  sites: SiteOption[];
  selectedSite: string | null;
  loading?: boolean;
  disabled?: boolean;
  onChange: (siteName: string | null) => void;
}

/**
 * Site selector for Architecture nav tree (#3095).
 */
export function SitePicker({
  sites,
  selectedSite,
  loading = false,
  disabled = false,
  onChange,
}: SitePickerProps): React.ReactElement {
  const value = selectedSite ?? "";
  return (
    <div
      data-testid="architecture-site-picker"
      style={{
        display: "flex",
        flexWrap: "wrap",
        alignItems: "center",
        gap: "0.5rem 0.75rem",
      }}
    >
      <label
        htmlFor="architecture-site-select"
        style={{
          fontWeight: 600,
          color: "#1a202c",
          fontSize: "0.95rem",
        }}
      >
        {ARCH_MSG.SITE_LABEL}
      </label>
      <select
        id="architecture-site-select"
        data-testid="architecture-site-select"
        value={value}
        disabled={disabled || loading || sites.length === 0}
        onChange={(e) => {
          const v = e.target.value;
          onChange(v.trim().length > 0 ? v : null);
        }}
        style={{
          minWidth: "14rem",
          maxWidth: "100%",
          padding: "0.4rem 0.6rem",
          border: `1px solid ${catalogColors.softBorder}`,
          borderRadius: 4,
          fontSize: "0.95rem",
          background: disabled || loading ? "#f0f0f0" : "#fff",
        }}
        aria-busy={loading || undefined}
      >
        <option value="">{ARCH_MSG.SITE_PLACEHOLDER}</option>
        {sites.map((s) => (
          <option
            key={s.name}
            value={s.name}
            {...{ [MKD_LANG_IGNORE_ATTR]: "1" as const }}
          >
            {s.name}
          </option>
        ))}
      </select>
    </div>
  );
}

export default SitePicker;
