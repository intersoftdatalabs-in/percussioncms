/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Allowlisted section / query mapping for the Publishing shell.
 *
 * @see specs/990-unified-publishing-ui/contracts/deep-links.md
 */

import type { PublishSection } from "./types";

const SECTION_ALIASES: Record<string, PublishSection> = {
  sites: "sites",
  site: "sites",
  servers: "sites",
  status: "status",
  logs: "logs",
  log: "logs",
  design: "design",
  runtime: "runtime",
  editions: "runtime",
  edition: "runtime",
  // Classic / history deep-link intents (US6 / US8)
  history: "logs",
  pubhistory: "logs",
  publishinghistory: "logs",
  activejob: "status",
  jobstatus: "status",
  pubruntime: "runtime",
  publishingdesign: "design",
};

/** Known modern section ids (default landing is sites). */
export const PUBLISH_SECTIONS: PublishSection[] = [
  "sites",
  "status",
  "logs",
  "design",
  "runtime",
];

/**
 * Map a query {@code section} value to a Publishing section.
 * Unknown values default to {@code sites} (ops first / progressive disclosure).
 */
export function mapSectionParam(
  section: string | null | undefined,
): PublishSection {
  if (section == null || section === "") {
    return "sites";
  }
  const normalized = section.trim().toLowerCase();
  return SECTION_ALIASES[normalized] ?? "sites";
}

/**
 * Whether a section string is allowlisted (for JSP parity checks / tests).
 */
export function isAllowlistedSection(section: string | null | undefined): boolean {
  if (section == null || section === "") {
    return true;
  }
  return Object.prototype.hasOwnProperty.call(
    SECTION_ALIASES,
    section.trim().toLowerCase(),
  );
}

/** Safe site/server id from query — empty if missing or unsafe. */
export function mapIdParam(raw: string | null | undefined): string {
  if (raw == null) {
    return "";
  }
  const trimmed = raw.trim();
  // Allow alphanumeric, hyphen, underscore only (XSS-safe for prop pass-through).
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(trimmed)) {
    return "";
  }
  return trimmed;
}

export function knownSectionAliases(): string[] {
  return Object.keys(SECTION_ALIASES);
}

/**
 * Map classic design/runtime path fragments to modern sections (US8 deep links).
 */
export function mapClassicPublishingPath(
  path: string | null | undefined,
): PublishSection {
  if (path == null || path === "") {
    return "sites";
  }
  const p = path.toLowerCase();
  if (p.includes("/ui/publishing") || p.includes("publishingdesign")) {
    return "design";
  }
  if (p.includes("/ui/pubruntime") || p.includes("pubruntime")) {
    return "runtime";
  }
  if (p.includes("activejob") || p.includes("status")) {
    return "status";
  }
  if (p.includes("log") || p.includes("history")) {
    return "logs";
  }
  return "sites";
}
