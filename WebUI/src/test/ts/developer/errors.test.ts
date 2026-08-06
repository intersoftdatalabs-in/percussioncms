/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { beforeEach, describe, expect, it } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import { panelErrMsg } from "../../../main/ts/developer/errors";
import { DEV_MSG } from "../../../main/ts/developer/messages";

describe("panelErrMsg", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("returns session-redirect message for SessionRedirectError", () => {
    expect(panelErrMsg(new SessionRedirectError(), DEV_MSG.CT_ERROR)).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
  });

  it("formats ApiError status without statusText", () => {
    const api = { status: 503, statusText: "Service Unavailable", body: null };
    expect(panelErrMsg(api, DEV_MSG.CT_ERROR)).toBe(`${DEV_MSG.CT_ERROR} (503)`);
  });

  it("surfaces RestError.message from ApiError body", () => {
    const api = {
      status: 500,
      statusText: "Internal Server Error",
      body: {
        errorCode: 99,
        errorType: "WebApplicationException",
        message: "adaptor boom",
        detailMessage: "IllegalStateException: adaptor boom",
      },
    };
    expect(panelErrMsg(api, DEV_MSG.CT_ERROR)).toBe(
      `${DEV_MSG.CT_ERROR} adaptor boom`,
    );
  });

  it("surfaces wrapped RestError root message", () => {
    const api = {
      status: 404,
      statusText: "Not Found",
      body: {
        Error: {
          message: "Control not found",
          detailMessage: null,
        },
      },
    };
    expect(panelErrMsg(api, DEV_MSG.CT_ERROR)).toBe(
      `${DEV_MSG.CT_ERROR} Control not found`,
    );
  });

  it("appends Error.message to the fallback", () => {
    expect(panelErrMsg(new Error("network down"), DEV_MSG.CT_ERROR)).toBe(
      `${DEV_MSG.CT_ERROR} network down`,
    );
  });

  it("returns fallback for unknown / empty values", () => {
    expect(panelErrMsg(null, DEV_MSG.CT_ERROR)).toBe(DEV_MSG.CT_ERROR);
    expect(panelErrMsg(undefined, DEV_MSG.CT_ERROR)).toBe(DEV_MSG.CT_ERROR);
    expect(panelErrMsg({}, DEV_MSG.CT_ERROR)).toBe(DEV_MSG.CT_ERROR);
    expect(panelErrMsg(new Error(""), DEV_MSG.CT_ERROR)).toBe(DEV_MSG.CT_ERROR);
  });
});
