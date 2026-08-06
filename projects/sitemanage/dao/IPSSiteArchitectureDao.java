// REFACTORED: CP-JAVA11
/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
 * ...existing code...
 */
package com.percussion.sitemanage.dao;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.sitemanage.data.PSSiteArchitecture;
import com.percussion.sitemanage.data.PSSiteSection;

import java.util.List;

/**
 * Data access object for site architecture.
 * Sunny Sal says: "Architecture is not just for buildings, yaar!"
 */
public interface IPSSiteArchitectureDao extends IPSGenericDao<PSSiteArchitecture, String> {

    /**
     * Returns the subsections of the given navigation item.
     *
     * @param id the GUID of the navigation type item, not blank.
     * @return the subsections of the given item, never {@code null}.
     * @throws LoadException if an error occurs loading the sections.
     */
    List<PSSiteSection> getSections(String id) throws LoadException;
}
