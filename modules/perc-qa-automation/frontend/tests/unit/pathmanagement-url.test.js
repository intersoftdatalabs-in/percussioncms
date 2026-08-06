/**
 * Unit tests for pathmanagement URL / explorer error helpers (no live CMS).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  isDoubleSlashPathmanagementUrl,
  isHumanReadableErrorText,
  EXPECTED_ROOT_FOLDER_NAMES,
} = require("../helpers/pathmanagement-url");

describe("isDoubleSlashPathmanagementUrl", () => {
  it("detects folder// and folder//Sites (encodePath regression)", () => {
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://localhost:9993/Rhythmyx/services/pathmanagement/path/folder//",
      ),
      true,
    );
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/folder//Sites",
      ),
      true,
    );
  });

  it("detects double slash on other path resources", () => {
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/paginatedFolder//Sites",
      ),
      true,
    );
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/item//Sites/Foo",
      ),
      true,
    );
  });

  it("accepts correct single-slash folder URLs", () => {
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/folder/",
      ),
      false,
    );
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/folder/Sites",
      ),
      false,
    );
    assert.equal(
      isDoubleSlashPathmanagementUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/folder/Sites/Foo%20Bar?x=1",
      ),
      false,
    );
  });

  it("rejects non-strings and unrelated URLs", () => {
    assert.equal(isDoubleSlashPathmanagementUrl(null), false);
    assert.equal(isDoubleSlashPathmanagementUrl(undefined), false);
    assert.equal(isDoubleSlashPathmanagementUrl(""), false);
    assert.equal(
      isDoubleSlashPathmanagementUrl("http://x/other//double"),
      false,
    );
  });
});

describe("isHumanReadableErrorText", () => {
  it("accepts normal error messages", () => {
    assert.equal(
      isHumanReadableErrorText("Failed to load folders: Invalid path"),
      true,
    );
    assert.equal(isHumanReadableErrorText("server down"), true);
  });

  it("rejects empty and [object Object] (formatApiError regression)", () => {
    assert.equal(isHumanReadableErrorText(""), false);
    assert.equal(isHumanReadableErrorText("   "), false);
    assert.equal(isHumanReadableErrorText(null), false);
    assert.equal(
      isHumanReadableErrorText("Failed to load folders: [object Object]"),
      false,
    );
    assert.equal(isHumanReadableErrorText("[object Object]"), false);
  });
});

describe("EXPECTED_ROOT_FOLDER_NAMES", () => {
  it("includes Sites, Assets, Design", () => {
    assert.ok(EXPECTED_ROOT_FOLDER_NAMES.includes("Sites"));
    assert.ok(EXPECTED_ROOT_FOLDER_NAMES.includes("Assets"));
    assert.ok(EXPECTED_ROOT_FOLDER_NAMES.includes("Design"));
  });
});
