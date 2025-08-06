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
// REFACTORED: CP-JAVA11
package com.percussion.share.dao;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.IPSFieldValue;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.share.data.PSAbstractPersistantObject;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.security.PSSecurityUtility;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSUnknownContentTypeException;
import com.percussion.webservices.content.IPSContentWs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.owasp.csrfguard.util.RandomGenerator;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * A Generic Content Item Dao that will store a serialized object in 
 * the field {@link #getDataFieldName()}.
 * The {@link #getUniqueIdFieldName()} property is the field used to uniquely identify the content item.
 * @author adamgent
 *
 * @param <T> The object type to be serialized.
 */
public abstract class PSGenericItemDao<T extends PSAbstractPersistantObject> implements IPSGenericDao<T, String> {

    /**
     * The type of object to be serialized to the field with name: {@link #dataFieldName}
     */
    private final Class<T> type;

    private final IPSContentWs contentWs;
    private final PSJcrNodeFinder jcrNodeFinder;
    private final IPSIdMapper idMapper;
    
    /**
     * Content type name.
     */
    private String contentType;
    /**
     * Unique field used for search and locating the item.
     */
    private String uniqueIdFieldName = "sys_title";
    /**
     * Field where the serialized xml is stored.
     */
    private String dataFieldName = "data";
    /**
     * Folder path where the content items will be stored.
     */
    private String folderPath;

    
    

    /**
     * Construct a default implementation of the item dao.
     * Use the setters to override the defaults.
     * 
     * @param contentWs never <code>null</code>.
     * @param contentMgr never <code>null</code>.
     * @param idMapper never <code>null</code>.
     * @param type never <code>null</code>.
     * @param contentType never <code>null</code>.
     * @param folderPath never <code>null</code>.
     */
    protected PSGenericItemDao(
            IPSContentWs contentWs, 
            IPSContentMgr contentMgr, 
            IPSIdMapper idMapper, 
            Class<T> type, 
            String contentType, 
            String folderPath) {
        super();
        notNull(contentWs);
        notNull(contentMgr);
        notNull(type);
        notNull(contentType);
        notNull(idMapper);
        notNull(folderPath);
        this.folderPath = folderPath;
        this.contentWs = contentWs;
        this.idMapper = idMapper;
        this.type = type;
        this.contentType = contentType;
        this.jcrNodeFinder = new PSJcrNodeFinder(contentMgr, this.contentType, this.uniqueIdFieldName);
    }

    public T find(String id) throws com.percussion.share.dao.IPSGenericDao.LoadException {
        log.debug("Finding object id: {}", id);
        notNull(id);
        T object = null;
        try {
            var item = findContentItem(id);
            if (item != null) {
                var field = item.getFieldByName(getDataFieldName());
                var fv = field.getValue();
                var dataField = fv.getValueAsString();
                object = PSSerializerUtils.unmarshal(dataField, type);
            }
        } catch (PSErrorException | PSCmsException | PSErrorResultsException e) {
            handleLoadException(id, e);
        }
        return object;
    }

    private void handleLoadException(String id, Exception e) throws LoadException {
        var error = errorMessage("load", id);
        throw new LoadException(error, e);
    }

    protected PSCoreItem findContentItem(String id)
            throws PSErrorException, PSErrorResultsException {
        notNull(id);
        var userGuid = findContentItemGuid(id);
        if (userGuid == null) return null;
        var items = contentWs.loadItems(Collections.singletonList(userGuid), true, false, false, true);
        if (items.isEmpty()) return null;
        return items.get(0);
    }

    protected abstract IPSGuid findContentItemGuid(String id);

    protected IPSGuid findContentItemGuidByConversion(String id) {
        return idMapper.getGuid(id);
    }

    protected IPSGuid findContentItemGuidWithSearch(String id) {
        var node = jcrNodeFinder.find(folderPath, id);
        if (node == null) {
            return null;
        }
        return node.getGuid();
    }

    public List<T> findAll() throws com.percussion.share.dao.IPSGenericDao.LoadException {
        throw new UnsupportedOperationException("Find all not supported");
    }

    public void delete(String id) throws com.percussion.share.dao.IPSGenericDao.DeleteException {
        log.debug("Deleting {} : {}", type.getSimpleName(), id);
        notNull(id);
        var guid = findContentItemGuid(id);
        if (guid == null) throw new DeleteException("#findContentItemGuid returned null.");
        try {
            contentWs.deleteItems(Collections.singletonList(guid));
        } catch (PSErrorsException | PSErrorException e) {
            var error = errorMessage("delete", id);
            throw new DeleteException(error, e);
        }
    }

    private String errorMessage(String action, String id) {
        return "Failed to " + action + " item with id: " + id + " for type " + type.getSimpleName();
    }

    private String errorMessage(String action, T item) {
        return "Failed to " + action + " item: " + item + " for type " + type.getSimpleName();
    }

    public T save(T object) throws SaveException {
        log.debug("Save {}: {}", type.getSimpleName(), object);
        notNull(object);
        try {
            var id = object.getId();
            PSCoreItem coreItem = null;
            if (id != null)
                coreItem = findContentItem(id);
            if (coreItem == null) {
                if (log.isDebugEnabled())
                    log.debug("Creating content item for object: {}", object);
                coreItem = contentWs.createItems(getContentType(), 1).get(0);
                if (id == null) {
                    var fakeId = createSurrogateId(coreItem, object);
                    isTrue(isNotBlank(fakeId), "Surrogate id cannot be blank");
                    id = fakeId;
                }
            }
            var data = marshal(object);
            var paths = getFolderPaths(object);
            notEmpty(paths);
            coreItem.setFolderPaths(paths);
            coreItem.setTextField("sys_title", id);
            coreItem.setTextField(getUniqueIdFieldName(), id);
            coreItem.setTextField(getDataFieldName(), data);
            var guid = contentWs.saveItems(Collections.singletonList(coreItem), false, false).get(0);
            if (object.getId() == null) {
                object.setId(idMapper.getString(guid));
            }
            return object;
        } catch (PSUnknownContentTypeException e) {
            var err = "Could not save because content type does not exist";
            throw new SaveException(err, e);
        } catch (PSErrorException | PSErrorResultsException e) {
            var error = errorMessage("save", object);
            throw new SaveException(error, e);
        }
    }

    protected String createSurrogateId(PSCoreItem item, T object) {
        log.debug("Creating surrogate id for object: {}", object);
        String surrogateId;
        if (item.getContentId() <= 0) {
            surrogateId = PSGuidManagerLocator.getGuidMgr().makeGuid(item.getContentId(), PSTypeEnum.LEGACY_CONTENT).toString();
        } else {
            surrogateId = RandomGenerator.generateRandomId(PSSecurityUtility.getSecureRandom(), 10);
        }
        log.debug("Surrogate id: {}", surrogateId);
        return surrogateId;
    }

    protected abstract List<String> getFolderPaths(T object);

    protected String marshal(T object) {
        return PSSerializerUtils.marshal(object);
    }

    protected T unmarshal(String dataField) {
        return PSSerializerUtils.unmarshal(dataField, type);
    }

    public String getUniqueIdFieldName() {
        return uniqueIdFieldName;
    }

    public void setUniqueIdFieldName(String uniqueIdFieldName) {
        this.uniqueIdFieldName = uniqueIdFieldName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    public void setDataFieldName(String dataFieldName) {
        this.dataFieldName = dataFieldName;
    }

    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    protected static final Logger log = LogManager.getLogger(PSGenericItemDao.class);

}
