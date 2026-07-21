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
});
