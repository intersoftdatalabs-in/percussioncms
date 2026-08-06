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

/**
 * Intersoft Data Labs branded chrome for the modern UI.
 *
 * <p>{@link BrandBar} renders a slim, marketing-style header that pairs
 * the Intersoft horizontal wordmark with the Percussion CMS product
 * wordmark and a tagline. {@link BrandFooter} renders a small footer
 * that credits the distributor and links to the publisher site.</p>
 *
 * <p>Both components rely entirely on the active {@link Theme} (via
 * {@link useTheme}) and the CSS module for layout, so a future theme
 * (e.g. "classic", "rhythmyx") can swap its visual language without
 * touching the chrome.</p>
 */

import React from "react";
import { useTheme } from "../ThemeProvider";
import styles from "./Branding.module.css";

export interface BrandBarProps {
  /** Optional override of the right-hand tagline area. */
  children?: React.ReactNode;
  /** ARIA role override. Defaults to {@code banner}. */
  role?: string;
  /** Extra class names appended to the rendered {@code <header>}. */
  className?: string;
}

/**
 * Top-of-page brand bar. Shown above the React shell. Includes the
 * Intersoft horizontal logo, the product wordmark, and an optional
 * right-hand slot for navigation/search summaries.
 */
export function BrandBar({
  children,
  role = "banner",
  className,
}: BrandBarProps): React.ReactElement {
  const theme = useTheme();
  const cls = [styles.brandBar, className].filter(Boolean).join(" ");
  return (
    <header role={role} className={cls} data-testid="perc-brand-bar">
      <div className={styles.brandLeft}>
        <a
          className={styles.brandLogoLink}
          href={theme.brand.publisherUrl ?? "#"}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`${theme.brand.publisher} homepage`}
        >
          <span className={styles.brandAccent} aria-hidden="true" />
          <img
            className={styles.brandLogo}
            src={theme.brand.logoHorizontal}
            alt={`${theme.brand.publisher} logo`}
            width={160}
            height={32}
            data-testid="perc-brand-logo"
          />
        </a>
        <span className={styles.brandProduct} data-testid="perc-brand-product">
          {theme.brand.productName}
        </span>
      </div>
      {children ? <div className={styles.brandRight}>{children}</div> : null}
      {theme.brand.tagline ? (
        <div
          className={styles.brandRight}
          data-testid="perc-brand-tagline"
          aria-hidden={children ? "true" : undefined}
        >
          <span className={styles.brandTagline}>{theme.brand.tagline}</span>
        </div>
      ) : null}
    </header>
  );
}

export interface BrandFooterProps {
  /** Extra content rendered before the publisher credit. */
  children?: React.ReactNode;
  /** ARIA role override. Defaults to {@code contentinfo}. */
  role?: string;
  /** Extra class names appended to the rendered {@code <footer>}. */
  className?: string;
}

/**
 * Bottom-of-page footer. Credits the distributor and links to its site.
 * Intentionally light-weight so it doesn't compete with the CMS chrome.
 */
export function BrandFooter({
  children,
  role = "contentinfo",
  className,
}: BrandFooterProps): React.ReactElement {
  const theme = useTheme();
  const cls = [styles.brandFooter, className].filter(Boolean).join(" ");
  return (
    <footer role={role} className={cls} data-testid="perc-brand-footer">
      {children}
      <span>
        Powered by{" "}
        {theme.brand.publisherUrl ? (
          <a
            className={styles.brandFooterLink}
            href={theme.brand.publisherUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            {theme.brand.publisher}
          </a>
        ) : (
          <span className={styles.brandProductBold}>{theme.brand.publisher}</span>
        )}{" "}
        | <span className={styles.brandProductBold}>{theme.brand.productName}</span>
      </span>
    </footer>
  );
}
