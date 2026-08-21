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
 * Copy the CSV Virtual Site QA fixture into the H2 Docker cell so
 * Developer Sites Build/Publish can POST /virtual/build and /virtual/publish
 * against a real tree.
 */

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

/** Absolute POSIX path inside the Linux QA CMS container. */
const CSV_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/csv-virtual-qa-3697";

function qaCmsContainer() {
  const fromEnv = (
    process.env.QA_CMS_CONTAINER ||
    process.env.PERC_QA_CMS_CONTAINER ||
    "perc-matrix-cms-h2"
  ).trim();
  return fromEnv || "perc-matrix-cms-h2";
}

function csvVirtualFixtureHostDir() {
  return path.join(__dirname, "..", "fixtures", "csv-virtual-site");
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
 * Place `_config.yaml`, theme, and `8.2/pages.csv` on the QA CMS host.
 *
 * @returns {string} in-container root path to save as virtual.rootPath
 */
function deployCsvVirtualFixtureToQaCell() {
  const hostDir = csvVirtualFixtureHostDir();
  const pages = path.join(hostDir, "8.2", "pages.csv");
  const theme = path.join(hostDir, "_theme", "page.html");
  for (const file of [pages, theme]) {
    if (!fs.existsSync(file)) {
      throw new Error(`CSV Virtual Site fixture missing: ${file}`);
    }
  }
  const container = qaCmsContainer();
  // Match SitesAdaptorTest optional-_config.yaml CSV tree: version folder + pages.csv.
  // Theme is extra so assembled HTML has a layout file when the default layout is page.html.
  dockerExec(container, [
    "mkdir",
    "-p",
    `${CSV_VIRTUAL_QA_ROOT}/8.2`,
    `${CSV_VIRTUAL_QA_ROOT}/_theme`,
  ]);
  dockerExec(container, ["rm", "-f", `${CSV_VIRTUAL_QA_ROOT}/_config.yaml`]);
  dockerCp(theme, `${container}:${CSV_VIRTUAL_QA_ROOT}/_theme/page.html`);
  dockerCp(pages, `${container}:${CSV_VIRTUAL_QA_ROOT}/8.2/pages.csv`);
  return CSV_VIRTUAL_QA_ROOT;
}

module.exports = {
  CSV_VIRTUAL_QA_ROOT,
  csvVirtualFixtureHostDir,
  deployCsvVirtualFixtureToQaCell,
  qaCmsContainer,
};
