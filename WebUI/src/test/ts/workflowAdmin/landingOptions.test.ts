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

import { describe, expect, it } from "vitest";
import { HOMEPAGE_TYPES } from "../../../main/ts/api/user/userHomepageApi";
import { fallbackLabelFromKey } from "../../../main/ts/i18n/message";
import {
  isLandingAllowedForRoles,
  landingOptionsForRoles,
} from "../../../main/ts/workflowAdmin/user/landingOptions";

describe("landingOptionsForRoles", () => {
  it("always includes role-default, Home, and Explorer; not Editor or Design (#3514)", () => {
    const opts = landingOptionsForRoles(["Contributor"]);
    const values = opts.map((o) => o.value);
    expect(values).toContain("");
    expect(values).toContain(HOMEPAGE_TYPES.HOME);
    expect(values).toContain(HOMEPAGE_TYPES.EXPLORER);
    expect(values).not.toContain(HOMEPAGE_TYPES.EDITOR);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);
    expect(values).not.toContain(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(values).not.toContain(HOMEPAGE_TYPES.WORKFLOW);
  });

  it("offers remaining top-nav apps and not Editor or Design (#3537)", () => {
    const values = landingOptionsForRoles(["Admin"]).map((o) => o.value);
    expect(values).toContain("");
    expect(values).toContain(HOMEPAGE_TYPES.HOME);
    expect(values).toContain(HOMEPAGE_TYPES.EXPLORER);
    expect(values).toContain(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(values).toContain(HOMEPAGE_TYPES.DEVELOPER);
    expect(values).toContain(HOMEPAGE_TYPES.PUBLISH);
    expect(values).toContain(HOMEPAGE_TYPES.WORKFLOW);
    expect(values).not.toContain(HOMEPAGE_TYPES.EDITOR);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);
  });

  it("includes Architecture, Developer, and Publish for Designer role", () => {
    const opts = landingOptionsForRoles(["Designer"]);
    const values = opts.map((o) => o.value);
    expect(values).toContain(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(values).toContain(HOMEPAGE_TYPES.DEVELOPER);
    expect(values).toContain(HOMEPAGE_TYPES.PUBLISH);
    expect(values).not.toContain(HOMEPAGE_TYPES.WORKFLOW);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);
    const arch = opts.find((o) => o.value === HOMEPAGE_TYPES.ARCHITECTURE);
    expect(fallbackLabelFromKey(arch!.labelKey)).toBe("Navigation");
  });

  it("includes Administration (Workflow) for Admin role without Design", () => {
    const opts = landingOptionsForRoles(["Admin"]);
    const values = opts.map((o) => o.value);
    expect(values).toContain(HOMEPAGE_TYPES.WORKFLOW);
    expect(values).toContain(HOMEPAGE_TYPES.DEVELOPER);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);
    expect(values).toContain(HOMEPAGE_TYPES.ARCHITECTURE);
  });

  it("keeps a currently stored disallowed value so admin can clear it", () => {
    const workflow = landingOptionsForRoles(
      ["Contributor"],
      HOMEPAGE_TYPES.WORKFLOW,
    );
    expect(workflow.map((o) => o.value)).toContain(HOMEPAGE_TYPES.WORKFLOW);
    const editor = landingOptionsForRoles(
      ["Contributor"],
      HOMEPAGE_TYPES.EDITOR,
    );
    expect(editor.map((o) => o.value)).toContain(HOMEPAGE_TYPES.EDITOR);
    const design = landingOptionsForRoles(
      ["Contributor"],
      HOMEPAGE_TYPES.DESIGNER,
    );
    expect(design.map((o) => o.value)).toContain(HOMEPAGE_TYPES.DESIGNER);
  });
});

describe("isLandingAllowedForRoles", () => {
  it("allows Home, Explorer, and empty for empty roles", () => {
    expect(isLandingAllowedForRoles("", [])).toBe(true);
    expect(isLandingAllowedForRoles(HOMEPAGE_TYPES.HOME, [])).toBe(true);
    expect(isLandingAllowedForRoles(HOMEPAGE_TYPES.EXPLORER, [])).toBe(true);
    expect(isLandingAllowedForRoles(HOMEPAGE_TYPES.EDITOR, [])).toBe(false);
    expect(isLandingAllowedForRoles(HOMEPAGE_TYPES.DESIGNER, [])).toBe(false);
    expect(isLandingAllowedForRoles(HOMEPAGE_TYPES.WORKFLOW, [])).toBe(false);
  });
});
