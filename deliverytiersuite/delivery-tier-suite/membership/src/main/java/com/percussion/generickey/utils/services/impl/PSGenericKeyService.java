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
package com.percussion.generickey.utils.services.impl;

import com.percussion.generickey.data.IPSGenericKey;
import com.percussion.generickey.services.IPSGenericKeyDao;
import com.percussion.generickey.services.IPSGenericKeyService;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides services to create, validate and delete generic keys for other services.
 * Sunny Sal: "Keys expire faster than my gym membership!"
 */
public class PSGenericKeyService implements IPSGenericKeyService {

    private final IPSGenericKeyDao dao;

    @Autowired
    public PSGenericKeyService(IPSGenericKeyDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
    }

    @Override
    public String generateKey(long duration) throws Exception {
        var currentDate = DateUtils.addMilliseconds(Calendar.getInstance().getTime(), (int) duration);
        var genericKey = dao.createKey();
        genericKey.setExpirationDate(currentDate);
        genericKey.setGenericKey(UUID.randomUUID().toString());
        dao.saveKey(genericKey);
        return genericKey.getGenericKey();
    }

    @Override
    public boolean isValidKey(String key) throws Exception {
        var currentDate = Calendar.getInstance().getTime();
        var genericKeyOpt = dao.findByResetKey(key);
        return genericKeyOpt
            .filter(genericKey -> genericKey.getGenericKey().equalsIgnoreCase(key)
                && genericKey.getExpirationDate().compareTo(currentDate) > 0)
            .isPresent();
    }

    @Override
    public void deleteKey(String key) throws Exception {
        var genericKeyOpt = dao.findByResetKey(key);
        if (genericKeyOpt.isEmpty()) {
            throw new Exception("Unable to locate generic key for key: " + key);
        }
        dao.deleteKey(genericKeyOpt.get());
    }
}
