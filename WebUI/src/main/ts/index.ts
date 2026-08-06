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
 * Main entry for the modern UI bundle.
 *
 * <ul>
 *   <li>Always registers {@code window.PercModernUI} for residual bridge embeds.</li>
 *   <li>If {@code #perc-login-root} is present, boots the React Login front door.</li>
 *   <li>If {@code #perc-logout-root} is present, boots the React Logout confirmation.</li>
 *   <li>If SPA root is present ({@code #perc-spa-root}, {@code #root}, {@code #perc-app-root}),
 *       boots the authenticated App shell.</li>
 * </ul>
 */

import React from "react";
import { createRoot } from "react-dom/client";
import "./bridge";
import { ensureModernStyles } from "./ensureModernStyles";
import { LoginPage, readLoginBootstrap } from "./login";
import { LogoutPage, readLogoutBootstrap } from "./logout";

// Entry CSS is not auto-linked without an HTML entry — inject before any boot.
ensureModernStyles();

function bootLogin(): void {
  const el = document.getElementById("perc-login-root");
  if (!el) {
    return;
  }
  const bootstrap = readLoginBootstrap();
  createRoot(el).render(React.createElement(LoginPage, { bootstrap }));
  console.info("[PercModernUI] Login SPA mounted.");
}

function bootLogout(): void {
  const el = document.getElementById("perc-logout-root");
  if (!el) {
    return;
  }
  const bootstrap = readLogoutBootstrap();
  createRoot(el).render(React.createElement(LogoutPage, { bootstrap }));
  console.info("[PercModernUI] Logout SPA mounted.");
}

function findSpaRoot(): HTMLElement | null {
  return (
    document.getElementById("perc-spa-root") ??
    document.getElementById("root") ??
    document.getElementById("perc-app-root")
  );
}

function bootSpa(): void {
  const el = findSpaRoot();
  if (!el) {
    return;
  }
  void import("./app/main")
    .then((m) => {
      m.boot(el);
    })
    .catch((err) => {
      console.error("[PercModernUI] Failed to load SPA app module", err);
      el.setAttribute("data-perc-spa-boot-error", "1");
      el.textContent =
        "Unable to load the modern UI. Check the browser console or reload the page.";
    });
}

function boot(): void {
  if (document.getElementById("perc-login-root")) {
    bootLogin();
    return;
  }
  if (document.getElementById("perc-logout-root")) {
    bootLogout();
    return;
  }
  if (findSpaRoot()) {
    bootSpa();
    return;
  }
  console.info("[PercModernUI] bridge ready (no SPA root)");
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", boot);
} else {
  boot();
}

console.info("[PercModernUI] Modern UI bridge loaded.");
