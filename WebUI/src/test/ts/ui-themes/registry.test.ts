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

import { describe, it, expect, beforeEach, afterEach } from "vitest";
import {
  getActiveTheme,
  getTheme,
  intersoftTheme,
  listThemeIds,
} from "../../../main/ts/ui-themes";

describe("ui-themes registry", () => {
  const originalWindow = (globalThis as { window?: unknown }).window;

  beforeEach(() => {
    delete (globalThis as { window?: unknown }).window;
  });

  afterEach(() => {
    (globalThis as { window?: unknown }).window = originalWindow;
  });

  it("exposes the intersoft theme by id", () => {
    expect(getTheme("intersoft")).toBe(intersoftTheme);
    expect(getTheme("nope")).toBeUndefined();
  });

  it("lists at least the intersoft theme id", () => {
    expect(listThemeIds()).toContain("intersoft");
  });

  it("resolves getActiveTheme to the default theme when no override is set", () => {
    expect(getActiveTheme().meta.id).toBe("intersoft");
    expect(getActiveTheme().meta.isDefault).toBe(true);
  });

  it("honors a window.PERC_THEME_ID override when valid", () => {
    (globalThis as { window: unknown }).window = { PERC_THEME_ID: "intersoft" };
    expect(getActiveTheme().meta.id).toBe("intersoft");
  });

  it("falls back to the default theme when the override is unknown", () => {
    (globalThis as { window: unknown }).window = { PERC_THEME_ID: "ghost" };
    expect(getActiveTheme().meta.id).toBe("intersoft");
  });
});
