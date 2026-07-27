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

import React from "react";
import { BrandBar, BrandFooter } from "../ui-themes/components";
import { ThemeProvider } from "../ui-themes/ThemeProvider";
import type { SpaLandingBootstrap } from "../login/types";

export interface LandingShellProps {
  bootstrap?: SpaLandingBootstrap;
}

const shellStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  background: "var(--color-surface-alt, #f5f7fb)",
  color: "var(--color-text, #181c2c)",
  fontFamily: "system-ui, -apple-system, sans-serif",
};

const mainStyle: React.CSSProperties = {
  flex: 1,
  padding: "2rem",
  maxWidth: 960,
  margin: "0 auto",
  width: "100%",
  boxSizing: "border-box",
};

const cardStyle: React.CSSProperties = {
  background: "var(--color-surface, #fff)",
  border: "1px solid var(--color-border, #d8dee9)",
  borderRadius: 12,
  padding: "1.5rem 1.75rem",
  boxShadow: "0 4px 16px rgba(11, 34, 74, 0.06)",
};

const navStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.75rem",
  marginTop: "1.25rem",
};

const linkStyle: React.CSSProperties = {
  display: "inline-block",
  padding: "0.45rem 0.85rem",
  borderRadius: 6,
  background: "var(--color-primary, #4a6aa3)",
  color: "#fff",
  textDecoration: "none",
  fontWeight: 600,
  fontSize: "0.9rem",
};

/**
 * Minimal authenticated SPA landing used until full router + shells are wired.
 * Stakeholder demo path: Login → this shell.
 */
export function LandingShell({
  bootstrap = {},
}: LandingShellProps): React.ReactElement {
  const name = bootstrap.userName?.trim() || "user";
  const entry = bootstrap.entry || "home";

  return (
    <ThemeProvider>
      <div style={shellStyle} data-testid="perc-spa-landing">
        <BrandBar>
          <span data-testid="perc-spa-landing-user" style={{ fontSize: "0.9rem" }}>
            Signed in as <strong>{name}</strong>
          </span>
        </BrandBar>
        <main style={mainStyle}>
          <div style={cardStyle}>
            <h1 data-testid="perc-spa-landing-title" style={{ marginTop: 0 }}>
              Welcome to Percussion CMS
            </h1>
            <p style={{ color: "var(--color-text-muted, #5b6478)" }}>
              You are in the modern React SPA shell
              {entry ? (
                <>
                  {" "}
                  (entry: <code data-testid="perc-spa-landing-entry">{entry}</code>)
                </>
              ) : null}
              . Feature routes (Home, Publish, Admin, …) land next; this page
              proves the front-door login → SPA path.
            </p>
            <nav style={navStyle} aria-label="Temporary navigation">
              <a style={linkStyle} href="/cm/app/spa.jsp?entry=home">
                Home
              </a>
              <a style={linkStyle} href="/cm/app/spa.jsp?entry=publish">
                Publishing
              </a>
              <a style={linkStyle} href="/cm/app/?view=dash">
                Dashboard
              </a>
              <a style={linkStyle} href="/logout">
                Logout
              </a>
            </nav>
          </div>
        </main>
        <BrandFooter />
      </div>
    </ThemeProvider>
  );
}
