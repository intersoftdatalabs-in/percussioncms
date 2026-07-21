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
  mapPublishResponse,
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

describe("mapPublishResponse (HTTP 200 application-level errors)", () => {
  it("returns null for an in-flight QUEUEING response", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "Queuing content",
        delivered: "0",
        failures: "0",
        jobid: 123,
      },
    });
    expect(r).toBeNull();
  });

  it("returns null for a COMPLETED response", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "Edition completed",
        delivered: "10",
        failures: "0",
        jobid: 456,
      },
    });
    expect(r).toBeNull();
  });

  it("flags BADCONFIG with the server warningMessage (issue #936)", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "BADCONFIG",
        delivered: "0",
        failures: "0",
        jobid: 0,
        warningMessage:
          "Could not connect to publishing server, please check publishing server configuration.",
      },
    });
    expect(r).not.toBeNull();
    expect(r?.state).toBe("badconfig");
    expect(r?.token).toBe("BADCONFIG");
    expect(r?.message).toBe(
      "Could not connect to publishing server, please check publishing server configuration.",
    );
  });

  it("flags BADCONFIGMULTIPLESITES as badconfig", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "BADCONFIGMULTIPLESITES",
        delivered: "0",
        failures: "0",
        jobid: 0,
        warningMessage: "Could not connect to publishing server for sites: foo",
      },
    });
    expect(r?.state).toBe("badconfig");
    expect(r?.token).toBe("BADCONFIGMULTIPLESITES");
  });

  it("flags FORBIDDEN as forbidden", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "FORBIDDEN",
        delivered: "0",
        failures: "0",
        jobid: 0,
        warningMessage: "Publication stopped because of licensing issues",
      },
    });
    expect(r?.state).toBe("forbidden");
    expect(r?.token).toBe("FORBIDDEN");
  });

  it("flags NOSTAGING_SERVERS as error with the server warning", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "NOSTAGING_SERVERS",
        delivered: "0",
        failures: "0",
        jobid: 0,
        warningMessage: "No staging servers available for the item to publish/unpublish.",
      },
    });
    expect(r?.state).toBe("error");
    expect(r?.token).toBe("NOSTAGING_SERVERS");
    expect(r?.message).toContain("No staging servers");
  });

  it("falls back to the status token when warningMessage is empty", () => {
    const r = mapPublishResponse({
      SitePublishResponse: {
        status: "BADCONFIG",
        delivered: "0",
        failures: "0",
        jobid: 0,
      },
    });
    expect(r?.state).toBe("badconfig");
    expect(r?.message).toBe("BADCONFIG");
  });

  it("accepts an unwrapped response object", () => {
    const r = mapPublishResponse({
      status: "BADCONFIG",
      delivered: "0",
      failures: "0",
      jobid: 0,
      warningMessage: "FTP credentials rejected",
    });
    expect(r?.state).toBe("badconfig");
    expect(r?.message).toBe("FTP credentials rejected");
  });

  it("returns null for null/undefined input", () => {
    expect(mapPublishResponse(null)).toBeNull();
    expect(mapPublishResponse(undefined)).toBeNull();
    expect(mapPublishResponse("nope")).toBeNull();
  });

  it("returns null for an empty status", () => {
    const r = mapPublishResponse({ SitePublishResponse: { status: "" } });
    expect(r).toBeNull();
  });
});
