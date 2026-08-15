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
 * Assembly preview-location client for Explorer template preview.
 *
 * <p>Server: {@code GET /services/assembly/preview-location} ({@code rest}
 * {@code AssemblyResource}). Replaces Data Flow {@code previewslotvariant}
 * HTML redirects.</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";

export interface PreviewLocation {
  previewUrl: string;
  contentId: number;
  templateId: number;
  revision: number;
}

function unwrapPreviewLocation(payload: unknown): PreviewLocation | null {
  if (payload == null || typeof payload !== "object") {
    return null;
  }
  const obj = payload as Record<string, unknown>;
  const inner =
    obj.PreviewLocation && typeof obj.PreviewLocation === "object"
      ? (obj.PreviewLocation as Record<string, unknown>)
      : obj;
  const previewUrl = inner.previewUrl ?? inner.PreviewUrl;
  const contentId = inner.contentId ?? inner.ContentId;
  const templateId = inner.templateId ?? inner.TemplateId;
  const revision = inner.revision ?? inner.Revision;
  if (typeof previewUrl !== "string" || !previewUrl.trim()) {
    return null;
  }
  return {
    previewUrl: previewUrl.trim(),
    contentId: Number(contentId) || 0,
    templateId: Number(templateId) || 0,
    revision: Number(revision) || 0,
  };
}

export async function fetchPreviewLocation(
  contentId: number,
  templateId: number,
  revision?: number,
): Promise<PreviewLocation> {
  const q = new URLSearchParams();
  q.set("contentId", String(contentId));
  q.set("templateId", String(templateId));
  if (revision != null && revision > 0) {
    q.set("revision", String(revision));
  }
  const res = await get<unknown>(`${PATHS.ASSEMBLY_PREVIEW_LOCATION}?${q.toString()}`);
  const loc = unwrapPreviewLocation(res);
  if (loc == null) {
    throw new Error("Preview location was empty");
  }
  return loc;
}

export interface AssemblyOperationResult {
  ok: boolean;
  message: string;
}

function unwrapOperationResult(payload: unknown): AssemblyOperationResult {
  if (payload == null || typeof payload !== "object") {
    return { ok: true, message: "" };
  }
  const obj = payload as Record<string, unknown>;
  const inner =
    obj.AssemblyOperationResult && typeof obj.AssemblyOperationResult === "object"
      ? (obj.AssemblyOperationResult as Record<string, unknown>)
      : obj;
  return {
    ok: inner.ok !== false && inner.Ok !== false,
    message: String(inner.message ?? inner.Message ?? ""),
  };
}

export async function flushAssemblerCache(): Promise<AssemblyOperationResult> {
  const res = await post<unknown>(PATHS.ASSEMBLY_FLUSH_CACHE);
  return unwrapOperationResult(res);
}

export async function resetNavigation(): Promise<AssemblyOperationResult> {
  const res = await post<unknown>(PATHS.ASSEMBLY_NAV_RESET);
  return unwrapOperationResult(res);
}
