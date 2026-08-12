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

import { useEffect, useRef } from "react";

/**
 * Close Architecture dialogs on Escape when open and not busy (#3098 a11y).
 * Peer: Design TemplateDetailDrawer Escape handler.
 *
 * <p>{@code onCancel} is held in a ref so inline arrow call-sites do not
 * teardown/re-register the window keydown listener every parent render.</p>
 */
export function useDialogEscape(
  open: boolean,
  busy: boolean,
  onCancel: () => void,
): void {
  const onCancelRef = useRef(onCancel);
  onCancelRef.current = onCancel;

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Escape") {
        return;
      }
      if (busy) {
        return;
      }
      ev.preventDefault();
      ev.stopPropagation();
      onCancelRef.current();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, busy]);
}
