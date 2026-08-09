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

import React, { useEffect, useState } from "react";
import { Link } from "react-router";
import { getCurrentUserBasic } from "../../api/user/userCurrentApi";
import { isSessionRedirectError } from "../../api/client";
import { i18nKeyAttr } from "../../i18n/i18nDom";
import { message, MSG } from "../../i18n/message";
import { loadGravatarEmailOverride } from "../../profile/avatarPrefs";
import { resolveAvatarPresentation } from "../../profile/gravatar";
import { PROFILE_MSG } from "../../profile/messages";
import { UserAvatar } from "../../profile/UserAvatar";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import styles from "./AppLayout.module.css";

export function UserMenu(): React.ReactElement {
  const bootstrap = useSpaBootstrap();
  const name = bootstrap.userName?.trim() || message(MSG.USER_DEFAULT_NAME);
  const allowExternal = bootstrap.allowExternalAvatarFetch !== false;
  const [imageUrl, setImageUrl] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        // Do not swallow SessionRedirectError on getCurrentUserBasic — let the
        // outer guard see it so login redirect is not silently skipped.
        const [basic, override] = await Promise.all([
          getCurrentUserBasic(),
          loadGravatarEmailOverride().catch((err: unknown) => {
            if (isSessionRedirectError(err)) {
              throw err;
            }
            return "";
          }),
        ]);
        if (cancelled) {
          return;
        }
        const presentation = await resolveAvatarPresentation({
          displayName: name,
          overrideEmail: override,
          primaryEmail: basic.email,
          allowExternalAvatarFetch: allowExternal,
          size: 64,
        });
        if (!cancelled) {
          setImageUrl(presentation.imageUrl);
        }
      } catch (err) {
        if (isSessionRedirectError(err) || cancelled) {
          return;
        }
        if (!cancelled) {
          setImageUrl(null);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [name, bootstrap.userName, allowExternal]);

  return (
    <div className={styles.userMenu} data-testid="perc-spa-user-menu">
      <UserAvatar
        displayName={name}
        imageUrl={imageUrl}
        size={32}
        className={styles.userAvatar}
        testId="perc-spa-user-avatar"
      />
      <span
        className="mkd-lang-target"
        {...i18nKeyAttr(MSG.USER_SIGNED_IN_AS)}
      >
        {message(MSG.USER_SIGNED_IN_AS)}{" "}
        <span
          className={styles.userName}
          data-testid="perc-spa-user-name"
          data-mkd-lang-ignore="1"
        >
          {name}
        </span>
      </span>
      <Link
        className={styles.profileLink}
        to="/profile"
        data-testid="perc-spa-my-profile"
        {...i18nKeyAttr(PROFILE_MSG.MENU_MY_PROFILE)}
      >
        {message(PROFILE_MSG.MENU_MY_PROFILE)}
      </Link>
      <a
        className={styles.logoutLink}
        href="/logout"
        data-testid="perc-spa-logout"
        {...i18nKeyAttr(MSG.USER_LOGOUT)}
      >
        {message(MSG.USER_LOGOUT)}
      </a>
    </div>
  );
}
