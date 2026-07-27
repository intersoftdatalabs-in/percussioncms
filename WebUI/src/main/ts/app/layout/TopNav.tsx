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
import { NavLink } from "react-router";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import { UserMenu } from "./UserMenu";
import styles from "./AppLayout.module.css";

/**
 * Product top navigation for the SPA shell.
 * SPA routes use client NavLink; unmigrated surfaces are full-page legacy exits.
 */
export function TopNav(): React.ReactElement {
  const { isAdmin, isDesigner, isWidgetBuilderActive } = useSpaBootstrap();
  const canPublish = isAdmin || isDesigner;
  const canWb = isWidgetBuilderActive && (isAdmin || isDesigner);

  const linkClass = ({ isActive }: { isActive: boolean }): string =>
    isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink;

  return (
    <nav className={styles.topNav} aria-label="Main" data-testid="perc-spa-topnav">
      <ul className={styles.navGroup}>
        <li>
          {/* Product default landing after login (not a separate “dashboard” SPA) */}
          <NavLink to="/home" className={linkClass} end data-testid="nav-home">
            Home
          </NavLink>
        </li>
        <li>
          {/*
            Legacy jQuery dashboard exit only. Product direction: Home is the
            primary landing; we may fold dashboard widgets into Home later
            rather than a peer SPA /dashboard route.
          */}
          <a
            className={styles.navLink}
            href="/cm/app/?view=dash"
            data-testid="nav-dashboard"
            title="Legacy dashboard (may merge into Home)"
          >
            Dashboard
          </a>
        </li>
        <li>
          <a
            className={styles.navLink}
            href="/cm/app/?view=editor"
            data-testid="nav-editor"
          >
            Editor
          </a>
        </li>
        {canPublish ? (
          <>
            <li>
              <a
                className={styles.navLink}
                href="/cm/app/?view=arch"
                data-testid="nav-architecture"
              >
                Architecture
              </a>
            </li>
            <li>
              <a
                className={styles.navLink}
                href="/cm/app/?view=design"
                data-testid="nav-design"
              >
                Design
              </a>
            </li>
            <li>
              <NavLink to="/publish" className={linkClass} data-testid="nav-publish">
                Publish
              </NavLink>
            </li>
          </>
        ) : null}
        {isAdmin ? (
          <li>
            <NavLink to="/workflow" className={linkClass} data-testid="nav-workflow">
              Administration
            </NavLink>
          </li>
        ) : null}
        {isAdmin ? (
          <li>
            <NavLink to="/admin" className={linkClass} data-testid="nav-admin">
              Admin tools
            </NavLink>
          </li>
        ) : null}
        {canWb ? (
          <li>
            <NavLink
              to="/widget-builder"
              className={linkClass}
              data-testid="nav-widget-builder"
            >
              Widget Builder
            </NavLink>
          </li>
        ) : null}
        <li>
          <NavLink to="/explorer" className={linkClass} data-testid="nav-explorer">
            Explorer
          </NavLink>
        </li>
      </ul>
      <div className={styles.spacer} />
      <UserMenu />
    </nav>
  );
}
