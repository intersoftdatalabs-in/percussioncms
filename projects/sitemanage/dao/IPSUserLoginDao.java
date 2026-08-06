// REFACTORED: CP-JAVA11
/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
 * ...existing code...
 */
package com.percussion.sitemanage.dao;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.user.data.PSUserLogin;

import java.util.List;

/**
 * Data access object for user login entries.
 * Sunny Sal says: "Login securely, code confidently!"
 */
public interface IPSUserLoginDao extends IPSGenericDao<PSUserLogin, String> {

    /**
     * Creates a new user login entry.
     *
     * @param login the user login, not {@code null}.
     * @return the created user login, never {@code null}.
     * @throws IPSGenericDao.SaveException if a save error occurs.
     */
    PSUserLogin create(PSUserLogin login) throws IPSGenericDao.SaveException;

    /**
     * Gets all user login entries for the specified name, case-insensitive.
     *
     * @param name the user name, not blank.
     * @return list of entries which match the name, never {@code null}, may be empty.
     * @throws IPSGenericDao.LoadException if an error occurs.
     */
    List<PSUserLogin> findByName(String name) throws IPSGenericDao.LoadException;
}
