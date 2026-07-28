/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

/**
 * Provides the helper that integrates the delivery-tier forms service with Google reCAPTCHA.
 * The service stores the configured secret / URL / user agent and exposes a single
 * {@link #verify(String)} entry point used by the REST layer after each submission.
 *
 * <p>The class also enumerates the error codes returned by the reCAPTCHA verify endpoint so
 * that callers can distinguish configuration problems from genuine spam.</p>
 */
package com.percussion.delivery.forms.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Service for checking recaptcha on form.
 *
 * @author natechadwick
 */
public class PSRecaptchaService {

  private static final Logger log = LogManager.getLogger(PSRecaptchaService.class);

  /** Name of the request parameter / field that carries the reCAPTCHA token from the browser. */
  public static final String RECAPTCHA_RESPONSE = "g-recaptcha-response";
  // Possible errors
  /** Error code returned when the reCAPTCHA secret parameter is missing from the request. */
  public static final String RECAPTCHA_ERR_MISSING_SECRET =
      "missing-input-secret"; //	The secret parameter is missing.
  /** Error code returned when the reCAPTCHA secret parameter is invalid or malformed. */
  public static final String RECAPTCHA_ERR_INVALID_SECRET =
      "invalid-input-secret"; //	The secret parameter is invalid or malformed.
  /** Error code returned when the response parameter is missing from the request. */
  public static final String RECAPTCHA_ERR_MISSING_INPUT =
      "missing-input-response"; // The response parameter is missing.
  /** Error code returned when the response parameter is invalid or malformed. */
  public static final String RECAPTCHA_ERR_INVALID_INPUT =
      "invalid-input-response"; //	The response parameter is invalid or malformed.
  /** Error code returned when the reCAPTCHA request is invalid or malformed. */
  public static final String RECAPTCHA_ERR_BAD_REQUEST =
      "bad-request"; // The request is invalid or malformed.
  /** Error code returned when the response token is too old or has been used previously. */
  public static final String RECAPTCHA_ERR_TIMEOUT =
      "timeout-or-duplicate"; // The response is no longer valid: either is too old or has been used
  // previously.

  private String url = "https://www.google.com/recaptcha/api/siteverify";
  private String secret;
  private String userAgent = "Mozilla/5.0";
  private boolean captchaOn = false;

  /**
   * Constructs a reCAPTCHA service with explicit configuration. Used by callers that wire the
   * service outside the default Spring container.
   *
   * @param captchaOn whether reCAPTCHA validation is currently active.
   * @param captchaUrl the URL of the reCAPTCHA verify endpoint, never <code>null</code>.
   * @param secret the shared secret issued by Google for the configured site, may be
   *     <code>null</code> only when validation is disabled.
   * @param userAgent the HTTP User-Agent header to send to the verify endpoint, never
   *     <code>null</code>.
   */
  public PSRecaptchaService(boolean captchaOn, String captchaUrl, String secret, String userAgent) {
    this.captchaOn = captchaOn;
    this.url = captchaUrl;
    this.secret = secret;
    this.userAgent = userAgent;
  }

  /**
   * Returns the URL of the reCAPTCHA verify endpoint.
   *
   * @return the verify URL, never <code>null</code>.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL of the reCAPTCHA verify endpoint.
   *
   * @param url the verify URL, never <code>null</code>.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Returns the shared secret used when calling the reCAPTCHA verify endpoint.
   *
   * @return the shared secret, may be <code>null</code> when validation is disabled.
   */
  public String getSecret() {
    return secret;
  }

  /**
   * Sets the shared secret used when calling the reCAPTCHA verify endpoint.
   *
   * @param secret the shared secret, may be <code>null</code> when validation is disabled.
   */
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * Returns the HTTP User-Agent header sent to the reCAPTCHA verify endpoint.
   *
   * @return the User-Agent header, never <code>null</code>.
   */
  public String getUserAgent() {
    return userAgent;
  }

  /**
   * Sets the HTTP User-Agent header sent to the reCAPTCHA verify endpoint.
   *
   * @param userAgent the User-Agent header, never <code>null</code>.
   */
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  /**
   * Indicates whether reCAPTCHA validation is currently active.
   *
   * @return {@code true} when validation is enabled, {@code false} otherwise.
   */
  public boolean isCaptchaOn() {
    return captchaOn;
  }

  /**
   * Enables or disables reCAPTCHA validation for subsequent calls to
   * {@link #verify(String)}.
   *
   * @param captchaOn {@code true} to enable validation, {@code false} to bypass it.
   */
  public void setCaptchaOn(boolean captchaOn) {
    this.captchaOn = captchaOn;
  }

  /**
   * Validates the supplied reCAPTCHA response token against the configured verify endpoint.
   * A {@code null} or empty token is rejected immediately. Any I/O or parsing error causes
   * the validation to fail closed and is logged.
   *
   * @param gRecaptchaResponse the token posted by the browser, may be <code>null</code> or
   *     empty.
   * @return {@code true} if Google reports a successful validation, {@code false} otherwise.
   * @throws IOException never thrown by the current implementation, declared for backward
   *     compatibility.
   */
  public boolean verify(String gRecaptchaResponse) throws IOException {
    if (gRecaptchaResponse == null || "".equals(gRecaptchaResponse)) {
      return false;
    }

    try {
      URL obj = new URL(url);
      HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

      // add requqest header
      con.setRequestMethod("POST");
      con.setRequestProperty("User-Agent", userAgent);
      con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

      String postParams = "secret=" + secret + "&response=" + gRecaptchaResponse;

      // Send post request
      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());
      wr.writeBytes(postParams);
      wr.flush();
      wr.close();

      int responseCode = con.getResponseCode();
      log.debug("reCaptcha: \nSending 'POST' request to URL : {}", url);
      log.debug("reCaptcha: Post parameters : {}", postParams);
      log.debug("reCaptcha: Response Code : {}", responseCode);

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuilder response = new StringBuilder();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();

      // print result
      log.debug("{}", response);

      // parse JSON response and return 'success' value
      JSONObject json = new JSONObject(response.toString());

      boolean ret = json.getBoolean("success");

      if (log.isDebugEnabled()) {
        if (ret == true) {
          log.debug("reCaptcha: Successful validation.  This is not a robot! Yay humans!");
        } else {
          log.debug("reCaptcha: Validation failed.  Bad robot!");
        }
      }

      return ret;

    } catch (Exception e) {
      log.error("An error occurred validating reCaptcha.  Failing validation.");
      log.debug("reCaptcha: Validation failed with exception", e);
      return false;
    }
  }
}
