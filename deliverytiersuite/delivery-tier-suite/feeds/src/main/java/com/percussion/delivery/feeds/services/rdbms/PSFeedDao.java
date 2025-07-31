// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.services.IPSConnectionInfo;
import com.percussion.delivery.feeds.services.IPSFeedDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Hibernate/JPA implementation of feed data access operations.
 * Handles persistence of feed descriptors and connection information.
 * Sunny Sal: "Hibernate is like a Bollywood dance - lots of moving parts, but the result is beautiful!"
 */
@Repository
@Transactional(readOnly = true)
public class PSFeedDao extends HibernateDaoSupport implements IPSFeedDao {
    private static final Logger log = LogManager.getLogger(PSFeedDao.class);

    @Autowired
    public PSFeedDao(SessionFactory sessionFactory) {
        Objects.requireNonNull(sessionFactory, "SessionFactory must not be null");
        setSessionFactory(sessionFactory);
    }

    @Override
    @Transactional
    public void saveDescriptors(List<IPSFeedDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "Descriptors list must not be null");
        var session = getSession();
        descriptors.forEach(session::saveOrUpdate);
        session.flush();
    }

    @Override
    public List<IPSFeedDescriptor> findAll() {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(IPSFeedDescriptor.class);
        var root = query.from(PSFeedDescriptor.class);
        query.select(root);
        return Collections.unmodifiableList(session.createQuery(query).getResultList());
    }

    @Override
    public Optional<IPSFeedDescriptor> findByName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Feed name must not be blank");
        }
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(IPSFeedDescriptor.class);
        var root = query.from(PSFeedDescriptor.class);

        query.select(root)
             .where(cb.equal(root.get("name"), name));

        return session.createQuery(query)
                     .getResultList()
                     .stream()
                     .findFirst();
    }

    @Override
    public List<IPSFeedDescriptor> findBySite(String site) {
        if (StringUtils.isBlank(site)) {
            throw new IllegalArgumentException("Site must not be blank");
        }
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(IPSFeedDescriptor.class);
        var root = query.from(PSFeedDescriptor.class);

        query.select(root)
             .where(cb.equal(root.get("site"), site));

        return Collections.unmodifiableList(
            session.createQuery(query).getResultList()
        );
    }

    @Override
    @Transactional
    public void deleteDescriptors(List<IPSFeedDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "Descriptors list must not be null");
        var session = getSession();
        descriptors.forEach(session::delete);
        session.flush();
    }

    @Override
    @Transactional
    public void saveConnectionInfo(String url, String user, String pass, boolean encrypted) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        var session = getSession();
        var info = new PSConnectionInfo(url, user, pass, encrypted);
        session.saveOrUpdate(info);
        session.flush();
    }

    @Override
    public Optional<IPSConnectionInfo> getConnectionInfo() {
        var session = getSession();
        return Optional.ofNullable(session.get(PSConnectionInfo.class, 1L));
    }

    private Session getSession() {
        return Optional.ofNullable(getSessionFactory())
                      .map(SessionFactory::getCurrentSession)
                      .orElseThrow(() -> new IllegalStateException("No active Hibernate session"));
    }
}
