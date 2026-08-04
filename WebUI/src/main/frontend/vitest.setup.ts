/**
 * Vitest Setup File
 *
 * This file configures test environment and global test utilities.
 * It runs before all tests to set up matchers and global test helpers.
 */

import "@testing-library/jest-dom";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// jsdom stubs used by axe-core / chart-ish components — avoid unhandled
// "Not implemented" errors that fail the suite with exit code 1.
if (typeof HTMLCanvasElement !== "undefined") {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (HTMLCanvasElement.prototype as any).getContext = function getContext() {
    return {
      fillRect: () => undefined,
      clearRect: () => undefined,
      getImageData: () => ({ data: new Uint8ClampedArray(0) }),
      putImageData: () => undefined,
      createImageData: () => ({ data: new Uint8ClampedArray(0) }),
      setTransform: () => undefined,
      drawImage: () => undefined,
      save: () => undefined,
      fillText: () => undefined,
      restore: () => undefined,
      beginPath: () => undefined,
      moveTo: () => undefined,
      lineTo: () => undefined,
      closePath: () => undefined,
      stroke: () => undefined,
      translate: () => undefined,
      scale: () => undefined,
      rotate: () => undefined,
      arc: () => undefined,
      fill: () => undefined,
      measureText: () => ({ width: 0 }),
      transform: () => undefined,
      rect: () => undefined,
      clip: () => undefined,
    };
  };
}

// getComputedStyle(elt, pseudoElt) is not fully implemented in jsdom.
const originalGetComputedStyle = window.getComputedStyle.bind(window);
window.getComputedStyle = ((elt: Element, pseudoElt?: string | null) => {
  if (pseudoElt) {
    return originalGetComputedStyle(elt);
  }
  return originalGetComputedStyle(elt);
}) as typeof window.getComputedStyle;

/**
 * jsdom does not implement full document navigation. Assigning
 * {@code window.location.href} or calling {@code assign}/{@code replace}
 * with a non-hash URL emits "Not implemented: navigation …" on the
 * virtual console and can fail Vitest suites when production code
 * redirects (Dashboard legacy flag, HomeShell open item, safeNavigate,
 * session 401 redirect, etc.).
 *
 * <p>Install a plain Location-like mock that applies navigations via
 * {@code history.pushState}/{@code replaceState} so pathname/search/hash
 * stay readable for React Router and tests. Production navigation
 * semantics are unchanged outside the test runner.</p>
 *
 * <p>Vitest's jsdom environment exposes a configurable
 * {@code window.location}; pure Node JSDOM often does not — in that case
 * we no-op rather than throw.</p>
 */
function installJsdomLocationNavigationMock(): void {
  if (typeof window === "undefined" || typeof window.location === "undefined") {
    return;
  }

  const locationDesc = Object.getOwnPropertyDescriptor(window, "location");
  if (!locationDesc?.configurable) {
    return;
  }

  let current: URL;
  try {
    current = new URL(window.location.href);
  } catch {
    current = new URL("http://localhost:3000/");
  }

  const origPushState = window.history.pushState.bind(window.history);
  const origReplaceState = window.history.replaceState.bind(window.history);

  const syncFromHistoryUrl = (url: string | URL | null | undefined): void => {
    if (url == null || url === "") {
      return;
    }
    try {
      current = new URL(String(url), current.href);
    } catch {
      // ignore unparseable history URLs
    }
  };

  // Keep mock state aligned when tests (or React Router) call history APIs.
  window.history.pushState = function pushState(data, unused, url) {
    const result = origPushState(data, unused, url as string | URL | null | undefined);
    syncFromHistoryUrl(url as string | URL | null | undefined);
    return result;
  };
  window.history.replaceState = function replaceState(data, unused, url) {
    const result = origReplaceState(
      data,
      unused,
      url as string | URL | null | undefined,
    );
    syncFromHistoryUrl(url as string | URL | null | undefined);
    return result;
  };

  window.addEventListener("popstate", () => {
    try {
      if (typeof document !== "undefined" && document.URL) {
        current = new URL(document.URL);
      }
    } catch {
      // ignore
    }
  });

  const parseRelative = (input: string | URL): URL =>
    new URL(String(input), current.href);

  const applyUrl = (next: URL, replace: boolean): void => {
    current = next;
    const path = `${next.pathname}${next.search}${next.hash}`;
    try {
      if (replace) {
        origReplaceState(window.history.state, "", path);
      } else {
        origPushState(window.history.state, "", path);
      }
    } catch {
      // history rejects some cross-origin / invalid URLs — swallow in tests
    }
  };

  const mockLocation = {
    get href() {
      return current.href;
    },
    set href(value: string) {
      try {
        applyUrl(parseRelative(value), false);
      } catch {
        // ignore invalid assignments
      }
    },
    get protocol() {
      return current.protocol;
    },
    set protocol(value: string) {
      const next = new URL(current.href);
      next.protocol = value;
      current = next;
    },
    get host() {
      return current.host;
    },
    set host(value: string) {
      const next = new URL(current.href);
      next.host = value;
      current = next;
    },
    get hostname() {
      return current.hostname;
    },
    set hostname(value: string) {
      const next = new URL(current.href);
      next.hostname = value;
      current = next;
    },
    get port() {
      return current.port;
    },
    set port(value: string) {
      const next = new URL(current.href);
      next.port = value;
      current = next;
    },
    get pathname() {
      return current.pathname;
    },
    set pathname(value: string) {
      try {
        const next = new URL(current.href);
        next.pathname = value;
        applyUrl(next, true);
      } catch {
        // ignore
      }
    },
    get search() {
      return current.search;
    },
    set search(value: string) {
      try {
        const next = new URL(current.href);
        next.search = value;
        applyUrl(next, true);
      } catch {
        // ignore
      }
    },
    get hash() {
      return current.hash;
    },
    set hash(value: string) {
      try {
        const next = new URL(current.href);
        next.hash = value;
        applyUrl(next, true);
      } catch {
        // ignore
      }
    },
    get origin() {
      return current.origin;
    },
    get ancestorOrigins(): DOMStringList {
      return [] as unknown as DOMStringList;
    },
    assign(url: string | URL) {
      try {
        applyUrl(parseRelative(url), false);
      } catch {
        // ignore
      }
    },
    replace(url: string | URL) {
      try {
        applyUrl(parseRelative(url), true);
      } catch {
        // ignore
      }
    },
    reload() {
      // no-op in unit tests
    },
    toString() {
      return current.href;
    },
  } as Location;

  // Vitest jsdom: window.location is configurable — replace with mock.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  delete (window as any).location;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (window as any).location = mockLocation;

  // Soft-filter residual navigation jsdomErrors if any code path still hits
  // the real Location (e.g. document.defaultView edge cases).
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const virtualConsole = (window as any)._virtualConsole;
  if (virtualConsole && typeof virtualConsole.on === "function") {
    virtualConsole.on("jsdomError", (error: Error) => {
      if (
        error &&
        typeof error.message === "string" &&
        /Not implemented: navigation/i.test(error.message)
      ) {
        return;
      }
      // Re-surface non-navigation jsdom errors to stderr for visibility.
      // eslint-disable-next-line no-console
      console.error(error);
    });
  }
}

installJsdomLocationNavigationMock();
