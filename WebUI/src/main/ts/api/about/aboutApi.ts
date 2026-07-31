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

import { get } from "../client";
import { PATHS } from "../paths";

/**
 * Server version and third-party license disclaimer (issue #1529).
 *
 * <p>Mirrors {@code com.percussion.rest.about.AboutDetail} - the same text the server prints to
 * the console at startup (with the {@code [Server]} log prefix).
 */
export interface AboutDetail {
  productName?: string;
  versionString?: string;
  copyright?: string;
  thirdPartyCopyright?: string;
}

/** Fetches the server version and license disclaimer shown in the About dialog. */
export async function fetchAbout(init?: {
  signal?: AbortSignal;
}): Promise<AboutDetail> {
  return get<AboutDetail>(PATHS.ABOUT, undefined, init);
}
