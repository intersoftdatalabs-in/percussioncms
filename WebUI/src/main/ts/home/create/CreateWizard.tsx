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

import React, { useEffect, useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import {
  fetchBlogsForSite,
  fetchSites,
  formatApiError,
} from "../../api/home/homeApi";
import { message, MSG } from "../../i18n/message";
import { errorStyle } from "../home.styles";
import { AssetWizard } from "./AssetWizard";
import { BlogWizard } from "./BlogWizard";
import { PageWizard } from "./PageWizard";

export type CreateKind = "page" | "asset" | "blog" | null;

/**
 * Classic Add New → type chooser → page/asset/blog wizard.
 * Equal capability to CUI addwizard + type wizards (FR-001a).
 */
export function CreateWizard(): React.ReactElement {
  const [kind, setKind] = useState<CreateKind>(null);
  const [hasSites, setHasSites] = useState<boolean | null>(null);
  const [hasBlogs, setHasBlogs] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSites()
      .then(async (sites) => {
        setHasSites(sites.length > 0);
        if (sites.length === 0) {
          setHasBlogs(false);
          return;
        }
        // Discover blogs across sites (classic addwizard)
        let found = false;
        for (const s of sites) {
          try {
            const blogs = await fetchBlogsForSite(s.name);
            if (blogs.length > 0) {
              found = true;
              break;
            }
          } catch {
            // ignore per-site blog failures
          }
        }
        setHasBlogs(found);
      })
      .catch((err: unknown) => {
        if (isSessionRedirectError(err)) {
          return;
        }
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
        setHasSites(false);
      });
  }, []);

  if (error) {
    return (
      <p role="alert" style={errorStyle}>
        {error}
      </p>
    );
  }
  if (hasSites === null) {
    return <p role="status">{message(MSG.LOADING)}</p>;
  }
  if (!hasSites) {
    return <p>{message(MSG.NO_SITES_ADMIN)}</p>;
  }

  if (kind === "page") {
    return <PageWizard onBack={() => setKind(null)} />;
  }
  if (kind === "asset") {
    return <AssetWizard onBack={() => setKind(null)} />;
  }
  if (kind === "blog") {
    return <BlogWizard onBack={() => setKind(null)} />;
  }

  return (
    <div data-testid="create-type-chooser">
      <p>{message(MSG.CREATE_HINT)}</p>
      <p style={{ fontWeight: 600 }}>{message(MSG.CREATE_CHOOSE_TYPE)}</p>
      <div style={{ display: "flex", flexDirection: "column", gap: 10, maxWidth: 320 }}>
        <button type="button" onClick={() => setKind("page")}>
          {message(MSG.CREATE_TYPE_PAGE)}
        </button>
        <button type="button" onClick={() => setKind("asset")}>
          {message(MSG.CREATE_TYPE_ASSET)}
        </button>
        {hasBlogs ? (
          <button type="button" onClick={() => setKind("blog")}>
            {message(MSG.CREATE_TYPE_BLOG)}
          </button>
        ) : null}
      </div>
    </div>
  );
}
