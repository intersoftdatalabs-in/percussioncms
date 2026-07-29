/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Shared gadget catalog metadata (no React components) for layout prefs
 * and the Widget Configuration gadget.
 */

export interface GadgetCatalogEntry {
  id: string;
  name: string;
  description: string;
  category: string;
}

/** Catalog aligned with Dashboard AVAILABLE_GADGETS / GadgetRegistry. */
export const GADGET_CATALOG: GadgetCatalogEntry[] = [
  {
    id: "welcome",
    name: "Welcome",
    description: "Welcome message and dashboard introduction",
    category: "System",
  },
  {
    id: "workflow",
    name: "Pages By Status",
    description: "Pages grouped by workflow state",
    category: "Content Management",
  },
  {
    id: "activity",
    name: "Activity",
    description: "Content activity metrics by path and duration",
    category: "Deprecated",
  },
  {
    id: "process-monitor",
    name: "Process Monitor",
    description: "System process and monitoring status",
    category: "System",
  },
  {
    id: "effectiveness",
    name: "What's Working",
    description: "Effectiveness scores (requires Google Analytics)",
    category: "Analytics",
  },
  {
    id: "assets-status",
    name: "Assets By Status",
    description: "Asset workflow status distribution",
    category: "Content Management",
  },
  {
    id: "bulk-upload",
    name: "Bulk Upload",
    description: "Upload files into Assets/uploads",
    category: "Content Management",
  },
  {
    id: "reports",
    name: "Reports",
    description: "Quick CMS reports hub",
    category: "Analytics",
  },
  {
    id: "traffic",
    name: "Traffic",
    description: "Content traffic series",
    category: "Analytics",
  },
  {
    id: "blogs",
    name: "Blogs",
    description: "Blog listings and section create",
    category: "Content Management",
  },
  {
    id: "comments",
    name: "Comments",
    description: "Pages with visitor comments",
    category: "Content Management",
  },
  {
    id: "forms-tracker",
    name: "Form Tracker",
    description: "Form submission tracking",
    category: "Content Management",
  },
  {
    id: "cookie-consent",
    name: "Cookie Consent",
    description: "Cookie consent log totals",
    category: "Compliance",
  },
  {
    id: "seo-audit",
    name: "SEO Audit",
    description: "Non-SEO pages by severity",
    category: "Analytics",
  },
  {
    id: "google-setup",
    name: "Google Setup",
    description: "Google Analytics provider and site profiles",
    category: "Analytics",
  },
  {
    id: "membership",
    name: "Membership",
    description: "Site membership users (DTS)",
    category: "Deprecated",
  },
  {
    id: "siteimprove",
    name: "Siteimprove",
    description: "Siteimprove token and publish config",
    category: "Deprecated",
  },
  {
    id: "iframe",
    name: "Iframe",
    description: "Embed an external URL",
    category: "External",
  },
  {
    id: "global-variables",
    name: "Global Variables",
    description: "System global variables metadata",
    category: "System",
  },
  {
    id: "sitewide-framework",
    name: "Sitewide Framework",
    description: "Theme / framework summaries",
    category: "Design",
  },
  {
    id: "widget-configuration",
    name: "Dashboard Configuration",
    description: "Choose which gadgets appear this session",
    category: "Deprecated",
  },
];

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
