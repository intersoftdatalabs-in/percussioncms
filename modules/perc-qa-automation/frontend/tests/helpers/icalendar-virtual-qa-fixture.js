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
 * Copy the icalendar Virtual Site QA fixture into the H2 Docker cell so
 * Developer Sites Build, Preview, and Publish can POST /virtual/build,
 * GET /virtual/preview, then POST /virtual/publish against a local RFC 5545
 * .ics (no CalDAV URLs or credentials). Event UID {@code index}
 * assembles {@code 8.2/index.html}.
 */

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

/** Absolute POSIX path inside the Linux QA CMS container. */
const ICALENDAR_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/icalendar-virtual-qa";

/** Marker from the local calendar.ics fixture body. */
const ICALENDAR_VIRTUAL_BUILD_MARKER = "Hello from iCalendar.";

/** Assembled home copied to the Site filesystem root after Publish. */
const ICALENDAR_VIRTUAL_PUBLISHED_HTML = "8.2/index.html";

/** Marker expected in published HTML (same as the event description). */
const ICALENDAR_VIRTUAL_PUBLISH_MARKER = ICALENDAR_VIRTUAL_BUILD_MARKER;

function qaCmsContainer() {
  const fromEnv = (
    process.env.QA_CMS_CONTAINER ||
    process.env.PERC_QA_CMS_CONTAINER ||
    "perc-matrix-cms-h2"
  ).trim();
  return fromEnv || "perc-matrix-cms-h2";
}

function icalendarVirtualFixtureHostDir() {
  return path.join(__dirname, "..", "fixtures", "icalendar-virtual-site");
}

function dockerFailed(detail, err) {
  const msg = err && err.message ? err.message : String(err);
  return new Error(`icalendar Virtual Site QA fixture Docker failed (${detail}): ${msg}`);
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
 * Place `_config.yaml`, `calendar.ics`, and theme on the QA CMS host.
 * Replaces any previous tree at {@link ICALENDAR_VIRTUAL_QA_ROOT}.
 *
 * @returns {string} in-container root path to save as virtual.rootPath
 */
function deployIcalendarVirtualFixtureToQaCell() {
  const hostDir = icalendarVirtualFixtureHostDir();
  const config = path.join(hostDir, "_config.yaml");
  const ics = path.join(hostDir, "calendar.ics");
  const theme = path.join(hostDir, "_theme", "page.html");
  for (const file of [config, ics, theme]) {
    if (!fs.existsSync(file)) {
      throw new Error(`icalendar Virtual Site fixture missing: ${file}`);
    }
  }
  const container = qaCmsContainer();
  dockerExec(container, ["rm", "-rf", ICALENDAR_VIRTUAL_QA_ROOT]);
  dockerExec(container, ["mkdir", "-p", `${ICALENDAR_VIRTUAL_QA_ROOT}/_theme`]);
  dockerCp(config, `${container}:${ICALENDAR_VIRTUAL_QA_ROOT}/_config.yaml`);
  dockerCp(ics, `${container}:${ICALENDAR_VIRTUAL_QA_ROOT}/calendar.ics`);
  dockerCp(theme, `${container}:${ICALENDAR_VIRTUAL_QA_ROOT}/_theme/page.html`);
  return ICALENDAR_VIRTUAL_QA_ROOT;
}

/**
 * Normalize Developer Sites publish dest text to a POSIX absolute path inside
 * the Linux QA cell. Rejects Windows drive letters, UNC, relatives, and
 * remaining {@code ..} so {@code docker exec … cat} never follows a traversal.
 * In-container filesystem paths always use {@code /}.
 *
 * @param {unknown} raw dest text from {@code developer-site-virtual-publish-dest}
 * @returns {string} POSIX absolute path
 */
function normalizeQaPublishDestPath(raw) {
  if (typeof raw !== "string") {
    throw new Error("icalendar Virtual Site publish dest is missing");
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    throw new Error("icalendar Virtual Site publish dest is blank");
  }
  const posix = trimmed.replace(/\\/g, "/");
  if (/^[a-zA-Z]:/.test(posix) || posix.startsWith("//")) {
    throw new Error(
      `icalendar Virtual Site publish dest is not a Linux QA cell path: ${trimmed}`,
    );
  }
  if (!posix.startsWith("/")) {
    throw new Error(`icalendar Virtual Site publish dest is not absolute: ${trimmed}`);
  }
  const parts = posix.split("/").filter((seg) => seg.length > 0);
  if (parts.some((seg) => seg === ".." || seg === ".")) {
    throw new Error(`icalendar Virtual Site publish dest is unsafe: ${trimmed}`);
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
        throw new Error(`icalendar Virtual Site publish relpath is unsafe: ${seg}`);
      }
      extra.push(part);
    }
  }
  return extra.length === 0 ? dest : `${dest}/${extra.join("/")}`;
}

/**
 * Fail closed unless assembled icalendar HTML exists under the Site
 * filesystem root inside the QA cell (acceptance: files exist after Publish).
 *
 * @param {unknown} publishPath dest shown in Developer Sites Publish chrome
 */
function assertPublishedIcalendarFilesOnQaCell(publishPath) {
  const html = posixJoin(publishPath, ICALENDAR_VIRTUAL_PUBLISHED_HTML);
  const container = qaCmsContainer();
  dockerExec(container, ["test", "-f", html]);
  const body = dockerExec(container, ["cat", html]);
  if (!body.includes(ICALENDAR_VIRTUAL_PUBLISH_MARKER) && !body.includes("iCalendar Home")) {
    throw new Error(
      `Published icalendar HTML missing fixture marker at ${html}: ${body.slice(0, 400)}`,
    );
  }
  return html;
}

module.exports = {
  ICALENDAR_VIRTUAL_QA_ROOT,
  ICALENDAR_VIRTUAL_BUILD_MARKER,
  ICALENDAR_VIRTUAL_PUBLISHED_HTML,
  ICALENDAR_VIRTUAL_PUBLISH_MARKER,
  icalendarVirtualFixtureHostDir,
  deployIcalendarVirtualFixtureToQaCell,
  normalizeQaPublishDestPath,
  posixJoin,
  assertPublishedIcalendarFilesOnQaCell,
  qaCmsContainer,
};
