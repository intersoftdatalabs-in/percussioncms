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

import React, { useCallback, useEffect, useRef, useState } from "react";
import { fetchAbout, type AboutDetail } from "../../api/about/aboutApi";
import { formatApiError } from "../../api/client";

interface AboutDialogProps {
  onClose: () => void;
}

/**
 * "About" dialog showing the server version and third-party license disclaimer (issue #1529).
 *
 * <p>Fetches {@code AboutDetail} from the {@code /about} REST endpoint, which is backed by the
 * same {@code com.percussion.server.PSStringResources} bundle keys ({@code copyright}, {@code
 * thirdPartyCopyright}) printed to the console at server startup - a single source of truth
 * shared between the startup log and this dialog.
 */
export function AboutDialog({ onClose }: AboutDialogProps): React.ReactElement {
  const [detail, setDetail] = useState<AboutDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const closeButtonRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    fetchAbout({ signal: controller.signal })
      .then((result) => {
        setDetail(result);
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          setError(formatApiError(err, "Failed to load About information"));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });
    return () => {
      controller.abort();
    };
  }, []);

  useEffect(() => {
    closeButtonRef.current?.focus();
  }, []);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLDivElement>) => {
      if (e.key === "Escape") {
        e.stopPropagation();
        onClose();
      }
    },
    [onClose],
  );

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="perc-about-dialog-title"
      data-testid="perc-about-dialog-overlay"
      onKeyDown={handleKeyDown}
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.4)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        zIndex: 1100,
      }}
    >
      <div
        style={{
          background: "#fff",
          padding: "24px",
          borderRadius: "8px",
          width: "100%",
          maxWidth: "560px",
          maxHeight: "80vh",
          overflowY: "auto",
          boxShadow: "0 10px 25px rgba(0,0,0,0.1)",
        }}
        data-testid="perc-about-dialog"
      >
        <h3 id="perc-about-dialog-title" style={{ margin: "0 0 16px 0" }}>
          About {detail?.productName ?? "Percussion CMS"}
        </h3>

        {loading && <p data-testid="perc-about-dialog-loading">Loading...</p>}

        {error && (
          <p style={{ color: "#d9534f" }} data-testid="perc-about-dialog-error">
            {error}
          </p>
        )}

        {!loading && !error && detail && (
          <div data-testid="perc-about-dialog-content">
            {detail.versionString && (
              <p data-testid="perc-about-dialog-version">
                <strong>{detail.versionString}</strong>
              </p>
            )}
            {detail.copyright && (
              <p data-testid="perc-about-dialog-copyright" style={{ whiteSpace: "pre-wrap" }}>
                {detail.copyright}
              </p>
            )}
            {detail.thirdPartyCopyright && (
              <p
                data-testid="perc-about-dialog-third-party"
                style={{ fontSize: "0.85em", whiteSpace: "pre-wrap" }}
              >
                {detail.thirdPartyCopyright}
              </p>
            )}
          </div>
        )}

        <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "24px" }}>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            style={{
              padding: "8px 16px",
              borderRadius: "4px",
              border: "1px solid #cbd5e1",
              cursor: "pointer",
            }}
            data-testid="perc-about-dialog-close"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
