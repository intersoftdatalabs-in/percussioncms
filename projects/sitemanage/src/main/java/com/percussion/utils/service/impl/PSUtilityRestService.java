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

package com.percussion.utils.service.impl;

import com.percussion.security.ToDoVulnerability;
import com.percussion.security.dao.IPSSecurityItemsDao;
import com.percussion.server.PSServer;
import com.percussion.share.data.PSListWrapper;
import com.percussion.share.data.PSMapWrapper;
import com.percussion.share.data.PSNoContent;
import com.percussion.utils.data.PSLogData;
import com.percussion.utils.data.PSPrivateKeysResponse;
import com.percussion.utils.service.IPSUtilityService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * REST utility endpoints for encryption, logging, and SaaS settings.
 *
 * <p>Sunny Sal says: "REST easy, your utilities are in good hands!"
 */
@Path("/utility")
@Component("utilityRestService")
@Lazy
@ToDoVulnerability
public class PSUtilityRestService {

  public static final String STRING_KEY = "string";
  public static final String KEY_KEY = "key";
  public static final String SAAS_SETTING_PROPERTY_NAME = "doSAAS";

  private final IPSUtilityService service;
  private final IPSSecurityItemsDao securityItemsDao;

  @Autowired
  public PSUtilityRestService(IPSUtilityService service, IPSSecurityItemsDao securityItemsDao) {
    this.service = service;
    this.securityItemsDao = securityItemsDao;
  }

  @GET
  @Path("/privatekeys")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPrivateKeysResponse getPrivateKeys() {
    var keyNames = securityItemsDao.getAvailablePrivateKeys();
    return new PSPrivateKeysResponse(keyNames);
  }

  @GET
  @Path("/maxInactiveInterval")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSNoContent getMaxInactiveInterval() {
    var result = new PSNoContent();
    var sessTimeout = PSServer.getServerConfiguration().getUserSessionTimeout();
    result.setResult(String.valueOf(sessTimeout));
    return result;
  }

  @POST
  @Path("/encryptstring")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSMapWrapper encryptString(PSMapWrapper values) {
    return encryptDecryptString(values, true);
  }

  @POST
  @Path("/decryptstring")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSMapWrapper decryptString(PSMapWrapper values) {
    return encryptDecryptString(values, false);
  }

  @POST
  @Path("/encryptstrings")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSMapWrapper encryptStrings(PSListWrapper values) {
    Validate.notNull(values, "values must not be null for encryptstrings method");
    Validate.notNull(values.getList(), "value list must not be null for encryptstrings method");
    return encryptDecryptStrings(values.getList(), true);
  }

  @POST
  @Path("/decryptstrings")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSMapWrapper decryptStrings(PSListWrapper values) {
    Validate.notNull(values, "values must not be null for decryptstrings method");
    Validate.notNull(values.getList(), "value list must not be null for decryptstrings method");
    return encryptDecryptStrings(values.getList(), false);
  }

  @POST
  @Path("/log")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSNoContent log(PSLogData logData) {
    Validate.notNull(logData, "logData must not be null for log method");
    service.log(
        logData.getType().orElse("info"),
        logData.getCategory().orElse("General"),
        logData.getMessage().orElse(""));
    return new PSNoContent("Logged the message.");
  }

  @GET
  @Path("/saas/flag")
  @Produces(MediaType.TEXT_PLAIN)
  public boolean getSaaSSetting() {
    return service.isSaaSEnvironment();
  }

  /**
   * Helper method to get values from the map wrapper and then encrypt or decrypt the passed in
   * string.
   */
  private PSMapWrapper encryptDecryptString(PSMapWrapper values, boolean encrypt) {
    var mw = new PSMapWrapper();
    Map<String, String> entries =
        Optional.ofNullable(values).map(PSMapWrapper::getEntries).orElse(Map.of());
    var key = entries.get(KEY_KEY);
    var str = StringUtils.defaultString(entries.get(STRING_KEY), "");
    mw.getEntries()
        .put(
            STRING_KEY,
            encrypt ? service.encryptString(str, key) : service.decryptString(str, key));
    return mw;
  }

  /**
   * Helper method to encrypt or decrypt the supplied values. Uses default key for encryption or
   * decryption.
   */
  private PSMapWrapper encryptDecryptStrings(List<String> values, boolean encrypt) {
    var result = new PSMapWrapper();
    var resultEntries = result.getEntries();
    values.forEach(
        key -> {
          var val = encrypt ? service.encryptString(key, null) : service.decryptString(key, null);
          resultEntries.put(key, val);
        });
    return result;
  }
}
