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
 * Bridge layer for embedding React components inside existing JSP pages.
 *
 * <p>{@code mount}/{@code unmount} remain <strong>synchronous</strong> for hosts.
 * Loads use shared {@link loadComponent} with generation tokens so late resolves
 * after unmount or remount are ignored (§2.9).</p>
 */

import React from "react";
import { createRoot, type Root } from "react-dom/client";
import { isRegisteredComponent, loadComponent } from "./registry";

declare global {
  interface Window {
    PercModernUI?: {
      mount: (
        elementId: string,
        componentName: string,
        props?: Record<string, unknown>,
      ) => void;
      unmount: (elementId: string) => void;
    };
  }
}

const activeRoots = new Map<string, Root>();
/** Generation token per elementId — bumped on each mount/unmount. */
const generations = new Map<string, number>();

function bumpGeneration(elementId: string): number {
  const next = (generations.get(elementId) ?? 0) + 1;
  generations.set(elementId, next);
  return next;
}

/**
 * Mounts a registered React component into a DOM element (sync API; async load).
 */
export function mountReactComponent(
  elementId: string,
  componentName: string,
  props: Record<string, unknown> = {},
): void {
  const container = document.getElementById(elementId);
  if (!container) {
    console.error(`[PercModernUI] Container element "#${elementId}" not found.`);
    return;
  }

  if (!isRegisteredComponent(componentName)) {
    console.error(
      `[PercModernUI] Component "${componentName}" is not registered.`,
    );
    return;
  }

  // Invalidate any prior pending load for this element
  const gen = bumpGeneration(elementId);
  unmountRootOnly(elementId);

  void loadComponent(componentName)
    .then((Component) => {
      if (generations.get(elementId) !== gen) {
        // Stale: unmount or remount happened before resolve
        return;
      }
      const el = document.getElementById(elementId);
      if (!el) {
        return;
      }
      unmountRootOnly(elementId);
      const root = createRoot(el);
      root.render(React.createElement(Component, props));
      activeRoots.set(elementId, root);
    })
    .catch((err) => {
      if (generations.get(elementId) !== gen) {
        return;
      }
      console.error(
        `[PercModernUI] Failed to load component "${componentName}"`,
        err,
      );
      const el = document.getElementById(elementId);
      if (el) {
        el.setAttribute("data-perc-mount-error", "1");
      }
    });
}

function unmountRootOnly(elementId: string): void {
  const existing = activeRoots.get(elementId);
  if (existing) {
    existing.unmount();
    activeRoots.delete(elementId);
  }
}

/**
 * Unmounts a previously mounted React component and invalidates pending loads.
 */
export function unmountReactComponent(elementId: string): void {
  bumpGeneration(elementId);
  unmountRootOnly(elementId);
  const el = document.getElementById(elementId);
  if (el) {
    el.removeAttribute("data-perc-mount-error");
  }
}

window.PercModernUI = {
  mount: mountReactComponent,
  unmount: unmountReactComponent,
};
