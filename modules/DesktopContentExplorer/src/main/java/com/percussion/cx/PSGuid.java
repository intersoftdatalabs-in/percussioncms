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

package com.percussion.cx;

/**
 * Encapsulates a Rhythmyx content GUID as a packed long value, providing accessors for the content
 * id, revision and content type components stored in the value.
 */
public class PSGuid {
  private long guid;

  /** Default constructor. */
  public PSGuid() {}

  /**
   * Constructs a GUID from a content id, initializing revision to -1 and type to 101.
   *
   * @param id the content id, may be any int.
   */
  public PSGuid(int id) {
    setId(id);
    setRevision(-1);
    setType(101);
  }

  /**
   * Constructs a GUID from a pre-packed long value.
   *
   * @param guid the packed GUID value.
   */
  public PSGuid(long guid) {
    this.guid = guid;
  }

  /**
   * Gets the packed GUID value.
   *
   * @return the packed GUID value.
   */
  public long getPSGuid() {
    return guid;
  }

  /**
   * Sets the packed GUID value.
   *
   * @param guid the new packed GUID value.
   */
  public void setPSGuid(long guid) {
    this.guid = guid;
  }

  /**
   * Gets the content id portion of the GUID.
   *
   * @return the content id.
   */
  public int getId() {
    return PSGuid.getId(guid);
  }

  /**
   * Sets the content id portion of the GUID.
   *
   * @param id the new content id.
   */
  public void setId(int id) {
    long mask = 0xffffffff00000000L;
    guid = mask & guid | Long.valueOf(id).longValue();
  }

  /**
   * Gets the revision portion of the GUID.
   *
   * @return the revision.
   */
  public int getRevision() {
    return getRevision(guid);
  }

  /**
   * Sets the revision portion of the GUID.
   *
   * @param revision the new revision.
   */
  public void setRevision(int revision) {
    long mask = 0xffffffffffL;
    guid = mask & guid | Long.valueOf(revision).longValue() << 40;
  }

  /**
   * Gets the content type portion of the GUID.
   *
   * @return the content type.
   */
  public int getType() {
    return PSGuid.getType(guid);
  }

  /**
   * Sets the content type portion of the GUID.
   *
   * @param type the new content type.
   */
  public void setType(int type) {
    long mask = 0xffffff00ffffffffL;
    guid = mask & guid | Long.valueOf(type).longValue() << 32;
  }

  /**
   * Returns a human-readable string of the form "type-id-revision".
   *
   * @return the string representation, never <code>null</code>.
   */
  public String toString() {
    return getType() + "-" + getId() + "-" + getRevision();
  }

  /**
   * Extracts the content id from a packed GUID value.
   *
   * @param id the packed GUID value.
   * @return the content id.
   */
  public static int getId(long id) {
    return (int) (id & 0xffffffffL);
  }

  /**
   * Extracts the revision from a packed GUID value.
   *
   * @param id the packed GUID value.
   * @return the revision.
   */
  public static int getRevision(long id) {
    return (int) (id >> 40);
  }

  /**
   * Extracts the content type from a packed GUID value.
   *
   * @param id the packed GUID value.
   * @return the content type.
   */
  public static int getType(long id) {
    return (int) (id >> 32) & 0xff;
  }
}
