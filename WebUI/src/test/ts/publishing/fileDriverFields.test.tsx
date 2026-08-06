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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { FileDriverFields } from "@/publishing/components/drivers/FileDriverFields";

function changeById(id: string, value: string): void {
  const el = document.getElementById(id) as HTMLInputElement | null;
  if (!el) {
    throw new Error(`missing element #${id}`);
  }
  fireEvent.change(el, { target: { value } });
}

describe("FileDriverFields canonical property keys", () => {
  it("writes FTP user under the canonical 'userid' key (not 'user')", () => {
    const onChange = vi.fn();
    render(
      <FileDriverFields
        driver="FTP"
        properties={{ serverip: "ftp.example.com", port: "21", password: "p" }}
        onChange={onChange}
      />,
    );
    changeById("drv-userid", "alice");
    const keys = onChange.mock.calls.map((c) => c[0]);
    expect(keys).toContain("userid");
    expect(keys).not.toContain("user");
    const lastUseridCall = [...onChange.mock.calls]
      .reverse()
      .find((c) => c[0] === "userid");
    expect(lastUseridCall?.[1]).toBe("alice");
  });

  it("writes S3 bucket under the canonical 'bucketlocation' key (not 'bucketName')", () => {
    const onChange = vi.fn();
    render(
      <FileDriverFields
        driver="AMAZONS3"
        properties={{ accesskey: "AKIA", securitykey: "secret", region: "us-east-1" }}
        onChange={onChange}
      />,
    );
    changeById("drv-bucketlocation", "my-bucket");
    const keys = onChange.mock.calls.map((c) => c[0]);
    expect(keys).toContain("bucketlocation");
    expect(keys).not.toContain("bucketName");
  });

  it("writes SFTP user under the canonical 'userid' key", () => {
    const onChange = vi.fn();
    render(
      <FileDriverFields
        driver="SFTP"
        properties={{ serverip: "sftp.example.com", port: "22" }}
        onChange={onChange}
      />,
    );
    changeById("drv-userid", "bob");
    const keys = onChange.mock.calls.map((c) => c[0]);
    expect(keys).toContain("userid");
    expect(keys).not.toContain("user");
  });
});