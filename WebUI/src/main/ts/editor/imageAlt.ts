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
 * Alternative-text companion for editor image fields (#6263 / #4532).
 *
 * <p>Image rows carry binaries outside the fields PUT, so alternative text is
 * edited on the image row and persisted as a sibling text entry named
 * {@code <imageFieldName>_alt} through the existing fields PUT path. The
 * server stores the entry when the content type defines the sibling field
 * and ignores it otherwise, so no new REST surface is required. The
 * required-alt gate applies only to content types that define the sibling:
 * image fields without an alt slot keep their existing save behavior. Pure
 * so Vitest can cover it without mounting the host.
 */

import type { EditorWidgetKind } from "./controlKinds";

/** Sibling suffix that carries an image field's alternative text. */
export const IMAGE_ALT_SUFFIX = "_alt";

/** Sibling field name that carries {@code imageFieldName}'s alternative text. */
export function imageAltFieldName(imageFieldName: string): string {
  return `${imageFieldName}${IMAGE_ALT_SUFFIX}`;
}

/**
 * Image field names whose loaded payload defines the {@code <name>_alt}
 * sibling entry — i.e. content types that store alternative text. The
 * required-alt gate applies only to these: image fields without a sibling
 * slot keep their existing save behavior (required-binary and NUL gates).
 */
export function managedImageAltNames(
  imageFieldNames: readonly string[],
  storedFields: readonly { name: string; value: string }[] | undefined,
): string[] {
  if (!storedFields || storedFields.length === 0) {
    return [];
  }
  const stored = new Set(
    storedFields.map((field) => field.name.trim().toLowerCase()),
  );
  return imageFieldNames.filter((name) =>
    stored.has(imageAltFieldName(name).toLowerCase()),
  );
}

export interface EditorImageRow {
  name: string;
  kind: EditorWidgetKind;
  required: boolean;
}

export interface AltTextEntry {
  name: string;
  value: string;
}

/**
 * Stored alternative text for an image field: the {@code <name>_alt} payload
 * entry when the content type defines it, otherwise blank. Never reads the
 * binary file name — the gate must read the alt slot, not the rendered name.
 */
export function imageAltStoredValue(
  fields: readonly { name: string; value: string }[] | undefined,
  imageFieldName: string,
): string {
  if (!fields) {
    return "";
  }
  const altName = imageAltFieldName(imageFieldName).toLowerCase();
  for (const field of fields) {
    if (field.name.trim().toLowerCase() === altName) {
      return String(field.value ?? "");
    }
  }
  return "";
}

/**
 * Current alternative text for an image field: the unsaved draft entry wins
 * over the stored sibling value so a refused save keeps what was typed.
 */
export function imageAltCurrentValue(
  draft: Readonly<Record<string, string>>,
  storedFields: readonly { name: string; value: string }[] | undefined,
  imageFieldName: string,
): string {
  const altName = imageAltFieldName(imageFieldName);
  if (Object.prototype.hasOwnProperty.call(draft, altName)) {
    return String(draft[altName] ?? "");
  }
  return imageAltStoredValue(storedFields, imageFieldName);
}

/**
 * True when the alternative text for an image field differs from storage.
 * Drives the row dirty marker: the sibling text row is hidden behind the
 * image row, so the image row carries the marker instead.
 */
export function imageAltIsDirty(
  storedFields: readonly { name: string; value: string }[] | undefined,
  draft: Readonly<Record<string, string>>,
  imageFieldName: string,
): boolean {
  const altName = imageAltFieldName(imageFieldName);
  if (!Object.prototype.hasOwnProperty.call(draft, altName)) {
    return false;
  }
  return (
    String(draft[altName] ?? "") !== imageAltStoredValue(storedFields, imageFieldName)
  );
}

/**
 * Required-image alternative-text errors, keyed by the image field so the
 * entry lands on the image row in the field-error summary (#5680). A blank or
 * whitespace-only alt on a required image refuses the save before the PUT;
 * optional images and non-image rows are unchanged.
 */
export function collectImageAltRequiredFieldErrors(
  rows: readonly EditorImageRow[],
  altByImage: Readonly<Record<string, string>>,
  requiredMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "image" || !row.required) {
      continue;
    }
    if (!(altByImage[row.name] ?? "").trim()) {
      out[row.name] = requiredMessage;
    }
  }
  return out;
}

/**
 * Alternative-text entries for the fields PUT: one {@code <name>_alt} entry
 * per image row carrying the current alt. The existing fields path stores the
 * entry when the content type defines the sibling and ignores it otherwise.
 */
export function imageAltSaveEntries(
  imageFieldNames: readonly string[],
  altByImage: Readonly<Record<string, string>>,
): AltTextEntry[] {
  return imageFieldNames.map((name) => ({
    name: imageAltFieldName(name),
    value: altByImage[name] ?? "",
  }));
}
