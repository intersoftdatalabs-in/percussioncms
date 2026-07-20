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

import React from "react";
import { message, MSG } from "../../i18n/message";
import { emptyStyle } from "../publishing.styles";

export interface PlaceholderSectionProps {
  sectionId: string;
  labelKey?: string;
}

/** Temporary empty section until story implementation lands. */
export function PlaceholderSection({
  sectionId,
  labelKey = MSG.PUBLISH_PLACEHOLDER_SECTION,
}: PlaceholderSectionProps): React.ReactElement {
  return (
    <div data-testid={`publish-section-${sectionId}`} style={emptyStyle}>
      <p>{message(labelKey)}</p>
      <p style={{ fontSize: "0.85rem", color: "#888" }}>{sectionId}</p>
    </div>
  );
}
