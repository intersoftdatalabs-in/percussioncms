/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Shared gadget catalog metadata (no React components) for layout prefs
 * and the Widget Configuration gadget.
 */

import { message, MSG } from "../i18n/message";

export interface GadgetCatalogEntry {
  id: string;
  /** TMX key used to localize the display name. */
  nameKey: string;
  /** Optional English fallback used by tests when I18N is not loaded. */
  name: string;
  /** TMX key used to localize the description. */
  descriptionKey: string;
  description: string;
  /** Categories are functional groupings, not user-facing prose. */
  category: string;
}

/** Catalog aligned with Dashboard AVAILABLE_GADGETS / GadgetRegistry. */
export const GADGET_CATALOG: GadgetCatalogEntry[] = [
  {
    id: "welcome",
    nameKey: MSG.GADGET_WELCOME,
    name: "Welcome",
    descriptionKey: MSG.GADGET_DESC_WELCOME,
    description: "Welcome message and dashboard introduction",
    category: "System",
  },
  {
    id: "workflow",
    nameKey: MSG.GADGET_PAGES_BY_STATUS,
    name: "Pages By Status",
    descriptionKey: MSG.GADGET_DESC_PAGES_BY_STATUS,
    description: "Pages grouped by workflow state",
    category: "Content Management",
  },
  {
    id: "activity",
    nameKey: MSG.GADGET_ACTIVITY,
    name: "Activity",
    descriptionKey: MSG.GADGET_DESC_ACTIVITY,
    description: "Content activity metrics by path and duration",
    category: "Deprecated",
  },
  {
    id: "process-monitor",
    nameKey: MSG.GADGET_PROCESS_MONITOR,
    name: "Process Monitor",
    descriptionKey: MSG.GADGET_DESC_PROCESS_MONITOR,
    description: "System process and monitoring status",
    category: "System",
  },
  {
    id: "effectiveness",
    nameKey: MSG.GADGET_WHATS_WORKING,
    name: "What's Working",
    descriptionKey: MSG.GADGET_DESC_WHATS_WORKING,
    description: "Effectiveness scores (requires Google Analytics)",
    category: "Analytics",
  },
  {
    id: "assets-status",
    nameKey: MSG.GADGET_ASSETS_BY_STATUS,
    name: "Assets By Status",
    descriptionKey: MSG.GADGET_DESC_ASSETS_BY_STATUS,
    description: "Asset workflow status distribution",
    category: "Content Management",
  },
  {
    id: "bulk-upload",
    nameKey: MSG.GADGET_BULK_UPLOAD,
    name: "Bulk Upload",
    descriptionKey: MSG.GADGET_DESC_BULK_UPLOAD,
    description: "Upload files into Assets/uploads",
    category: "Content Management",
  },
  {
    id: "reports",
    nameKey: MSG.GADGET_REPORTS,
    name: "Reports",
    descriptionKey: MSG.GADGET_DESC_REPORTS,
    description: "Quick CMS reports hub",
    category: "Analytics",
  },
  {
    id: "traffic",
    nameKey: MSG.GADGET_TRAFFIC,
    name: "Traffic",
    descriptionKey: MSG.GADGET_DESC_TRAFFIC,
    description: "Content traffic series",
    category: "Analytics",
  },
  {
    id: "blogs",
    nameKey: MSG.GADGET_BLOGS,
    name: "Blogs",
    descriptionKey: MSG.GADGET_DESC_BLOGS,
    description: "Blog listings and section create",
    category: "Content Management",
  },
  {
    id: "comments",
    nameKey: MSG.GADGET_COMMENTS,
    name: "Comments",
    descriptionKey: MSG.GADGET_DESC_COMMENTS,
    description: "Pages with visitor comments",
    category: "Content Management",
  },
  {
    id: "forms-tracker",
    nameKey: MSG.GADGET_FORM_TRACKER,
    name: "Form Tracker",
    descriptionKey: MSG.GADGET_DESC_FORM_TRACKER,
    description: "Form submission tracking",
    category: "Content Management",
  },
  {
    id: "cookie-consent",
    nameKey: MSG.GADGET_COOKIE_CONSENT,
    name: "Cookie Consent",
    descriptionKey: MSG.GADGET_DESC_COOKIE_CONSENT,
    description: "Cookie consent log totals",
    category: "Compliance",
  },
  {
    id: "seo-audit",
    nameKey: MSG.GADGET_SEO_AUDIT,
    name: "SEO Audit",
    descriptionKey: MSG.GADGET_DESC_SEO_AUDIT,
    description: "Non-SEO pages by severity",
    category: "Analytics",
  },
  {
    id: "google-setup",
    nameKey: MSG.GADGET_GOOGLE_SETUP,
    name: "Google Setup",
    descriptionKey: MSG.GADGET_DESC_GOOGLE_SETUP,
    description: "Google Analytics provider and site profiles",
    category: "Analytics",
  },
  {
    id: "membership",
    nameKey: MSG.GADGET_MEMBERSHIP,
    name: "Membership",
    descriptionKey: MSG.GADGET_DESC_MEMBERSHIP,
    description: "Site membership users (DTS)",
    category: "Deprecated",
  },
  {
    id: "siteimprove",
    nameKey: MSG.GADGET_SITEIMPROVE,
    name: "Siteimprove",
    descriptionKey: MSG.GADGET_DESC_SITEIMPROVE,
    description: "Siteimprove token and publish config",
    category: "Deprecated",
  },
  {
    id: "iframe",
    nameKey: MSG.GADGET_EXTERNAL_CONTENT,
    name: "External Content",
    descriptionKey: MSG.GADGET_DESC_EXTERNAL_CONTENT,
    description: "Embed an external URL",
    category: "External",
  },
  {
    id: "global-variables",
    nameKey: MSG.GADGET_GLOBAL_VARIABLES,
    name: "Global Variables",
    descriptionKey: MSG.GADGET_DESC_GLOBAL_VARIABLES,
    description: "System global variables metadata",
    category: "System",
  },
  {
    id: "sitewide-framework",
    nameKey: MSG.GADGET_SITEWIDE_FRAMEWORK,
    name: "Sitewide Framework",
    descriptionKey: MSG.GADGET_DESC_SITEWIDE_FRAMEWORK,
    description: "Theme / framework summaries",
    category: "Design",
  },
  {
    id: "widget-configuration",
    nameKey: MSG.GADGET_DASHBOARD_CONFIG,
    name: "Dashboard Configuration",
    descriptionKey: MSG.GADGET_DESC_DASHBOARD_CONFIG,
    description: "Choose which gadgets appear this session",
    category: "Deprecated",
  },
];

/**
 * Resolve localized gadget name and description for the catalog.
 * Render-side helper so locale switches re-render.
 */
export function localizedGadget(
  entry: GadgetCatalogEntry
): { name: string; description: string } {
  return {
    name: message(entry.nameKey),
    description: message(entry.descriptionKey),
  };
}

export const PREFERRED_GADGETS_STORAGE_KEY = "perc.home.gadget.preferredIds";
export const PREFERRED_GADGETS_EVENT = "perc-home-gadgets-preferred-changed";

export function loadPreferredGadgetIds(): string[] | null {
  try {
    const raw = sessionStorage.getItem(PREFERRED_GADGETS_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return null;
    return parsed.map((x) => String(x)).filter(Boolean);
  } catch {
    return null;
  }
}

export function savePreferredGadgetIds(ids: string[]): void {
  sessionStorage.setItem(PREFERRED_GADGETS_STORAGE_KEY, JSON.stringify(ids));
  try {
    window.dispatchEvent(
      new CustomEvent(PREFERRED_GADGETS_EVENT, { detail: { ids } }),
    );
  } catch {
    /* ignore */
  }
}
