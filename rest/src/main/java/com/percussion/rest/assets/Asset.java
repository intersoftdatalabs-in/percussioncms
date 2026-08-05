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

package com.percussion.rest.assets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.rest.LinkRef;
import com.percussion.rest.pages.WorkflowInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Represents a shared asset. */
@XmlRootElement(name = "Asset")
@JsonRootName(value = "Asset")
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder()
@XmlType(propOrder = {})
@Schema(description = "Represents a shared asset")
public class Asset {

  /** Custom fields on the asset. */
  @Schema(description = "fields")
  private AssetFieldList fields = new AssetFieldList();

  /** Asset id. */
  @Schema(
      description =
          "id must match the id of the item for the same server path, usually best not to send id"
              + " to server.")
  private String id;

  /** Asset name. */
  private String name;

  /** Asset type. */
  private String type;

  /** Folder path of the asset. */
  private String folderPath;

  /** Workflow information. */
  private WorkflowInfo workflow;

  /** Last modification timestamp. */
  private Date lastModifiedDate;

  /** Creation timestamp. */
  private Date createdDate;

  /** Hypermedia links. */
  private List<LinkRef> links;

  /** Full-size image info. */
  private ImageInfo image;

  /** Thumbnail image info. */
  private ImageInfo thumbnail;

  /** Binary file payload. */
  private BinaryFile file;

  /** Flag indicating the asset should be removed on save. */
  private Boolean remove;

  /** No-op constructor. */
  public Asset() {}

  // --- Getters and Setters with Optional ---

  /**
   * Returns the asset's custom fields.
   *
   * @return the fields
   */
  public AssetFieldList getFields() {
    return fields;
  }

  /**
   * Replaces the asset's custom fields.
   *
   * @param fields the new fields
   */
  public void setFields(AssetFieldList fields) {
    this.fields = fields;
  }

  /**
   * Returns the asset id.
   *
   * @return the asset id, may be empty
   */
  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  /**
   * Sets the asset id.
   *
   * @param id the new asset id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the asset name.
   *
   * @return the name, may be empty
   */
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  /**
   * Sets the asset name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the asset type.
   *
   * @return the type, may be empty
   */
  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  /**
   * Sets the asset type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the asset's folder path.
   *
   * @return the folder path, may be empty
   */
  public Optional<String> getFolderPath() {
    return Optional.ofNullable(folderPath);
  }

  /**
   * Sets the asset's folder path.
   *
   * @param path the new folder path
   */
  public void setFolderPath(String path) {
    this.folderPath = path;
  }

  /**
   * Returns the workflow info for the asset.
   *
   * @return the workflow info, may be empty
   */
  public Optional<WorkflowInfo> getWorkflow() {
    return Optional.ofNullable(workflow);
  }

  /**
   * Sets the workflow info for the asset.
   *
   * @param workflow the new workflow info
   */
  public void setWorkflow(WorkflowInfo workflow) {
    this.workflow = workflow;
  }

  /**
   * Returns the last modification date.
   *
   * @return the date, may be empty
   */
  public Optional<Date> getLastModifiedDate() {
    return Optional.ofNullable(lastModifiedDate);
  }

  /**
   * Sets the last modification date.
   *
   * @param lastModifiedDate the new date
   */
  public void setLastModifiedDate(Date lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  /**
   * Returns the creation date.
   *
   * @return the date, may be empty
   */
  public Optional<Date> getCreatedDate() {
    return Optional.ofNullable(createdDate);
  }

  /**
   * Sets the creation date.
   *
   * @param createdDate the new date
   */
  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Returns the hypermedia links.
   *
   * @return the links, may be empty
   */
  public Optional<List<LinkRef>> getLinks() {
    return Optional.ofNullable(links);
  }

  /**
   * Sets the hypermedia links.
   *
   * @param links the new links
   */
  public void setLinks(List<LinkRef> links) {
    this.links = links;
  }

  /**
   * Returns the full-size image info.
   *
   * @return the image info, may be empty
   */
  public Optional<ImageInfo> getImage() {
    return Optional.ofNullable(image);
  }

  /**
   * Sets the full-size image info.
   *
   * @param image the new image info
   */
  public void setImage(ImageInfo image) {
    this.image = image;
  }

  /**
   * Returns the thumbnail image info.
   *
   * @return the thumbnail, may be empty
   */
  public Optional<ImageInfo> getThumbnail() {
    return Optional.ofNullable(thumbnail);
  }

  /**
   * Sets the thumbnail image info.
   *
   * @param thumbnail the new thumbnail
   */
  public void setThumbnail(ImageInfo thumbnail) {
    this.thumbnail = thumbnail;
  }

  /**
   * Returns the binary file payload.
   *
   * @return the binary file, may be empty
   */
  public Optional<BinaryFile> getFile() {
    return Optional.ofNullable(file);
  }

  /**
   * Sets the binary file payload.
   *
   * @param file the new binary file
   */
  public void setFile(BinaryFile file) {
    this.file = file;
  }

  /**
   * Returns whether the asset should be removed on save.
   *
   * @return the remove flag, may be empty
   */
  public Optional<Boolean> getRemove() {
    return Optional.ofNullable(remove);
  }

  /**
   * Sets whether the asset should be removed on save.
   *
   * @param remove the new remove flag
   */
  public void setRemove(Boolean remove) {
    this.remove = remove;
  }

  // --- equals, hashCode, toString ---

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Asset)) return false;
    var asset = (Asset) o;
    return Objects.equals(fields, asset.fields)
        && Objects.equals(id, asset.id)
        && Objects.equals(name, asset.name)
        && Objects.equals(type, asset.type)
        && Objects.equals(folderPath, asset.folderPath)
        && Objects.equals(workflow, asset.workflow)
        && Objects.equals(lastModifiedDate, asset.lastModifiedDate)
        && Objects.equals(createdDate, asset.createdDate)
        && Objects.equals(links, asset.links)
        && Objects.equals(image, asset.image)
        && Objects.equals(thumbnail, asset.thumbnail)
        && Objects.equals(file, asset.file)
        && Objects.equals(remove, asset.remove);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        fields,
        id,
        name,
        type,
        folderPath,
        workflow,
        lastModifiedDate,
        createdDate,
        links,
        image,
        thumbnail,
        file,
        remove);
  }

  @Override
  public String toString() {
    return "Asset{"
        + "fields="
        + fields
        + ", id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", type='"
        + type
        + '\''
        + ", folderPath='"
        + folderPath
        + '\''
        + ", workflow="
        + workflow
        + ", lastModifiedDate="
        + lastModifiedDate
        + ", createdDate="
        + createdDate
        + ", links="
        + links
        + ", image="
        + image
        + ", thumbnail="
        + thumbnail
        + ", file="
        + file
        + ", remove="
        + remove
        + '}';
  }
}