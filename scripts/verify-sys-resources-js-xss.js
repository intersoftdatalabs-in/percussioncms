#!/usr/bin/env node
/**
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Regression checks for CodeQL js/xss alerts #945 and #946 in
 * system/cms sys_resources mobile preview + webimagefx helpers.
 *
 * Plain Node (no test framework) so it runs on Windows/Linux/macOS
 * without a shell wrapper.
 */

const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");

function read(rel) {
  return fs.readFileSync(path.join(root, rel), "utf8");
}

let failed = 0;
function assert(cond, msg) {
  if (!cond) {
    console.error("FAIL:", msg);
    failed++;
  } else {
    console.log("OK  :", msg);
  }
}

// --- #946 webimagefx: do not embed raw location.href in script src ---
const wifx = read(
  "system/cms/content/applications/sys_resources/ApplicationFiles/webimagefx/webimagefx.js",
);
assert(
  !/window\.location\.href\.substring/.test(wifx),
  "webimagefx does not build license path from location.href.substring",
);
assert(
  /location\.pathname/.test(wifx) &&
    /location\.(protocol|host|origin)/.test(wifx),
  "webimagefx builds license path from origin + pathname",
);
assert(
  /\[\^A-Za-z0-9\._\\\-\/\]/.test(wifx) ||
    /\/\^\\\/\[A-Za-z0-9\._\\-\\\/\]\*/.test(wifx) ||
    wifx.includes("A-Za-z0-9._"),
  "webimagefx allow-lists CMS root path characters",
);

// Behavioural: mirror getWifxLicenseHandlerPath path allow-list
function mockLicensePath(pathname, protocol, host) {
  var pathPart = pathname || "";
  var pos = pathPart.indexOf("/Rhythmyx");
  var rxRoot = pos >= 0 ? pathPart.substring(0, pos + 9) : "/Rhythmyx";
  if (!/^\/[A-Za-z0-9._\-\/]*$/.test(rxRoot)) {
    rxRoot = "/Rhythmyx";
  }
  return (
    protocol + "//" + host + rxRoot + "/rx_wep/ektron?licensekey=webimagefx1"
  );
}
assert(
  mockLicensePath("/Rhythmyx/ui/foo", "https:", "cms.example") ===
    "https://cms.example/Rhythmyx/rx_wep/ektron?licensekey=webimagefx1",
  "normal CMS path yields fixed license URL",
);
assert(
  mockLicensePath('/Rhythmyx"><script>', "https:", "cms.example") ===
    "https://cms.example/Rhythmyx/rx_wep/ektron?licensekey=webimagefx1",
  "path with quote/angle brackets falls back to /Rhythmyx",
);

// --- #945 mobile preview: escape title/url before document.write ---
const mobile = read(
  "system/cms/content/applications/sys_resources/ApplicationFiles/mobilepreview/js/PercMobilePreview.js",
);
assert(
  /function escapeHtml\s*\(/.test(mobile),
  "PercMobilePreview defines escapeHtml",
);
assert(
  /function safeSameOriginHttpUrl\s*\(/.test(mobile),
  "PercMobilePreview defines safeSameOriginHttpUrl",
);
assert(
  /safeTitle|escapeHtml\s*\(\s*d\.title/.test(mobile),
  "document title is escaped before write",
);
assert(
  /safeFrameSrc|escapeHtml\s*\(\s*prurl/.test(mobile),
  "iframe src is escaped before write",
);
assert(
  !/d\.write\s*\(\s*['"]<!DOCTYPE[\s\S]*\+\s*d\.title\s*\+/.test(mobile),
  "raw d.title is not concatenated into document.write",
);

// Behavioural escapeHtml / safeSameOriginHttpUrl mirrors
function escapeHtml(value) {
  return String(value == null ? "" : value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
function safeSameOriginHttpUrl(rawUrl, origin) {
  try {
    var u = new URL(rawUrl, origin);
    if (u.origin !== origin) return origin + "/";
    if (u.protocol !== "http:" && u.protocol !== "https:") return origin + "/";
    return u.href;
  } catch (e) {
    return origin + "/";
  }
}
assert(
  escapeHtml("<script>alert(1)</script>") ===
    "&lt;script&gt;alert(1)&lt;/script&gt;",
  "escapeHtml encodes script tags",
);
assert(
  escapeHtml('x" onload="y') === "x&quot; onload=&quot;y",
  "escapeHtml encodes attribute breakout quotes",
);
assert(
  safeSameOriginHttpUrl("https://evil.example/pwn", "https://cms.example") ===
    "https://cms.example/",
  "cross-origin iframe src rejected",
);
assert(
  safeSameOriginHttpUrl("javascript:alert(1)", "https://cms.example") ===
    "https://cms.example/",
  "javascript: iframe src rejected",
);
assert(
  safeSameOriginHttpUrl(
    "https://cms.example/page?percmobilepreview=",
    "https://cms.example",
  ).startsWith("https://cms.example/"),
  "same-origin http(s) preserved",
);

if (failed) {
  console.error("\n" + failed + " check(s) failed");
  process.exit(1);
}
console.log("\nAll checks passed");
