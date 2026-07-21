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
  mapPublishError,
  startPublishState,
  successPublishState,
} from "@/publishing/publishActions";

describe("publish action state machine", () => {
  it("starts and succeeds", () => {
    expect(startPublishState()).toBe("starting");
    expect(successPublishState()).toBe("success");
  });

  it("maps FORBIDDEN from status 403", () => {
    const r = mapPublishError({ status: 403, statusText: "Forbidden", body: "" });
    expect(r.state).toBe("forbidden");
    expect(r.token).toBe("FORBIDDEN");
  });

  it("maps BADCONFIG from body", () => {
    const r = mapPublishError({
      status: 400,
      statusText: "Bad",
      body: "BADCONFIG: missing host",
    });
    expect(r.state).toBe("badconfig");
    expect(r.token).toBe("BADCONFIG");
  });

  it("maps generic errors", () => {
    const r = mapPublishError({
      status: 500,
      statusText: "Server Error",
      body: { message: "boom" },
    });
    expect(r.state).toBe("error");
    expect(r.message).toContain("boom");
  });
});
