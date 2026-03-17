# WebUI Build Structure Refactoring Plan

## Executive Summary

Consolidate all `system/ear` files that are currently copied into the WebUI WAR at Maven build time into their proper source locations within `WebUI/src/main/`. This eliminates unnecessary build-time copy operations, improves maintainability, and makes the source tree structure match the WAR structure.

**Current State**: 7 directory copy operations in `WebUI/pom.xml` (lines 579-640)
**Target State**: All files located in proper Maven source directories; zero copy operations from system/ear

**Effort**: Medium (consolidation task, no code changes required)
**Risk**: Low (purely structural, no logic changes)
**Timeline**: Can be done incrementally

---

## Current Build Structure Analysis

### Files Currently Being Copied

| Source (system/ear)  |    Target (WAR)    |   Size    |                             Notes                              |
|----------------------|--------------------|-----------|----------------------------------------------------------------|
| `config/`            | `WEB-INF/config`   | ~40 files | Spring configs, security configs, user configs, velocity       |
| `config/hibernate/`  | `WEB-INF/classes`  | 1 file    | ehcache.xml needed at runtime                                  |
| `config/trinidad/`   | `WEB-INF/trinidad` | 1 file    | rx.css CSS theme                                               |
| `metadata/`          | `META-INF`         | 3 files   | Spring handlers, schemas, application.xml                      |
| `WEB-INF/`           | `WEB-INF`          | ~20 files | JSF configs, TLD files, jetty-env.xml, web.xml, tags, images   |
| `jsps/` (filtered)   | `/ (root)`         | See below | JSP files and JNLP descriptors with property filtering enabled |
| `jsps/` (unfiltered) | `/ (root)`         | See below | Static assets (images, etc.) without filtering                 |

### POM Copy Operations (WebUI/pom.xml lines 579-640)

```xml
<resource>
  <directory>../system/ear/config</directory>
  <targetPath>WEB-INF/config</targetPath>
  <filtering>false</filtering>
  <excludes>
    <exclude>**/trinidad/*</exclude>
    <exclude>**/hibernate/*</exclude>
  </excludes>
</resource>

<resource>
  <directory>../system/ear/config/hibernate</directory>
  <targetPath>WEB-INF/classes</targetPath>
  <filtering>false</filtering>
</resource>

<resource>
  <directory>../system/ear/config/trinidad</directory>
  <targetPath>WEB-INF/trinidad</targetPath>
  <filtering>false</filtering>
</resource>

<resource>
  <directory>../system/ear/metadata</directory>
  <targetPath>META-INF</targetPath>
  <filtering>true</filtering>
</resource>

<resource>
  <directory>../system/ear/WEB-INF</directory>
  <targetPath>WEB-INF</targetPath>
  <filtering>false</filtering>
</resource>

<resource>
  <directory>../system/ear/jsps</directory>
  <targetPath></targetPath>
  <filtering>true</filtering>
  <excludes>
    <exclude>favicon.ico</exclude>
  </excludes>
</resource>

<resource>
  <directory>../system/ear/jsps</directory>
  <targetPath></targetPath>
  <filtering>false</filtering>
  <excludes>
    <exclude>**/*.jsp</exclude>
    <exclude>**/*.jnlp</exclude>
    <exclude>**/*.html</exclude>
  </excludes>
</resource>
```

### Existing WebUI/src Structure

```
WebUI/src/main/
├── java/
│   └── com/...
├── resources/
│   ├── favicon.ico
│   └── minify/
├── webapp/
│   ├── WEB-INF/
│   │   ├── classes/...       (JAR contents after build)
│   │   ├── tlds/
│   │   ├── Owasp.CsrfGuard.js
│   │   ├── tmxtags.tld
│   │   └── web.xml           (Note: already here!)
│   ├── META-INF/
│   │   └── README.txt
│   ├── cm/
│   ├── jslib/
│   └── ...
├── frontend/
├── ts/
```

---

## Refactoring Plan

### Phase 1: Preparation & Validation

**Goal**: Understand current system/ear usage and identify any shared files

**Tasks**:

1. **Inventory all system/ear subdirectories**
   - ✅ `config/` - Spring configs, security, trinidad, user, velocity (to be moved)
   - ✅ `metadata/` - Spring metadata, XML schemas (to be moved)
   - ✅ `WEB-INF/` - JSF configs, TLD files, jetty-env.xml, etc. (to be moved)
   - ✅ `jsps/` - JSP pages, JNLP descriptors, test JSPs (to be moved)
   - ⚠️ `jboss-4.0/` - Legacy JBoss config (DEPRECATED - should be removed in separate task)
   - ⚠️ `webdav/` - WebDAV config (check if used elsewhere)
   - ⚠️ `test/` - Test data (not copied; keep in system/ear for reference)
2. **Verify no other modules reference system/ear**
   - Status: ✅ Only WebUI/pom.xml references system/ear
3. **Check for conflicts between system/ear and existing WebUI sources**
   - Issue: `WEB-INF/web.xml` exists in both places!
   - Decision: Keep WebUI version (its content takes precedence)
   - Files to merge/review:
     - web.xml - Need to compare versions
     - Any other duplicate files?
4. **Document all files that need filtering vs. non-filtered**
   - Filtering: metadata/ (pom properties), jsps/ (JSP files only)
   - Non-filtered: Everything else

**Files to Check**: Compare existing files between system/ear and WebUI/src/main/webapp/WEB-INF/

### Phase 2: Directory Structure Preparation

**Goal**: Create target directories before moving files

**Tasks**:

1. Create new directories in WebUI/src/main/webapp:

   ```bash
   mkdir -p src/main/webapp/WEB-INF/config/{spring,security,user,velocity}
   mkdir -p src/main/webapp/WEB-INF/config/hibernate
   mkdir -p src/main/webapp/WEB-INF/config/trinidad
   mkdir -p src/main/webapp/WEB-INF/tags/{banner,layout,nav}
   ```
2. Create new directories in WebUI/src/main/webapp:

   ```bash
   mkdir -p src/main/webapp/dce
   mkdir -p src/main/webapp/reports
   mkdir -p src/main/webapp/test/{jsx,tmx,ui}
   mkdir -p src/main/webapp/jslib  (might already exist)
   ```
3. Create resource directory for hibernate configs:

   ```bash
   mkdir -p src/main/resources/config
   ```

### Phase 3: File Movement

**Critical**: Requires careful testing to verify no WAR structure changes

#### Step 1: Move config/ directory

**Source**: `system/ear/config/`
**Target**: `WebUI/src/main/webapp/WEB-INF/config/`

Files to move:
- `config/spring/*` → `WEB-INF/config/spring/`
- `config/security/*` → `WEB-INF/config/security/`
- `config/user/*` → `WEB-INF/config/user/`
- `config/velocity/*` → `WEB-INF/config/velocity/`

Note: `config/hibernate/` and `config/trinidad/` handled separately

#### Step 2: Move config/hibernate/

**Source**: `system/ear/config/hibernate/`
**Target**: `WebUI/src/main/resources/config/`

Reason: Files in src/main/resources are copied to WEB-INF/classes at build time by Maven. The pom.xml resource config targets `WEB-INF/classes` for this directory.

Files:
- `ehcache.xml` → `src/main/resources/config/ehcache.xml`

**POM Update**: Add resource filter:

```xml
<resource>
  <directory>src/main/resources/config</directory>
  <targetPath>WEB-INF/classes</targetPath>
  <filtering>false</filtering>
</resource>
```

#### Step 3: Move config/trinidad/

**Source**: `system/ear/config/trinidad/`
**Target**: `WebUI/src/main/webapp/WEB-INF/trinidad/`

Files:
- `rx.css` → `WEB-INF/trinidad/rx.css`

Wait - this is a static CSS file. Check why it needs to go to WEB-INF. May need to move to webapp/css/ instead?

#### Step 4: Move WEB-INF files

**Source**: `system/ear/WEB-INF/`
**Target**: `WebUI/src/main/webapp/WEB-INF/`

⚠️ **CONFLICT**: `web.xml` exists in both locations!
- **Decision**: Keep WebUI version, merge if necessary

Files to move:
- `admin-faces-config.xml` → `WEB-INF/admin-faces-config.xml`
- `faces-config.xml` → `WEB-INF/faces-config.xml`
- `publishing-faces-config.xml` → `WEB-INF/publishing-faces-config.xml`
- `jetty-env.xml` → `WEB-INF/jetty-env.xml` (ALREADY DONE - fix reference if needed)
- `servicesContext-ws.xml` → `WEB-INF/servicesContext-ws.xml`
- `trinidad-config.xml` → `WEB-INF/trinidad-config.xml`
- `trinidad-skins.xml` → `WEB-INF/trinidad-skins.xml`
- `rxcomp.tld` → `WEB-INF/rxcomp.tld`
- `server-config.wsdd` → `WEB-INF/server-config.wsdd`
- `tags/` → `WEB-INF/tags/`
- `tlds/` → `WEB-INF/tlds/`
- `images/` → `WEB-INF/images/`

Skip:
- `web.xml` (keep existing WebUI version)

#### Step 5: Move metadata/

**Source**: `system/ear/metadata/`
**Target**: `WebUI/src/main/webapp/META-INF/`

Note: Filtering enabled (to substitute properties)

Files:
- `application.xml` → `META-INF/application.xml`
- `spring.handlers` → `META-INF/spring.handlers`
- `spring.schemas` → `META-INF/spring.schemas`

**POM Update**: Add filtering to webapp resource:

```xml
<resource>
  <directory>src/main/webapp</directory>
  <includes>**/META-INF/**</includes>
  <filtering>true</filtering>
</resource>
```

Or more specifically:

```xml
<resource>
  <directory>src/main/webapp/META-INF</directory>
  <filtering>true</filtering>
  <targetPath>META-INF</targetPath>
</resource>
```

#### Step 6: Move JSPs

**Source**: `system/ear/jsps/`
**Target**: `WebUI/src/main/webapp/`

Simply copy the entire tree as-is:

```
jsps/ → webapp/
├── dce/
├── reports/
├── test/
├── tmx/
├── ui/
├── *.jsp files
├── *.jnlp files
└── *.html files
```

Skip:
- `favicon.ico` (already in resources)

**Filtering**: Don't apply filtering at this level. Any filtering needed during distribution/installation will be handled by the build packaging layer, not the WebUI module itself.

**POM Update**: Simply remove both `../system/ear/jsps` copy operations (lines 630-640) from pom.xml—no special resource configuration needed.

### Phase 4: POM File Updates

**Goal**: Remove all system/ear copy operations, may add new resource definitions

1. **Remove system/ear copy operations** (Lines 579-640 in WebUI/pom.xml)
   - Remove 7 obsolete resource elements
2. **Add new resource elements for META-INF filtered content** (if filtering needed)
3. **Ensure webapp resources are properly defined** for build process
4. **Verify build-helper-maven-plugin configuration** if used for resource filtering

### Phase 5: Build Verification

**Goal**: Ensure WAR output structure matches input structure

**Tests**:
1. Run: `mvn clean package` on WebUI
2. Compare WAR structure with previous builds
3. Extract and verify:
- `WEB-INF/config/*` exists and has all files
- `WEB-INF/trinity/*` exists
- `META-INF/` has spring metadata
- JavaServer Faces configs loaded
- JSPs compiled
- Static assets present

### Phase 6: Integration Testing

**Goal**: Verify that Jetty still works with consolidated sources

**Tests**:
1. Deploy updated WAR to Jetty 12
2. Start server: `./StartJetty.sh`
3. Verify:
- Rhythmyx webapp deploys successfully
- No missing config file errors
- JSFs render properly
- Database connections work
- Static assets load (images, CSS, etc.)
4. Run smoke tests to verify:
- Login page loads
- Configuration loads correctly
- No resource not found errors in logs

---

## Migration Steps (Execution Order)

### Preparation Phase (0-1 days)

- [ ] Create backup of system/ear directory
- [ ] Create backup of WebUI/src directory
- [ ] Document current web.xml from both locations and decide merge strategy
- [ ] Run clean package to establish baseline build time

### Execution Phase (1-2 days)

**Note**: These can be done incrementally, one directory at a time

1. **Move config/ files** (safest first)
   - [ ] Copy system/ear/config → WebUI/src/main/webapp/WEB-INF/config
   - [ ] Verify files present
   - [ ] Remove system/ear/config copy from pom.xml
   - [ ] Run mvn clean package
   - [ ] Verify WAR structure
2. **Move WEB-INF/ files** (watch for web.xml conflict)
   - [ ] Merge/verify web.xml versions
   - [ ] Copy system/ear/WEB-INF → WebUI/src/main/webapp/WEB-INF (skip web.xml)
   - [ ] Verify files present
   - [ ] Remove system/ear/WEB-INF copy from pom.xml
   - [ ] Run mvn clean package
   - [ ] Verify WAR structure
3. **Move metadata/** (requires filtering setup)
   - [ ] Copy system/ear/metadata → WebUI/src/main/webapp/META-INF
   - [ ] Add filtering configuration to pom.xml
   - [ ] Run mvn clean package
   - [ ] Verify properties substituted correctly
4. **Move JSPs** (tricky with filtering)
   - [ ] Copy system/ear/jsps → WebUI/src/main/webapp
   - [ ] Handle filtering for JSP files
   - [ ] Remove favicon.ico duplicate
   - [ ] Remove system/ear/jsps copy from pom.xml
   - [ ] Run mvn clean package
   - [ ] Verify JSP compilation works
5. **Move config/hibernate/** (classpath resources)
   - [ ] Copy system/ear/config/hibernate → WebUI/src/main/resources
   - [ ] Update pom.xml resource configuration (if needed)
   - [ ] Run mvn clean package
6. **Move config/trinidad/**
   - [ ] Clarify correct target location
   - [ ] Copy files
   - [ ] Remove from pom.xml
   - [ ] Run mvn clean package

### Cleanup Phase (1 day)

- [ ] Remove all system/ear copy operations from WebUI/pom.xml (lines 579-640)
- [ ] Simplify pom.xml resource section
- [ ] Run final clean package
- [ ] Deploy and test with Jetty

### Optional: Legacy Cleanup (separate task)

- [ ] Investigate system/ear/jboss-4.0/ directory
  - Likely obsolete (JBoss 4 is antique - project is on Jetty 12)
  - Recommend removing if confirmed unused
- [ ] Investigate system/ear/webdav/
  - Verify if used by WebUI or any other module
  - If used, clarify deployment strategy
- [ ] Consider removing system/ear entirely if only used by WebUI

---

## Risk Assessment

|                Risk                | Likelihood |  Impact  |                   Mitigation                    |
|------------------------------------|------------|----------|-------------------------------------------------|
| WAR structure mismatch             | Low        | High     | Automate WAR comparison script, test thoroughly |
| Property filtering breaks          | Medium     | Medium   | Test with known properties, verify substitution |
| File permissions/encoding issues   | Low        | Low      | Use git mv, preserve attributes                 |
| Merge conflicts in version control | Low        | Medium   | Do synchronously,good commit messages           |
| JSP compilation changes            | Low        | High     | Compare JSP bytecode before/after               |
| Application breaks on deploy       | Low        | Critical | Full integration test on Jetty                  |

---

## Benefits

1. **Simplified build**: Fewer copy operations, faster Maven builds
2. **Clearer structure**: Files live where they belong conceptually
3. **Easier maintenance**: Single source-of-truth for each file
4. **Better IDE integration**: IDEs understand Maven structure better
5. **Easier to contribute**: New developers find files faster
6. **Reduced confusion**: No "where did that file come from?" questions

---

## Files Modified

|               File                |                          Changes                           | Priority |
|-----------------------------------|------------------------------------------------------------|----------|
| `WebUI/pom.xml`                   | Remove 7 resource copy entries (lines 579-640)             | High     |
| `WebUI/src/main/webapp/WEB-INF/`  | Add directories from system/ear/config, system/ear/WEB-INF | High     |
| `WebUI/src/main/webapp/META-INF/` | Add files from system/ear/metadata                         | High     |
| `WebUI/src/main/resources/`       | Add config/hibernate files                                 | Medium   |
| `WebUI/src/main/webapp/`          | Add JSP tree from system/ear/jsps                          | High     |

---

## Rollback Plan

If issues occur:

1. **Before starting**: Create git branch for this work

   ```bash
   git checkout -b refactor/webui-build-structure
   ```
2. **At any point**: Revert changes

   ```bash
   git reset --hard HEAD
   ```
3. **To test before/after**: Compare WAR contents

   ```bash
   unzip -l target/WebUI-8.2-dev.war | grep -E "config|metadata|jsps" | sort
   ```
4. **If partial completion**: Tag current state, move one operation at a time

---

## Communication

- **Team notification**: Brief team before starting
- **Progress updates**: Notify of significant milestones
- **Build verification**: Run CI/CD to validate each step
- **Documentation**: Update README.md if needed

---

## Success Criteria

- ✅ All 7 copy operations removed from WebUI/pom.xml
- ✅ WebUI build time unchanged or faster
- ✅ WAR structure identical to before
- ✅ All files packaged correctly
- ✅ Jetty 12 deployment successful
- ✅ Rhythmyx application loads without errors
- ✅ No resource not found errors
- ✅ Tests pass

---

## Document Change Log

|    Date    | Version |                       Changes                        |
|------------|---------|------------------------------------------------------|
| 2025-03-08 | 1.0     | Initial plan created based on WebUI/pom.xml analysis |

