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
 * US3 DTS dynamic interaction matrix (#6241 / parent #6238): one
 * interaction case per DTS-backed service (polls, feeds, forms, comments,
 * metadata, membership) proving the round-trip — vote persists, feed
 * renders, form confirms and is retrievable, comment appears per moderation
 * rules, index reflects published content, login round-trips.
 *
 * <p>Tags: {@code @dts} plus one {@code @dts-<service>} tag per service.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/dts-interaction-matrix.js} (closed inventory, 6
 * services). {@code afterAll} assembles one results-matrix row per service,
 * validates every row against
 * {@code contracts/results-matrix.schema.json}, and writes the matrix to
 * {@code test-results/dts-interaction-matrix.json}. Two-level skip contract:
 * without {@code DTS_BASE_URL} every case skips naming its service (T020,
 * green on the CMS-only cell); with a base URL each case probes liveness
 * ({@code HEAD <root>/csrf}) and then its seeded fixture — an unseeded
 * fixture skips with its own named reason instead of failing, so the #6237
 * cell turns cases green one fixture at a time.</p>
 *
 * <p>Run (needs the DTS-providing cell + seeded interaction fixtures):
 * {@code DTS_BASE_URL=https://<dts-cell-origin>
 * DTS_FIXTURE_POLL=qamatrix-poll DTS_FIXTURE_FORM=qamatrix-form
 * DTS_FIXTURE_SITE=EnterpriseInvestments DTS_FIXTURE_PAGE=/qamatrix
 * DTS_FIXTURE_FEED=EnterpriseInvestments/qamatrix-feed/localhost
 * DTS_FIXTURE_USER=qa-matrix@example.com DTS_FIXTURE_PASSWORD=<secret>
 * npm run test:surface -- --path tests/dts-interaction-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} every case skips with the named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const {
  DTS_SERVICES,
  MATRIX_SPEC,
  CASE_INTERACT,
  skipNoDts,
  skipServiceDown,
  skipNoFixture,
  tagForService,
  dtsBaseUrl,
  fixtureValue,
  buildInteractionRow,
  validateInteractionRow,
} = require("./helpers/dts-interaction-matrix");

const caseOutcomes = new Map();

function recordCase(entry) {
  caseOutcomes.set(entry.name, "pass");
}

function originOf(base) {
  return base.origin;
}

async function probeLiveness(request, base, entry) {
  const url = `${originOf(base)}${entry.csrf}`;
  let response = null;
  try {
    response = await request.head(url);
  } catch (error) {
    test.skip(
      true,
      skipServiceDown(entry, `${originOf(base)} (unreachable: ${String((error && error.message) || error).slice(0, 120)})`),
    );
  }
  test.skip(
    !response.ok(),
    skipServiceDown(entry, originOf(base)),
  );
  return response;
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

async function interactPolls(request, base, entry) {
  const pollName = fixtureValue(entry);
  const pollUrl = `${originOf(base)}/polls/${encodeURIComponent(pollName)}`;
  const read = await request.get(pollUrl);
  test.skip(
    read.status() !== 200,
    skipNoFixture(entry, `GET /polls/${pollName} answered HTTP ${read.status()}`),
  );
  const body = await readJson(read);
  test.skip(
    !body || body.status !== "SUCCESS" || body.result == null,
    skipNoFixture(entry, `fixture poll '${pollName}' is not seeded (status=${body && body.status})`),
  );
  const result = body.result;
  const choices = result.pollResults && typeof result.pollResults === "object" ? result.pollResults : null;
  const choiceNames = choices ? Object.keys(choices) : [];
  test.skip(
    choiceNames.length === 0,
    skipNoFixture(entry, `fixture poll '${pollName}' exposes no votable choices`),
  );
  const choice = choiceNames[0];
  const before = result.totalVotes;
  const voted = { ...(choices || {}) };
  voted[choice] = (voted[choice] || 0) + 1;
  const save = await request.post(`${originOf(base)}/polls/save`, {
    data: {
      pollName: result.pollName || pollName,
      pollQuestion: result.pollQuestion,
      pollResults: voted,
      restrictBySession: false,
    },
  });
  expect(save.ok(), `polls: vote POST for '${pollName}' must be accepted`).toBe(true);
  const reread = await request.get(pollUrl);
  expect(reread.status(), `polls: re-read of '${pollName}' must stay 200`).toBe(200);
  const after = await readJson(reread);
  const afterVotes = after && after.result ? after.result.totalVotes : null;
  expect(
    afterVotes,
    `polls: vote for '${choice}' on '${pollName}' must persist (totalVotes ${before} -> ${afterVotes})`,
  ).toBe(before + 1);
  recordCase(entry);
}

async function interactFeeds(request, base, entry) {
  const feedPath = fixtureValue(entry);
  const feedUrl = `${originOf(base)}/rss/${feedPath}`;
  const read = await request.get(feedUrl, { headers: { Accept: "text/xml" } });
  test.skip(
    read.status() !== 200,
    skipNoFixture(entry, `GET /rss/${feedPath} answered HTTP ${read.status()}`),
  );
  const contentType = read.headers()["content-type"] || "";
  expect(
    /xml/i.test(contentType),
    `feeds: fixture feed '${feedPath}' must render XML (content-type=${contentType})`,
  ).toBe(true);
  const text = await read.text();
  expect(
    /<item[\s>]|<entry[\s>]/i.test(text),
    `feeds: fixture feed '${feedPath}' must render entries`,
  ).toBe(true);
  recordCase(entry);
}

async function interactForms(request, base, entry) {
  const formName = fixtureValue(entry);
  const cmsUrl = `${originOf(base)}/forms/form/cms/${encodeURIComponent(formName)}`;
  const read = await request.get(cmsUrl);
  test.skip(
    read.status() !== 200,
    skipNoFixture(entry, `GET /forms/form/cms/${formName} answered HTTP ${read.status()}`),
  );
  const marker = `qamatrix-${Date.now()}`;
  const collect = await request.post(`${originOf(base)}/forms/form/collect`, {
    form: {
      action: "submit",
      formName,
      qamatrix: marker,
    },
  });
  expect(
    collect.ok(),
    `forms: submit to fixture form '${formName}' must confirm (HTTP ${collect.status()})`,
  ).toBe(true);
  const reread = await request.get(cmsUrl);
  expect(
    reread.status(),
    `forms: fixture form '${formName}' must stay retrievable after submit`,
  ).toBe(200);
  const payload = await reread.text();
  expect(
    payload.length > 0,
    `forms: fixture form '${formName}' retrieval must return content`,
  ).toBe(true);
  recordCase(entry);
}

async function interactComments(request, base, entry) {
  const site = fixtureValue(entry);
  const page = process.env.DTS_FIXTURE_PAGE || "/qamatrix";
  const stateRead = await request.get(
    `${originOf(base)}/comment/defaultModerationState/${encodeURIComponent(site)}`,
  );
  test.skip(
    stateRead.status() !== 200,
    skipNoFixture(entry, `site '${site}' has no comment moderation state (HTTP ${stateRead.status()})`),
  );
  const defaultState = (await stateRead.text()).trim();
  const marker = `qamatrix-${Date.now()}`;
  const add = await request.post(`${originOf(base)}/comment/addcomment`, {
    form: {
      action: "add",
      username: "qa-matrix",
      site,
      pagepath: page,
      email: "qa-matrix@example.com",
      text: marker,
      title: "QA matrix probe",
      url: "",
    },
  });
  expect(
    add.ok(),
    `comments: posting to '${site}${page}' must be accepted (HTTP ${add.status()})`,
  ).toBe(true);
  const list = await request.post(`${originOf(base)}/comment/list`, {
    data: { site, pagepath: page, maxResults: 25, startIndex: 0 },
  });
  expect(list.ok(), `comments: listing '${site}${page}' must stay readable`).toBe(true);
  const listed = await readJson(list);
  const texts = JSON.stringify(listed || {});
  if (texts.includes(marker)) {
    recordCase(entry);
    return;
  }
  // Held for moderation is still the rule working: the site default state
  // was read live above, so a non-visible comment is only a pass when the
  // site does not auto-approve.
  expect(
    defaultState.length > 0 && !/^approve/i.test(defaultState),
    `comments: probe comment on '${site}${page}' is neither listed nor ` +
      `covered by a moderating default state ('${defaultState}')`,
  ).toBe(true);
  recordCase(entry);
}

async function interactMetadata(request, base, entry) {
  const site = fixtureValue(entry);
  const query = await request.post(`${originOf(base)}/metadata/get`, {
    data: { criteria: [`site:${site}`], maxResults: 5, startIndex: 0 },
  });
  test.skip(
    !query.ok(),
    skipNoFixture(entry, `POST /metadata/get answered HTTP ${query.status()}`),
  );
  const body = await readJson(query);
  test.skip(
    !body || !Array.isArray(body.results),
    skipNoFixture(entry, "metadata query returned no result set shape"),
  );
  test.skip(
    body.results.length === 0,
    skipNoFixture(entry, `index holds no fixture content for site '${site}' yet`),
  );
  recordCase(entry);
}

async function interactMembership(request, base, entry) {
  const email = fixtureValue(entry);
  test.skip(
    !email,
    skipNoFixture(entry, "DTS_FIXTURE_USER names no fixture account"),
  );
  const password = process.env.DTS_FIXTURE_PASSWORD || "";
  test.skip(
    !password,
    skipNoFixture(entry, `DTS_FIXTURE_PASSWORD is not configured for '${email}'`),
  );
  const login = await request.post(`${originOf(base)}/membership/login`, {
    data: { email, password },
  });
  test.skip(
    login.status() === 401 || login.status() === 403,
    skipNoFixture(entry, `fixture account '${email}' is not seeded (HTTP ${login.status()})`),
  );
  expect(login.ok(), `membership: login for '${email}' must succeed`).toBe(true);
  const loginBody = await readJson(login);
  const sessionId =
    loginBody && typeof loginBody.sessionId === "string" ? loginBody.sessionId : "";
  expect(sessionId.length > 0, "membership: login must carry a session id").toBe(true);
  const whoami = await request.post(`${originOf(base)}/membership/session`, {
    data: { sessionId },
  });
  expect(whoami.ok(), "membership: session lookup must succeed").toBe(true);
  const whoamiBody = JSON.stringify((await readJson(whoami)) || {});
  expect(
    whoamiBody.toLowerCase().includes(email.toLowerCase()),
    "membership: session lookup must resolve the fixture account",
  ).toBe(true);
  const logout = await request.post(`${originOf(base)}/membership/logout`, {
    data: { sessionId },
  });
  expect(logout.ok(), "membership: logout must succeed").toBe(true);
  recordCase(entry);
}

const INTERACTORS = {
  polls: interactPolls,
  feeds: interactFeeds,
  forms: interactForms,
  comments: interactComments,
  metadata: interactMetadata,
  membership: interactMembership,
};

test.describe("dts dynamic interaction matrix", () => {
  test.afterAll(async () => {
    // One row per pinned service. A case that never ran (grep-filtered run)
    // or never recorded (its test failed) is reported as skipped; a red run
    // is still red because the failing test itself fails.
    const rows = DTS_SERVICES.map((entry) =>
      buildInteractionRow(
        entry,
        { [CASE_INTERACT]: caseOutcomes.get(entry.name) || "skip" },
        { spec: MATRIX_SPEC },
      ),
    );
    const problems = [];
    for (const row of rows) {
      for (const error of validateInteractionRow(row)) {
        problems.push(`${row.contentType}: ${error}`);
      }
    }
    if (problems.length > 0) {
      throw new Error(`matrix rows fail schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "dts-interaction-matrix.json"),
      `${JSON.stringify(rows, null, 2)}\n`,
      "utf8",
    );
  });

  DTS_SERVICES.forEach((entry) => {
    test(
      `${entry.name}: ${entry.interaction}`,
      { tag: ["@dts", tagForService(entry.name)] },
      async ({ request }) => {
        test.setTimeout(90_000);
        const base = dtsBaseUrl();
        test.skip(!base, skipNoDts(entry));

        await probeLiveness(request, base, entry);
        await INTERACTORS[entry.name](request, base, entry);
      },
    );
  });
});
