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

import React, { useEffect, useMemo, useState } from "react";
import { isApiError } from "../api/client";
import { includeContentTypeField } from "../api/developer/contentTypesApi";
import { getSharedFieldGroupDetail, listSharedFieldGroups } from "../api/developer/sharedFieldsApi";
import { getSystemDef } from "../api/developer/systemDefApi";
import type { ContentTypeDetail, ContentTypeFieldSummary } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import {
  extractIncludeCatalogFields,
  isIncludeLockConflict,
  parseIncludeFieldOrigin,
  toIncludeCandidates,
  unusedIncludeCandidates,
  type IncludeFieldOrigin,
} from "./contentTypeIncludeField";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const smallBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "4px 8px",
  cursor: "pointer",
};

export function ContentTypeIncludeFieldSection({
  idOrName,
  existingFields,
  canEdit,
  onBusy,
  onIncluded,
  onError,
  onNotice,
  onLockLost,
}: {
  idOrName: string;
  existingFields: ContentTypeFieldSummary[];
  canEdit: boolean;
  onBusy: (busy: boolean) => void;
  onIncluded: (detail: ContentTypeDetail) => void;
  onError: (message: string | null) => void;
  onNotice: (message: string | null) => void;
  onLockLost: () => void;
}): React.ReactElement {
  const [origin, setOrigin] = useState<IncludeFieldOrigin>("system");
  const [name, setName] = useState("");
  const [systemCandidates, setSystemCandidates] = useState<
    ReturnType<typeof toIncludeCandidates>
  >([]);
  const [sharedCandidates, setSharedCandidates] = useState<
    ReturnType<typeof toIncludeCandidates>
  >([]);

  useEffect(() => {
    let cancelled = false;
    getSystemDef()
      .then((payload) => {
        if (cancelled) return;
        setSystemCandidates(
          toIncludeCandidates(extractIncludeCatalogFields(payload), "system"),
        );
      })
      .catch(() => {
        if (!cancelled) {
          setSystemCandidates([]);
        }
      });
    listSharedFieldGroups()
      .then(async (groups) => {
        const details = await Promise.all(
          groups.map((g) => {
            const groupName = (g.name || "").trim();
            if (!groupName) {
              return Promise.resolve(null);
            }
            return getSharedFieldGroupDetail(groupName).catch(() => null);
          }),
        );
        if (cancelled) return;
        const fields = details.flatMap((d) => extractIncludeCatalogFields(d));
        setSharedCandidates(toIncludeCandidates(fields, "shared"));
      })
      .catch(() => {
        if (!cancelled) {
          setSharedCandidates([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const catalog = origin === "system" ? systemCandidates : sharedCandidates;
  const unused = useMemo(
    () => unusedIncludeCandidates(catalog, existingFields),
    [catalog, existingFields],
  );

  async function handleInclude() {
    if (!canEdit) {
      onError(DEV_MSG.CT_INCLUDE_LOCK_REQUIRED);
      return;
    }
    const fieldType = parseIncludeFieldOrigin(origin);
    if (!fieldType) {
      onError(DEV_MSG.CT_INCLUDE_ORIGIN_INVALID);
      return;
    }
    const fieldName = name.trim();
    if (!fieldName) {
      onError(DEV_MSG.CT_INCLUDE_NAME_REQUIRED);
      return;
    }
    onBusy(true);
    onError(null);
    onNotice(null);
    try {
      const updated = await includeContentTypeField(idOrName, {
        name: fieldName,
        fieldType,
      });
      onIncluded(updated);
      setName("");
      onNotice(DEV_MSG.CT_INCLUDED);
    } catch (err: unknown) {
      if (isApiError(err) && isIncludeLockConflict(err)) {
        onLockLost();
      }
      onError(panelErrMsg(err, DEV_MSG.CT_INCLUDE_ERROR));
    } finally {
      onBusy(false);
    }
  }

  const includeDisabled = !canEdit || !name.trim();

  return (
    <section style={{ marginBottom: "16px" }} data-testid="developer-ct-include">
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_INCLUDE}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_INCLUDE_HINT}</p>
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "8px",
          alignItems: "flex-end",
        }}
      >
        <label style={{ display: "flex", flexDirection: "column", gap: "4px", minWidth: "8rem" }}>
          <span>{DEV_MSG.CT_INCLUDE_ORIGIN}</span>
          <select
            data-testid="developer-ct-include-origin"
            aria-label={DEV_MSG.CT_INCLUDE_ORIGIN}
            value={origin}
            disabled={!canEdit}
            onChange={(e) => {
              const next = parseIncludeFieldOrigin(e.target.value) ?? "system";
              setOrigin(next);
              if (next !== origin) {
                setName("");
              }
            }}
            style={inputStyle}
          >
            <option value="system">{DEV_MSG.CT_INCLUDE_ORIGIN_SYSTEM}</option>
            <option value="shared">{DEV_MSG.CT_INCLUDE_ORIGIN_SHARED}</option>
          </select>
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: "4px", minWidth: "12rem" }}>
          <span>{DEV_MSG.CT_INCLUDE_PICK}</span>
          <select
            data-testid="developer-ct-include-pick"
            aria-label={DEV_MSG.CT_INCLUDE_PICK}
            value={unused.some((c) => c.name === name) ? name : ""}
            disabled={!canEdit || unused.length === 0}
            onChange={(e) => setName(e.target.value)}
            style={inputStyle}
          >
            <option value="">{DEV_MSG.CT_INCLUDE_PICK_PLACEHOLDER}</option>
            {unused.map((c) => (
              <option key={`${c.fieldType}:${c.name}`} value={c.name}>
                {c.label ? `${c.name} (${c.label})` : c.name}
              </option>
            ))}
          </select>
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: "4px", flex: "1 1 12rem" }}>
          <span>{DEV_MSG.CT_INCLUDE_NAME}</span>
          <input
            type="text"
            data-testid="developer-ct-include-name"
            aria-label={DEV_MSG.CT_INCLUDE_NAME}
            list="developer-ct-include-options"
            placeholder={DEV_MSG.CT_INCLUDE_NAME_PLACEHOLDER}
            value={name}
            disabled={!canEdit}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                void handleInclude();
              }
            }}
            style={inputStyle}
          />
          <datalist id="developer-ct-include-options">
            {unused.map((c) => (
              <option key={`opt:${c.fieldType}:${c.name}`} value={c.name} />
            ))}
          </datalist>
        </label>
        <button
          type="button"
          data-testid="developer-ct-include-submit"
          aria-label={DEV_MSG.CT_INCLUDE_ACTION}
          disabled={includeDisabled}
          onClick={() => void handleInclude()}
          style={{
            ...smallBtnStyle,
            padding: "8px 12px",
            cursor: includeDisabled ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.CT_INCLUDE_ACTION}
        </button>
      </div>
    </section>
  );
}
