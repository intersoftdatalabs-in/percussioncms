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
  BLOG_LIST_WIDGET_ID,
  BLOG_POST_WIDGET_ID,
  fetchAllBlogs,
  fetchBlogListTemplates,
  fetchBlogPostTemplates,
  fetchSites,
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
import { message, MSG } from "../i18n/message";

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
  title,
  refreshInterval = 60000,
}) => {
  const heading = title ?? message(MSG.GADGET_BLOGS);
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
        <span>{heading}</span>
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
  const [listTemplates, setListTemplates] = useState<TemplateSummary[]>([]);
  const [postTemplates, setPostTemplates] = useState<TemplateSummary[]>([]);
  const [indexTemplateId, setIndexTemplateId] = useState("");
  const [postTemplateId, setPostTemplateId] = useState("");
  const [title, setTitle] = useState("");
  const [urlId, setUrlId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [templatesLoading, setTemplatesLoading] = useState(false);

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
      setListTemplates([]);
      setPostTemplates([]);
      setIndexTemplateId("");
      setPostTemplateId("");
      return;
    }
    setTemplatesLoading(true);
    setError(null);
    // Index template must contain Blog List widget; post template Blog Post widget.
    Promise.all([
      fetchBlogListTemplates(site),
      fetchBlogPostTemplates(site),
    ])
      .then(([listT, postT]) => {
        setListTemplates(listT);
        setPostTemplates(postT);
        setIndexTemplateId(listT[0]?.id ?? "");
        setPostTemplateId(postT[0]?.id ?? "");
      })
      .catch((err: unknown) => {
        if (!isSessionRedirectError(err)) {
          setError(formatApiError(err, "Failed to load blog templates"));
        }
      })
      .finally(() => setTemplatesLoading(false));
  }, [site]);

  const canCreate =
    Boolean(site) &&
    listTemplates.length > 0 &&
    postTemplates.length > 0 &&
    Boolean(indexTemplateId) &&
    Boolean(postTemplateId);

  const onTitleChange = (v: string) => {
    setTitle(v);
    const base = titleToPageFileName(v).replace(/\.html$/i, "");
    setUrlId(sanitizeFileNameInput(base));
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!canCreate || !title.trim() || !urlId.trim()) {
      setError(
        "Site, title, URL name, and eligible list/post templates are required.",
      );
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
      {templatesLoading && (
        <p style={{ fontSize: "0.85rem", color: "#666" }}>
          Checking templates for Blog List / Blog Post widgets…
        </p>
      )}
      <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
        Blog list template
        <span style={{ display: "block", color: "#666", fontWeight: 400 }}>
          Must include the <code>{BLOG_LIST_WIDGET_ID}</code> (Blog List) widget
        </span>
        <select
          data-testid="blogs-create-index-template"
          value={indexTemplateId}
          onChange={(e) => setIndexTemplateId(e.target.value)}
          required
          disabled={!site || templatesLoading || listTemplates.length === 0}
          style={{ display: "block", width: "100%", marginTop: 4 }}
        >
          <option value="">
            {listTemplates.length === 0
              ? "No eligible list templates"
              : "Select…"}
          </option>
          {listTemplates.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </label>
      <label style={{ display: "block", marginBottom: 8, fontSize: "0.85rem" }}>
        Blog post template
        <span style={{ display: "block", color: "#666", fontWeight: 400 }}>
          Must include the <code>{BLOG_POST_WIDGET_ID}</code> (Blog Post) widget
        </span>
        <select
          data-testid="blogs-create-post-template"
          value={postTemplateId}
          onChange={(e) => setPostTemplateId(e.target.value)}
          required
          disabled={!site || templatesLoading || postTemplates.length === 0}
          style={{ display: "block", width: "100%", marginTop: 4 }}
        >
          <option value="">
            {postTemplates.length === 0
              ? "No eligible post templates"
              : "Select…"}
          </option>
          {postTemplates.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </label>
      {!templatesLoading &&
        site &&
        (listTemplates.length === 0 || postTemplates.length === 0) && (
          <p
            data-testid="blogs-create-no-templates"
            style={{ fontSize: "0.85rem", color: "#664d03", background: "#fff8e1", padding: 8 }}
          >
            A blog needs two existing templates: one with a <strong>Blog List</strong>{" "}
            widget and one with a <strong>Blog Post</strong> widget. Create those in
            Design / Templates first (or copy base blog templates onto this site).
            Server create will then clone them as {"{blog}"}-{"{source}"} templates.
          </p>
        )}
      {error && (
        <p role="alert" style={{ color: "#a00", fontSize: "0.85rem" }}>
          {error}
        </p>
      )}
      <div style={{ display: "flex", gap: 8 }}>
        <button
          type="submit"
          data-testid="blogs-create-submit"
          disabled={busy || !canCreate}
        >
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
