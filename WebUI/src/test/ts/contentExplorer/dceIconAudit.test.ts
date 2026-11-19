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
  normalizeActionKey,
  resolveActionIcon,
  resolveMenuCommandIcon,
} from "../../../main/ts/contentExplorer/actionIcons";

/**
 * Retire-or-map contract for the classic DCE icon vocabulary (issue #6604,
 * follow-on to #6515 scope 1 + #6603 scope 2).
 *
 * <p>Every classic GIF under
 * {@code DesktopContentExplorer .../cx/images/} either has a modern glyph in
 * the toolbar/menu pipeline or is explicitly retired with a documented
 * replacement (see the module docblock on {@code actionIcons.tsx} and
 * {@code docs/ai-generated/tasks/issue-6604-dce-icon-retire-map-audit.md}).
 * These tests pin that contract: retired names MUST resolve to
 * {@code null} (callers render label-only / text rows) and mapped names MUST
 * resolve to their documented glyph, so a future pipeline change cannot
 * silently resurrect a retired bitmap meaning or drop a mapped one.</p>
 */
describe("DCE icon retire-or-map audit (#6604)", () => {
  it.each([
    // Applet chrome bitmaps — the SPA shell has no splash/about/update affordance.
    "about",
    "splash",
    "main",
    "update",
    "Admin",
    // Tree expand/collapse + breadcrumb + sort chrome — text controls now.
    "collapsedIcon",
    "expandedIcon",
    "ContentPathSeparator",
    "up",
    "down",
    // Checkout-state bitmaps — CheckoutOwnerPanel shows owner text instead.
    "CheckedIn",
    "CheckedOut",
    "CheckedOutByMe",
    "Reader",
    // Assignee avatars — no assignee picker UI; names render as text.
    "Assignee",
    "MySelf",
    "Nobody",
    "SomeOneElse",
    // ACL member avatars — FolderSecurity rows render member/role text.
    "group_member",
    "user_member",
    "virtual_role",
    // Generic/unknown kinds — text rows / label-only buttons.
    "Missing",
    "None",
    "Other",
    "OtherContent",
    // Structural nodes with no modern bitmap — breadcrumb/tree/text instead.
    "Parent",
    "Outbox",
    "ROOT",
    "Category",
    "SystemCategory",
    "SystemView",
  ])("retired DCE name %s resolves to null (label-only)", (name) => {
    expect(normalizeActionKey(name)).not.toBe("");
    expect(resolveActionIcon(name)).toBeNull();
  });

  it.each([
    ["Folder", "folder"],
    ["Folders", "folder"],
    ["FolderRef", "folder"],
    ["Item", "item"],
    ["Inbox", "inbox"],
    ["MyContent", "user"],
    ["CommunityContent", "user"],
    ["AllContent", "grid"],
    ["Refresh", "refresh"],
    ["Site", "site"],
    ["Sites", "site"],
    ["Views", "preview"],
    ["View", "preview"],
    ["SavedSearch", "search"],
    ["NewSearch", "search"],
    ["EmptySearch", "search"],
    ["CustomSearch", "search"],
    ["StandardSearch", "search"],
    ["SearchResults", "search"],
    ["Relationship", "relationships"],
    ["Slot", "slot"],
    ["SlotItem", "slot"],
  ] as Array<[string, string]>)("mapped DCE name %s resolves to %s", (name, icon) => {
    expect(resolveActionIcon(name)).toBe(icon);
  });

  it.each([
    // Marked state has no distinct modern glyph — the folder glyph renders
    // and selection state is carried by the row highlight.
    ["FolderMarked", "folder"],
    ["FolderMarked_Expanded", "folder"],
    ["SiteSubfolderMarked", "folder"],
    // Design-time items render as generic items — no design-time mode.
    ["DTItem", "item"],
    // Site subfolders share the folder glyph — the tree label keeps the site name.
    ["SiteSubfolder", "folder"],
  ] as Array<[string, string]>)("fallback DCE name %s resolves to generic %s", (name, icon) => {
    expect(resolveActionIcon(name)).toBe(icon);
  });

  it("resolves a real server action shape with punctuation", () => {
    expect(resolveActionIcon("workflow-transition:Submit")).toBe("transition");
  });

  it("menu-bar shell commands share the toolbar vocabulary", () => {
    expect(resolveMenuCommandIcon("content-search")).toBe("search");
    expect(resolveMenuCommandIcon("view-relationships")).toBe("relationships");
    expect(resolveMenuCommandIcon("help-about")).toBe("help");
  });
});
