/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React from "react";
import type { SiteDef } from "../api/developer/types";
import { catalogColors, backButton, metaGrid, monoCell } from "./catalogStyles";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";
import { VirtualSiteSourcePanel } from "./VirtualSiteSourcePanel";

export function SiteDetailPanel({
  site,
  onBack,
}: {
  site: SiteDef;
  onBack: () => void;
}): React.ReactElement {
  const name = typeof site.name === "string" ? site.name : "—";
  const siteKey = typeof site.name === "string" ? site.name.trim() : "";
  const gaps =
    site.designGaps && site.designGaps.length
      ? site.designGaps
      : [DEV_MSG.SITE_GAP_WRITE, DEV_MSG.SITE_GAP_PUBLISH, DEV_MSG.SITE_GAP_WF];

  return (
    <div data-testid="developer-site-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-site-back"
        aria-label={DEV_MSG.SITE_BACK}
        style={backButton}
      >
        ← {DEV_MSG.SITE_BACK}
      </button>

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 4px" }} data-testid="developer-site-detail-title">
          {name}
        </h2>
        <dl style={metaGrid}>
          <dt>{DEV_MSG.SITE_COL_NAME}</dt>
          <dd style={{ margin: 0, ...monoCell }}>{name}</dd>
          <dt>{DEV_MSG.SITE_COL_DESC}</dt>
          <dd style={{ margin: 0 }}>{site.description || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_URL}</dt>
          <dd style={{ margin: 0, ...monoCell }}>{site.baseUrl || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_PROTOCOL}</dt>
          <dd style={{ margin: 0 }}>{site.siteProtocol || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_DEFAULT_DOC}</dt>
          <dd style={{ margin: 0, ...monoCell }}>{site.defaultDocument || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_EXT}</dt>
          <dd style={{ margin: 0 }}>{site.defaultFileExtention || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_PAGE_BASED}</dt>
          <dd style={{ margin: 0 }}>
            {site.pageBasedSite ? DEV_MSG.SITE_YES : DEV_MSG.SITE_NO}
          </dd>
        </dl>
      </header>

      {siteKey ? <VirtualSiteSourcePanel siteName={siteKey} /> : null}

      <ObjectAclSection
        objectGuid={site.guid?.stringValue}
        objectKind="site"
        testIdPrefix="developer-site-acl"
      />

      <section data-testid="developer-site-gaps">
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SITE_GAPS}</h3>
        <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {gaps.map((g, i) => (
            <li key={`${g}-${i}`}>{g}</li>
          ))}
        </ul>
      </section>
    </div>
  );
}
