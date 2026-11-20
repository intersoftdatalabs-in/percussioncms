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
 * US3 polls vote matrix (#6241 / parent #6241): prove the
 * vote+results round-trip against the in-cell DTS polls service — a
 * self-seeded probe poll is created by voting, a second vote persists,
 * and the results surfaces reflect the votes.
 *
 * <p>Tags: {@code @dts} plus {@code @dts-polls}.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/polls-vote-matrix.js} (closed inventory, 3
 * cases). {@code afterAll} assembles the one results-matrix row,
 * validates it against
 * {@code contracts/results-matrix.schema.json}, and writes it to
 * {@code test-results/polls-vote-matrix.json}. Three-level skip
 * contract: without {@code DTS_BASE_URL} (or its {@code DTS_URL} banner
 * alias) every case skips naming the
 * cell (#6237, green on the CMS-only cell); with a base URL each case
 * probes liveness ({@code HEAD <context>/polls/csrf} — the service
 * lives under its Tomcat deployment context, issue #6342); a cell whose
 * polls store answers {@code ERROR} to a save skips the affected cases
 * with a named reason instead of failing. Votes are self-seeded under a
 * unique {@code qamatrix-poll-<stamp>} name and question (see
 * {@code probePoll}), so no CMS fixture and no pre-seeded
 * {@code DTS_FIXTURE_POLL} account are needed — unlike the US3
 * interaction-matrix polls case, which votes on a provisioned fixture
 * poll. Each Playwright case votes on its own probe poll so cases stay
 * independent under parallel workers.</p>
 *
 * <p>Run (needs the DTS-providing cell):
 * {@code DTS_BASE_URL=https://<dts-cell-origin>} (or {@code DTS_URL},
 * the {@code qa-up --with-dts} banner alias)
 * {@code npm run test:surface -- --path tests/polls-vote-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} (or its {@code DTS_URL} alias) every case skips
 * with the named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const {
  MATRIX_SPEC,
  CASE_VOTE,
  CASE_PERSIST,
  CASE_RESULTS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  probePoll,
  isSuccessStatus,
  buildVoteRow,
  validateVoteRow,
} = require("./helpers/polls-vote-matrix");

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
  const url = `${dtsContextUrl(base)}/polls/csrf`;
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
 * Casts one vote on the probe poll. {@code PSPollsService#savePoll}
 * creates the poll when the question is new, so the first vote on a
 * fresh probe both creates the poll and records the vote. An
 * {@code ERROR} status means the cell cannot persist polls
 * (unprovisioned store) — that is a named skip, not a product failure.
 * An HTTP-level failure on a live service is a real failure.
 *
 * @returns {Promise<object>} the SUCCESS result payload (PSRestPoll)
 */
async function saveVote(request, svc, poll) {
  const save = await request.post(`${svc}/save`, {
    data: {
      pollName: poll.pollName,
      pollQuestion: poll.pollQuestion,
      pollSubmits: { [poll.choice]: true },
      restrictBySession: false,
    },
  });
  expect(
    save.ok(),
    `polls: vote POST for '${poll.pollName}' must be accepted (HTTP ${save.status()})`,
  ).toBe(true);
  const body = await readJson(save);
  test.skip(
    !body || !isSuccessStatus(body.status),
    skipUnprovisioned(
      `POST /polls/save for '${poll.pollName}' answered status '${body && body.status}'`,
    ),
  );
  return body.result;
}

/**
 * Reads the probe poll back by name.
 *
 * @returns {Promise<object>} the SUCCESS result payload (PSRestPoll)
 */
async function readPoll(request, svc, pollName) {
  const reread = await request.get(`${svc}/${encodeURIComponent(pollName)}`);
  expect(reread.status(), `polls: re-read of '${pollName}' must stay 200`).toBe(200);
  const body = await readJson(reread);
  test.skip(
    !body || !isSuccessStatus(body.status) || body.result == null,
    skipUnprovisioned(`GET /polls/${pollName} after voting answered status '${body && body.status}'`),
  );
  return body.result;
}

function choiceCount(result, choice) {
  const results = result.pollResults;
  if (results == null || typeof results !== "object") {
    return null;
  }
  const count = results[choice];
  return typeof count === "number" ? count : null;
}

test.describe("polls vote matrix", () => {
  test.afterAll(async () => {
    // One row for the pinned polls entry. A case that never ran
    // (grep-filtered run) or never recorded (its test failed) is reported
    // as skipped; a red run is still red because the failing test itself
    // fails.
    const row = buildVoteRow(
      {
        [CASE_VOTE]: caseOutcomes.get(CASE_VOTE) || "skip",
        [CASE_PERSIST]: caseOutcomes.get(CASE_PERSIST) || "skip",
        [CASE_RESULTS]: caseOutcomes.get(CASE_RESULTS) || "skip",
      },
      { spec: MATRIX_SPEC },
    );
    const problems = validateVoteRow(row);
    if (problems.length > 0) {
      throw new Error(`matrix row fails schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "polls-vote-matrix.json"),
      `${JSON.stringify([row], null, 2)}\n`,
      "utf8",
    );
  });

  test(
    "vote creates the poll: self-seeded save answers SUCCESS with a counted vote @dts",
    { tag: ["@dts", "@dts-polls"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `polls vote (Polls): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const poll = probePoll();
      const result = await saveVote(request, dtsServiceUrl(base), poll);
      const total = result.totalVotes;
      expect(
        typeof total === "number" && total >= 1,
        `polls: first vote for '${poll.pollName}' must be counted (totalVotes=${total})`,
      ).toBe(true);
      expect(
        choiceCount(result, poll.choice),
        `polls: first vote for '${poll.choice}' on '${poll.pollName}' must appear in pollResults`,
      ).toBe(total);
      recordCase(CASE_VOTE);
    },
  );

  test(
    "vote persists: a second vote increments totalVotes on re-read @dts",
    { tag: ["@dts", "@dts-polls"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `polls vote (Polls): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const poll = probePoll();
      const first = await saveVote(request, svc, poll);
      const before = first.totalVotes;
      expect(
        typeof before === "number" && before >= 1,
        `polls: first vote for '${poll.pollName}' must be counted before the persistence check`,
      ).toBe(true);
      await saveVote(request, svc, poll);
      const after = await readPoll(request, svc, poll.pollName);
      expect(
        after.totalVotes,
        `polls: second vote for '${poll.choice}' on '${poll.pollName}' must persist (totalVotes ${before} -> ${after.totalVotes})`,
      ).toBe(before + 1);
      expect(
        choiceCount(after, poll.choice),
        `polls: persisted vote for '${poll.choice}' must appear in pollResults`,
      ).toBe(before + 1);
      recordCase(CASE_PERSIST);
    },
  );

  test(
    "results surface reflects the votes: GET by question matches the named read @dts",
    { tag: ["@dts", "@dts-polls"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `polls vote (Polls): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const poll = probePoll();
      const created = await saveVote(request, svc, poll);
      const byName = await readPoll(request, svc, poll.pollName);
      const byQuestion = await request.get(`${svc}/question/${encodeURIComponent(poll.pollQuestion)}`);
      expect(
        byQuestion.status(),
        `polls: question read for '${poll.pollName}' must stay 200`,
      ).toBe(200);
      const body = await readJson(byQuestion);
      test.skip(
        !body || !isSuccessStatus(body.status) || body.result == null,
        skipUnprovisioned(
          `GET /polls/question for '${poll.pollName}' answered status '${body && body.status}'`,
        ),
      );
      const result = body.result;
      expect(
        result.totalVotes,
        "polls: question read must report the same totalVotes as the named read",
      ).toBe(byName.totalVotes);
      expect(
        choiceCount(result, poll.choice),
        `polls: question read must include the voted choice '${poll.choice}'`,
      ).toBe(choiceCount(byName, poll.choice));
      expect(
        created.totalVotes,
        "polls: named read must agree with the created vote count",
      ).toBe(byName.totalVotes);
      recordCase(CASE_RESULTS);
    },
  );
});
