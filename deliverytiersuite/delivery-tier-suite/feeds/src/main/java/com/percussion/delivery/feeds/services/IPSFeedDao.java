/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.services;

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for feed descriptors with support for multiple repository types.
 * Implementations can target different storage backends (RDBMS, BigTable, etc.).
 */
public interface IPSFeedDao {
    /**
     * Saves or updates a list of feed descriptors.
     *
     * @param descriptors List of descriptors to save, must not be null
     * @throws IllegalArgumentException if descriptors is null or contains null elements
     */
    void saveDescriptors(List<IPSFeedDescriptor> descriptors);

    /**
     * Retrieves all existing feed descriptors.
     *
     * @return Immutable list of all feed descriptors, never null
     */
    List<IPSFeedDescriptor> findAll();

    /**
     * Finds a feed descriptor by its name.
     *
     * @param name the name of the feed to find, must not be null
     * @return Optional containing the feed descriptor if found, empty if not found
     * @throws IllegalArgumentException if name is null
     */
    Optional<IPSFeedDescriptor> findByName(String name);

    /**
     * Finds feed descriptors by site name.
     *
     * @param site the site name to search for, must not be null
     * @return List of feed descriptors for the site, empty if none found
     * @throws IllegalArgumentException if site is null
     */
    List<IPSFeedDescriptor> findBySite(String site);

    /**
     * Deletes a feed descriptor by its name.
     *
     * @param name the name of the feed to delete, must not be null
     * @return true if the feed was deleted, false if it didn't exist
     * @throws IllegalArgumentException if name is null
     */
    boolean deleteByName(String name);

    /**
     * Checks if a feed descriptor exists by name.
     *
     * @param name the name of the feed to check, must not be null
     * @return true if the feed exists, false otherwise
     * @throws IllegalArgumentException if name is null
     */
    boolean existsByName(String name);

    /**
     * Returns the total count of feed descriptors.
     *
     * @return the total number of feed descriptors
     */
    long count();
}
