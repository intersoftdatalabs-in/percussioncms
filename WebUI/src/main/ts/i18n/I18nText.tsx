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

import React from "react";
import { i18nKeyAttr } from "./i18nDom";
import { message } from "./message";

type Intrinsic = keyof React.JSX.IntrinsicElements;

export type I18nTextProps<T extends Intrinsic = "span"> = {
  /** Full TMX catalog key. */
  msgKey: string;
  /** Optional format args for {@link message}. */
  args?: unknown[];
  /** Host element type. Default {@code span}. */
  as?: T;
  /** When true (default), add {@code mkd-lang-target} for non-default scan hosts. */
  markTarget?: boolean;
  className?: string;
  children?: never;
} & Omit<React.ComponentPropsWithoutRef<T>, "children" | "className" | "as">;

/**
 * Localized text node with {@code data-i18n-key} for {@code @mkd/language}.
 *
 * Prefer spreading {@link i18nKeyAttr} on existing {@code button}/{@code a}/{@code label}
 * hosts. Use this for headings, empty states, and other non-default scan targets.
 */
export function I18nText<T extends Intrinsic = "span">({
  msgKey,
  args,
  as,
  markTarget = true,
  className,
  ...rest
}: I18nTextProps<T>): React.ReactElement {
  const Tag = (as ?? "span") as React.ElementType;
  const classes = [markTarget ? "mkd-lang-target" : null, className]
    .filter(Boolean)
    .join(" ");
  return (
    <Tag className={classes || undefined} {...i18nKeyAttr(msgKey)} {...rest}>
      {message(msgKey, args)}
    </Tag>
  );
}
