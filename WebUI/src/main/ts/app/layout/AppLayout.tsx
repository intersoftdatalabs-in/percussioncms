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
import { Outlet } from "react-router-dom";
import { BrandBar, BrandFooter } from "../../ui-themes/components";
import { TopNav } from "./TopNav";
import styles from "./AppLayout.module.css";

/**
 * Authenticated SPA chrome: brand bar, TopNav, feature outlet, footer.
 * Feature shells should use {@code embedded} (PR-3+) to avoid duplicate chrome.
 */
export function AppLayout(): React.ReactElement {
  return (
    <div className={styles.shell} data-testid="perc-spa-app">
      <BrandBar />
      <TopNav />
      <main className={styles.main} data-testid="perc-spa-outlet">
        <Outlet />
      </main>
      <BrandFooter />
    </div>
  );
}
