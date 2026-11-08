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
 * Collapsible page-metadata field groups for the React Content Editor
 * (#5739, parent #5622 slice 6 batch 2).
 *
 * <p>Ports the retired classic {@code perc_page_edit_dialog}
 * {@code _addFieldGroups} chrome: the first group renders open, the rest
 * collapsed; toggling flips the marker and the group body. Groups holding
 * a field error are expanded by the host (classic auto-expanded sections
 * with save errors).</p>
 */

import React, { useEffect, useRef } from "react";
import { message } from "../i18n/message";
import type { EditorFieldRow } from "./controlKinds";
import {
  appendSanitizedSummaryHtml,
  type PageMetadataSection,
} from "./editorPageMetadata";
import { EDITOR_MSG } from "./messages";
import styles from "./EditorHost.module.css";

export interface EditorPageMetadataPanelProps {
  sections: PageMetadataSection[];
  /** Rows no group claims; rendered after the groups in payload order. */
  ungrouped: EditorFieldRow[];
  openGroups: Record<string, boolean>;
  onToggleGroup: (id: string) => void;
  renderRow: (row: EditorFieldRow) => React.ReactNode;
}

function SectionToggle({
  section,
  open,
  onToggle,
}: {
  section: PageMetadataSection;
  open: boolean;
  onToggle: (id: string) => void;
}): React.ReactElement {
  return (
    <button
      type="button"
      className={styles.metadataGroupToggle}
      data-testid={`editor-metadata-group-toggle-${section.id}`}
      aria-expanded={open}
      aria-controls={`editor-metadata-group-body-${section.id}`}
      onClick={() => onToggle(section.id)}
    >
      <span
        className={styles.metadataGroupMarker}
        data-metadata-open={open ? "true" : "false"}
        aria-hidden="true"
      />
      {message(section.labelKey)}
    </button>
  );
}

export function EditorPageMetadataPanel({
  sections,
  ungrouped,
  openGroups,
  onToggleGroup,
  renderRow,
}: EditorPageMetadataPanelProps): React.ReactElement {
  return (
    <div data-testid="editor-metadata-groups">
      {sections.map((section) => {
        const open = openGroups[section.id] ?? section.openByDefault;
        return (
          <section
            key={section.id}
            className={styles.metadataGroup}
            data-testid={`editor-metadata-group-${section.id}`}
          >
            <SectionToggle section={section} open={open} onToggle={onToggleGroup} />
            {open ? (
              <div
                className={styles.metadataGroupBody}
                id={`editor-metadata-group-body-${section.id}`}
                data-testid={`editor-metadata-group-body-${section.id}`}
              >
                {section.rows.map((row) => (
                  <React.Fragment key={row.name}>
                    {row.name.trim().toLowerCase() === "sys_contentpostdate" ? (
                      <div
                        className={styles.metadataNote}
                        data-testid="editor-metadata-postdate-note"
                      >
                        <div className={styles.metadataNoteTitle}>
                          {message(EDITOR_MSG.METADATA_DATE_OVERRIDE_LABEL)}
                        </div>
                        <span className={styles.metadataNoteText}>
                          {message(EDITOR_MSG.METADATA_DATE_OVERRIDE_NOTE)}
                        </span>
                      </div>
                    ) : null}
                    {renderRow(row)}
                  </React.Fragment>
                ))}
              </div>
            ) : null}
          </section>
        );
      })}
      {ungrouped.map((row) => (
        <React.Fragment key={row.name}>{renderRow(row)}</React.Fragment>
      ))}
    </div>
  );
}

/**
 * Read-only sanitized preview replacing the summary editor while
 * auto-generate summary is on (classic {@code _handleAutoSummary}).
 */
export function PageSummaryPreview({
  html,
}: {
  html: string;
}): React.ReactElement {
  const ref = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    if (ref.current) {
      appendSanitizedSummaryHtml(ref.current, html);
    }
  }, [html]);
  return (
    <div
      ref={ref}
      className={`${styles.input} ${styles.readonly} ${styles.metadataPreview}`}
      data-testid="editor-metadata-summary-preview"
      role="region"
      aria-label={message(EDITOR_MSG.METADATA_AUTO_SUMMARY_PREVIEW)}
    />
  );
}
