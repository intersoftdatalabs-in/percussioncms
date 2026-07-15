/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.google.analytics.admin.v1alpha.AnalyticsAdminServiceClient;
import com.google.analytics.admin.v1alpha.AnalyticsAdminServiceSettings;
import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.utils.date.PSDateRange;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** @author erikserating */
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
   * Helper method to retrieve a {@link BetaAnalyticsDataClient} for the GA4 Data API.
   *
   * @param email the Service Email Account for access to the provider. Cannot be <code>null</code>
   *     or empty.
   * @param key the content of the JSON keyfile. Cannot be <code>null</code> or empty.
   * @return the GA4 Data API client, never <code>null</code>.
   * @throws PSAnalyticsProviderException if an error occurs when getting the service.
   * @throws PSValidationException if parameters are invalid.
   */
  public BetaAnalyticsDataClient getGa4DataClient(String email, String key)
      throws PSAnalyticsProviderException, PSValidationException {
    if (StringUtils.isBlank(key)) {
      PSValidationErrorsBuilder builder = validateParameters("json file");
      builder.reject("Google Auth error", "Key file content is null or empty").throwIfInvalid();
    }
    try {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(
                  new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8)))
              .createScoped("https://www.googleapis.com/auth/analytics.readonly");

      validateServiceAccountEmail(credentials, email);

      BetaAnalyticsDataSettings settings =
          BetaAnalyticsDataSettings.newBuilder()
              .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
              .build();

      return BetaAnalyticsDataClient.create(settings);
    } catch (PSValidationException ve) {
      throw ve;
    } catch (Exception e) {
      log.error("Google Auth error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e), e);
      PSValidationErrorsBuilder builder = validateParameters("json file");
      String msg =
          "Google authentication failed. Verify the service account email and JSON key file.";
      builder.reject("Google Auth error", msg).throwIfInvalid();
      return null;
    }
  }

  /**
   * Helper method to retrieve an {@link AnalyticsAdminServiceClient} for the GA4 Admin API.
   *
   * @param email the Service Email Account for access to the provider. Cannot be <code>null</code>
   *     or empty.
   * @param key the content of the JSON keyfile. Cannot be <code>null</code> or empty.
   * @return the GA4 Admin API client, never <code>null</code>.
   * @throws PSAnalyticsProviderException if an error occurs when getting the service.
   * @throws PSValidationException if parameters are invalid.
   */
  public AnalyticsAdminServiceClient getGa4AdminClient(String email, String key)
      throws PSAnalyticsProviderException, PSValidationException {
    if (StringUtils.isBlank(key)) {
      PSValidationErrorsBuilder builder = validateParameters("json file");
      builder.reject("Google Auth error", "Key file content is null or empty").throwIfInvalid();
    }
    try {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(
                  new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8)))
              .createScoped("https://www.googleapis.com/auth/analytics.readonly");

      validateServiceAccountEmail(credentials, email);

      AnalyticsAdminServiceSettings settings =
          AnalyticsAdminServiceSettings.newBuilder()
              .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
              .build();

      return AnalyticsAdminServiceClient.create(settings);
    } catch (PSValidationException ve) {
      throw ve;
    } catch (Exception e) {
      log.error("Google Auth error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e), e);
      PSValidationErrorsBuilder builder = validateParameters("json file");
      String msg =
          "Google authentication failed. Verify the service account email and JSON key file.";
      builder.reject("Google Auth error", msg).throwIfInvalid();
      return null;
    }
  }

  private void validateServiceAccountEmail(GoogleCredentials credentials, String email)
      throws PSValidationException {
    if (credentials instanceof ServiceAccountCredentials) {
      String clientEmail = ((ServiceAccountCredentials) credentials).getClientEmail();
      if (!StringUtils.equals(clientEmail, email)) {
        PSValidationErrorsBuilder builder =
            new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
        builder
            .reject(CAUSETYPE.INVALID_CREDS.toString(), "Email does not match with key file")
            .throwIfInvalid();
      }
    }
  }

  /**
   * Helper method to parse a google date string into a <code>java.util.Date</code> object.
   *
   * @param googleDate the date string (yyyyMMdd format), cannot be <code>null</code> or empty.
   * @return the date object, never <code>null</code>
   * @throws PSAnalyticsProviderException if a date parse error occurs.
   */
  public Date parseDate(String googleDate) throws PSAnalyticsProviderException {
    if (StringUtils.isBlank(googleDate))
      throw new IllegalArgumentException("googleDate cannot be null or empty.");
    try {
      Date ret = null;
      synchronized (PSGoogleAnalyticsProviderHelper.class) {
        ret = DATE_FORMAT.parse(googleDate);
      }
      return ret;
    } catch (ParseException e) {
      throw new PSAnalyticsProviderException(
          "Invalid date returned by provider.", CAUSETYPE.INVALID_DATA);
    }
  }

  /*
   * Checks if the start date of the range is not before google analytics launch date.
   * if that is the case set the start date to analytics launch date
   */
  public PSDateRange createValidPSDateRange(PSDateRange range) throws PSAnalyticsProviderException {
    FastDateFormat formatter = FastDateFormat.getInstance("MM/dd/yyyy");
    try {
      Date analyticsLaunchDate = formatter.parse(ANALYTICS_LAUNCH_DATE);
      if (analyticsLaunchDate.compareTo(range.getStart()) > 0) {
        range = new PSDateRange(analyticsLaunchDate, range.getEnd(), range.getGranularity());
      }
    } catch (ParseException e) {
      throw new PSAnalyticsProviderException(
          "Error occurred while parsing the analytics launch date.", CAUSETYPE.INVALID_DATA);
    }
    return range;
  }

  /** Date format to use to parse date from a google query. Never <code>null</code>. */
  private final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd");

  public static final String ANALYTICS_LAUNCH_DATE = "11/14/2005";

  public static final String APPLICATION_NAME = "Percussion CMS";

  public synchronized FastDateFormat getDateFormat() {
    return DATE_FORMAT;
  }

  /** Mappings of Google exceptions to our own cause enums. */
  public static final Map<String, PSAnalyticsProviderException.CAUSETYPE> CAUSE_MAPPINGS =
      new HashMap<>();

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
