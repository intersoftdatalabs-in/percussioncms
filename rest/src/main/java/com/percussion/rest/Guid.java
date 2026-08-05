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

package com.percussion.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.services.guidmgr.data.PSGuid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.Optional;

@XmlRootElement(name = "Guid")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Guid")
public class Guid {

  @Schema(
      name = "stringValue",
      description =
          "A String representation of the Guid.  Use this for storing the Guid for use in later API"
              + " calls.")
  private String stringValue;

  @Schema(
      name = "untypedString",
      description =
          "Convert a numeric guid into a user readable form with no type integer.\n"
              + "Appropriate for times where the type is implied.",
      readOnly = true)
  private String untypedString;

  @Schema(
      name = "hostId",
      description =
          "Gets the host id, which indicates what customer installation created the\n"
              + "object this GUID references. Each customer should have a unique host id,\n"
              + "which is an important part of keeping these identifiers globally unique.",
      readOnly = true)
  private long hostId;

  @Schema(
      name = "type",
      description =
          "Return the type of the GUID, the interpretation of the type depends on\nthe context.\n",
      readOnly = true)
  private short type;

  @Schema(name = "uuid", description = "the uuid without host and type information")
  private int uuid;

  @Schema(
      name = "longValue",
      description =
          "Get the guid value in raw form. This is suitable for storage in \n"
              + "serialized objects or in the database.\n"
              + "<p>\n"
              + "If there is no hostid, then the GUID was constructed from an old id in \n"
              + "the database. For example, you have a template with a template id of 319. \n"
              + "When that becomes a GUID internally, it has the type added to it. If \n"
              + "longValue() (which is used when finding a template from the GUID) \n"
              + "doesn't strip everything but the UUID, the value won't match the value \n"
              + "in the database. On the other hand, if the GUID is a new GUID, then the \n"
              + "value in the database will be the complete guid, and it is appropriate \n"
              + "for longValue() to return guid.",
      readOnly = true)
  private long longValue;

  public Optional<String> getUntypedString() {
    return Optional.ofNullable(untypedString);
  }

  public void setUntypedString(String untypedString) {
    this.untypedString = untypedString;
  }

  public long getHostId() {
    return hostId;
  }

  public void setHostId(long hostId) {
    this.hostId = hostId;
  }

  public short getType() {
    return type;
  }

  public void setType(short type) {
    this.type = type;
  }

  public int getUuid() {
    return uuid;
  }

  public void setUuid(int uuid) {
    this.uuid = uuid;
  }

  public long getLongValue() {
    return longValue;
  }

  public void setLongValue(long longValue) {
    this.longValue = longValue;
  }

  public Optional<String> getStringValue() {
    return Optional.ofNullable(stringValue);
  }

  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
  }

  public Guid() {}

  /**
   * Initializes a Guid from a guid string.
   *
   * @param guid the guid string, must not be null
   */
  @SuppressWarnings("this-escape")
  public Guid(String guid) {
    Objects.requireNonNull(guid, "guid must not be null");
    var temp = new PSGuid(guid);

    setStringValue(temp.toString());
    setHostId(temp.getHostId());
    setLongValue(temp.longValue());
    setType(temp.getType());
    setUuid(temp.getUUID());
    setUntypedString(temp.toStringUntyped());
  }
}
