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

package com.percussion.sitemanage.dao.impl;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.user.data.PSUserLogin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link PSUserLoginDao}.
 */
@Disabled
@SpringJUnitJupiterConfig(locations = "TestSpringContext.xml")
public class PSUserLoginDaoTestIntegration extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Logger log = LogManager.getLogger(PSUserLoginDaoTestIntegration.class);

    @Autowired
    PSUserLoginDao dao;

    @Autowired
    SessionFactory sessionFactory;

    /**
     * Test delete operation for user login.
     */
    @Test
    public void testDelete() throws IPSGenericDao.DeleteException {
        var count = countRows();
        assertEquals(0, count, "user xyzzy already exists");
        addRow();

        log.info("Testing delete of user xyzzy");

        dao.delete("xyzzy");

        count = countRows();
        assertEquals(0, count, "user xyzzy not deleted");
    }

    /**
     * Test find operation for user login.
     */
    @Test
    public void testFind() throws IPSGenericDao.LoadException {
        var count = countRows();
        assertEquals(0, count, "user xyzzy already exists");
        addRow();
        log.info("finding xyzzy");
        var login = dao.find("xyzzy");
        assertNotNull(login);
        log.info("login is " + login);
        assertEquals("xyzzy", login.getUserid());
        assertEquals("demo", login.getPassword());
    }

    /**
     * Test findAll operation for user logins.
     */
    @Test
    public void testFindAll() throws IPSGenericDao.LoadException {
        var count = countRows();
        assertEquals(0, count, "user xyzzy already exists");
        addRow();
        log.info("finding all entries");

        var users = dao.findAll();
        assertTrue(users.size() > 0);
        log.info("There are " + users.size() + " user entries");

        var myLogin = new PSUserLogin();
        myLogin.setUserid("xyzzy");
        myLogin.setPassword("demo");

        assertTrue(users.contains(myLogin));
    }

    /**
     * Test save operation for user login.
     */
    @Test
    public void testSave() throws IPSGenericDao.SaveException {
        var count = countRows();
        assertEquals(0, count, "user xyzzy already exists");
        addRow();

        log.info("testing save");
        var myLogin = new PSUserLogin();
        myLogin.setUserid("xyzzy");
        myLogin.setPassword("demo2");

        dao.save(myLogin);

        count = countRows();
        assertEquals(1, count);

        var pw2 = jdbcTemplate.queryForObject("select password from userlogin where userid = 'xyzzy'", String.class);
        log.debug("new password is " + pw2);
        assertEquals("demo2", pw2);
    }

    /**
     * Test create operation for user login.
     */
    @Test
    public void testCreate() throws IPSGenericDao.SaveException {
        var myLogin = new PSUserLogin();
        myLogin.setUserid("xyzzy");
        myLogin.setPassword("demo");

        log.info("testing create");
        dao.create(myLogin);

        var count = countRows();
        assertEquals(1, count);
    }

    public PSUserLoginDao getDao() {
        return dao;
    }

    public void setDao(PSUserLoginDao dao) {
        this.dao = dao;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected int countRows() {
        var query = "select count(*) from userlogin where userid = 'xyzzy'";
        return jdbcTemplate.queryForObject(query, Integer.class);
    }

    protected void addRow() {
        jdbcTemplate.execute("insert into userlogin (userid,password) values ('xyzzy', 'demo')");
    }
}
