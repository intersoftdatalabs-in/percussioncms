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

import React from "react";
import { styles } from "./dashboard.styles";

export interface UnavailableGadgetShellProps {
  title: string;
  /** Short reason — e.g. no CMS REST peer, retired product. */
  reason: string;
  testId?: string;
}

/**
 * Honest placeholder when a React gadget has no verified sitemanage/DTS API.
 * Replaces invented endpoints that only produced HTTP 500/404.
 */
export const UnavailableGadgetShell: React.FC<UnavailableGadgetShellProps> = ({
  title,
  reason,
  testId = "unavailable-gadget",
}) => (
  <div style={styles.widget} data-testid={testId}>
    <div style={styles.widgetTitle}>{title}</div>
    <div style={styles.widgetContent}>
      <p style={{ marginTop: 0, fontWeight: 600 }}>Not available in React Home</p>
      <p style={{ fontSize: "0.9em", color: "#555" }}>{reason}</p>
    </div>
  </div>
);

export default UnavailableGadgetShell;
