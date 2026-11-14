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
import { fileFieldContainsNul, pickedFileNameContainsNul } from "../fileField";
import { imageAltFieldName } from "../imageAlt";
import { FileFieldWidget, type FileFieldWidgetProps } from "./FileFieldWidget";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";

/**
 * A NUL cannot be stored in item field XML / JDBC text (#5695 / #4532).
 * The image field carries its text (stored name / picked file name) through
 * the binary widget, so the gate reads the raw value, not the rendered name.
 * Mirrors the file-filename NUL slice (#5588); the file gate covers
 * {@code kind === "file"} only.
 */
export function imageFieldContainsNul(value: string | undefined | null): boolean {
  return fileFieldContainsNul(value);
}

/** NUL only, image rows. Stored value and picked file name are both refused. */
export function collectImageNulFieldErrors(
  rows: readonly { name: string; kind: string; value: string }[],
  pendingFiles: Record<string, File>,
  invalidMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "image") {
      continue;
    }
    const pending = pendingFiles[row.name];
    if (
      imageFieldContainsNul(row.value) ||
      pickedFileNameContainsNul(pending)
    ) {
      out[row.name] = invalidMessage;
    }
  }
  return out;
}

export interface ImageFieldWidgetProps
  extends Omit<FileFieldWidgetProps, "accept" | "preview"> {
  /** Alternative text for the image row (#6263). Persisted as `<name>_alt`. */
  altValue?: string;
  /** Alternative-text edits. Never fires for the binary pick/clear. */
  onAlt?: (value: string) => void;
}

export function ImageFieldWidget(
  props: ImageFieldWidgetProps,
): React.ReactElement {
  const { altValue = "", onAlt, ...binary } = props;
  return (
    <>
      <FileFieldWidget {...binary} accept="image/*" preview />
      <input
        className={styles.input}
        data-testid={`editor-image-alt-${binary.name}`}
        aria-label={message(EDITOR_MSG.IMAGE_ALT)}
        type="text"
        name={imageAltFieldName(binary.name)}
        value={altValue}
        disabled={binary.readOnly}
        onChange={(e) => onAlt?.(e.target.value)}
      />
    </>
  );
}
