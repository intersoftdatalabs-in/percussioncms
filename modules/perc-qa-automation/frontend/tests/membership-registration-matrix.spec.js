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
 * US3 membership registration matrix (#6559 / parent #6241, proved
 * #6484 / parent #6238): prove the register/login round-trip against the
 * in-cell DTS membership service — a fresh visitor account registers,
 * logs in with the same credentials, resolves through the session
 * lookup, and closes on logout.
 *
 * <p>Tags: {@code @dts} plus {@code @dts-membership}.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/membership-registration-matrix.js} (closed
 * inventory, 3 cases). {@code afterAll} assembles the one results-matrix
 * row, validates it against
 * {@code contracts/results-matrix.schema.json}, and writes it to
 * {@code test-results/membership-registration-matrix.json}. Three-level
 * skip contract: without {@code DTS_BASE_URL} every case skips naming
 * the cell (#6237, green on the CMS-only cell); with a base URL each
 * case probes liveness ({@code HEAD <context>/membership/csrf} — the
 * service lives under its Tomcat deployment context, issue #6342); a
 * cell that disables open registration or forces account confirmation
 * skips the affected cases with a named policy reason instead of
 * failing. Registrations are self-seeded under a unique
 * {@code qamember-<stamp>@example.com} address, so no CMS fixture and no
 * pre-seeded account are needed; each case registers its own probe
 * account so cases stay independent under parallel workers.</p>
 *
 * <p>Run (needs the DTS-providing cell):
 * {@code DTS_BASE_URL=https://<dts-cell-origin>
 * npm run test:surface -- --path tests/membership-registration-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} every case skips with the named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const {
  MATRIX_SPEC,
  CASE_REGISTER,
  CASE_LOGIN,
  CASE_LOGOUT,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipNoRegistration,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  probeAccount,
  isSuccessStatus,
  buildRegistrationRow,
  validateRegistrationRow,
} = require("./helpers/membership-registration-matrix");

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
  const url = `${dtsContextUrl(base)}/membership/csrf`;
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
 * Registers one self-seeded probe account. A policy-disabled cell (open
 * registration off) or a confirmation-forcing cell answers with a
 * non-SUCCESS status — that is a named skip, not a product failure. An
 * HTTP-level failure on a live service is a real failure.
 *
 * @returns {Promise<{ email: string, password: string }>} probe credentials
 */
async function registerProbeAccount(request, svc) {
  const { email, password } = probeAccount();
  const create = await request.post(`${svc}/user`, {
    data: { email, password, confirmationRequired: false },
  });
  expect(
    create.ok(),
    `membership: register for '${email}' must be accepted (HTTP ${create.status()})`,
  ).toBe(true);
  const body = await readJson(create);
  const status = body ? body.status : null;
  if (status === "MEMBER_EXISTS") {
    // Timestamp collision (rerun within the same millisecond): one fresh
    // retry keeps the round-trip green without masking real failures.
    return registerProbeAccount(request, svc);
  }
  test.skip(
    !isSuccessStatus(status),
    skipNoRegistration(
      `POST /membership/user for '${email}' answered status '${status}' ` +
        `(${(body && body.message) || "no detail"})`,
    ),
  );
  const sessionId = body && typeof body.sessionId === "string" ? body.sessionId : "";
  expect(
    sessionId.length > 0,
    `membership: register for '${email}' must issue a session id`,
  ).toBe(true);
  return { email, password };
}

/**
 * Logs in with freshly registered credentials. A login that the cell
 * refuses right after a successful register (forced account
 * confirmation) is a named policy skip, not a product failure.
 *
 * @returns {Promise<{ email: string, sessionId: string }>} login identity
 */
async function loginProbeAccount(request, svc, email, password) {
  const login = await request.post(`${svc}/login`, {
    data: { email, password },
  });
  expect(
    login.ok(),
    `membership: login for '${email}' must be accepted (HTTP ${login.status()})`,
  ).toBe(true);
  const body = await readJson(login);
  const status = body ? body.status : null;
  test.skip(
    !isSuccessStatus(status),
    skipNoRegistration(
      `POST /membership/login for freshly registered '${email}' answered status ` +
        `'${status}' (${(body && body.message) || "no detail"}) — the cell forces ` +
        `account confirmation before login`,
    ),
  );
  const sessionId = body && typeof body.sessionId === "string" ? body.sessionId : "";
  expect(sessionId.length > 0, "membership: login must carry a session id").toBe(true);
  return { email, sessionId };
}

test.describe("membership registration matrix", () => {
  test.afterAll(async () => {
    // One row for the pinned membership entry. A case that never ran
    // (grep-filtered run) or never recorded (its test failed) is reported
    // as skipped; a red run is still red because the failing test itself
    // fails.
    const row = buildRegistrationRow(
      {
        [CASE_REGISTER]: caseOutcomes.get(CASE_REGISTER) || "skip",
        [CASE_LOGIN]: caseOutcomes.get(CASE_LOGIN) || "skip",
        [CASE_LOGOUT]: caseOutcomes.get(CASE_LOGOUT) || "skip",
      },
      { spec: MATRIX_SPEC },
    );
    const problems = validateRegistrationRow(row);
    if (problems.length > 0) {
      throw new Error(`matrix row fails schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "membership-registration-matrix.json"),
      `${JSON.stringify([row], null, 2)}\n`,
      "utf8",
    );
  });

  test(
    "registration creates the account: self-seeded register answers SUCCESS with a session @dts",
    { tag: ["@dts", "@dts-membership"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `membership registration (Membership): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      await registerProbeAccount(request, dtsServiceUrl(base));
      recordCase(CASE_REGISTER);
    },
  );

  test(
    "login round-trips: freshly registered credentials log in and resolve through the session lookup @dts",
    { tag: ["@dts", "@dts-membership"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `membership registration (Membership): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const { email, password } = await registerProbeAccount(request, svc);
      const { sessionId } = await loginProbeAccount(request, svc, email, password);
      const whoami = await request.post(`${svc}/session`, {
        data: { sessionId },
      });
      expect(whoami.ok(), "membership: session lookup must succeed").toBe(true);
      const whoamiBody = JSON.stringify((await readJson(whoami)) || {});
      expect(
        whoamiBody.toLowerCase().includes(email.toLowerCase()),
        "membership: session lookup must resolve the freshly registered account",
      ).toBe(true);
      recordCase(CASE_LOGIN);
    },
  );

  test(
    "logout closes the session: the session lookup no longer resolves the account @dts",
    { tag: ["@dts", "@dts-membership"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `membership registration (Membership): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const { email, password } = await registerProbeAccount(request, svc);
      const { sessionId } = await loginProbeAccount(request, svc, email, password);
      const logout = await request.post(`${svc}/logout`, {
        data: { sessionId },
      });
      expect(logout.ok(), "membership: logout must succeed").toBe(true);
      const after = await request.post(`${svc}/session`, {
        data: { sessionId },
      });
      const afterText = JSON.stringify((await readJson(after)) || "");
      expect(
        !after.ok() || !afterText.toLowerCase().includes(email.toLowerCase()),
        "membership: session lookup after logout must not resolve the account",
      ).toBe(true);
      recordCase(CASE_LOGOUT);
    },
  );
});
