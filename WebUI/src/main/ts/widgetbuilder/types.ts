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

export interface WidgetField {
  name: string;
  type: string;
  label?: string;
}

export interface WidgetDefinition {
  widgetId?: number | string;
  label: string;
  prefix: string;
  author: string;
  description: string;
  version: string;
  publisherUrl: string;
  responsive: boolean;
  toolTipMessage: string;
  widgetTrayCustomizedIconPath: string;
  widgetHtml: string;
  fieldsList: { fields: WidgetField[] };
  jsFileList: { resourceList: string[] };
  cssFileList: { resourceList: string[] };
}

export interface WidgetSummary {
  widgetId?: number | string;
  label?: string;
  prefix?: string;
  version?: string;
  description?: string;
  author?: string;
}

export function emptyDefinition(): WidgetDefinition {
  return {
    label: "",
    prefix: "",
    author: "",
    description: "",
    version: "1.0.0",
    publisherUrl: "",
    responsive: false,
    toolTipMessage: "",
    widgetTrayCustomizedIconPath: "",
    widgetHtml: "",
    fieldsList: { fields: [] },
    jsFileList: { resourceList: [] },
    cssFileList: { resourceList: [] },
  };
}

/** Normalize server payload (possibly wrapped) to WidgetDefinition. */
export function fromServer(raw: unknown): WidgetDefinition {
  let obj = raw as Record<string, unknown>;
  if (obj && typeof obj === "object" && "WidgetBuilderDefinitionData" in obj) {
    obj = obj.WidgetBuilderDefinitionData as Record<string, unknown>;
  }
  const base = emptyDefinition();
  if (!obj || typeof obj !== "object") {
    return base;
  }
  return {
    ...base,
    widgetId: (obj.widgetId as number) ?? undefined,
    label: String(obj.label ?? ""),
    prefix: String(obj.prefix ?? ""),
    author: String(obj.author ?? ""),
    description: String(obj.description ?? ""),
    version: String(obj.version ?? "1.0.0"),
    publisherUrl: String(obj.publisherUrl ?? ""),
    responsive: Boolean(obj.responsive),
    toolTipMessage: String(obj.toolTipMessage ?? ""),
    widgetTrayCustomizedIconPath: String(
      obj.widgetTrayCustomizedIconPath ?? "",
    ),
    widgetHtml: String(obj.widgetHtml ?? ""),
    fieldsList: normalizeFields(obj.fieldsList),
    jsFileList: normalizeResources(obj.jsFileList),
    cssFileList: normalizeResources(obj.cssFileList),
  };
}

export function toServerPayload(def: WidgetDefinition): unknown {
  const data: Record<string, unknown> = {
    prefix: def.prefix,
    author: def.author,
    label: def.label,
    publisherUrl: def.publisherUrl,
    description: def.description,
    version: def.version,
    responsive: def.responsive,
    widgetTrayCustomizedIconPath: def.widgetTrayCustomizedIconPath,
    toolTipMessage: def.toolTipMessage,
    widgetHtml: def.widgetHtml,
    fieldsList: def.fieldsList,
    jsFileList: def.jsFileList,
    cssFileList: def.cssFileList,
  };
  if (def.widgetId != null && def.widgetId !== "" && Number(def.widgetId) > 0) {
    data.widgetId = Number(def.widgetId);
  }
  return { WidgetBuilderDefinitionData: data };
}

function normalizeFields(raw: unknown): { fields: WidgetField[] } {
  if (raw && typeof raw === "object" && "fields" in (raw as object)) {
    const fields = (raw as { fields: unknown }).fields;
    if (Array.isArray(fields)) {
      return { fields: fields as WidgetField[] };
    }
  }
  return { fields: [] };
}

function normalizeResources(raw: unknown): { resourceList: string[] } {
  if (raw && typeof raw === "object" && "resourceList" in (raw as object)) {
    const list = (raw as { resourceList: unknown }).resourceList;
    if (Array.isArray(list)) {
      return { resourceList: list.map(String) };
    }
  }
  return { resourceList: [] };
}

export function extractValidationMessages(result: unknown): string[] {
  if (!result || typeof result !== "object") {
    return [];
  }
  const r = result as Record<string, unknown>;
  const nested =
    (r.WidgetBuilderValidationResults as Record<string, unknown>) || r;
  const results = nested.results ?? nested.validationResults ?? nested.errors;
  if (Array.isArray(results)) {
    return results.map((item) => {
      if (typeof item === "string") {
        return item;
      }
      if (item && typeof item === "object") {
        const o = item as Record<string, unknown>;
        return String(o.message ?? o.name ?? JSON.stringify(o));
      }
      return String(item);
    });
  }
  if (typeof nested.message === "string") {
    return [nested.message];
  }
  return [];
}
