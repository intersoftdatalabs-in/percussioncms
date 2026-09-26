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
 * Shows who has the selected page or asset checked out (#4910).
 * Folders pass a null item id and render nothing.
 */

import React, { useEffect, useState } from "react";
import { message } from "../i18n/message";
import {
  checkoutOwnerErrorReason,
  lookupCheckoutOwner,
  type CheckoutOwnerInfo,
} from "./checkoutOwner";
import { EXPLORER_MSG } from "./messages";

export interface CheckoutOwnerPanelProps {
  /** Page or asset id. Null hides the panel (folders). */
  itemId: string | null;
  load?: (itemId: string) => Promise<CheckoutOwnerInfo>;
}

export function CheckoutOwnerPanel({
  itemId,
  load = lookupCheckoutOwner,
}: CheckoutOwnerPanelProps): React.ReactElement | null {
  const [owner, setOwner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const id = (itemId ?? "").trim();
    if (!id) {
      setOwner(null);
      setError(null);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setOwner(null);
    load(id)
      .then((info) => {
        if (cancelled) return;
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
  }, [itemId, load]);

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
      <span data-testid="explorer-checkout-owner-label">
        {message(EXPLORER_MSG.CHECKOUT_OWNER_LABEL)}
      </span>{" "}
      {loading ? (
        <span data-testid="explorer-checkout-owner-loading">
          {message(EXPLORER_MSG.CHECKOUT_OWNER_LOADING)}
        </span>
      ) : null}
      {error ? (
        <span data-testid="explorer-checkout-owner-error" style={{ color: "#b00020" }}>
          {error}
        </span>
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
