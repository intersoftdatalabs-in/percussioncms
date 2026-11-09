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

import {
  LOCALE_LANGUAGE_PATTERN,
  normalizeLanguageString,
  unwrapLocaleDetail,
  type LocaleWriteBody,
} from "../api/developer/localesApi";
import type { LocaleDetail, LocaleFormatSummary } from "../api/developer/types";

/**
 * Per-locale fallback chain for the locale editor (#5788 / #1690).
 *
 * <p>The chain is an ordered list of locale language strings stored on the
 * locale's format profile ({@code RXLOCALEFORMAT.FALLBACKCHAIN}). A
 * fallback-chain-only locale PUT ({@code format.fallbackChain} only,
 * {@code hasFormatProfile} omitted) replaces the chain and leaves the
 * language, label, description, status, base flag, and every other format
 * field unchanged. An empty list clears the stored chain.</p>
 */

export type LocaleFallbackChainWrite = { format: { fallbackChain: string[] } };

export type LocaleFallbackChainRejection = "unchanged" | "invalid";

/**
 * Stored chain normalized to an ordered code list. JAXB JSON may unwrap a
 * one-element list to a scalar string; blanks and duplicates are dropped.
 */
export function storedLocaleFallbackChain(
  value: LocaleFormatSummary["fallbackChain"],
): string[] {
  const raw: unknown[] = Array.isArray(value) ? value : value == null ? [] : [value];
  const out: string[] = [];
  const seen = new Set<string>();
  for (const item of raw) {
    if (typeof item !== "string") {
      continue;
    }
    const code = normalizeLanguageString(item);
    if (!code || seen.has(code)) {
      continue;
    }
    seen.add(code);
    out.push(code);
  }
  return out;
}

/** True when the code is a safe REST locale key (mirrors the server pattern). */
export function isFallbackChainCode(code: string | undefined | null): boolean {
  const normalized = normalizeLanguageString(code);
  return normalized.length > 0 && LOCALE_LANGUAGE_PATTERN.test(normalized);
}

/**
 * Parses free-text chain input (comma, semicolon, or newline separated) into
 * normalized codes, or {@code "invalid"} when any entry fails the locale
 * pattern. Duplicates are dropped in first-seen order.
 */
export function parseLocaleFallbackChainInput(
  value: string | undefined | null,
): string[] | "invalid" {
  const parts = String(value ?? "").split(/[,;\r\n]+/);
  const out: string[] = [];
  const seen = new Set<string>();
  for (const part of parts) {
    const trimmed = part.trim();
    if (!trimmed) {
      continue;
    }
    const code = normalizeLanguageString(trimmed);
    if (!isFallbackChainCode(code)) {
      return "invalid";
    }
    if (!seen.has(code)) {
      seen.add(code);
      out.push(code);
    }
  }
  return out;
}

/** Display form of a stored chain (comma-separated, for the read-only row). */
export function formatLocaleFallbackChainForDisplay(chain: string[]): string {
  return (chain || []).join(", ");
}

function chainEquals(a: string[], b: string[]): boolean {
  if (a.length !== b.length) {
    return false;
  }
  return a.every((code, i) => code === b[i]);
}

/**
 * Wire encoding for an empty chain. The CXF JSON provider drops nested empty
 * arrays (a bare {@code []} arrives as omitted and would preserve the stored
 * chain), so a clear is sent as a blank-only list, which both providers keep
 * and the server normalizes back to empty. Never persisted: the server strips
 * blanks before storing.
 */
export const LOCALE_FALLBACK_CHAIN_CLEAR = [""];

function encodeFallbackChainForWire(chain: string[]): string[] {
  return chain.length > 0 ? [...chain] : [...LOCALE_FALLBACK_CHAIN_CLEAR];
}

/**
 * Body for the existing locale PUT that replaces the fallback chain.
 *
 * <p>Language string, label, description, status, base flag, and other format
 * fields are omitted. An empty list clears the stored chain (sent as
 * {@link LOCALE_FALLBACK_CHAIN_CLEAR}). A chain that matches the stored chain
 * is {@code "unchanged"} (no write); any entry that fails the locale pattern,
 * or the locale's own code in the chain, is {@code "invalid"} (no write —
 * membership against the catalog is enforced server-side with a 400).</p>
 */
export function localeFallbackChainWrite(
  baseline: Pick<LocaleDetail, "format" | "languageString">,
  nextChain: string[],
): LocaleFallbackChainWrite | LocaleFallbackChainRejection {
  const chain = storedLocaleFallbackChain(nextChain);
  for (const code of chain) {
    if (!isFallbackChainCode(code)) {
      return "invalid";
    }
  }
  const self = normalizeLanguageString(baseline.languageString);
  if (self && chain.includes(self)) {
    return "invalid";
  }
  const previous = storedLocaleFallbackChain(baseline.format?.fallbackChain);
  if (chainEquals(previous, chain)) {
    return "unchanged";
  }
  return { format: { fallbackChain: encodeFallbackChainForWire(chain) } };
}

const OMITTED_ON_FALLBACK_WRITE = [
  "languageString",
  "label",
  "description",
  "status",
  "baseLocale",
  "hasFormatProfile",
] as const;

const OMITTED_FORMAT_FIELDS = [
  "languageString",
  "textDir",
  "datePattern",
  "timePattern",
  "dateTimePattern",
  "decimalSep",
  "groupingSep",
  "currencyCode",
  "currencyPattern",
  "firstDayOfWeek",
  "measurementSystem",
  "defaultTz",
  "numberingSystem",
  "calendar",
] as const;

function storedStatus(value?: string | null): string {
  return (value ?? "").trim().toLowerCase();
}

function storedText(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

function otherFormatFingerprint(fmt?: LocaleFormatSummary | null): string {
  const src: LocaleFormatSummary = fmt || {};
  return [
    src.textDir || "",
    src.datePattern || "",
    src.timePattern || "",
    src.dateTimePattern || "",
    src.decimalSep || "",
    src.groupingSep || "",
    src.currencyCode || "",
    src.currencyPattern || "",
    src.firstDayOfWeek == null ? "" : String(src.firstDayOfWeek),
    src.measurementSystem || "",
    src.defaultTz || "",
    src.numberingSystem || "",
    src.calendar || "",
  ].join("");
}

function sentOmitsOtherFormatFields(fmt: LocaleFormatSummary): boolean {
  for (const key of OMITTED_FORMAT_FIELDS) {
    if (key in fmt) {
      return false;
    }
  }
  return true;
}

/**
 * Chain to show after a fallback-chain-only update, or null when the response
 * must not replace the previous value (wrong shape, or the save touched
 * fields outside the chain).
 */
export function savedLocaleFallbackChain(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string[] | null {
  for (const key of OMITTED_ON_FALLBACK_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  const sentFormat = sent.format as Record<string, unknown>;
  if (!("fallbackChain" in sentFormat) || !sentOmitsOtherFormatFields(sent.format)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentChain = storedLocaleFallbackChain(
    Array.isArray(sentFormat.fallbackChain)
      ? (sentFormat.fallbackChain as string[])
      : null,
  );
  const savedChain = storedLocaleFallbackChain(saved.format?.fallbackChain);
  if (!chainEquals(sentChain, savedChain)) {
    return null;
  }
  const savedLang = normalizeLanguageString(saved.languageString);
  const previousLang = normalizeLanguageString(previous.languageString);
  if (!savedLang || (previousLang && savedLang !== previousLang)) {
    return null;
  }
  if (storedText(saved.label) !== storedText(previous.label)) {
    return null;
  }
  if (storedText(saved.description) !== storedText(previous.description)) {
    return null;
  }
  const previousStatus = storedStatus(previous.status);
  if (previousStatus && storedStatus(saved.status) !== previousStatus) {
    return null;
  }
  if ((previous.baseLocale === true) !== (saved.baseLocale === true)) {
    return null;
  }
  if (otherFormatFingerprint(previous.format) !== otherFormatFingerprint(saved.format)) {
    return null;
  }
  return sentChain;
}
