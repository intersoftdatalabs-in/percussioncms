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
 * Shows who has the selected item checked out (#4910, extended to generic
 * custom-type rows without a recognized type name in #6838). A failed
 * lookup offers Retry, which re-runs the lookup for the current item
 * (#6871). Folders and rows without an id pass a null item id and render
 * nothing.
 */

import React, { useEffect, useRef, useState } from "react";
import { message } from "../i18n/message";
import {
  checkoutOwnerErrorReason,
  lookupCheckoutOwner,
  type CheckoutOwnerInfo,
} from "./checkoutOwner";
import { EXPLORER_MSG } from "./messages";

export interface CheckoutOwnerPanelProps {
  /** Item id. Null hides the panel (folders, rows without an id). */
  itemId: string | null;
  /**
   * Changes only after a successful check-in. The same token leaves the
   * displayed checkout user in place (#5199).
   */
  reloadToken?: number;
  load?: (itemId: string) => Promise<CheckoutOwnerInfo>;
}

/**
 * Visible keyboard focus ring for the checkout-owner Retry button (#7146).
 * The button is focusable by default but the panel carries no UA outline
 * styling, so the focus returned after a failed retry would be invisible
 * to sighted keyboard operators. Inline styles cannot express
 * {@code :focus-visible}, so this class carries the ring (same pattern as
 * the context-menu Retry, #7122, the tree rows, #7035, and the detail-list
 * rows, #6596). Plain {@code :focus} is suppressed so pointer clicks do
 * not leave a sticky outline.
 */
const CHECKOUT_OWNER_RETRY_FOCUS_CSS = `
.perc-checkout-owner-retry:focus {
  outline: none;
}
.perc-checkout-owner-retry:focus-visible {
  outline: 2px solid #1a73e8;
  outline-offset: 2px;
}
`;

export function CheckoutOwnerPanel({
  itemId,
  reloadToken = 0,
  load = lookupCheckoutOwner,
}: CheckoutOwnerPanelProps): React.ReactElement | null {
  const [owner, setOwner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  /**
   * Bumped by the error-state Retry button (#6871, residual from #6838).
   * Re-runs the lookup for the current itemId without waiting for a
   * selection change or a checkout action. Mirrors the identity-error
   * Retry in ContentExplorerShell (#6783).
   */
  const [retryKey, setRetryKey] = useState(0);
  const retryButtonRef = useRef<HTMLButtonElement | null>(null);
  /**
   * Set on Retry click; when the retried lookup fails again the button
   * unmounts during loading, so focus is returned to it once the error
   * re-renders (keyboard operators are not stranded on <body>). Cleared
   * on success or empty lookup (#7146) so a later fresh failure in the
   * same mount does not move focus unprompted.
   */
  const returnFocusToRetryRef = useRef(false);

  useEffect(() => {
    const id = (itemId ?? "").trim();
    if (!id) {
      setOwner(null);
      setError(null);
      setLoading(false);
      // Empty lookup disarms any pending retry-focus request (#7146):
      // there is no Retry button to return to, and a later failure in the
      // same mount must not inherit a stale request (focus theft for
      // SR/keyboard operators).
      returnFocusToRetryRef.current = false;
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setOwner(null);
    load(id)
      .then((info) => {
        if (cancelled) return;
        // Success disarms the pending retry-focus request (#7146, mirrors
        // the #7122 disarm effect): the retried lookup landed, so a later
        // fresh failure in the same mount must not move focus to Retry
        // unprompted.
        returnFocusToRetryRef.current = false;
        setOwner((info.checkOutUser ?? "").trim());
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const reason = checkoutOwnerErrorReason(err);
        setError(
          reason === "forbidden"
            ? message(EXPLORER_MSG.CHECKOUT_OWNER_FORBIDDEN)
            : message(EXPLORER_MSG.CHECKOUT_OWNER_FAILED),
        );
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [itemId, load, reloadToken, retryKey]);

  useEffect(() => {
    if (error && returnFocusToRetryRef.current) {
      returnFocusToRetryRef.current = false;
      retryButtonRef.current?.focus();
    }
  }, [error]);

  if (!(itemId ?? "").trim()) {
    return null;
  }

  return (
    <div
      data-testid="explorer-checkout-owner"
      role="status"
      aria-live="polite"
      style={{ fontSize: "0.85rem", padding: "4px 8px" }}
    >
      <style>{CHECKOUT_OWNER_RETRY_FOCUS_CSS}</style>
      <span data-testid="explorer-checkout-owner-label">
        {message(EXPLORER_MSG.CHECKOUT_OWNER_LABEL)}
      </span>{" "}
      {loading ? (
        <span data-testid="explorer-checkout-owner-loading">
          {message(EXPLORER_MSG.CHECKOUT_OWNER_LOADING)}
        </span>
      ) : null}
      {error ? (
        <>
          <span data-testid="explorer-checkout-owner-error" style={{ color: "#b00020" }}>
            {error}
          </span>{" "}
          <button
            type="button"
            data-testid="explorer-checkout-owner-retry"
            className="perc-checkout-owner-retry"
            ref={retryButtonRef}
            onClick={() => {
              returnFocusToRetryRef.current = true;
              setRetryKey((n) => n + 1);
            }}
            style={{ minHeight: "44px", minWidth: "44px", padding: "8px 12px" }}
          >
            {message(EXPLORER_MSG.RETRY)}
          </button>
        </>
      ) : null}
      {!loading && !error && owner ? (
        <span data-testid="explorer-checkout-owner-user">{owner}</span>
      ) : null}
      {!loading && !error && owner === "" ? (
        <span data-testid="explorer-checkout-owner-none">
          {message(EXPLORER_MSG.CHECKOUT_OWNER_NONE)}
        </span>
      ) : null}
    </div>
  );
}
