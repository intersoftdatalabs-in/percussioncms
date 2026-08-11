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
package com.intsof.percussioncms.auditlog.codes;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;

/**
 * HTTP status catalog bridging legacy {@code com.percussion.server.IPSHttpErrors} ints (RFC HTTP
 * status codes used by CMS request handling).
 *
 * <p>These are <strong>protocol status codes</strong>, not security audit events. All constants set
 * {@link #isAuditable()} to {@code false}. Authentication / authorization dual-write remains on
 * {@link SecurityErrorCodes} and server auth codes in {@link ServerErrorCodes}.
 *
 * <p>Registered in {@link LegacyErrorCodeRegistry} so bare status ints resolve as non-auditable
 * (safe skip) when a handler passes them to {@code logIfAuditable}. Module code is {@link
 * AuditModule#SYS}.
 */
public enum HttpErrorCodes implements SystemErrorCode {
  HTTP_CONTINUE(
      100,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 100: Http continue",
      "HTTP status 100 HTTP_CONTINUE"),
  HTTP_SWITCHING_PROTOCOLS(
      101,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 101: Http switching protocols",
      "HTTP status 101 HTTP_SWITCHING_PROTOCOLS"),
  HTTP_OK(
      200,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 200: Http ok",
      "HTTP status 200 HTTP_OK"),
  HTTP_CREATED(
      201,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 201: Http created",
      "HTTP status 201 HTTP_CREATED"),
  HTTP_ACCEPTED(
      202,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 202: Http accepted",
      "HTTP status 202 HTTP_ACCEPTED"),
  HTTP_NON_AUTHORITATIVE_INFO(
      203,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 203: Http non authoritative info",
      "HTTP status 203 HTTP_NON_AUTHORITATIVE_INFO"),
  HTTP_NO_CONTENT(
      204,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 204: Http no content",
      "HTTP status 204 HTTP_NO_CONTENT"),
  HTTP_RESET_CONTENT(
      205,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 205: Http reset content",
      "HTTP status 205 HTTP_RESET_CONTENT"),
  HTTP_PARTIAL_CONTENT(
      207,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 207: Http partial content",
      "HTTP status 207 HTTP_PARTIAL_CONTENT"),
  HTTP_MULTIPLE_CHOICES(
      300,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 300: Http multiple choices",
      "HTTP status 300 HTTP_MULTIPLE_CHOICES"),
  HTTP_MOVED_PERMANENTLY(
      301,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 301: Http moved permanently",
      "HTTP status 301 HTTP_MOVED_PERMANENTLY"),
  HTTP_MOVED_TEMPORARILY(
      302,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 302: Http moved temporarily",
      "HTTP status 302 HTTP_MOVED_TEMPORARILY"),
  HTTP_SEE_OTHER(
      303,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 303: Http see other",
      "HTTP status 303 HTTP_SEE_OTHER"),
  HTTP_NOT_MODIFIED(
      304,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 304: Http not modified",
      "HTTP status 304 HTTP_NOT_MODIFIED"),
  HTTP_USE_PROXY(
      305,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 305: Http use proxy",
      "HTTP status 305 HTTP_USE_PROXY"),
  HTTP_BAD_REQUEST(
      400,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 400: Http bad request",
      "HTTP status 400 HTTP_BAD_REQUEST"),
  HTTP_UNAUTHORIZED(
      401,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 401: Http unauthorized",
      "HTTP status 401 HTTP_UNAUTHORIZED"),
  HTTP_PAYMENT_REQUIRED(
      402,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 402: Http payment required",
      "HTTP status 402 HTTP_PAYMENT_REQUIRED"),
  HTTP_FORBIDDEN(
      403,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 403: Http forbidden",
      "HTTP status 403 HTTP_FORBIDDEN"),
  HTTP_NOT_FOUND(
      404,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 404: Http not found",
      "HTTP status 404 HTTP_NOT_FOUND"),
  HTTP_METHOD_NOT_ALLOWED(
      405,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 405: Http method not allowed",
      "HTTP status 405 HTTP_METHOD_NOT_ALLOWED"),
  HTTP_NOT_ACCEPTABLE(
      406,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 406: Http not acceptable",
      "HTTP status 406 HTTP_NOT_ACCEPTABLE"),
  HTTP_PROXY_AUTHENT_REQD(
      407,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 407: Http proxy authent reqd",
      "HTTP status 407 HTTP_PROXY_AUTHENT_REQD"),
  HTTP_REQUEST_TIMEOUT(
      408,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 408: Http request timeout",
      "HTTP status 408 HTTP_REQUEST_TIMEOUT"),
  HTTP_CONFLICT(
      409,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 409: Http conflict",
      "HTTP status 409 HTTP_CONFLICT"),
  HTTP_GONE(
      410,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 410: Http gone",
      "HTTP status 410 HTTP_GONE"),
  HTTP_LENGTH_REQUIRED(
      411,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 411: Http length required",
      "HTTP status 411 HTTP_LENGTH_REQUIRED"),
  HTTP_PRECONDITION_FAILED(
      412,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 412: Http precondition failed",
      "HTTP status 412 HTTP_PRECONDITION_FAILED"),
  HTTP_REQUEST_ENTITY_TOO_LARGE(
      413,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 413: Http request entity too large",
      "HTTP status 413 HTTP_REQUEST_ENTITY_TOO_LARGE"),
  HTTP_REQUEST_URI_TOO_LARGE(
      414,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 414: Http request uri too large",
      "HTTP status 414 HTTP_REQUEST_URI_TOO_LARGE"),
  HTTP_UNSUPPORTED_MEDIA_TYPE(
      415,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 415: Http unsupported media type",
      "HTTP status 415 HTTP_UNSUPPORTED_MEDIA_TYPE"),
  HTTP_INTERNAL_SERVER_ERROR(
      500,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 500: Http internal server error",
      "HTTP status 500 HTTP_INTERNAL_SERVER_ERROR"),
  HTTP_NOT_IMPLEMENTED(
      501,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 501: Http not implemented",
      "HTTP status 501 HTTP_NOT_IMPLEMENTED"),
  HTTP_BAD_GATEWAY(
      502,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 502: Http bad gateway",
      "HTTP status 502 HTTP_BAD_GATEWAY"),
  HTTP_SERVICE_UNAVAILABLE(
      503,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 503: Http service unavailable",
      "HTTP status 503 HTTP_SERVICE_UNAVAILABLE"),
  HTTP_GATEWAY_TIMEOUT(
      504,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 504: Http gateway timeout",
      "HTTP status 504 HTTP_GATEWAY_TIMEOUT"),
  HTTP_VERSION_NOT_SUPPORTED(
      505,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "HTTP 505: Http version not supported",
      "HTTP status 505 HTTP_VERSION_NOT_SUPPORTED");

  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  HttpErrorCodes(
      int numericCode,
      boolean auditable,
      AuditEventType eventType,
      AuditOutcome defaultOutcome,
      String userMessageTemplate,
      String logMessageTemplate) {
    this.numericCode = numericCode;
    this.auditable = auditable;
    this.eventType = eventType;
    this.defaultOutcome = defaultOutcome;
    this.userMessageTemplate = userMessageTemplate;
    this.logMessageTemplate = logMessageTemplate;
  }

  static {
    ensureRegistered();
  }

  /**
   * Register (or re-register) all constants in {@link LegacyErrorCodeRegistry}. Safe to call
   * repeatedly — used by registry bootstrap and tests after {@code clearForTests}.
   */
  public static void ensureRegistered() {
    for (HttpErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }


  @Override
  public AuditModule module() {
    return AuditModule.SYS;
  }

  @Override
  public int numericCode() {
    return numericCode;
  }

  @Override
  public String userMessageTemplate() {
    return userMessageTemplate;
  }

  @Override
  public String logMessageTemplate() {
    return logMessageTemplate;
  }

  @Override
  public boolean isAuditable() {
    return auditable;
  }

  @Override
  public AuditEventType eventType() {
    return eventType;
  }

  @Override
  public AuditOutcome defaultOutcome() {
    return defaultOutcome;
  }
}
