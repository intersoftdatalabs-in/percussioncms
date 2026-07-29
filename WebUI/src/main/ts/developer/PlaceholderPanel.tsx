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
import { DEV_MSG } from "./messages";

export function PlaceholderPanel({
  sectionLabel,
}: {
  sectionLabel: string;
}): React.ReactElement {
  return (
    <div
      data-testid="developer-placeholder"
      style={{
        padding: "1.25rem",
        background: "#f7fafc",
        border: "1px solid #e2e8f0",
        borderRadius: "6px",
      }}
    >
      <h2 style={{ marginTop: 0, fontSize: "1.1rem" }}>
        {sectionLabel}: {DEV_MSG.PLACEHOLDER_TITLE}
      </h2>
      <p style={{ marginBottom: 0, color: "#4a5568" }}>{DEV_MSG.PLACEHOLDER_BODY}</p>
    </div>
  );
}
