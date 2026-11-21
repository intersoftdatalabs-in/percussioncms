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

/**
 * Blob preview URL for a freshly picked file, or null when preview is off,
 * the pick is not an image, or object URLs are unavailable. An unsafe (non
 * blob:) object URL is revoked before returning null so no tainted value
 * survives. Behavior-preserving extraction from {@code FileFieldWidget}'s
 * change handler (#6960: keeps the component under the cognitive budget).
 */
export function previewUrlForPick(preview: boolean, file: File): string | null {
  if (!preview || !file.type.startsWith("image/")) {
    return null;
  }
  if (typeof URL === "undefined" || typeof URL.createObjectURL !== "function") {
    return null;
  }
  const objectUrl = URL.createObjectURL(file);
  if (blobPreviewSrc(objectUrl)) {
    return objectUrl;
  }
  URL.revokeObjectURL(objectUrl);
  return null;
}

export interface FileInputChangeRequest {
  file: File | null;
  preview: boolean;
  previousPreviewUrl: string | null;
  storedHasBinary: boolean;
  cleared: boolean;
  storedName: string;
  onFile: (file: File | null) => void;
  setFilename: (value: string) => void;
  setPicked: (value: boolean) => void;
  setCleared: (value: boolean) => void;
  setPreviewUrl: (value: string | null) => void;
}

/**
 * File-input change handling for {@code FileFieldWidget}. Revokes the prior
 * preview, records a pick, or restores the stored name on dialog cancel.
 */
export function applyFileInputChange(request: FileInputChangeRequest): void {
  if (request.previousPreviewUrl) {
    URL.revokeObjectURL(request.previousPreviewUrl);
    request.setPreviewUrl(null);
  }
  if (!request.file) {
    request.setPicked(false);
    request.setFilename(
      request.storedHasBinary && !request.cleared ? request.storedName : "",
    );
    request.onFile(null);
    return;
  }
  request.setCleared(false);
  request.setPicked(true);
  request.setFilename(displayBinaryFileName(request.file.name));
  request.onFile(request.file);
  const previewUrl = previewUrlForPick(request.preview, request.file);
  if (previewUrl) {
    request.setPreviewUrl(previewUrl);
  }
}

export interface StoredDownloadRequest {
  itemId: string;
  field: string;
  storedName: string;
  filename: string;
  present: boolean;
  downloadBinary: (
    itemId: string,
    field: string,
    storedName: string,
  ) => Promise<ItemEditorBinaryDownload>;
  setDownloadError: (value: string) => void;
}

/**
 * Download-button handling for {@code FileFieldWidget}. Downloads stored
 * bytes only — never an unsaved local pick.
 */
export function requestStoredDownload(request: StoredDownloadRequest): void {
  request.setDownloadError("");
  if (!request.present) {
    request.setDownloadError(
      `${message(EDITOR_MSG.FILE_DOWNLOAD_EMPTY)} (${request.field})`,
    );
    return;
  }
  void request
    .downloadBinary(request.itemId, request.field, request.storedName)
    .then((result) => {
      if (
        !saveBinaryDownload(result.bytes, result.filename || request.filename)
      ) {
        request.setDownloadError(
          `${message(EDITOR_MSG.FILE_DOWNLOAD_EMPTY)} (${request.field})`,
        );
      }
    })
    .catch((err: { status?: number }) => {
      request.setDownloadError(
        downloadErrorMessage(request.field, err?.status),
      );
    });
}

export interface ClearFileFieldRequest {
  input: HTMLInputElement | null;
  previewUrl: string | null;
  hadStored: boolean;
  onFile: (file: File | null) => void;
  onClear?: (hadStored: boolean) => void;
  setFilename: (value: string) => void;
  setStoredName: (value: string) => void;
  setPresent: (value: boolean) => void;
  setPicked: (value: boolean) => void;
  setCleared: (value: boolean) => void;
  setDownloadError: (value: string) => void;
  setPreviewUrl: (value: string | null) => void;
}

/**
 * Clear-button handling for {@code FileFieldWidget}. Local clear only;
 * the server is not called until the host saves.
 */
export function clearFileField(request: ClearFileFieldRequest): void {
  if (request.previewUrl) {
    URL.revokeObjectURL(request.previewUrl);
    request.setPreviewUrl(null);
  }
  if (request.input) {
    request.input.value = "";
  }
  request.setFilename("");
  request.setStoredName("");
  request.setPresent(false);
  request.setPicked(false);
  request.setCleared(true);
  request.setDownloadError("");
  request.onFile(null);
  request.onClear?.(request.hadStored);
}

export interface FileFieldWidgetProps {
  itemId: string;
  name: string;
  readOnly: boolean;
  invalid?: boolean;
  /**
   * DOM id of the row's inline error text (#6933). Set only while the row
   * carries an error so {@code aria-describedby} never dangles.
   */
  describedById?: string;
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
  /**
   * Local clear. {@code hadStored} is true only when a saved binary is being
   * marked for removal. The server is not called until the host saves.
   */
  onClear?: (hadStored: boolean) => void;
}

export function FileFieldWidget({
  itemId,
  name,
  readOnly,
  invalid,
  describedById,
  accept,
  preview = false,
  loadMeta = fetchItemEditorBinary,
  downloadBinary = downloadItemEditorBinary,
  onFile,
  onClear,
}: FileFieldWidgetProps): React.ReactElement {
  const [filename, setFilename] = useState("");
  const [storedName, setStoredName] = useState("");
  const [present, setPresent] = useState(false);
  const [picked, setPicked] = useState(false);
  const [cleared, setCleared] = useState(false);
  const [ready, setReady] = useState(false);
  const [downloadError, setDownloadError] = useState("");
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const nameEl = useRef<HTMLSpanElement>(null);
  const inputEl = useRef<HTMLInputElement>(null);
  const storedPresent = useRef(false);

  useEffect(() => {
    let cancelled = false;
    void loadMeta(itemId, name)
      .then((meta) => {
        if (!cancelled) {
          const hasStored = Boolean(meta.present);
          storedPresent.current = hasStored;
          setFilename(displayBinaryFileName(meta.filename));
          setStoredName(displayBinaryFileName(meta.filename));
          setPresent(hasStored);
          setPicked(false);
          setCleared(false);
          setReady(true);
        }
      })
      .catch(() => {
        if (!cancelled) {
          storedPresent.current = false;
          setFilename("");
          setStoredName("");
          setPresent(false);
          setPicked(false);
          setCleared(false);
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
  const showClear = !readOnly && ((present && !cleared) || picked);
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
        ref={inputEl}
        className={styles.fileInput}
        data-testid={`editor-file-${name}`}
        type="file"
        name={name}
        accept={accept}
        disabled={readOnly}
        aria-label={message(preview ? EDITOR_MSG.IMAGE_CHOOSE : EDITOR_MSG.FILE_CHOOSE)}
        aria-invalid={invalid ? true : undefined}
        aria-describedby={describedById}
        onChange={(e) => {
          applyFileInputChange({
            file: e.target.files?.[0] ?? null,
            preview,
            previousPreviewUrl: previewUrl,
            storedHasBinary: storedPresent.current,
            cleared,
            storedName,
            onFile,
            setFilename,
            setPicked,
            setCleared,
            setPreviewUrl,
          });
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
          requestStoredDownload({
            itemId,
            field: name,
            storedName,
            filename,
            present,
            downloadBinary,
            setDownloadError,
          });
        }}
      >
        {message(EDITOR_MSG.FILE_DOWNLOAD)}
      </button>
      {showClear ? (
        <button
          type="button"
          className={styles.fileClear}
          data-testid={`editor-file-clear-${name}`}
          aria-label={message(EDITOR_MSG.FILE_CLEAR)}
          onClick={() => {
            clearFileField({
              input: inputEl.current,
              previewUrl,
              hadStored: storedPresent.current && !cleared,
              onFile,
              onClear,
              setFilename,
              setStoredName,
              setPresent,
              setPicked,
              setCleared,
              setDownloadError,
              setPreviewUrl,
            });
          }}
        >
          {message(EDITOR_MSG.FILE_CLEAR)}
        </button>
      ) : null}
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
