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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import type { ProfileEmailStatus } from "../api/auth";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { PROFILE_MSG } from "./messages";
import styles from "./AccountSection.module.css";

export interface EmailVerificationStatusProps {
  status: ProfileEmailStatus | null;
  emailEditable: boolean;
  isResending: boolean;
  onResend: () => void;
}

/**
 * Renders verified/unverified email badge, pending email notice, and resend trigger.
 */
export function EmailVerificationStatusBanner({
  status,
  emailEditable,
  isResending,
  onResend,
}: EmailVerificationStatusProps): React.ReactElement | null {
  if (!status) {
    return null;
  }

  const hasAddress = Boolean(status.email.trim() || status.pendingEmail.trim());
  const showResend =
    emailEditable && hasAddress && (!status.emailVerified || Boolean(status.pendingEmail.trim()));

  return (
    <div data-testid="perc-profile-email-verification">
      <div className={styles.verificationRow}>
        {status.emailVerified ? (
          <span
            className={styles.verifiedBadge}
            data-testid="perc-profile-email-verified-badge"
            {...i18nKeyAttr(PROFILE_MSG.EMAIL_VERIFIED_BADGE)}
          >
            {message(PROFILE_MSG.EMAIL_VERIFIED_BADGE)}
          </span>
        ) : (
          <span
            className={styles.unverifiedBadge}
            data-testid="perc-profile-email-unverified-badge"
            {...i18nKeyAttr(PROFILE_MSG.EMAIL_UNVERIFIED_BADGE)}
          >
            {message(PROFILE_MSG.EMAIL_UNVERIFIED_BADGE)}
          </span>
        )}

        {showResend && (
          <button
            type="button"
            className={styles.secondaryButton}
            disabled={isResending}
            onClick={onResend}
            data-testid="perc-profile-email-resend"
            {...i18nKeyAttr(
              isResending ? PROFILE_MSG.EMAIL_RESENDING : PROFILE_MSG.EMAIL_RESEND,
            )}
          >
            {message(
              isResending ? PROFILE_MSG.EMAIL_RESENDING : PROFILE_MSG.EMAIL_RESEND,
            )}
          </button>
        )}
      </div>

      {status.pendingEmail.trim() && (
        <p
          className={styles.pendingNotice}
          data-testid="perc-profile-email-pending"
        >
          {message(PROFILE_MSG.EMAIL_PENDING_NOTICE).replace(
            "{0}",
            status.pendingEmail.trim(),
          )}
        </p>
      )}
    </div>
  );
}
