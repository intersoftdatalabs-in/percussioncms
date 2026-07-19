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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ContentExplorerShell placeholder component for feature 992-react-content-explorer.
 *
 * <p>Registered in {@code registry.ts} so the bridge can resolve the name
 * {@code "ContentExplorerShell"} for {@code window.PercModernUI.mount(...)},
 * and so Vite's bundler pulls the module in. The full tree + detail-list +
 * ReducedAction set implementation lands in US1 (tasks.md T017–T027).</p>
 */

import * as React from "react";

export interface ContentExplorerShellProps {
  initialPath?: string;
}

export const ContentExplorerShell: React.FC<ContentExplorerShellProps> = (props) => {
  return React.createElement(
    "div",
    {
      "data-component": "ContentExplorerShell",
      "data-feature": "992-react-content-explorer",
      "data-status": "placeholder",
      role: "region",
      "aria-label": "Content Explorer (placeholder — US1 implementation pending)",
    },
    `ContentExplorerShell placeholder — initialPath=${props.initialPath ?? "<root>"}. Implementer: replace with full shell in tasks.md US1 (T017–T027).`,
  );
};

export default ContentExplorerShell;