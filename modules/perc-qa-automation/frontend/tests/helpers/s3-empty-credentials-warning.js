/**
 * Pure helpers for Amazon S3 empty-credentials non-modal footer warning
 * (issue #2284 / PR residual Playwright + unit coverage).
 *
 * Mirrors the client-side logic in PercPublishMinuetView.js:
 * Access Key / Security Key empty → warn; Role ARN only when Assume Role is on.
 * Message text must stay in lock-step with product UI en-us TMX strings
 * (keys under perc.ui.publish.servers.s3@* in CmsUi.tmx; UI resolves via I18N.message).
 */

"use strict";

/** Product issue that introduced optional S3 keys + footer warning. */
const PRODUCT_ISSUE = 2284;

/** Stable DOM anchors used by the minuet publish server editor + footer. */
const SELECTORS = {
  accessKey: "#perc-access-key",
  securityKey: "#perc-security-key",
  arnRole: "#ARNRole",
  useAssumeRole: "#useAssumeRole",
  optionalCredentialsHint: "#perc-s3-optional-credentials-hint",
  footerAlert: "#percFooterAlertTarget",
  footerAlertMessage: "#percFooterAlertTarget .alert",
  publishRoot: "#perc-publishing-root, #perc-publish-body-target",
};

/**
 * Collect empty S3 credential field labels for the non-modal footer warning.
 *
 * @param {{ accessKey?: string|null, secretKey?: string|null, arnRole?: string|null, useAssumeRole?: boolean }} fields
 * @returns {string[]} ordered labels matching product UI
 */
function collectEmptyS3CredentialFields(fields) {
  const f = fields || {};
  const empty = [];
  if (!String(f.accessKey || "").trim()) {
    empty.push("Access Key");
  }
  if (!String(f.secretKey || "").trim()) {
    empty.push("Security Key");
  }
  if (f.useAssumeRole && !String(f.arnRole || "").trim()) {
    empty.push("Role ARN");
  }
  return empty;
}

/**
 * Build the footer warning body shown via processAlert / templateResponseFooterAlert.
 *
 * @param {string[]} emptyFields labels from {@link collectEmptyS3CredentialFields}
 * @returns {string|null} warning text, or null when nothing is empty
 */
function buildS3EmptyCredentialsWarning(emptyFields) {
  if (!Array.isArray(emptyFields) || emptyFields.length === 0) {
    return null;
  }
  return (
    "Amazon S3 fields are empty (" +
    emptyFields.join(", ") +
    "). Save will proceed; ensure EC2 instance profile, Assume Role, or other AWS credentials are available. On Amazon Linux 2023+ IMDS uses tokens (HttpTokens=required); containers need HttpPutResponseHopLimit >= 2."
  );
}

/**
 * True when a footer alert text looks like the #2284 S3 empty-credentials warning.
 * @param {string|null|undefined} text
 * @returns {boolean}
 */
function isS3EmptyCredentialsWarningText(text) {
  const t = String(text || "");
  return (
    t.includes("Amazon S3 fields are empty") &&
    t.includes("Save will proceed") &&
    t.includes("HttpPutResponseHopLimit")
  );
}

/**
 * Durable skip-with-BUG reason when live CMS publish S3 surface is unavailable.
 * @param {string} detail
 * @returns {string}
 */
function s3WarningSurfaceSkipReason(detail) {
  return (
    `skip-with-BUG: publish S3 empty-credentials footer surface unavailable (${detail}). ` +
    `Product issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/${PRODUCT_ISSUE}`
  );
}

module.exports = {
  PRODUCT_ISSUE,
  SELECTORS,
  collectEmptyS3CredentialFields,
  buildS3EmptyCredentialsWarning,
  isS3EmptyCredentialsWarningText,
  s3WarningSurfaceSkipReason,
};
