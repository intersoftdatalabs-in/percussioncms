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
 * US3 comments moderation matrix (#6485 / parent #6238): prove the
 * approve/reject round-trip against the in-cell DTS comments service —
 * a self-seeded visitor comment is submitted through the open surface,
 * approved through the {@code deliverymanager}-gated moderation surface
 * and confirmed publicly visible, then a second probe is rejected and
 * confirmed hidden from the public list.
 *
 * <p>Tags: {@code @dts} plus {@code @dts-comments}.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/comments-moderation-matrix.js} (closed inventory,
 * 3 cases). {@code afterAll} assembles the one results-matrix row,
 * validates it against
 * {@code contracts/results-matrix.schema.json}, and writes it to
 * {@code test-results/comments-moderation-matrix.json}. Three-level skip
 * contract: without {@code DTS_BASE_URL} every case skips naming the
 * cell (#6237, green on the CMS-only cell); with a base URL each case
 * probes liveness ({@code HEAD <context>/comment/csrf} — the service
 * lives under its Tomcat deployment context, issue #6342); a cell
 * without provisioned {@code deliverymanager} credentials — or without
 * a reachable DTS HTTPS port for the {@code CONFIDENTIAL} redirect on
 * the moderation paths — skips the gated legs with a named reason
 * instead of failing (provisioning tracked in #6375). Probe threads are
 * self-seeded under a unique {@code qamod-<stamp>} site + page, so no
 * CMS fixture and no pre-seeded comment thread are needed; each case
 * submits its own probe thread so cases stay independent under parallel
 * workers.</p>
 *
 * <p>Run (needs the DTS-providing cell):
 * {@code DTS_BASE_URL=https://<dts-cell-origin>
 * npm run test:surface -- --path tests/comments-moderation-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} every case skips with the named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const {
  MATRIX_SPEC,
  CASE_SUBMIT,
  CASE_APPROVE,
  CASE_REJECT,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipNoModeration,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  deliveryAuthHeader,
  probeThread,
  buildModerationRow,
  validateModerationRow,
} = require("./helpers/comments-moderation-matrix");

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
  const url = `${dtsContextUrl(base)}/comment/csrf`;
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
 * Submits one self-seeded probe comment through the open visitor
 * surface. An HTTP-level failure on a live service is a real failure;
 * the submit surface needs no credentials.
 *
 * @returns {object} the probe thread that was submitted
 */
async function submitProbeComment(request, svc, thread) {
  const add = await request.post(`${svc}/addcomment`, {
    form: {
      action: "add",
      username: thread.username,
      site: thread.site,
      pagepath: thread.page,
      email: thread.email,
      text: thread.marker,
      title: thread.title,
      url: "",
    },
  });
  expect(
    add.ok(),
    `comments: posting probe '${thread.marker}' to '${thread.site}${thread.page}' must be accepted (HTTP ${add.status()})`,
  ).toBe(true);
  return thread;
}

/**
 * Resolves the ids of queued probe comments carrying the marker through
 * the moderator queue. A 401/403 means the cell did not provision usable
 * {@code deliverymanager} credentials; an unreachable endpoint (the
 * {@code CONFIDENTIAL} redirect to a missing DTS HTTPS port) means the
 * gated surface cannot be read here — both are named skips, not product
 * failures.
 *
 * @returns {string[]|null} queued ids, or null when the queue is unreadable here
 */
async function moderatorIdsFor(request, svc, thread, authHeaders) {
  let queue = null;
  try {
    queue = await request.post(`${svc}/moderation/asmoderator`, {
      data: { site: thread.site, pagepath: thread.page, maxResults: 50, startIndex: 0 },
      headers: authHeaders,
    });
  } catch (error) {
    test.skip(
      true,
      skipNoModeration(
        `the moderator queue for '${thread.site}${thread.page}' is unreachable ` +
          `(probe failed: ${String((error && error.message) || error).slice(0, 120)})`,
      ),
    );
  }
  if (queue.status() === 401 || queue.status() === 403) {
    test.skip(
      true,
      skipNoModeration(
        `POST /comment/moderation/asmoderator answered HTTP ${queue.status()} ` +
          `— the cell did not provision deliverymanager credentials`,
      ),
    );
  }
  expect(
    queue.ok(),
    `comments: the moderator queue for '${thread.site}${thread.page}' must stay readable (HTTP ${queue.status()})`,
  ).toBe(true);
  const body = await readJson(queue);
  const items = Array.isArray(body) ? body : (body && body.comments) || [];
  const ids = [];
  for (const item of items || []) {
    if (item && JSON.stringify(item).includes(thread.marker) && item.id) {
      ids.push(String(item.id));
    }
  }
  return ids;
}

/**
 * Reads the public comment list for the probe thread. The public list
 * needs no credentials; it must stay readable on a live service.
 *
 * @returns {boolean} whether the public list names the probe marker
 */
async function publicListsMarker(request, svc, thread) {
  const list = await request.post(`${svc}/list`, {
    data: { site: thread.site, pagepath: thread.page, maxResults: 25, startIndex: 0 },
  });
  expect(
    list.ok(),
    `comments: listing '${thread.site}${thread.page}' must stay readable (HTTP ${list.status()})`,
  ).toBe(true);
  return JSON.stringify((await readJson(list)) || {}).includes(thread.marker);
}

/**
 * Applies one moderation decision (approve/reject) to queued probe ids.
 * A 401/403 means the cell did not provision usable credentials; an
 * unreachable endpoint (the {@code CONFIDENTIAL} redirect to a missing
 * DTS HTTPS port) means the gated surface cannot be written here.
 */
async function applyModeration(request, svc, verb, ids, thread) {
  let applied = null;
  try {
    applied = await request.put(`${svc}/moderation/${verb}`, {
      data: { comments: ids },
      headers: deliveryAuthHeader(),
    });
  } catch (error) {
    test.skip(
      true,
      skipNoModeration(
        `PUT /comment/moderation/${verb} for probe '${thread.marker}' is unreachable ` +
          `(probe failed: ${String((error && error.message) || error).slice(0, 120)})`,
      ),
    );
  }
  if (applied.status() === 401 || applied.status() === 403) {
    test.skip(
      true,
      skipNoModeration(
        `PUT /comment/moderation/${verb} answered HTTP ${applied.status()} ` +
          `— the cell did not provision deliverymanager credentials`,
      ),
    );
  }
  expect(
    applied.ok() || applied.status() === 204,
    `comments: ${verb} for probe '${thread.marker}' must be accepted (HTTP ${applied.status()})`,
  ).toBe(true);
}

test.describe("comments moderation matrix", () => {
  test.afterAll(async () => {
    // One row for the pinned comments entry. A case that never ran
    // (grep-filtered run) or never recorded (its test failed) is reported
    // as skipped; a red run is still red because the failing test itself
    // fails.
    const row = buildModerationRow(
      {
        [CASE_SUBMIT]: caseOutcomes.get(CASE_SUBMIT) || "skip",
        [CASE_APPROVE]: caseOutcomes.get(CASE_APPROVE) || "skip",
        [CASE_REJECT]: caseOutcomes.get(CASE_REJECT) || "skip",
      },
      { spec: MATRIX_SPEC },
    );
    const problems = validateModerationRow(row);
    if (problems.length > 0) {
      throw new Error(`matrix row fails schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "comments-moderation-matrix.json"),
      `${JSON.stringify([row], null, 2)}\n`,
      "utf8",
    );
  });

  test(
    "submit lands in the moderation queue: the open surface accepts the probe and it is queued or auto-listed @dts",
    { tag: ["@dts", "@dts-comments"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `comments moderation (Comments): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const thread = await submitProbeComment(request, svc, probeThread());
      if (await publicListsMarker(request, svc, thread)) {
        // Auto-approve cell: the probe is already publicly listed.
        recordCase(CASE_SUBMIT);
        return;
      }
      const queued = await moderatorIdsFor(request, svc, thread, deliveryAuthHeader());
      expect(
        queued.length > 0,
        `comments: probe '${thread.marker}' must be queued for moderation once submitted`,
      ).toBe(true);
      recordCase(CASE_SUBMIT);
    },
  );

  test(
    "approve publishes: the gated approve surface makes the probe publicly visible @dts",
    { tag: ["@dts", "@dts-comments"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `comments moderation (Comments): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const thread = await submitProbeComment(request, svc, probeThread());
      const queued = await moderatorIdsFor(request, svc, thread, deliveryAuthHeader());
      expect(
        queued.length > 0,
        `comments: probe '${thread.marker}' must be queued before it can be approved`,
      ).toBe(true);
      await applyModeration(request, svc, "approve", queued, thread);
      expect(
        await publicListsMarker(request, svc, thread),
        `comments: probe '${thread.marker}' must be publicly listed after approve`,
      ).toBe(true);
      recordCase(CASE_APPROVE);
    },
  );

  test(
    "reject hides: the gated reject surface removes the probe from the public list @dts",
    { tag: ["@dts", "@dts-comments"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `comments moderation (Comments): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const thread = await submitProbeComment(request, svc, probeThread());
      const queued = await moderatorIdsFor(request, svc, thread, deliveryAuthHeader());
      expect(
        queued.length > 0,
        `comments: probe '${thread.marker}' must be queued before it can be rejected`,
      ).toBe(true);
      await applyModeration(request, svc, "reject", queued, thread);
      expect(
        await publicListsMarker(request, svc, thread),
        `comments: probe '${thread.marker}' must stay hidden from the public list after reject`,
      ).toBe(false);
      recordCase(CASE_REJECT);
    },
  );
});
