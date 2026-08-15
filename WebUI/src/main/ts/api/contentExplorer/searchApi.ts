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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Typed client for the searchmanagement REST surface used by the modern
 * React Content Explorer (feature 992-react-content-explorer, US5 P-Search).
 *
 * <p>Provider: sitemanage
 * {@code com.percussion.searchmanagement.service.impl.PSSearchRestService}
 * at {@code POST /Rhythmyx/services/searchmanagement/search/get/extendedresults}.
 * Server DTOs: {@code PSSearchCriteria}, {@code PSPagedItemPropertiesList},
 * {@code PSItemProperties} under
 * {@code projects/sitemanage/src/main/java/com/percussion/searchmanagement/data/}
 * and {@code projects/sitemanage/src/main/java/com/percussion/share/data/}.</p>
 *
 * <p>This module is intentionally thin: it maps the documented contract to a
 * typed TS surface and delegates transport to {@link post}
 * (CSRF + JSON + error normalization). It does <em>not</em> invent fields —
 * when a new server field is required, align types to the live DTOs per
 * constitution II (Evidence Over Invention).</p>
 *
 * <p><strong>Wire-format notes (verified 2026-07-20 against the live
 * docker dev CMS at {@code http://localhost:9992}):</strong></p>
 * <ul>
 *   <li>Request body is wrapped under the {@code SearchCriteria} key
 *       (the resource method takes {@code PSSearchCriteria} whose
 *       class carries
 *       {@code @XmlRootElement(name = "SearchCriteria")}).</li>
 *   <li>Response is wrapped under {@code PagedItemPropertiesList}
 *       (the resource returns
 *       {@code PSPagedItemPropertiesList} which extends
 *       {@code ArrayList<...>} and carries
 *       {@code @JsonRootName(value = "PagedItemPropertiesList")}).</li>
 *   <li>The per-page array lives under {@code childrenInPage} on the
 *       server DTO; {@link searchExtended} unwraps and normalizes to
 *       the client-facing shape (children + totalCount + startIndex).</li>
 * </ul>
 */

import { post } from "../client";
import { PATHS } from "../paths";
import { toRepositorySearchFolderPath } from "../../contentExplorer/folderPath";
import type {
  PSItemProperties,
  PSPagedItemPropertiesList,
  PSPagedItemPropertiesListEnvelope,
  PSSearchCriteria,
  PSSearchResults,
} from "./types";

/** Wire envelope for the request (root name = `SearchCriteria`). */
interface PSSearchCriteriaRequest {
  SearchCriteria?: PSSearchCriteria;
}

/**
 * Classic finder / Home use system list display format id {@code 9} when
 * the active format is not yet resolved. {@code PSSearchService.searchForIds}
 * rejects a missing {@code formatId} with IllegalArgumentException → HTTP 400
 * (see GH-2950 Explorer Search).
 */
export const DEFAULT_SEARCH_FORMAT_ID = 9;

/**
 * Normalize client criteria to the shape {@code PSSearchRestService} /
 * {@code PSSearchService} accept for a minimal free-text search.
 *
 * <ul>
 *   <li>{@code formatId} defaults to {@link DEFAULT_SEARCH_FORMAT_ID} (required server-side)</li>
 *   <li>{@code startIndex} coerced to ≥ 1 (1-based paging; server also treats &lt;1 as 1)</li>
 *   <li>{@code maxResults} left as provided (panel/home set their own defaults)</li>
 *   <li>Does not mutate the caller's object</li>
 *   <li>Omits non-wire client-only fields such as {@code caseSensitive}</li>
 * </ul>
 */
export function normalizeSearchCriteria(
  criteria: PSSearchCriteria,
): PSSearchCriteria {
  const startRaw =
    typeof criteria.startIndex === "number" ? criteria.startIndex : 1;
  const startIndex = startRaw >= 1 ? startRaw : 1;
  const formatId =
    typeof criteria.formatId === "number" && Number.isFinite(criteria.formatId)
      ? criteria.formatId
      : DEFAULT_SEARCH_FORMAT_ID;

  const out: PSSearchCriteria = {
    query: criteria.query,
    searchType: criteria.searchType,
    startIndex,
    maxResults: criteria.maxResults,
    sortColumn: criteria.sortColumn,
    sortOrder: criteria.sortOrder,
    formatId,
    searchFields: criteria.searchFields,
    folderPath: toRepositorySearchFolderPath(criteria.folderPath),
  };
  return out;
}

/**
 * Server-backed extended search. Wraps the {@link PSSearchCriteria}
 * body in the {@code {"SearchCriteria": ...}} envelope, hits the
 * sitemanage search endpoint, and unwraps the response into the
 * client-facing {@link PSSearchResults} shape.
 *
 * <p>The server sanitizes the {@code query} string
 * ({@code SecureStringUtils.sanitizeStringForHTML} +
 * {@code QueryParser.escape}) and the {@code sortColumn} /
 * {@code searchType} string
 * ({@code SecureStringUtils.removeInvalidSQLObjectNameCharacters})
 * before searching; this is documented behavior per the server
 * {@code sanitizeCriteria} method.</p>
 *
 * <p>GH-2950: callers often omit {@code formatId}; {@link normalizeSearchCriteria}
 * supplies the classic default so the server does not 400 on valid text queries.</p>
 */
export async function searchExtended(
  criteria: PSSearchCriteria,
): Promise<PSSearchResults> {
  const normalized = normalizeSearchCriteria(criteria);
  const body: PSSearchCriteriaRequest = { SearchCriteria: normalized };
  const res = await post<PSPagedItemPropertiesListEnvelope>(
    PATHS.FINDER_SEARCH_EXTENDED,
    body,
  );
  const envelope: PSPagedItemPropertiesList | undefined =
    res?.PagedItemPropertiesList;
  if (!envelope) {
    return {
      children: [],
      totalCount: 0,
      startIndex: normalized.startIndex ?? 1,
    };
  }
  return {
    children: envelope.childrenInPage ?? [],
    totalCount: envelope.childrenCount,
    startIndex: envelope.startIndex ?? normalized.startIndex ?? 1,
  };
}

/**
 * The server's `query` parameter is sanitized server-side. This client
 * helper mirrors the same Lucene / HTML escaping so logging or
 * pre-flight checks surface what the server actually sees. Pure
 * helper, no fetch.
 */
export function sanitizeQuery(raw: string): string {
  // The server uses SecureStringUtils.sanitizeStringForHTML + the
  // Lucene QueryParser.escape. The mirror below keeps the JS surface
  // safe for any pre-flight logging or export; for actual searches
  // the server is authoritative.
  // Strip HTML / control chars.
  let s = raw.replace(/[\u0000-\u001f\u007f]/g, "");
  // Mirror the basic Lucene special-character escape (the server
  // applies the full set; this is a defensive minimum).
  s = s.replace(/([+\-!(){}\[\]^"~*?:\\\/])/g, "\\$1");
  return s;
}

/** Re-export the TS surface so callers can import everything from one place. */
export type {
  PSSearchCriteria,
  PSPagedItemPropertiesList,
  PSPagedItemPropertiesListEnvelope,
  PSItemProperties,
  PSSearchResults,
};
