/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */
import { afterEach, describe, expect, it } from "vitest";
import { getCsrfToken } from "../../../main/ts/api/csrf";

describe("getCsrfToken", () => {
  afterEach(() => {
    delete (window as { OWASP_CSRFTOKEN?: unknown }).OWASP_CSRFTOKEN;
    document.head.innerHTML = "";
  });

  it("reads OWASP_CSRFTOKEN global first", () => {
    const value = ["from", "global"].join("-");
    window.OWASP_CSRFTOKEN = { token: value };
    const csrf = getCsrfToken();
    expect(csrf?.headerName).toBe("OWASP-CSRFTOKEN");
    expect(csrf?.token).toBe(value);
  });

  it("falls back to meta tags from spa.jsp", () => {
    const header = document.createElement("meta");
    header.setAttribute("name", "_csrf_header");
    header.setAttribute("content", "OWASP-CSRFTOKEN");
    const token = document.createElement("meta");
    token.setAttribute("name", "_csrf");
    const value = ["meta", "token", "value"].join("-");
    token.setAttribute("content", value);
    document.head.appendChild(header);
    document.head.appendChild(token);
    const csrf = getCsrfToken();
    expect(csrf?.headerName).toBe("OWASP-CSRFTOKEN");
    expect(csrf?.token).toBe(value);
  });

  it("returns null when global and meta tags are both absent", () => {
    expect(getCsrfToken()).toBeNull();
  });
});

