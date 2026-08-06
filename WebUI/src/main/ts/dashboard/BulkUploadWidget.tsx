/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useState } from "react";
import { isSessionRedirectError } from "../api/client";
import {
  uploadAssetFiles,
  type BulkUploadResult,
  type UploadAssetType,
} from "../api/dashboard/shellGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface BulkUploadWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Bulk Upload** gadget — multi-file upload into Assets.
 * Uses {@code /cm/uploadAssetFile} (PSAssetUploadServlet).
 */
export const BulkUploadWidget: React.FC<BulkUploadWidgetProps> = ({
  title,
}) => {
  const heading = title ?? message(MSG.GADGET_BULK_UPLOAD);
  const [folder, setFolder] = useState("/Assets/uploads/");
  const [assetType, setAssetType] = useState<UploadAssetType>("file");
  const [approve, setApprove] = useState(false);
  const [busy, setBusy] = useState(false);
  const [results, setResults] = useState<BulkUploadResult[]>([]);
  const [error, setError] = useState<string | null>(null);

  const onUpload = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    try {
      setBusy(true);
      setError(null);
      const out = await uploadAssetFiles(files, {
        folder,
        assetType,
        approveOnUpload: approve,
      });
      setResults(out);
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, "Upload failed"));
      }
    } finally {
      setBusy(false);
    }
  };

  const okCount = results.filter((r) => r.ok).length;

  return (
    <div style={styles.widget} data-testid="bulk-upload-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent}>
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          Upload files into the Assets tree (classic bulk upload servlet).
        </p>
        <label style={{ display: "block", fontSize: "0.85em", marginBottom: 8 }}>
          Folder path
          <input
            value={folder}
            onChange={(e) => setFolder(e.target.value)}
            disabled={busy}
            data-testid="bulk-upload-folder"
            style={{
              display: "block",
              width: "100%",
              marginTop: 4,
              padding: 6,
              boxSizing: "border-box",
            }}
          />
        </label>
        <label style={{ display: "block", fontSize: "0.85em", marginBottom: 8 }}>
          Asset type
          <select
            value={assetType}
            onChange={(e) => setAssetType(e.target.value as UploadAssetType)}
            disabled={busy}
            data-testid="bulk-upload-type"
            style={{ display: "block", marginTop: 4, padding: 6 }}
          >
            <option value="file">File</option>
            <option value="image">Image</option>
          </select>
        </label>
        <label style={{ fontSize: "0.85em", display: "flex", gap: 6, marginBottom: 8 }}>
          <input
            type="checkbox"
            checked={approve}
            onChange={(e) => setApprove(e.target.checked)}
            disabled={busy}
          />
          Approve on upload
        </label>
        <input
          type="file"
          multiple
          disabled={busy}
          data-testid="bulk-upload-files"
          onChange={(e) => void onUpload(e.target.files)}
        />
        {busy ? (
          <p style={{ fontSize: "0.85em" }}>Uploading…</p>
        ) : null}
        {error ? (
          <div style={styles.widgetError} data-testid="bulk-upload-error">
            {error}
          </div>
        ) : null}
        {results.length > 0 ? (
          <div data-testid="bulk-upload-results" style={{ marginTop: 12 }}>
            <div style={{ fontSize: "0.85em", fontWeight: 600 }}>
              {okCount}/{results.length} succeeded
            </div>
            <ul style={{ listStyle: "none", padding: 0, margin: "8px 0 0" }}>
              {results.map((r) => (
                <li
                  key={r.fileName + (r.assetName || "")}
                  style={{
                    fontSize: "0.8em",
                    color: r.ok ? "#2e7d32" : "#c62828",
                    padding: "2px 0",
                  }}
                >
                  {r.ok
                    ? `✓ ${r.fileName} → ${r.assetName}`
                    : `✕ ${r.fileName}: ${r.error}`}
                </li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default BulkUploadWidget;
