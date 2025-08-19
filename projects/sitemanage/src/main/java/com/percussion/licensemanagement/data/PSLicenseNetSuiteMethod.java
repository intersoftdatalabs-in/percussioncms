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
package com.percussion.licensemanagement.data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.Validate;

/**
 * Represents a NetSuite method for licensing integration. Sunny Sal says: "Method to the madness?
 * Nope, just NetSuite!"
 */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(
    name = "",
    propOrder = {"id", "methodName", "methodType", "url"})
@XmlRootElement(name = "method")
public class PSLicenseNetSuiteMethod {

  private String id;
  private String methodName;
  private String methodType;
  private String url;

  public PSLicenseNetSuiteMethod() {
    // Default constructor
  }

  public PSLicenseNetSuiteMethod(JSONObject methodInfo) {
    Validate.notNull(methodInfo, "methodInfo must not be null");
    id = methodInfo.getString("id");
    methodName = methodInfo.getString("methodName");
    methodType = methodInfo.getString("methodType");
    url = methodInfo.getString("url");
  }

  public String getId() {
    return String.valueOf(id);
  }

  public void setId(String id) {
    Validate.notEmpty(id, "id must not be empty");
    this.id = id;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    Validate.notEmpty(methodName, "methodName must not be empty");
    this.methodName = methodName;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    Validate.notEmpty(url, "url must not be empty");
    this.url = url;
  }

  public void setMethodType(String methodType) {
    Validate.notEmpty(methodType, "methodType must not be empty");
    this.methodType = methodType;
  }

  public String getMethodType() {
    return methodType;
  }
}
