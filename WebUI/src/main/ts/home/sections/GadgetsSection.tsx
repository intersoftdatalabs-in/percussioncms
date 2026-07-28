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
import { useSpaBootstrap } from "../../app/bootstrap/BootstrapContext";
import { Dashboard } from "../../dashboard";

/**
 * Home section that hosts dashboard gadgets (PR-7 product lock).
 * Reuses the React Dashboard widget registry — not a peer SPA /dashboard route.
 *
 * <p>Passes the SPA bootstrap username so dashboard layout can load/save
 * per-user configuration. Without it, only hard-coded default gadgets render
 * and persistence is skipped.</p>
 */
export function GadgetsSection(): React.ReactElement {
  const { userName } = useSpaBootstrap();
  return (
    <div data-testid="home-gadgets-section" aria-label="Gadgets">
      <Dashboard embedded userId={userName || undefined} />
    </div>
  );
}
