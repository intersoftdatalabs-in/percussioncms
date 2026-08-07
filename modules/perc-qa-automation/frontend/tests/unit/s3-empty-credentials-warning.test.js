/**
 * Unit tests for S3 empty-credentials footer warning pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 *   node --test tests/unit/s3-empty-credentials-warning.test.js
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  PRODUCT_ISSUE,
  SELECTORS,
  collectEmptyS3CredentialFields,
  buildS3EmptyCredentialsWarning,
  isS3EmptyCredentialsWarningText,
  s3WarningSurfaceSkipReason,
} = require("../helpers/s3-empty-credentials-warning");

describe("s3-empty-credentials-warning helpers", () => {
  it("exposes product issue and footer selectors", () => {
    assert.equal(PRODUCT_ISSUE, 2284);
    assert.equal(SELECTORS.accessKey, "#perc-access-key");
    assert.equal(SELECTORS.footerAlert, "#percFooterAlertTarget");
  });

  it("collectEmptyS3CredentialFields lists Access/Security when blank", () => {
    assert.deepEqual(
      collectEmptyS3CredentialFields({
        accessKey: "",
        secretKey: "  ",
        arnRole: "",
        useAssumeRole: false,
      }),
      ["Access Key", "Security Key"],
    );
  });

  it("collectEmptyS3CredentialFields includes Role ARN only when Assume Role is on", () => {
    assert.deepEqual(
      collectEmptyS3CredentialFields({
        accessKey: "AKIA",
        secretKey: "secret",
        arnRole: "",
        useAssumeRole: true,
      }),
      ["Role ARN"],
    );
    assert.deepEqual(
      collectEmptyS3CredentialFields({
        accessKey: "AKIA",
        secretKey: "secret",
        arnRole: "",
        useAssumeRole: false,
      }),
      [],
    );
  });

  it("buildS3EmptyCredentialsWarning matches product footer copy", () => {
    assert.equal(buildS3EmptyCredentialsWarning([]), null);
    assert.equal(buildS3EmptyCredentialsWarning(null), null);
    const msg = buildS3EmptyCredentialsWarning(["Access Key", "Security Key"]);
    assert.ok(msg.includes("Amazon S3 fields are empty (Access Key, Security Key)"));
    assert.ok(msg.includes("Save will proceed"));
    assert.ok(msg.includes("HttpPutResponseHopLimit >= 2"));
    assert.ok(isS3EmptyCredentialsWarningText(msg));
  });

  it("skip-with-BUG reason embeds durable issue URL", () => {
    const reason = s3WarningSurfaceSkipReason("no Add Server");
    assert.ok(reason.includes("skip-with-BUG"));
    // Parse the embedded URL (host + path) instead of substring-matching the
    // full URL string — avoids js/incomplete-url-substring-sanitization.
    const urlToken = reason.match(/https:\/\/\S+/)?.[0];
    assert.ok(urlToken, "expected absolute product-issue URL in skip reason");
    const u = new URL(urlToken);
    assert.equal(u.hostname, "github.com");
    assert.equal(
      u.pathname,
      `/intersoftdatalabs-in/percussioncms/issues/${PRODUCT_ISSUE}`,
    );
  });
});
