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

import React, { createContext, useContext } from "react";
import { DEFAULT_SPA_BOOTSTRAP, type SpaBootstrap } from "./types";

/**
 * {@code null} default so a missing provider is distinguishable from a real
 * {@link DEFAULT_SPA_BOOTSTRAP} value. Callers that only need identity flags
 * should use {@link useSpaBootstrap} (falls back). Explorer uses
 * {@link useSpaBootstrapOptional} so a missing provider is an error state
 * instead of a {@code useContext} crash (#3331 / parent #3329).
 */
const BootstrapContext = createContext<SpaBootstrap | null>(null);

export function BootstrapProvider({
  value,
  children,
}: {
  value: SpaBootstrap;
  children?: React.ReactNode;
}): React.ReactElement {
  return (
    <BootstrapContext.Provider value={value}>{children}</BootstrapContext.Provider>
  );
}

/**
 * Read SPA bootstrap without throwing when the provider is absent or React's
 * dispatcher is unset (bridge remount / dual-React). Returns {@code null} in
 * those cases — do not treat as {@link DEFAULT_SPA_BOOTSTRAP}.
 */
export function useSpaBootstrapOptional(): SpaBootstrap | null {
  try {
    return useContext(BootstrapContext);
  } catch {
    return null;
  }
}

export function useSpaBootstrap(): SpaBootstrap {
  return useSpaBootstrapOptional() ?? DEFAULT_SPA_BOOTSTRAP;
}
