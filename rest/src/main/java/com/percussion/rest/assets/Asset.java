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

@XmlRootElement(name = "Asset")
@JsonRootName(value = "Asset")
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder()
@XmlType(propOrder = {})
@Schema(description = "Represents a shared asset")
public class Asset {

  @Schema(description = "fields")
  private AssetFieldList fields = new AssetFieldList();

  @Schema(
      description =
          "id must match the id of the item for the same server path, usually best not to send id"
              + " to server.")
  private String id;

  private String name;
  private String type;
  private String folderPath;
  private WorkflowInfo workflow;
  private Date lastModifiedDate;
  private Date createdDate;
  private List<LinkRef> links;
  private ImageInfo image;
  private ImageInfo thumbnail;
  private BinaryFile file;
  private Boolean remove;

  public Asset() {}

  // --- Getters and Setters with Optional ---

  public AssetFieldList getFields() {
    return fields;
  }

  public void setFields(AssetFieldList fields) {
    this.fields = fields;
  }

  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(String id) {
    this.id = id;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<String> getFolderPath() {
    return Optional.ofNullable(folderPath);
  }

  public void setFolderPath(String path) {
    this.folderPath = path;
  }

  public Optional<WorkflowInfo> getWorkflow() {
    return Optional.ofNullable(workflow);
  }

  public void setWorkflow(WorkflowInfo workflow) {
    this.workflow = workflow;
  }

  public Optional<Date> getLastModifiedDate() {
    return Optional.ofNullable(lastModifiedDate);
  }

  public void setLastModifiedDate(Date lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public Optional<Date> getCreatedDate() {
    return Optional.ofNullable(createdDate);
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Optional<List<LinkRef>> getLinks() {
    return Optional.ofNullable(links);
  }

  public void setLinks(List<LinkRef> links) {
    this.links = links;
  }

  public Optional<ImageInfo> getImage() {
    return Optional.ofNullable(image);
  }

  public void setImage(ImageInfo image) {
    this.image = image;
  }

  public Optional<ImageInfo> getThumbnail() {
    return Optional.ofNullable(thumbnail);
  }

  public void setThumbnail(ImageInfo thumbnail) {
    this.thumbnail = thumbnail;
  }

  public Optional<BinaryFile> getFile() {
    return Optional.ofNullable(file);
  }

  public void setFile(BinaryFile file) {
    this.file = file;
  }

  public Optional<Boolean> getRemove() {
    return Optional.ofNullable(remove);
  }

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
