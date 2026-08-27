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

import { get, put } from "../client";
import { PATHS } from "../paths";
import { asJacksonArray } from "./slotLists";
import { normalizeContentTypeDesignGaps } from "./contentTypeLists";
import type { DesignGapWire } from "./designGaps";

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeFieldRuleExpressions}. */
export const CONTENT_TYPE_FIELD_RULE_EXPRESSIONS_ROOT = "ContentTypeFieldRuleExpressions";

export const FIELD_RULE_TYPE_CONDITIONAL = "conditional";
export const FIELD_RULE_TYPE_EXTENSION = "extension";
export const FIELD_RULE_TYPE_REFERENCE = "reference";

export type FieldRuleType =
  | typeof FIELD_RULE_TYPE_CONDITIONAL
  | typeof FIELD_RULE_TYPE_EXTENSION
  | typeof FIELD_RULE_TYPE_REFERENCE;

export interface ContentTypeFieldConditional {
  variable?: string;
  operator?: string;
  value?: string;
  booleanOperator?: string;
}

export interface ContentTypeFieldRuleParam {
  name?: string;
  value?: string;
}

export interface ContentTypeFieldRule {
  type?: FieldRuleType | string;
  conditionals?: ContentTypeFieldConditional[];
  extension?: string;
  name?: string;
  parameters?: ContentTypeFieldRuleParam[];
  reference?: string;
  summary?: string;
}

export interface ContentTypeFieldTranslation {
  extension?: string;
  name?: string;
  parameters?: ContentTypeFieldRuleParam[];
  condition?: string;
  maxErrorsToStop?: number;
  summary?: string;
}

export interface ContentTypeFieldRuleExpressions {
  fieldName?: string;
  validation?: ContentTypeFieldRule[];
  visibility?: ContentTypeFieldRule[];
  inputTranslation?: ContentTypeFieldTranslation[];
  outputTranslation?: ContentTypeFieldTranslation[];
  maxErrorsToStop?: number;
  errorMessage?: string;
  validationExpression?: string;
  visibilityExpression?: string;
  inputTranslationExpression?: string;
  outputTranslationExpression?: string;
  designGaps?: DesignGapWire[];
}

/** Operator-facing text for the four rule lists. */
export interface FieldRuleExpressionTexts {
  validation: string;
  visibility: string;
  inputTranslation: string;
  outputTranslation: string;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function looksLikeParam(obj: Record<string, unknown>): boolean {
  return "name" in obj || "value" in obj;
}

function looksLikeConditional(obj: Record<string, unknown>): boolean {
  return "variable" in obj || "operator" in obj || "value" in obj;
}

function looksLikeRule(obj: Record<string, unknown>): boolean {
  return (
    "type" in obj ||
    "conditionals" in obj ||
    "extension" in obj ||
    "reference" in obj ||
    "summary" in obj
  );
}

function looksLikeTranslation(obj: Record<string, unknown>): boolean {
  return "extension" in obj || "name" in obj || "parameters" in obj || "summary" in obj;
}

function normalizeParams(raw: unknown): ContentTypeFieldRuleParam[] {
  return asJacksonArray<ContentTypeFieldRuleParam>(
    raw,
    ["ContentTypeItemExitParam", "contentTypeItemExitParam", "parameters"],
    looksLikeParam,
  ).map((p) => {
    const out: ContentTypeFieldRuleParam = {};
    if (typeof p.name === "string") {
      out.name = p.name;
    }
    if (typeof p.value === "string") {
      out.value = p.value;
    }
    return out;
  });
}

function normalizeConditionals(raw: unknown): ContentTypeFieldConditional[] {
  return asJacksonArray<ContentTypeFieldConditional>(
    raw,
    ["ContentTypeFieldConditional", "contentTypeFieldConditional", "conditionals"],
    looksLikeConditional,
  ).map((c) => {
    const out: ContentTypeFieldConditional = {};
    if (typeof c.variable === "string") {
      out.variable = c.variable;
    }
    if (typeof c.operator === "string") {
      out.operator = c.operator;
    }
    if (typeof c.value === "string") {
      out.value = c.value;
    }
    if (typeof c.booleanOperator === "string") {
      out.booleanOperator = c.booleanOperator;
    }
    return out;
  });
}

function normalizeRule(raw: ContentTypeFieldRule): ContentTypeFieldRule {
  const out: ContentTypeFieldRule = {};
  if (typeof raw.type === "string") {
    out.type = raw.type;
  }
  out.conditionals = normalizeConditionals(raw.conditionals);
  if (typeof raw.extension === "string") {
    out.extension = raw.extension;
  }
  if (typeof raw.name === "string") {
    out.name = raw.name;
  }
  out.parameters = normalizeParams(raw.parameters);
  if (typeof raw.reference === "string") {
    out.reference = raw.reference;
  }
  if (typeof raw.summary === "string") {
    out.summary = raw.summary;
  }
  return out;
}

function normalizeTranslation(raw: ContentTypeFieldTranslation): ContentTypeFieldTranslation {
  const out: ContentTypeFieldTranslation = {};
  if (typeof raw.extension === "string") {
    out.extension = raw.extension;
  }
  if (typeof raw.name === "string") {
    out.name = raw.name;
  }
  out.parameters = normalizeParams(raw.parameters);
  if (typeof raw.condition === "string") {
    out.condition = raw.condition;
  }
  if (typeof raw.maxErrorsToStop === "number") {
    out.maxErrorsToStop = raw.maxErrorsToStop;
  }
  if (typeof raw.summary === "string") {
    out.summary = raw.summary;
  }
  return out;
}

export function normalizeFieldRules(raw: unknown): ContentTypeFieldRule[] {
  return asJacksonArray<ContentTypeFieldRule>(
    raw,
    ["ContentTypeFieldRule", "contentTypeFieldRule"],
    looksLikeRule,
  ).map(normalizeRule);
}

export function normalizeFieldTranslations(raw: unknown): ContentTypeFieldTranslation[] {
  return asJacksonArray<ContentTypeFieldTranslation>(
    raw,
    ["ContentTypeItemExit", "contentTypeItemExit"],
    looksLikeTranslation,
  ).map(normalizeTranslation);
}

export function emptyFieldRuleExpressions(fieldName = ""): ContentTypeFieldRuleExpressions {
  return {
    fieldName,
    validation: [],
    visibility: [],
    inputTranslation: [],
    outputTranslation: [],
  };
}

function normalizeMaxErrors(raw: unknown): number | undefined {
  if (typeof raw === "number" && Number.isFinite(raw)) {
    return raw;
  }
  if (typeof raw === "string" && raw.trim() !== "") {
    const n = Number(raw);
    if (Number.isFinite(n)) {
      return n;
    }
  }
  return undefined;
}

/**
 * Flatten GET/PUT {@code .../ruleExpressions} JSON to {@link ContentTypeFieldRuleExpressions}.
 *
 * <p>Handles WRAP_ROOT {@code ContentTypeFieldRuleExpressions}, a flat body, JAXB
 * list envelopes, singleton objects, and empty-collection beans.
 */
export function unwrapContentTypeFieldRuleExpressions(
  payload: unknown,
): ContentTypeFieldRuleExpressions {
  const root = asRecord(payload);
  if (!root) {
    return emptyFieldRuleExpressions();
  }
  const nested = asRecord(
    root[CONTENT_TYPE_FIELD_RULE_EXPRESSIONS_ROOT] ?? root.contentTypeFieldRuleExpressions,
  );
  const body = nested ?? root;
  const out: ContentTypeFieldRuleExpressions = {
    validation: normalizeFieldRules(body.validation),
    visibility: normalizeFieldRules(body.visibility),
    inputTranslation: normalizeFieldTranslations(body.inputTranslation),
    outputTranslation: normalizeFieldTranslations(body.outputTranslation),
    designGaps: normalizeContentTypeDesignGaps(body.designGaps),
  };
  if (typeof body.fieldName === "string") {
    out.fieldName = body.fieldName;
  }
  const maxErrors = normalizeMaxErrors(body.maxErrorsToStop);
  if (maxErrors != null) {
    out.maxErrorsToStop = maxErrors;
  }
  if (typeof body.errorMessage === "string") {
    out.errorMessage = body.errorMessage;
  }
  if (typeof body.validationExpression === "string") {
    out.validationExpression = body.validationExpression;
  }
  if (typeof body.visibilityExpression === "string") {
    out.visibilityExpression = body.visibilityExpression;
  }
  if (typeof body.inputTranslationExpression === "string") {
    out.inputTranslationExpression = body.inputTranslationExpression;
  }
  if (typeof body.outputTranslationExpression === "string") {
    out.outputTranslationExpression = body.outputTranslationExpression;
  }
  return out;
}

export function wrapContentTypeFieldRuleExpressionsForWire(
  body: ContentTypeFieldRuleExpressions,
): Record<string, ContentTypeFieldRuleExpressions> {
  return { [CONTENT_TYPE_FIELD_RULE_EXPRESSIONS_ROOT]: body };
}

function cloneParams(params: ContentTypeFieldRuleParam[] | undefined): ContentTypeFieldRuleParam[] {
  return (params ?? []).map((p) => ({ name: p.name, value: p.value }));
}

function cloneRule(rule: ContentTypeFieldRule): ContentTypeFieldRule {
  return {
    type: rule.type,
    conditionals: (rule.conditionals ?? []).map((c) => ({
      variable: c.variable,
      operator: c.operator,
      value: c.value,
      booleanOperator: c.booleanOperator,
    })),
    extension: rule.extension,
    name: rule.name,
    parameters: cloneParams(rule.parameters),
    reference: rule.reference,
    summary: rule.summary,
  };
}

function cloneTranslation(t: ContentTypeFieldTranslation): ContentTypeFieldTranslation {
  return {
    extension: t.extension,
    name: t.name,
    parameters: cloneParams(t.parameters),
    condition: t.condition,
    maxErrorsToStop: t.maxErrorsToStop,
    summary: t.summary,
  };
}

export function cloneFieldRuleExpressions(
  env: ContentTypeFieldRuleExpressions,
): ContentTypeFieldRuleExpressions {
  return {
    fieldName: env.fieldName,
    validation: (env.validation ?? []).map(cloneRule),
    visibility: (env.visibility ?? []).map(cloneRule),
    inputTranslation: (env.inputTranslation ?? []).map(cloneTranslation),
    outputTranslation: (env.outputTranslation ?? []).map(cloneTranslation),
    maxErrorsToStop: env.maxErrorsToStop,
    errorMessage: env.errorMessage,
    validationExpression: env.validationExpression,
    visibilityExpression: env.visibilityExpression,
    inputTranslationExpression: env.inputTranslationExpression,
    outputTranslationExpression: env.outputTranslationExpression,
    designGaps: env.designGaps ? [...env.designGaps] : undefined,
  };
}

function paramsEqual(
  a: ContentTypeFieldRuleParam[] | undefined,
  b: ContentTypeFieldRuleParam[] | undefined,
): boolean {
  const left = a ?? [];
  const right = b ?? [];
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if ((left[i].name || "") !== (right[i].name || "")) {
      return false;
    }
    if ((left[i].value || "") !== (right[i].value || "")) {
      return false;
    }
  }
  return true;
}

function conditionalsEqual(
  a: ContentTypeFieldConditional[] | undefined,
  b: ContentTypeFieldConditional[] | undefined,
): boolean {
  const left = a ?? [];
  const right = b ?? [];
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if ((left[i].variable || "") !== (right[i].variable || "")) {
      return false;
    }
    if ((left[i].operator || "") !== (right[i].operator || "")) {
      return false;
    }
    if ((left[i].value || "") !== (right[i].value || "")) {
      return false;
    }
    if ((left[i].booleanOperator || "") !== (right[i].booleanOperator || "")) {
      return false;
    }
  }
  return true;
}

function rulesEqual(
  a: ContentTypeFieldRule[] | undefined,
  b: ContentTypeFieldRule[] | undefined,
): boolean {
  const left = a ?? [];
  const right = b ?? [];
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if ((left[i].type || "") !== (right[i].type || "")) {
      return false;
    }
    if (!conditionalsEqual(left[i].conditionals, right[i].conditionals)) {
      return false;
    }
    if ((left[i].extension || "") !== (right[i].extension || "")) {
      return false;
    }
    if ((left[i].reference || "") !== (right[i].reference || "")) {
      return false;
    }
    if (!paramsEqual(left[i].parameters, right[i].parameters)) {
      return false;
    }
  }
  return true;
}

function translationsEqual(
  a: ContentTypeFieldTranslation[] | undefined,
  b: ContentTypeFieldTranslation[] | undefined,
): boolean {
  const left = a ?? [];
  const right = b ?? [];
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if ((left[i].extension || left[i].name || "") !== (right[i].extension || right[i].name || "")) {
      return false;
    }
    if (!paramsEqual(left[i].parameters, right[i].parameters)) {
      return false;
    }
  }
  return true;
}

export function fieldRuleExpressionsEqual(
  a: ContentTypeFieldRuleExpressions,
  b: ContentTypeFieldRuleExpressions,
): boolean {
  return (
    rulesEqual(a.validation, b.validation) &&
    rulesEqual(a.visibility, b.visibility) &&
    translationsEqual(a.inputTranslation, b.inputTranslation) &&
    translationsEqual(a.outputTranslation, b.outputTranslation)
  );
}

export function emptyFieldRuleExpressionTexts(): FieldRuleExpressionTexts {
  return {
    validation: "",
    visibility: "",
    inputTranslation: "",
    outputTranslation: "",
  };
}

export function fieldRuleExpressionTextsEqual(
  a: FieldRuleExpressionTexts,
  b: FieldRuleExpressionTexts,
): boolean {
  return (
    a.validation === b.validation &&
    a.visibility === b.visibility &&
    a.inputTranslation === b.inputTranslation &&
    a.outputTranslation === b.outputTranslation
  );
}

const CONDITIONAL_OPERATORS = [
  "IS NOT NULL",
  "IS NULL",
  "NOT BETWEEN",
  "BETWEEN",
  "NOT LIKE",
  "LIKE",
  "NOT IN",
  "IN",
  "<=",
  ">=",
  "<>",
  "!=",
  "=",
  "<",
  ">",
] as const;

function unquote(raw: string): string {
  const t = raw.trim();
  if (t.length >= 2) {
    const a = t.charAt(0);
    const b = t.charAt(t.length - 1);
    if ((a === '"' && b === '"') || (a === "'" && b === "'")) {
      return t.slice(1, -1);
    }
  }
  return t;
}

function quoteIfNeeded(raw: string): string {
  if (raw === "") {
    return '""';
  }
  if (/\s/.test(raw) || raw.includes("|")) {
    return `"${raw.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
  }
  return raw;
}

function firstParamValue(params: ContentTypeFieldRuleParam[] | undefined): string {
  const first = (params ?? []).find((p) => (p.value ?? "") !== "" || (p.name ?? "") !== "");
  if (!first) {
    return "";
  }
  return first.value ?? "";
}

function findConditionalOperator(line: string): { op: string; index: number } | null {
  const upper = line.toUpperCase();
  let found: { op: string; index: number } | null = null;
  for (const op of CONDITIONAL_OPERATORS) {
    const idx = op.match(/[A-Z]/) ? upper.indexOf(op) : line.indexOf(op);
    if (idx < 0) {
      continue;
    }
    if (found == null || idx < found.index || (idx === found.index && op.length > found.op.length)) {
      found = { op, index: idx };
    }
  }
  return found;
}

function parseExtensionLine(rest: string): { extension: string; param: string } {
  const pipe = rest.indexOf("|");
  if (pipe < 0) {
    return { extension: rest.trim(), param: "" };
  }
  return {
    extension: rest.slice(0, pipe).trim(),
    param: unquote(rest.slice(pipe + 1)),
  };
}

/**
 * Parse one validation/visibility line into a rule.
 *
 * <p>{@code ext:FQN} or {@code ext:FQN | param} → extension. {@code ref:name} →
 * reference. Otherwise {@code variable operator value} (operator {@code !=}
 * becomes {@code <>}).
 */
export function parseFieldRuleLine(line: string, list: "validation" | "visibility"): ContentTypeFieldRule {
  const trimmed = line.trim();
  if (!trimmed) {
    throw new Error("Empty rule line");
  }
  const lower = trimmed.toLowerCase();
  if (lower.startsWith("ext:")) {
    const { extension, param } = parseExtensionLine(trimmed.slice(4));
    if (!extension) {
      throw new Error("Extension FQN is required");
    }
    const rule: ContentTypeFieldRule = { type: FIELD_RULE_TYPE_EXTENSION, extension };
    if (param) {
      rule.parameters = [{ value: param }];
    }
    return rule;
  }
  if (lower.startsWith("ref:")) {
    if (list === "visibility") {
      throw new Error("Visibility rules cannot use type=reference");
    }
    const reference = trimmed.slice(4).trim();
    if (!reference) {
      throw new Error("Rule reference name is required");
    }
    return { type: FIELD_RULE_TYPE_REFERENCE, reference };
  }
  const found = findConditionalOperator(trimmed);
  if (!found) {
    throw new Error(`No operator in: ${trimmed}`);
  }
  const variable = trimmed.slice(0, found.index).trim();
  const valueRaw = trimmed.slice(found.index + found.op.length);
  if (!variable) {
    throw new Error("Conditional variable is required");
  }
  const operator = found.op === "!=" ? "<>" : found.op;
  const value = unquote(valueRaw);
  return {
    type: FIELD_RULE_TYPE_CONDITIONAL,
    conditionals: [{ variable, operator, value }],
  };
}

export function parseTranslationLine(line: string): ContentTypeFieldTranslation {
  const trimmed = line.trim();
  if (!trimmed) {
    throw new Error("Empty translation line");
  }
  const rest = trimmed.toLowerCase().startsWith("ext:") ? trimmed.slice(4) : trimmed;
  const { extension, param } = parseExtensionLine(rest);
  if (!extension) {
    throw new Error("Translation extension FQN is required");
  }
  const out: ContentTypeFieldTranslation = { extension };
  if (param) {
    out.parameters = [{ value: param }];
  }
  return out;
}

export function parseFieldRuleLines(
  text: string,
  list: "validation" | "visibility",
): ContentTypeFieldRule[] {
  const lines = text.split(/\r?\n/);
  const out: ContentTypeFieldRule[] = [];
  for (const line of lines) {
    if (!line.trim()) {
      continue;
    }
    out.push(parseFieldRuleLine(line, list));
  }
  return out;
}

export function parseTranslationLines(text: string): ContentTypeFieldTranslation[] {
  const lines = text.split(/\r?\n/);
  const out: ContentTypeFieldTranslation[] = [];
  for (const line of lines) {
    if (!line.trim()) {
      continue;
    }
    out.push(parseTranslationLine(line));
  }
  return out;
}

export function formatFieldRuleLine(rule: ContentTypeFieldRule): string {
  const type = (rule.type || "").toLowerCase();
  if (type === FIELD_RULE_TYPE_EXTENSION || (!type && rule.extension)) {
    const fqn = (rule.extension || rule.name || "").trim();
    const param = firstParamValue(rule.parameters);
    return param ? `ext:${fqn} | ${quoteIfNeeded(param)}` : `ext:${fqn}`;
  }
  if (type === FIELD_RULE_TYPE_REFERENCE || (!type && rule.reference)) {
    return `ref:${(rule.reference || "").trim()}`;
  }
  const first = (rule.conditionals ?? [])[0];
  if (first) {
    const variable = (first.variable || "").trim();
    const operator = (first.operator || "=").trim();
    const value = first.value ?? "";
    return `${variable} ${operator} ${quoteIfNeeded(value)}`;
  }
  return (rule.summary || "").trim();
}

export function formatTranslationLine(t: ContentTypeFieldTranslation): string {
  const fqn = (t.extension || t.name || "").trim();
  const param = firstParamValue(t.parameters);
  if (!fqn) {
    return (t.summary || "").trim();
  }
  return param ? `${fqn} | ${quoteIfNeeded(param)}` : fqn;
}

export function formatFieldRuleLines(rules: ContentTypeFieldRule[] | undefined): string {
  return (rules ?? [])
    .map(formatFieldRuleLine)
    .filter((line) => line.length > 0)
    .join("\n");
}

export function formatTranslationLines(
  list: ContentTypeFieldTranslation[] | undefined,
): string {
  return (list ?? [])
    .map(formatTranslationLine)
    .filter((line) => line.length > 0)
    .join("\n");
}

export function fieldRuleExpressionsToTexts(
  env: ContentTypeFieldRuleExpressions,
): FieldRuleExpressionTexts {
  return {
    validation: formatFieldRuleLines(env.validation),
    visibility: formatFieldRuleLines(env.visibility),
    inputTranslation: formatTranslationLines(env.inputTranslation),
    outputTranslation: formatTranslationLines(env.outputTranslation),
  };
}

/**
 * Build the four required PUT lists from operator text. Throws with a
 * field-prefixed message when a line cannot be parsed.
 */
export function textsToFieldRuleExpressions(
  fieldName: string,
  texts: FieldRuleExpressionTexts,
): ContentTypeFieldRuleExpressions {
  try {
    const validation = parseFieldRuleLines(texts.validation, "validation");
    const visibility = parseFieldRuleLines(texts.visibility, "visibility");
    const inputTranslation = parseTranslationLines(texts.inputTranslation);
    const outputTranslation = parseTranslationLines(texts.outputTranslation);
    return {
      fieldName,
      validation,
      visibility,
      inputTranslation,
      outputTranslation,
    };
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(msg);
  }
}

function toParamPayload(p: ContentTypeFieldRuleParam): ContentTypeFieldRuleParam {
  const out: ContentTypeFieldRuleParam = {};
  if (p.name) {
    out.name = p.name;
  }
  if (p.value != null && p.value !== "") {
    out.value = p.value;
  }
  return out;
}

function toRulePutPayload(rule: ContentTypeFieldRule): ContentTypeFieldRule {
  const type = (rule.type || FIELD_RULE_TYPE_CONDITIONAL).toLowerCase();
  const out: ContentTypeFieldRule = { type };
  if (type === FIELD_RULE_TYPE_EXTENSION) {
    out.extension = (rule.extension || rule.name || "").trim();
    const params = (rule.parameters ?? []).map(toParamPayload).filter((p) => p.value != null || p.name);
    if (params.length > 0) {
      out.parameters = params;
    }
    return out;
  }
  if (type === FIELD_RULE_TYPE_REFERENCE) {
    out.reference = (rule.reference || "").trim();
    return out;
  }
  out.conditionals = (rule.conditionals ?? []).map((c) => {
    const row: ContentTypeFieldConditional = {
      variable: (c.variable || "").trim(),
      operator: (c.operator || "").trim(),
    };
    if (c.value != null) {
      row.value = c.value;
    }
    if (c.booleanOperator) {
      row.booleanOperator = c.booleanOperator;
    }
    return row;
  });
  return out;
}

function toTranslationPutPayload(t: ContentTypeFieldTranslation): ContentTypeFieldTranslation {
  const out: ContentTypeFieldTranslation = {
    extension: (t.extension || t.name || "").trim(),
  };
  const params = (t.parameters ?? []).map(toParamPayload).filter((p) => p.value != null || p.name);
  if (params.length > 0) {
    out.parameters = params;
  }
  return out;
}

/** PUT body: required lists only (empty clears). */
export function toFieldRuleExpressionsPutBody(
  env: ContentTypeFieldRuleExpressions,
): ContentTypeFieldRuleExpressions {
  return {
    fieldName: env.fieldName,
    validation: (env.validation ?? []).map(toRulePutPayload),
    visibility: (env.visibility ?? []).map(toRulePutPayload),
    inputTranslation: (env.inputTranslation ?? []).map(toTranslationPutPayload),
    outputTranslation: (env.outputTranslation ?? []).map(toTranslationPutPayload),
  };
}

function ruleExpressionsPath(idOrName: string, fieldName: string): string {
  const typeKey = encodeURIComponent(idOrName);
  const fieldKey = encodeURIComponent(fieldName);
  return `${PATHS.CONTENT_TYPES}/${typeKey}/fields/${fieldKey}/ruleExpressions`;
}

/**
 * GET /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions.
 * No design lock required.
 */
export async function getContentTypeFieldRuleExpressions(
  idOrName: string,
  fieldName: string,
): Promise<ContentTypeFieldRuleExpressions> {
  const payload = await get<unknown>(ruleExpressionsPath(idOrName, fieldName));
  const unwrapped = unwrapContentTypeFieldRuleExpressions(payload);
  if (!unwrapped.fieldName) {
    unwrapped.fieldName = fieldName;
  }
  return unwrapped;
}

/**
 * PUT /services/contenttypes/{idOrName}/fields/{fieldName}/ruleExpressions.
 *
 * <p>Requires a held design-session lock. Does not acquire or release the lock.
 * HTTP 409 when unlocked or locked by another user. Full replace of the four lists.
 */
export async function replaceContentTypeFieldRuleExpressions(
  idOrName: string,
  fieldName: string,
  body: ContentTypeFieldRuleExpressions,
): Promise<ContentTypeFieldRuleExpressions> {
  const payload = await put<unknown>(
    ruleExpressionsPath(idOrName, fieldName),
    wrapContentTypeFieldRuleExpressionsForWire(toFieldRuleExpressionsPutBody(body)),
  );
  const unwrapped = unwrapContentTypeFieldRuleExpressions(payload);
  if (!unwrapped.fieldName) {
    unwrapped.fieldName = fieldName;
  }
  return unwrapped;
}
