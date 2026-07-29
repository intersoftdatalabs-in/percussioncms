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
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.services.IPSConnectionInfo;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * JPA entity backing the singleton row in the {@code PERC_CONNECTION_INFO} table that stores
 * credentials for the feeds metadata service.
 *
 * @author erikserating
 */
@Entity
@Table(name = "PERC_CONNECTION_INFO")
public class PSConnectionInfo implements IPSConnectionInfo {
  @Id private long id = 1; // this will always be one as there will only be one info entry in table

  @Basic private String url;

  @Basic private String username;

  @Basic private String password;

  @Basic private String encrypted;

  /** Default no-arg constructor required by JPA. */
  public PSConnectionInfo() {}

  /**
   * Constructs a connection info entity with the supplied values.
   *
   * @param url the metadata service URL, may be <code>null</code>
   * @param user the metadata service user name, may be <code>null</code>
   * @param password the metadata service password, may be <code>null</code>
   * @param encrypted <code>true</code> if the password is stored encrypted
   */
  public PSConnectionInfo(String url, String user, String password, boolean encrypted) {
    this.url = url;
    this.username = user;
    this.password = password;
    this.encrypted = Boolean.toString(encrypted);
  }

  /**
   * Gets the metadata service URL.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the metadata service URL.
   *
   * @param url the metadata service URL, may be <code>null</code>
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets the metadata service user name.
   *
   * @return the user
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the metadata service user name.
   *
   * @param user the metadata service user name, may be <code>null</code>
   */
  public void setUsername(String user) {
    this.username = user;
  }

  /**
   * Gets the metadata service password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the metadata service password.
   *
   * @param password the metadata service password, may be <code>null</code>
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Gets the encrypted form of the password, as stored in the database.
   *
   * @return the encrypted
   */
  public String getEncrypted() {
    return encrypted;
  }

  /**
   * Sets the encrypted form of the password, as stored in the database.
   *
   * @param encrypted the encrypted password string, may be <code>null</code>
   */
  public void setEncrypted(String encrypted) {
    this.encrypted = encrypted;
  }

  /**
   * Gets the connection record id. Always <code>1</code> since the table holds a singleton row.
   *
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the connection record id.
   *
   * @param id the connection record id
   */
  public void setId(long id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, url, username, password, encrypted);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PSConnectionInfo that = (PSConnectionInfo) obj;
    return id == that.id
        && Objects.equals(url, that.url)
        && Objects.equals(username, that.username)
        && Objects.equals(password, that.password)
        && Objects.equals(encrypted, that.encrypted);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PSConnectionInfo [id=");
    builder.append(id);
    builder.append(", ");
    if (url != null) {
      builder.append("url=");
      builder.append(url);
      builder.append(", ");
    }
    if (username != null) {
      builder.append("username=");
      builder.append(username);
      builder.append(", ");
    }
    if (password != null) {
      builder.append("password=");
      builder.append(password);
      builder.append(", ");
    }
    if (encrypted != null) {
      builder.append("encrypted=");
      builder.append(encrypted);
    }
    builder.append("]");
    return builder.toString();
  }
}
