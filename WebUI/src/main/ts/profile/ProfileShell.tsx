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

import React, { useId } from "react";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { PROFILE_MSG } from "./messages";
import styles from "./ProfileShell.module.css";

export interface ProfileShellProps {
  /**
   * When true (SPA AppLayout), shell content only — chrome comes from AppLayout.
   * Kept for symmetry with other feature shells; always embedded under SPA routes.
   */
  embedded?: boolean;
}

type ProfileSectionId = "account" | "security" | "preferences" | "avatar";

const SECTIONS: {
  id: ProfileSectionId;
  titleKey: string;
  bodyKey: string;
  testId: string;
}[] = [
  {
    id: "account",
    titleKey: PROFILE_MSG.SECTION_ACCOUNT,
    bodyKey: PROFILE_MSG.SECTION_ACCOUNT_BODY,
    testId: "perc-profile-section-account",
  },
  {
    id: "security",
    titleKey: PROFILE_MSG.SECTION_SECURITY,
    bodyKey: PROFILE_MSG.SECTION_SECURITY_BODY,
    testId: "perc-profile-section-security",
  },
  {
    id: "preferences",
    titleKey: PROFILE_MSG.SECTION_PREFERENCES,
    bodyKey: PROFILE_MSG.SECTION_PREFERENCES_BODY,
    testId: "perc-profile-section-preferences",
  },
  {
    id: "avatar",
    titleKey: PROFILE_MSG.SECTION_AVATAR,
    bodyKey: PROFILE_MSG.SECTION_AVATAR_BODY,
    testId: "perc-profile-section-avatar",
  },
];

/**
 * User profile hub shell (slice 1): landmarks, heading hierarchy, and
 * placeholder sections for later account / security / preferences / avatar work.
 * Does not load or mutate account data.
 */
export function ProfileShell(_props: ProfileShellProps = {}): React.ReactElement {
  const reactId = useId();
  const titleId = `perc-profile-title-${reactId}`;
  const sectionsNavId = `perc-profile-sections-nav-${reactId}`;

  return (
    <div
      className={styles.shell}
      data-testid="perc-profile-shell"
      aria-labelledby={titleId}
    >
      <header className={styles.header}>
        <h1
          id={titleId}
          className={styles.title}
          data-testid="perc-profile-title"
          {...i18nKeyAttr(PROFILE_MSG.TITLE)}
        >
          {message(PROFILE_MSG.TITLE)}
        </h1>
        <p
          className={styles.intro}
          data-testid="perc-profile-intro"
          {...i18nKeyAttr(PROFILE_MSG.INTRO)}
        >
          {message(PROFILE_MSG.INTRO)}
        </p>
      </header>

      <nav
        className={styles.sectionNav}
        aria-labelledby={sectionsNavId}
        data-testid="perc-profile-section-nav"
      >
        <h2 id={sectionsNavId} className="visually-hidden" style={visuallyHidden}>
          <span {...i18nKeyAttr(PROFILE_MSG.SECTIONS_NAV)}>
            {message(PROFILE_MSG.SECTIONS_NAV)}
          </span>
        </h2>
        <ul className={styles.sectionNavList}>
          {SECTIONS.map((section) => (
            <li key={section.id}>
              <a
                className={styles.sectionNavLink}
                href={`#perc-profile-${section.id}`}
                data-testid={`perc-profile-nav-${section.id}`}
                {...i18nKeyAttr(section.titleKey)}
              >
                {message(section.titleKey)}
              </a>
            </li>
          ))}
        </ul>
      </nav>

      <div className={styles.sections}>
        {SECTIONS.map((section) => {
          const headingId = `perc-profile-${section.id}-heading`;
          return (
            <section
              key={section.id}
              id={`perc-profile-${section.id}`}
              className={styles.section}
              aria-labelledby={headingId}
              data-testid={section.testId}
              tabIndex={-1}
            >
              <h2
                id={headingId}
                className={styles.sectionHeading}
                {...i18nKeyAttr(section.titleKey)}
              >
                {message(section.titleKey)}
              </h2>
              <p className={styles.sectionBody} {...i18nKeyAttr(section.bodyKey)}>
                {message(section.bodyKey)}
              </p>
              <p
                className={styles.comingSoon}
                data-testid={`${section.testId}-status`}
                {...i18nKeyAttr(PROFILE_MSG.COMING_SOON)}
              >
                {message(PROFILE_MSG.COMING_SOON)}
              </p>
            </section>
          );
        })}
      </div>
    </div>
  );
}

/** Inline visually-hidden styles so we do not depend on a global a11y utility class. */
const visuallyHidden: React.CSSProperties = {
  position: "absolute",
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: "hidden",
  clip: "rect(0, 0, 0, 0)",
  whiteSpace: "nowrap",
  border: 0,
};
