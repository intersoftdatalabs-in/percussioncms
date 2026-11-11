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

import React from "react";
import { TipTapFieldWidget } from "./TipTapFieldWidget";

export interface HtmlFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  invalid?: boolean;
  onChange: (value: string) => void;
}

/**
 * HTML field widget for #5622. TipTap-only since #5902 (the
 * {@code ?engine=tinymce} escape hatch from #5758 is removed): every HTML
 * field renders the bundled TipTap editor, so the React editor never
 * fetches {@code /sys_resources/tinymce} under any param.
 */
export function HtmlFieldWidget({
  name,
  value,
  readOnly,
  invalid = false,
  onChange,
}: HtmlFieldWidgetProps): React.ReactElement {
  return (
    <TipTapFieldWidget
      name={name}
      value={value}
      readOnly={readOnly}
      invalid={invalid}
      onChange={onChange}
    />
  );
}
