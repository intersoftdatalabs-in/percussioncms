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

/**
 * Served gadget registry localization contract (issue #6348).
 *
 * <p>The Java-served {@code gadget-catalog.json} and the React
 * {@code gadgetsCatalog.ts} must resolve the same localized display name per
 * locale: every served entry carries the TMX {@code nameKey} the SPA
 * resolves, while {@code name} stays the English fallback (no wire-shape
 * change for existing {@code name}/{@code group} consumers).</p>
 */

import { describe, expect, it } from "vitest";
import { GADGET_CATALOG } from "@/dashboard/gadgetsCatalog";
import servedCatalog from "../../../main/resources/com/percussion/webui/gadget/servlets/gadget-catalog.json";

interface ServedGadget {
  id: string;
  name: string;
  nameKey?: string;
  group: string;
}

function servedGadgets(): ServedGadget[] {
  return (servedCatalog as { gadgets: ServedGadget[] }).gadgets;
}

describe("served gadget-catalog.json nameKeys mirror the React catalog", () => {
  it("every served gadget carries a non-blank TMX nameKey", () => {
    const gadgets = servedGadgets();
    expect(gadgets.length).toBe(21);
    const ids = new Set<string>();
    for (const gadget of gadgets) {
      expect(gadget.id, "gadget id").toBeTruthy();
      expect(ids.has(gadget.id), `duplicate gadget id ${gadget.id}`).toBe(false);
      ids.add(gadget.id);
      // English fallback stays on the wire; localization rides nameKey.
      expect(gadget.name, `English name for ${gadget.id}`).toBeTruthy();
      expect(gadget.nameKey, `TMX nameKey for ${gadget.id}`).toBeTruthy();
      expect(gadget.nameKey, `TMX key shape for ${gadget.id}`).toContain("@");
    }
  });

  it("served nameKeys are exactly the React catalog nameKeys", () => {
    const servedKeys = new Set(servedGadgets().map((gadget) => gadget.nameKey));
    const reactKeys = new Set(GADGET_CATALOG.map((entry) => entry.nameKey));
    expect([...servedKeys].sort()).toEqual([...reactKeys].sort());
  });

  it("known gadgets resolve to their TMX keys", () => {
    const byId = new Map(servedGadgets().map((gadget) => [gadget.id, gadget]));
    expect(byId.get("PercBlogsGadget")?.nameKey).toBe(
      "perc.ui.dashboard.modern@Blogs",
    );
    expect(byId.get("perc_iframe_gadget")?.nameKey).toBe(
      "perc.ui.dashboard.modern@External Content",
    );
    expect(byId.get("PercWidgetConfigGadget")?.nameKey).toBe(
      "perc.ui.dashboard.modern@Dashboard Configuration",
    );
  });
});
