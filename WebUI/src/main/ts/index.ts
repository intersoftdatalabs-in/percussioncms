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
 * Main entry for the modern UI bundle.
 *
 * <ul>
 *   <li>Always registers {@code window.PercModernUI} for residual bridge embeds.</li>
 *   <li>If {@code #perc-login-root} is present, boots the React Login front door.</li>
 *   <li>If SPA root is present ({@code #perc-spa-root}, {@code #root}, {@code #perc-app-root}),
 *       boots the authenticated App shell.</li>
 * </ul>
 */

import React from "react";
import { createRoot } from "react-dom/client";
import "./bridge";
import { LoginPage, readLoginBootstrap } from "./login";

function bootLogin(): void {
  const el = document.getElementById("perc-login-root");
  if (!el) {
    return;
  }
  const bootstrap = readLoginBootstrap();
  createRoot(el).render(React.createElement(LoginPage, { bootstrap }));
  console.info("[PercModernUI] Login SPA mounted.");
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
  void import("./app/main").then((m) => {
    m.boot(el);
  });
}

function boot(): void {
  if (document.getElementById("perc-login-root")) {
    bootLogin();
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
