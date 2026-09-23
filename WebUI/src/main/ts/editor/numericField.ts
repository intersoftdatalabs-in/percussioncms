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
 * Client-side numeric checks for EditorHost save (#4752).
 * Mirrors {@code PSItemEditorNumeric}: blank is allowed; integer/number are
 * whole numbers; float allows a fraction; bounds are inclusive.
 */

import type { ContentTypeControlProperty, ContentTypeFieldSummary } from "../api/developer/types";
import type { EditorWidgetKind } from "./controlKinds";

export interface EditorNumericRow {
  name: string;
  kind: EditorWidgetKind;
  value: string;
  numericInteger?: boolean;
  numericMinimum?: string;
  numericMaximum?: string;
}

export interface NumericFieldMeta {
  integer: boolean;
  minimum?: string;
  maximum?: string;
}

const INTEGER_RE = /^-?\d+$/;
const DECIMAL_RE = /^-?(?:\d+(?:\.\d+)?|\.\d+)$/;
const LONG_MIN = BigInt("-9223372036854775808");
const LONG_MAX = BigInt("9223372036854775807");

function norm(value: string | undefined | null): string {
  return (value ?? "").trim().toLowerCase();
}

function boundFromProperties(
  properties: ContentTypeControlProperty[] | undefined,
  names: readonly string[],
): string | undefined {
  if (!properties) {
    return undefined;
  }
  for (const property of properties) {
    const name = norm(property.name);
    if (!names.includes(name)) {
      continue;
    }
    const value = (property.value ?? "").trim();
    if (value && DECIMAL_RE.test(value)) {
      return value;
    }
  }
  return undefined;
}

export function numericMetaForSchema(
  schema: ContentTypeFieldSummary | undefined | null,
): NumericFieldMeta | null {
  const dataType = norm(schema?.dataType);
  const control = norm(schema?.control);
  const integer =
    dataType === "integer" ||
    dataType === "number" ||
    (control === "sys_number" && dataType !== "float");
  const isFloat = dataType === "float";
  if (!integer && !isFloat && control !== "sys_number") {
    return null;
  }
  return {
    integer: !isFloat,
    minimum: boundFromProperties(schema?.controlProperties, ["min", "minimum"]),
    maximum: boundFromProperties(schema?.controlProperties, ["max", "maximum"]),
  };
}

function compareDecimal(left: string, right: string): number {
  const [a, b] = [left, right].map((raw) => {
    const negative = raw.startsWith("-");
    const body = negative ? raw.slice(1) : raw;
    const [whole, frac = ""] = body.split(".");
    return { negative, whole: whole.replace(/^0+(?=\d)/, "") || "0", frac: frac.replace(/0+$/, "") };
  });
  if (a.negative !== b.negative) {
    return a.negative ? -1 : 1;
  }
  const sign = a.negative ? -1 : 1;
  if (a.whole.length !== b.whole.length) {
    return a.whole.length > b.whole.length ? sign : -sign;
  }
  if (a.whole !== b.whole) {
    return a.whole > b.whole ? sign : -sign;
  }
  const width = Math.max(a.frac.length, b.frac.length);
  const af = a.frac.padEnd(width, "0");
  const bf = b.frac.padEnd(width, "0");
  if (af === bf) {
    return 0;
  }
  return af > bf ? sign : -sign;
}

/** {@code invalid} is not a number; {@code range} is outside inclusive bounds. */
export function numericFieldProblem(
  value: string,
  meta: Pick<NumericFieldMeta, "integer" | "minimum" | "maximum">,
): "invalid" | "range" | null {
  const text = (value ?? "").trim();
  if (!text) {
    return null;
  }
  if (meta.integer) {
    if (!INTEGER_RE.test(text)) {
      return "invalid";
    }
    let low = meta.minimum;
    let high = meta.maximum;
    if (!low && !high) {
      try {
        const parsed = BigInt(text);
        if (parsed < LONG_MIN || parsed > LONG_MAX) {
          return "range";
        }
      } catch {
        return "invalid";
      }
      return null;
    }
    if (low && compareDecimal(text, low) < 0) {
      return "range";
    }
    if (high && compareDecimal(text, high) > 0) {
      return "range";
    }
    return null;
  }
  if (!DECIMAL_RE.test(text)) {
    return "invalid";
  }
  if (meta.minimum && compareDecimal(text, meta.minimum) < 0) {
    return "range";
  }
  if (meta.maximum && compareDecimal(text, meta.maximum) > 0) {
    return "range";
  }
  return null;
}

export function collectInvalidNumericFieldErrors(
  rows: readonly EditorNumericRow[],
  invalidMessage: string,
  rangeMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "number") {
      continue;
    }
    const problem = numericFieldProblem(row.value, {
      integer: row.numericInteger !== false,
      minimum: row.numericMinimum,
      maximum: row.numericMaximum,
    });
    if (problem === "invalid") {
      out[row.name] = invalidMessage;
    } else if (problem === "range") {
      out[row.name] = rangeMessage;
    }
  }
  return out;
}
