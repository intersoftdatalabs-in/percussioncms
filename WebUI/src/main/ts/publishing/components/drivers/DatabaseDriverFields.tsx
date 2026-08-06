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
import { formRowStyle } from "../../publishing.styles";

export interface DatabaseDriverFieldsProps {
  driver: string;
  properties: Record<string, string>;
  onChange: (key: string, value: string) => void;
}

function Field({
  label,
  propKey,
  properties,
  onChange,
  type = "text",
  required,
}: {
  label: string;
  propKey: string;
  properties: Record<string, string>;
  onChange: (key: string, value: string) => void;
  type?: string;
  required?: boolean;
}): React.ReactElement {
  return (
    <div style={formRowStyle}>
      <label htmlFor={`db-${propKey}`}>
        {required ? "* " : ""}
        {label}
      </label>
      <input
        id={`db-${propKey}`}
        type={type}
        autoComplete="off"
        value={properties[propKey] ?? ""}
        onChange={(e) => onChange(propKey, e.target.value)}
        aria-required={required || undefined}
      />
    </div>
  );
}

/**
 * Database common + MSSQL/MySQL/Oracle fields.
 *
 * <p>Property keys match the constants on the backend
 * {@code com.percussion.services.pubserver.IPSPubServerDao} and the legacy
 * Minuet {@code percName} attributes in {@code cm/.../propEditor.jsp}. In
 * particular, MSSQL uses {@code owner} (not {@code schema}) and Oracle uses
 * {@code sid} (not {@code database}); see
 * {@code PSDatabasePubServer(String)} which reads
 * {@code PUBLISH_OWNER_PROPERTY} for MSSQL and {@code PUBLISH_SCHEMA_PROPERTY}
 * for Oracle.</p>
 */
export function DatabaseDriverFields({
  driver,
  properties,
  onChange,
}: DatabaseDriverFieldsProps): React.ReactElement {
  const d = driver.toUpperCase();
  return (
    <div data-testid={`driver-db-${d.toLowerCase()}`}>
      <Field
        label="Database server"
        propKey="server"
        properties={properties}
        onChange={onChange}
        required
      />
      <Field
        label="Port"
        propKey="port"
        properties={properties}
        onChange={onChange}
        required
      />
      {(d === "MYSQL" || d === "MSSQL") && (
        <Field
          label="Database name"
          propKey="database"
          properties={properties}
          onChange={onChange}
          required
        />
      )}
      {d === "ORACLE" && (
        <Field
          label="SID / service"
          propKey="sid"
          properties={properties}
          onChange={onChange}
          required
        />
      )}
      <Field
        label="User ID"
        propKey="userid"
        properties={properties}
        onChange={onChange}
        required
      />
      <Field
        label="Password"
        propKey="password"
        properties={properties}
        onChange={onChange}
        type="password"
        required
      />
      {d === "MSSQL" && (
        <Field
          label="Owner"
          propKey="owner"
          properties={properties}
          onChange={onChange}
          required
        />
      )}
      {d === "ORACLE" && (
        <Field
          label="Schema"
          propKey="schema"
          properties={properties}
          onChange={onChange}
          required
        />
      )}
    </div>
  );
}
