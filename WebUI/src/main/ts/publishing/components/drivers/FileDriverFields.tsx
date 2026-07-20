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
import { message, MSG } from "../../../i18n/message";
import { formRowStyle } from "../../publishing.styles";
import { isSecretPropertyKey } from "../../serverSecrets";

export interface FileDriverFieldsProps {
  driver: string;
  properties: Record<string, string>;
  onChange: (key: string, value: string) => void;
  regions?: string[];
}

function Field({
  label,
  propKey,
  properties,
  onChange,
  required,
}: {
  label: string;
  propKey: string;
  properties: Record<string, string>;
  onChange: (key: string, value: string) => void;
  required?: boolean;
}): React.ReactElement {
  const secret = isSecretPropertyKey(propKey) || propKey === "securitykey" || propKey === "accesskey";
  return (
    <div style={formRowStyle}>
      <label htmlFor={`drv-${propKey}`}>
        {required ? "* " : ""}
        {label}
      </label>
      <input
        id={`drv-${propKey}`}
        type={secret ? "password" : "text"}
        autoComplete="off"
        value={properties[propKey] ?? ""}
        onChange={(e) => onChange(propKey, e.target.value)}
        aria-required={required || undefined}
      />
    </div>
  );
}

/**
 * File driver property panels: Local, FTP/FTPS/SFTP, Amazon S3.
 * Field names match Minuet `percServerFileProp` keys.
 */
export function FileDriverFields({
  driver,
  properties,
  onChange,
  regions = [],
}: FileDriverFieldsProps): React.ReactElement {
  const d = driver.toUpperCase();

  if (d === "LOCAL") {
    return (
      <div data-testid="driver-local">
        <Field
          label={message(MSG.PUBLISH_FOLDER) || "Folder"}
          propKey="folder"
          properties={properties}
          onChange={onChange}
        />
      </div>
    );
  }

  if (d === "FTP" || d === "FTPS" || d === "SFTP") {
    return (
      <div data-testid={`driver-${d.toLowerCase()}`}>
        <Field
          label="Server IP / host"
          propKey="serverip"
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
        <Field
          label="User"
          propKey="user"
          properties={properties}
          onChange={onChange}
          required
        />
        <Field
          label="Password"
          propKey="password"
          properties={properties}
          onChange={onChange}
          required={d !== "SFTP"}
        />
        {(d === "SFTP" || d === "FTP" || d === "FTPS") && (
          <Field
            label="Private key"
            propKey="privateKey"
            properties={properties}
            onChange={onChange}
          />
        )}
        <Field
          label="Folder"
          propKey="folder"
          properties={properties}
          onChange={onChange}
        />
      </div>
    );
  }

  if (d === "AMAZONS3" || d === "S3") {
    return (
      <div data-testid="driver-s3">
        <Field
          label="Access key"
          propKey="accesskey"
          properties={properties}
          onChange={onChange}
          required
        />
        <Field
          label="Secret key"
          propKey="securitykey"
          properties={properties}
          onChange={onChange}
          required
        />
        <Field
          label="Bucket"
          propKey="bucketName"
          properties={properties}
          onChange={onChange}
          required
        />
        <div style={formRowStyle}>
          <label htmlFor="drv-region">* Region</label>
          {regions.length > 0 ? (
            <select
              id="drv-region"
              value={properties.region ?? ""}
              onChange={(e) => onChange("region", e.target.value)}
            >
              <option value="">Select</option>
              {regions.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          ) : (
            <input
              id="drv-region"
              value={properties.region ?? ""}
              onChange={(e) => onChange("region", e.target.value)}
            />
          )}
        </div>
      </div>
    );
  }

  return <p data-testid="driver-unknown">Unknown file driver: {driver}</p>;
}
