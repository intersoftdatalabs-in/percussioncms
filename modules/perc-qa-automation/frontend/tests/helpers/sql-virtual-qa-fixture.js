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
 * Copy the SQL Virtual Site QA fixture into the H2 Docker cell so
 * Developer Sites Build can POST /virtual/build against in-memory H2.
 *
 * The query is a single SELECT of literals (no CREATE/INSERT, no INIT/RUNSCRIPT)
 * so the CMS JVM can assemble pages without a pre-seeded mem database.
 */

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

/** Absolute POSIX path inside the Linux QA CMS container. */
const SQL_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/sql-virtual-qa";

/** Assembled home copied to the Site filesystem root after SQL Publish (Linux cell). */
const SQL_VIRTUAL_PUBLISHED_HTML = "8.2/index.html";

/** Marker from the in-memory H2 SELECT fixture body, present in published HTML. */
const SQL_VIRTUAL_PUBLISH_MARKER = "Hello from SQL.";

function qaCmsContainer() {
  const fromEnv = (
    process.env.QA_CMS_CONTAINER ||
    process.env.PERC_QA_CMS_CONTAINER ||
    "perc-matrix-cms-h2"
  ).trim();
  return fromEnv || "perc-matrix-cms-h2";
}

function sqlVirtualFixtureHostDir() {
  return path.join(__dirname, "..", "fixtures", "sql-virtual-site");
}

function dockerFailed(detail, err) {
  const msg = err && err.message ? err.message : String(err);
  return new Error(`SQL Virtual Site QA fixture Docker failed (${detail}): ${msg}`);
}

function dockerExec(container, args) {
  try {
    return execFileSync("docker", ["exec", container, ...args], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
  } catch (err) {
    throw dockerFailed(`docker exec ${container} ${args.join(" ")}`, err);
  }
}

function dockerCp(hostFile, containerDest) {
  try {
    execFileSync("docker", ["cp", hostFile, containerDest], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
  } catch (err) {
    throw dockerFailed(`docker cp ${hostFile} ${containerDest}`, err);
  }
}

/**
 * Place `_config.yaml` (required sql: mapping) and theme on the QA CMS host.
 * Replaces any previous tree at {@link SQL_VIRTUAL_QA_ROOT} so later runs do
 * not inherit stale files.
 *
 * @returns {string} in-container root path to save as virtual.rootPath
 */
function deploySqlVirtualFixtureToQaCell() {
  const hostDir = sqlVirtualFixtureHostDir();
  const config = path.join(hostDir, "_config.yaml");
  const theme = path.join(hostDir, "_theme", "page.html");
  for (const file of [config, theme]) {
    if (!fs.existsSync(file)) {
      throw new Error(`SQL Virtual Site fixture missing: ${file}`);
    }
  }
  const container = qaCmsContainer();
  dockerExec(container, ["rm", "-rf", SQL_VIRTUAL_QA_ROOT]);
  dockerExec(container, ["mkdir", "-p", `${SQL_VIRTUAL_QA_ROOT}/_theme`]);
  dockerCp(config, `${container}:${SQL_VIRTUAL_QA_ROOT}/_config.yaml`);
  dockerCp(theme, `${container}:${SQL_VIRTUAL_QA_ROOT}/_theme/page.html`);
  return SQL_VIRTUAL_QA_ROOT;
}

/**
 * Normalize a Developer Sites Publish destination path for the Linux QA cell.
 * Rejects empty, relative, drive-letter, and {@code ..} paths so docker exec
 * never follows a traversal. In-container filesystem paths always use {@code /}.
 *
 * @param {unknown} raw dest text from {@code developer-site-virtual-publish-dest}
 * @returns {string} POSIX absolute path
 */
function normalizeQaPublishDestPath(raw) {
  if (typeof raw !== "string") {
    throw new Error("SQL Virtual Site publish dest is missing");
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    throw new Error("SQL Virtual Site publish dest is blank");
  }
  const posix = trimmed.replace(/\\/g, "/");
  if (/^[a-zA-Z]:/.test(posix) || posix.startsWith("//")) {
    throw new Error(
      `SQL Virtual Site publish dest is not a Linux QA cell path: ${trimmed}`,
    );
  }
  if (!posix.startsWith("/")) {
    throw new Error(`SQL Virtual Site publish dest is not absolute: ${trimmed}`);
  }
  const parts = posix.split("/").filter((seg) => seg.length > 0);
  if (parts.some((seg) => seg === ".." || seg === ".")) {
    throw new Error(`SQL Virtual Site publish dest is unsafe: ${trimmed}`);
  }
  return `/${parts.join("/")}`;
}

function posixJoin(base, ...segments) {
  const dest = normalizeQaPublishDestPath(base);
  const extra = [];
  for (const seg of segments) {
    const piece = String(seg ?? "").trim().replace(/\\/g, "/");
    for (const part of piece.split("/")) {
      if (!part) {
        continue;
      }
      if (part === ".." || part === ".") {
        throw new Error(`SQL Virtual Site publish relpath is unsafe: ${seg}`);
      }
      extra.push(part);
    }
  }
  return extra.length === 0 ? dest : `${dest}/${extra.join("/")}`;
}

/**
 * Fail closed unless assembled SQL HTML exists under the Site filesystem root
 * inside the QA cell (acceptance: files exist after Publish).
 *
 * @param {unknown} publishPath dest shown in Developer Sites Publish chrome
 */
function assertPublishedSqlFilesOnQaCell(publishPath) {
  const html = posixJoin(publishPath, SQL_VIRTUAL_PUBLISHED_HTML);
  const container = qaCmsContainer();
  dockerExec(container, ["test", "-f", html]);
  const body = dockerExec(container, ["cat", html]);
  if (!body.includes(SQL_VIRTUAL_PUBLISH_MARKER) && !body.includes("SQL Home")) {
    throw new Error(
      `Published SQL HTML missing fixture marker at ${html}: ${body.slice(0, 400)}`,
    );
  }
  return html;
}

module.exports = {
  SQL_VIRTUAL_QA_ROOT,
  SQL_VIRTUAL_PUBLISHED_HTML,
  SQL_VIRTUAL_PUBLISH_MARKER,
  sqlVirtualFixtureHostDir,
  deploySqlVirtualFixtureToQaCell,
  normalizeQaPublishDestPath,
  posixJoin,
  assertPublishedSqlFilesOnQaCell,
  qaCmsContainer,
};
