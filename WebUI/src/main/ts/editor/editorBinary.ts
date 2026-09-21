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
 * EditorHost file-field PUT error mapping (#4678). HTTP 403 / 400 / 413 are
 * not treated as a successful upload.
 */

import { isApiError } from "../api/client";

export type EditorBinaryErrorReason =
  | "forbidden"
  | "tooLarge"
  | "badRequest"
  | "failed";

export function editorBinaryErrorReason(err: unknown): EditorBinaryErrorReason {
  if (!isApiError(err)) {
    return "failed";
  }
  if (err.status === 403) {
    return "forbidden";
  }
  if (err.status === 413) {
    return "tooLarge";
  }
  if (err.status === 400) {
    return "badRequest";
  }
  return "failed";
}

const IMAGE_EXTS = new Set([
  ".png",
  ".jpg",
  ".jpeg",
  ".gif",
  ".webp",
  ".svg",
  ".tif",
  ".tiff",
  ".bmp",
]);

export function isImageFile(file: File | null | undefined): boolean {
  if (!file) {
    return false;
  }
  const type = (file.type || "").trim().toLowerCase();
  if (type.startsWith("image/")) {
    return true;
  }
  const name = (file.name || "").trim().toLowerCase();
  const dot = name.lastIndexOf(".");
  if (dot < 0) {
    return false;
  }
  return IMAGE_EXTS.has(name.slice(dot));
}
