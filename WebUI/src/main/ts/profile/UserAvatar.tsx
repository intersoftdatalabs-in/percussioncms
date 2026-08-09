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
import { message } from "../i18n/message";
import { PROFILE_MSG } from "./messages";
import { userInitials } from "./gravatar";
import styles from "./UserAvatar.module.css";

export type UserAvatarProps = {
  displayName: string;
  /** When null/empty, initials fallback is shown. */
  imageUrl?: string | null;
  size?: number;
  className?: string;
  testId?: string;
};

/**
 * Avatar chip: Gravatar image when URL is available, else accessible initials.
 * Image is decorative relative to the container {@code aria-label}.
 */
export function UserAvatar({
  displayName,
  imageUrl,
  size = 32,
  className,
  testId = "perc-user-avatar",
}: UserAvatarProps): React.ReactElement {
  const name = displayName?.trim() || message(PROFILE_MSG.AVATAR_DEFAULT_NAME);
  const initials = userInitials(name);
  const [imgFailed, setImgFailed] = useState(false);

  useEffect(() => {
    setImgFailed(false);
  }, [imageUrl]);

  const showImage = Boolean(imageUrl) && !imgFailed;
  const label = message(PROFILE_MSG.AVATAR_ARIA).replace("{0}", name);

  return (
    <span
      className={[styles.chip, className].filter(Boolean).join(" ")}
      style={{ width: size, height: size, fontSize: Math.max(10, size * 0.38) }}
      role="img"
      aria-label={label}
      data-testid={testId}
      data-avatar-mode={showImage ? "image" : "initials"}
    >
      {showImage ? (
        <img
          className={styles.image}
          src={imageUrl ?? undefined}
          alt=""
          width={size}
          height={size}
          decoding="async"
          referrerPolicy="no-referrer"
          onError={() => setImgFailed(true)}
          data-testid={`${testId}-img`}
        />
      ) : (
        <span className={styles.initials} aria-hidden="true" data-testid={`${testId}-initials`}>
          {initials}
        </span>
      )}
    </span>
  );
}
