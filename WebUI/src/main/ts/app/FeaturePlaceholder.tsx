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
import { useParams } from "react-router";
import styles from "./layout/AppLayout.module.css";

export interface FeaturePlaceholderProps {
  title: string;
  /** Optional legacy full-page exit while shell embed lands in PR-3+ */
  legacyHref?: string;
  testId?: string;
}

/**
 * Route placeholder until feature shells are mounted embedded (PR-3 / PR-4).
 */
export function FeaturePlaceholder({
  title,
  legacyHref,
  testId = "perc-feature-placeholder",
}: FeaturePlaceholderProps): React.ReactElement {
  const params = useParams();
  const detail = Object.entries(params)
    .filter(([, v]) => v != null && String(v).length > 0)
    .map(([k, v]) => `${k}=${v}`)
    .join(", ");

  return (
    <div className={styles.placeholder} data-testid={testId}>
      <h1 data-testid={`${testId}-title`}>{title}</h1>
      <p>
        SPA route is active
        {detail ? (
          <>
            {" "}
            (<code>{detail}</code>)
          </>
        ) : null}
        . Full feature shell mounts in a follow-on PR; use TopNav or the
        temporary product link below.
      </p>
      {legacyHref ? (
        <p>
          <a href={legacyHref} data-testid={`${testId}-legacy`}>
            Open current product UI
          </a>
        </p>
      ) : null}
    </div>
  );
}
