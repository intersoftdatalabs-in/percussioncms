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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { NavLink } from "react-router";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import { i18nKeyAttr } from "../../i18n/i18nDom";
import { message, MSG } from "../../i18n/message";
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
    <nav
      className={styles.topNav}
      aria-label={message(MSG.NAV_ARIA_MAIN)}
      data-testid="perc-spa-topnav"
    >
      <ul className={styles.navGroup}>
        <li>
          {/* Product default landing after login (not a separate "dashboard" SPA) */}
          <NavLink
            to="/home"
            className={linkClass}
            end
            data-testid="nav-home"
            {...i18nKeyAttr(MSG.NAV_HOME)}
          >
            {message(MSG.NAV_HOME)}
          </NavLink>
        </li>
        <li>
          {/*
            PR-7 product lock: gadgets live on Home (not a peer SPA /dashboard).
            Label kept as Dashboard for familiarity; deep link is /home/gadgets.
          */}
          <NavLink
            to="/home/gadgets"
            className={linkClass}
            data-testid="nav-dashboard"
            title={message(MSG.NAV_DASHBOARD_TITLE)}
            {...i18nKeyAttr(MSG.NAV_DASHBOARD)}
          >
            {message(MSG.NAV_DASHBOARD)}
          </NavLink>
        </li>
        <li>
          <a
            className={styles.navLink}
            href="/cm/app/?view=editor"
            data-testid="nav-editor"
            {...i18nKeyAttr(MSG.NAV_EDITOR)}
          >
            {message(MSG.NAV_EDITOR)}
          </a>
        </li>
        {canPublish ? (
          <>
            <li>
              <a
                className={styles.navLink}
                href="/cm/app/?view=arch"
                data-testid="nav-architecture"
                {...i18nKeyAttr(MSG.NAV_ARCHITECTURE)}
              >
                {message(MSG.NAV_ARCHITECTURE)}
              </a>
            </li>
            <li>
              <NavLink
                to="/developer"
                className={linkClass}
                data-testid="nav-developer"
                title={message(MSG.NAV_DEVELOPER_TITLE)}
                {...i18nKeyAttr(MSG.NAV_DEVELOPER)}
              >
                {message(MSG.NAV_DEVELOPER)}
              </NavLink>
            </li>
            <li>
              <NavLink
                to="/publish"
                className={linkClass}
                data-testid="nav-publish"
                {...i18nKeyAttr(MSG.NAV_PUBLISH)}
              >
                {message(MSG.NAV_PUBLISH)}
              </NavLink>
            </li>
          </>
        ) : null}
        {isAdmin ? (
          <li>
            <NavLink
              to="/workflow"
              className={linkClass}
              data-testid="nav-workflow"
              {...i18nKeyAttr(MSG.NAV_ADMINISTRATION)}
            >
              {message(MSG.NAV_ADMINISTRATION)}
            </NavLink>
          </li>
        ) : null}
        {isAdmin ? (
          <li>
            <NavLink
              to="/admin"
              className={linkClass}
              data-testid="nav-admin"
              {...i18nKeyAttr(MSG.NAV_ADMIN_TOOLS)}
            >
              {message(MSG.NAV_ADMIN_TOOLS)}
            </NavLink>
          </li>
        ) : null}
        {canWb ? (
          <li>
            <NavLink
              to="/widget-builder"
              className={linkClass}
              data-testid="nav-widget-builder"
              {...i18nKeyAttr(MSG.NAV_WIDGET_BUILDER)}
            >
              {message(MSG.NAV_WIDGET_BUILDER)}
            </NavLink>
          </li>
        ) : null}
        <li>
          <NavLink
            to="/explorer"
            className={linkClass}
            data-testid="nav-explorer"
            {...i18nKeyAttr(MSG.NAV_EXPLORER)}
          >
            {message(MSG.NAV_EXPLORER)}
          </NavLink>
        </li>
      </ul>
      <div className={styles.spacer} />
      <UserMenu />
    </nav>
  );
}
