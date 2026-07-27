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

import { describe, expect, it } from "vitest";
import {
  emptyServerModel,
  modelToSaveBody,
  propsToMap,
  serverToModel,
} from "@/publishing/serverFormModel";

describe("serverFormModel", () => {
  it("maps property list to record", () => {
    expect(
      propsToMap([
        { key: "driver", value: "FTP" },
        { key: "port", value: "21" },
      ]),
    ).toEqual({ driver: "FTP", port: "21" });
  });

  it("builds save body with serverInfo", () => {
    const model = emptyServerModel();
    model.serverName = "LocalProd";
    model.isDefault = true;
    const body = modelToSaveBody(model);
    expect(body.serverInfo.serverName).toBe("LocalProd");
    expect(body.serverInfo.isDefault).toBe(true);
    expect(body.serverInfo.type).toBe("File");
    const props = body.serverInfo.properties as Array<{ key: string; value: string }>;
    expect(props.some((p) => p.key === "driver" && p.value === "Local")).toBe(
      true,
    );
  });

  it("reads nested serverInfo from API", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "9",
        serverName: "S3",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [{ key: "driver", value: "AMAZONS3" }],
      },
    });
    expect(m.serverId).toBe("9");
    expect(m.driver).toBe("AMAZONS3");
  });

  it("self-heals legacy FTP 'user' alias into canonical 'userid'", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "1",
        serverName: "FTP-Prod",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "FTP" },
          { key: "serverip", value: "ftp.example.com" },
          { key: "port", value: "21" },
          { key: "user", value: "alice" },
          { key: "password", value: "secret" },
        ],
      },
    });
    expect(m.properties.userid).toBe("alice");
    expect(m.properties.user).toBeUndefined();
  });

  it("self-heals legacy S3 'bucketName' alias into canonical 'bucketlocation'", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "2",
        serverName: "S3",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "AMAZONS3" },
          { key: "accesskey", value: "AKIA" },
          { key: "securitykey", value: "secret" },
          { key: "bucketName", value: "my-bucket" },
          { key: "region", value: "us-east-1" },
        ],
      },
    });
    expect(m.properties.bucketlocation).toBe("my-bucket");
    expect(m.properties.bucketName).toBeUndefined();
  });

  it("prefers canonical keys over the legacy alias when both are present", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "3",
        serverName: "FTP-Prod",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "FTP" },
          { key: "userid", value: "alice" },
          { key: "user", value: "stale-alice" },
        ],
      },
    });
    expect(m.properties.userid).toBe("alice");
  });

  it("writes only canonical keys on save (no legacy alias leak)", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "4",
        serverName: "FTP-Prod",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "FTP" },
          { key: "user", value: "alice" },
        ],
      },
    });
    m.serverId = "4";
    m.serverName = "FTP-Prod";
    m.driver = "FTP";
    const body = modelToSaveBody(m);
    const props = body.serverInfo.properties as Array<{
      key: string;
      value: string;
    }>;
    const keys = props.map((p) => p.key);
    expect(keys).toContain("userid");
    expect(keys).not.toContain("user");
    const useridProp = props.find((p) => p.key === "userid");
    expect(useridProp?.value).toBe("alice");
  });

  it("does NOT apply the user alias to Local servers (preserves unrelated 'user' property)", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "5",
        serverName: "LocalProd",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "Local" },
          { key: "folder", value: "/var/www" },
          { key: "user", value: "tracker-note" },
        ],
      },
    });
    expect(m.properties.userid).toBeUndefined();
    expect(m.properties.user).toBe("tracker-note");
  });

  it("does NOT apply the bucketName alias to Local servers", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "6",
        serverName: "LocalCustom",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "Local" },
          { key: "bucketName", value: "unrelated-bucket" },
        ],
      },
    });
    expect(m.properties.bucketlocation).toBeUndefined();
    expect(m.properties.bucketName).toBe("unrelated-bucket");
  });

  it("does NOT apply the user alias to Database servers", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "7",
        serverName: "MySqlDb",
        type: "Database",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "MYSQL" },
          { key: "user", value: "should-be-untouched" },
        ],
      },
    });
    expect(m.properties.userid).toBeUndefined();
    expect(m.properties.user).toBe("should-be-untouched");
  });

  it("applies the user alias to SFTP driver", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "8",
        serverName: "SftpProd",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "SFTP" },
          { key: "serverip", value: "sftp.example.com" },
          { key: "port", value: "22" },
          { key: "user", value: "alice" },
        ],
      },
    });
    expect(m.properties.userid).toBe("alice");
    expect(m.properties.user).toBeUndefined();
  });

  it("treats whitespace-only canonical value as missing and falls back to legacy", () => {
    const m = serverToModel({
      serverInfo: {
        serverId: "9",
        serverName: "FTP-Prod",
        type: "File",
        serverType: "PRODUCTION",
        isDefault: false,
        properties: [
          { key: "driver", value: "FTP" },
          { key: "userid", value: "   " },
          { key: "user", value: "alice" },
        ],
      },
    });
    expect(m.properties.userid).toBe("alice");
  });
});
