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
import styles from "../EditorHost.module.css";
import {
  fromWidgetValue,
  toWidgetValue,
  type EditorDateKind,
} from "../dateField";

export interface DateFieldWidgetProps {
  name: string;
  value: string;
  kind: EditorDateKind;
  readOnly: boolean;
  invalid?: boolean;
  required?: boolean;
  onChange: (value: string) => void;
}

/**
 * Native date / datetime-local control for sys_CalendarSimple and datetime fields.
 */
export function DateFieldWidget({
  name,
  value,
  kind,
  readOnly,
  invalid,
  required,
  onChange,
}: DateFieldWidgetProps): React.ReactElement {
  const widget = toWidgetValue(kind, value);
  return (
    <input
      className={`${styles.input} ${readOnly ? styles.readonly : ""}`}
      data-testid={`editor-field-${name}`}
      data-editor-kind={kind}
      type={kind === "datetime" ? "datetime-local" : "date"}
      name={name}
      value={widget}
      readOnly={readOnly}
      disabled={readOnly}
      aria-invalid={invalid ? true : undefined}
      aria-required={required ? true : undefined}
      onChange={(e) => onChange(fromWidgetValue(kind, e.target.value))}
    />
  );
}
