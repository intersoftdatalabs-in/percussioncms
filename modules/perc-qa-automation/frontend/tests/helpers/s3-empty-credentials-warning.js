/**
 * Pure helpers for Amazon S3 empty-credentials non-modal footer warning
 * (issue #2284 / PR residual Playwright + unit coverage).
 *
 * Mirrors the client-side logic in PercPublishMinuetView.js:
 * Access Key / Security Key empty → warn; Role ARN only when Assume Role is on.
 *
 * Product UI resolves copy via I18N.message(key[, args]) against CmsUi.tmx
 * keys under perc.ui.publish.servers.s3@*. Helpers accept an optional
 * messageFn so live CMS tests can bind to page I18N (any locale). When
 * omitted, en-us catalog strings matching CmsUi.tmx are used (unit tests /
 * offline).
 */

"use strict";

/** Product issue that introduced optional S3 keys + footer warning. */
const PRODUCT_ISSUE = 2284;

/**
 * CmsUi.tmx / I18N.message keys used by PercPublishMinuetView for #2284.
 * @readonly
 */
const MESSAGE_KEYS = {
  accessKey: "perc.ui.publish.servers.s3@Access Key",
  securityKey: "perc.ui.publish.servers.s3@Security Key",
  roleArn: "perc.ui.publish.servers.s3@Role ARN",
  emptyCredentialsWarning:
    "perc.ui.publish.servers.s3@Empty credentials warning",
};

/**
 * en-us catalog matching modules/perc-i18n/.../CmsUi.tmx (issue #2284).
 * Used when no page-bound I18N messageFn is provided.
 * @readonly
 */
const EN_US_CATALOG = {
  [MESSAGE_KEYS.accessKey]: "Access Key",
  [MESSAGE_KEYS.securityKey]: "Security Key",
  [MESSAGE_KEYS.roleArn]: "Role ARN",
  [MESSAGE_KEYS.emptyCredentialsWarning]:
    "Amazon S3 fields are empty ({0}). Save will proceed; ensure EC2 instance profile, Assume Role, or other AWS credentials are available. On Amazon Linux 2023+ IMDS uses tokens (HttpTokens=required); containers need HttpPutResponseHopLimit >= 2.",
};

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
 * Apply `{0}`, `{1}`, … placeholders the same way product I18N.message does.
 * @param {string} template
 * @param {string[]|undefined} args
 * @returns {string}
 */
function formatMessage(template, args) {
  let s = String(template);
  const list = Array.isArray(args) ? args : [];
  for (let i = 0; i < list.length; i++) {
    s = s.split("{" + i + "}").join(String(list[i]));
  }
  return s;
}

/**
 * Build a messageFn from a key→template catalog (en-us or custom).
 * @param {Record<string, string>} catalog
 * @returns {(key: string, args?: string[]) => string}
 */
function messageFnFromCatalog(catalog) {
  const cat = catalog || EN_US_CATALOG;
  return function message(key, args) {
    const template = Object.prototype.hasOwnProperty.call(cat, key)
      ? cat[key]
      : key;
    return formatMessage(template, args);
  };
}

/** Default en-us messageFn (CmsUi.tmx lock-step). */
const defaultMessage = messageFnFromCatalog(EN_US_CATALOG);

/**
 * Bind a Playwright page's live I18N.message to a messageFn.
 * Falls back to en-us catalog when I18N is not available on the page.
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<(key: string, args?: string[]) => string>}
 */
async function messageFnFromPage(page) {
  const keys = Object.values(MESSAGE_KEYS);
  const resolved = await page.evaluate((keyList) => {
    const hasI18n =
      typeof I18N !== "undefined" && typeof I18N.message === "function";
    if (!hasI18n) {
      return { hasI18n: false, catalog: null };
    }
    // Capture templates: for the warning key, leave {0} by substituting a
    // sentinel then restoring so offline formatMessage can re-apply args.
    const catalog = {};
    for (const k of keyList) {
      if (k.indexOf("Empty credentials warning") !== -1) {
        const withSentinel = I18N.message(k, ["\u0001"]);
        catalog[k] = String(withSentinel).split("\u0001").join("{0}");
      } else {
        catalog[k] = I18N.message(k);
      }
    }
    return { hasI18n: true, catalog: catalog };
  }, keys);

  if (!resolved || !resolved.hasI18n || !resolved.catalog) {
    return defaultMessage;
  }
  return messageFnFromCatalog(resolved.catalog);
}

/**
 * Collect empty S3 credential field labels for the non-modal footer warning.
 *
 * @param {{ accessKey?: string|null, secretKey?: string|null, arnRole?: string|null, useAssumeRole?: boolean }} fields
 * @param {(key: string, args?: string[]) => string} [messageFn=defaultMessage] I18N resolver
 * @returns {string[]} ordered labels matching product UI (localized when messageFn is)
 */
function collectEmptyS3CredentialFields(fields, messageFn) {
  const msg = typeof messageFn === "function" ? messageFn : defaultMessage;
  const f = fields || {};
  const empty = [];
  if (!String(f.accessKey || "").trim()) {
    empty.push(msg(MESSAGE_KEYS.accessKey));
  }
  if (!String(f.secretKey || "").trim()) {
    empty.push(msg(MESSAGE_KEYS.securityKey));
  }
  if (f.useAssumeRole && !String(f.arnRole || "").trim()) {
    empty.push(msg(MESSAGE_KEYS.roleArn));
  }
  return empty;
}

/**
 * Build the footer warning body shown via processAlert / templateResponseFooterAlert.
 *
 * @param {string[]} emptyFields labels from {@link collectEmptyS3CredentialFields}
 * @param {(key: string, args?: string[]) => string} [messageFn=defaultMessage] I18N resolver
 * @returns {string|null} warning text, or null when nothing is empty
 */
function buildS3EmptyCredentialsWarning(emptyFields, messageFn) {
  const msg = typeof messageFn === "function" ? messageFn : defaultMessage;
  if (!Array.isArray(emptyFields) || emptyFields.length === 0) {
    return null;
  }
  return msg(MESSAGE_KEYS.emptyCredentialsWarning, [emptyFields.join(", ")]);
}

/**
 * True when a footer alert text looks like the #2284 S3 empty-credentials warning
 * for the given locale catalog (default en-us).
 *
 * Matching uses ordered template fragments around the `{0}` field-list slot so
 * non-English catalogs work when messageFn is page-bound.
 *
 * @param {string|null|undefined} text
 * @param {(key: string, args?: string[]) => string} [messageFn=defaultMessage]
 * @returns {boolean}
 */
function isS3EmptyCredentialsWarningText(text, messageFn) {
  const msg = typeof messageFn === "function" ? messageFn : defaultMessage;
  const t = String(text || "");
  if (!t) {
    return false;
  }
  const withSentinel = msg(MESSAGE_KEYS.emptyCredentialsWarning, ["\u0001"]);
  const parts = String(withSentinel).split("\u0001");
  if (parts.length === 1) {
    return t.includes(withSentinel);
  }
  let idx = 0;
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i];
    if (!part) {
      continue;
    }
    const found = t.indexOf(part, idx);
    if (found < 0) {
      return false;
    }
    idx = found + part.length;
  }
  return true;
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
  MESSAGE_KEYS,
  EN_US_CATALOG,
  SELECTORS,
  formatMessage,
  messageFnFromCatalog,
  messageFnFromPage,
  defaultMessage,
  collectEmptyS3CredentialFields,
  buildS3EmptyCredentialsWarning,
  isS3EmptyCredentialsWarningText,
  s3WarningSurfaceSkipReason,
};
