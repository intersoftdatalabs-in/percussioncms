/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.rest.assets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.rest.LinkRef;
import com.percussion.rest.pages.WorkflowInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name="Asset")
@JsonRootName(value="Asset")
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder()
@Schema(description = "Represents a shared asset")
public class Asset
{
    @Schema(description = "fields")
	private AssetFieldList fields = new AssetFieldList();
	@Schema(description = "id must match the id of the item for the same server path, usually best not to send id to server.")
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
	private Flash flash;
	
	private Boolean remove;
	

    /**
	 * @return the fields
	 */
	public AssetFieldList getFields()
	{
		return fields;
	}

	/**
	 * @param fields
	 *            the fields to set
	 */
	public void setFields(AssetFieldList fields)
	{
		this.fields = fields;
	}

	/**
	 * @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @return the path
	 */
	public String getFolderPath()
	{
		return folderPath;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setFolderPath(String path)
	{
		this.folderPath = path;
	}

	/**
	 * @return the workflow
	 */
	public WorkflowInfo getWorkflow()
	{
		return workflow;
	}

	/**
	 * @param workflow
	 *            the workflow to set
	 */
	public void setWorkflow(WorkflowInfo workflow)
	{
		this.workflow = workflow;
	}

	/**
	 * @return the lastModifiedDate
	 */
	public Date getLastModifiedDate()
	{
		return lastModifiedDate;
	}

	/**
	 * @param lastModifiedDate
	 *            the lastModifiedDate to set
	 */
	public void setLastModifiedDate(Date lastModifiedDate)
	{
		this.lastModifiedDate = lastModifiedDate;
	}

	/**
	 * @return the createdDate
	 */
	public Date getCreatedDate()
	{
		return createdDate;
	}

	/**
	 * @param createdDate the createdDate to set
	 */
	public void setCreatedDate(Date createdDate)
	{
		this.createdDate = createdDate;
	}

	/**
	 * @return the links
	 */
	public List<LinkRef> getLinks()
	{
		return links;
	}

	/**
	 * @param links the links to set
	 */
	public void setLinks(List<LinkRef> links)
	{
		this.links = links;
	}

	/**
	 * @return the image
	 */
	public ImageInfo getImage()
	{
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(ImageInfo image)
	{
		this.image = image;
	}

	/**
	 * @return the thumbnail
	 */
	public ImageInfo getThumbnail()
	{
		return thumbnail;
	}

	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(ImageInfo thumbnail)
	{
		this.thumbnail = thumbnail;
	}

	/**
	 * @return the file
	 */
	public BinaryFile getFile()
	{
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(BinaryFile file)
	{
		this.file = file;
	}

	/**
	 * @return the flash
	 */
	public Flash getFlash()
	{
		return flash;
	}
	
	/**
     * @return is remove set
     */
	public Boolean getRemove()
    {
        return remove;
    }

	/**
     * @param remove To remove the asset of not
     */
    public void setRemove(Boolean remove)
    {
        this.remove = remove;
    }
	/**
	 * @param flash the flash to set
	 */
	public void setFlash(Flash flash)
	{
		this.flash = flash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Asset)) return false;
		Asset asset = (Asset) o;
		return Objects.equals(getFields(), asset.getFields()) && Objects.equals(getId(), asset.getId()) && Objects.equals(getName(), asset.getName()) && Objects.equals(getType(), asset.getType()) && Objects.equals(getFolderPath(), asset.getFolderPath()) && Objects.equals(getWorkflow(), asset.getWorkflow()) && Objects.equals(getLastModifiedDate(), asset.getLastModifiedDate()) && Objects.equals(getCreatedDate(), asset.getCreatedDate()) && Objects.equals(getLinks(), asset.getLinks()) && Objects.equals(getImage(), asset.getImage()) && Objects.equals(getThumbnail(), asset.getThumbnail()) && Objects.equals(getFile(), asset.getFile()) && Objects.equals(getFlash(), asset.getFlash()) && Objects.equals(getRemove(), asset.getRemove());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getFields(), getId(), getName(), getType(), getFolderPath(), getWorkflow(), getLastModifiedDate(), getCreatedDate(), getLinks(), getImage(), getThumbnail(), getFile(), getFlash(), getRemove());
	}
}
