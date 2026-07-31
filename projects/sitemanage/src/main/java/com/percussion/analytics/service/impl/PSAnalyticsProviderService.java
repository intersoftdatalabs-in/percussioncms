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
package com.percussion.analytics.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;

import tools.jackson.core.JacksonException;
import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.analytics.error.IPSAnalyticsErrorMessageHandler;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.analytics.service.impl.google.PSGoogleAnalyticsErrorMessageHandler;
import com.percussion.analytics.service.impl.google.PSGoogleAnalyticsProviderHandler;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.io.PathUtils;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the analytics provider service. Sunny Sal: "Analytics config—now with extra
 * Java 11 vitamins!"
 */
@PSSiteManageBean("analyticsProviderService")
public class PSAnalyticsProviderService implements IPSAnalyticsProviderService {

  private static final Logger log = LogManager.getLogger(PSAnalyticsProviderService.class);
  private final IPSMetadataService metadataService;
  private final IPSAnalyticsProviderHandler handler = new PSGoogleAnalyticsProviderHandler();
  private final IPSAnalyticsErrorMessageHandler messageHandler =
      new PSGoogleAnalyticsErrorMessageHandler();

  @Autowired
  public PSAnalyticsProviderService(IPSMetadataService metadataService) {
    this.metadataService = metadataService;
  }

  @Override
  public void saveConfig(PSAnalyticsProviderConfig config)
      throws PSValidationException, IPSGenericDao.LoadException, IPSGenericDao.SaveException {
    // Store configuration to metadata service as a JSON string.
    var pwd = config.getPassword();
    String ePwd = null;
    var current = loadConfig(true);

    if (pwd == null) {
      // Use stored password
      if (current != null) {
        ePwd = current.getPassword();
        config.setUserid(current.getUserid());
      }
    } else {
      try {
        ePwd =
            PSEncryptor.encryptString(
                PathUtils.getRxPath().toAbsolutePath().toString().concat(PSEncryptor.SECURE_DIR),
                pwd);
      } catch (PSEncryptionException e) {
        ePwd = pwd;
        config.setEncrypted(false);
      }
    }

    config.setPassword(ePwd);

    var objectMapper = new tools.jackson.databind.ObjectMapper();
    String objectToJson = null;
    try {
      objectToJson = objectMapper.writeValueAsString(config);
    } catch (JacksonException e) {
      log.error("Exception occurred while parsing -> {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    var metadata = new PSMetadata(METADATA_KEY, objectToJson);
    metadataService.save(metadata);
  }

  @Override
  public void deleteConfig() throws IPSGenericDao.LoadException, IPSGenericDao.DeleteException {
    metadataService.delete(METADATA_KEY);
  }

  @Override
  public PSAnalyticsProviderConfig loadConfig(boolean encrypted)
      throws IPSGenericDao.LoadException, PSValidationException {
    var metadata = metadataService.find(METADATA_KEY);
    PSAnalyticsProviderConfig config = null;
    String userID;
    if (metadata == null) return null;
    var json = metadata.getData();
    if (StringUtils.isBlank(json)) return null;
    var objectMapper = new tools.jackson.databind.ObjectMapper();
    try {
      config = objectMapper.readValue(json, PSAnalyticsProviderConfig.class);

      var rawPwd = config.getPassword();
      userID = config.getUserid();
      if (userID == null) {
        userID = config.getUid();
      }

      String pwd;
      try {
        pwd =
            encrypted
                ? rawPwd
                : PSEncryptor.decryptString(
                    PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), rawPwd);
      } catch (PSEncryptionException | IllegalArgumentException e) {
        pwd =
            PSLegacyEncrypter.getInstance(
                    PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR))
                .decrypt(
                    rawPwd,
                    PSLegacyEncrypter.getInstance(
                            PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR))
                        .CRYPT_KEY(),
                    null);
      }

      config.setPassword(pwd);
      config.setUserid(userID);
    } catch (JacksonException e) {
      log.error("Error parsing Analytics configuration: {}", e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      var builder = validateParameters("json file");
      var msg = "Error parsing Analytics configuration:" + e.getMessage();
      builder.reject("Invalid JSON", msg).throwIfInvalid();
    }

    return config;
  }

  @Override
  public Map<String, String> getProfiles(String uid, String password)
      throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException {
    if (StringUtils.isBlank(uid) || StringUtils.isBlank(password)) {
      // One of the creds is null, try to use stored cred
      var config = loadConfig(false);
      if (config != null) {
        if (StringUtils.isBlank(uid)) uid = config.getUserid();
        if (StringUtils.isBlank(password)) password = config.getPassword();
      } else {
        var builder = new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
        builder
            .reject(
                PSAnalyticsProviderException.CAUSETYPE.INVALID_CREDS.toString(),
                "User id and password are both required.")
            .throwIfInvalid();
      }
    }
    return handler.getProfiles(uid, password);
  }

  @Override
  public void testConnection(String uid, String password)
      throws PSAnalyticsProviderException,
          IPSGenericDao.LoadException,
          IPSGenericDao.SaveException,
          PSValidationException {
    if (StringUtils.isBlank(uid) || StringUtils.isBlank(password)) {
      // One of the creds is null, try to use stored cred
      var config = loadConfig(false);
      if (config != null) {
        if (StringUtils.isBlank(uid)) uid = config.getUserid();
        if (StringUtils.isBlank(password)) password = config.getPassword();
      } else {
        var builder = new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
        builder
            .reject(
                PSAnalyticsProviderException.CAUSETYPE.INVALID_CREDS.toString(),
                "User id and keyfile both required.")
            .throwIfInvalid();
      }
    } else {
      var config = loadConfig(false);
      if (config == null
          || !StringUtils.equalsIgnoreCase(config.getUserid(), uid)
          || (StringUtils.isNotEmpty(password))) {
        config = new PSAnalyticsProviderConfig(uid, password, false, null);
        saveConfig(config);
      }
    }
    handler.testConnection(uid, password);
  }

  @Override
  public boolean isProfileConfigured(String sitename)
      throws IPSGenericDao.LoadException, PSValidationException {
    return getSiteProfile(sitename) != null;
  }

  @Override
  public String getProfileId(String sitename) throws IPSGenericDao.LoadException {
    return getProfileProperty(sitename, 0);
  }

  @Override
  public String getWebPropertyId(String sitename) throws IPSGenericDao.LoadException {
    return getProfileProperty(sitename, 1);
  }

  @Override
  public String getGoogleApiKey(String sitename) throws IPSGenericDao.LoadException {
    return getProfileProperty(sitename, 2);
  }

  /**
   * Gets the specified profile property for the specified site.
   *
   * @param sitename the name of the site, assumed not blank.
   * @param propertyIndex the index of the property. Assume it is 0, 1 or 2, which is 'profile id',
   *     'web property ID' or 'API key'.
   * @return the profile property. It is null if it is not configured for the site.
   */
  private String getProfileProperty(String sitename, int propertyIndex)
      throws IPSGenericDao.LoadException {
    String profile;
    try {
      profile = getSiteProfile(sitename);
    } catch (PSValidationException e) {
      log.error("Error Getting Site profile -> {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return null;
    }
    if (profile == null) return null;

    var properties = profile.split("\\|");
    if (properties.length <= propertyIndex) return null;

    return properties[propertyIndex];
  }

  /**
   * Gets the profile for the specified site. The profile contains
   * <profile-id>|<web-property-id>|<api-key>. The API key is optional. If the profile is configured
   * correctly, it should contain profile-id and tracking-code.
   *
   * @param sitename the name of the site, assumed not blank.
   * @return the profile of the site. It may be null if it is not configured for the site.
   */
  private String getSiteProfile(String sitename)
      throws IPSGenericDao.LoadException, PSValidationException {
    if (StringUtils.isBlank(sitename))
      throw new IllegalArgumentException("sitename cannot be null or empty.");
    var config = loadConfig(false);
    if (config == null) return null;
    var params = config.getExtraParamsMap();
    if (params != null && !params.isEmpty()) {
      return params.get(sitename);
    } else {
      return null;
    }
  }

  @Override
  public IPSAnalyticsErrorMessageHandler getErrorMessageHandler() {
    return messageHandler;
  }

  /** The metadata key to store the config. */
  public static final String METADATA_KEY = "perc.analytics.provider.config";
}
