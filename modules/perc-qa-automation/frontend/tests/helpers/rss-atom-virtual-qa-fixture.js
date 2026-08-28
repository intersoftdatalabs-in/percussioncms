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
 * Copy the rss-atom Virtual Site QA fixture into the H2 Docker cell so
 * Developer Sites Build and Preview can POST /virtual/build then GET
 * /virtual/preview against a local RSS 2.0 feed (no live feed URLs or
 * credentials). Feed item id {@code index} assembles {@code 8.2/index.html}.
 */

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

/** Absolute POSIX path inside the Linux QA CMS container. */
const RSS_ATOM_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/rss-atom-virtual-qa";

/** Marker from the local feed.xml fixture body. */
const RSS_ATOM_VIRTUAL_BUILD_MARKER = "Hello from RSS.";

function qaCmsContainer() {
  const fromEnv = (
    process.env.QA_CMS_CONTAINER ||
    process.env.PERC_QA_CMS_CONTAINER ||
    "perc-matrix-cms-h2"
  ).trim();
  return fromEnv || "perc-matrix-cms-h2";
}

function rssAtomVirtualFixtureHostDir() {
  return path.join(__dirname, "..", "fixtures", "rss-atom-virtual-site");
}

function dockerFailed(detail, err) {
  const msg = err && err.message ? err.message : String(err);
  return new Error(`rss-atom Virtual Site QA fixture Docker failed (${detail}): ${msg}`);
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
 * Place `_config.yaml`, `feed.xml`, and theme on the QA CMS host.
 * Replaces any previous tree at {@link RSS_ATOM_VIRTUAL_QA_ROOT}.
 *
 * @returns {string} in-container root path to save as virtual.rootPath
 */
function deployRssAtomVirtualFixtureToQaCell() {
  const hostDir = rssAtomVirtualFixtureHostDir();
  const config = path.join(hostDir, "_config.yaml");
  const feed = path.join(hostDir, "feed.xml");
  const theme = path.join(hostDir, "_theme", "page.html");
  for (const file of [config, feed, theme]) {
    if (!fs.existsSync(file)) {
      throw new Error(`rss-atom Virtual Site fixture missing: ${file}`);
    }
  }
  const container = qaCmsContainer();
  dockerExec(container, ["rm", "-rf", RSS_ATOM_VIRTUAL_QA_ROOT]);
  dockerExec(container, ["mkdir", "-p", `${RSS_ATOM_VIRTUAL_QA_ROOT}/_theme`]);
  dockerCp(config, `${container}:${RSS_ATOM_VIRTUAL_QA_ROOT}/_config.yaml`);
  dockerCp(feed, `${container}:${RSS_ATOM_VIRTUAL_QA_ROOT}/feed.xml`);
  dockerCp(theme, `${container}:${RSS_ATOM_VIRTUAL_QA_ROOT}/_theme/page.html`);
  return RSS_ATOM_VIRTUAL_QA_ROOT;
}

module.exports = {
  RSS_ATOM_VIRTUAL_QA_ROOT,
  RSS_ATOM_VIRTUAL_BUILD_MARKER,
  rssAtomVirtualFixtureHostDir,
  deployRssAtomVirtualFixtureToQaCell,
  qaCmsContainer,
};
