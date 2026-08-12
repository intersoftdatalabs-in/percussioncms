/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  extractRestErrorMessage,
  formatApiError,
  SessionRedirectError,
  type ApiError,
} from "../../../main/ts/api/client";

describe("extractRestErrorMessage", () => {
  it("returns plain string bodies", () => {
    expect(extractRestErrorMessage("No file sent")).toBe("No file sent");
    expect(extractRestErrorMessage("  ")).toBeUndefined();
  });

  it("reads RestError message / detailMessage", () => {
    expect(
      extractRestErrorMessage({
        errorCode: 99,
        errorType: "WebApplicationException",
        message: "Control not found",
        detailMessage: "ignored when message present",
      }),
    ).toBe("Control not found");
    expect(
      extractRestErrorMessage({
        errorCode: 99,
        detailMessage: "only detail",
      }),
    ).toBe("only detail");
  });

  it("unwraps Jackson Error root", () => {
    expect(
      extractRestErrorMessage({
        Error: {
          errorCode: 99,
          message: "adaptor boom",
          detailMessage: "IllegalStateException: adaptor boom",
        },
      }),
    ).toBe("adaptor boom");
  });

  it("unwraps sitemanage PSErrors / PathItem error envelope (#3196)", () => {
    expect(
      extractRestErrorMessage({
        Errors: {
          globalError: {
            defaultMessage: "1 counts of IllegalAnnotationExceptions",
            code: "jakarta.ws.rs.InternalServerErrorException",
          },
        },
      }),
    ).toBe("1 counts of IllegalAnnotationExceptions");
    expect(
      extractRestErrorMessage({
        PathItem: [
          {
            Errors: {
              globalError: {
                cause: {
                  message: "HTTP 500 Internal Server Error",
                  errorCause: {
                    message: "1 counts of IllegalAnnotationExceptions",
                  },
                },
              },
            },
          },
        ],
      }),
    ).toBe("1 counts of IllegalAnnotationExceptions");
  });
});

describe("formatApiError RestError alignment", () => {
  it("surfaces RestError.message from ApiError body", () => {
    const err: ApiError = {
      status: 404,
      statusText: "Not Found",
      body: {
        errorCode: 99,
        errorType: "WebApplicationException",
        message: "Control not found",
        detailMessage: null,
      },
    };
    expect(formatApiError(err, "Request failed")).toBe("Control not found");
  });

  it("falls back to HTTP status chrome when body has no message", () => {
    const err: ApiError = {
      status: 503,
      statusText: "Service Unavailable",
      body: null,
    };
    expect(formatApiError(err, "Request failed")).toBe(
      "Request failed (HTTP 503 Service Unavailable)",
    );
  });

  it("keeps SessionRedirectError and plain Error paths", () => {
    expect(formatApiError(new SessionRedirectError(), "fallback")).toBe(
      "fallback",
    );
    expect(formatApiError(new Error("network down"), "fallback")).toBe(
      "network down",
    );
  });
});
