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
import { message } from "../../i18n/message";
import styles from "../EditorHost.module.css";
import { EDITOR_MSG } from "../messages";
import {
  parseTableField,
  serializeTableField,
  type EditorTableModel,
} from "../tableField";

export function TableFieldWidget({
  name,
  value,
  readOnly,
  invalid,
  required,
  onChange,
}: {
  name: string;
  value: string;
  readOnly: boolean;
  invalid?: boolean;
  required?: boolean;
  onChange: (value: string) => void;
}): React.ReactElement {
  const model = parseTableField(value);

  function commit(next: EditorTableModel): void {
    onChange(serializeTableField(next));
  }

  function setCell(rowIndex: number, colIndex: number, cell: string): void {
    const rows = model.rows.map((row, index) => {
      if (index !== rowIndex) {
        return row;
      }
      const copy = [...row];
      copy[colIndex] = cell;
      return copy;
    });
    commit({ columns: model.columns, rows });
  }

  function addRow(): void {
    commit({
      columns: model.columns,
      rows: [...model.rows, model.columns.map(() => "")],
    });
  }

  function removeRow(rowIndex: number): void {
    commit({
      columns: model.columns,
      rows: model.rows.filter((_, index) => index !== rowIndex),
    });
  }

  return (
    <div
      className={styles.tableField}
      data-testid={`editor-field-${name}`}
      data-editor-kind="table"
      aria-invalid={invalid ? true : undefined}
      aria-required={required ? true : undefined}
    >
      <table className={styles.tableGrid}>
        <thead>
          <tr>
            {model.columns.map((column) => (
              <th key={column} scope="col">
                {column}
              </th>
            ))}
            {readOnly ? null : <th scope="col" />}
          </tr>
        </thead>
        <tbody>
          {model.rows.map((row, rowIndex) => (
            <tr key={`${name}-row-${rowIndex}`}>
              {model.columns.map((column, colIndex) => (
                <td key={`${column}-${colIndex}`}>
                  <input
                    className={`${styles.input} ${readOnly ? styles.readonly : ""}`}
                    data-testid={`editor-table-cell-${name}-${rowIndex}-${colIndex}`}
                    name={`${name}-${rowIndex}-${colIndex}`}
                    value={row[colIndex] ?? ""}
                    readOnly={readOnly}
                    aria-label={`${column} ${rowIndex + 1}`}
                    onChange={(e) => setCell(rowIndex, colIndex, e.target.value)}
                  />
                </td>
              ))}
              {readOnly ? null : (
                <td>
                  <button
                    type="button"
                    className={styles.button}
                    data-testid={`editor-table-remove-${name}-${rowIndex}`}
                    onClick={() => removeRow(rowIndex)}
                  >
                    {message(EDITOR_MSG.TABLE_REMOVE_ROW)}
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      {readOnly ? null : (
        <button
          type="button"
          className={styles.button}
          data-testid={`editor-table-add-${name}`}
          onClick={addRow}
        >
          {message(EDITOR_MSG.TABLE_ADD_ROW)}
        </button>
      )}
    </div>
  );
}
