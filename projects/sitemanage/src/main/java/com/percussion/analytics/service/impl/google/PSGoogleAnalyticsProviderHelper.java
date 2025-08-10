// REFACTORED: CP-JAVA11
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
package com.percussion.analytics.service.impl.google;

import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.PemReader;
import com.google.api.client.util.PemReader.Section;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.model.DateRange;
import com.google.api.services.analyticsreporting.v4.model.ReportRequest;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.utils.date.PSDateRange;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper for Google Analytics OAuth2 and API access.
 * Sunny Sal: "OAuth2 is like a Bollywood dance—complex, but worth it!"
 */
public class PSGoogleAnalyticsProviderHelper {

  private static PSGoogleAnalyticsProviderHelper INSTANCE;

  public static PSGoogleAnalyticsProviderHelper getInstance() {
    synchronized (PSGoogleAnalyticsProviderHelper.class) {
      if (INSTANCE == null) {
        INSTANCE = new PSGoogleAnalyticsProviderHelper();
      }
    }
    return INSTANCE;
  }

  private PSGoogleAnalyticsProviderHelper() {}

  /**
   * Helper method to retrieve an Analytics service object for Google Analytics API.
   * Throws if credentials are invalid.
   */
  public Analytics getAnalyticsService(String email, String key)
      throws PSAnalyticsProviderException, PSValidationException {
    var JSON_FACTORY = JacksonFactory.getDefaultInstance();
    Analytics service = null;
    try {
      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      var mapper = new ObjectMapper();
      var creds = mapper.readValue(key, GoogleCreds.class);
      if (!StringUtils.equals(creds.getClientEmail(), email)) {
        var builder = new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
        builder
            .reject(CAUSETYPE.INVALID_CREDS.toString(), "Email does not match with key file")
            .throwIfInvalid();
      }
      PrivateKey serviceAccountPrivateKey = privateKeyFromPkcs8(creds.getPrivateKey());
      var credential =
          new GoogleCredential.Builder()
              .setTransport(httpTransport)
              .setJsonFactory(JSON_FACTORY)
              .setServiceAccountId(email)
              .setServiceAccountPrivateKeyId(creds.getPrivateKeyId())
              .setServiceAccountPrivateKey(serviceAccountPrivateKey)
              .setServiceAccountScopes(AnalyticsScopes.all())
              .build();
      service =
          new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
              .setApplicationName(APPLICATION_NAME)
              .setHttpRequestInitializer(credential)
              .build();
    } catch (PSValidationException ve) {
      throw ve;
    } catch (Exception e) {
      log.error("Google Auth error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e), e);
      var builder = validateParameters("json file");
      var msg = "Google Auth error: " + PSExceptionUtils.getMessageForLog(e);
      builder.reject("Google Auth error", msg).throwIfInvalid();
    }
    return service;
  }

  public AnalyticsReporting initializeAnalyticsReporting(String email, String key)
      throws PSAnalyticsProviderException, PSValidationException {
    var JSON_FACTORY = JacksonFactory.getDefaultInstance();
    AnalyticsReporting analyticsReporting = null;
    try {
      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      var mapper = new ObjectMapper();
      var creds = mapper.readValue(key, GoogleCreds.class);
      if (!StringUtils.equals(creds.getClientEmail(), email)) {
        var builder = validateParameters("Email");
        builder.reject("Google Auth error", "Email does not match key file").throwIfInvalid();
      }
      PrivateKey serviceAccountPrivateKey = privateKeyFromPkcs8(creds.getPrivateKey());
      var credential =
          new GoogleCredential.Builder()
              .setTransport(httpTransport)
              .setJsonFactory(JSON_FACTORY)
              .setServiceAccountId(email)
              .setServiceAccountPrivateKeyId(creds.getPrivateKeyId())
              .setServiceAccountPrivateKey(serviceAccountPrivateKey)
              .setServiceAccountScopes(AnalyticsScopes.all())
              .build();
      analyticsReporting =
          new AnalyticsReporting.Builder(httpTransport, JSON_FACTORY, credential)
              .setApplicationName(APPLICATION_NAME)
              .build();
    } catch (PSValidationException ve) {
      throw ve;
    } catch (Exception e) {
      log.error("Google Auth error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e), e);
      var builder = validateParameters("json file");
      var msg = "Google Auth error: " + PSExceptionUtils.getMessageForLog(e);
      builder.reject("Google Auth error", msg).throwIfInvalid();
    }
    return analyticsReporting;
  }

  /**
   * Helper method to create a new Google analytics ReportRequest object.
   */
  public ReportRequest createNewDataQuery(PSDateRange range) {
    var formatter = FastDateFormat.getInstance("yyyy-MM-dd");
    var dateRange = new DateRange();
    dateRange.setStartDate(formatter.format(range.getStart()));
    dateRange.setEndDate(formatter.format(range.getEnd()));
    return new ReportRequest().setDateRanges(Arrays.asList(dateRange));
  }

  /**
   * Helper method to parse a google date string into a java.util.Date object.
   */
  public Date parseDate(String googleDate) throws PSAnalyticsProviderException {
    if (StringUtils.isBlank(googleDate))
      throw new IllegalArgumentException("googleDate cannot be null or empty.");
    try {
      Date ret;
      synchronized (PSGoogleAnalyticsProviderHelper.class) {
        ret = DATE_FORMAT.parse(googleDate);
      }
      return ret;
    } catch (ParseException e) {
      throw new PSAnalyticsProviderException(
          "Invalid date returned by provider.", CAUSETYPE.INVALID_DATA);
    }
  }

  /**
   * Checks if the start date of the range is not before Google Analytics launch date.
   * If that is the case, set the start date to analytics launch date.
   */
  public PSDateRange createValidPSDateRange(PSDateRange range) throws PSAnalyticsProviderException {
    var formatter = FastDateFormat.getInstance("MM/dd/yyyy");
    try {
      var analyticsLaunchDate = formatter.parse(ANALYTICS_LAUNCH_DATE);
      if (analyticsLaunchDate.compareTo(range.getStart()) > 0) {
        range = new PSDateRange(analyticsLaunchDate, range.getEnd(), range.getGranularity());
      }
    } catch (ParseException e) {
      throw new PSAnalyticsProviderException(
          "Error occurred while parsing the analytics launch date.", CAUSETYPE.INVALID_DATA);
    }
    return range;
  }

  /**
   * Date format to use to parse date from a Google query. Never null.
   */
  private final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd");

  public static final String ANALYTICS_LAUNCH_DATE = "11/14/2005";
  public static final String APPLICATION_NAME = "Percussion CMS";

  public synchronized FastDateFormat getDateFormat() {
    return DATE_FORMAT;
  }

  /**
   * Mappings of Google exceptions to our own cause enums.
   */
  public static final Map<String, PSAnalyticsProviderException.CAUSETYPE> CAUSE_MAPPINGS =
      new HashMap<>();

  /**
   * Helper to convert from a PKCS#8 String to an RSA private key.
   */
  static PrivateKey privateKeyFromPkcs8(String privateKeyPkcs8)
      throws IOException, PSAnalyticsProviderException {
    try (Reader reader = new StringReader(privateKeyPkcs8)) {
      Section section = PemReader.readFirstSectionAndClose(reader, "PRIVATE KEY");
      if (section == null) {
        throw new IOException("Invalid PKCS#8 data.");
      }
      var bytes = section.getBase64DecodedBytes();
      var keySpec = new PKCS8EncodedKeySpec(bytes);
      try {
        KeyFactory keyFactory = SecurityUtils.getRsaKeyFactory();
        return keyFactory.generatePrivate(keySpec);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
        throw new PSAnalyticsProviderException(
            exception.getMessage(), CAUSETYPE.AUTHENTICATION_ERROR);
      }
    }
  }

  static {
    CAUSE_MAPPINGS.put("AccountDeletedException", CAUSETYPE.ACCOUNT_DELETED);
    CAUSE_MAPPINGS.put("AccountDisabledException", CAUSETYPE.ACCOUNT_DISABLED);
    CAUSE_MAPPINGS.put("CaptchaRequiredException", CAUSETYPE.INVALID_CREDS);
    CAUSE_MAPPINGS.put("InvalidCredentialsException", CAUSETYPE.INVALID_CREDS);
    CAUSE_MAPPINGS.put("NotVerifiedException", CAUSETYPE.NOT_VERIFIED);
    CAUSE_MAPPINGS.put("ServiceUnavailableException", CAUSETYPE.SERVICE_UNAVAILABLE);
    CAUSE_MAPPINGS.put("SessionExpiredException", CAUSETYPE.SESSION_EXPIRED);
    CAUSE_MAPPINGS.put("TermsNotAgreedException", CAUSETYPE.TERMS_NOT_AGREED);
  }

  private static final Logger log = LogManager.getLogger(PSGoogleAnalyticsProviderHelper.class);
}
