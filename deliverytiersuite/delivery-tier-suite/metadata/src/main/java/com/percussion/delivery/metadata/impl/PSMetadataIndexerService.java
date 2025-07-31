// REFACTORED: CP-JAVA11
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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import com.percussion.delivery.metadata.IPSMetadataDao;
import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataIndexerService;

import java.util.*;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Hibernate-based implementation of {@link IPSMetadataIndexerService}.
 * @author miltonpividori
 */
public class PSMetadataIndexerService implements IPSMetadataIndexerService {

    private final IPSMetadataDao dao;
    private IPSServiceDataChangeListener connector;
    private final List<IPSServiceDataChangeListener> listeners = new ArrayList<>();
    private static final String[] PERC_METADATA_SERVICES = {"perc-metadata-services"};

    @Autowired
    public PSMetadataIndexerService(IPSMetadataDao dao) {
        this.dao = dao;
    }

    @Override
    public void save(IPSMetadataEntry entry) {
        dao.save(entry);
    }

    @Override
    public void save(Collection<IPSMetadataEntry> entries) {
        Validate.notNull(entries, "entries cannot be null");
        if (entries.isEmpty()) return;

        var siteNames = new HashSet<String>(entries.size());
        entries.forEach(entry -> siteNames.add(entry.getSite()));

        var hasDirty = dao.hasDirtyEntries(entries);
        if (hasDirty) {
            fireDataChangeRequestedEvent(siteNames);
        }

        try {
            dao.save(entries);
        } finally {
            if (hasDirty) {
                fireDataChangedEvent(siteNames);
            }
        }
    }

    @Override
    public void delete(String pagepath) {
        Validate.notEmpty(pagepath, "pagepath cannot be null or empty.");
        var siteNames = new HashSet<String>();
        var site = getSiteNameFromPagePath(pagepath);
        siteNames.add(site);

        if (dao.delete(pagepath)) {
            fireDataChangeRequestedEvent(siteNames);
            fireDataChangedEvent(siteNames);
        }
    }

    /**
     * Utility method to extract site name from a page path.
     * Assumes page path is of the form /sitename/rest/of/path/to/page
     */
    private String getSiteNameFromPagePath(String pagepath) {
        var splitPath = pagepath.split("/");
        var site = splitPath.length > 1 ? splitPath[1] : "";
        if (site.endsWith("apps")) {
            site = site.substring(0, site.length() - 4);
        }
        return site;
    }

    @Override
    public void delete(Collection<String> pagepaths) {
        Validate.notNull(pagepaths, "pagepaths cannot be null.");
        var siteNames = new HashSet<String>(pagepaths.size());
        pagepaths.forEach(path -> siteNames.add(getSiteNameFromPagePath(path)));
        dao.delete(pagepaths);
        fireDataChangeRequestedEvent(siteNames);
        fireDataChangedEvent(siteNames);
    }

    @Override
    public IPSMetadataEntry findEntry(String pagepath) {
        Validate.notEmpty(pagepath, "pagepath cannot be null nor empty");
        return dao.findEntry(pagepath);
    }

    @Override
    public Set<String> getAllIndexedDirectories() {
        return dao.getAllIndexedDirectories();
    }

    @Override
    public void deleteAllMetadataEntries() {
        var sites = dao.getAllSites();
        var siteSet = new HashSet<>(sites);
        fireDataChangeRequestedEvent(siteSet);
        dao.deleteAllMetadataEntries();
        fireDataChangedEvent(siteSet);
    }

    @Override
    public List<IPSMetadataEntry> getAllEntries() {
        return dao.getAllEntries();
    }

    @Override
    public void addMetadataListener(IPSServiceDataChangeListener listener) {
        Validate.notNull(listener, "listener cannot be null.");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeMetadataListener(IPSServiceDataChangeListener listener) {
        Validate.notNull(listener, "listener cannot be null.");
        listeners.remove(listener);
    }

    /**
     * Fire a data change event for all registered listeners.
     */
    private void fireDataChangedEvent(Set<String> sites) {
        if (sites == null || sites.isEmpty()) return;
        listeners.forEach(listener -> listener.dataChanged(sites, PERC_METADATA_SERVICES));
    }

    /**
     * Fire a data change requested event for all registered listeners.
     */
    private void fireDataChangeRequestedEvent(Set<String> sites) {
        if (sites == null || sites.isEmpty()) return;
        listeners.forEach(listener -> listener.dataChangeRequested(sites, PERC_METADATA_SERVICES));
    }

    public IPSServiceDataChangeListener getConnector() {
        return connector;
    }

    public void setConnector(IPSServiceDataChangeListener connector) {
        this.connector = connector;
        this.addMetadataListener(connector);
    }
}
