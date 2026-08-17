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
import { Link, useInRouterContext } from "react-router";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { isWidgetBuilderDeveloperEntry } from "../app/layout/topNavConfig";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message, MSG } from "../i18n/message";
import { catalogColors } from "./catalogStyles";

/**
 * Developer sub-entries that are not product top-nav items (#3514).
 *
 * <p>Design is the existing template-library SPA ({@code /design}). Widget
 * Builder reuses {@code /widget-builder} when that feature is active. Neither
 * link invents a new builder surface.</p>
 */
export function DeveloperRelatedLinks(): React.ReactElement {
  const { isAdmin, isDesigner, isWidgetBuilderActive } = useSpaBootstrap();
  const showWidgetBuilder = isWidgetBuilderDeveloperEntry({
    isAdmin,
    isDesigner,
    isWidgetBuilderActive,
  });

  return (
    <nav
      className="perc-developer-related"
      aria-label={message(MSG.NAV_DEVELOPER_TITLE)}
      data-testid="developer-related-links"
      style={{
        display: "flex",
        flexWrap: "wrap",
        gap: "12px 20px",
        marginTop: "10px",
      }}
    >
      <DeveloperSpaLink
        to="/design"
        testId="developer-design-library-link"
        title={message(MSG.NAV_DESIGN_TITLE)}
        labelKey={MSG.NAV_DESIGN}
      >
        {message(MSG.NAV_DESIGN)}
      </DeveloperSpaLink>
      {showWidgetBuilder ? (
        <DeveloperSpaLink
          to="/widget-builder"
          testId="developer-widget-builder-link"
          labelKey={MSG.NAV_WIDGET_BUILDER}
        >
          {message(MSG.NAV_WIDGET_BUILDER)}
        </DeveloperSpaLink>
      ) : null}
    </nav>
  );
}

function DeveloperSpaLink({
  to,
  testId,
  title,
  labelKey,
  children,
}: {
  to: string;
  testId: string;
  title?: string;
  labelKey: string;
  children: React.ReactNode;
}): React.ReactElement {
  const inRouter = useInRouterContext();
  const style: React.CSSProperties = {
    color: catalogColors.accent,
    fontWeight: 600,
    textDecoration: "none",
  };
  if (inRouter) {
    return (
      <Link
        to={to}
        data-testid={testId}
        title={title}
        style={style}
        {...i18nKeyAttr(labelKey)}
      >
        {children}
      </Link>
    );
  }
  return (
    <a href={to} data-testid={testId} title={title} style={style} {...i18nKeyAttr(labelKey)}>
      {children}
    </a>
  );
}
