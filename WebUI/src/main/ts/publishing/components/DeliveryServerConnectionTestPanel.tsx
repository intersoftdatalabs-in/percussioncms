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
import { formatApiError, isApiError } from "../../api/client";
import { testServerConnection } from "../../api/publishing/serversApi";
import { message, MSG } from "../../i18n/message";
import {
  CONNECTION_TEST_FAILURE,
  CONNECTION_TEST_SUCCESS,
  connectionTestHttpFailure,
  isConnectionTestSuccess,
} from "../deliveryServerConnectionTest";
import { mapDeliveryServerSaveError } from "../deliveryServerSaveErrors";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  toolbarStyle,
} from "../publishing.styles";

function testErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = connectionTestHttpFailure(err.status);
    if (kind === "bad_request" || kind === "forbidden" || kind === "conflict") {
      return mapDeliveryServerSaveError(err);
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Test the stored delivery-server connection. The call sends an empty body
 * and persists nothing; the notice shows success or failure without ever
 * rendering passwords, keys, or passphrases (no server properties are
 * loaded at all). Works for every driver.
 */
export function DeliveryServerConnectionTestPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [notice, setNotice] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setNotice(null);
    setFailed(false);
    setError(null);
  }, [siteId, serverId]);

  if (!serverId) {
    return null;
  }

  async function runTest(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setNotice(null);
    setFailed(false);
    setError(null);
    try {
      const data = await testServerConnection(siteId, serverId);
      if (isConnectionTestSuccess(data)) {
        setNotice(CONNECTION_TEST_SUCCESS);
      } else {
        setNotice(CONNECTION_TEST_FAILURE);
        setFailed(true);
      }
    } catch (err) {
      setError(testErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="delivery-connection-test-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Connection test</h3>
      {notice && !error && (
        <p
          style={failed ? errorStyle : emptyStyle}
          role={failed ? "alert" : "status"}
          data-testid="delivery-connection-test-notice"
        >
          {notice}
        </p>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="delivery-connection-test-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <button
          type="button"
          style={buttonStyle}
          data-testid="delivery-connection-test-button"
          disabled={busy}
          onClick={() => void runTest()}
        >
          Test connection
        </button>
      </div>
    </div>
  );
}
