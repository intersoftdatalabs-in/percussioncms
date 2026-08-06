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
 * Welcome widget: static welcome greeting and quick action links.
 *
 * <p>This is the first dashboard widget, providing a welcome message,
 * quick links to common tasks, and getting started resources.</p>
 */

import React from "react";
import { message, MSG } from "../i18n/message";
import { styles } from "./dashboard.styles";

export interface WelcomeWidgetProps {
  userName?: string;
}

export const WelcomeWidget: React.FC<WelcomeWidgetProps> = ({
  userName = "User",
}) => {
  const now = new Date();
  const hour = now.getHours();
  let greetingKey: string = MSG.WELCOME_GREETING_MORNING;
  if (hour >= 12 && hour < 18) greetingKey = MSG.WELCOME_GREETING_AFTERNOON;
  if (hour >= 18) greetingKey = MSG.WELCOME_GREETING_EVENING;

  return (
    <div style={styles.widget}>
      <h3 style={styles.widgetTitle}>{message(MSG.GADGET_WELCOME)}</h3>
      <div style={styles.widgetContent}>
        <p>
          {message(greetingKey)}, <strong>{userName}</strong>!
        </p>
        <p style={{ color: "#666", fontSize: "14px", marginTop: "12px" }}>
          {message(MSG.WELCOME_BLURB)}
        </p>
        <ul style={{ marginTop: "12px", paddingLeft: "20px" }}>
          <li>
            <a href="/cm/app/sitemanage" style={styles.link}>
              {message(MSG.WELCOME_LINK_SITEMANAGE)}
            </a>
          </li>
          <li>
            <a href="/cm/app/webmgt" style={styles.link}>
              {message(MSG.WELCOME_LINK_WEBMGT)}
            </a>
          </li>
          <li>
            <a href="/cm/app/admin" style={styles.link}>
              {message(MSG.NAV_ADMINISTRATION)}
            </a>
          </li>
          <li>
            <a href="/Rhythmyx/ui/admin/console.faces" style={styles.link}>
              {message(MSG.WELCOME_LINK_ADMINCONSOLE)}
            </a>
          </li>
        </ul>
      </div>
    </div>
  );
};
