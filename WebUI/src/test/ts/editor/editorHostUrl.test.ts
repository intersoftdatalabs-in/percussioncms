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

import { describe, expect, it } from "vitest";
import {
  buildEditorHostUrl,
  editorWindowName,
  normalizeEditorMode,
} from "../../../main/ts/editor/editorHostUrl";

describe("editorHostUrl", () => {
  it("builds spa.jsp query contract", () => {
    expect(buildEditorHostUrl(42)).toBe(
      "/cm/app/spa.jsp?entry=editor&contentId=42&mode=edit",
    );
    expect(buildEditorHostUrl(42, "view")).toBe(
      "/cm/app/spa.jsp?entry=editor&contentId=42&mode=view",
    );
    expect(editorWindowName(42)).toBe("percEditor_42");
  });

  it("defaults unknown mode to edit", () => {
    expect(normalizeEditorMode("view")).toBe("view");
    expect(normalizeEditorMode("VIEW")).toBe("view");
    expect(normalizeEditorMode("promote")).toBe("promote");
    expect(normalizeEditorMode("edit")).toBe("edit");
    expect(normalizeEditorMode(null)).toBe("edit");
  });
});
