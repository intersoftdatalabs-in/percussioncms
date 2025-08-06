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
package com.percussion.pagemanagement.dao.impl;

import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Abstract DAO for content items, providing generic CRUD operations.
 * @param <T> the type of item summary.
 */
public abstract class PSAbstractContentItemDao<T extends IPSItemSummary> implements IPSGenericDao<T, String> {

    private final IPSContentItemDao contentItemDao;
    private final IPSIdMapper idMapper;

    protected abstract String getType();

    protected abstract T createObject();

    protected abstract void convertToObject(PSContentItem contentItem, T object);

    protected abstract void convertToItem(T page, PSContentItem contentItem);

    /**
     * Hook for subclasses to perform actions after saving an item.
     */
    protected void postItemSave(@SuppressWarnings("unused") T object,
                                @SuppressWarnings("unused") PSContentItem contentItem)
            throws SaveException, LoadException {
        // No-op by default.
    }

    public PSAbstractContentItemDao(IPSContentItemDao contentItemDao, IPSIdMapper idMapper) {
        this.contentItemDao = contentItemDao;
        this.idMapper = idMapper;
    }

    protected final IPSContentItemDao getContentItemDao() {
        return contentItemDao;
    }

    @Override
    public void delete(String id) throws PSDataServiceException {
        find(id);
        contentItemDao.delete(id);
    }

    @Override
    public List<T> findAll() throws PSDataServiceException {
        var ids = getContentItemDao().findAllItemIdsByType(getType());
        var results = new ArrayList<T>();
        for (var id : ids) {
            var guid = new PSLegacyGuid(id, -1);
            var sid = idMapper.getString(guid);
            results.add(find(sid));
        }
        return results;
    }

    @Override
    public T find(String id) throws PSDataServiceException {
        notNull(id, "id");
        var contentItem = contentItemDao.find(id);
        if (contentItem == null) {
            return null;
        }
        return getObjectFromContentItem(contentItem);
    }

    /**
     * Converts the specified content item to the generic object.
     *
     * @param contentItem the Content Item, not {@code null}.
     * @return the generic object, not {@code null}.
     */
    protected T getObjectFromContentItem(PSContentItem contentItem) {
        notNull(contentItem, "contentItem");
        isTrue(isNotBlank(contentItem.getId()), "contentItem#getId() is blank");
        var object = createObject();
        object.setType(getType());
        object.setId(contentItem.getId());
        object.setFolderPaths(contentItem.getFolderPaths());
        object.setCategory(contentItem.getCategory());
        convertToObject(contentItem, object);
        return object;
    }

    @Override
    public T save(T object) throws PSDataServiceException {
        var item = new PSContentItem();
        item.setId(object.getId());
        item.setType(getType());
        item.setFolderPaths(object.getFolderPaths());
        convertToItem(object, item);
        item = contentItemDao.save(item);
        object.setId(item.getId());
        object.setType(item.getType());
        object.setFolderPaths(item.getFolderPaths());
        postItemSave(object, item);
        return find(item.getId());
    }

    /**
     * Gets the first folder path from the content item, or null if none.
     */
    protected String getFolderPath(PSContentItem contentItem) {
        var paths = contentItem.getFolderPaths();
        if (paths != null && !paths.isEmpty()) {
            return paths.get(0);
        }
        return null;
    }

    /**
     * The log instance to use for this class, never {@code null}.
     */
    private static final Logger log = LogManager.getLogger(PSAbstractContentItemDao.class);
}
