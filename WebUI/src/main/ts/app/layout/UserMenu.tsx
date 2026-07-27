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
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import styles from "./AppLayout.module.css";

export function UserMenu(): React.ReactElement {
  const { userName } = useSpaBootstrap();
  const name = userName?.trim() || "user";

  return (
    <div className={styles.userMenu} data-testid="perc-spa-user-menu">
      <span>
        Signed in as{" "}
        <span className={styles.userName} data-testid="perc-spa-user-name">
          {name}
        </span>
      </span>
      <a className={styles.logoutLink} href="/logout" data-testid="perc-spa-logout">
        Logout
      </a>
    </div>
  );
}
