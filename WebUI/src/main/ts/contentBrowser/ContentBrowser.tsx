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
 * ContentBrowser placeholder component for feature 992-react-content-explorer.
 *
 * <p>Registered in {@code registry.ts} so the bridge can resolve the name
 * {@code "ContentBrowser"} for {@code window.PercModernUI.mount(...)} from
 * legacy JSP / dialog hosts. The full embeddable navigate/search/select UI
 * lands in US2 (tasks.md T037–T047).</p>
 */

import * as React from "react";
import type { ContentBrowserProps } from "./types";

export const ContentBrowser: React.FC<ContentBrowserProps> = (props) => {
  return React.createElement(
    "div",
    {
      "data-component": "ContentBrowser",
      "data-feature": "992-react-content-explorer",
      "data-status": "placeholder",
      role: "dialog",
      "aria-label": "Content Browser (placeholder — US2 implementation pending)",
    },
    `ContentBrowser placeholder — mode=${props.mode ?? "select"}, roots=${JSON.stringify(props.roots ?? "all")}. Implementer: replace with full component in tasks.md US2 (T037–T047).`,
  );
};

export default ContentBrowser;