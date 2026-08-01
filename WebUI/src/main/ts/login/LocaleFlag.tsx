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
import * as FlagSvgs from "country-flag-icons/react/3x2";
import { localeFlagEmoji, localeRegionCode } from "./localeLabels";
import styles from "./LocaleFlag.module.css";

type FlagComponent = React.ComponentType<
  React.SVGProps<SVGSVGElement> & { title?: string }
>;

const FLAGS = FlagSvgs as unknown as Record<string, FlagComponent>;

export interface LocaleFlagProps {
  /** BCP-47 locale tag (e.g. fr-fr, es) */
  locale: string;
  className?: string;
  /** Accessible name for the flag (defaults to region code) */
  title?: string;
}

/**
 * SVG flag for a CMS locale tag. Uses {@code country-flag-icons} (3×2) so flags
 * render correctly on Windows (unlike regional-indicator emoji). Falls back to
 * a globe glyph when the region is unknown or the SVG is missing.
 */
export function LocaleFlag({
  locale,
  className,
  title,
}: LocaleFlagProps): React.ReactElement {
  const region = localeRegionCode(locale);
  const Flag = region ? FLAGS[region] : undefined;
  const cls = [styles.flag, className].filter(Boolean).join(" ");

  if (Flag) {
    return (
      <Flag
        className={cls}
        title={title ?? region}
        aria-hidden="true"
        focusable="false"
      />
    );
  }

  const emoji = localeFlagEmoji(locale);
  return (
    <span className={cls} aria-hidden="true" data-testid="perc-locale-flag-fallback">
      {emoji || "🌐"}
    </span>
  );
}
