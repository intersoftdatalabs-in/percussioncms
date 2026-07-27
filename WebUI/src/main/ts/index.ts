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
 *   <li>If {@code #perc-spa-root} is present, boots the authenticated SPA landing.</li>
 * </ul>
 */

import React from "react";
import { createRoot } from "react-dom/client";
import "./bridge";
import { LandingShell } from "./app/LandingShell";
import { LoginPage, readLoginBootstrap, readSpaLandingBootstrap } from "./login";

function bootLogin(): void {
  const el = document.getElementById("perc-login-root");
  if (!el) {
    return;
  }
  const bootstrap = readLoginBootstrap();
  createRoot(el).render(React.createElement(LoginPage, { bootstrap }));
  console.info("[PercModernUI] Login SPA mounted.");
}

function bootSpaLanding(): void {
  const el = document.getElementById("perc-spa-root");
  if (!el) {
    return;
  }
  const bootstrap = readSpaLandingBootstrap();
  createRoot(el).render(React.createElement(LandingShell, { bootstrap }));
  console.info("[PercModernUI] SPA landing mounted.");
}

function boot(): void {
  // Login page is public and exclusive; SPA landing is authenticated.
  if (document.getElementById("perc-login-root")) {
    bootLogin();
    return;
  }
  if (document.getElementById("perc-spa-root")) {
    bootSpaLanding();
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", boot);
} else {
  boot();
}

console.info("[PercModernUI] Modern UI bridge loaded.");
