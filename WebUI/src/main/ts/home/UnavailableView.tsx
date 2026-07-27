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
import { message } from "../i18n/message";

const KEY = "perc.ui.home.modern@Unavailable";

export interface UnavailableViewProps {
  /** Optional detail for debugging (not required for users) */
  detail?: string;
}

/** On-page moved/unavailable surface for retired Home/CUI/WB paths (FR-013). */
export function UnavailableView({
  detail,
}: UnavailableViewProps): React.ReactElement {
  return (
    <div data-testid="unavailable-view" style={{ padding: 24, maxWidth: 560 }}>
      <h1 style={{ fontSize: "1.25rem" }}>{message(KEY)}</h1>
      {detail ? (
        <p style={{ color: "#666", fontSize: 14 }}>
          <code>{detail}</code>
        </p>
      ) : null}
      <p>
        <a href="/cm/app/spa.jsp?entry=home">Home</a>
        {" · "}
        <a href="/cm/app/spa.jsp?entry=home&section=gadgets">Gadgets</a>
      </p>
    </div>
  );
}
