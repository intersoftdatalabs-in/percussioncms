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

import React, { createContext, useContext } from "react";
import { DEFAULT_SPA_BOOTSTRAP, type SpaBootstrap } from "./types";

const BootstrapContext = createContext<SpaBootstrap>(DEFAULT_SPA_BOOTSTRAP);

export function BootstrapProvider({
  value,
  children,
}: {
  value: SpaBootstrap;
  children: React.ReactNode;
}): React.ReactElement {
  return (
    <BootstrapContext.Provider value={value}>{children}</BootstrapContext.Provider>
  );
}

export function useSpaBootstrap(): SpaBootstrap {
  return useContext(BootstrapContext);
}
