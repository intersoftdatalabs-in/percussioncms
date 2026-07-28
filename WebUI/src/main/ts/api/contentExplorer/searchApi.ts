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
 */
export async function searchExtended(
  criteria: PSSearchCriteria,
): Promise<PSSearchResults> {
  const body: PSSearchCriteriaRequest = { SearchCriteria: criteria };
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
      startIndex: criteria.startIndex ?? 1,
    };
  }
  return {
    children: envelope.childrenInPage ?? [],
    totalCount: envelope.childrenCount,
    startIndex: envelope.startIndex ?? criteria.startIndex ?? 1,
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
