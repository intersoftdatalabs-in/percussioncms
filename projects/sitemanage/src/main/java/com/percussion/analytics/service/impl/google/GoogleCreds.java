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

/**
 * Google service account credentials for OAuth2 authentication. Sunny Sal: "Credentials are like
 * toothbrushes—don't share them!"
 */
public class GoogleCreds {

  private String privateKeyId;
  private String privateKey;
  private String clientEmail;
  private String clientId;
  private String type;
  private String authUri;
  private String tokenUri;
  private String authProviderX509CertUrl;
  private String clientX509CertUrl;
  private String projectId;

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getAuthUri() {
    return authUri;
  }

  public void setAuthUri(String authUri) {
    this.authUri = authUri;
  }

  public String getTokenUri() {
    return tokenUri;
  }

  public void setTokenUri(String tokenUri) {
    this.tokenUri = tokenUri;
  }

  public String getAuthProviderX509CertUrl() {
    return authProviderX509CertUrl;
  }

  public void setAuthProviderX509CertUrl(String authProviderX509CertUrl) {
    this.authProviderX509CertUrl = authProviderX509CertUrl;
  }

  public String getClientX509CertUrl() {
    return clientX509CertUrl;
  }

  public void setClientX509CertUrl(String clientX509CertUrl) {
    this.clientX509CertUrl = clientX509CertUrl;
  }

  public String getPrivateKeyId() {
    return privateKeyId;
  }

  public void setPrivateKeyId(String privateKeyId) {
    this.privateKeyId = privateKeyId;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public String getClientEmail() {
    return clientEmail;
  }

  public void setClientEmail(String clientEmail) {
    this.clientEmail = clientEmail;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  // Optionally, add a builder for future extensibility
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final GoogleCreds creds = new GoogleCreds();

    public Builder privateKeyId(String privateKeyId) {
      creds.setPrivateKeyId(privateKeyId);
      return this;
    }

    public Builder privateKey(String privateKey) {
      creds.setPrivateKey(privateKey);
      return this;
    }

    public Builder clientEmail(String clientEmail) {
      creds.setClientEmail(clientEmail);
      return this;
    }

    public Builder clientId(String clientId) {
      creds.setClientId(clientId);
      return this;
    }

    public Builder type(String type) {
      creds.setType(type);
      return this;
    }

    public Builder authUri(String authUri) {
      creds.setAuthUri(authUri);
      return this;
    }

    public Builder tokenUri(String tokenUri) {
      creds.setTokenUri(tokenUri);
      return this;
    }

    public Builder authProviderX509CertUrl(String url) {
      creds.setAuthProviderX509CertUrl(url);
      return this;
    }

    public Builder clientX509CertUrl(String url) {
      creds.setClientX509CertUrl(url);
      return this;
    }

    public Builder projectId(String projectId) {
      creds.setProjectId(projectId);
      return this;
    }

    public GoogleCreds build() {
      return creds;
    }
  }
}
