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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DatabaseDriverFields } from "@/publishing/components/drivers/DatabaseDriverFields";

function keys(onChange: ReturnType<typeof vi.fn>): string[] {
  return onChange.mock.calls.map((c) => c[0]);
}

describe("DatabaseDriverFields canonical property keys", () => {
  it("MSSQL writes 'owner' (not 'schema')", () => {
    const onChange = vi.fn();
    render(
      <DatabaseDriverFields
        driver="MSSQL"
        properties={{}}
        onChange={onChange}
      />,
    );
    const ownerInput = document.getElementById("db-owner") as HTMLInputElement;
    fireEvent.change(ownerInput, { target: { value: "dbo" } });
    expect(keys(onChange)).toContain("owner");
    expect(keys(onChange)).not.toContain("schema");
    expect(screen.queryByLabelText(/Schema/i)).toBeNull();
  });

  it("Oracle writes 'sid' and 'schema' and NOT 'database'", () => {
    const onChange = vi.fn();
    render(
      <DatabaseDriverFields
        driver="ORACLE"
        properties={{}}
        onChange={onChange}
      />,
    );
    const sidInput = document.getElementById("db-sid") as HTMLInputElement;
    const schemaInput = document.getElementById("db-schema") as HTMLInputElement;
    fireEvent.change(sidInput, { target: { value: "orcl" } });
    fireEvent.change(schemaInput, { target: { value: "APP" } });
    expect(keys(onChange)).toContain("sid");
    expect(keys(onChange)).toContain("schema");
    expect(keys(onChange)).not.toContain("database");
    expect(document.getElementById("db-database")).toBeNull();
  });

  it("MySQL writes 'database' and not 'owner' / 'sid' / 'schema'", () => {
    const onChange = vi.fn();
    render(
      <DatabaseDriverFields
        driver="MYSQL"
        properties={{}}
        onChange={onChange}
      />,
    );
    const dbInput = document.getElementById("db-database") as HTMLInputElement;
    fireEvent.change(dbInput, { target: { value: "appdb" } });
    expect(keys(onChange)).toContain("database");
    expect(keys(onChange)).not.toContain("owner");
    expect(keys(onChange)).not.toContain("sid");
    expect(keys(onChange)).not.toContain("schema");
    expect(document.getElementById("db-owner")).toBeNull();
    expect(document.getElementById("db-sid")).toBeNull();
    expect(document.getElementById("db-schema")).toBeNull();
  });

  it("always renders common fields under canonical keys", () => {
    const onChange = vi.fn();
    render(
      <DatabaseDriverFields
        driver="MYSQL"
        properties={{}}
        onChange={onChange}
      />,
    );
    const serverInput = document.getElementById("db-server") as HTMLInputElement;
    const portInput = document.getElementById("db-port") as HTMLInputElement;
    const useridInput = document.getElementById("db-userid") as HTMLInputElement;
    const passwordInput = document.getElementById("db-password") as HTMLInputElement;
    fireEvent.change(serverInput, { target: { value: "db.local" } });
    fireEvent.change(portInput, { target: { value: "3306" } });
    fireEvent.change(useridInput, { target: { value: "alice" } });
    fireEvent.change(passwordInput, { target: { value: "secret" } });
    expect(keys(onChange)).toEqual(
      expect.arrayContaining(["server", "port", "userid", "password"]),
    );
  });
});