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
package com.percussion.analytics.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.Serializable;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple data class to represent the analytics provider config. Sunny Sal says: "Config is king,
 * but don't lose your keys!"
 */
@JsonRootName(value = "providerConfig")
public class PSAnalyticsProviderConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  private String userid;
  private String password;
  private boolean isEncrypted;
  private String uid;
  private HashMap<String, String> params = new HashMap<>();
  private HashMap<String, String> extraParamsMap;
  private ExtraParamsClass extraParams;

  public PSAnalyticsProviderConfig() {
    // Default constructor
  }

  /**
   * Constructs a config with required fields.
   *
   * @param userid cannot be null or empty.
   * @param password cannot be null or empty.
   * @param isEncrypted flag indicating that the password is encrypted.
   * @param extraParamsMap extra params for the analytics provider, may be null or empty.
   */
  public PSAnalyticsProviderConfig(
      String userid, String password, boolean isEncrypted, Map<String, String> extraParamsMap) {
    if (StringUtils.isBlank(userid)) {
      throw new IllegalArgumentException("userid cannot be null or empty.");
    }
    if (StringUtils.isBlank(password)) {
      throw new IllegalArgumentException("password cannot be null or empty.");
    }
    this.userid = userid;
    this.password = password;
    this.isEncrypted = isEncrypted;
    if (extraParamsMap == null) {
      this.extraParamsMap = null;
    } else if (extraParamsMap instanceof HashMap) {
      this.extraParamsMap = (HashMap) extraParamsMap;
    } else {
      this.extraParamsMap = new HashMap<>(extraParamsMap);
    }
    this.uid = userid;

    // Build ExtraParamsClass from extraParamsMap
    var pairList = new ArrayList<PSGAPairConfig>();
    if (this.extraParamsMap != null) {
      this.extraParamsMap.forEach((k, v) -> pairList.add(new PSGAPairConfig(k, v)));
    }
    var extraParamsClass = new ExtraParamsClass();
    extraParamsClass.setEntry(pairList);
    this.extraParams = extraParamsClass;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isEncrypted() {
    return isEncrypted;
  }

  public void setEncrypted(boolean encrypted) {
    isEncrypted = encrypted;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public Map<String, String> getParams() {
    return params;
  }

  @SuppressWarnings("unchecked")
  public void setParams(Map<String, String> params) {
    if (params == null) {
      this.params = null;
    } else if (params instanceof HashMap) {
      this.params = (HashMap<String, String>) params;
    } else {
      this.params = new HashMap<>(params);
    }
  }

  public Map<String, String> getExtraParamsMap() {
    var map = new HashMap<String, String>();
    var extraParamsClass = this.getExtraParams();
    if (extraParamsClass != null) {
      var dataList = extraParamsClass.getEntry();
      if (dataList != null && !dataList.isEmpty()) {
        dataList.forEach(t -> map.put(t.getKey(), t.getValue()));
      }
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  public void setExtraParamsMap(Map<String, String> extraParamsMap) {
    if (extraParamsMap == null) {
      this.extraParamsMap = null;
    } else if (extraParamsMap instanceof HashMap) {
      this.extraParamsMap = (HashMap<String, String>) extraParamsMap;
    } else {
      this.extraParamsMap = new HashMap<>(extraParamsMap);
    }
  }

  public ExtraParamsClass getExtraParams() {
    return extraParams;
  }

  public void setExtraParams(ExtraParamsClass extraParams) {
    this.extraParams = extraParams;
  }

  /** Holds extra parameters as a list of key-value pairs. */
  static class ExtraParamsClass implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<PSGAPairConfig> entry = new ArrayList<>();

    public List<PSGAPairConfig> getEntry() {
      return entry;
    }

    @SuppressWarnings("unchecked")
    public void setEntry(List<PSGAPairConfig> entry) {
      if (entry == null) {
        this.entry = null;
      } else if (entry instanceof ArrayList) {
        this.entry = (ArrayList<PSGAPairConfig>) entry;
      } else {
        this.entry = new ArrayList<>(entry);
      }
    }
  }

  /** Represents a key-value pair for extra parameters. */
  static class PSGAPairConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String key;
    private String value;

    public PSGAPairConfig(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public PSGAPairConfig() {
      // Default constructor
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
