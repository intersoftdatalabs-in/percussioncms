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

import { useCallback, useEffect, useLayoutEffect, useRef } from "react";

/** Last structure-toolbar control that opened a dialog (#3350). */
let pendingOpener: HTMLElement | null = null;

/**
 * Record the control that opened a structure dialog so focus can return
 * after Escape / cancel. Prefer the clicked button over
 * {@code document.activeElement} (jsdom click does not move focus).
 */
export function captureDialogOpener(el: EventTarget | null): void {
  pendingOpener = el instanceof HTMLElement ? el : null;
}

function takePendingOpener(): HTMLElement | null {
  const fromClick = pendingOpener;
  pendingOpener = null;
  if (fromClick) {
    return fromClick;
  }
  const ae = document.activeElement;
  return ae instanceof HTMLElement ? ae : null;
}

/** Keep the latest callback in a ref without re-subscribing effects. */
function useLatest(fn: () => void): { current: () => void } {
  const ref = useRef(fn);
  ref.current = fn;
  return ref;
}

/** Focus when the element is still attached; no-op otherwise. */
function focusSafely(el: HTMLElement | null | undefined): void {
  if (!el) {
    return;
  }
  if (typeof el.focus !== "function" || !document.contains(el)) {
    return;
  }
  el.focus();
}

/** Take the captured opener out of the ref and focus it. */
function restoreOpener(openerRef: { current: HTMLElement | null }): void {
  const el = openerRef.current;
  openerRef.current = null;
  focusSafely(el);
}

/** Capture the opener on the first open render (#3350). */
function useOpenerCapture(open: boolean): {
  openerRef: { current: HTMLElement | null };
  wasOpenRef: { current: boolean };
} {
  const openerRef = useRef<HTMLElement | null>(null);
  const wasOpenRef = useRef(false);
  if (open && !wasOpenRef.current) {
    openerRef.current = takePendingOpener();
  }
  wasOpenRef.current = open;
  return { openerRef, wasOpenRef };
}

/** Return focus when the dialog closes. */
function useOpenerRestore(
  open: boolean,
  openerRef: { current: HTMLElement | null },
): void {
  useLayoutEffect(() => {
    if (open) {
      return;
    }
    restoreOpener(openerRef);
  }, [open, openerRef]);
}

/** Return focus when unmounted while still open (#6988, shared for #7003). */
function useUnmountOpenerRestore(
  openerRef: { current: HTMLElement | null },
  wasOpenRef: { current: boolean },
): void {
  // Read through a stable getter: the cleanup needs the unmount-time value,
  // which the exhaustive-deps rule flags when read as `.current` inline.
  const isStillOpen = useCallback(
    () => wasOpenRef.current,
    [wasOpenRef],
  );
  useEffect(() => {
    return () => {
      if (!isStillOpen()) {
        return;
      }
      restoreOpener(openerRef);
    };
  }, [openerRef, isStillOpen]);
}

function isEscapeEvent(ev: KeyboardEvent): boolean {
  return ev.key === "Escape";
}

/** Build the window keydown handler so the effect stays flat. */
function createEscapeHandler(
  isBusy: () => boolean,
  fire: () => void,
): (ev: KeyboardEvent) => void {
  return (ev: KeyboardEvent) => {
    if (!isEscapeEvent(ev) || isBusy()) {
      return;
    }
    ev.preventDefault();
    ev.stopPropagation();
    fire();
  };
}

/** Close on Escape while open and not busy. */
function useEscapeKey(
  open: boolean,
  busy: boolean,
  fireRef: { current: () => void },
): void {
  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = createEscapeHandler(
      () => busy,
      () => fireRef.current(),
    );
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, busy, fireRef]);
}

/**
 * Close Architecture dialogs on Escape when open and not busy (#3098 a11y).
 * Peer: Design TemplateDetailDrawer Escape handler.
 *
 * <p>{@code onCancel} is held in a ref so inline arrow call-sites do not
 * teardown/re-register the window keydown listener every parent render.</p>
 *
 * <p>#3350: capture the opener (toolbar click or current focus) on the first
 * open render and restore focus when the dialog closes.</p>
 */
export function useDialogEscape(
  open: boolean,
  busy: boolean,
  onCancel: () => void,
): void {
  const fireRef = useLatest(onCancel);
  const { openerRef, wasOpenRef } = useOpenerCapture(open);
  useOpenerRestore(open, openerRef);
  useUnmountOpenerRestore(openerRef, wasOpenRef);
  useEscapeKey(open, busy, fireRef);
}

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

function focusableIn(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) => el.tabIndex !== -1 && !el.hasAttribute("disabled"),
  );
}

/** Preferred initial control when it sits inside the dialog root. */
function resolveInitialFocus(
  root: HTMLElement,
  preferred: HTMLElement | null | undefined,
): HTMLElement | undefined {
  if (preferred && root.contains(preferred)) {
    return preferred;
  }
  return focusableIn(root)[0];
}

/** Focus the trap's starting control (first focusable by default). */
function focusInitialControl(
  root: HTMLElement,
  preferred: HTMLElement | null | undefined,
): void {
  focusSafely(resolveInitialFocus(root, preferred));
}

/**
 * Pick the wrap target for a Tab step, or null when focus already cycles.
 *
 * @param inside whether the active element sits inside the dialog root.
 */
function trapTarget(
  shift: boolean,
  inside: boolean,
  active: Element | null,
  first: HTMLElement,
  last: HTMLElement,
): HTMLElement | null {
  if (shift) {
    return active === first || !inside ? last : null;
  }
  return active === last || !inside ? first : null;
}

/** Build the container keydown handler so the effect stays flat. */
function createTabTrapHandler(root: HTMLElement): (ev: KeyboardEvent) => void {
  return (ev: KeyboardEvent) => {
    if (ev.key !== "Tab") {
      return;
    }
    const list = focusableIn(root);
    if (list.length === 0) {
      ev.preventDefault();
      return;
    }
    const first = list[0];
    const last = list[list.length - 1];
    const target = trapTarget(
      ev.shiftKey,
      root.contains(document.activeElement),
      document.activeElement,
      first,
      last,
    );
    if (target) {
      ev.preventDefault();
      target.focus();
    }
  };
}

/**
 * Keep Tab/Shift+Tab inside an open modal (WCAG 2.1 focus trap).
 * Call with a ref on the {@code role="dialog"} root.
 *
 * <p>{@code initialRef} names the control that takes initial focus
 * (destructive confirms focus Cancel so an accidental Enter cannot
 * confirm; pickers focus their text field; wizards their primary
 * button). The ref must resolve inside the dialog root; when it is
 * absent or outside the root the first focusable control is used.
 * When focus is somehow outside the container, Tab re-anchors to the
 * first control (Shift+Tab to the last), and Tab with no focusable
 * control at all is swallowed.</p>
 */
export function useDialogFocusTrap(
  open: boolean,
  containerRef: { current: HTMLElement | null },
  initialRef?: { current: HTMLElement | null },
): void {
  useEffect(() => {
    if (!open) {
      return;
    }
    const root = containerRef.current;
    if (!root) {
      return;
    }
    focusInitialControl(root, initialRef?.current);
    const onKey = createTabTrapHandler(root);
    root.addEventListener("keydown", onKey);
    return () => root.removeEventListener("keydown", onKey);
  }, [open, containerRef, initialRef]);
}
