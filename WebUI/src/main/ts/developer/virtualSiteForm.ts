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

import type { VirtualSiteProperties } from "../api/developer/types";

/** Traditional repository site (not virtual). */
export const SOURCE_KIND_REPOSITORY = "repository";

/** Phase 1 Virtual Site adapter wire name (Git/Markdown tree). */
export const SOURCE_KIND_GIT_FILESYSTEM = "git-filesystem";

/** Virtual Site adapter wire name for a CSV tree on the filesystem. */
export const SOURCE_KIND_CSV_FILESYSTEM = "csv-filesystem";

/** Form select values for source kind. */
export type VirtualSourceKindOption =
  | typeof SOURCE_KIND_REPOSITORY
  | typeof SOURCE_KIND_GIT_FILESYSTEM
  | typeof SOURCE_KIND_CSV_FILESYSTEM;

/** Editable form model for the Virtual Site source panel. */
export interface VirtualSiteFormModel {
  sourceKind: VirtualSourceKindOption;
  rootPath: string;
  remoteUrl: string;
  branch: string;
  configFile: string;
  siteKey: string;
}

/**
 * Normalize a wire/sourceKind string into a form select option.
 * Blank, missing, or {@code repository} → repository; git-filesystem and
 * csv-filesystem map to themselves; unknown kinds → repository (safe default).
 */
export function normalizeSourceKindOption(
  raw: string | null | undefined,
): VirtualSourceKindOption {
  const v = (raw ?? "").trim().toLowerCase();
  if (!v || v === SOURCE_KIND_REPOSITORY) {
    return SOURCE_KIND_REPOSITORY;
  }
  if (v === SOURCE_KIND_GIT_FILESYSTEM) {
    return SOURCE_KIND_GIT_FILESYSTEM;
  }
  if (v === SOURCE_KIND_CSV_FILESYSTEM) {
    return SOURCE_KIND_CSV_FILESYSTEM;
  }
  // Unknown kinds: surface as repository so operators do not accidentally
  // re-save an unsupported adapter without changing the select.
  return SOURCE_KIND_REPOSITORY;
}

/** True when the form (or wire) represents a Virtual Site adapter. */
export function isVirtualSourceKind(kind: string | null | undefined): boolean {
  const v = (kind ?? "").trim().toLowerCase();
  return v.length > 0 && v !== SOURCE_KIND_REPOSITORY;
}

/** True when source kind is the Git filesystem adapter (Build/Publish chrome). */
export function isGitFilesystemSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_GIT_FILESYSTEM;
}

/** True when source kind is the CSV filesystem adapter (root path only). */
export function isCsvFilesystemSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_CSV_FILESYSTEM;
}

/**
 * Map API payload → form fields. Defensive string coercion for Optional-ish
 * Jackson shapes.
 */
export function virtualPropsToForm(
  props: VirtualSiteProperties | null | undefined,
): VirtualSiteFormModel {
  return {
    sourceKind: normalizeSourceKindOption(asOptionalString(props?.sourceKind)),
    rootPath: asOptionalString(props?.rootPath) ?? "",
    remoteUrl: asOptionalString(props?.remoteUrl) ?? "",
    branch: asOptionalString(props?.branch) ?? "",
    configFile: asOptionalString(props?.configFile) ?? "",
    siteKey: asOptionalString(props?.siteKey) ?? "",
  };
}

/**
 * Build PUT body from form state.
 * Repository / blank kind clears virtual configuration on the server.
 */
export function formToVirtualProps(form: VirtualSiteFormModel): VirtualSiteProperties {
  const kind = form.sourceKind;
  if (!isVirtualSourceKind(kind)) {
    return {
      sourceKind: SOURCE_KIND_REPOSITORY,
      rootPath: null,
      configFile: null,
      siteKey: null,
    };
  }
  if (kind === SOURCE_KIND_CSV_FILESYSTEM) {
    // CSV rejects a non-blank virtual.remoteUrl (REST 400). Send empty
    // remoteUrl/branch so a prior Git remote is cleared (omit would keep it).
    return {
      sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  return {
    sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
    rootPath: form.rootPath.trim() || null,
    // Empty string (not omit) so Save can clear a stored remote on #3568;
    // omitted remoteUrl/branch would keep the previous server value.
    remoteUrl: form.remoteUrl.trim(),
    branch: form.branch.trim(),
    configFile: form.configFile.trim() || null,
    siteKey: form.siteKey.trim() || null,
  };
}

/**
 * Lightweight client-side checks aligned with PSVirtualSiteHelper (not a full
 * NIO walk — server still validates on PUT). Remote URL / branch are optional
 * and are not validated here (checkout and fail-closed URL rules live on the
 * server).
 *
 * @returns error message key fragment, or null when OK
 */
export function validateVirtualSiteForm(
  form: VirtualSiteFormModel,
): "root-required" | "root-unsafe" | "config-unsafe" | null {
  if (!isVirtualSourceKind(form.sourceKind)) {
    return null;
  }
  const root = form.rootPath.trim();
  if (!root) {
    return "root-required";
  }
  // Reject obvious path traversal in the string (server also checks after NIO normalize).
  if (root.includes("..")) {
    return "root-unsafe";
  }
  const config = form.configFile.trim();
  if (config) {
    if (config.includes("..") || config.includes("/") || config.includes("\\")) {
      return "config-unsafe";
    }
  }
  return null;
}

/** Empty form (traditional repository default). */
export function emptyVirtualSiteForm(): VirtualSiteFormModel {
  return {
    sourceKind: SOURCE_KIND_REPOSITORY,
    rootPath: "",
    remoteUrl: "",
    branch: "",
    configFile: "",
    siteKey: "",
  };
}

function asOptionalString(value: unknown): string | undefined {
  if (value == null) return undefined;
  if (typeof value === "string") return value;
  // Jackson Optional sometimes arrives already unwrapped; ignore objects.
  return undefined;
}
