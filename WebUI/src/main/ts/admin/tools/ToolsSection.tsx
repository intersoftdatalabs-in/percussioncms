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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useState } from "react";
import { ConsistencyChecker } from "./ConsistencyChecker";

export const ToolsSection: React.FC = () => {
  const [activeTool, setActiveTool] = useState<"consistency">("consistency");

  return (
    <div style={{ padding: "16px" }} data-testid="perc-tools-section">
      <div style={{ display: "flex", gap: "16px", borderBottom: "1px solid #e2e8f0", marginBottom: "16px" }}>
        <button
          type="button"
          onClick={() => setActiveTool("consistency")}
          style={{
            padding: "8px 16px",
            background: "none",
            border: "none",
            borderBottom: activeTool === "consistency" ? "2px solid #0284c7" : "2px solid transparent",
            fontWeight: activeTool === "consistency" ? 600 : 400,
            color: activeTool === "consistency" ? "#0284c7" : "#64748b",
            cursor: "pointer",
          }}
          data-testid="tool-tab-consistency"
        >
          Consistency Checker
        </button>
      </div>

      {activeTool === "consistency" && <ConsistencyChecker />}
    </div>
  );
};
