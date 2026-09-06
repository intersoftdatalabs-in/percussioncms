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
 * Unit tests for Developer Pipelines OpenAPI catalog ranking (#4384).
 * No live CMS.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  OPENAPI_CANDIDATE_LIMIT,
  rankPipelineNamesForOpenApi,
  openApiCandidateNames,
  openApiHasResourceExecutePath,
  isPipelineOpenApiGetUrl,
  pipelineOpenApiRestUrl,
  staleOpenApiChromeMessage,
  noExecutePathMessage,
} = require("../helpers/developer-pipelines-openapi-surface");

describe("rankPipelineNamesForOpenApi", () => {
  it("prefers sys_cmp* IR apps over sys_ActionPage", () => {
    const ranked = rankPipelineNamesForOpenApi([
      "sys_ActionPage",
      "sys_cmpDocuments",
      "sys_cx",
      "customApp",
    ]);
    assert.equal(ranked[0], "sys_cmpDocuments");
    assert.ok(ranked.indexOf("sys_ActionPage") > ranked.indexOf("customApp"));
  });

  it("dedupes case-insensitively and skips blanks", () => {
    const ranked = rankPipelineNamesForOpenApi([
      " sys_cmpDocuments ",
      "SYS_CMPDOCUMENTS",
      "",
      "  ",
      12,
    ]);
    assert.deepEqual(ranked, ["sys_cmpDocuments"]);
  });

  it("rejects non-arrays", () => {
    assert.throws(() => rankPipelineNamesForOpenApi("sys_cmpDocuments"), TypeError);
  });
});

describe("openApiCandidateNames", () => {
  it("uses PIPELINE_APP_NAME exclusively when set", () => {
    assert.deepEqual(
      openApiCandidateNames(["sys_cmpDocuments", "other"], " sys_ActionPage "),
      ["sys_ActionPage"],
    );
  });

  it("caps ranked names at OPENAPI_CANDIDATE_LIMIT", () => {
    const names = [];
    for (let i = 0; i < 20; i++) {
      names.push(`sys_cmpApp${i}`);
    }
    const candidates = openApiCandidateNames(names, "", OPENAPI_CANDIDATE_LIMIT);
    assert.equal(candidates.length, OPENAPI_CANDIDATE_LIMIT);
    assert.equal(candidates[0], "sys_cmpApp0");
  });
});

describe("openApiHasResourceExecutePath", () => {
  it("matches OpenAPI execute paths and rejects empty-IR docs", () => {
    assert.equal(
      openApiHasResourceExecutePath(
        'openapi: "3.0.3"\npaths:\n  /pipelines/sys_cmpDocuments/resources/contenteditor/execute:\n',
      ),
      true,
    );
    assert.equal(openApiHasResourceExecutePath('openapi: "3.0.3"\npaths: {}\n'), false);
    assert.equal(openApiHasResourceExecutePath(""), false);
  });
});

describe("isPipelineOpenApiGetUrl", () => {
  it("matches GET openapi for the catalog token only", () => {
    assert.equal(
      isPipelineOpenApiGetUrl(
        "http://127.0.0.1:9993/Rhythmyx/services/pipelines/sys_cmpDocuments/openapi?format=yaml",
        "sys_cmpDocuments",
      ),
      true,
    );
    assert.equal(
      isPipelineOpenApiGetUrl(
        "http://127.0.0.1:9993/Rhythmyx/services/pipelines/sys_ActionPage/openapi",
        "sys_cmpDocuments",
      ),
      false,
    );
    assert.equal(
      isPipelineOpenApiGetUrl("/services/pipelines/sys_cmpDocuments/ir", "sys_cmpDocuments"),
      false,
    );
  });

  it("does not treat names as filesystem paths", () => {
    assert.equal(isPipelineOpenApiGetUrl("C:\\\\temp\\\\openapi", "sys_cmpDocuments"), false);
    assert.equal(isPipelineOpenApiGetUrl("/tmp/openapi", "sys_cmpDocuments"), false);
  });
});

describe("pipelineOpenApiRestUrl", () => {
  it("uses Rhythmyx services root when the SPA path is under /Rhythmyx", () => {
    assert.equal(
      pipelineOpenApiRestUrl(
        "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=developer",
        "sys_cmpDocuments",
      ),
      "http://127.0.0.1:9993/Rhythmyx/services/pipelines/sys_cmpDocuments/openapi?format=yaml",
    );
  });

  it("encodes catalog tokens and does not join OS separators", () => {
    const url = pipelineOpenApiRestUrl("http://127.0.0.1:9993/", "App Name");
    assert.equal(
      url,
      "http://127.0.0.1:9993/services/pipelines/App%20Name/openapi?format=yaml",
    );
    assert.equal(url.includes("\\"), false);
  });
});

describe("messages", () => {
  it("mentions Slice C SPA deploy and PIPELINE_APP_NAME", () => {
    assert.match(staleOpenApiChromeMessage(["sys_ActionPage"]), /qa-deploy-webui/);
    assert.match(noExecutePathMessage(["sys_ActionPage"]), /PIPELINE_APP_NAME/);
  });
});
