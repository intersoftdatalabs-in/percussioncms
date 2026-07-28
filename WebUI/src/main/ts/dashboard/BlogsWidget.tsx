/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../api/client";
import {
  fetchAllBlogs,
  fetchSites,
  fetchTemplatesForSite,
  formatApiError,
} from "../api/home/homeApi";
import type { BlogSummary, SiteSummary, TemplateSummary } from "../api/home/types";
import { post } from "../api/client";
import { PATHS } from "../api/paths";
import { styles } from "./dashboard.styles";
import {
  sanitizeFileNameInput,
  titleToPageFileName,
  toRepositoryCmsPath,
} from "../home/create/filenameUtils";

export interface BlogsWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Blogs gadget — list blogs from real sitemanage APIs and create a blog section.
 *
 * <p>Create Blog Post (posts under a blog) is Home → Create; this gadget creates
 * the <strong>blog section itself</strong> (classic Blogs gadget responsibility).</p>
 */
export const BlogsWidget: React.FC<BlogsWidgetProps> = ({
  title = "Blogs",
  refreshInterval = 60000,
}) => {
  const [blogs, setBlogs] = useState<BlogSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const list = await fetchAllBlogs();
      setBlogs(list);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load blogs"));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    if (refreshInterval <= 0) {
      return;
    }
    const id = window.setInterval(() => void load(), refreshInterval);
    return () => window.clearInterval(id);
  }, [load, refreshInterval]);

  const openPostCreate = () => {
    window.location.href = "/cm/app/home/create";
  };

  return (
    <div style={styles.widget} data-testid="blogs-widget">
      <div
        style={{
          ...styles.widgetTitle,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: 8,
        }}
      >
        <span>{title}</span>
        <button
          type="button"
          data-testid="blogs-widget-create-section"
          onClick={() => setShowCreate((v) => !v)}
          style={{
            fontSize: "0.8rem",
            padding: "4px 8px",
            cursor: "pointer",
          }}
        >
          {showCreate ? "Cancel" : "New blog"}
        </button>
      </div>

      {showCreate && (
        <CreateBlogSectionForm
          onCreated={async () => {
            setShowCreate(false);
            await load();
          }}
          onCancel={() => setShowCreate(false)}
        />
      )}

      {isLoading && (
        <div style={styles.widgetLoading} data-testid="blogs-widget-loading">
          <p>Loading blogs…</p>
        </div>
      )}
      {!isLoading && error && (
        <div style={styles.widgetError} data-testid="blogs-widget-error">
          <p>{error}</p>
        </div>
      )}
      {!isLoading && !error && blogs.length === 0 && (
        <div style={styles.widgetContent} data-testid="blogs-widget-empty">
          <p style={{ margin: "0 0 8px" }}>
            No blogs yet. Create a blog section to enable Home → Create → Blog
            Post.
          </p>
        </div>
      )}
      {!isLoading && !error && blogs.length > 0 && (
        <div style={styles.widgetContent} data-testid="blogs-widget-list">
          <ul style={{ listStyle: "none", margin: 0, padding: 0 }}>
            {blogs.map((b, i) => (
              <li
                key={`${b.path ?? b.folderPath}-${i}`}
                style={{
                  padding: "10px 0",
                  borderBottom: "1px solid #eee",
                }}
              >
                <div style={{ fontWeight: 600 }}>
                  {b.site ? `${b.site} / ` : ""}
                  {b.title}
                </div>
                {b.path && (
                  <div style={{ fontSize: "0.8rem", color: "#666" }}>{b.path}</div>
                )}
              </li>
            ))}
          </ul>
          <p style={{ marginTop: 12, fontSize: "0.85rem" }}>
            <button type="button" onClick={openPostCreate}>
              Create blog post…
            </button>
          </p>
        </div>
      )}
    </div>
  );
};

interface CreateBlogSectionFormProps {
  onCreated: () => void | Promise<void>;
  onCancel: () => void;
}

/**
 * Create a blog site section (folder + index + post template).
 * POST /services/sitemanage/section with CreateSiteSection sectionType=blog.
 */
function CreateBlogSectionForm({
  onCreated,
  onCancel,
}: CreateBlogSectionFormProps): React.ReactElement {
  const [sites, setSites] = useState<SiteSummary[]>([]);
  const [site, setSite] = useState("");
  const [templates, setTemplates] = useState<TemplateSummary[]>([]);
  const [indexTemplateId, setIndexTemplateId] = useState("");
  const [postTemplateId, setPostTemplateId] = useState("");
  const [title, setTitle] = useState("");
  const [urlId, setUrlId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchSites()
      .then((list) => {
        setSites(list);
        if (list.length === 1) {
          setSite(list[0].name);
        }
      })
      .catch((err: unknown) => {
        if (!isSessionRedirectError(err)) {
          setError(formatApiError(err, "Failed to load sites"));
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!site) {
      setTemplates([]);
      setIndexTemplateId("");
      setPostTemplateId("");
      return;
    }
    fetchTemplatesForSite(site)
      .then((t) => {
        setTemplates(t);
        if (t.length >= 1) {
          setIndexTemplateId(t[0].id);
          setPostTemplateId(t[0].id);
        }
        if (t.length >= 2) {
          setPostTemplateId(t[1].id);
        }
      })
      .catch((err: unknown) => {
        if (!isSessionRedirectError(err)) {
          setError(formatApiError(err, "Failed to load templates"));
        }
      });
  }, [site]);

  const onTitleChange = (v: string) => {
    setTitle(v);
    const base = titleToPageFileName(v).replace(/\.html$/i, "");
    setUrlId(sanitizeFileNameInput(base));
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!site || !title.trim() || !urlId.trim() || !indexTemplateId || !postTemplateId) {
      setError("Site, title, URL name, and templates are required.");
      return;
    }
    setBusy(true);
    try {
      const folderPath = toRepositoryCmsPath(`/Sites/${site}`);
      // Wire: CreateSiteSection (PSSiteSectionRestService#create)
      await post(PATHS.SECTION_CREATE, {
        CreateSiteSection: {
          pageTitle: title.trim(),
          pageLinkTitle: title.trim(),
          pageName: urlId.trim(),
          pageUrlIdentifier: urlId.trim(),
          templateId: indexTemplateId,
          blogPostTemplateId: postTemplateId,
          folderPath,
          sectionType: "blog",
          copyTemplates: true,
        },
      });
      await onCreated();
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Could not create blog section"));
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <p style={{ padding: 8 }}>Loading…</p>;
  }

  return (
    <form
      data-testid="blogs-widget-create-form"
      onSubmit={onSubmit}
      style={{
        padding: 12,
        margin: "0 0 12px",
        background: "#f7f9fc",
        border: "1px solid #dde3ea",
        borderRadius: 6,
      }}
    >
      <p style={{ margin: "0 0 8px", fontWeight: 600, fontSize: "0.9rem" }}>
        Create blog section
      </p>
      {sites.length > 1 && (
        <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
          Site
          <select
            data-testid="blogs-create-site"
            value={site}
            onChange={(e) => setSite(e.target.value)}
            required
            style={{ display: "block", width: "100%", marginTop: 4 }}
          >
            <option value="">Select…</option>
            {sites.map((s) => (
              <option key={s.name} value={s.name}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
      )}
      <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
        Title
        <input
          data-testid="blogs-create-title"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          required
          style={{ display: "block", width: "100%", marginTop: 4 }}
        />
      </label>
      <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
        URL name
        <input
          data-testid="blogs-create-url"
          value={urlId}
          onChange={(e) => setUrlId(sanitizeFileNameInput(e.target.value))}
          required
          style={{ display: "block", width: "100%", marginTop: 4 }}
        />
      </label>
      <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
        Index template
        <select
          data-testid="blogs-create-index-template"
          value={indexTemplateId}
          onChange={(e) => setIndexTemplateId(e.target.value)}
          required
          disabled={!site}
          style={{ display: "block", width: "100%", marginTop: 4 }}
        >
          <option value="">Select…</option>
          {templates.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </label>
      <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
        Post template
        <select
          data-testid="blogs-create-post-template"
          value={postTemplateId}
          onChange={(e) => setPostTemplateId(e.target.value)}
          required
          disabled={!site}
          style={{ display: "block", width: "100%", marginTop: 4 }}
        >
          <option value="">Select…</option>
          {templates.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </label>
      {error && (
        <p role="alert" style={{ color: "#a00", fontSize: "0.85rem" }}>
          {error}
        </p>
      )}
      <div style={{ display: "flex", gap: 8 }}>
        <button type="submit" data-testid="blogs-create-submit" disabled={busy}>
          {busy ? "Creating…" : "Create blog"}
        </button>
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

export default BlogsWidget;
