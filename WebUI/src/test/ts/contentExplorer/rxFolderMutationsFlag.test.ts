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

import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  isRxCapableFolderPath,
  isRxFolderMutationsEnabled,
  RX_FOLDER_MUTATIONS_STORAGE_KEY,
  RX_FOLDER_MUTATIONS_WINDOW_KEY,
  setRxFolderMutationsFlagOverride,
  shouldUseRxFolderMutations,
} from "../../../main/ts/api/contentExplorer/rxFolderMutationsFlag";

describe("rxFolderMutationsFlag", () => {
  beforeEach(() => {
    setRxFolderMutationsFlagOverride(null);
    try {
      sessionStorage.removeItem(RX_FOLDER_MUTATIONS_STORAGE_KEY);
      localStorage.removeItem(RX_FOLDER_MUTATIONS_STORAGE_KEY);
    } catch {
      /* ignore */
    }
    delete (window as unknown as Record<string, unknown>)[RX_FOLDER_MUTATIONS_WINDOW_KEY];
    // Reset query string without full navigation when possible.
    window.history.replaceState({}, "", window.location.pathname);
  });

  afterEach(() => {
    setRxFolderMutationsFlagOverride(null);
  });

  it("defaults to false (zero behavior change)", () => {
    expect(isRxFolderMutationsEnabled()).toBe(false);
    expect(shouldUseRxFolderMutations("/Folders/Foo")).toBe(false);
  });

  it("honors programmatic override for tests", () => {
    setRxFolderMutationsFlagOverride(true);
    expect(isRxFolderMutationsEnabled()).toBe(true);
    setRxFolderMutationsFlagOverride(false);
    expect(isRxFolderMutationsEnabled()).toBe(false);
  });

  it("reads sessionStorage key when set", () => {
    sessionStorage.setItem(RX_FOLDER_MUTATIONS_STORAGE_KEY, "true");
    expect(isRxFolderMutationsEnabled()).toBe(true);
    sessionStorage.setItem(RX_FOLDER_MUTATIONS_STORAGE_KEY, "0");
    expect(isRxFolderMutationsEnabled()).toBe(false);
  });

  it("reads window global when set", () => {
    (window as unknown as Record<string, unknown>)[RX_FOLDER_MUTATIONS_WINDOW_KEY] = true;
    expect(isRxFolderMutationsEnabled()).toBe(true);
  });
});

describe("isRxCapableFolderPath", () => {
  it("accepts Folders and Sites finder and repository forms", () => {
    expect(isRxCapableFolderPath("/Folders")).toBe(true);
    expect(isRxCapableFolderPath("/Folders/Child")).toBe(true);
    expect(isRxCapableFolderPath("//Folders/Child")).toBe(true);
    expect(isRxCapableFolderPath("/Sites")).toBe(true);
    expect(isRxCapableFolderPath("/Sites/MySite/section")).toBe(true);
    expect(isRxCapableFolderPath("//Sites/MySite")).toBe(true);
    expect(isRxCapableFolderPath("Folders/x")).toBe(true);
  });

  it("rejects non-RX roots and empty paths", () => {
    expect(isRxCapableFolderPath(null)).toBe(false);
    expect(isRxCapableFolderPath("")).toBe(false);
    expect(isRxCapableFolderPath("/")).toBe(false);
    expect(isRxCapableFolderPath("/Assets")).toBe(false);
    expect(isRxCapableFolderPath("/Assets/uploads")).toBe(false);
    expect(isRxCapableFolderPath("/Design")).toBe(false);
    expect(isRxCapableFolderPath("/Recycling")).toBe(false);
  });

  it("treats $System$/Assets as non-RX in finder and repository forms (#3363)", () => {
    expect(isRxCapableFolderPath("/Folders/$System$/Assets")).toBe(false);
    expect(isRxCapableFolderPath("/Folders/$System$/Assets/")).toBe(false);
    expect(isRxCapableFolderPath("/Folders/$System$/Assets/uploads")).toBe(false);
    expect(isRxCapableFolderPath("//Folders/$System$/Assets")).toBe(false);
    expect(isRxCapableFolderPath("//Folders/$System$/Assets/uploads")).toBe(
      false,
    );
    expect(isRxCapableFolderPath("/Assets")).toBe(false);
    expect(isRxCapableFolderPath("/Assets/uploads")).toBe(false);
    // $System$ itself and non-library siblings stay RX-capable.
    expect(isRxCapableFolderPath("/Folders/$System$")).toBe(true);
    expect(isRxCapableFolderPath("/Folders/$System$/Other")).toBe(true);
    expect(isRxCapableFolderPath("/Folders/$System$/Recycling")).toBe(false);
    expect(isRxCapableFolderPath("//Folders/$System$/Recycling/x")).toBe(false);
  });

  it("shouldUseRxFolderMutations requires both flag and RX root", () => {
    setRxFolderMutationsFlagOverride(true);
    expect(shouldUseRxFolderMutations("/Folders/A")).toBe(true);
    expect(shouldUseRxFolderMutations("/Assets/A")).toBe(false);
    expect(shouldUseRxFolderMutations("/Folders/$System$/Assets")).toBe(false);
    expect(shouldUseRxFolderMutations("//Folders/$System$/Assets/uploads")).toBe(
      false,
    );
    setRxFolderMutationsFlagOverride(false);
    expect(shouldUseRxFolderMutations("/Folders/A")).toBe(false);
  });
});
