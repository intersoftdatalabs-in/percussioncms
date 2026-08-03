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

/**
 * Ambient types for {@code country-flag-icons/react/3x2}.
 *
 * The package ships {@code index.d.ts}, but WebUI tsconfig maps {@code "*"} →
 * {@code node_modules/*}, which resolves the nested package via package.json
 * {@code "main"} ({@code index.cjs}). TypeScript then looks for {@code index.d.cts}
 * and never picks up the adjacent {@code .d.ts} (package {@code "exports".types}
 * is bypassed by path mapping).
 *
 * Keep this declaration until path mapping no longer shadows package exports,
 * or the package ships {@code index.d.cts}.
 */
declare module "country-flag-icons/react/3x2" {
  import type { FC, SVGProps } from "react";

  type FlagComponent = FC<SVGProps<SVGSVGElement> & { title?: string }>;

  /** ISO 3166-1 alpha-2 (and some subdivision) flag React components. */
  const flags: { [countryCode: string]: FlagComponent };
  export = flags;
}
