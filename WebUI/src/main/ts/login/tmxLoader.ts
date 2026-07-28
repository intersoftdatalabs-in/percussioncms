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

import { normalizeTag } from "./localeLabels";

/**
 * Lazy TMX bundle loader for the login screen.
 *
 * <p>The host page loads a single {@code /tmx/tmx.jsp} bundle for the
 * initial session locale. When the user changes the locale dropdown we
 * want a fresh bundle so {@code I18N.message} resolves against the
 * picked locale. This loader injects a classic (non-module) script
 * element per requested locale and de-duplicates concurrent requests so
 * the most recently selected locale wins without thrash.</p>
 */

const DEFAULT_TMX_BASE = "/tmx/tmx.jsp";

const loaded: Set<string> = new Set();
const inFlight: Map<string, Promise<void>> = new Map();

function buildUrl(locale: string, baseHref: string): string {
  const tag = normalizeTag(locale);
  const sep = baseHref.includes("?") ? "&" : "?";
  return (
    `${baseHref}${sep}mode=js&prefix=perc.ui.` +
    `&sys_lang=${encodeURIComponent(tag)}`
  );
}

/**
 * Ensure the TMX bundle for {@code locale} is loaded into
 * {@code window.I18N}. Resolves once the script's {@code load} event
 * fires. Subsequent calls for the same locale return the cached
 * resolved promise.
 *
 * @param locale   the locale tag to load (normalized via {@link normalizeTag})
 * @param baseHref optional override for the tmx.jsp endpoint
 */
export function ensureTmxLoaded(
  locale: string,
  baseHref: string = DEFAULT_TMX_BASE,
): Promise<void> {
  const tag = normalizeTag(locale);
  if (!tag) {
    return Promise.reject(new Error("tmxLoader: empty locale tag"));
  }
  if (loaded.has(tag)) {
    return Promise.resolve();
  }
  const pending = inFlight.get(tag);
  if (pending) {
    return pending;
  }

  const url = buildUrl(tag, baseHref);

  const promise = new Promise<void>((resolve, reject) => {
    if (typeof document === "undefined") {
      reject(new Error("tmxLoader: no document"));
      return;
    }
    const script = document.createElement("script");
    script.src = url;
    script.async = false;
    script.dataset.percTmxLocale = tag;
    const cleanup = (): void => {
      script.removeEventListener("load", onLoad);
      script.removeEventListener("error", onError);
    };
    const onLoad = (): void => {
      cleanup();
      // Tag stays in DOM so browser HTTP cache serves repeat selections.
      loaded.add(tag);
      inFlight.delete(tag);
      resolve();
    };
    const onError = (): void => {
      cleanup();
      script.parentNode?.removeChild(script);
      inFlight.delete(tag);
      reject(new Error(`tmxLoader: failed to load ${url}`));
    };
    script.addEventListener("load", onLoad);
    script.addEventListener("error", onError);
    document.head.appendChild(script);
  });

  inFlight.set(tag, promise);
  return promise;
}

/** Test-only: clear the module-level cache between unit tests. */
export function __resetTmxLoaderCache(): void {
  loaded.clear();
  inFlight.clear();
}