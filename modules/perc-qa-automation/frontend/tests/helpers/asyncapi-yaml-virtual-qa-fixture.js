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
 * Copy the asyncapi-yaml Virtual Site QA fixture into the H2 Docker cell so
 * Developer Sites Build/Preview/Publish can POST /virtual/build, GET /virtual/preview,
 * and POST /virtual/publish against a local AsyncAPI 2 YAML file (no live spec fetch
 * URLs or credentials). A single PUBLISH light/measured operation assembles
 * {@code 8.2/onLightMeasured-1.html} (sole HTML home fallback; Publish copies that
 * file to IPSSite.root).
 *
 * Bind-mount / docker cp only — no Jetty restart.
 */

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

/** Absolute POSIX path inside the Linux QA CMS container. */
const ASYNCAPI_YAML_VIRTUAL_QA_ROOT = "/opt/Percussion/tmp/asyncapi-yaml-virtual-qa";

/** Marker from the local asyncapi.yaml fixture description. */
const ASYNCAPI_YAML_VIRTUAL_BUILD_MARKER = "Hello-from-asyncapi";

/** Assembled operation page copied to the Site filesystem root after Publish. */
const ASYNCAPI_YAML_VIRTUAL_PUBLISHED_HTML = "8.2/onLightMeasured-1.html";

/** Marker expected in published HTML (same as the AsyncAPI description token). */
const ASYNCAPI_YAML_VIRTUAL_PUBLISH_MARKER = ASYNCAPI_YAML_VIRTUAL_BUILD_MARKER;

function qaCmsContainer() {
  const fromEnv = (
    process.env.QA_CMS_CONTAINER ||
    process.env.PERC_QA_CMS_CONTAINER ||
    "perc-matrix-cms-h2"
  ).trim();
  return fromEnv || "perc-matrix-cms-h2";
}

function asyncApiYamlVirtualFixtureHostDir() {
  return path.join(__dirname, "..", "fixtures", "asyncapi-yaml-virtual-site");
}

function dockerFailed(detail, err) {
  const msg = err && err.message ? err.message : String(err);
  return new Error(`asyncapi-yaml Virtual Site QA fixture Docker failed (${detail}): ${msg}`);
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
 * Place `_config.yaml`, `asyncapi.yaml`, and theme on the QA CMS host.
 * Replaces any previous tree at {@link ASYNCAPI_YAML_VIRTUAL_QA_ROOT}.
 *
 * @returns {string} in-container root path to save as virtual.rootPath
 */
function deployAsyncApiYamlVirtualFixtureToQaCell() {
  const hostDir = asyncApiYamlVirtualFixtureHostDir();
  const config = path.join(hostDir, "_config.yaml");
  const spec = path.join(hostDir, "asyncapi.yaml");
  const theme = path.join(hostDir, "_theme", "page.html");
  for (const file of [config, spec, theme]) {
    if (!fs.existsSync(file)) {
      throw new Error(`asyncapi-yaml Virtual Site fixture missing: ${file}`);
    }
  }
  const container = qaCmsContainer();
  dockerExec(container, ["rm", "-rf", ASYNCAPI_YAML_VIRTUAL_QA_ROOT]);
  dockerExec(container, ["mkdir", "-p", `${ASYNCAPI_YAML_VIRTUAL_QA_ROOT}/_theme`]);
  dockerCp(config, `${container}:${ASYNCAPI_YAML_VIRTUAL_QA_ROOT}/_config.yaml`);
  dockerCp(spec, `${container}:${ASYNCAPI_YAML_VIRTUAL_QA_ROOT}/asyncapi.yaml`);
  dockerCp(theme, `${container}:${ASYNCAPI_YAML_VIRTUAL_QA_ROOT}/_theme/page.html`);
  return ASYNCAPI_YAML_VIRTUAL_QA_ROOT;
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
    throw new Error("asyncapi-yaml Virtual Site publish dest is missing");
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    throw new Error("asyncapi-yaml Virtual Site publish dest is blank");
  }
  const posix = trimmed.replace(/\\/g, "/");
  if (/^[a-zA-Z]:/.test(posix) || posix.startsWith("//")) {
    throw new Error(
      `asyncapi-yaml Virtual Site publish dest is not a Linux QA cell path: ${trimmed}`,
    );
  }
  if (!posix.startsWith("/")) {
    throw new Error(`asyncapi-yaml Virtual Site publish dest is not absolute: ${trimmed}`);
  }
  const parts = posix.split("/").filter((seg) => seg.length > 0);
  if (parts.some((seg) => seg === ".." || seg === ".")) {
    throw new Error(`asyncapi-yaml Virtual Site publish dest is unsafe: ${trimmed}`);
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
        throw new Error(`asyncapi-yaml Virtual Site publish relpath is unsafe: ${seg}`);
      }
      extra.push(part);
    }
  }
  return extra.length === 0 ? dest : `${dest}/${extra.join("/")}`;
}

/**
 * Fail closed unless assembled asyncapi-yaml HTML exists under the Site
 * filesystem root inside the QA cell (acceptance: files exist after Publish).
 *
 * @param {unknown} publishPath dest shown in Developer Sites Publish chrome
 */
function assertPublishedAsyncApiYamlFilesOnQaCell(publishPath) {
  const html = posixJoin(publishPath, ASYNCAPI_YAML_VIRTUAL_PUBLISHED_HTML);
  const container = qaCmsContainer();
  dockerExec(container, ["test", "-f", html]);
  const body = dockerExec(container, ["cat", html]);
  if (
    !body.includes(ASYNCAPI_YAML_VIRTUAL_PUBLISH_MARKER) &&
    !body.includes("Inform about lighting") &&
    !body.includes("onLightMeasured")
  ) {
    throw new Error(
      `Published asyncapi-yaml HTML missing fixture marker at ${html}: ${body.slice(0, 400)}`,
    );
  }
  return html;
}

module.exports = {
  deployAsyncApiYamlVirtualFixtureToQaCell,
  assertPublishedAsyncApiYamlFilesOnQaCell,
  ASYNCAPI_YAML_VIRTUAL_QA_ROOT,
  ASYNCAPI_YAML_VIRTUAL_BUILD_MARKER,
  ASYNCAPI_YAML_VIRTUAL_PUBLISHED_HTML,
  ASYNCAPI_YAML_VIRTUAL_PUBLISH_MARKER,
};
