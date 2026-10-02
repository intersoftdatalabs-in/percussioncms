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

/**
 * Explorer approve of one page or asset onto the incremental queue (#5056).
 * HTTP errors propagate; callers must not treat them as success.
 */

import { post } from "../client";
import { SERVICES_ROOT } from "../paths";

export function explorerIncrementalApproveUrl(contentId: string): string {
  const id = String(contentId ?? "").trim();
  return `${SERVICES_ROOT}/sitemanage/publish/incremental/explorer/${encodeURIComponent(id)}/approve`;
}

export async function approveSelectedItemToIncrementalQueue(
  contentId: string,
): Promise<void> {
  const id = String(contentId ?? "").trim();
  if (!id) {
    throw new Error("approveSelectedItemToIncrementalQueue requires contentId");
  }
  await post(explorerIncrementalApproveUrl(id));
}
