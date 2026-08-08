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

import React, { lazy } from "react";
import { LazyRouteFrame } from "./RouteErrorBoundary";

const ProfileShellLazy = lazy(() =>
  import("../../profile/ProfileShell").then((m) => ({ default: m.ProfileShell })),
);

/**
 * SPA My profile route — available to any authenticated user (self profile hub).
 * No admin/designer gate: slice 1 is shell + entry only.
 */
export function ProfileRoute(): React.ReactElement {
  return (
    <LazyRouteFrame
      label="Profile"
      fallback={
        <div data-testid="route-profile-loading" style={{ padding: "1.5rem" }}>
          Loading…
        </div>
      }
    >
      <ProfileShellLazy embedded />
    </LazyRouteFrame>
  );
}
