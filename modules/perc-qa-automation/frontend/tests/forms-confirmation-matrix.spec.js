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
 * US3 forms confirmation matrix (#6482 / parent #6238): submit a form
 * against the in-cell DTS forms service and prove the confirmation is
 * retrievable — the visitor submit takes the success redirect, and the
 * admin retrieval surface (summaries count + CSV export) reflects the
 * submitted entry.
 *
 * <p>Tags: {@code @dts} plus {@code @dts-forms}.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/forms-confirmation-matrix.js} (closed inventory, 3
 * cases). {@code afterAll} assembles the one results-matrix row, validates
 * it against {@code contracts/results-matrix.schema.json}, and writes it
 * to {@code test-results/forms-confirmation-matrix.json}. Three-level skip
 * contract: without {@code DTS_BASE_URL} every case skips naming the cell
 * (#6237, green on the CMS-only cell); with a base URL each case probes
 * liveness ({@code HEAD <context>/forms/csrf} — the service lives under its
 * Tomcat deployment context, issue #6342). The visitor submit runs over
 * plain HTTP (open surface). The two retrieval cases run over the
 * published DTS HTTPS origin ({@code DTS_HTTPS_URL} from the
 * {@code qa-up --with-dts} banner, #6375/#6599) with the provisioned
 * {@code deliverymanager} credential, because the admin surface is
 * {@code CONFIDENTIAL}-gated and plain-HTTP callers only see its 302
 * redirect; a gated answer over HTTPS (401/403/302 — missing credential
 * or unreachable HTTPS) still skips by name instead of failing.
 * Submissions are self-seeded under a unique {@code qaconfirm-<stamp>}
 * form name, so no CMS fixture is needed.</p>
 *
 * <p>Run (needs the DTS-providing cell):
 * {@code DTS_URL=http://<dts-cell-origin>
 * DTS_HTTPS_URL=https://<dts-cell-https-origin>
 * DTS_DELIVERY_USER=<user> DTS_DELIVERY_PASSWORD=<secret>
 * npm run test:surface -- --path tests/forms-confirmation-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} (or {@code DTS_URL}) every case skips with the named
 * reason; without {@code DTS_HTTPS_URL} the retrieval cases skip naming
 * the missing HTTPS publish. Seed the credential first with
 * {@code python docker/scripts/perc-devctl.py qa-seed-widget-fixtures
 * --delivery-password <secret>} (parent #6237) and export the same secret
 * as {@code DTS_DELIVERY_PASSWORD}.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect, request: baseRequest } = require("@playwright/test");
const {
  MATRIX_SPEC,
  CASE_SUBMIT,
  CASE_SUMMARIES,
  CASE_CSV,
  SKIP_NO_DTS_BASE,
  SKIP_NO_DTS_HTTPS,
  skipServiceDown,
  skipNoRetrieval,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  dtsHttpsBaseUrl,
  dtsHttpsServiceUrl,
  deliveryAuthHeader,
  probeFormName,
  retrievalDetail,
  summariesTotal,
  buildConfirmationRow,
  validateConfirmationRow,
} = require("./helpers/forms-confirmation-matrix");

const caseOutcomes = new Map();

function recordCase(name) {
  caseOutcomes.set(name, "pass");
}

function originOf(base) {
  return base.origin;
}

async function probeLiveness(request, base) {
  // Liveness hangs off the deployment context (war name) plus the JAX-RS
  // root — the service is not served at the DTS origin root (issue #6342).
  const url = `${dtsContextUrl(base)}/forms/csrf`;
  let response = null;
  try {
    response = await request.head(url);
  } catch (error) {
    test.skip(
      true,
      `${skipServiceDown(originOf(base))} ` +
        `(probe unreachable: ${String((error && error.message) || error).slice(0, 120)})`,
    );
  }
  test.skip(!response.ok(), skipServiceDown(originOf(base)));
  return response;
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

/**
 * Opens the retrieval surface: the published DTS HTTPS origin plus a
 * dedicated request context that allows the cell's self-signed TLS
 * identity. The admin retrieval surface is {@code CONFIDENTIAL}-gated, so
 * plain-HTTP callers only see its 302 redirect — the summaries and CSV
 * reads must run here, not on the {@code DTS_BASE_URL} origin. Skips by
 * name when the cell publishes no HTTPS origin.
 *
 * @returns {Promise<{ httpsBase: URL, httpsRequest: import("@playwright/test").APIRequestContext }>}
 */
async function httpsRetrievalContext() {
  const httpsBase = dtsHttpsBaseUrl();
  test.skip(!httpsBase, `forms confirmation (Forms): ${SKIP_NO_DTS_HTTPS}`);
  const httpsRequest = await baseRequest.newContext({
    ignoreHTTPSErrors: true,
  });
  return { httpsBase, httpsRequest };
}

/**
 * Submits one probe entry to a self-seeded form. The real widget posts to
 * the trailing-slash collect path (see the form widget JS: action +
 * "collect/"): that path is the openly served submit contract, while the
 * bare /forms/form/* tree stays behind the deliverymanager role (admin
 * retrieval surface).
 *
 * @returns {Promise<{ formName: string, marker: string }>} seeded identity
 */
async function submitProbeEntry(request, svc, siteOrigin) {
  const stamp = Date.now();
  const formName = probeFormName(stamp);
  const marker = `qaconfirm-${stamp}`;
  // maxRedirects: 0 keeps the success redirect observable as a 302 to
  // /success.html instead of following it.
  const collect = await request.post(`${svc}/form/collect/`, {
    maxRedirects: 0,
    form: {
      action: "submit",
      perc_formName: formName,
      perc_hostUrl: siteOrigin,
      qaconfirm: marker,
    },
  });
  expect(
    collect.status(),
    `forms: submit to self-seeded form '${formName}' must take the success redirect (HTTP ${collect.status()})`,
  ).toBe(302);
  expect(
    String(collect.headers().location || ""),
    `forms: submit to '${formName}' must redirect at the success page`,
  ).toMatch(/\/success\.html$/);
  return { formName, marker };
}

/**
 * Reads the admin summaries for one form over the published DTS HTTPS
 * origin. Returns {@code null} when the cell cannot serve the retrieval
 * surface over HTTPS (auth-gated or redirected), so the caller skips with
 * the named reason; any other failure shape fails the test.
 */
async function readSummaries(request, svc, formName) {
  const url = `${svc}/form/cms/${encodeURIComponent(formName)}`;
  let read = null;
  try {
    // maxRedirects: 0 keeps a gated answer (401/403) or a stray
    // CONFIDENTIAL redirect as a plain response so the skip below engages;
    // following the redirect would throw instead of skipping.
    read = await request.get(url, {
      maxRedirects: 0,
      headers: deliveryAuthHeader(),
    });
  } catch (error) {
    return {
      skipped:
        `${skipNoRetrieval(`GET /forms/form/cms/${formName} is unreachable ` +
          `(${String((error && error.message) || error).slice(0, 120)})`)}`,
    };
  }
  if (!read.ok() && [401, 403, 302].includes(read.status())) {
    return {
      skipped: skipNoRetrieval(
        retrievalDetail(
          `/forms/form/cms/${formName}`,
          read.status(),
          read.headers().location || null,
        ),
      ),
    };
  }
  expect(
    read.ok(),
    `forms: summaries for '${formName}' must answer 200 (HTTP ${read.status()})`,
  ).toBe(true);
  return { body: await readJson(read) };
}

test.describe("forms confirmation matrix", () => {
  test.afterAll(async () => {
    // One row for the pinned forms entry. A case that never ran
    // (grep-filtered run) or never recorded (its test failed) is reported
    // as skipped; a red run is still red because the failing test itself
    // fails.
    const row = buildConfirmationRow(
      {
        [CASE_SUBMIT]: caseOutcomes.get(CASE_SUBMIT) || "skip",
        [CASE_SUMMARIES]: caseOutcomes.get(CASE_SUMMARIES) || "skip",
        [CASE_CSV]: caseOutcomes.get(CASE_CSV) || "skip",
      },
      { spec: MATRIX_SPEC },
    );
    const problems = validateConfirmationRow(row);
    if (problems.length > 0) {
      throw new Error(`matrix row fails schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "forms-confirmation-matrix.json"),
      `${JSON.stringify([row], null, 2)}\n`,
      "utf8",
    );
  });

  test(
    "submit confirms: self-seeded form submit takes the success redirect @dts",
    { tag: ["@dts", "@dts-forms"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `forms confirmation (Forms): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      await submitProbeEntry(request, svc, base.origin);
      recordCase(CASE_SUBMIT);
    },
  );

  test(
    "confirmation is retrievable: summaries count increments after submit @dts",
    { tag: ["@dts", "@dts-forms"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `forms confirmation (Forms): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      // Retrieval runs over the published DTS HTTPS origin: the summaries
      // endpoint is CONFIDENTIAL-gated, so plain-HTTP callers only see its
      // 302 redirect. The submit itself is an open surface either way.
      const { httpsBase, httpsRequest } = await httpsRetrievalContext();
      try {
        const svc = dtsHttpsServiceUrl(httpsBase);
        const stamp = Date.now();
        const formName = probeFormName(stamp);
        const before = await readSummaries(httpsRequest, svc, formName);
        test.skip(!!before.skipped, before.skipped);
        const beforeTotal = summariesTotal(before.body, formName) || 0;

        const marker = `qaconfirm-${stamp}`;
        const collect = await httpsRequest.post(`${svc}/form/collect/`, {
          maxRedirects: 0,
          form: {
            action: "submit",
            perc_formName: formName,
            perc_hostUrl: httpsBase.origin,
            qaconfirm: marker,
          },
        });
        expect(
          collect.status(),
          `forms: submit to self-seeded form '${formName}' must take the success redirect (HTTP ${collect.status()})`,
        ).toBe(302);

        const after = await readSummaries(httpsRequest, svc, formName);
        test.skip(!!after.skipped, after.skipped);
        const afterTotal = summariesTotal(after.body, formName);
        expect(
          afterTotal,
          `forms: summaries for '${formName}' must report the submitted entry (before=${beforeTotal}, after=${afterTotal})`,
        ).not.toBeNull();
        expect(
          afterTotal,
          `forms: summaries total for '${formName}' must increment (${beforeTotal} -> ${afterTotal})`,
        ).toBe(beforeTotal + 1);
        recordCase(CASE_SUMMARIES);
      } finally {
        await httpsRequest.dispose();
      }
    },
  );

  test(
    "confirmation is exportable: CSV export contains the submitted entry @dts",
    { tag: ["@dts", "@dts-forms"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `forms confirmation (Forms): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      // Export runs over the published DTS HTTPS origin like the
      // summaries: a gated/redirected answer over HTTPS skips by name
      // instead of failing.
      const { httpsBase, httpsRequest } = await httpsRetrievalContext();
      try {
        const svc = dtsHttpsServiceUrl(httpsBase);
        const { formName, marker } = await submitProbeEntry(
          httpsRequest,
          svc,
          httpsBase.origin,
        );

        let csv = null;
        try {
          csv = await httpsRequest.get(
            `${svc}/form/cms/${encodeURIComponent(formName)}/${encodeURIComponent(`${formName}.csv`)}`,
            { maxRedirects: 0, headers: deliveryAuthHeader() },
          );
        } catch (error) {
          test.skip(
            true,
            skipNoRetrieval(
              `GET /forms/form/cms/${formName} export is unreachable ` +
                `(${String((error && error.message) || error).slice(0, 120)})`,
            ),
          );
        }
        test.skip(
          !csv.ok() && [401, 403, 302].includes(csv.status()),
          skipNoRetrieval(
            retrievalDetail(
              `/forms/form/cms/${formName} export`,
              csv.status(),
              csv.headers().location || null,
            ),
          ),
        );
        expect(
          csv.ok(),
          `forms: CSV export for '${formName}' must answer 200 (HTTP ${csv.status()})`,
        ).toBe(true);
        const contentType = csv.headers()["content-type"] || "";
        expect(
          /csv/i.test(contentType),
          `forms: CSV export for '${formName}' must be text/csv (content-type=${contentType})`,
        ).toBe(true);
        const text = await csv.text();
        expect(
          text.includes(marker),
          `forms: CSV export for '${formName}' must contain the submitted entry (marker=${marker})`,
        ).toBe(true);
        recordCase(CASE_CSV);
      } finally {
        await httpsRequest.dispose();
      }
    },
  );
});
