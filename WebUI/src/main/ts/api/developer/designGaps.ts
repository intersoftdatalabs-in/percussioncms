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
 * Structured design capability gap (REST-GAPS-01).
 * Wire shape on ContentType / Template / Slot detail: `{ code, message }`.
 */
export interface DesignGap {
  code?: string;
  message?: string;
}

/**
 * Accepts structured objects (new wire) or legacy free-text strings (other detail peers
 * still emit string arrays until migrated).
 */
export type DesignGapWire = DesignGap | string;

/** Prefer human message; fall back to code or raw string. */
export function formatDesignGap(gap: DesignGapWire | null | undefined): string {
  if (gap == null) {
    return "";
  }
  if (typeof gap === "string") {
    return gap;
  }
  const message = (gap.message ?? "").trim();
  if (message) {
    return message;
  }
  return (gap.code ?? "").trim();
}

/** Stable list key: code → message → legacy string → index. */
export function designGapKey(gap: DesignGapWire | null | undefined, index: number): string {
  if (gap == null) {
    return String(index);
  }
  if (typeof gap === "string") {
    return gap || String(index);
  }
  const code = (gap.code ?? "").trim();
  if (code) {
    return code;
  }
  const message = (gap.message ?? "").trim();
  return message || String(index);
}

/** Optional machine code for data attributes / grouping (empty for legacy strings). */
export function designGapCode(gap: DesignGapWire | null | undefined): string | undefined {
  if (gap == null || typeof gap === "string") {
    return undefined;
  }
  const code = (gap.code ?? "").trim();
  return code || undefined;
}
