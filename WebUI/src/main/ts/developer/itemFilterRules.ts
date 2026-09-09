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

import { coerceRules } from "../api/developer/itemFiltersApi";
import type { ItemFilterRule, ItemFilterRuleParam } from "../api/developer/types";

export { coerceRules };

/** Clone GET rule rows into editable draft state. */
export function cloneRules(rules: ItemFilterRule[] | unknown | undefined | null): ItemFilterRule[] {
  return coerceRules(rules).map((r) => {
    const paramsRaw = (r as ItemFilterRule).params as unknown;
    let params: ItemFilterRuleParam[] = [];
    if (Array.isArray(paramsRaw)) {
      params = paramsRaw.map((p) => ({ name: p.name ?? "", value: p.value ?? "" }));
    } else if (paramsRaw != null && typeof paramsRaw === "object") {
      const p = paramsRaw as ItemFilterRuleParam;
      params = [{ name: p.name ?? "", value: p.value ?? "" }];
    }
    return {
      name: r.name ?? "",
      ruleId: r.ruleId,
      params,
    };
  });
}

/** Drop blank param rows; keep param values that are empty string. */
export function sanitizeRuleParams(params: ItemFilterRuleParam[] | undefined | null): ItemFilterRuleParam[] {
  if (!Array.isArray(params)) {
    return [];
  }
  return params
    .map((p) => ({
      name: (p.name ?? "").trim(),
      value: p.value == null ? "" : String(p.value),
    }))
    .filter((p) => p.name.length > 0);
}

/**
 * Wire shape for POST/PUT {@code rules[]}. Blank rule names are kept so the
 * server can return 400 ("rule name is required") rather than silently dropping.
 */
export function toWireRules(rules: ItemFilterRule[]): ItemFilterRule[] {
  return rules.map((r) => ({
    name: (r.name ?? "").trim(),
    params: sanitizeRuleParams(r.params),
  }));
}

/** Stable compare key for dirty detection (order-sensitive). */
export function rulesFingerprint(rules: ItemFilterRule[] | undefined | null): string {
  return JSON.stringify(toWireRules(cloneRules(rules)));
}

export function emptyRule(): ItemFilterRule {
  return { name: "", params: [] };
}

export function emptyParam(): ItemFilterRuleParam {
  return { name: "", value: "" };
}
