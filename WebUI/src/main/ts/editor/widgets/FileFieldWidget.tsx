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

import React, { useEffect, useRef, useState } from "react";
import { message } from "../../i18n/message";
import {
  downloadItemEditorBinary,
  fetchItemEditorBinary,
  type ItemEditorBinaryDownload,
  type ItemEditorBinaryMeta,
} from "../itemBinaryApi";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";

/**
 * Display-only filename. Strips HTML/control metacharacters so a crafted
 * {@code File.name} or stored binary name cannot be reinterpreted as HTML.
 */
export function displayBinaryFileName(raw: string | null | undefined): string {
  return String(raw ?? "").replace(/[\u0000-\u001F<>&"'`]/g, "");
}

/**
 * Image preview {@code src} must be a blob: object URL from
 * {@code URL.createObjectURL}. Rejects javascript:/data:/http(s) values so a
 * tainted File cannot be reinterpreted as HTML.
 */
export function blobPreviewSrc(url: string | null | undefined): string {
  const value = String(url ?? "");
  return value.startsWith("blob:") ? value : "";
}

/**
 * Starts a browser download of stored bytes. Empty payloads return false and
 * do not create a file (a missing binary is an error, not a blank download).
 */
export function saveBinaryDownload(
  bytes: Uint8Array<ArrayBuffer> | null | undefined,
  filename: string,
): boolean {
  if (!bytes || bytes.length === 0 || typeof document === "undefined") {
    return false;
  }
  const name = displayBinaryFileName(filename).trim() || "download.bin";
  const blob = new Blob([bytes]);
  let href = "";
  let objectUrl: string | null = null;
  if (typeof URL !== "undefined" && typeof URL.createObjectURL === "function") {
    objectUrl = URL.createObjectURL(blob);
    href = objectUrl;
  }
  if (!href) {
    return false;
  }
  const a = document.createElement("a");
  a.href = href;
  a.download = name;
  a.rel = "noopener";
  a.style.display = "none";
  document.body.appendChild(a);
  a.click();
  a.remove();
  if (objectUrl && typeof URL.revokeObjectURL === "function") {
    const toRevoke = objectUrl;
    globalThis.setTimeout(() => {
      URL.revokeObjectURL(toRevoke);
    }, 1000);
  }
  return true;
}

function downloadErrorMessage(field: string, status: number | undefined): string {
  const named = ` (${field})`;
  if (status === 403) {
    return message(EDITOR_MSG.FILE_DOWNLOAD_FORBIDDEN) + named;
  }
  if (status === 404) {
    return message(EDITOR_MSG.FILE_DOWNLOAD_NOT_FOUND) + named;
  }
  return message(EDITOR_MSG.FILE_DOWNLOAD_FAILED) + named;
}

export interface FileFieldWidgetProps {
  itemId: string;
  name: string;
  readOnly: boolean;
  accept?: string;
  preview?: boolean;
  loadMeta?: (itemId: string, field: string) => Promise<ItemEditorBinaryMeta>;
  /** Stored bytes only. Must not return an unsaved local pick. */
  downloadBinary?: (
    itemId: string,
    field: string,
    storedName: string,
  ) => Promise<ItemEditorBinaryDownload>;
  onFile: (file: File | null) => void;
}

export function FileFieldWidget({
  itemId,
  name,
  readOnly,
  accept,
  preview = false,
  loadMeta = fetchItemEditorBinary,
  downloadBinary = downloadItemEditorBinary,
  onFile,
}: FileFieldWidgetProps): React.ReactElement {
  const [filename, setFilename] = useState("");
  const [storedName, setStoredName] = useState("");
  const [present, setPresent] = useState(false);
  const [ready, setReady] = useState(false);
  const [downloadError, setDownloadError] = useState("");
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const nameEl = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    let cancelled = false;
    void loadMeta(itemId, name)
      .then((meta) => {
        if (!cancelled) {
          setFilename(displayBinaryFileName(meta.filename));
          setStoredName(displayBinaryFileName(meta.filename));
          setPresent(Boolean(meta.present));
          setReady(true);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setFilename("");
          setStoredName("");
          setPresent(false);
          setReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [itemId, name, loadMeta]);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const nameLabel =
    filename || message(preview ? EDITOR_MSG.IMAGE_NONE : EDITOR_MSG.FILE_NONE);
  useEffect(() => {
    const el = nameEl.current;
    if (el) {
      // textContent, not JSX/HTML — closes js/xss-through-dom on File.name
      el.textContent = nameLabel; // codeql[js/xss-through-dom]
    }
  }, [nameLabel]);

  return (
    <div className={styles.binary} data-testid={`editor-field-${name}`} data-editor-kind={preview ? "image" : "file"}>
      <input
        className={styles.fileInput}
        data-testid={`editor-file-${name}`}
        type="file"
        name={name}
        accept={accept}
        disabled={readOnly}
        aria-label={message(preview ? EDITOR_MSG.IMAGE_CHOOSE : EDITOR_MSG.FILE_CHOOSE)}
        onChange={(e) => {
          const file = e.target.files?.[0] ?? null;
          if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            setPreviewUrl(null);
          }
          if (file) {
            setFilename(displayBinaryFileName(file.name));
            onFile(file);
            if (
              preview &&
              file.type.startsWith("image/") &&
              typeof URL !== "undefined" &&
              typeof URL.createObjectURL === "function"
            ) {
              const objectUrl = URL.createObjectURL(file);
              const safe = blobPreviewSrc(objectUrl);
              if (safe) {
                setPreviewUrl(safe);
              } else {
                URL.revokeObjectURL(objectUrl);
              }
            }
          } else {
            onFile(null);
          }
        }}
      />
      <span
        ref={nameEl}
        className={styles.fileName}
        data-testid={`editor-file-name-${name}`}
      />
      {blobPreviewSrc(previewUrl) ? (
        <img
          className={styles.imagePreview}
          data-testid={`editor-image-preview-${name}`}
          src={blobPreviewSrc(previewUrl)} // codeql[js/xss-through-dom]
          alt=""
        />
      ) : null}
      <button
        type="button"
        className={styles.fileDownload}
        data-testid={`editor-file-download-${name}`}
        disabled={!ready}
        aria-label={message(EDITOR_MSG.FILE_DOWNLOAD)}
        onClick={() => {
          setDownloadError("");
          if (!present) {
            setDownloadError(
              `${message(EDITOR_MSG.FILE_DOWNLOAD_EMPTY)} (${name})`,
            );
            return;
          }
          void downloadBinary(itemId, name, storedName)
            .then((result) => {
              if (!saveBinaryDownload(result.bytes, result.filename || filename)) {
                setDownloadError(
                  `${message(EDITOR_MSG.FILE_DOWNLOAD_EMPTY)} (${name})`,
                );
              }
            })
            .catch((err: { status?: number }) => {
              setDownloadError(downloadErrorMessage(name, err?.status));
            });
        }}
      >
        {message(EDITOR_MSG.FILE_DOWNLOAD)}
      </button>
      {downloadError ? (
        <span
          className={styles.fieldError}
          role="alert"
          data-testid={`editor-file-download-error-${name}`}
        >
          {downloadError}
        </span>
      ) : null}
    </div>
  );
}
