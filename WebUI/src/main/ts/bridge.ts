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
 * <p>This module exposes a {@code mountReactComponent} function on
 * {@code window.PercModernUI} so legacy pages can mount React components into
 * arbitrary DOM elements without a full SPA takeover.</p>
 *
 * <p>Usage from a JSP or legacy JS file:</p>
 * <pre>
 *   window.PercModernUI.mount('my-container', 'HelloWorld', { name: 'Sal' });
 * </pre>
 */

import React from "react";
import { createRoot, type Root } from "react-dom/client";
import { componentRegistry } from "./registry";

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

/**
 * Mounts a registered React component into a DOM element.
 *
 * @param elementId - the id of the target container element
 * @param componentName - the key used to register the component
 * @param props - optional props to pass to the component
 */
export function mountReactComponent(
  elementId: string,
  componentName: string,
  props: Record<string, unknown> = {},
): void {
  const container = document.getElementById(elementId);
  if (!container) {
    console.error(
      `[PercModernUI] Container element "#${elementId}" not found.`,
    );
    return;
  }

  const Component = componentRegistry.get(componentName);
  if (!Component) {
    console.error(
      `[PercModernUI] Component "${componentName}" is not registered.`,
    );
    return;
  }

  // Unmount any existing root at the same element to avoid leaks
  unmountReactComponent(elementId);

  const root = createRoot(container);
  root.render(React.createElement(Component, props));
  activeRoots.set(elementId, root);
}

/**
 * Unmounts a previously mounted React component.
 *
 * @param elementId - the id of the container element
 */
export function unmountReactComponent(elementId: string): void {
  const existing = activeRoots.get(elementId);
  if (existing) {
    existing.unmount();
    activeRoots.delete(elementId);
  }
}

// Expose on the global window so legacy JS/JSP pages can call it
window.PercModernUI = {
  mount: mountReactComponent,
  unmount: unmountReactComponent,
};
