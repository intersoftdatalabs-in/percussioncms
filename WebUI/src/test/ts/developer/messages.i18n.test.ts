/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import { DEV_MSG, DEV_MSG_KEYS } from "../../../main/ts/developer/messages";
import { fallbackLabelFromKey } from "../../../main/ts/i18n/message";

describe("Developer DEV_MSG i18n keys", () => {
  it("uses perc.ui.developer@ catalog keys", () => {
    expect(DEV_MSG_KEYS.TITLE).toBe("perc.ui.developer@Developer");
    expect(DEV_MSG_KEYS.TITLE.startsWith("perc.ui.developer@")).toBe(true);
    for (const [name, key] of Object.entries(DEV_MSG_KEYS)) {
      expect(key.startsWith("perc.ui.developer@"), name).toBe(true);
      expect(key.length).toBeGreaterThan("perc.ui.developer@".length);
    }
  });

  it("resolves English fallback without I18N global", () => {
    expect(DEV_MSG.TITLE).toBe("Developer");
    expect(DEV_MSG.CATALOG_CONFIRM_TITLE).toBe("Confirm delete");
    expect(DEV_MSG.CATALOG_CONFIRM_SUBMIT).toBe("Delete");
    expect(DEV_MSG.TAB_CONTENT_TYPES).toBe("Content Types");
    expect(DEV_MSG.TAB_FILE_EXPLORER).toBe("File Explorer");
    expect(DEV_MSG.TAB_DATABASE_EXPLORER).toBe("Database Explorer");
    expect(DEV_MSG.TAB_PROBLEMS).toBe("Problems");
    expect(DEV_MSG.TAB_OBJECT_SORTER).toBe("Object Sorter");
    expect(DEV_MSG.ACL_SAVED).toBe("Object ACL saved.");
    expect(DEV_MSG.CT_LOCK).toBe("Lock");
    expect(DEV_MSG.CT_UNLOCK).toBe("Unlock");
    expect(DEV_MSG.CT_LOCKED).toBe("Locked by you");
    expect(DEV_MSG.CT_FORM_SEARCH_INDEXING).toBe("Search indexing");
    expect(DEV_MSG.CT_SI_LOAD_ERROR).toBe("Could not load search indexing. Default is on.");
    expect(DEV_MSG.CT_CONTROL_PROPS).toBe("Control property values");
    expect(DEV_MSG.CT_INCLUDE).toBe("Include system or shared field");
    expect(DEV_MSG.CT_INCLUDE_ACTION).toBe("Include field");
    expect(DEV_MSG.CTL_SAVE_HINT).toContain("blank description is cleared");
    expect("CTL_GAP_USER" in DEV_MSG_KEYS).toBe(false);
  });

  it("fallbackLabelFromKey extracts text after @", () => {
    expect(fallbackLabelFromKey("perc.ui.developer@Hello")).toBe("Hello");
    expect(fallbackLabelFromKey("perc.ui.developer@A / B")).toBe("A / B");
  });
});
