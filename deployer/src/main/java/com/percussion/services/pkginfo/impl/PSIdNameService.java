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

package com.percussion.services.pkginfo.impl;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.pkginfo.IPSIdNameService;
import com.percussion.services.pkginfo.data.PSIdName;
import com.percussion.util.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of the id-name service. */
@Transactional
@PSBaseBean("sys_idNameService")
public class PSIdNameService implements IPSIdNameService {
  @PersistenceContext private EntityManager entityManager;

  private IPSCacheAccess cache;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  public synchronized void deleteAll() {
    var session = getSession();
    loadAll().forEach(session::delete);
    clearCache();
  }

  public synchronized void saveIdName(PSIdName mapping) {
    if (mapping == null) {
      throw new IllegalArgumentException("mapping may not be null");
    }
    getSession().saveOrUpdate(mapping);
    clearCache();
  }

  public synchronized Optional<IPSGuid> findId(String name, PSTypeEnum type) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    var key = getNameTypeKey(name, type);
    return Optional.ofNullable(loadNameTypeToIdMap().get(key));
  }

  public synchronized Optional<String> findName(IPSGuid guid) {
    if (guid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    return Optional.ofNullable(loadIdToNameMap().get(guid));
  }

  private Collection<PSIdName> loadAll() {
    var session = getSession();
    var builder = session.getCriteriaBuilder();
    var criteria = builder.createQuery(PSIdName.class);
    criteria.from(PSIdName.class);
    return entityManager.createQuery(criteria).getResultList();
  }

  private Map<String, IPSGuid> loadNameTypeToIdMap() {
    return loadMap(NAME_TYPE_ID_MAP);
  }

  private Map<IPSGuid, String> loadIdToNameMap() {
    return loadMap(ID_NAME_MAP);
  }

  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> loadMap(int requiredMap) {
    if (requiredMap == NAME_TYPE_ID_MAP) {
      var m = getNameTypeToIdMap();
      if (m != null) {
        return (Map<K, V>) m;
      }
    } else if (requiredMap == ID_NAME_MAP) {
      var m = getIdToNameMap();
      if (m != null) {
        return (Map<K, V>) m;
      }
    } else {
      throw new IllegalArgumentException("unknown map required for load: " + requiredMap);
    }

    var ntMap = new HashMap<String, IPSGuid>();
    var idMap = new HashMap<IPSGuid, String>();
    loadMaps(ntMap, idMap);

    if (requiredMap == NAME_TYPE_ID_MAP) {
      return (Map<K, V>) ntMap;
    } else {
      return (Map<K, V>) idMap;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, IPSGuid> getNameTypeToIdMap() {
    return (Map<String, IPSGuid>) cache.get(NAME_TYPE_ID_MAP_KEY, IPSCacheAccess.IN_MEMORY_STORE);
  }

  @SuppressWarnings("unchecked")
  private Map<IPSGuid, String> getIdToNameMap() {
    return (Map<IPSGuid, String>) cache.get(ID_NAME_MAP_KEY, IPSCacheAccess.IN_MEMORY_STORE);
  }

  private void loadMaps(Map<String, IPSGuid> ntMap, Map<IPSGuid, String> idMap) {
    var mappings = loadAll();
    populateNameTypeToIdMap(mappings, ntMap);
    populateIdToNameMap(mappings, idMap);
  }

  private void populateNameTypeToIdMap(Collection<PSIdName> mappings, Map<String, IPSGuid> ntMap) {
    mappings.forEach(
        mapping -> {
          var guid = new PSGuid(mapping.getId());
          var name = mapping.getName();
          ntMap.put(getNameTypeKey(name, mapping.getType()), guid);
        });
    cache.save(NAME_TYPE_ID_MAP_KEY, ntMap, IPSCacheAccess.IN_MEMORY_STORE);
  }

  private void populateIdToNameMap(Collection<PSIdName> mappings, Map<IPSGuid, String> idMap) {
    mappings.forEach(
        mapping -> {
          var guid = new PSGuid(mapping.getId());
          var name = mapping.getName();
          idMap.put(guid, name);
        });
    cache.save(ID_NAME_MAP_KEY, idMap, IPSCacheAccess.IN_MEMORY_STORE);
  }

  private void clearCache() {
    if (cache != null) {
      cache.evict(NAME_TYPE_ID_MAP_KEY, IPSCacheAccess.IN_MEMORY_STORE);
      cache.evict(ID_NAME_MAP_KEY, IPSCacheAccess.IN_MEMORY_STORE);
    }
  }

  public IPSCacheAccess getCache() {
    return cache;
  }

  public void setCache(IPSCacheAccess cache) {
    if (cache == null) {
      throw new IllegalArgumentException("cache may not be null");
    }
    this.cache = cache;
  }

  private String getNameTypeKey(String name, PSTypeEnum type) {
    return name.toLowerCase() + '-' + type;
  }

  private static final String NAME_TYPE_ID_MAP_KEY =
      "com.percussion.deployer.services.impl.PSIdNameService.name_type_id_map";
  private static final String ID_NAME_MAP_KEY =
      "com.percussion.deployer.services.impl.PSIdnameService.id_name_map";
  private static final int NAME_TYPE_ID_MAP = 0;
  private static final int ID_NAME_MAP = 1;
}
