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

import React, { useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router";
import {
  formatApiError,
  isSessionRedirectError,
} from "../../api/client";
import {
  allowedSessionCommunities,
  switchSessionCommunity,
} from "../../api/user/communitySwitchApi";
import { getCurrentUserProfile } from "../../api/user/userProfileApi";
import { i18nKeyAttr } from "../../i18n/i18nDom";
import { message, MSG } from "../../i18n/message";
import { resolveAvatarPresentation } from "../../profile/gravatar";
import { PROFILE_MSG } from "../../profile/messages";
import { UserAvatar } from "../../profile/UserAvatar";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import styles from "./AppLayout.module.css";
import {
  communityOptionTestId,
  dispatchSessionCommunityChanged,
} from "./sessionCommunity";

export function UserMenu(): React.ReactElement {
  const bootstrap = useSpaBootstrap();
  const name = bootstrap.userName?.trim() || message(MSG.USER_DEFAULT_NAME);
  const allowExternal = bootstrap.allowExternalAvatarFetch !== false;
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [currentCommunity, setCurrentCommunity] = useState("");
  const [allowedCommunities, setAllowedCommunities] = useState<string[]>([]);
  const [switchOpen, setSwitchOpen] = useState(false);
  const [switchError, setSwitchError] = useState<string | null>(null);
  const [switchingTo, setSwitchingTo] = useState<string | null>(null);
  const listId = useId();
  const communityBlockRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        // One GET /user/user/current for avatar email + membership communities.
        // Do not swallow SessionRedirectError — let the outer guard redirect.
        // Do not GET /preferences/{name} (or the list) from chrome — unset
        // prefs 404 on both /services and /Rhythmyx/services and pollute
        // Explorer (#3468 / #3458). Profile AvatarSection loads via list.
        // Do not GET the communities catalog — list only session membership.
        const profile = await getCurrentUserProfile();
        if (cancelled) {
          return;
        }
        setAllowedCommunities(allowedSessionCommunities(profile.communities));
        setCurrentCommunity((profile.currentCommunity ?? "").trim());
        const presentation = await resolveAvatarPresentation({
          displayName: name,
          overrideEmail: "",
          primaryEmail: profile.email,
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

  useEffect(() => {
    if (!switchOpen) {
      return;
    }
    const onPointerDown = (event: MouseEvent): void => {
      const root = communityBlockRef.current;
      if (root && !root.contains(event.target as Node)) {
        setSwitchOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent): void => {
      if (event.key === "Escape") {
        setSwitchOpen(false);
      }
    };
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [switchOpen]);

  async function handleSwitch(nextName: string): Promise<void> {
    const trimmed = nextName.trim();
    if (!trimmed) {
      return;
    }
    if (trimmed === currentCommunity) {
      setSwitchOpen(false);
      return;
    }
    setSwitchError(null);
    setSwitchingTo(trimmed);
    try {
      await switchSessionCommunity(trimmed);
      setCurrentCommunity(trimmed);
      setSwitchOpen(false);
      dispatchSessionCommunityChanged(trimmed);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setSwitchError(
        formatApiError(err, message(MSG.USER_COMMUNITY_SWITCH_ERROR)),
      );
    } finally {
      setSwitchingTo(null);
    }
  }

  const communityDisplay =
    currentCommunity || message(MSG.USER_COMMUNITY_NONE);
  const canSwitch = allowedCommunities.length > 0;
  const switching = switchingTo != null;

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
      <div
        className={styles.communityBlock}
        ref={communityBlockRef}
        data-testid="perc-spa-community"
      >
        <span className={styles.communityLabel} {...i18nKeyAttr(MSG.USER_COMMUNITY)}>
          {message(MSG.USER_COMMUNITY)}
        </span>
        <span
          className={styles.communityName}
          data-testid="perc-spa-community-name"
          data-mkd-lang-ignore="1"
        >
          {communityDisplay}
        </span>
        {canSwitch ? (
          <button
            type="button"
            className={styles.communitySwitch}
            data-testid="perc-spa-community-switch"
            aria-expanded={switchOpen}
            aria-controls={listId}
            aria-haspopup="listbox"
            aria-label={message(MSG.USER_COMMUNITY_SWITCH_ARIA)}
            disabled={switching}
            onClick={() => {
              setSwitchError(null);
              setSwitchOpen((open) => !open);
            }}
            {...i18nKeyAttr(MSG.USER_COMMUNITY_SWITCH)}
          >
            {switching
              ? message(MSG.USER_COMMUNITY_SWITCHING)
              : message(MSG.USER_COMMUNITY_SWITCH)}
          </button>
        ) : null}
        {switchOpen ? (
          <ul
            id={listId}
            className={styles.communityList}
            role="listbox"
            data-testid="perc-spa-community-list"
            aria-label={message(MSG.USER_COMMUNITY_LIST_ARIA)}
          >
            {allowedCommunities.map((communityName) => {
              const selected = communityName === currentCommunity;
              return (
                <li key={communityName} role="presentation">
                  <button
                    type="button"
                    role="option"
                    aria-selected={selected}
                    className={
                      selected
                        ? `${styles.communityOption} ${styles.communityOptionCurrent}`
                        : styles.communityOption
                    }
                    data-testid={communityOptionTestId(communityName)}
                    data-community-name={communityName}
                    disabled={switching}
                    onClick={() => {
                      void handleSwitch(communityName);
                    }}
                  >
                    {communityName}
                  </button>
                </li>
              );
            })}
          </ul>
        ) : null}
        {switchError ? (
          <div
            className={styles.communityError}
            role="alert"
            data-testid="perc-spa-community-switch-error"
          >
            {switchError}
          </div>
        ) : null}
      </div>
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
