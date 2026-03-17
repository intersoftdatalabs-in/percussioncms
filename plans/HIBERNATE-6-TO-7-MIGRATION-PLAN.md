# Hibernate 6.6 to 7.2 Migration Plan

**Date:** March 9, 2026
**Current Versions:** Spring Framework 7.0.4 (requires Hibernate 7.1+) | Hibernate ORM 6.6.42.Final | Java 21

---

## Executive Summary

Upgrading from Hibernate 6.6 to 7.2 requires code changes due to removal of deprecated API methods. This is a **major breaking change** requiring careful migration. The upgrade is necessary for full Spring Framework 7.0 support and access to production-recommended features like improved `StatelessSession` support.

---

## Phase 1: Code Refactoring (BLOCKING - Must Complete First)

### Category 1: Session.delete() → Session.remove()

**Total occurrences: 22 files**

#### Critical Locations (Service/DAO Layer)

|                                                                                                                                          File                                                                                                                                           |    Line     |                  Current Code                   |                 Required Change                 |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|-------------------------------------------------|-------------------------------------------------|
| [deployer/src/main/java/com/percussion/services/pkginfo/impl/PSPkgInfoService.java](deployer/src/main/java/com/percussion/services/pkginfo/impl/PSPkgInfoService.java#L120)                                                                                                             | 120,202,382 | `session.delete(...)`                           | `session.remove(...)`                           |
| [deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/generickey/utils/services/rdbms/impl/PSGenericKeyDao.java](deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/generickey/utils/services/rdbms/impl/PSGenericKeyDao.java#L107) | 107         | `session.delete(resetKey)`                      | `session.remove(resetKey)`                      |
| [deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSMetadataDao.java](deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSMetadataDao.java#L102)                         | 102         | `session.delete(entry)`                         | `session.remove(entry)`                         |
| [deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/rdbms/PSFeedDao.java](deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/rdbms/PSFeedDao.java#L179)                                     | 179         | `session.delete(p)`                             | `session.remove(p)`                             |
| [system/services/src/com/percussion/services/widgetbuilder/PSWidgetBuilderDefinitionDao.java](system/services/src/com/percussion/services/widgetbuilder/PSWidgetBuilderDefinitionDao.java#L140)                                                                                         | 140         | `session.delete(definition.get())`              | `session.remove(definition.get())`              |
| [system/services/src/com/percussion/services/linkmanagement/impl/PSManagedLinkDao.java](system/services/src/com/percussion/services/linkmanagement/impl/PSManagedLinkDao.java#L130)                                                                                                     | 130,154     | `session.delete(link)`                          | `session.remove(link)`                          |
| [system/services/src/com/percussion/services/legacy/impl/PSCmsObjectMgr.java](system/services/src/com/percussion/services/legacy/impl/PSCmsObjectMgr.java#L809)                                                                                                                         | 809         | `summaries.forEach(sum -> session.delete(sum))` | `summaries.forEach(sum -> session.remove(sum))` |
| [system/services/src/com/percussion/services/useritems/impl/PSUserItemsDao.java](system/services/src/com/percussion/services/useritems/impl/PSUserItemsDao.java#L161)                                                                                                                   | 161         | `session.delete(userItem)`                      | `session.remove(userItem)`                      |
| [system/services/src/com/percussion/services/siteimportsummary/impl/PSSiteImportSummaryDao.java](system/services/src/com/percussion/services/siteimportsummary/impl/PSSiteImportSummaryDao.java#L137)                                                                                   | 137         | `session.delete(summary)`                       | `session.remove(summary)`                       |
| [system/services/src/com/percussion/services/content/impl/PSContentService.java](system/services/src/com/percussion/services/content/impl/PSContentService.java#L351)                                                                                                                   | 351,401     | `session.delete(...)`                           | `session.remove(...)`                           |
| [system/services/src/com/percussion/services/filestorage/impl/PSHashedFileDAO.java](system/services/src/com/percussion/services/filestorage/impl/PSHashedFileDAO.java#L245)                                                                                                             | 245         | `session.delete(entry)`                         | `session.remove(entry)`                         |
| [system/services/src/com/percussion/services/assembly/impl/PSAssemblyService.java](system/services/src/com/percussion/services/assembly/impl/PSAssemblyService.java#L1878)                                                                                                              | 1878,2061   | `session.delete(...)`                           | `session.remove(...)`                           |
| [projects/sitemanage/src/main/java/com/percussion/integritymanagement/service/impl/PSIntegrityCheckerDao.java](projects/sitemanage/src/main/java/com/percussion/integritymanagement/service/impl/PSIntegrityCheckerDao.java#L85)                                                        | 85          | `session.delete(intStatus)`                     | `session.remove(intStatus)`                     |
| [projects/sitemanage/src/main/java/com/percussion/sitemanage/dao/impl/PSUserLoginDao.java](projects/sitemanage/src/main/java/com/percussion/sitemanage/dao/impl/PSUserLoginDao.java#L74)                                                                                                | 74          | `session.delete(login)`                         | `session.remove(login)`                         |
| [projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/dao/impl/PSImportLogDao.java](projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/dao/impl/PSImportLogDao.java#L101)                                                                             | 101         | `session.delete(logEntry)`                      | `session.remove(logEntry)`                      |
| [projects/sitemanage/src/main/java/com/percussion/metadata/dao/impl/PSMetadataDao.java](projects/sitemanage/src/main/java/com/percussion/metadata/dao/impl/PSMetadataDao.java#L82)                                                                                                      | 82          | `session.delete(data)`                          | `session.remove(data)`                          |

---

### Category 2: Session.saveOrUpdate() → Session.merge() or Session.persist()

**Total occurrences: 6 files**

**Decision Required:** For each location, determine if entity is:
- **New (transient)** → use `session.persist(entity)`
- **Detached** → use `session.merge(entity)`
- **Both cases** → use conditional logic or always use `merge()` for safety

#### Critical Locations

|                                                                                                                                    File                                                                                                                                     |  Line   |           Current Code           |      Required Change      |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|----------------------------------|---------------------------|
| [deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/membership/services/rdbms/impl/PSMembershipDao.java](deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/membership/services/rdbms/impl/PSMembershipDao.java#L153) | 153,205 | `session.saveOrUpdate(member)`   | `session.merge(member)`   |
| [deliverytiersuite/delivery-tier-suite/generickey/utils/services/rdbms/impl/PSGenericKeyDao.java](deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/generickey/utils/services/rdbms/impl/PSGenericKeyDao.java#L95)                              | 95      | `session.saveOrUpdate(resetKey)` | `session.merge(resetKey)` |
| [deliverytiersuite/delivery-tier-suite/polls/src/main/java/com/percussion/delivery/polls/service/rdbms/PSPollsDao.java](deliverytiersuite/delivery-tier-suite/polls/src/main/java/com/percussion/delivery/polls/service/rdbms/PSPollsDao.java#L62)                          | 62      | `session.saveOrUpdate(poll)`     | `session.merge(poll)`     |
| [deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/rdbms/PSFeedDao.java](deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/rdbms/PSFeedDao.java#L137)                         | 137,153 | `session.saveOrUpdate(...)`      | `session.merge(...)`      |

**Note:** Direct DAO methods like `dao.save()` and `dao.delete()` are OK (custom wrappers), only Hibernate Session methods need updating.

---

### Category 3: Session.createQuery() Missing Result Type

**Total occurrences: 6+ locations in DeliveryTierSuite**

**Pattern:** All HQL queries using `session.createQuery(string)` without result type need the class parameter.

|                                                                                                                              File                                                                                                                              | Pattern  |                Before                |                           After                            |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------|------------------------------------------------------------|
| [deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSMetadataDao.java](deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSMetadataDao.java#L80) | Line 80  | `Query q = session.createQuery(hql)` | `Query<Entity> q = session.createQuery(hql, Entity.class)` |
| [deployer/src/main/java/com/percussion/services/pkginfo/impl/PSPkgInfoService.java](deployer/src/main/java/com/percussion/services/pkginfo/impl/PSPkgInfoService.java#L238)                                                                                    | Line 238 | `var q = session.createQuery(query)` | `var q = session.createQuery(query, ResultType.class)`     |

**Good News:** Most code already uses Criteria API with proper typing. These are legacy HQL queries.

---

### Category 4: Cascade Type Annotations

**Current Status:** No `CascadeType.SAVE_UPDATE` or `CascadeType.DELETE` found in annotations.
- Only `CascadeType.DELETE_ORPHAN` found (line 743 in PSAclEntryImpl.java) - this is still valid in 7.x

**Action:** No changes needed in this category.

---

## Phase 2: Dependency Updates

### Step 1: Update pom.xml Versions

**File:** [pom.xml](pom.xml)

|           Property            |   Current    |            Target             |
|-------------------------------|--------------|-------------------------------|
| `hibernate.version`           | 6.6.42.Final | 7.2.x (recommend 7.2.6.Final) |
| `hibernate.validator.version` | 6.2.3.Final  | 8.0.x (recommend 8.0.1.Final) |

### Step 2: Update Cache Configuration Files

**Issue:** `SingletonEhCacheRegionFactory` (removed in Hibernate 6.0) still referenced in DeliveryTierSuite

**Files to Update:**
- [deliverytiersuite/delivery-tier-suite/feeds/src/main/java/webapp/WEB-INF/perc-datasources.xml](deliverytiersuite/delivery-tier-suite/feeds/src/main/java/webapp/WEB-INF/perc-datasources.xml)
- [deliverytiersuite/delivery-tier-suite/forms/src/main/java/webapp/WEB-INF/perc-datasources.xml](deliverytiersuite/delivery-tier-suite/forms/src/main/java/webapp/WEB-INF/perc-datasources.xml)
- [deliverytiersuite/delivery-tier-suite/membership/src/main/java/webapp/WEB-INF/perc-datasources.xml](deliverytiersuite/delivery-tier-suite/membership/src/main/java/webapp/WEB-INF/perc-datasources.xml)
- [deliverytiersuite/delivery-tier-suite/metadata/src/main/java/webapp/WEB-INF/perc-datasources.xml](deliverytiersuite/delivery-tier-suite/metadata/src/main/java/webapp/WEB-INF/perc-datasources.xml)
- [deliverytiersuite/delivery-tier-suite/comments/src/main/java/webapp/WEB-INF/perc-datasources.xml](deliverytiersuite/delivery-tier-suite/comments/src/main/java/webapp/WEB-INF/perc-datasources.xml)
- [deliverytiersuite/delivery-tier-suite/polls/.../perc-datasources.xml]()

**Change Required:**

```xml
<!-- OLD (removed in Hibernate 6.0) -->
<prop key="hibernate.cache.region.factory_class">
    org.hibernate.cache.ehcache.EhcacheRegionFactory
</prop>

<!-- NEW (compatible with 6.6+) -->
<prop key="hibernate.cache.region.factory_class">
    org.hibernate.cache.jcache.JCacheRegionFactory
</prop>
```

---

## Phase 3: Testing Strategy

### Unit Tests

- [ ] Run all `*Test.java` in affected modules
- [ ] Verify DAO save/delete operations work correctly
- [ ] Test Criteria API queries still return proper types

### Integration Tests

- [ ] Start CMS with new Hibernate version
- [ ] Verify cache initialization (cache factory)
- [ ] Test entity persistence (save/load/delete workflow)
- [ ] Verify StatelessSession behavior (now uses 2nd-level cache by default)

### Modules to Test First

1. **DeliveryTierSuite** (has most deprecated usage)
2. **System services** (widgetbuilder, linkmanagement, content)
3. **Core CMS** (assembly, useritems, filestorage)

---

## Phase 4: Behavioral Changes to Aware Of

### Change 1: StatelessSession Now Uses 2nd-Level Cache

- **Before:** Bypassed all caching
- **After:** Uses 2nd-level cache by default
- **Impact:** Improved performance but different semantics
- **Mitigation:** If needed, call `session.setCacheMode(CacheMode.IGNORE)` for old behavior

### Change 2: More Strict Domain Validation

- **Before:** Loose annotation checking
- **After:** Strict enforcement of JPA rules
- **May cause:** Warnings or errors on improper annotations
- **Action:** Review compiler warnings after upgrade

### Change 3: Detached Entities Cannot Be Refreshed

- **Before:** `session.refresh(detachedEntity)` was allowed
- **After:** Throws `IllegalArgumentException`
- **Mitigation:** Use `session.merge(detachedEntity)` first

---

## Implementation Order

### Week 1: Code Changes

- Day 1-2: Update `session.delete()` → `session.remove()` (22 locations)
- Day 3-4: Update `session.saveOrUpdate()` → `session.merge()` (6 locations)
- Day 5: Update `session.createQuery()` calls (6 locations)

### Week 2: Configuration & Testing

- Day 1-2: Update pom.xml versions and cache configs
- Day 3-4: Run full test suite
- Day 5: Integration testing & validation

---

## Risk Assessment

### High Risk

- 🔴 **Session method changes** - Will cause compilation errors if missed
- 🔴 **Cache configuration** - Already causing server failures
- 🔴 **StatelessSession behavior change** - May affect performance expectations

### Medium Risk

- 🟡 **Query type safety** - May reveal type inconsistencies
- 🟡 **Cascade behavior** - With new persist semantics

### Low Risk

- 🟢 **Validation strictness** - Mostly informational warnings
- 🟢 **CascadeType annotations** - Already compliant

---

## Rollback Plan

If critical issues discovered:
1. Revert pom.xml versions to 6.6.42.Final / 6.2.3.Final
2. Revert cache config XML files
3. Revert Session method calls
4. Analyze root cause before retry

---

## Success Criteria

✅ All unit tests pass
✅ Integration tests pass
✅ Server starts without cache initialization errors
✅ All deprecated API calls replaced
✅ No compiler warnings related to Hibernate
✅ Application behavior unchanged from user perspective

---

## References

- [Hibernate 7.0 Migration Guide](https://docs.hibernate.org/orm/7.0/migration-guide/)
- [Hibernate 7.2 Migration Guide](https://docs.hibernate.org/orm/7.2/migration-guide/)
- [Spring Framework 7.0 Release Notes](https://spring.io/blog/2024/11/12/spring-framework-7-0-goes-ga)

