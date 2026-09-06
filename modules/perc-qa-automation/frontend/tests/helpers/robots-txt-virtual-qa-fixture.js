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
 * Copy the robots-txt Virtual Site QA fixture into the H2 Docker cell so
 * Developer Sites Build/Preview can POST /virtual/build and GET /virtual/preview
 * against a local robots.txt (no live crawl URLs or credentials). User-agent
 * {@code *} assembles {@code 8.2/star-1.html} (sole HTML home fallback).
 *
 * Bind-mount / docker cp only — no Jetty restart.
 */

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

/** Absolute POSIX path inside the Linux QA CMS container. */
const ROBOTS_TXT_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/robots-txt-virtual-qa";

/** Marker from the local robots.txt fixture body. */
const ROBOTS_TXT_VIRTUAL_BUILD_MARKER = "Hello-from-robots";

function qaCmsContainer() {
  const fromEnv = (
    process.env.QA_CMS_CONTAINER ||
    process.env.PERC_QA_CMS_CONTAINER ||
    "perc-matrix-cms-h2"
  ).trim();
  return fromEnv || "perc-matrix-cms-h2";
}

function robotsTxtVirtualFixtureHostDir() {
  return path.join(__dirname, "..", "fixtures", "robots-txt-virtual-site");
}

function dockerFailed(detail, err) {
  const msg = err && err.message ? err.message : String(err);
  return new Error(`robots-txt Virtual Site QA fixture Docker failed (${detail}): ${msg}`);
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
 * Place `_config.yaml`, `robots.txt`, and theme on the QA CMS host.
 * Replaces any previous tree at {@link ROBOTS_TXT_VIRTUAL_QA_ROOT}.
 *
 * @returns {string} in-container root path to save as virtual.rootPath
 */
function deployRobotsTxtVirtualFixtureToQaCell() {
  const hostDir = robotsTxtVirtualFixtureHostDir();
  const config = path.join(hostDir, "_config.yaml");
  const robots = path.join(hostDir, "robots.txt");
  const theme = path.join(hostDir, "_theme", "page.html");
  for (const file of [config, robots, theme]) {
    if (!fs.existsSync(file)) {
      throw new Error(`robots-txt Virtual Site fixture missing: ${file}`);
    }
  }
  const container = qaCmsContainer();
  dockerExec(container, ["rm", "-rf", ROBOTS_TXT_VIRTUAL_QA_ROOT]);
  dockerExec(container, ["mkdir", "-p", `${ROBOTS_TXT_VIRTUAL_QA_ROOT}/_theme`]);
  dockerCp(config, `${container}:${ROBOTS_TXT_VIRTUAL_QA_ROOT}/_config.yaml`);
  dockerCp(robots, `${container}:${ROBOTS_TXT_VIRTUAL_QA_ROOT}/robots.txt`);
  dockerCp(theme, `${container}:${ROBOTS_TXT_VIRTUAL_QA_ROOT}/_theme/page.html`);
  return ROBOTS_TXT_VIRTUAL_QA_ROOT;
}

module.exports = {
  deployRobotsTxtVirtualFixtureToQaCell,
  ROBOTS_TXT_VIRTUAL_QA_ROOT,
  ROBOTS_TXT_VIRTUAL_BUILD_MARKER,
};
