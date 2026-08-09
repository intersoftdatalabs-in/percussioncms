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
  buildGravatarUrl,
  normalizeGravatarEmail,
  resolveAvatarPresentation,
  resolveGravatarEmail,
  sha256Hex,
  userInitials,
} from "../../../main/ts/profile/gravatar";

describe("gravatar helpers", () => {
  it("normalizes email for hashing", () => {
    expect(normalizeGravatarEmail("  Foo@Example.COM ")).toBe("foo@example.com");
  });

  it("prefers override email over primary", () => {
    expect(resolveGravatarEmail("override@ex.com", "primary@ex.com")).toBe(
      "override@ex.com",
    );
    expect(resolveGravatarEmail("  ", "primary@ex.com")).toBe("primary@ex.com");
    expect(resolveGravatarEmail(null, null)).toBe("");
  });

  it("builds initials from display name", () => {
    expect(userInitials("Ada Lovelace")).toBe("AL");
    expect(userInitials("editor1")).toBe("ED");
    expect(userInitials("  ")).toBe("?");
    expect(userInitials("X")).toBe("X");
  });

  it("hashes with SHA-256 and builds Gravatar URL", async () => {
    const hex = await sha256Hex("foo@example.com");
    expect(hex).toMatch(/^[a-f0-9]{64}$/);
    const url = await buildGravatarUrl("  Foo@Example.COM ", { size: 64 });
    expect(url).toBe(
      `https://www.gravatar.com/avatar/${hex}?s=64&d=404`,
    );
  });

  it("returns null image when external fetch disabled or no email", async () => {
    const disabled = await resolveAvatarPresentation({
      displayName: "Ada Lovelace",
      primaryEmail: "ada@example.com",
      allowExternalAvatarFetch: false,
    });
    expect(disabled.imageUrl).toBeNull();
    expect(disabled.initials).toBe("AL");

    const noEmail = await resolveAvatarPresentation({
      displayName: "Bob",
      allowExternalAvatarFetch: true,
    });
    expect(noEmail.imageUrl).toBeNull();
    expect(noEmail.initials).toBe("BO");
  });

  it("returns image URL when external fetch allowed", async () => {
    const presentation = await resolveAvatarPresentation({
      displayName: "Ada",
      primaryEmail: "ada@example.com",
      allowExternalAvatarFetch: true,
      size: 80,
    });
    expect(presentation.imageUrl).toMatch(
      /^https:\/\/www\.gravatar\.com\/avatar\/[a-f0-9]{64}\?s=80&d=404$/,
    );
  });
});
