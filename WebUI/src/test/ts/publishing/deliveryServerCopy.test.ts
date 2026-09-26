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

import { describe, expect, it } from "vitest";
import { buildDeliveryServerCopy } from "@/publishing/deliveryServerCopy";
import { mapDeliveryServerCopyError } from "@/publishing/deliveryServerSaveErrors";

const source = {
  serverId: 7,
  serverName: "FTP-Prod",
  serverType: "PRODUCTION",
  type: "File",
  isDefault: true,
  properties: [
    { key: "driver", value: "FTP" },
    { key: "folder", value: "pubroot" },
    { key: "userid", value: "publish" },
    { key: "password", value: "s3cr3t" },
    { key: "securitykey", value: "sec" },
    { key: "accesskey", value: "ak" },
    { key: "secretKey", value: "sk" },
  ],
};

describe("buildDeliveryServerCopy", () => {
  it("copies the driver and non-secret settings under a new name", () => {
    const result = buildDeliveryServerCopy(source, " FTP-Copy ");
    expect(result.ok).toBe(true);
    if (!result.ok) {
      return;
    }
    const info = result.body.serverInfo;
    expect(info.serverName).toBe("FTP-Copy");
    expect(info.serverId).toBeUndefined();
    expect(info.isDefault).toBe(false);
    expect(info.serverType).toBe("PRODUCTION");
    expect(info.type).toBe("File");
    const props = info.properties as { key: string; value: string }[];
    const map = Object.fromEntries(props.map((p) => [p.key, p.value]));
    expect(map.driver).toBe("FTP");
    expect(map.folder).toBe("pubroot");
    expect(map.userid).toBe("publish");
    expect(map.password).toBeUndefined();
    expect(map.securitykey).toBeUndefined();
    expect(map.accesskey).toBeUndefined();
    expect(map.secretKey).toBeUndefined();
    expect(JSON.stringify(result.body)).not.toContain("s3cr3t");
  });

  it("rejects a blank name", () => {
    expect(buildDeliveryServerCopy(source, "   ").ok).toBe(false);
  });
});

describe("mapDeliveryServerCopyError", () => {
  it("maps HTTP 400 to a required-name message", () => {
    expect(
      mapDeliveryServerCopyError({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/required|400/i);
  });

  it("maps HTTP 409 to a name conflict", () => {
    expect(
      mapDeliveryServerCopyError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Publish server name already exists" },
      }),
    ).toMatch(/already exists/i);
  });
});
