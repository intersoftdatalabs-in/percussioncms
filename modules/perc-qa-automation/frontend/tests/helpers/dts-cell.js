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
 * Shared DTS cell helpers for the QA DTS matrix suites (#6719).
 *
 * <p>The QA DTS matrix suites resolve the DTS cell from env vars that did
 * not match what operators are given: {@code perc-devctl qa-up --with-dts}
 * prints {@code DTS_URL:} in its banner, but the matrix helpers only read
 * {@code DTS_BASE_URL}. Copying the banner value gave an all-skip run that
 * looked green. {@link dtsBaseUrl} now accepts the {@code DTS_URL} alias,
 * so both names resolve the same cell origin.</p>
 *
 * <p>{@link dtsHttpsBaseUrl} (the CONFIDENTIAL writer origin,
 * {@code DTS_HTTPS_URL} → {@code DTS_HTTPS_HOST_PORT} →
 * {@code DTS_HTTPS_PORT}, #6375) was copy-pasted across the matrix
 * helpers; this module is the single choke point. {@link
 * openDtsHttpsContext} is the shared HTTPS writer context: it probes the
 * origin, skips by name, and disposes the request context on the skip
 * path — the per-spec {@code httpsWriterContext} copies leaked their
 * {@code APIRequestContext} when they skipped on an unanswered origin
 * (the skip throws, so a bare skip after {@code newContext} never
 * disposes).</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths).</p>
 */

"use strict";

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin) or DTS_URL=(qa-up --with-dts banner alias); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the cell serves no DTS HTTPS origin (default H2 QA
 * cell and pre-#6375 cells). The CONFIDENTIAL writer paths ride the
 * published HTTPS origin from the {@code qa-up --with-dts} banner
 * ({@code DTS_HTTPS_URL}); plain-HTTP seeds 302 to the unpublished
 * HTTPS port (#6375).
 */
const SKIP_NO_DTS_HTTPS =
  "needs DTS_HTTPS_URL=(in-cell DTS HTTPS origin from the qa-up --with-dts banner) " +
  "or DTS_HTTPS_HOST_PORT=(published host port for the in-cell 8443); " +
  "plain-HTTP seeds 302 to the unpublished HTTPS port (#6375)";

/**
 * @returns {URL|null} DTS-providing cell origin, or null when the cell is
 * CMS-only (default H2 QA cell: every case skips named). Accepts the
 * {@code qa-up --with-dts} banner alias: {@code DTS_BASE_URL} wins, else
 * {@code DTS_URL}.
 */
function dtsBaseUrl() {
  const baseUrl = process.env.DTS_BASE_URL || process.env.DTS_URL || "";
  if (!baseUrl) {
    return null;
  }
  return new URL(baseUrl);
}

/**
 * In-cell DTS HTTPS origin for the CONFIDENTIAL writer paths, or null
 * when the cell publishes no HTTPS origin. Resolution mirrors the
 * freeport contract: explicit {@code DTS_HTTPS_URL} wins, else
 * {@code DTS_HTTPS_HOST_PORT} / {@code DTS_HTTPS_PORT} build
 * {@code https://127.0.0.1:<port>}. The cell TLS identity is
 * shipped/self-signed, so callers must allow the untrusted chain
 * (Playwright {@code ignoreHTTPSErrors}).
 *
 * @returns {URL|null} in-cell DTS HTTPS origin for host-side writer calls
 */
function dtsHttpsBaseUrl() {
  const explicit = process.env.DTS_HTTPS_URL || "";
  if (explicit) {
    return new URL(explicit);
  }
  const port =
    process.env.DTS_HTTPS_HOST_PORT || process.env.DTS_HTTPS_PORT || "";
  if (!port) {
    return null;
  }
  return new URL(`https://127.0.0.1:${port}`);
}

/**
 * Opens the HTTPS writer context for the CONFIDENTIAL writer paths: the
 * writers answer plain-HTTP calls with a 302 to the in-cell HTTPS port,
 * so seeding rides the published DTS HTTPS origin (see {@link
 * dtsHttpsBaseUrl}, #6375) through an untrusted-chain-allowed context —
 * the cell TLS identity is shipped/self-signed. Skips by name when the
 * cell publishes no HTTPS origin or the origin never answers (default H2
 * QA cell, pre-#6375 cell).
 *
 * <p>The returned context is owned by the caller: dispose it in a
 * {@code finally} block so a passing run leaves no request state behind.
 * The skip-on-unanswered-origin path disposes before skipping (the
 * per-spec copies leaked the context here).</p>
 *
 * @param {object} test Playwright test object (for {@code test.skip})
 * @param {object} baseRequest Playwright request fixture factory (for {@code newContext})
 * @param {{ label?: string }} [options] suite label prefixing the named skip
 * @returns {Promise<{ httpsBase: URL, httpsRequest: object }>} HTTPS origin + context
 */
async function openDtsHttpsContext(test, baseRequest, options = {}) {
  const label = options.label || "dts";
  const httpsBase = dtsHttpsBaseUrl();
  test.skip(!httpsBase, `${label}: ${SKIP_NO_DTS_HTTPS}`);
  const httpsRequest = await baseRequest.newContext({
    ignoreHTTPSErrors: true,
  });
  let answered = false;
  try {
    // maxRedirects: 0 keeps a misconfigured-HTTPS answer as a plain
    // response so the skip below engages; following the redirect would
    // throw instead of skipping.
    await httpsRequest.get(`${httpsBase.origin}/`, { maxRedirects: 0 });
    answered = true;
  } catch {
    answered = false;
  }
  if (!answered) {
    // Disposing before the skip is the leak fix: test.skip throws, so a
    // bare skip here would orphan the request context.
    await httpsRequest.dispose().catch(() => {
      // Best-effort: the skip below already names the cell gap.
    });
    test.skip(
      true,
      `${label}: ${SKIP_NO_DTS_HTTPS} (no answer at ` +
        `'${httpsBase.origin}/'; the cell predates the qa-up --with-dts HTTPS publish, #6375)`,
    );
  }
  return { httpsBase, httpsRequest };
}

module.exports = {
  SKIP_NO_DTS_BASE,
  SKIP_NO_DTS_HTTPS,
  dtsBaseUrl,
  dtsHttpsBaseUrl,
  openDtsHttpsContext,
};
