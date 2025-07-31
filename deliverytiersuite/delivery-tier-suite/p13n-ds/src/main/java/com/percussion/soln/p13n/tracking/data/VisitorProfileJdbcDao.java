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

// REFACTORED: CP-JAVA11
package com.percussion.soln.p13n.tracking.data;

import com.percussion.soln.p13n.tracking.IVisitorProfileDataService;
import com.percussion.soln.p13n.tracking.VisitorProfile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.Map.Entry;
import java.util.UUID;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;

/**
 * Standard JDBC DAO for visitor profiles.
 * Sunny Sal says: "JDBC like a boss, debug like a hero!"
 */
public class VisitorProfileJdbcDao extends JdbcDaoSupport implements IVisitorProfileDataService {

    private static final String VISITOR_PROFILE_TABLE = "visitor_profile";
    private static final String VISITOR_PROFILE_WEIGHT_TABLE = "visitor_profile_weight";
    private static final Log log = LogFactory.getLog(VisitorProfileJdbcDao.class);

    @Override
    public VisitorProfile createProfile() {
        return new VisitorProfile();
    }

    @Override
    public VisitorProfile find(long visitorId) {
        var t = getJdbcTemplate();
        try {
            var profile = t.queryForObject(
                    "select * from " + VISITOR_PROFILE_TABLE + " where id=?",
                    visitorProfileMapper, visitorId);
            if (profile == null) return null;
            loadWeights(profile);
            return profile;
        } catch (EmptyResultDataAccessException e) {
            log.debug("Could not find profile: " + visitorId);
        }
        return null;
    }

    @Override
    public VisitorProfile findByUserId(String userId) {
        if (userId == null) return null;
        VisitorProfile profile;
        var t = getJdbcTemplate();
        var profileIds = t.query(
                "select id from " + VISITOR_PROFILE_TABLE + " where userid=? order by last_updated",
                idMapper, userId);
        if (profileIds.isEmpty()) {
            profile = null;
        } else if (profileIds.size() == 1) {
            profile = find(profileIds.get(0));
        } else {
            log.warn("Found more than one profile for userId: " + userId);
            log.warn("Picking the last updated");
            profile = find(profileIds.get(0));
        }
        return profile;
    }

    @Override
    public boolean hasProfile(long visitorId) {
        var t = getJdbcTemplate();
        if (visitorId == 0) {
            throw new IllegalArgumentException("Visitor Id cannot be zero");
        }
        var profiles = t.queryForList("select id from " + VISITOR_PROFILE_TABLE + " where id = ?", visitorId);
        return !profiles.isEmpty();
    }

    @Override
    public void delete(VisitorProfile profile) {
        deleteProfileWeight(profile.getId());
        getJdbcTemplate().update("delete from " + VISITOR_PROFILE_TABLE + " where id = ?", profile.getId());
    }

    protected void deleteProfileWeight(long profileId) {
        var hasWeights = !getJdbcTemplate()
                .queryForList("select id from " + VISITOR_PROFILE_WEIGHT_TABLE + " where id = ?", profileId).isEmpty();
        if (hasWeights) {
            getJdbcTemplate().update("delete from " + VISITOR_PROFILE_WEIGHT_TABLE + " where id = ?", profileId);
        }
    }

    @Override
    public boolean hasProfile(VisitorProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Profile cannot be null");
        return hasProfile(profile.getId());
    }

    @Override
    public VisitorProfile save(VisitorProfile profile) {
        var t = getJdbcTemplate();
        String query;
        boolean insert = false;
        if (profile.getId() == 0) {
            insert = true;
            profile.setId(nextProfileId());
        }
        if (profile.getLastUpdated() == null) {
            profile.setLastUpdated(new Date());
        }
        Object[] params = new Object[]{
                profile.getLabel(),
                profile.getLastUpdated(),
                profile.isLockProfile(),
                profile.getUserId(),
                profile.getId()
        };
        int[] types = new int[]{
                Types.VARCHAR,
                Types.DATE,
                Types.INTEGER,
                Types.VARCHAR,
                Types.BIGINT
        };

        if (!insert && hasProfile(profile)) {
            query = "update " + VISITOR_PROFILE_TABLE +
                    " set label = ?, last_updated = ?, lock_profile = ?, userid = ? where id = ?";
        } else {
            query = "insert into " + VISITOR_PROFILE_TABLE +
                    " (label,last_updated,lock_profile,userid,id) values (?,?,?,?,?)";
        }
        if (log.isDebugEnabled()) {
            log.debug("Executing SQL: " + query + " with params: " + asList(params));
        }
        t.update(query, params, types);
        deleteProfileWeight(profile.getId());
        query = "insert into " + VISITOR_PROFILE_WEIGHT_TABLE + " (id, segment_id, weight) values (?,?,?)";
        var weights = profile.copySegmentWeights();

        if (weights != null && !weights.isEmpty()) {
            for (Entry<String, Integer> entry : weights.entrySet()) {
                var segId = entry.getKey();
                var tempWeight = entry.getValue();
                var weight = Integer.parseInt(tempWeight.toString());
                if (segId == null) {
                    log.error(format("Bad entry segment id: {0} weight: {1}", segId, weight));
                } else {
                    t.update(query, profile.getId(), entry.getKey(), entry.getValue());
                }
            }
        }
        return profile;
    }

    private Map<String, Integer> retrieveWeights(long id) {
        var t = getJdbcTemplate();
        Object[] p = new Object[]{id};
        final Map<String, Integer> w = new HashMap<>();
        t.query("select * from " + VISITOR_PROFILE_WEIGHT_TABLE + " where id=?", p, new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
                var segId = rs.getString("segment_id");
                if (segId != null) {
                    var weight = rs.getInt("weight");
                    w.put(segId, weight);
                }
            }
        });
        return w;
    }

    private void loadWeights(VisitorProfile profile) {
        profile.setSegmentWeights(retrieveWeights(profile.getId()));
    }

    private void loadWeights(Iterator<VisitorProfile> profiles) {
        if (profiles == null) return;
        while (profiles.hasNext()) {
            loadWeights(profiles.next());
        }
    }

    @Override
    public Iterator<VisitorProfile> retrieveProfiles() {
        var sql = "select * from " + VISITOR_PROFILE_TABLE + " where label is not null";
        var profiles = getJdbcTemplate().query(sql, visitorProfileMapper);
        loadWeights(profiles.iterator());
        return profiles.iterator();
    }

    @Override
    public List<VisitorProfile> retrieveTestProfiles() {
        var sql = "select * from " + VISITOR_PROFILE_TABLE + " where label is not null order by label";
        var profiles = getJdbcTemplate().query(sql, visitorProfileMapper);
        loadWeights(profiles.iterator());
        return profiles;
    }

    private static final VisitorProfileRowMapper visitorProfileMapper = new VisitorProfileRowMapper();
    private static final IdRowMapper idMapper = new IdRowMapper();

    public static class IdRowMapper extends SingleColumnRowMapper<Long> {
        @Override
        public Long mapRow(ResultSet rs, int index) throws SQLException {
            return rs.getLong(1);
        }
    }

    public static class VisitorProfileRowMapper extends SingleColumnRowMapper<VisitorProfile> {
        @Override
        public VisitorProfile mapRow(ResultSet rs, int index) throws SQLException {
            var profile = new VisitorProfile();
            profile.setId(rs.getLong("id"));
            profile.setUserId(rs.getString("userid"));
            profile.setLabel(rs.getString("label"));
            profile.setLastUpdated(getDate("last_updated", rs));
            profile.setLockProfile(rs.getBoolean("lock_profile"));
            return profile;
        }
    }

    private static Date getDate(String column, ResultSet rs) throws SQLException {
        var d = rs.getDate(column);
        if (d == null) return null;
        return new Date(d.getTime());
    }

    private long nextProfileId() {
        return UUID.randomUUID().getMostSignificantBits();
    }
}