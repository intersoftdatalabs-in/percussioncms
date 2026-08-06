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

import React, { lazy, useCallback } from "react";
import { useNavigate, useParams } from "react-router";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import { loadComponent } from "../../registry";
import type { HomeSection } from "../../home/deepLinkMap";
import { LazyRouteFrame } from "./RouteErrorBoundary";

const HomeShellLazy = lazy(() =>
  loadComponent("HomeShell").then((C) => ({ default: C })),
);

/**
 * SPA Home route — product default landing after login.
 * Dashboard gadgets compose as Home section {@code gadgets} (PR-7), not a peer SPA route.
 * Section tabs update the client path ({@code /home}, {@code /home/library}, …).
 */
export function HomeRoute(): React.ReactElement {
  const { section } = useParams();
  const navigate = useNavigate();
  const { isAdmin } = useSpaBootstrap();

  const onSectionChange = useCallback(
    (next: HomeSection) => {
      // recent is the default /home index (no extra segment)
      if (next === "recent") {
        navigate("/home");
      } else {
        navigate(`/home/${next}`);
      }
    },
    [navigate],
  );

  return (
    <LazyRouteFrame
      label="Home"
      fallback={
        <div data-testid="route-home-loading" style={{ padding: "1.5rem" }}>
          Loading Home…
        </div>
      }
    >
      <HomeShellLazy
        embedded
        initialSection={section}
        isAdmin={isAdmin}
        onSectionChange={onSectionChange}
      />
    </LazyRouteFrame>
  );
}
