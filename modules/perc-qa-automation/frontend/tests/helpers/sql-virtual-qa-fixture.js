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
const SQL_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/sql-virtual-qa-3759";

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

function dockerExec(container, args) {
  return execFileSync("docker", ["exec", container, ...args], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function dockerCp(hostFile, containerDest) {
  execFileSync("docker", ["cp", hostFile, containerDest], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

/**
 * Place `_config.yaml` (required sql: mapping) and theme on the QA CMS host.
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
  dockerExec(container, [
    "mkdir",
    "-p",
    `${SQL_VIRTUAL_QA_ROOT}/8.2`,
    `${SQL_VIRTUAL_QA_ROOT}/_theme`,
  ]);
  dockerCp(config, `${container}:${SQL_VIRTUAL_QA_ROOT}/_config.yaml`);
  dockerCp(theme, `${container}:${SQL_VIRTUAL_QA_ROOT}/_theme/page.html`);
  return SQL_VIRTUAL_QA_ROOT;
}

module.exports = {
  SQL_VIRTUAL_QA_ROOT,
  sqlVirtualFixtureHostDir,
  deploySqlVirtualFixtureToQaCell,
  qaCmsContainer,
};
