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

import React, {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";
import { LocaleFlag } from "./LocaleFlag";
import { localeLabel } from "./localeLabels";
import type { LoginLocaleOption } from "./types";
import styles from "./LocaleSelect.module.css";

export interface LocaleSelectProps {
  id: string;
  locales: LoginLocaleOption[];
  value: string;
  onChange: (next: string) => void;
  /** Field name for the hidden input (form POST). Default {@code j_locale}. */
  name?: string;
  /** Tab index for the combobox button. */
  tabIndex?: number;
  "data-testid"?: string;
}

/**
 * Accessible custom locale combobox with SVG flag icons.
 *
 * <p>Native {@code <select>} cannot render HTML/SVG inside options, so this
 * replaces it for the login screen while still posting {@code j_locale} via a
 * hidden input. Keyboard: ArrowUp/Down, Home/End, Enter/Space, Escape.</p>
 */
export function LocaleSelect({
  id,
  locales,
  value,
  onChange,
  name = "j_locale",
  tabIndex,
  "data-testid": testId = "perc-login-locale",
}: LocaleSelectProps): React.ReactElement {
  const listId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLUListElement>(null);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);

  const selectedIndex = useMemo(() => {
    const i = locales.findIndex((l) => l.name === value);
    return i >= 0 ? i : 0;
  }, [locales, value]);

  const selected = locales[selectedIndex] ?? locales[0];
  const selectedLabel = selected
    ? localeLabel(selected.name, selected.name, selected.displayName)
    : value;

  useEffect(() => {
    if (open) {
      setActiveIndex(selectedIndex);
    }
  }, [open, selectedIndex]);

  // Close on outside click
  useEffect(() => {
    if (!open) {
      return;
    }
    const onDoc = (e: MouseEvent): void => {
      const t = e.target as Node | null;
      if (rootRef.current && t && !rootRef.current.contains(t)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  // Scroll active option into view
  useEffect(() => {
    if (!open || !listRef.current) {
      return;
    }
    const el = listRef.current.querySelector<HTMLElement>(
      `[data-index="${activeIndex}"]`,
    );
    // jsdom does not implement scrollIntoView
    if (el && typeof el.scrollIntoView === "function") {
      el.scrollIntoView({ block: "nearest" });
    }
  }, [activeIndex, open]);

  const commit = useCallback(
    (index: number) => {
      const loc = locales[index];
      if (!loc) {
        return;
      }
      onChange(loc.name);
      setOpen(false);
    },
    [locales, onChange],
  );

  const onButtonKeyDown = (e: React.KeyboardEvent): void => {
    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        if (!open) {
          setOpen(true);
        } else {
          setActiveIndex((i) => Math.min(i + 1, locales.length - 1));
        }
        break;
      case "ArrowUp":
        e.preventDefault();
        if (!open) {
          setOpen(true);
        } else {
          setActiveIndex((i) => Math.max(i - 1, 0));
        }
        break;
      case "Home":
        if (open) {
          e.preventDefault();
          setActiveIndex(0);
        }
        break;
      case "End":
        if (open) {
          e.preventDefault();
          setActiveIndex(Math.max(0, locales.length - 1));
        }
        break;
      case "Enter":
      case " ":
        e.preventDefault();
        if (!open) {
          setOpen(true);
        } else {
          commit(activeIndex);
        }
        break;
      case "Escape":
        if (open) {
          e.preventDefault();
          setOpen(false);
        }
        break;
      default:
        break;
    }
  };

  return (
    <div className={styles.root} ref={rootRef}>
      {/* Hidden native field for multipart form POST to /login */}
      <input
        type="hidden"
        name={name}
        value={value}
        data-testid={`${testId}-value`}
      />

      <button
        type="button"
        id={id}
        className={styles.trigger}
        tabIndex={tabIndex}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((o) => !o)}
        onKeyDown={onButtonKeyDown}
        data-testid={testId}
      >
        {selected ? (
          <LocaleFlag locale={selected.name} className={styles.triggerFlag} />
        ) : null}
        <span className={styles.triggerLabel}>{selectedLabel}</span>
        <span className={styles.chevron} aria-hidden="true">
          ▾
        </span>
      </button>

      {open ? (
        <ul
          ref={listRef}
          id={listId}
          className={styles.list}
          role="listbox"
          aria-labelledby={id}
          tabIndex={-1}
          data-testid={`${testId}-list`}
        >
          {locales.map((loc, index) => {
            const label = localeLabel(loc.name, loc.name, loc.displayName);
            const selectedOpt = loc.name === value;
            const active = index === activeIndex;
            return (
              <li
                key={loc.name}
                id={`${listId}-opt-${index}`}
                role="option"
                aria-selected={selectedOpt}
                data-index={index}
                data-testid={`${testId}-option-${loc.name}`}
                className={[
                  styles.option,
                  selectedOpt ? styles.optionSelected : "",
                  active ? styles.optionActive : "",
                ]
                  .filter(Boolean)
                  .join(" ")}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => commit(index)}
              >
                <LocaleFlag locale={loc.name} className={styles.optionFlag} />
                <span className={styles.optionLabel}>{label}</span>
              </li>
            );
          })}
        </ul>
      ) : null}
    </div>
  );
}
