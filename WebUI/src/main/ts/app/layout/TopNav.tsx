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
import { NavLink, useLocation } from "react-router";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import { i18nKeyAttr } from "../../i18n/i18nDom";
import { message, MSG } from "../../i18n/message";
import { UserMenu } from "./UserMenu";
import styles from "./AppLayout.module.css";
import {
  ADMIN_NAV_LANDING,
  isAdminNavPath,
  topNavItemIds,
} from "./topNavConfig";

/**
 * Product top navigation for the SPA shell.
 * SPA routes use client NavLink; unmigrated surfaces are full-page legacy exits.
 *
 * Order / labels: Home → Explorer → … → single Admin (issue #2702).
 * Dashboard is not a top-nav item (gadgets remain under Home / deep link).
 */
export function TopNav(): React.ReactElement {
  const { isAdmin, isDesigner, isWidgetBuilderActive } = useSpaBootstrap();
  const location = useLocation();
  const itemIds = topNavItemIds({
    isAdmin,
    isDesigner,
    isWidgetBuilderActive,
  });

  const linkClass = ({ isActive }: { isActive: boolean }): string =>
    isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink;

  const adminActive = isAdminNavPath(location.pathname);

  return (
    <nav
      className={styles.topNav}
      aria-label={message(MSG.NAV_ARIA_MAIN)}
      data-testid="perc-spa-topnav"
    >
      <ul className={styles.navGroup}>
        {itemIds.map((id) => {
          switch (id) {
            case "home":
              return (
                <li key={id}>
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
              );
            case "explorer":
              return (
                <li key={id}>
                  <NavLink
                    to="/explorer"
                    className={linkClass}
                    data-testid="nav-explorer"
                    {...i18nKeyAttr(MSG.NAV_EXPLORER)}
                  >
                    {message(MSG.NAV_EXPLORER)}
                  </NavLink>
                </li>
              );
            case "editor":
              return (
                <li key={id}>
                  <a
                    className={styles.navLink}
                    href="/cm/app/?view=editor"
                    data-testid="nav-editor"
                    {...i18nKeyAttr(MSG.NAV_EDITOR)}
                  >
                    {message(MSG.NAV_EDITOR)}
                  </a>
                </li>
              );
            case "architecture":
              // SPA Navigation shell at /architecture (#3094 / #3217)
              return (
                <li key={id}>
                  <NavLink
                    to="/architecture"
                    className={linkClass}
                    data-testid="nav-architecture"
                    title={message(MSG.NAV_ARCHITECTURE_TITLE)}
                    {...i18nKeyAttr(MSG.NAV_ARCHITECTURE)}
                  >
                    {message(MSG.NAV_ARCHITECTURE)}
                  </NavLink>
                </li>
              );
            case "design":
              return (
                <li key={id}>
                  <NavLink
                    to="/design"
                    className={linkClass}
                    data-testid="nav-design"
                    title={message(MSG.NAV_DESIGN_TITLE)}
                    {...i18nKeyAttr(MSG.NAV_DESIGN)}
                  >
                    {message(MSG.NAV_DESIGN)}
                  </NavLink>
                </li>
              );
            case "developer":
              return (
                <li key={id}>
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
              );
            case "publish":
              return (
                <li key={id}>
                  <NavLink
                    to="/publish"
                    className={linkClass}
                    data-testid="nav-publish"
                    {...i18nKeyAttr(MSG.NAV_PUBLISH)}
                  >
                    {message(MSG.NAV_PUBLISH)}
                  </NavLink>
                </li>
              );
            case "admin":
              // Consolidated Admin entry (#2702 / #2784 / #3088).
              // Landing: unified Admin shell; legacy /workflow* redirects into /admin*.
              return (
                <li key={id}>
                  <NavLink
                    to={ADMIN_NAV_LANDING}
                    className={() =>
                      adminActive
                        ? `${styles.navLink} ${styles.navLinkActive}`
                        : styles.navLink
                    }
                    data-testid="nav-admin"
                    // NavLink only marks active for /admin*; force for /workflow* too
                    data-nav-active={adminActive ? "true" : "false"}
                    aria-current={adminActive ? "page" : undefined}
                    {...i18nKeyAttr(MSG.NAV_ADMIN)}
                  >
                    {message(MSG.NAV_ADMIN)}
                  </NavLink>
                </li>
              );
            case "widget-builder":
              return (
                <li key={id}>
                  <NavLink
                    to="/widget-builder"
                    className={linkClass}
                    data-testid="nav-widget-builder"
                    {...i18nKeyAttr(MSG.NAV_WIDGET_BUILDER)}
                  >
                    {message(MSG.NAV_WIDGET_BUILDER)}
                  </NavLink>
                </li>
              );
            default:
              return null;
          }
        })}
      </ul>
      <div className={styles.spacer} />
      <UserMenu />
    </nav>
  );
}
